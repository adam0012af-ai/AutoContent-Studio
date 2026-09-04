import express from 'express';
import Database from 'better-sqlite3';
import { customAlphabet } from 'nanoid';

const app = express();
app.use(express.json());

const db = new Database('gamehub.db');
const makePlayerId = customAlphabet('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', 8);
const adminKey = process.env.ADMIN_KEY || '';

db.exec(`
CREATE TABLE IF NOT EXISTS players (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  coins INTEGER NOT NULL DEFAULT 1000,
  role TEXT NOT NULL DEFAULT 'player',
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS coin_ledger (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  player_id TEXT NOT NULL,
  amount INTEGER NOT NULL,
  reason TEXT NOT NULL,
  actor TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS matches (
  id TEXT PRIMARY KEY,
  game TEXT NOT NULL,
  stake INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'open',
  winner_id TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS match_players (
  match_id TEXT NOT NULL,
  player_id TEXT NOT NULL,
  PRIMARY KEY(match_id, player_id)
);
`);

function getPlayer(id) {
  return db.prepare('SELECT id, display_name, coins, role, created_at FROM players WHERE id = ?').get(id);
}

function requireAdmin(req, res, next) {
  if (!adminKey || req.header('x-admin-key') !== adminKey) {
    return res.status(403).json({ error: 'admin_required' });
  }
  next();
}

app.post('/players', (req, res) => {
  const name = String(req.body?.displayName || '').trim().slice(0, 30);
  if (!name) return res.status(400).json({ error: 'display_name_required' });
  let id;
  do id = makePlayerId(); while (getPlayer(id));
  db.prepare('INSERT INTO players (id, display_name) VALUES (?, ?)').run(id, name);
  db.prepare('INSERT INTO coin_ledger (player_id, amount, reason, actor) VALUES (?, ?, ?, ?)')
    .run(id, 1000, 'welcome_bonus', 'system');
  res.status(201).json(getPlayer(id));
});

app.get('/players/:id', (req, res) => {
  const player = getPlayer(req.params.id);
  if (!player) return res.status(404).json({ error: 'player_not_found' });
  res.json(player);
});

app.post('/admin/coins/adjust', requireAdmin, (req, res) => {
  const playerId = String(req.body?.playerId || '');
  const amount = Number(req.body?.amount);
  const reason = String(req.body?.reason || 'admin_adjustment').slice(0, 100);
  if (!Number.isInteger(amount) || amount === 0) return res.status(400).json({ error: 'invalid_amount' });
  const player = getPlayer(playerId);
  if (!player) return res.status(404).json({ error: 'player_not_found' });
  if (player.coins + amount < 0) return res.status(409).json({ error: 'insufficient_balance' });
  const tx = db.transaction(() => {
    db.prepare('UPDATE players SET coins = coins + ? WHERE id = ?').run(amount, playerId);
    db.prepare('INSERT INTO coin_ledger (player_id, amount, reason, actor) VALUES (?, ?, ?, ?)')
      .run(playerId, amount, reason, 'admin');
  });
  tx();
  res.json(getPlayer(playerId));
});

app.post('/matches', (req, res) => {
  const game = String(req.body?.game || '').toLowerCase();
  const stake = Number(req.body?.stake);
  const playerIds = Array.isArray(req.body?.playerIds) ? req.body.playerIds.map(String) : [];
  if (!['domino', 'cards', 'ludo'].includes(game)) return res.status(400).json({ error: 'unsupported_game' });
  if (!Number.isInteger(stake) || stake < 0) return res.status(400).json({ error: 'invalid_stake' });
  if (playerIds.length < 2 || playerIds.length > 4 || new Set(playerIds).size !== playerIds.length) {
    return res.status(400).json({ error: 'invalid_players' });
  }
  const players = playerIds.map(getPlayer);
  if (players.some(p => !p)) return res.status(404).json({ error: 'player_not_found' });
  if (players.some(p => p.coins < stake)) return res.status(409).json({ error: 'insufficient_balance' });
  const matchId = `M-${makePlayerId()}`;
  const tx = db.transaction(() => {
    db.prepare('INSERT INTO matches (id, game, stake) VALUES (?, ?, ?)').run(matchId, game, stake);
    for (const p of players) {
      db.prepare('UPDATE players SET coins = coins - ? WHERE id = ?').run(stake, p.id);
      db.prepare('INSERT INTO coin_ledger (player_id, amount, reason, actor) VALUES (?, ?, ?, ?)')
        .run(p.id, -stake, `match_entry:${matchId}`, 'system');
      db.prepare('INSERT INTO match_players (match_id, player_id) VALUES (?, ?)').run(matchId, p.id);
    }
  });
  tx();
  res.status(201).json({ id: matchId, game, stake, playerIds, pot: stake * playerIds.length });
});

app.post('/matches/:id/settle', (req, res) => {
  const match = db.prepare('SELECT * FROM matches WHERE id = ?').get(req.params.id);
  if (!match) return res.status(404).json({ error: 'match_not_found' });
  if (match.status !== 'open') return res.status(409).json({ error: 'match_already_settled' });
  const winnerId = String(req.body?.winnerId || '');
  const member = db.prepare('SELECT 1 FROM match_players WHERE match_id = ? AND player_id = ?').get(match.id, winnerId);
  if (!member) return res.status(400).json({ error: 'winner_not_in_match' });
  const count = db.prepare('SELECT COUNT(*) AS c FROM match_players WHERE match_id = ?').get(match.id).c;
  const pot = match.stake * count;
  const tx = db.transaction(() => {
    db.prepare('UPDATE matches SET status = ?, winner_id = ? WHERE id = ?').run('settled', winnerId, match.id);
    db.prepare('UPDATE players SET coins = coins + ? WHERE id = ?').run(pot, winnerId);
    db.prepare('INSERT INTO coin_ledger (player_id, amount, reason, actor) VALUES (?, ?, ?, ?)')
      .run(winnerId, pot, `match_win:${match.id}`, 'system');
  });
  tx();
  res.json({ matchId: match.id, winnerId, prize: pot, winner: getPlayer(winnerId) });
});

app.get('/players/:id/ledger', (req, res) => {
  const rows = db.prepare('SELECT amount, reason, actor, created_at FROM coin_ledger WHERE player_id = ? ORDER BY id DESC LIMIT 100').all(req.params.id);
  res.json(rows);
});

app.get('/health', (_req, res) => res.json({ ok: true }));

const port = Number(process.env.PORT || 8080);
app.listen(port, () => console.log(`Game Hub server listening on :${port}`));

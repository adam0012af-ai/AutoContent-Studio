'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { WebSocketServer } = require('ws');

const PORT = Number(process.env.PORT || 8080);
const ADMIN_PLAYER_ID = process.env.ADMIN_PLAYER_ID || '';
const DATA_FILE = path.join(__dirname, 'data.json');
const START_COINS = 1000;

let state = { players: {}, rooms: {}, ledger: [] };
try { state = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch (_) {}

function save() {
  const temp = DATA_FILE + '.tmp';
  fs.writeFileSync(temp, JSON.stringify(state, null, 2));
  fs.renameSync(temp, DATA_FILE);
}
function id(prefix, bytes = 4) { return prefix + crypto.randomBytes(bytes).toString('hex').toUpperCase(); }
function send(ws, type, data = {}) { if (ws.readyState === 1) ws.send(JSON.stringify({ type, ...data })); }
function publicPlayer(p) { return { playerId: p.playerId, name: p.name, coins: p.coins, role: p.role, online: !!p.online }; }
function requireAuth(ws) { if (!ws.playerId || !state.players[ws.playerId]) throw new Error('AUTH_REQUIRED'); return state.players[ws.playerId]; }
function requireAdmin(ws) { const p = requireAuth(ws); if (p.role !== 'admin') throw new Error('ADMIN_REQUIRED'); return p; }
function ledger(kind, playerId, amount, meta = {}) { state.ledger.push({ id: id('L-', 6), at: Date.now(), kind, playerId, amount, ...meta }); if (state.ledger.length > 5000) state.ledger.splice(0, 1000); }

const server = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'content-type': 'application/json' });
    return res.end(JSON.stringify({ ok: true, players: Object.keys(state.players).length, rooms: Object.keys(state.rooms).length }));
  }
  res.writeHead(404); res.end('Not found');
});

const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
  send(ws, 'hello', { protocol: 1 });

  ws.on('message', (raw) => {
    try {
      const m = JSON.parse(raw.toString());

      if (m.type === 'register') {
        let p = m.playerId && state.players[m.playerId];
        if (p) {
          if (!m.token || m.token !== p.token) throw new Error('BAD_TOKEN');
        } else {
          const playerId = id('P-', 3);
          p = { playerId, token: id('', 16), name: String(m.name || 'Player').slice(0, 20), coins: START_COINS, role: playerId === ADMIN_PLAYER_ID ? 'admin' : 'player', friends: [], online: false };
          state.players[playerId] = p;
          ledger('welcome', playerId, START_COINS);
        }
        p.online = true; ws.playerId = p.playerId; save();
        return send(ws, 'session', { player: publicPlayer(p), token: p.token });
      }

      if (m.type === 'profile') return send(ws, 'profile', { player: publicPlayer(requireAuth(ws)) });

      if (m.type === 'adminCoins') {
        requireAdmin(ws);
        const target = state.players[m.playerId];
        const amount = Math.trunc(Number(m.amount));
        if (!target || !Number.isFinite(amount) || amount === 0) throw new Error('BAD_REQUEST');
        if (target.coins + amount < 0) throw new Error('INSUFFICIENT_COINS');
        target.coins += amount;
        ledger('admin', target.playerId, amount, { by: ws.playerId }); save();
        return send(ws, 'coinsUpdated', { player: publicPlayer(target) });
      }

      if (m.type === 'createRoom') {
        const p = requireAuth(ws);
        const stake = Math.max(0, Math.trunc(Number(m.stake || 0)));
        const target = [101, 151].includes(Number(m.target)) ? Number(m.target) : 101;
        const maxPlayers = Number(m.maxPlayers) === 4 ? 4 : 2;
        if (p.coins < stake) throw new Error('INSUFFICIENT_COINS');
        const roomId = id('R-', 3);
        state.rooms[roomId] = { roomId, game: 'domino', ownerId: p.playerId, target, maxPlayers, stake, players: [p.playerId], ready: {}, scores: {}, pot: 0, status: 'lobby', settled: false };
        save(); return send(ws, 'room', { room: state.rooms[roomId] });
      }

      if (m.type === 'joinRoom') {
        const p = requireAuth(ws); const room = state.rooms[m.roomId];
        if (!room || room.status !== 'lobby') throw new Error('ROOM_NOT_FOUND');
        if (!room.players.includes(p.playerId) && room.players.length >= room.maxPlayers) throw new Error('ROOM_FULL');
        if (p.coins < room.stake) throw new Error('INSUFFICIENT_COINS');
        if (!room.players.includes(p.playerId)) room.players.push(p.playerId);
        save(); return send(ws, 'room', { room });
      }

      if (m.type === 'startMatch') {
        const p = requireAuth(ws); const room = state.rooms[m.roomId];
        if (!room || room.ownerId !== p.playerId || room.status !== 'lobby') throw new Error('BAD_ROOM_STATE');
        if (room.players.length < 2) throw new Error('NEED_PLAYERS');
        for (const pid of room.players) if (state.players[pid].coins < room.stake) throw new Error('INSUFFICIENT_COINS');
        for (const pid of room.players) { state.players[pid].coins -= room.stake; room.pot += room.stake; ledger('stake', pid, -room.stake, { roomId: room.roomId }); room.scores[pid] = 0; }
        room.status = 'playing'; save(); return send(ws, 'matchStarted', { room });
      }

      if (m.type === 'addRoundScore') {
        const p = requireAuth(ws); const room = state.rooms[m.roomId];
        if (!room || room.status !== 'playing' || !room.players.includes(p.playerId)) throw new Error('BAD_ROOM_STATE');
        const points = Math.max(0, Math.trunc(Number(m.points || 0)));
        room.scores[p.playerId] = (room.scores[p.playerId] || 0) + points;
        save(); return send(ws, 'score', { roomId: room.roomId, scores: room.scores, target: room.target });
      }

      if (m.type === 'settleMatch') {
        const p = requireAuth(ws); const room = state.rooms[m.roomId];
        if (!room || room.ownerId !== p.playerId || room.status !== 'playing' || room.settled) throw new Error('BAD_ROOM_STATE');
        const winnerId = String(m.winnerId || '');
        if (!room.players.includes(winnerId) || (room.scores[winnerId] || 0) < room.target) throw new Error('INVALID_WINNER');
        state.players[winnerId].coins += room.pot; ledger('payout', winnerId, room.pot, { roomId: room.roomId });
        room.settled = true; room.status = 'finished'; room.winnerId = winnerId; save();
        return send(ws, 'matchFinished', { room, winner: publicPlayer(state.players[winnerId]) });
      }

      throw new Error('UNKNOWN_MESSAGE');
    } catch (e) { send(ws, 'error', { code: e.message || 'SERVER_ERROR' }); }
  });

  ws.on('close', () => { if (ws.playerId && state.players[ws.playerId]) { state.players[ws.playerId].online = false; save(); } });
});

server.listen(PORT, () => console.log(`Game Club server listening on :${PORT}`));

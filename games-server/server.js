'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { WebSocketServer } = require('ws');

const PORT = Number(process.env.PORT || 8080);
const DATA_FILE = path.join(__dirname, 'data.json');
const START_COINS = 1000;
let state = { players: {}, rooms: {}, ledger: [] };
try { state = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8')); } catch (_) {}
const sockets = new Map();

function save(){ const t=DATA_FILE+'.tmp'; fs.writeFileSync(t,JSON.stringify(state,null,2)); fs.renameSync(t,DATA_FILE); }
function id(p,b=4){ return p+crypto.randomBytes(b).toString('hex').toUpperCase(); }
function send(ws,type,data={}){ if(ws&&ws.readyState===1) ws.send(JSON.stringify({type,...data})); }
function broadcastRoom(room,type,data={}){ room.players.forEach(pid=>send(sockets.get(pid),type,data)); }
function pub(p){ return {playerId:p.playerId,name:p.name,coins:p.coins,role:p.role,online:!!p.online}; }
function auth(ws){ if(!ws.playerId||!state.players[ws.playerId]) throw Error('AUTH_REQUIRED'); return state.players[ws.playerId]; }
function ledger(kind,playerId,amount,meta={}){ state.ledger.push({id:id('L-',6),at:Date.now(),kind,playerId,amount,...meta}); }
function tile(a,b){ return {a,b}; }
function key(t){ return `${Math.min(t.a,t.b)}-${Math.max(t.a,t.b)}`; }
function shuffledDeck(){ const d=[]; for(let a=0;a<=6;a++) for(let b=a;b<=6;b++) d.push(tile(a,b)); for(let i=d.length-1;i>0;i--){ const j=Math.floor(Math.random()*(i+1)); [d[i],d[j]]=[d[j],d[i]]; } return d; }
function canPlay(t,g){ return g.board.length===0 || t.a===g.left || t.b===g.left || t.a===g.right || t.b===g.right; }
function orientForSide(t,side,g){ if(g.board.length===0) return {a:t.a,b:t.b}; if(side==='left'){ if(t.b===g.left) return {a:t.a,b:t.b}; if(t.a===g.left) return {a:t.b,b:t.a}; } else { if(t.a===g.right) return {a:t.a,b:t.b}; if(t.b===g.right) return {a:t.b,b:t.a}; } return null; }
function sendState(room,msg=''){ const g=room.gameState; for(const pid of room.players){ send(sockets.get(pid),'gameState',{roomId:room.roomId,target:room.target,stake:room.stake,status:room.status,message:msg,scores:room.scores,turn:g.turn,board:g.board,boneyardCount:g.boneyard.length,hand:g.hands[pid]||[],opponentCount:(g.hands[room.players.find(x=>x!==pid)]||[]).length}); } }
function beginRound(room){ const d=shuffledDeck(), p1=room.players[0], p2=room.players[1]; room.gameState={hands:{[p1]:d.splice(0,7),[p2]:d.splice(0,7)},boneyard:d,board:[],left:null,right:null,turn:room.roundStarter||p1,passes:0}; room.roundStarter=room.roundStarter===p1?p2:p1; room.status='playing'; sendState(room,'بدأت جولة جديدة'); }
function finishRound(room,winnerId,reason){ const g=room.gameState; const loserId=room.players.find(x=>x!==winnerId); const points=(g.hands[loserId]||[]).reduce((s,t)=>s+t.a+t.b,0); room.scores[winnerId]=(room.scores[winnerId]||0)+points; if(room.scores[winnerId]>=room.target){ state.players[winnerId].coins+=room.pot; ledger('payout',winnerId,room.pot,{roomId:room.roomId}); room.status='finished'; room.winnerId=winnerId; room.settled=true; broadcastRoom(room,'matchFinished',{roomId:room.roomId,winnerId,scores:room.scores,points,reason,coins:state.players[winnerId].coins}); save(); return; } broadcastRoom(room,'roundFinished',{roomId:room.roomId,winnerId,scores:room.scores,points,reason}); beginRound(room); save(); }

const server=http.createServer((req,res)=>{ if(req.url==='/health'){res.writeHead(200,{'content-type':'application/json'});return res.end(JSON.stringify({ok:true,players:Object.keys(state.players).length,rooms:Object.keys(state.rooms).length}));} res.writeHead(404);res.end('Not found'); });
const wss=new WebSocketServer({server});
wss.on('connection',ws=>{
 send(ws,'hello',{protocol:2});
 ws.on('message',raw=>{ try{
  const m=JSON.parse(raw.toString());
  if(m.type==='register'){
   let p=m.playerId&&state.players[m.playerId];
   if(p){ if(m.token!==p.token) throw Error('BAD_TOKEN'); }
   else { const playerId=id('P-',3); p={playerId,token:id('',16),name:String(m.name||'Player').slice(0,20),coins:START_COINS,role:'player',friends:[],online:false}; state.players[playerId]=p; ledger('welcome',playerId,START_COINS); }
   p.online=true; ws.playerId=p.playerId; sockets.set(p.playerId,ws); save(); return send(ws,'session',{player:pub(p),token:p.token});
  }
  const p=auth(ws);
  if(m.type==='createRoom'){
   const stake=Math.max(0,Math.trunc(Number(m.stake||0))); if(p.coins<stake) throw Error('INSUFFICIENT_COINS');
   const roomId=id('R-',3), target=Number(m.target)===151?151:101;
   state.rooms[roomId]={roomId,ownerId:p.playerId,target,stake,players:[p.playerId],scores:{[p.playerId]:0},pot:0,status:'lobby',settled:false}; save(); return send(ws,'room',{room:state.rooms[roomId]});
  }
  if(m.type==='joinRoom'){
   const room=state.rooms[String(m.roomId||'').toUpperCase()]; if(!room||room.status!=='lobby') throw Error('ROOM_NOT_FOUND'); if(room.players.length>=2&&!room.players.includes(p.playerId)) throw Error('ROOM_FULL'); if(p.coins<room.stake) throw Error('INSUFFICIENT_COINS');
   if(!room.players.includes(p.playerId)){room.players.push(p.playerId);room.scores[p.playerId]=0;} save(); broadcastRoom(room,'room',{room}); return;
  }
  if(m.type==='startMatch'){
   const room=state.rooms[m.roomId]; if(!room||room.ownerId!==p.playerId||room.players.length!==2||room.status!=='lobby') throw Error('BAD_ROOM_STATE');
   for(const pid of room.players){ if(state.players[pid].coins<room.stake) throw Error('INSUFFICIENT_COINS'); }
   for(const pid of room.players){state.players[pid].coins-=room.stake;room.pot+=room.stake;ledger('stake',pid,-room.stake,{roomId:room.roomId});}
   beginRound(room); save(); return;
  }
  if(m.type==='playTile'){
   const room=state.rooms[m.roomId], g=room&&room.gameState; if(!room||room.status!=='playing'||!g||g.turn!==p.playerId) throw Error('NOT_YOUR_TURN');
   const hand=g.hands[p.playerId]||[], idx=hand.findIndex(t=>key(t)===key(m.tile||{})); if(idx<0) throw Error('TILE_NOT_IN_HAND'); const t=hand[idx]; if(!canPlay(t,g)) throw Error('INVALID_TILE');
   let side=m.side==='left'?'left':'right'; if(g.board.length===0) side='right'; let o=orientForSide(t,side,g); if(!o){ side=side==='left'?'right':'left'; o=orientForSide(t,side,g); } if(!o) throw Error('INVALID_SIDE');
   hand.splice(idx,1); if(g.board.length===0){g.board.push(o);g.left=o.a;g.right=o.b;} else if(side==='left'){g.board.unshift(o);g.left=o.a;} else {g.board.push(o);g.right=o.b;} g.passes=0;
   if(hand.length===0){finishRound(room,p.playerId,'domino');return;} g.turn=room.players.find(x=>x!==p.playerId); sendState(room); save(); return;
  }
  if(m.type==='drawTile'){
   const room=state.rooms[m.roomId], g=room&&room.gameState; if(!room||room.status!=='playing'||g.turn!==p.playerId) throw Error('NOT_YOUR_TURN'); if((g.hands[p.playerId]||[]).some(t=>canPlay(t,g))) throw Error('PLAY_AVAILABLE');
   if(g.boneyard.length){g.hands[p.playerId].push(g.boneyard.pop());sendState(room,'تم سحب قطعة');save();return;}
   g.passes++; if(g.passes>=2){ const sums=room.players.map(pid=>({pid,sum:g.hands[pid].reduce((s,t)=>s+t.a+t.b,0)})).sort((a,b)=>a.sum-b.sum); finishRound(room,sums[0].pid,'blocked'); return; }
   g.turn=room.players.find(x=>x!==p.playerId);sendState(room,'تمرير الدور');save();return;
  }
  throw Error('UNKNOWN_MESSAGE');
 }catch(e){send(ws,'error',{code:e.message||'SERVER_ERROR'});} });
 ws.on('close',()=>{if(ws.playerId&&state.players[ws.playerId]){state.players[ws.playerId].online=false;sockets.delete(ws.playerId);save();}});
});
server.listen(PORT,()=>console.log(`Game Club server listening on :${PORT}`));

// Test receiver: logs every event from the phone to events.jsonl and prints a short line.
// GET /status shows counts and how long ago the last heartbeat arrived.
// Usage: TOKEN=some-secret node receiver/server.js   (listens on 0.0.0.0:8787)
const http = require("http");
const fs = require("fs");
const path = require("path");

const TOKEN = process.env.TOKEN || "";
const PORT = Number(process.env.PORT || 8787);
const LOG = path.join(__dirname, "events.jsonl");
if (!TOKEN) {
  console.error("Set TOKEN=... (the same value you enter in the app)");
  process.exit(1);
}

const counts = {};
let lastHeartbeat = null;

http
  .createServer((req, res) => {
    if (req.method === "GET" && req.url === "/status") {
      const age = lastHeartbeat ? Math.round((Date.now() - lastHeartbeat) / 1000) + "s ago" : "never";
      res.end(JSON.stringify({ counts, lastHeartbeat: age }, null, 2));
      return;
    }
    if (req.method !== "POST" || req.url !== "/ingest") return res.writeHead(404).end();
    if (req.headers["x-token"] !== TOKEN) return res.writeHead(401).end();
    let body = "";
    req.on("data", (c) => (body += c));
    req.on("end", () => {
      let ev;
      try {
        ev = JSON.parse(body);
      } catch {
        return res.writeHead(400).end();
      }
      ev.receivedAt = Date.now();
      fs.appendFileSync(LOG, JSON.stringify(ev) + "\n");
      counts[ev.type] = (counts[ev.type] || 0) + 1;
      if (ev.type === "heartbeat") lastHeartbeat = ev.createdAt;
      const lag = ev.postTime ? ` lag=${Math.round((ev.receivedAt - ev.postTime) / 1000)}s` : "";
      console.log(new Date().toISOString(), ev.type, ev.title || "", "|", ev.text || "", lag);
      res.end("ok");
    });
  })
  .listen(PORT, "0.0.0.0", () => console.log(`receiver on :${PORT}, logging to ${LOG}`));

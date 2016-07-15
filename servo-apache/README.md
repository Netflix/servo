The following metrics are collected by Servo from the Apache [mod_status](http://httpd.apache.org/docs/current/mod/mod_status.html) server-status machine readable page.  The location of the Apache server-status page is accepted as an argument to the constructor.  The typical form of this URL is as follows:

`http://your.server.name/server-status?auto`

The CPULoad metric provided by the server-status page has been blacklisted.

| Name  | Class | State | Type | Description |
|-------|-------|-------|------|-------------|
| BusyWorkers | ApacheStatusPoller | -- | GAUGE | number of workers serving requests |
| BytesPerReq | ApacheStatusPoller | -- | GAUGE | average number of bytes per request |
| BytesPerSec | ApacheStatusPoller | -- | GAUGE | average number of bytes served per second |
| IdleWorkers | ApacheStatusPoller | -- | GAUGE | number of idle workers |
| ReqPerSec | ApacheStatusPoller | -- | GAUGE | average number of requests per second |
| Scoreboard | ApacheStatusPoller | ClosingConnection | GAUGE | C |
| Scoreboard | ApacheStatusPoller | DnsLookup | GAUGE | D |
| Scoreboard | ApacheStatusPoller | GracefullyFinishing | GAUGE | G |
| Scoreboard | ApacheStatusPoller | IdleCleanupOfWorker | GAUGE | I |
| Scoreboard | ApacheStatusPoller | Keepalive | GAUGE | K |
| Scoreboard | ApacheStatusPoller | Logging | GAUGE | L |
| Scoreboard | ApacheStatusPoller | OpenSlotWithNoCurrentProcess | GAUGE | . |
| Scoreboard | ApacheStatusPoller | ReadingRequest | GAUGE | R |
| Scoreboard | ApacheStatusPoller | SendingReply | GAUGE | W |
| Scoreboard | ApacheStatusPoller | StartingUp | GAUGE | S |
| Scoreboard | ApacheStatusPoller | UnknownState | GAUGE | unknown symbol in the scoreboard |
| Scoreboard | ApacheStatusPoller | WaitingForConnection | GAUGE | _ |
| Total_Accesses | ApacheStatusPoller | -- | COUNTER | total number of accesses |
| Total_kBytes | ApacheStatusPoller | -- | COUNTER | total byte count served |
| Uptime | ApacheStatusPoller | -- | COUNTER | time the server has been running for |

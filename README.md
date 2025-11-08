# redis-lite

A minimal, educational Redis clone written in **Java 17**.  
Implements the [RESP protocol](https://redis.io/docs/reference/protocol-spec/) and a subset of Redis commands, including expiry options.  
Perfect for learning how Redis works under the hood — from sockets to serialization.

---

## 🚀 Features

| Step | Command(s) | Description |
|------|-------------|-------------|
| 1 | `PING`, `ECHO` | Basic RESP request/response handling |
| 3 | `SET`, `GET` | Core key–value storage |
| 5 | `SET` with `EX`, `PX`, `EXAT`, `PXAT` | Expiry support (lazy & optional sweeper) |
| Future | `DEL`, `EXISTS`, `INCR`, persistence | Planned extensions |

---


### Core Modules
- **RespParser / RespWriter** → Encode & decode RESP messages
- **CommandDispatcher** → Routes commands to logic implementations
- **DataStore** → Thread-safe `ConcurrentHashMap`-based key–value store
- **Value** → Container for bytes + expiry timestamp
- **RedisServer** → Multi-client TCP server

---

## ⚙️ Setup & Run

### Requirements
- Java 17 or newer
- Maven 3.8+
- (Optional) `redis-cli` or `nc` (netcat) for testing

### Build
```bash
mvn clean package
mvn exec:java -Dexec.mainClass=com.tejas.redis.server.RedisServer
OR
java -jar target/redis-lite-1.0-SNAPSHOT-jar-with-dependencies.jar

Try It Out
Using redis-cli

redis-cli -p 6379 ping
# → PONG

redis-cli -p 6379 set name John
# → OK

redis-cli -p 6379 get name
# → "John"

redis-cli -p 6379 set temp value PX 1500
# → OK
sleep 2
redis-cli -p 6379 get temp
# → (nil)

--------------------------------------------------------------------------------

Using nc (Netcat)

# SET foo bar PX 1500
printf '*5\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$2\r\nPX\r\n$4\r\n1500\r\n' | nc localhost 6379
# +OK

# GET foo
printf '*2\r\n$3\r\nGET\r\n$3\r\nfoo\r\n' | nc localhost 6379
# $3\r\nbar\r\n

```
### Concurrency Model
1) Thread per connection — Each client handled by its own thread (ThreadPoolExecutor)
2) Thread-safe data — ConcurrentHashMap used for safe concurrent SET/GET
3) Lazy expiry — Expired keys removed on access
4) Optional active sweeper — Background task can periodically remove expired keys

### Limitations
1) Thread-per-client model → not ideal for thousands of connections
2) No persistence (SAVE, AOF) yet
3) Partial SET options only (no NX, XX, GET, KEEPTTL)
4) RESP2 only (RESP3 not supported)
5) No clustering or pub/sub

### Developer Notes
1) CRLF (\r\n) is mandatory for RESP — missing it breaks parsing.
2) Each RESP object maps to a Java record/class (RespObject hierarchy).
3) Use xxd to debug byte-level traffic:
```bash
   printf '*1\r\n$4\r\nPING\r\n' | nc localhost 6379 | xxd -g1
```
4) Lazy expiry keeps memory low; enable sweeper for large datasets.

Authored by Tejas Ratnapagol

### Acknowledgments
Inspired by https://codingchallenges.fyi/challenges/challenge-redis/ Redis challenge.

#!/usr/bin/env bash
# Starts a Minecraft server, waits for "Done (", stops it, and checks the log.
# Usage: server-test.sh <server-dir> <expected-log-regex> -- <start command...>
# Optional: AFTER_START="cmd1;cmd2" runs those console commands once the server is up (Range Sight self-test),
# then waits for the expected line before stopping.
set -u
DIR="$1"; EXPECT="$2"; shift 3
cd "$DIR"
echo "eula=true" > eula.txt
rm -f console.in; mkfifo console.in
sleep infinity > console.in &
KEEP=$!
"$@" < console.in > server.log 2>&1 &
PID=$!
ok=0
for i in $(seq 1 900); do
  if grep -q 'Done (' server.log; then ok=1; break; fi
  if ! kill -0 "$PID" 2>/dev/null; then break; fi
  sleep 1
done
sleep 5
if [ -n "${AFTER_START:-}" ]; then
  IFS=';' read -ra CMDS <<< "$AFTER_START"
  for c in "${CMDS[@]}"; do echo "$c" > console.in; sleep 2; done
  # Wait (up to 3 minutes) for the expected line or a self-test failure, instead of a fixed sleep.
  for i in $(seq 1 180); do
    if grep -Eq "$EXPECT|self-test FAIL" server.log; then break; fi
    sleep 1
  done
fi
echo "stop" > console.in
for i in $(seq 1 120); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
kill "$PID" 2>/dev/null || true
kill "$KEEP" 2>/dev/null || true
echo "================ last 80 log lines ================"
tail -n 80 server.log
echo "==================================================="
if [ "$ok" != "1" ]; then echo "::error::Server did not finish starting"; exit 1; fi
if ! grep -Eq "$EXPECT" server.log; then echo "::error::Expected log line not found: $EXPECT"; exit 1; fi
if grep -Eq "Error reading pack metadata|load .*pack metadata" server.log; then echo "::error::A pack (mod resources) could not be read"; grep -m3 -A1 "pack metadata" server.log; exit 1; fi
if grep -iE "logiclib|rangesight|range sight" server.log | grep -Eiq "exception|error|could not load|couldn.t load|failed"; then
  echo "::error::Range Sight or LogicLib problem in the log:"; grep -iE "logiclib|rangesight|range sight" server.log | grep -Ei "exception|error|could not load|failed"; exit 1
fi
echo "PASS: server started, Range Sight loaded with no errors."

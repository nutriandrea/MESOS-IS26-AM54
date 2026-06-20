#!/bin/bash
# Test CLI disconnessione: simula 3 client + server
# Ogni client legge comandi da un gruppo { ... } | java

set +e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$PROJECT_DIR/target/IS26-AM54-1.0-SNAPSHOT.jar"
TMPDIR=$(mktemp -d /tmp/mesos-test-XXXXXX)
echo "=== Test directory: $TMPDIR ==="

cleanup() {
    echo "=== Pulizia ==="
    kill $SERVER_PID 2>/dev/null || true
    kill $ALICE_PID 2>/dev/null || true
    kill $BOB_PID 2>/dev/null || true
    kill $CHARLIE_PID 2>/dev/null || true
    sleep 1
    kill -9 $SERVER_PID 2>/dev/null || true
    kill -9 $ALICE_PID 2>/dev/null || true
    kill -9 $BOB_PID 2>/dev/null || true
    kill -9 $CHARLIE_PID 2>/dev/null || true
    rm -rf "$TMPDIR"
}
trap cleanup EXIT INT TERM

# --- 1. Avvia server ---
echo "=== Avvio server ==="
java -cp "$JAR" it.polimi.ingsw.am54.network.server.Server > "$TMPDIR/server.log" 2>&1 &
SERVER_PID=$!
sleep 3

# --- 2. Avvia client ---
echo "=== Avvio client ==="

# ALICE
{
    echo "Alice"; sleep 1
    echo "localhost"; sleep 1
    echo "1"; sleep 5
    echo "RED"; sleep 6
    echo "0"; sleep 30
} | java -cp "$JAR" it.polimi.ingsw.am54.MainClient > "$TMPDIR/alice-out.log" 2>&1 &
ALICE_PID=$!
sleep 1

# BOB
{
    echo "Bob"; sleep 1
    echo "localhost"; sleep 1
    echo "1"; sleep 9
    echo "YELLOW"; sleep 6
    echo "0"; sleep 30
} | java -cp "$JAR" it.polimi.ingsw.am54.MainClient > "$TMPDIR/bob-out.log" 2>&1 &
BOB_PID=$!
sleep 1

# CHARLIE
{
    echo "Charlie"; sleep 1
    echo "localhost"; sleep 1
    echo "1"; sleep 13
    echo "BLUE"; sleep 30
} | java -cp "$JAR" it.polimi.ingsw.am54.MainClient > "$TMPDIR/charlie-out.log" 2>&1 &
CHARLIE_PID=$!

echo "Client: ALICE=$ALICE_PID BOB=$BOB_PID CHARLIE=$CHARLIE_PID"

# --- 3. Attesa ---
echo "=== Attesa 20s ==="
sleep 20

# --- 4. Uccidi Charlie ---
echo "=== Uccido Charlie (PID=$CHARLIE_PID) ==="
kill $CHARLIE_PID 2>/dev/null || echo "(gia' terminato)"
sleep 5

# --- 5. Termina Alice e Bob ---
kill $ALICE_PID $BOB_PID 2>/dev/null
sleep 2
kill -9 $ALICE_PID $BOB_PID 2>/dev/null || true
sleep 1

# --- 6. Output ---
echo ""
echo "===== RISULTATI ====="
echo ""
echo "--- ALICE (ultime 50) ---"
tail -50 "$TMPDIR/alice-out.log" 2>/dev/null || echo "(nessun output)"
echo ""
echo "--- BOB (ultime 50) ---"
tail -50 "$TMPDIR/bob-out.log" 2>/dev/null || echo "(nessun output)"
echo ""
echo "--- CHARLIE (ultime 10) ---"
tail -10 "$TMPDIR/charlie-out.log" 2>/dev/null || echo "(nessun output)"
echo ""
echo "--- SERVER (ultime 20) ---"
tail -20 "$TMPDIR/server.log" 2>/dev/null || echo "(nessun output)"
echo ""
echo "=== Fine test ==="

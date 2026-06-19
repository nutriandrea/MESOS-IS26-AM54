#!/bin/bash
# ==============================================================
# TEST CLI: Partita 3 giocatori + disconnessione
# ==============================================================
set +e
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$PROJECT_DIR/target/IS26-AM54-1.0-SNAPSHOT.jar"
TMPDIR=$(mktemp -d /tmp/mesos-cli-test-XXXXXX)

echo "========================================="
echo " TEST CLI - Partita 3 giocatori"
echo "========================================="
echo "Directory test: $TMPDIR"
echo ""

cleanup() {
    echo ""
    echo "=== Pulizia processi ==="
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

# ==============================================================
# 1. AVVIA SERVER
# ==============================================================
echo "[1] Avvio server..."
java -cp "$JAR" it.polimi.ingsw.am54.network.server.Server > "$TMPDIR/server.log" 2>&1 &
SERVER_PID=$!
sleep 3

if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "ERRORE: Server non avviato!"
    cat "$TMPDIR/server.log"
    exit 1
fi
echo "[OK] Server avviato (PID=$SERVER_PID)"

# ==============================================================
# 2. AVVIA CLIENT
# ==============================================================
echo "[2] Avvio client..."

# ALICE (host)
# Timing: nickname(0) → ip(1) → protocol(2) → color(5) → START(20) → totem0(35)
{
    echo "Alice";  sleep 1
    echo "localhost"; sleep 1
    echo "1";      sleep 5
    echo "RED";    sleep 15
    echo "START";  sleep 20
    echo "0";      sleep 40
    echo "exit";   sleep 2
} | java -cp "$JAR" it.polimi.ingsw.am54.MainClient > "$TMPDIR/alice.log" 2>&1 &
ALICE_PID=$!
echo "  Alice (PID=$ALICE_PID)" ; sleep 1

# BOB
{
    echo "Bob";    sleep 1
    echo "localhost"; sleep 1
    echo "1";      sleep 7
    echo "YELLOW"; sleep 22
    echo "0";      sleep 40
    echo "exit";   sleep 2
} | java -cp "$JAR" it.polimi.ingsw.am54.MainClient > "$TMPDIR/bob.log" 2>&1 &
BOB_PID=$!
echo "  Bob (PID=$BOB_PID)"   ; sleep 1

# CHARLIE
{
    echo "Charlie"; sleep 1
    echo "localhost";  sleep 1
    echo "1";      sleep 9
    echo "BLUE";   sleep 30
    echo "0";      sleep 15
} | java -cp "$JAR" it.polimi.ingsw.am54.MainClient > "$TMPDIR/charlie.log" 2>&1 &
CHARLIE_PID=$!
echo "  Charlie (PID=$CHARLIE_PID)"

# ==============================================================
# 3. ATTESA + DISCONNESSIONE
# ==============================================================
echo ""
echo "[3] Attesa setup partita (45s)..."
sleep 45

echo "[4] Disconnessione Charlie (PID=$CHARLIE_PID)..."
kill $CHARLIE_PID 2>/dev/null
wait $CHARLIE_PID 2>/dev/null
echo "[OK] Charlie disconnesso"

echo "[5] Attesa post-disconnessione (15s)..."
sleep 15

# ==============================================================
# 4. TERMINA
# ==============================================================
echo "[6] Termine partita..."
kill $ALICE_PID $BOB_PID 2>/dev/null
sleep 2
kill -9 $ALICE_PID $BOB_PID $SERVER_PID 2>/dev/null || true
sleep 1

# ==============================================================
# 5. REPORT
# ==============================================================
echo ""
echo "========================================="
echo " REPORT TEST CLI"
echo "========================================="

check_crash() {
    local log=$1 name=$2
    if grep -qi "Exception\|NullPointer" "$log" 2>/dev/null; then
        if grep -q "ClassNotFoundException\|NoClassDefFound" "$log" 2>/dev/null; then
            echo "  ⚠️  $name: CLASSI MANCANTI (shade JAR incompleto)"
            return 0
        fi
        echo "  ❌ $name: CRASH ($(grep -c 'Exception\|NullPointer' $log) eccezioni)"
        grep -n "Exception\|NullPointer" "$log" 2>/dev/null | head -3
        return 1
    else
        echo "  ✅ $name: OK"
        return 0
    fi
}

echo ""
echo "--- Crash Check ---"
check_crash "$TMPDIR/server.log" "Server"
check_crash "$TMPDIR/alice.log" "Alice"
check_crash "$TMPDIR/bob.log"   "Bob"
check_crash "$TMPDIR/charlie.log" "Charlie"

echo ""
echo "--- Fasi Raggiunte ---"
grep -o "FASE PIAZZAMENTO TOTEM\|FASE DI PESCA\|LOBBY\|START\|VINCITORE\|BENVENUTO" "$TMPDIR/alice.log" 2>/dev/null | sort -u
grep -o "FASE PIAZZAMENTO TOTEM\|FASE DI PESCA\|LOBBY\|VINCITORE\|BENVENUTO" "$TMPDIR/bob.log" 2>/dev/null | sort -u
grep -o "FASE PIAZZAMENTO TOTEM\|FASE DI PESCA\|LOBBY\|VINCITORE\|BENVENUTO" "$TMPDIR/charlie.log" 2>/dev/null | sort -u

echo ""
echo "--- Alice (ultime 20) ---"
tail -20 "$TMPDIR/alice.log" 2>/dev/null || echo "(vuoto)"

echo ""
echo "--- Bob (ultime 20) ---"
tail -20 "$TMPDIR/bob.log" 2>/dev/null || echo "(vuoto)"

echo ""
echo "--- Charlie (ultime 15) ---"
tail -15 "$TMPDIR/charlie.log" 2>/dev/null || echo "(vuoto)"

echo ""
echo "--- Server (ultime 15) ---"
tail -15 "$TMPDIR/server.log" 2>/dev/null || echo "(vuoto)"

echo ""
echo "========================================="
echo " TEST COMPLETATO"
echo "========================================="

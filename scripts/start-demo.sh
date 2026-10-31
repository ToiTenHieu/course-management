#!/usr/bin/env bash
# Start the demo in the background on macOS/Linux (also works in Git Bash).
# Usage: ./scripts/start-demo.sh [--skip-build] [--ai] [--port 8080] [--timeout 90]
set -euo pipefail

skip_build=0
use_ai=0
port="${PORT:-8080}"
timeout=90
while [ $# -gt 0 ]; do
    case "$1" in
        --skip-build) skip_build=1 ;;
        --ai) use_ai=1 ;;
        --port) port="${2:?--port needs a value}"; shift ;;
        --timeout) timeout="${2:?--timeout needs a value}"; shift ;;
        -h|--help) sed -n '2,3p' "$0"; exit 0 ;;
        *) echo "Unknown option: $1" >&2; exit 2 ;;
    esac
    shift
done
case "$port" in ''|*[!0-9]*) echo 'PORT must be a number.' >&2; exit 2 ;; esac

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
run_dir="${DEMO_RUN_DIR:-$root/target/local-demo}"
mkdir -p "$run_dir"
pid_file="$run_dir/server.pid"
local_config="$root/target/local-demo/application-local.properties"
jar="$root/target/course_management-0.0.1-SNAPSHOT.jar"
url="http://127.0.0.1:$port"

# Java on Windows (Git Bash) needs Windows paths; elsewhere the path is unchanged.
native_path() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }
is_ready() { curl -fsS --max-time 2 "$url/api/auth/ready" 2>/dev/null | grep -q '"ready":true'; }
is_demo_pid() {
    kill -0 "$1" 2>/dev/null || return 1
    local cmd
    cmd="$(ps -p "$1" -o command= 2>/dev/null)" || return 0  # ps without -o: trust the PID file
    case "$cmd" in *course_management-0.0.1-SNAPSHOT.jar*) return 0 ;; *) return 1 ;; esac
}

if [ -f "$pid_file" ]; then
    old_pid="$(tr -d '[:space:]' < "$pid_file")"
    if [ -n "$old_pid" ] && is_demo_pid "$old_pid"; then
        if is_ready; then echo "Demo is already ready: $url"; exit 0; fi
        echo 'The recorded demo process is still starting or uses another port. Check its logs, or run scripts/stop-demo.sh.' >&2
        exit 1
    fi
    rm -f "$pid_file"
fi
if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
    echo "Port $port is already in use. Use --port or stop that application's server." >&2
    exit 1
fi

java_bin="java"
[ -n "${JAVA_HOME:-}" ] && java_bin="$JAVA_HOME/bin/java"
java_major="$("$java_bin" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
if [ -z "$java_major" ] || [ "$java_major" -lt 21 ]; then
    echo 'JDK 21 or newer is required (java -version). See docs/CHAY_TREN_MAC.md.' >&2
    exit 1
fi

if [ "$use_ai" -eq 1 ]; then
    if [ -z "${GEMINI_API_KEY:-}" ]; then
        read -rsp 'Dan GEMINI_API_KEY roi nhan Enter: ' GEMINI_API_KEY
        echo
    fi
    if [ -z "$GEMINI_API_KEY" ]; then echo 'GEMINI_API_KEY is empty.' >&2; exit 1; fi
    export GEMINI_API_KEY AI_PROVIDER=gemini
fi

if [ "$skip_build" -eq 0 ]; then
    sh ./mvnw -B -ntp -DskipTests package
fi
if [ ! -f "$jar" ]; then echo 'Build the project first (run without --skip-build).' >&2; exit 1; fi

args=(-jar "$(native_path "$jar")" --spring.profiles.active=demo "--server.port=$port")
if [ -f "$local_config" ]; then
    args+=("--spring.config.additional-location=file:$(native_path "$local_config")")
fi
nohup "$java_bin" "${args[@]}" > "$run_dir/server.log" 2> "$run_dir/server-error.log" < /dev/null &
server_pid=$!
echo "$server_pid" > "$pid_file"
echo "Starting demo (PID $server_pid), waiting for readiness..."

deadline=$(( $(date +%s) + timeout ))
while [ "$(date +%s)" -lt "$deadline" ]; do
    if ! kill -0 "$server_pid" 2>/dev/null; then
        rm -f "$pid_file"
        echo "Demo exited before becoming ready. Check $run_dir/server.log and server-error.log." >&2
        exit 1
    fi
    if is_ready; then
        echo "Demo ready (PID $server_pid): $url"
        echo "Logs: $run_dir/server.log"
        exit 0
    fi
    sleep 1
done
kill "$server_pid" 2>/dev/null || true
rm -f "$pid_file"
echo "Demo was not ready after ${timeout}s and was stopped. Check $run_dir/server.log, or use --timeout 180." >&2
exit 1

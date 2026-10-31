#!/usr/bin/env bash
# Stop the demo started by scripts/start-demo.sh.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
pid_file="${DEMO_RUN_DIR:-$root/target/local-demo}/server.pid"
if [ ! -f "$pid_file" ]; then echo 'No demo PID recorded'; exit 0; fi
demo_pid="$(tr -d '[:space:]' < "$pid_file")"
case "$demo_pid" in
    ''|*[!0-9]*) rm -f "$pid_file"; echo 'Removed an invalid demo PID record'; exit 0 ;;
esac
cmd="$(ps -p "$demo_pid" -o command= 2>/dev/null || true)"
if kill -0 "$demo_pid" 2>/dev/null && { [ -z "$cmd" ] || [[ "$cmd" == *course_management-0.0.1-SNAPSHOT.jar* ]]; }; then
    kill "$demo_pid"
    echo "Stopped demo PID $demo_pid"
else
    echo 'The recorded demo process is no longer running'
fi
rm -f "$pid_file"

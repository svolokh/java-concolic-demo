#!/bin/bash
set -e

cd /root/demo
cp /root/demo-code/Main.java .

schedule-shutdown() {
    sleep 300 # allow running a max of 5 minutes
    kill -s SIGINT $(ps -eo pid | sed 1d)
}
schedule-shutdown &

echo "Compiling and instrumenting..."
if ! make >/dev/null 2>builderr.txt; then
    echo "Error when compiling code: "
    cat builderr.txt
    exit
fi
python3 concolic.py config.json

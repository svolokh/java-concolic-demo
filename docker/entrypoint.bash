#!/bin/bash
set -e

cd /root/demo
cp /root/demo-code/Main.java .

schedule-shutdown() {
    sleep 300 # allow running a max of 5 minutes
    kill -s SIGINT $(ps -eo pid | sed 1d)
}
schedule-shutdown &

if [[ -f /root/demo-code/stopOnCrash ]]; then
    sed -i 's/"stopOnError": false/"stopOnError": true/g' /root/demo/config.json
fi

echo "Compiling and instrumenting..."
if ! make >/dev/null 2>builderr.txt; then
    echo "Error when compiling code: "
    cat builderr.txt
    exit
fi

if [[ -f /root/demo/concrete.txt ]]; then
    cat /root/demo/concrete.txt
fi

python3 concolic.py config.json

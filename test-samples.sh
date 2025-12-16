#!/bin/bash

cd "$(dirname "$0")"

CP="zmq-samples/build/libs/zmq-samples-0.1.jar:zmq/build/libs/zmq-0.1.jar:zmq-core/build/libs/zmq-core-0.1.jar"

run_sample() {
    local name=$1
    echo "=== $name ==="
    timeout 12s java --enable-native-access=ALL-UNNAMED -cp "$CP" "io.github.ulalax.zmq.samples.$name" 2>&1 | head -20
    local exit_code=$?
    if [ $exit_code -eq 0 ]; then
        echo -e "\033[32m✓ PASSED\033[0m"
    elif [ $exit_code -eq 124 ]; then
        echo -e "\033[33m⏱ TIMEOUT (expected for long-running samples)\033[0m"
    else
        echo -e "\033[31m✗ FAILED (exit: $exit_code)\033[0m"
    fi
    echo ""
    sleep 2
}

echo "========================================"
echo "  JVM-ZMQ Sample Test"
echo "========================================"
echo ""

# Build first
./gradlew :zmq-samples:build -q
echo "Build complete"
echo ""

run_sample "ReqRepSample"
run_sample "PubSubSample"
run_sample "PushPullSample"
run_sample "PairSample"
run_sample "RouterDealerSample"
run_sample "RouterToRouterSample"
run_sample "ProxySample"
run_sample "SteerableProxySample"
run_sample "PollerSample"
run_sample "MonitorSample"
run_sample "MultipartSample"
run_sample "RouterBenchmarkSample"
run_sample "CurveSecuritySample"

echo "========================================"
echo "  All samples completed!"
echo "========================================"

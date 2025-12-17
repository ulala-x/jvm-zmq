#!/bin/bash

# Quick benchmark runner for testing
# Full benchmarks can take 10-30 minutes

echo "Running Quick ZMQ Benchmark..."
echo "================================"
echo ""

# Quick settings for fast feedback
./gradlew :zmq:jmh \
  -Pjmh.warmupIterations=1 \
  -Pjmh.iterations=2 \
  -Pjmh.fork=1 \
  -Pjmh.params="messageSize=1500"

echo ""
echo "Results saved to: zmq/build/reports/jmh/results.json"
echo ""
echo "Formatting results in BenchmarkDotNet style..."
echo "=============================================="
python3 zmq/scripts/format_jmh_dotnet_style.py

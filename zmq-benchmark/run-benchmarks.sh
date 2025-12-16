#!/bin/bash

# JMH Benchmark Runner Script for ZMQ

echo "=========================================="
echo "ZMQ JMH Benchmark Runner"
echo "=========================================="
echo ""

# Function to display usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  all                   Run all benchmarks (default)"
    echo "  throughput            Run throughput benchmarks only"
    echo "  latency               Run latency benchmarks only"
    echo "  list                  List available benchmarks"
    echo "  help                  Display this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Run all benchmarks"
    echo "  $0 throughput         # Run throughput benchmarks only"
    echo "  $0 latency            # Run latency benchmarks only"
    echo ""
}

# Navigate to project root
cd "$(dirname "$0")/.." || exit 1

# Parse arguments
BENCHMARK_TYPE="${1:-all}"

case "$BENCHMARK_TYPE" in
    all)
        echo "Running all benchmarks..."
        ./gradlew :zmq-benchmark:jmh
        ;;
    throughput)
        echo "Running throughput benchmarks..."
        ./gradlew :zmq-benchmark:jmh -Pjmh.includes='.*Throughput.*'
        ;;
    latency)
        echo "Running latency benchmarks..."
        ./gradlew :zmq-benchmark:jmh -Pjmh.includes='.*Latency.*'
        ;;
    list)
        echo "Available benchmarks:"
        echo ""
        echo "Throughput Benchmarks:"
        echo "  - ThroughputBenchmark.pushPullThroughput"
        echo "  - ThroughputBenchmark.pubSubThroughput"
        echo ""
        echo "Latency Benchmarks:"
        echo "  - LatencyBenchmark.reqRepLatency"
        echo "  - LatencyBenchmark.dealerRouterLatency"
        echo ""
        echo "Use './gradlew :zmq-benchmark:jmh -Pjmh.includes=<pattern>' to run specific benchmarks"
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo "Error: Unknown option '$BENCHMARK_TYPE'"
        echo ""
        usage
        exit 1
        ;;
esac

echo ""
echo "Benchmark execution completed!"
echo "Results are saved in: zmq-benchmark/build/reports/jmh/results.json"

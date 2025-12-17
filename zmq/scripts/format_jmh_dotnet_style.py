#!/usr/bin/env python3
"""
Format JMH benchmark results in .NET BenchmarkDotNet style.
Matches the format from Net.Zmq.Benchmarks-report-github.md
"""
import json
import sys

def format_size(bytes_val):
    """Format bytes to human readable size"""
    if bytes_val < 1024:
        return f"{bytes_val:.2f} B"
    elif bytes_val < 1024 * 1024:
        return f"{bytes_val/1024:.2f} KB"
    elif bytes_val < 1024 * 1024 * 1024:
        return f"{bytes_val/(1024*1024):.2f} MB"
    else:
        return f"{bytes_val/(1024*1024*1024):.2f} GB"

def format_throughput(msg_size, msg_per_sec):
    """Format data throughput like .NET: Mbps, Gbps, or GB/s"""
    bits_per_sec = msg_size * 8 * msg_per_sec

    if msg_size <= 1500:  # Small messages: use bits
        if bits_per_sec >= 1e9:
            return f"{bits_per_sec/1e9:.2f} Gbps"
        else:
            return f"{bits_per_sec/1e6:.2f} Mbps"
    else:  # Large messages: use bytes
        bytes_per_sec = msg_size * msg_per_sec
        if bytes_per_sec >= 1e9:
            return f"{bytes_per_sec/1e9:.2f} GB/s"
        else:
            return f"{bytes_per_sec/1e6:.2f} MB/s"

def format_messages_per_sec(count):
    """Format message rate like .NET: 3.07M, 908.96K, etc."""
    if count >= 1e6:
        return f"{count/1e6:.2f}M"
    elif count >= 1e3:
        return f"{count/1e3:.2f}K"
    else:
        return f"{count:.2f}"

def process_benchmark_type(data, benchmark_type):
    """Process and display results for a specific benchmark type"""

    # Method display names (matching .NET)
    method_names = {
        # MemoryStrategyBenchmark
        'ByteArray_SendRecv': 'ByteArray_SendRecv',
        'ArrayPool_SendRecv': 'ArrayPool_SendRecv',
        'Message_SendRecv': 'Message_SendRecv',
        'MessageZeroCopy_SendRecv': 'MessageZeroCopy_SendRecv',
        # ReceiveModeBenchmark
        'BLOCKING': 'Blocking_RouterToRouter',
        'NON_BLOCKING': 'NonBlocking_RouterToRouter',
        'POLLER': 'Poller_RouterToRouter'
    }

    # Filter benchmarks by type
    filtered_data = []
    for bench in data:
        benchmark_name = bench['benchmark']
        if benchmark_type == 'memory' and 'MemoryStrategyBenchmark' in benchmark_name:
            filtered_data.append(bench)
        elif benchmark_type == 'receive' and 'ReceiveModeBenchmark' in benchmark_name:
            filtered_data.append(bench)

    if not filtered_data:
        return

    # Parse results
    results = {}
    for bench in filtered_data:
        benchmark_name = bench['benchmark']

        # Extract method name
        if benchmark_type == 'memory':
            name = benchmark_name.split('.')[-1]
        elif benchmark_type == 'receive':
            # ReceiveMode uses mode parameter instead of method name
            mode = bench['params'].get('mode', 'UNKNOWN')
            name = mode
        else:
            name = benchmark_name.split('.')[-1]

        msg_size = int(bench['params']['messageSize'])
        msg_count = int(bench['params']['messageCount'])

        score = bench['primaryMetric']['score']  # ops/s
        error = bench['primaryMetric']['scoreError']

        # Calculate StdDev from raw data
        raw_data = bench['primaryMetric']['rawData'][0]
        mean = sum(raw_data) / len(raw_data)
        variance = sum((x - mean) ** 2 for x in raw_data) / len(raw_data)
        stddev = variance ** 0.5

        # GC metrics
        gc_alloc_rate = bench.get('secondaryMetrics', {}).get('gc.alloc.rate', {}).get('score', 0)
        gc_alloc_norm = bench.get('secondaryMetrics', {}).get('gc.alloc.rate.norm', {}).get('score', 0)
        gc_count = bench.get('secondaryMetrics', {}).get('gc.count', {}).get('score', 0)

        key = (msg_size, name)
        results[key] = {
            'name': name,
            'msg_size': msg_size,
            'msg_count': msg_count,
            'mean': 1000.0 / score,  # Convert ops/s to ms (each op = 10K messages)
            'error': 1000.0 * error / (score * score),  # Error propagation
            'stddev': stddev * 1000.0 / (score * score),
            'score': score,
            'gc_alloc_rate': gc_alloc_rate,
            'gc_alloc_norm': gc_alloc_norm,
            'gc_count': gc_count
        }

    # Group by message size
    msg_sizes = sorted(set(k[0] for k in results.keys()))

    # Calculate baseline for each size (first method is baseline)
    baselines = {}
    if benchmark_type == 'memory':
        baseline_name = 'ByteArray_SendRecv'
    elif benchmark_type == 'receive':
        baseline_name = 'BLOCKING'
    else:
        baseline_name = None

    for size in msg_sizes:
        if baseline_name:
            baseline_key = (size, baseline_name)
            if baseline_key in results:
                baselines[size] = results[baseline_key]

    # Print section header
    if benchmark_type == 'memory':
        print("\n## Memory Strategy Benchmarks\n")
    else:
        print("\n## Receive Mode Benchmarks\n")

    # Print table header
    print("| Method                   | MessageSize | MessageCount | Score (ops/s) | Mean       | Error       | StdDev    | Ratio | RatioSD | Latency   | Messages/sec | Data Throughput | Gen0      | Allocated    | Alloc Ratio |")
    print("|------------------------- |------------ |------------- |--------------:|-----------:|------------:|----------:|------:|--------:|----------:|-------------:|----------------:|----------:|-------------:|------------:|")

    # Print results grouped by message size
    if benchmark_type == 'memory':
        method_order = ['ByteArray_SendRecv', 'ArrayPool_SendRecv', 'Message_SendRecv', 'MessageZeroCopy_SendRecv']
    elif benchmark_type == 'receive':
        method_order = ['BLOCKING', 'NON_BLOCKING', 'POLLER']
    else:
        # Auto-detect from results
        method_order = sorted(set(k[1] for k in results.keys()))

    for size_idx, size in enumerate(msg_sizes):
        baseline = baselines.get(size)

        for method_idx, method in enumerate(method_order):
            key = (size, method)
            if key not in results:
                continue

            r = results[key]
            display_name = method_names.get(method, method)

            # Calculate metrics
            mean_ms = r['mean']
            error_ms = r['error']
            stddev_ms = r['stddev']

            # Ratio to baseline
            is_baseline = (method == baseline_name) if baseline_name else False
            if baseline and not is_baseline:
                ratio = r['mean'] / baseline['mean']
                ratio_sd = ratio * ((r['stddev']/r['mean'])**2 + (baseline['stddev']/baseline['mean'])**2)**0.5
            else:
                ratio = 1.0
                ratio_sd = 0.01 if baseline else 0.00

            # Latency per message
            latency_ns = mean_ms * 1e6 / r['msg_count']
            if latency_ns < 1000:
                latency_str = f"{latency_ns:.2f} ns"
            else:
                latency_str = f"{latency_ns/1000:.2f} μs"

            # Messages per second
            msg_per_sec = r['msg_count'] * r['score']
            msg_per_sec_str = format_messages_per_sec(msg_per_sec)

            # Data throughput
            throughput_str = format_throughput(r['msg_size'], msg_per_sec)

            # GC Gen0 (collections per op)
            gen0_str = f"{r['gc_count']:.4f}" if r['gc_count'] > 0 else "-"

            # Allocated memory
            allocated_kb = r['gc_alloc_norm'] / 1024
            allocated_str = format_size(r['gc_alloc_norm'])

            # Alloc ratio
            if baseline and not is_baseline:
                alloc_ratio = r['gc_alloc_norm'] / baseline['gc_alloc_norm'] if baseline['gc_alloc_norm'] > 0 else 0
            else:
                alloc_ratio = 1.0

            # JMH Score (ops/s)
            score_ops = r['score']

            # Print row
            print(f"| {display_name:24} | {r['msg_size']:<11} | {r['msg_count']:<12} | {score_ops:>12.2f} | {mean_ms:>8.3f} ms | {error_ms:>9.4f} ms | {stddev_ms:>8.4f} ms | {ratio:>5.2f} | {ratio_sd:>6.2f} | {latency_str:>9} | {msg_per_sec_str:>12} | {throughput_str:>15} | {gen0_str:>9} | {allocated_str:>12} | {alloc_ratio:>10.3f} |")

        # Add separator between message sizes (except after last)
        if size_idx < len(msg_sizes) - 1:
            print("|                          |             |              |               |            |             |           |       |         |           |              |                 |           |              |             |")

    print()

with open('zmq/build/reports/jmh/results.json', 'r') as f:
    data = json.load(f)

# Print header
print("\n```")
print("\nBenchmarkDotNet-style JMH Results")
print("JMH v1.37, Ubuntu 24.04 LTS")
print("Java HotSpot(TM) 64-Bit Server VM, JDK 22.0.2")
print()
print("Job=Default  Runtime=Java 22  Platform=X64")
print("Warmup=3 iterations (2s each)  Measurement=5 iterations (5s each)")
print()
print("```")

# Process both benchmark types
process_benchmark_type(data, 'memory')
process_benchmark_type(data, 'receive')

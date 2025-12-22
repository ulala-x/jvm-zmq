#!/bin/bash

# ZMQ 벤치마크 실행 스크립트
# 사용법:
#   ./bench.sh all      - 모든 벤치마크 실행
#   ./bench.sh buffer   - MessageBufferStrategyBenchmark만 실행
#   ./bench.sh receive  - ReceiveModeBenchmark만 실행

set -e

show_usage() {
    echo "사용법: $0 <옵션>"
    echo ""
    echo "옵션:"
    echo "  all     - 모든 벤치마크 실행 (MessageBufferStrategy + ReceiveMode, ~30분)"
    echo "  buffer  - MessageBufferStrategyBenchmark만 실행 (~20분)"
    echo "  receive - ReceiveModeBenchmark만 실행 (~10분)"
    echo ""
    echo "예시:"
    echo "  $0 all       # 전체 벤치마크"
    echo "  $0 buffer    # 메시지 버퍼 전략 벤치마크만"
    echo "  $0 receive   # Receive 모드 벤치마크만"
}

format_results() {
    echo ""
    echo "=================================="
    echo "결과 포맷팅 (.NET BenchmarkDotNet 스타일)"
    echo "=================================="
    python3 zmq/scripts/format_jmh_dotnet_style.py
    echo ""
    echo "✓ 완료!"
    echo "결과 파일: zmq/build/reports/jmh/results.json"
}

if [ $# -eq 0 ]; then
    show_usage
    exit 1
fi

case "$1" in
    all)
        echo "=================================="
        echo "전체 벤치마크 실행"
        echo "=================================="
        echo "- MessageBufferStrategyBenchmark"
        echo "- ReceiveModeBenchmark"
        echo "예상 시간: ~30분"
        echo ""
        read -p "계속하시겠습니까? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "취소됨"
            exit 1
        fi

        echo ""
        echo "벤치마크 실행 중..."
        ./gradlew :zmq:jmh

        format_results
        ;;

    buffer)
        echo "=================================="
        echo "MessageBufferStrategyBenchmark 실행"
        echo "=================================="
        echo "테스트: ByteArray, ArrayPool, Message, Zero-Copy"
        echo "메시지 크기: 64, 512, 1024, 65536 bytes"
        echo "예상 시간: ~20분"
        echo ""

        ./gradlew :zmq:jmh -PjmhIncludes=".*MessageBufferStrategyBenchmark.*"

        format_results
        ;;

    receive)
        echo "=================================="
        echo "ReceiveModeBenchmark 실행"
        echo "=================================="
        echo "테스트: Byte Array, Message 수신 모드"
        echo "예상 시간: ~10분"
        echo ""

        ./gradlew :zmq:jmh -PjmhIncludes=".*ReceiveModeBenchmark.*"

        format_results
        ;;

    *)
        echo "오류: 알 수 없는 옵션 '$1'"
        echo ""
        show_usage
        exit 1
        ;;
esac

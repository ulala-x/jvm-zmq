# ZMQ Performance Benchmarks

이 디렉토리는 jvm-zmq의 성능 벤치마크 테스트를 포함합니다.

## 벤치마크 테스트

### 1. ThroughputBenchmarkTest.java
메시지 처리량(Throughput) 벤치마크 테스트입니다.

**측정 항목:**
- PUSH-PULL 패턴의 초당 메시지 수 (messages/sec)
- 대역폭 (MB/sec)
- 메시지당 레이턴시 (microseconds)

**테스트 케이스:**
- 1-byte 메시지
- 128-byte 메시지
- 1KB 메시지
- 64KB 메시지

**추가 패턴:**
- PUB-SUB 패턴 처리량 (1KB 메시지)

### 2. LatencyBenchmarkTest.java
라운드 트립 지연 시간(Latency) 벤치마크 테스트입니다.

**측정 항목:**
- REQ-REP 패턴의 round-trip 지연 시간
- 평균(Average), 최소(Min), 최대(Max), 중앙값(Median), P99 지연 시간

**테스트 케이스:**
- 1-byte 메시지
- 128-byte 메시지
- 1KB 메시지
- 64KB 메시지

**추가 패턴:**
- DEALER-ROUTER 패턴 지연 시간 (1KB 메시지)

## 실행 방법

### 모든 벤치마크 실행
```bash
./gradlew :zmq:test --tests "*Benchmark*" -Dtest.profile=benchmark
```

### 특정 벤치마크 실행

#### Throughput 벤치마크만 실행
```bash
./gradlew :zmq:test --tests "*ThroughputBenchmark*"
```

#### Latency 벤치마크만 실행
```bash
./gradlew :zmq:test --tests "*LatencyBenchmark*"
```

#### 특정 메시지 크기만 테스트
```bash
# 1KB 메시지 throughput 테스트
./gradlew :zmq:test --tests "*ThroughputBenchmark*1KB*"

# 128-byte 메시지 latency 테스트
./gradlew :zmq:test --tests "*LatencyBenchmark*128*"
```

## 테스트 특징

### @Disabled 어노테이션
모든 벤치마크 테스트는 기본적으로 `@Disabled`로 비활성화되어 있습니다.
- 일반 CI/CD 파이프라인에서 자동으로 실행되지 않음
- 성능 측정이 필요할 때만 수동으로 실행
- 위의 명령어로 명시적으로 실행 가능

### @Tag("benchmark")
모든 벤치마크는 `@Tag("benchmark")` 태그가 지정되어 있습니다.
- 일반 테스트와 분리하여 관리
- 필요 시 태그 기반 필터링 가능

### Warm-up 단계
모든 벤치마크는 실제 측정 전에 워밍업 단계를 포함합니다.
- JIT 컴파일 최적화 대기
- 네트워크 연결 안정화
- 더 정확한 성능 측정

## 출력 예시

### Throughput 벤치마크 출력
```
========================================
THROUGHPUT BENCHMARK - Message Size: 1KB
========================================
Warming up...
Running benchmark...

Results:
  Message Size:        1KB (1024 bytes)
  Messages Sent:       10,000
  Throughput:          1,234,567 msg/sec
  Bandwidth:           1234.57 MB/sec
  Per-message Latency: 0.81 μs
========================================
```

### Latency 벤치마크 출력
```
========================================
LATENCY BENCHMARK - Message Size: 1KB
========================================
Warming up...
Running benchmark...

Results:
  Message Size:        1KB (1024 bytes)
  Iterations:          1,000
  Average Latency:     45.23 μs
  Median Latency:      43.12 μs
  Min Latency:         38.45 μs
  Max Latency:         123.67 μs
  P99 Latency:         89.34 μs

  Round-trips/sec:     22,109
========================================
```

## 벤치마크 설정

### 기본 설정
- **Warmup 횟수**: 3회
- **Warmup 메시지 수**: 1,000개 (Throughput), 100회 (Latency)
- **벤치마크 메시지 수**: 10,000개 (Throughput), 1,000회 (Latency)
- **타임아웃**: 30초

### 커스터마이징
테스트 파일 내부의 상수를 수정하여 설정 변경 가능:

**ThroughputBenchmarkTest.java:**
```java
private static final int WARMUP_MESSAGE_COUNT = 1000;
private static final int BENCHMARK_MESSAGE_COUNT = 10000;
private static final int WARMUP_ITERATIONS = 3;
```

**LatencyBenchmarkTest.java:**
```java
private static final int WARMUP_ITERATIONS = 100;
private static final int BENCHMARK_ITERATIONS = 1000;
private static final int WARMUP_ROUNDS = 3;
```

## 참고 사항

### 성능 측정 시 주의사항
1. **시스템 리소스**: 다른 프로세스가 CPU를 많이 사용하지 않는 환경에서 실행
2. **네트워크**: 로컬 IPC 또는 TCP 루프백 사용으로 네트워크 오버헤드 최소화
3. **반복 실행**: 안정적인 결과를 위해 여러 번 실행 후 평균 사용
4. **JVM 워밍업**: 첫 실행 결과는 JIT 최적화 전이므로 참고만 할 것

### netzmq 벤치마크와의 비교
이 벤치마크는 .NET의 netzmq 프로젝트 벤치마크를 참조하여 작성되었습니다:
- 유사한 테스트 시나리오 (PUSH-PULL, REQ-REP)
- 동일한 메시지 크기 범위
- 비슷한 측정 지표 (throughput, latency, percentiles)

단, netzmq는 BenchmarkDotNet을 사용하는 반면, jvm-zmq는 JUnit 5를 사용합니다.

## 트러블슈팅

### 테스트 타임아웃
만약 테스트가 타임아웃되면:
1. 메시지 수를 줄이거나
2. 타임아웃 시간을 늘리거나
3. 시스템 리소스를 확인하세요

### 일관성 없는 결과
1. 다른 프로세스 종료
2. 시스템 부하 확인
3. 여러 번 실행하여 평균 계산
4. Warmup 횟수 증가

## 라이선스
MIT License - jvm-zmq 프로젝트와 동일

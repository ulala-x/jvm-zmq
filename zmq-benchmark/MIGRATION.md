# 벤치마크 모듈 마이그레이션 가이드

## 변경 사항 요약

### 이전 구조 (JUnit 기반)
```
zmq/src/test/java/io/github/ulalax/zmq/
├── ThroughputBenchmarkTest.java
└── LatencyBenchmarkTest.java
```

- JUnit 5 테스트로 작성
- `@Disabled` 어노테이션으로 CI에서 제외
- 수동 타이밍 측정 및 통계 계산
- 테스트 프레임워크에 의존

### 현재 구조 (JMH 기반)
```
zmq-benchmark/
├── src/jmh/java/io/github/ulalax/zmq/benchmark/
│   ├── ThroughputBenchmark.java
│   └── LatencyBenchmark.java
├── build.gradle.kts
├── README.md
├── MIGRATION.md
├── run-benchmarks.sh
└── .gitignore
```

- 전용 `zmq-benchmark` 모듈로 분리
- JMH (Java Microbenchmark Harness) 사용
- 자동화된 통계 및 프로파일링
- 표준 벤치마크 프레임워크 사용

## 주요 개선사항

### 1. 전문적인 벤치마크 프레임워크
- **JMH**: OpenJDK 공식 마이크로벤치마크 하네스
- JIT 컴파일러 최적화 고려
- Dead code elimination 방지
- 자동 워밍업 및 측정

### 2. 풍부한 통계 정보
JMH가 자동으로 제공하는 통계:
- Average (평균)
- Min/Max (최소/최대)
- Percentiles (p50, p90, p95, p99, p99.9, p99.99)
- Standard deviation (표준편차)
- Confidence intervals (신뢰구간)

### 3. 프로파일링 지원
```bash
# GC 통계
./gradlew :zmq-benchmark:jmh -Pjmh.profilers=gc

# CPU 프로파일링
./gradlew :zmq-benchmark:jmh -Pjmh.profilers=stack

# Linux perf 통합 (Linux만 해당)
./gradlew :zmq-benchmark:jmh -Pjmh.profilers=perfnorm
```

### 4. 모듈 분리의 이점
- CI/CD에서 일반 테스트와 벤치마크 분리
- 벤치마크 실행 시간이 빌드 시간에 영향 없음
- 독립적인 의존성 관리
- Maven 배포에서 자동 제외

## 벤치마크 비교

### Throughput 벤치마크

**이전 (JUnit):**
```java
@Test
@DisplayName("Should measure throughput with 1KB messages")
void should_Measure_Throughput_With_1KB_Messages() throws Exception {
    runThroughputBenchmark(1024, "1KB");
}
```

**현재 (JMH):**
```java
@Benchmark
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public void pushPullThroughput(PushPullState state, Blackhole blackhole) {
    state.pusher.send(state.messageData, SendFlags.NONE);
    blackhole.consume(state.messageData);
}
```

### Latency 벤치마크

**이전 (JUnit):**
```java
@Test
@DisplayName("Should measure latency with 1-byte messages")
void should_Measure_Latency_With_1_Byte_Messages() throws Exception {
    runLatencyBenchmark(1, "1B");
}
```

**현재 (JMH):**
```java
@Benchmark
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public void reqRepLatency(ReqRepState state, Blackhole blackhole) {
    state.client.send(state.requestData, SendFlags.NONE);
    int received = state.client.recv(state.receiveBuffer, RecvFlags.NONE);
    blackhole.consume(received);
}
```

## 실행 방법 차이

### 이전 방법
```bash
# JUnit 테스트로 실행 (비활성화 상태로는 실행 불가)
./gradlew test --tests "*ThroughputBenchmark*"
```

### 현재 방법
```bash
# 모든 벤치마크 실행
./gradlew :zmq-benchmark:jmh

# 특정 벤치마크만 실행
./gradlew :zmq-benchmark:jmh -Pjmh.includes='.*Throughput.*'

# 편의 스크립트 사용
cd zmq-benchmark
./run-benchmarks.sh throughput
```

## 파라미터화

### 이전 방식
여러 테스트 메서드로 분리:
```java
void should_Measure_Throughput_With_1_Byte_Messages()
void should_Measure_Throughput_With_128_Byte_Messages()
void should_Measure_Throughput_With_1KB_Messages()
```

### 현재 방식
`@Param` 어노테이션으로 자동 파라미터화:
```java
@State(Scope.Thread)
public static class PushPullState {
    @Param({"1", "128", "1024", "65536"})
    int messageSize;
    // ...
}
```

## 결과 형식

### 이전 (콘솔 출력)
```
========================================
THROUGHPUT BENCHMARK - Message Size: 1KB
========================================
Results:
  Message Size:        1KB (1024 bytes)
  Messages Sent:       10,000
  Throughput:          500,000 msg/sec
  Bandwidth:           488.28 MB/sec
```

### 현재 (JMH 표준 출력 + JSON)
```
Benchmark                              (messageSize)   Mode  Cnt      Score     Error  Units
ThroughputBenchmark.pushPullThroughput             1  thrpt    5  500000.123 ± 1234.567  ops/s
ThroughputBenchmark.pushPullThroughput           128  thrpt    5  450000.456 ± 2345.678  ops/s
ThroughputBenchmark.pushPullThroughput          1024  thrpt    5  400000.789 ± 3456.789  ops/s
```

결과는 JSON 형식으로도 저장됨:
- 위치: `zmq-benchmark/build/reports/jmh/results.json`
- 다른 도구와 통합 가능
- 시계열 분석 가능

## 마이그레이션 체크리스트

- [x] `zmq-benchmark` 모듈 생성
- [x] JMH 플러그인 및 의존성 설정
- [x] ThroughputBenchmark JMH 변환
  - [x] PUSH-PULL 패턴
  - [x] PUB-SUB 패턴
- [x] LatencyBenchmark JMH 변환
  - [x] REQ-REP 패턴
  - [x] DEALER-ROUTER 패턴
- [x] `settings.gradle.kts`에 모듈 추가
- [x] 기존 JUnit 벤치마크 파일 삭제
- [x] README.md 작성
- [x] 실행 스크립트 작성
- [x] .gitignore 설정

## 추가 개선 가능 사항

### 1. 더 많은 벤치마크 추가
- ROUTER-DEALER 패턴
- XPUB-XSUB 패턴
- Multipart 메시지 벤치마크
- Socket options 영향도 측정

### 2. CI/CD 통합
```yaml
# GitHub Actions 예시
- name: Run benchmarks
  run: ./gradlew :zmq-benchmark:jmh

- name: Archive benchmark results
  uses: actions/upload-artifact@v3
  with:
    name: jmh-results
    path: zmq-benchmark/build/reports/jmh/results.json
```

### 3. 벤치마크 결과 추적
- 커밋별 성능 변화 추적
- 성능 회귀 감지
- 벤치마크 결과 시각화

### 4. 다양한 JVM 벤치마크
```kotlin
jmh {
    fork.set(3)
    jvmArgs.addAll(listOf(
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+UseG1GC",
        "-Xms2g",
        "-Xmx2g"
    ))
}
```

## 문제 해결

### 벤치마크가 실행되지 않을 때
```bash
# JMH JAR 재생성
./gradlew :zmq-benchmark:clean :zmq-benchmark:jmhJar

# 캐시 정리
./gradlew clean --no-build-cache
```

### 결과가 불안정할 때
- Warmup 반복 횟수 증가: `-Pjmh.warmupIterations=10`
- Measurement 반복 횟수 증가: `-Pjmh.iterations=10`
- Fork 수 증가: `-Pjmh.fork=5`

### 네이티브 접근 오류
```
ERROR: Unable to access native library
```

해결: `build.gradle.kts`에 JVM 인자가 올바르게 설정되어 있는지 확인
```kotlin
jmh {
    jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}
```

## 참고 자료

- [JMH 공식 문서](https://github.com/openjdk/jmh)
- [JMH 샘플](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples)
- [JMH Gradle Plugin](https://github.com/melix/jmh-gradle-plugin)
- [JMH 베스트 프랙티스](https://shipilev.net/blog/2014/nanotrusting-nanotime/)

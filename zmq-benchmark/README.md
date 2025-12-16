# ZMQ Benchmark Module

JMH (Java Microbenchmark Harness) 기반 ZeroMQ Java 바인딩 성능 벤치마크 모듈입니다.

## 개요

이 모듈은 ZeroMQ의 다양한 메시징 패턴에 대한 성능 벤치마크를 제공합니다:

- **Throughput Benchmarks**: 메시지 처리량 측정 (messages/sec, MB/sec)
  - PUSH-PULL 패턴
  - PUB-SUB 패턴

- **Latency Benchmarks**: 왕복 지연시간 측정 (microseconds)
  - REQ-REP 패턴
  - DEALER-ROUTER 패턴

## 실행 방법

### 모든 벤치마크 실행

```bash
./gradlew :zmq-benchmark:jmh
```

### 특정 벤치마크 실행

```bash
# Throughput 벤치마크만 실행
./gradlew :zmq-benchmark:jmh -Pjmh.includes='.*Throughput.*'

# Latency 벤치마크만 실행
./gradlew :zmq-benchmark:jmh -Pjmh.includes='.*Latency.*'

# REQ-REP 패턴만 실행
./gradlew :zmq-benchmark:jmh -Pjmh.includes='.*reqRep.*'
```

### 벤치마크 파라미터 커스터마이징

```bash
# Warmup과 Measurement 반복 횟수 조정
./gradlew :zmq-benchmark:jmh -Pjmh.warmupIterations=5 -Pjmh.iterations=10

# Fork 수 조정 (여러 JVM 프로세스에서 실행)
./gradlew :zmq-benchmark:jmh -Pjmh.fork=3

# 스레드 수 조정
./gradlew :zmq-benchmark:jmh -Pjmh.threads=2
```

### Profiler 사용

```bash
# GC profiler 사용 (가비지 컬렉션 통계)
./gradlew :zmq-benchmark:jmh -Pjmh.profilers=gc

# Stack profiler 사용 (핫스팟 분석)
./gradlew :zmq-benchmark:jmh -Pjmh.profilers=stack

# 여러 profiler 동시 사용
./gradlew :zmq-benchmark:jmh -Pjmh.profilers=gc,stack
```

사용 가능한 profiler 목록:
- `gc`: GC 통계
- `stack`: 스택 샘플링
- `perfnorm`: 정규화된 성능 카운터
- `perfasm`: 어셈블리 수준 프로파일링 (Linux perf 필요)

## 결과 확인

벤치마크 결과는 다음 위치에 JSON 형식으로 저장됩니다:
```
zmq-benchmark/build/reports/jmh/results.json
```

콘솔 출력에서도 실시간으로 결과를 확인할 수 있습니다.

## 벤치마크 메시지 크기

### Throughput Benchmarks
- 1 byte
- 128 bytes
- 1 KB (1024 bytes)
- 64 KB (65536 bytes)

### Latency Benchmarks
- REQ-REP: 1 byte, 128 bytes, 1 KB, 64 KB
- DEALER-ROUTER: 1 KB

메시지 크기는 `@Param` 어노테이션을 통해 벤치마크 코드에서 조정할 수 있습니다.

## JMH 설정

`build.gradle.kts`에서 다음 JMH 설정을 확인할 수 있습니다:

```kotlin
jmh {
    jmhVersion.set("1.37")
    warmupIterations.set(3)      // Warmup 반복 횟수
    iterations.set(5)             // Measurement 반복 횟수
    fork.set(1)                   // Fork 수
    threads.set(1)                // 스레드 수
    resultsFile.set(...)          // 결과 파일 위치
    resultFormat.set("JSON")      // 결과 형식
}
```

## 주의사항

1. **성능 벤치마크 실행 환경**
   - 다른 프로세스의 간섭을 최소화하기 위해 조용한 시스템 환경에서 실행하는 것이 좋습니다
   - CPU 주파수 스케일링을 고정하면 더 일관된 결과를 얻을 수 있습니다

2. **JVM 워밍업**
   - JMH는 자동으로 워밍업을 수행하지만, JIT 컴파일 최적화를 위해 충분한 워밍업 반복이 필요합니다
   - 기본값(3회)으로 충분하지 않다면 `-Pjmh.warmupIterations` 옵션으로 증가시킬 수 있습니다

3. **FFM (Foreign Function & Memory) API**
   - ZeroMQ FFM 바인딩은 네이티브 라이브러리 접근을 위해 `--enable-native-access=ALL-UNNAMED` JVM 옵션이 필요합니다
   - 이미 `build.gradle.kts`에 설정되어 있습니다

## 벤치마크 코드 수정

새로운 벤치마크를 추가하거나 기존 벤치마크를 수정하려면:

1. `src/main/java/io/github/ulalax/zmq/benchmark/` 디렉토리의 Java 파일을 편집
2. JMH 어노테이션 사용:
   - `@Benchmark`: 벤치마크 메서드 표시
   - `@State`: 벤치마크 상태 객체 정의
   - `@Setup` / `@TearDown`: 초기화 및 정리 메서드
   - `@Param`: 파라미터화된 벤치마크
   - `@BenchmarkMode`: 측정 모드 (Throughput, AverageTime, SampleTime 등)

자세한 내용은 [JMH 공식 문서](https://github.com/openjdk/jmh)를 참조하세요.

## 참고 자료

- [JMH 공식 GitHub](https://github.com/openjdk/jmh)
- [JMH Gradle Plugin](https://github.com/melix/jmh-gradle-plugin)
- [ZeroMQ 공식 문서](https://zeromq.org/)

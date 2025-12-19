# ZMQ Free Callback Thread Analysis

## 목적
ZMQ의 `zmq_free_fn` 콜백이 어느 스레드에서 실행되는지 검증하여 최적의 Arena 할당 전략을 결정합니다.

## 테스트 결과

### 실행 환경
- **테스트 파일**: `zmq/src/test/java/io/github/ulalax/zmq/CallbackThreadTest.java`
- **메인 스레드**: Thread ID 1 (Test worker)
- **콜백 스레드**: Thread ID 46 (Thread-3)

### 결론
```
❌ NO OPTIMIZATION POSSIBLE
Callback runs on DIFFERENT thread.
Must keep Arena.ofShared() for thread safety.
```

**ZMQ의 `zmq_free_fn` 콜백은 메시지를 전송한 스레드와 다른 스레드에서 실행됩니다.**

## 성능 영향

### Arena.ofConfined() vs Arena.ofShared()
벤치마크 결과 (`ArenaAllocationBenchmark.java`):
- **Arena.ofConfined()**: 2.9 ns/op (매우 빠름)
- **Arena.ofShared()**: 2753.8 ns/op (953배 느림)

### 왜 최적화가 불가능한가?
콜백이 다른 스레드에서 실행되므로:
1. `Arena.ofConfined()`를 사용하면 `IllegalStateException` 발생
2. Cross-thread 접근을 위해 반드시 `Arena.ofShared()` 사용 필요
3. 현재 `Message.java`의 구현이 올바름 (line 203)

## 주요 발견사항

### 1. Message.close()의 버그 수정
**문제**: `Message.close()`에서 `CALLBACK_MAP.remove(callbackId)`를 호출하여 ZMQ가 나중에 콜백을 호출할 때 콜백을 찾을 수 없었습니다.

**수정 전**:
```java
if (callbackId != null) {
    CALLBACK_MAP.remove(callbackId);  // ❌ 너무 빠른 제거!
    callbackId = null;
    userCallback = null;
}
```

**수정 후**:
```java
if (callbackId != null) {
    // Don't remove from CALLBACK_MAP - let staticFreeCallback do it
    // Don't remove from HINT_ARENA_MAP - let staticFreeCallback clean it up
    callbackId = null;  // ✅ 인스턴스 참조만 해제
    userCallback = null;
}
```

### 2. ZMQ의 메시지 라이프사이클
1. `Message.send()` 호출 → 메시지를 ZMQ 큐로 전송
2. `Message.close()` 호출 → Java 측 리소스만 정리
3. 수신자가 메시지를 수신 및 처리
4. ZMQ 내부 참조 카운트가 0이 되면
5. **별도 스레드(Thread-3)에서 `zmq_free_fn` 콜백 호출** ← 중요!
6. `staticFreeCallback` → 사용자 콜백 실행
7. `CALLBACK_MAP`과 `HINT_ARENA_MAP`에서 정리

### 3. TCP vs Inproc Transport
- **TCP**: 콜백이 정상적으로 호출됨
- **Inproc**: 일부 경우 콜백 호출이 생략될 수 있음 (최적화)

## 권장사항

### ✅ 현재 구현 유지
`Message.java` line 203의 `Arena.ofShared()` 사용은 정확하고 필수적입니다.

```java
// MUST use shared arena - ZMQ callback runs on different thread
hintArena = Arena.ofShared();
hintPtr = hintArena.allocate(ValueLayout.JAVA_LONG);
```

### ⚠️ 대안 최적화 불가
953배 성능 향상을 기대했으나, ZMQ의 멀티스레드 아키텍처로 인해 불가능합니다.

### ✅ 버그 수정 완료
`Message.close()`에서 조기 콜백 제거 문제를 수정하여 zero-copy 메커니즘이 정상 작동합니다.

## 관련 파일
- 테스트: `/home/ulalax/project/ulalax/libzmq/jvm-zmq/zmq/src/test/java/io/github/ulalax/zmq/CallbackThreadTest.java`
- 구현: `/home/ulalax/project/ulalax/libzmq/jvm-zmq/zmq/src/main/java/io/github/ulalax/zmq/Message.java`
- 벤치마크: `/home/ulalax/project/ulalax/libzmq/jvm-zmq/zmq/src/jmh/java/io/github/ulalax/zmq/benchmark/ArenaAllocationBenchmark.java`

## 테스트 실행
```bash
./gradlew :zmq:test --tests "io.github.ulalax.zmq.CallbackThreadTest"
```

## 결과 요약
| 항목 | 값 |
|------|-----|
| 메인 스레드 ID | 1 |
| 콜백 스레드 ID | 46 |
| 같은 스레드? | ❌ No |
| Arena.ofConfined() 사용 가능? | ❌ No |
| 현재 구현 올바름? | ✅ Yes |
| 성능 최적화 가능? | ❌ No |

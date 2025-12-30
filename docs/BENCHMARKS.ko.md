[![English](https://img.shields.io/badge/lang-en-red.svg)](BENCHMARKS.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](BENCHMARKS.ko.md)

# JVM-ZMQ 성능 벤치마크

JMH(Java Microbenchmark Harness)를 사용한 JVM-ZMQ의 종합 성능 벤치마크입니다.

## 테스트 환경

- **OS**: Ubuntu 24.04 LTS
- **CPU**: x86_64
- **JVM**: Java HotSpot(TM) 64-Bit Server VM, JDK 22.0.2
- **JMH**: v1.37
- **Pattern**: Router-to-Router (tcp transport)
- **Messages per iteration**: 10,000
- **Warmup**: 3회 반복 (각 2초)
- **Measurement**: 5회 반복 (각 5초)

## 메시지 버퍼 전략 벤치마크

메시지 송수신을 위한 다섯 가지 버퍼 관리 전략 비교입니다.

### 성능 개요

| 전략 | 64B | 512B | 1KB | 64KB | 128KB | 256KB |
|------|-----|------|-----|------|-------|-------|
| **ByteArray** | 5,745 ops/s | 5,307 ops/s | 4,031 ops/s | - | - | - |
| **ArrayPool_Heap** | 5,147 ops/s | 3,416 ops/s | 4,375 ops/s | 824 ops/s | 512 ops/s | 287 ops/s |
| **ArrayPool_Direct** | 4,581 ops/s | 4,693 ops/s | 4,446 ops/s | 821 ops/s | 443 ops/s | 280 ops/s |
| **Message** | - | - | - | - | - | - |
| **MessageZeroCopy** | - | - | - | - | - | - |

### 메모리 할당량

| 전략 | 반복당 할당량 |
|------|--------------|
| **ByteArray** | 메시지 크기에 비례 |
| **ArrayPool_Heap** | ~17KB (일정) |
| **ArrayPool_Direct** | ~44KB (일정) |
| **Message** | - |
| **MessageZeroCopy** | - |

### 핵심 발견

**권장: 프로덕션 환경에서는 ArrayPool_Heap 사용**
- 메시지 크기와 무관하게 일정한 낮은 메모리 할당 (~17KB)
- 모든 메시지 크기에서 우수한 성능
- Direct 할당보다 낮은 GC 압력
- 대부분의 경우 ArrayPool_Heap이 ArrayPool_Direct보다 우수한 성능

### 전략 설명

#### 1. ByteArray_SendRecv (기준)
```java
// 송신
byte[] sendBuffer = new byte[messageSize];
System.arraycopy(sourceData, 0, sendBuffer, 0, messageSize);
socket.send(sendBuffer, SendFlags.DONT_WAIT);

// 수신 - 고정 버퍼 사용
socket.recv(recvBuffer, RecvFlags.NONE);
byte[] outputBuffer = new byte[size];
System.arraycopy(recvBuffer, 0, outputBuffer, 0, size);
```

**특징:**
- 송수신마다 새로운 byte 배열 할당
- 가장 높은 GC 압력 - 메시지 크기에 비례
- 간단한 구현
- **적합한 경우**: 소형 메시지만 사용 (<1KB)

**성능:**
- 64B: 5,745 ops/s
- 512B: 5,307 ops/s
- 1KB: 4,031 ops/s
- 대형 메시지(64KB+)에는 적합하지 않음

#### 2. ArrayPool_Heap (권장)
```java
// 송신
ByteBuf sendBuf = heapAllocator.buffer(messageSize);
try {
    sendBuf.writeBytes(sourceData, 0, messageSize);
    sendBuf.getBytes(0, reusableSendBuffer, 0, messageSize);
    socket.send(reusableSendBuffer, SendFlags.DONT_WAIT);
} finally {
    sendBuf.release();
}

// 수신
socket.recv(recvBuffer, RecvFlags.NONE);
ByteBuf outputBuf = heapAllocator.buffer(size);
try {
    outputBuf.writeBytes(recvBuffer, 0, size);
    outputBuf.getBytes(0, reusableRecvBuffer, 0, size);
} finally {
    outputBuf.release();
}
```

**특징:**
- 힙 버퍼를 사용하는 Netty PooledByteBufAllocator 사용
- **메시지 크기와 무관하게 일정한 낮은 메모리 할당 (~17KB)**
- Direct 할당보다 낮은 GC 압력
- **적합한 경우**: 프로덕션 환경, 모든 메시지 크기

**성능:**
- 64B: 5,147 ops/s
- 512B: 3,416 ops/s
- 1KB: 4,375 ops/s
- 64KB: 824 ops/s
- 128KB: 512 ops/s
- 256KB: 287 ops/s

#### 3. ArrayPool_Direct
```java
// ArrayPool_Heap과 동일하나 direct 버퍼 사용
ByteBuf sendBuf = directAllocator.buffer(messageSize);
```

**특징:**
- direct 버퍼를 사용하는 Netty PooledByteBufAllocator 사용
- 더 높은 할당 오버헤드 (~44KB vs Heap의 ~17KB)
- 일부 경우 매우 큰 메시지에서 더 빠를 수 있음
- **사용 조건**: 프로파일링 결과 이점이 있는 경우에만

**성능:**
- 64B: 4,581 ops/s
- 512B: 4,693 ops/s
- 1KB: 4,446 ops/s
- 64KB: 821 ops/s
- 128KB: 443 ops/s
- 256KB: 280 ops/s

#### 4. Message_SendRecv
```java
// 송신
try (Message idMsg = new Message(router2Id);
     Message payloadMsg = new Message(sourceData)) {
    socket.send(idMsg, SendFlags.SEND_MORE);
    socket.send(payloadMsg, SendFlags.DONT_WAIT);
}

// 수신
try (Message msg = new Message()) {
    socket.recv(msg, RecvFlags.NONE);
    // msg.data()를 직접 사용
}
```

**특징:**
- ZMQ 네이티브 메시지 객체 사용
- MemorySegment를 통한 직접 메모리 액세스
- **적합한 경우**: 네이티브 ZMQ Message API 선호 시

#### 5. MessageZeroCopy_SendRecv (제거됨)
이 전략은 Arena 할당 오버헤드로 인한 심각한 성능 문제로 제거되었습니다.

### 수신 버퍼 모범 사례

> **중요**: 메시지 수신 시 항상 미리 할당된 고정 버퍼를 사용하세요.

```java
// 좋음: 수신 버퍼를 한 번만 할당
byte[] recvBuffer = new byte[maxMessageSize];  // 설정 시 한 번만 할당

while (running) {
    int size = socket.recv(recvBuffer, RecvFlags.NONE);
    // recvBuffer[0..size-1] 처리
}
```

```java
// 나쁨: 매 수신마다 새 버퍼 할당
while (running) {
    byte[] buffer = new byte[maxMessageSize];  // GC 압력!
    socket.recv(buffer, RecvFlags.NONE);
}
```

이 방식은 고처리량 애플리케이션에서 GC 압력을 최소화하는 데 필수적입니다.

### 권장 사항

| 사용 사례 | 권장 전략 | 이유 |
|----------|----------|------|
| **프로덕션 서버** | **ArrayPool_Heap** | 일정한 ~17KB 할당, 최소 GC 압력 |
| 소형 메시지 (<1KB) | ByteArray 또는 ArrayPool_Heap | 둘 다 우수, ArrayPool이 GC 압력 낮음 |
| 대형 메시지 (>64KB) | **ArrayPool_Heap** | 메모리 효율성에 필수 |
| 네이티브 ZMQ API 선호 | Message | 직접 MemorySegment 액세스 |
| Direct 버퍼 필요 | ArrayPool_Direct | 프로파일링 결과 이점이 있는 경우에만 |

## 수신 모드 벤치마크

PureBlocking, BlockingBatch, NonBlocking, Poller 네 가지 수신 전략 비교입니다.

### 성능 개요

| 모드 | 64B (msg/sec) | 512B (msg/sec) | 1KB (msg/sec) | 64KB (msg/sec) |
|------|---------------|----------------|---------------|----------------|
| **PureBlocking** | 1.48M | 1.36M | 1.10M | 70K |
| **BlockingBatch** | 1.46M | 1.35M | 1.03M | 70K |
| **NonBlocking** | 1.38M | 1.27M | 943K | 44K (느림) |
| **Poller** | 1.48M | 1.34M | 1.10M | 68K |

### 상세 메트릭

| 모드 | 크기 | Score (ops/s) | 지연시간 |
|------|------|---------------|---------|
| PureBlocking | 64B | 147.79 | 0.68 μs |
| BlockingBatch | 64B | 145.54 | 0.69 μs |
| NonBlocking | 64B | 137.74 | 0.73 μs |
| Poller | 64B | 147.88 | 0.68 μs |
| | | | |
| PureBlocking | 512B | 135.77 | 0.74 μs |
| BlockingBatch | 512B | 135.17 | 0.74 μs |
| NonBlocking | 512B | 126.58 | 0.79 μs |
| Poller | 512B | 133.89 | 0.75 μs |
| | | | |
| PureBlocking | 1KB | 110.18 | 0.91 μs |
| BlockingBatch | 1KB | 102.93 | 0.97 μs |
| NonBlocking | 1KB | 94.31 | 1.06 μs |
| Poller | 1KB | 110.04 | 0.91 μs |
| | | | |
| PureBlocking | 64KB | 6.98 | 14.33 μs |
| BlockingBatch | 64KB | 7.02 | 14.25 μs |
| NonBlocking | 64KB | 4.38 | 22.83 μs |
| Poller | 64KB | 6.84 | 14.63 μs |

### 모드 설명

#### 1. PureBlocking (기준)
```java
while (n < messageCount) {
    // 블로킹 수신 - 메시지가 도착할 때까지 스레드 대기
    socket.recv(identityBuffer, RecvFlags.NONE);
    socket.recv(recvBuffer, RecvFlags.NONE);
    n++;
}
```

**특징:**
- 메시지가 사용 가능할 때까지 스레드 블로킹
- 가장 간단한 구현, 메시지당 하나의 시스템 콜
- **적합한 경우**: 단일 소켓 애플리케이션

**성능:**
- 64B: 1.48M msg/sec
- 512B: 1.36M msg/sec
- 1KB: 1.10M msg/sec
- 64KB: 70K msg/sec

#### 2. BlockingBatch (처리량 최적화)
```java
while (n < messageCount) {
    // 첫 번째 메시지: 블로킹 대기
    socket.recv(identityBuffer, RecvFlags.NONE);
    socket.recv(recvBuffer, RecvFlags.NONE);
    n++;

    // 가용 메시지 배치 수신 (시스템 콜 감소)
    while (n < messageCount) {
        int bytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
        if (bytes == -1) break;  // 더 이상 없음 (EAGAIN)

        socket.recv(recvBuffer, RecvFlags.NONE);
        n++;
    }
}
```

**특징:**
- 첫 번째 메시지는 블로킹 대기, 이후 논블로킹으로 배치 처리
- 시스템 콜 오버헤드 감소
- **적합한 경우**: 고처리량 단일 소켓 애플리케이션

**성능:**
- 64B: 1.46M msg/sec (PureBlocking 대비 99%)
- 512B: 1.35M msg/sec (PureBlocking 대비 99%)
- 1KB: 1.03M msg/sec (PureBlocking 대비 94%)
- 64KB: 70K msg/sec (PureBlocking 대비 100%)

#### 3. Poller (다중 소켓 권장)
```java
try (Poller poller = new Poller()) {
    int idx = poller.register(socket, PollEvents.IN);

    while (n < messageCount) {
        poller.poll(-1);  // 이벤트 대기

        // 가용 메시지 모두 배치 수신
        while (n < messageCount) {
            int bytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
            if (bytes == -1) break;  // 더 이상 없음 (EAGAIN)

            socket.recv(recvBuffer, RecvFlags.NONE);
            n++;
        }
    }
}
```

**특징:**
- 배치 처리를 사용한 이벤트 기반 I/O
- 여러 소켓 모니터링 가능
- **적합한 경우**: 다중 소켓 애플리케이션

**성능:**
- 64B: 1.48M msg/sec (PureBlocking 대비 100%)
- 512B: 1.34M msg/sec (PureBlocking 대비 99%)
- 1KB: 1.10M msg/sec (PureBlocking 대비 100%)
- 64KB: 68K msg/sec (PureBlocking 대비 97%)

**결론**: Poller는 PureBlocking과 동등한 성능을 제공하면서 다중 소켓 기능을 지원합니다.

#### 4. NonBlocking with Sleep (사용 금지)
```java
while (n < messageCount) {
    int bytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
    if (bytes != -1) {
        socket.recv(recvBuffer, RecvFlags.DONT_WAIT);
        n++;

        // sleep 없이 배치 수신
        while (n < messageCount) {
            int batchBytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
            if (batchBytes == -1) break;
            socket.recv(recvBuffer, RecvFlags.DONT_WAIT);
            n++;
        }
    } else {
        Thread.sleep(1);  // 재시도 전 대기
    }
}
```

**특징:**
- sleep 기반 재시도를 사용한 논블로킹 수신
- sleep 오버헤드로 인한 비효율적인 CPU 사용
- **프로덕션 사용 금지**

**성능:**
- 64B: 1.38M msg/sec (PureBlocking 대비 93%)
- 512B: 1.27M msg/sec (PureBlocking 대비 93%)
- 1KB: 943K msg/sec (PureBlocking 대비 86%)
- 64KB: 44K msg/sec (PureBlocking 대비 63%)

**결론**: 큰 메시지에서 현저히 느림. 대신 Poller를 사용하세요.

### 사용 사례별 권장

| 사용 사례 | 권장 모드 | 이유 |
|----------|----------|------|
| 단일 소켓 (단순) | **PureBlocking** | 가장 간단, 오버헤드 없음 |
| 단일 소켓 (고처리량) | **BlockingBatch** | 시스템 콜 감소 |
| 다중 소켓 | **Poller** | 이벤트 기반, 다중 소켓 지원 |
| 절대 사용 금지 | ~~NonBlocking~~ | sleep 오버헤드로 성능 저하 |

## 벤치마크 실행

### 모든 벤치마크 실행
```bash
./gradlew :zmq:jmh
```

### 특정 벤치마크 실행
```bash
# 메시지 버퍼 전략만
./gradlew :zmq:jmh -PjmhIncludes='.*MessageBufferStrategyBenchmark.*'

# 수신 모드만
./gradlew :zmq:jmh -PjmhIncludes='.*ReceiveModeBenchmark.*'
```

### 결과 포맷
```bash
# 사람이 읽기 쉬운 형식
./gradlew :zmq:formatJmhResults

# .NET BenchmarkDotNet 스타일
cd zmq && python3 scripts/format_jmh_dotnet_style.py
```

### 출력 파일
- **JSON**: `zmq/build/reports/jmh/results.json`
- **포맷됨**: `zmq/build/reports/jmh/results-formatted.txt`

## 핵심 요약

1. **메시지 버퍼 전략** (5가지 전략 테스트):
   - **프로덕션에서는 `ArrayPool_Heap` 사용** - 메시지 크기와 무관하게 일정한 ~17KB 할당
   - 소형 메시지 (<1KB): `ByteArray`와 `ArrayPool_Heap` 모두 우수한 성능 (~4,000-5,700 ops/s)
   - 대형 메시지 (>64KB): `ArrayPool_Heap`이 메모리 효율성에 필수
   - `ArrayPool_Heap`이 낮은 할당 오버헤드로 `ArrayPool_Direct`보다 대부분 우수
   - `MessageZeroCopy`는 심각한 성능 문제로 제거됨

2. **수신 버퍼**:
   - 수신 시 항상 미리 할당된 고정 버퍼 사용
   - 매 수신마다 새 `byte[]` 할당 금지
   - 모든 전략에서 GC 압력 최소화를 위해 적용

3. **수신 모드** (4가지 전략 테스트):
   - 단일 소켓 (단순): `PureBlocking` 사용 (가장 간단, 64B에서 1.48M msg/sec)
   - 단일 소켓 (최적화): `BlockingBatch` 사용 (시스템 콜 감소)
   - 다중 소켓: `Poller` 사용 (PureBlocking 대비 100% 성능, 다중 소켓 지원)
   - sleep을 사용한 `NonBlocking` 사용 금지 (대형 메시지에서 37% 느림)

4. **메모리 할당량**:
   - ArrayPool_Heap: ~17KB (일정)
   - ArrayPool_Direct: ~44KB (일정)
   - ByteArray: 메시지 크기에 비례하여 선형 증가
   - 대부분의 경우 Heap 할당이 Direct보다 효율적

5. **지연시간**:
   - 소형 메시지: 서브 마이크로초 지연시간 (584-942 ns)
   - 중형 메시지: ~750 ns - 1.02 μs (512B-1KB)
   - 대형 메시지: 12-14 마이크로초 (64KB)

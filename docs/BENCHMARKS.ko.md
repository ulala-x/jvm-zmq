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

## 메모리 전략 벤치마크

메시지 송수신을 위한 네 가지 메모리 관리 전략 비교입니다.

### 성능 개요

| Method                   | Size    | Throughput | Msg/sec | Mean      | Ratio | Allocated | Alloc Ratio |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| ByteArray_SendRecv       |     64B | 876.11 Mbps |   1.71M |    5.84 ms |  1.00 |   1.53 MB |       1.00 |
| ArrayPool_SendRecv       |     64B | 660.21 Mbps |   1.29M |    7.76 ms |  1.33 | 178.36 KB |       0.11 |
| MessageZeroCopy_SendRecv |     64B | 14.21 Mbps |  27.75K |  360.30 ms | 61.65 |  10.86 MB |       7.11 |
| Message_SendRecv         |     64B | 543.56 Mbps |   1.06M |    9.42 ms |  1.61 |   7.55 MB |       4.95 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    512B |  5.45 Gbps |   1.33M |    7.51 ms |  1.00 |  10.07 MB |       1.00 |
| ArrayPool_SendRecv       |    512B |  5.26 Gbps |   1.28M |    7.78 ms |  1.04 | 179.11 KB |       0.02 |
| MessageZeroCopy_SendRecv |    512B | 106.89 Mbps |  26.10K |  383.19 ms | 51.00 |  11.14 MB |       1.11 |
| Message_SendRecv         |    512B |  4.07 Gbps | 993.51K |   10.07 ms |  1.34 |   7.55 MB |       0.75 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |     1KB |  8.51 Gbps |   1.04M |    9.63 ms |  1.00 |  19.84 MB |       1.00 |
| ArrayPool_SendRecv       |     1KB |  8.07 Gbps | 984.60K |   10.16 ms |  1.06 | 179.62 KB |       0.01 |
| MessageZeroCopy_SendRecv |     1KB | 210.75 Mbps |  25.73K |  388.70 ms | 40.38 |  11.20 MB |       0.56 |
| Message_SendRecv         |     1KB |  8.17 Gbps | 997.05K |   10.03 ms |  1.04 |   7.55 MB |       0.38 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    64KB |  4.74 GB/s |  72.35K |  138.21 ms |  1.00 |   1.22 GB |       1.00 |
| ArrayPool_SendRecv       |    64KB |  5.10 GB/s |  77.88K |  128.40 ms |  0.93 | 177.15 KB |       0.00 |
| MessageZeroCopy_SendRecv |    64KB |  1.23 GB/s |  18.74K |  533.52 ms |  3.86 |  11.24 MB |       0.01 |
| Message_SendRecv         |    64KB |  5.10 GB/s |  77.83K |  128.48 ms |  0.93 |   7.55 MB |       0.01 |

### 상세 메트릭

| Method                   | Size    | Score (ops/s) | Error      | StdDev    | Latency   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| ByteArray_SendRecv       |     64B |       171.11 |   0.1014 ms |   0.0236 ms | 584.40 ns |   29.0000 |
| ArrayPool_SendRecv       |     64B |       128.95 |   1.2621 ms |   0.2932 ms | 775.51 ns |   21.0000 |
| MessageZeroCopy_SendRecv |     64B |         2.78 |  16.6015 ms |   3.8562 ms |  36.03 μs |    1.0000 |
| Message_SendRecv         |     64B |       106.16 |   0.0954 ms |   0.0222 ms | 941.93 ns |   40.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    512B |       133.10 |   1.0116 ms |   0.2350 ms | 751.29 ns |   73.0000 |
| ArrayPool_SendRecv       |    512B |       128.46 |   0.0989 ms |   0.0230 ms | 778.47 ns |   21.0000 |
| MessageZeroCopy_SendRecv |    512B |         2.61 |   9.1644 ms |   2.1287 ms |  38.32 μs |    1.0000 |
| Message_SendRecv         |    512B |        99.35 |   0.0757 ms |   0.0176 ms |   1.01 μs |   38.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |     1KB |       103.89 |   0.5087 ms |   0.1182 ms | 962.53 ns |   96.0000 |
| ArrayPool_SendRecv       |     1KB |        98.46 |   0.1869 ms |   0.0434 ms |   1.02 μs |   16.0000 |
| MessageZeroCopy_SendRecv |     1KB |         2.57 |  48.1970 ms |  11.1952 ms |  38.87 μs |    1.0000 |
| Message_SendRecv         |     1KB |        99.70 |   0.0772 ms |   0.0179 ms |   1.00 μs |   38.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    64KB |         7.24 |  10.6799 ms |   2.4807 ms |  13.82 μs |  244.0000 |
| ArrayPool_SendRecv       |    64KB |         7.79 |  10.4213 ms |   2.4207 ms |  12.84 μs |    1.0000 |
| MessageZeroCopy_SendRecv |    64KB |         1.87 |  22.4202 ms |   5.2078 ms |  53.35 μs |    1.0000 |
| Message_SendRecv         |    64KB |         7.78 |   4.0053 ms |   0.9303 ms |  12.85 μs |    3.0000 |

### 핵심 발견

**ArrayPool을 프로덕션에서 사용하세요:**
- 메시지 크기와 무관하게 일정한 ~178KB 메모리 할당
- ByteArray는 메시지 크기에 비례하여 증가 (64KB에서 1.22GB vs 178KB)
- 64KB 메시지 기준: ArrayPool이 ByteArray보다 **7,000배 적은 메모리** 사용

### 전략 설명

#### 1. ByteArray_SendRecv (기준)
```java
// 송신
byte[] sendBuffer = new byte[messageSize];
System.arraycopy(sourceData, 0, sendBuffer, 0, messageSize);
socket.send(sendBuffer, SendFlags.DONT_WAIT);

// 수신 - 고정 버퍼 사용
socket.recv(recvBuffer, RecvFlags.NONE);  // recvBuffer는 미리 할당됨
byte[] outputBuffer = new byte[size];     // 매 메시지마다 새 할당
System.arraycopy(recvBuffer, 0, outputBuffer, 0, size);
```

**특징:**
- 송수신마다 새로운 byte 배열 할당
- 가장 높은 GC 압력 (기준 할당량 = 1.0)
- 간단한 구현
- **적합한 경우**: 처리량이 중요한 소형 메시지

**성능:**
- 64B: 1.71M msg/sec (최고)
- 512B: 1.33M msg/sec (최고)
- 1KB: 1.04M msg/sec
- 64KB: 72K msg/sec

#### 2. ArrayPool_SendRecv ✅ 권장
```java
// 송신
ByteBuf sendBuf = allocator.buffer(messageSize);
try {
    sendBuf.writeBytes(sourceData, 0, messageSize);
    sendBuf.getBytes(0, reusableSendBuffer, 0, messageSize);  // 고정 버퍼
    socket.send(reusableSendBuffer, SendFlags.DONT_WAIT);
} finally {
    sendBuf.release();
}

// 수신 - 고정 버퍼 사용
socket.recv(recvBuffer, RecvFlags.NONE);  // recvBuffer는 미리 할당됨
ByteBuf outputBuf = allocator.buffer(size);
try {
    outputBuf.writeBytes(recvBuffer, 0, size);
    outputBuf.getBytes(0, reusableRecvBuffer, 0, size);  // 고정 버퍼
} finally {
    outputBuf.release();
}
```

**특징:**
- 버퍼 풀링을 위해 Netty PooledByteBufAllocator 사용
- **GC 압력 대폭 감소** (512B에서 2% 할당, 1KB에서 1%)
- 메시지 크기와 무관하게 일정한 메모리 할당 (~178KB)
- **적합한 경우**: 프로덕션 환경, 장시간 운영 서버

**성능:**
- 64B: 1.29M msg/sec (ByteArray 대비 75%, 89% 적은 할당)
- 512B: 1.28M msg/sec (ByteArray 대비 96%, 98% 적은 할당)
- 1KB: 984K msg/sec (ByteArray 대비 95%, 99% 적은 할당)
- 64KB: 78K msg/sec (ByteArray 대비 108%, **99.99% 적은 할당**)

#### 3. Message_SendRecv
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
    // msg.data()를 직접 사용 (관리 메모리로 복사 없음)
}
```

**특징:**
- ZMQ 네이티브 메시지 객체 사용
- 중간 GC 압력 (메시지 크기와 무관하게 일정한 ~7.55MB)
- MemorySegment를 통한 직접 메모리 액세스
- **적합한 경우**: 네이티브 ZMQ Message API 선호 시

**성능:**
- 64B: 1.06M msg/sec (ByteArray 대비 62%)
- 512B: 993K msg/sec (ByteArray 대비 75%)
- 1KB: 997K msg/sec (ByteArray 대비 96%)
- 64KB: 78K msg/sec (ByteArray 대비 108%)

#### 4. MessageZeroCopy_SendRecv ❌ 사용 금지
```java
// 제로카피 콜백으로 송신
Arena dataArena = Arena.ofShared();
MemorySegment dataSeg = dataArena.allocate(messageSize);
MemorySegment.copy(sourceData, 0, dataSeg, JAVA_BYTE, 0, messageSize);

Message payloadMsg = new Message(dataSeg, messageSize, data -> {
    dataArena.close();
});
socket.send(payloadMsg, SendFlags.DONT_WAIT);
```

**특징:**
- Arena 할당으로 진정한 제로카피 시도
- **심각한 성능 저하** (64B에서 62배, 512B에서 51배 느림)
- `Arena.ofShared()` 오버헤드 (~31μs/생성)가 제로카피 이점을 압도
- **프로덕션에서 절대 사용 금지**

**성능:**
- 64B: 28K msg/sec (ByteArray 대비 1.6%) ❌
- 512B: 26K msg/sec (ByteArray 대비 2.0%) ❌
- 1KB: 26K msg/sec (ByteArray 대비 2.5%) ❌
- 64KB: 19K msg/sec (ByteArray 대비 26%) ❌

### 수신 버퍼 모범 사례

> **⚠️ 중요**: 메시지 수신 시 항상 미리 할당된 고정 버퍼를 사용하세요.

```java
// ✅ 좋음: 수신 버퍼를 한 번만 할당
byte[] recvBuffer = new byte[maxMessageSize];  // 설정 시 한 번만 할당

while (running) {
    int size = socket.recv(recvBuffer, RecvFlags.NONE);
    // recvBuffer[0..size-1] 처리
}
```

```java
// ❌ 나쁨: 매 수신마다 새 버퍼 할당
while (running) {
    byte[] buffer = new byte[maxMessageSize];  // GC 압력!
    socket.recv(buffer, RecvFlags.NONE);
}
```

이 방식은 고처리량 애플리케이션에서 GC 압력을 최소화하는 데 필수적입니다.

### 권장 사항

| 사용 사례 | 권장 전략 | 이유 |
|----------|----------|------|
| **프로덕션 서버** | ✅ **ArrayPool** | 일정한 ~178KB 할당, 최소 GC 압력 |
| 최대 처리량 (소형 메시지) | ByteArray | <512B에서 최고 msg/sec |
| 네이티브 ZMQ API 선호 | Message | 직접 MemorySegment 액세스 |
| 제로카피 요구사항 | ❌ MessageZeroCopy 사용 금지 | Arena.ofShared() 오버헤드가 너무 높음 |

## 수신 모드 벤치마크

PureBlocking, BlockingBatch, NonBlocking, Poller 네 가지 수신 전략 비교입니다.

### 성능 개요

| 모드 | 64B (msg/sec) | 512B (msg/sec) | 1KB (msg/sec) | 64KB (msg/sec) |
|------|---------------|----------------|---------------|----------------|
| **PureBlocking** | 1.48M | 1.36M | 1.10M | 70K |
| **BlockingBatch** | 1.46M | 1.35M | 1.03M | 70K |
| **NonBlocking** | 1.38M | 1.27M | 943K | 44K ❌ |
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

#### 4. NonBlocking with Sleep ❌ 사용 금지
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
- 64KB: 44K msg/sec (PureBlocking 대비 63%) ❌

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
# 메모리 전략만
./gradlew :zmq:jmh -PjmhIncludes='.*MemoryStrategyBenchmark.*'

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

1. **메모리 전략**:
   - ✅ **프로덕션에서는 `ArrayPool` 사용** - 메시지 크기와 무관하게 일정한 ~178KB 할당
   - 소형 메시지 (<512B): `ByteArray`가 최고 처리량 제공 (64B에서 1.71M msg/sec)
   - 대형 메시지 (>8KB): `ArrayPool` 필수 (ByteArray 대비 7,000배 적은 할당)
   - ❌ **`MessageZeroCopy` 절대 사용 금지** (Arena.ofShared() 오버헤드로 40-62배 느림)

2. **수신 버퍼**:
   - ⚠️ 수신 시 항상 미리 할당된 고정 버퍼 사용
   - 매 수신마다 새 `byte[]` 할당 금지
   - 모든 전략에서 GC 압력 최소화를 위해 적용

3. **수신 모드** (4가지 전략 테스트):
   - 단일 소켓 (단순): `PureBlocking` 사용 (가장 간단, 64B에서 1.48M msg/sec)
   - 단일 소켓 (최적화): `BlockingBatch` 사용 (시스템 콜 감소)
   - 다중 소켓: `Poller` 사용 (PureBlocking 대비 100% 성능, 다중 소켓 지원)
   - ❌ sleep을 사용한 `NonBlocking` 절대 사용 금지 (대형 메시지에서 37% 느림)

4. **GC 압력** (64KB 메시지 기준):
   - ArrayPool: 177KB (일정)
   - Message: 7.55MB (일정)
   - ByteArray: 1.22GB (메시지 크기에 비례)
   - ArrayPool은 64KB에서 ByteArray 대비 **99.99%** 적은 할당

5. **지연시간**:
   - 소형 메시지: 서브 마이크로초 지연시간 (584-942 ns)
   - 중형 메시지: ~750 ns - 1.02 μs (512B-1KB)
   - 대형 메시지: 12-14 마이크로초 (64KB)

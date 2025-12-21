[![English](https://img.shields.io/badge/lang-en-red.svg)](BENCHMARKS.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](BENCHMARKS.ko.md)

# JVM-ZMQ 성능 벤치마크

JMH(Java Microbenchmark Harness)를 사용한 JVM-ZMQ의 종합 성능 벤치마크입니다.

## 테스트 환경

- **OS**: Ubuntu 24.04 LTS
- **CPU**: x86_64
- **JVM**: Java HotSpot(TM) 64-Bit Server VM, JDK 22.0.2
- **JMH**: v1.37
- **패턴**: Router-to-Router (inproc transport)
- **반복당 메시지 수**: 10,000
- **워밍업**: 3회 반복 (각 2초)
- **측정**: 5회 반복 (각 5초)

## 메모리 전략 벤치마크

메시지 송수신을 위한 네 가지 메모리 관리 전략 비교입니다.

### 성능 개요

| 방법                   | 크기    | 처리량 | Msg/sec | 평균      | 비율 | 할당량 | 할당 비율 |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| ByteArray_SendRecv       |     64B |  1.51 Gbps |   2.94M |    3.40 ms |  1.00 |   1.83 MB |       1.00 |
| ArrayPool_SendRecv       |     64B | 922.62 Mbps |   1.80M |    5.55 ms |  1.63 | 490.97 KB |       0.26 |
| MessageZeroCopy_SendRecv |     64B | 14.13 Mbps |  27.59K |  362.46 ms | 106.69 |  11.00 MB |       6.00 |
| Message_SendRecv         |     64B | 614.22 Mbps |   1.20M |    8.34 ms |  2.45 |   7.71 MB |       4.21 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    512B |  6.55 Gbps |   1.60M |    6.26 ms |  1.00 |  10.53 MB |       1.00 |
| ArrayPool_SendRecv       |    512B |  6.19 Gbps |   1.51M |    6.62 ms |  1.06 | 649.20 KB |       0.06 |
| MessageZeroCopy_SendRecv |    512B | 104.48 Mbps |  25.51K |  392.05 ms | 62.66 |  11.14 MB |       1.06 |
| Message_SendRecv         |    512B |  4.31 Gbps |   1.05M |    9.50 ms |  1.52 |   7.71 MB |       0.73 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |     1KB |  9.47 Gbps |   1.16M |    8.65 ms |  1.00 |  20.30 MB |       1.00 |
| ArrayPool_SendRecv       |     1KB |  8.94 Gbps |   1.09M |    9.16 ms |  1.06 | 651.25 KB |       0.03 |
| MessageZeroCopy_SendRecv |     1KB | 204.37 Mbps |  24.95K |  400.83 ms | 46.35 |  11.25 MB |       0.55 |
| Message_SendRecv         |     1KB |  8.69 Gbps |   1.06M |    9.43 ms |  1.09 |   7.71 MB |       0.38 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    64KB |  5.19 GB/s |  79.13K |  126.37 ms |  1.00 |   1.22 GB |       1.00 |
| ArrayPool_SendRecv       |    64KB |  5.26 GB/s |  80.32K |  124.50 ms |  0.99 | 698.09 KB |       0.00 |
| MessageZeroCopy_SendRecv |    64KB |  1.16 GB/s |  17.74K |  563.85 ms |  4.46 |  11.71 MB |       0.01 |
| Message_SendRecv         |    64KB |  5.17 GB/s |  78.94K |  126.68 ms |  1.00 |   7.79 MB |       0.01 |

### 세부 메트릭

| 방법                   | 크기    | 점수 (ops/s) | 오차      | 표준편차    | 지연시간   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| ByteArray_SendRecv       |     64B |       294.36 |   0.1038 ms |   0.0241 ms | 339.72 ns |   50.0000 |
| ArrayPool_SendRecv       |     64B |       180.20 |   0.2714 ms |   0.0630 ms | 554.94 ns |   30.0000 |
| MessageZeroCopy_SendRecv |     64B |         2.76 |  13.1408 ms |   3.0523 ms |  36.25 μs |    1.0000 |
| Message_SendRecv         |     64B |       119.97 |   0.0909 ms |   0.0211 ms | 833.57 ns |   46.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    512B |       159.82 |   0.0851 ms |   0.0198 ms | 625.69 ns |   88.0000 |
| ArrayPool_SendRecv       |    512B |       151.01 |   0.0596 ms |   0.0139 ms | 662.23 ns |   26.0000 |
| MessageZeroCopy_SendRecv |    512B |         2.55 |  20.7017 ms |   4.8086 ms |  39.20 μs |    1.0000 |
| Message_SendRecv         |    512B |       105.31 |   0.1804 ms |   0.0419 ms | 949.58 ns |   40.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |     1KB |       115.64 |   0.1907 ms |   0.0443 ms | 864.77 ns |  107.0000 |
| ArrayPool_SendRecv       |     1KB |       109.13 |   0.3631 ms |   0.0843 ms | 916.32 ns |   18.0000 |
| MessageZeroCopy_SendRecv |     1KB |         2.49 |  23.3933 ms |   5.4338 ms |  40.08 μs |    1.0000 |
| Message_SendRecv         |     1KB |       106.04 |   0.1806 ms |   0.0420 ms | 943.08 ns |   41.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    64KB |         7.91 |   5.0892 ms |   1.1821 ms |  12.64 μs |  223.0000 |
| ArrayPool_SendRecv       |    64KB |         8.03 |   3.5545 ms |   0.8256 ms |  12.45 μs |    2.0000 |
| MessageZeroCopy_SendRecv |    64KB |         1.77 |  11.7885 ms |   2.7382 ms |  56.38 μs |    1.0000 |
| Message_SendRecv         |    64KB |         7.89 |   4.5665 ms |   1.0607 ms |  12.67 μs |    3.0000 |

### 전략 설명

#### 1. ByteArray_SendRecv (기준선)
```java
// 송신
byte[] sendBuffer = new byte[messageSize];
System.arraycopy(sourceData, 0, sendBuffer, 0, messageSize);
socket.send(sendBuffer, SendFlags.DONT_WAIT);

// 수신
byte[] outputBuffer = new byte[size];
System.arraycopy(recvBuffer, 0, outputBuffer, 0, size);
```

**특성:**
- 송수신마다 새로운 byte 배열 할당
- 가장 높은 GC 압력 (기준 할당량 = 1.0)
- 간단한 구현
- **최적 사용처**: 처리량이 중요한 소규모~중규모 메시지

**성능:**
- 64B: 2.94M msg/sec (최고)
- 512B: 1.60M msg/sec (최고)
- 1KB: 1.16M msg/sec (경쟁력 있음)
- 64KB: 79K msg/sec (최고)

#### 2. ArrayPool_SendRecv
```java
// 송신
ByteBuf sendBuf = allocator.buffer(messageSize);
try {
    sendBuf.writeBytes(sourceData, 0, messageSize);
    sendBuf.getBytes(0, reusableSendBuffer, 0, messageSize);
    socket.send(reusableSendBuffer, SendFlags.DONT_WAIT);
} finally {
    sendBuf.release();
}

// 수신
ByteBuf outputBuf = allocator.buffer(size);
try {
    outputBuf.writeBytes(recvBuffer, 0, size);
    outputBuf.getBytes(0, reusableRecvBuffer, 0, size);
} finally {
    outputBuf.release();
}
```

**특성:**
- 버퍼 풀링을 위해 Netty PooledByteBufAllocator 사용
- GC 압력 대폭 감소 (512B에서 6% 할당, 1KB에서 3%)
- 버퍼 관리 규율 필요
- **최적 사용처**: GC 압력이 중요한 중대형 메시지

**성능:**
- 64B: 1.80M msg/sec (ByteArray 대비 61%)
- 512B: 1.51M msg/sec (ByteArray 대비 94%, 할당량 94% 감소)
- 1KB: 1.09M msg/sec (ByteArray 대비 94%, 할당량 97% 감소)
- 64KB: 80K msg/sec (ByteArray 대비 101%, 할당량 >99% 감소)

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
    socket.recv(msg, RecvFlags.NONE).value();
    // msg.data()를 직접 사용 (관리 메모리로 복사 없음)
}
```

**특성:**
- ZMQ 네이티브 메시지 객체 사용
- 중간 GC 압력 (64B에서 4.21배, 512B에서 0.73배, 1KB에서 0.38배)
- MemorySegment를 통한 직접 메모리 액세스
- **최적 사용처**: 중간 크기 메시지 (512B-8KB)

**성능:**
- 64B: 1.20M msg/sec (ByteArray 대비 41%)
- 512B: 1.05M msg/sec (ByteArray 대비 66%)
- 1KB: 1.06M msg/sec (ByteArray 대비 92%)
- 64KB: 79K msg/sec (ByteArray 대비 100%)

#### 4. MessageZeroCopy_SendRecv ❌ 권장하지 않음
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

**특성:**
- Arena 할당으로 진정한 제로카피 시도
- **심각한 성능 저하** (64B에서 107배, 512B에서 63배 느림)
- Arena 할당 오버헤드가 제로카피 이점을 압도
- **프로덕션에서 사용하지 말 것**

**성능:**
- 64B: 27K msg/sec (ByteArray 대비 0.9%) ❌
- 512B: 25K msg/sec (ByteArray 대비 1.6%) ❌
- 1KB: 25K msg/sec (ByteArray 대비 2.1%) ❌
- 64KB: 18K msg/sec (ByteArray 대비 22%) ❌

### 메시지 크기별 권장사항

| 메시지 크기 | 권장 전략 | 처리량 | 이유 |
|--------------|---------------------|------------|--------|
| < 512 B | **ByteArray** | 2.94M msg/sec | 최고 처리량, 간단한 구현 |
| 512 B - 1 KB | **ByteArray** 또는 **ArrayPool** | ~1.5M msg/sec | 유사한 성능, GC 요구사항에 따라 선택 |
| 1-8 KB | **Message** 또는 **ArrayPool** | ~1M msg/sec | 경쟁력 있는 처리량으로 낮은 GC 압력 |
| > 8 KB | **ArrayPool** | 80K msg/sec | >99% 적은 GC 압력으로 최고 처리량 |

## 수신 모드 벤치마크

블로킹, Poller, 논블로킹 세 가지 수신 전략 비교입니다.

### 성능 개요

| 방법                   | 크기    | 처리량 | Msg/sec | 평균      | 비율 | 할당량 | 할당 비율 |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| Blocking_RouterToRouter  |     64B | 735.17 Mbps |   1.44M |    6.96 ms |  1.00 |   5.34 MB |       1.00 |
| NonBlocking_RouterToRouter |     64B | 699.81 Mbps |   1.37M |    7.32 ms |  1.05 |   5.34 MB |       1.00 |
| Poller_RouterToRouter    |     64B | 730.11 Mbps |   1.43M |    7.01 ms |  1.01 |   5.34 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |    512B |  5.57 Gbps |   1.36M |    7.36 ms |  1.00 |   5.50 MB |       1.00 |
| NonBlocking_RouterToRouter |    512B |  5.04 Gbps |   1.23M |    8.12 ms |  1.10 |   5.49 MB |       1.00 |
| Poller_RouterToRouter    |    512B |  5.45 Gbps |   1.33M |    7.52 ms |  1.02 |   5.50 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |     1KB |  8.69 Gbps |   1.06M |    9.42 ms |  1.00 |   5.50 MB |       1.00 |
| NonBlocking_RouterToRouter |     1KB |  8.00 Gbps | 976.90K |   10.24 ms |  1.09 |   5.49 MB |       1.00 |
| Poller_RouterToRouter    |     1KB |  8.76 Gbps |   1.07M |    9.35 ms |  0.99 |   5.50 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |    64KB |  4.41 GB/s |  67.33K |  148.51 ms |  1.00 |   5.57 MB |       1.00 |
| NonBlocking_RouterToRouter |    64KB |  2.23 GB/s |  34.10K |  293.29 ms |  1.97 |   5.50 MB |       0.99 |
| Poller_RouterToRouter    |    64KB |  4.59 GB/s |  70.02K |  142.82 ms |  0.96 |   5.57 MB |       1.00 |

### 세부 메트릭

| 방법                   | 크기    | 점수 (ops/s) | 오차      | 표준편차    | 지연시간   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| Blocking_RouterToRouter  |     64B |       143.59 |   0.0627 ms |   0.0146 ms | 696.43 ns |   55.0000 |
| NonBlocking_RouterToRouter |     64B |       136.68 |   0.1011 ms |   0.0235 ms | 731.62 ns |   53.0000 |
| Poller_RouterToRouter    |     64B |       142.60 |   0.1237 ms |   0.0287 ms | 701.27 ns |   55.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |    512B |       135.92 |   0.1854 ms |   0.0431 ms | 735.75 ns |   52.0000 |
| NonBlocking_RouterToRouter |    512B |       123.12 |   0.1267 ms |   0.0294 ms | 812.22 ns |   47.0000 |
| Poller_RouterToRouter    |    512B |       132.97 |   0.1580 ms |   0.0367 ms | 752.05 ns |   50.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |     1KB |       106.11 |   0.2328 ms |   0.0541 ms | 942.44 ns |   40.0000 |
| NonBlocking_RouterToRouter |     1KB |        97.69 |   0.1830 ms |   0.0425 ms |   1.02 μs |   37.0000 |
| Poller_RouterToRouter    |     1KB |       106.95 |   0.2135 ms |   0.0496 ms | 935.04 ns |   41.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |    64KB |         6.73 |   3.1031 ms |   0.7208 ms |  14.85 μs |    2.0000 |
| NonBlocking_RouterToRouter |    64KB |         3.41 |  12.4926 ms |   2.9018 ms |  29.33 μs |    2.0000 |
| Poller_RouterToRouter    |    64KB |         7.00 |   2.2215 ms |   0.5160 ms |  14.28 μs |    2.0000 |

### 모드 설명

#### 1. 블로킹 (기준선)
```java
while (n < messageCount) {
    // 블로킹 수신 - 메시지가 도착할 때까지 스레드 대기
    socket.recv(identityBuffer, RecvFlags.NONE).value();
    socket.recv(recvBuffer, RecvFlags.NONE).value();
    n++;
}
```

**특성:**
- 메시지가 사용 가능할 때까지 스레드 블로킹
- 가장 간단한 구현
- **최적 사용처**: 단일 소켓 애플리케이션

**성능:**
- 64B: 1.44M msg/sec
- 512B: 1.36M msg/sec
- 1KB: 1.06M msg/sec
- 64KB: 67K msg/sec

#### 2. Poller (다중 소켓에 권장)
```java
Poller poller = new Poller();
int idx = poller.register(socket, PollEvents.IN);

while (n < messageCount) {
    if (poller.poll(1000) > 0) {
        if (poller.isReadable(idx)) {
            socket.recv(identityBuffer, RecvFlags.NONE).value();
            socket.recv(recvBuffer, RecvFlags.NONE).value();
            n++;
        }
    }
}
```

**특성:**
- 이벤트 기반 I/O
- 여러 소켓 모니터링 가능
- **최적 사용처**: 다중 소켓 애플리케이션

**성능:**
- 64B: 1.43M msg/sec (블로킹 대비 99%)
- 512B: 1.33M msg/sec (블로킹 대비 98%)
- 1KB: 1.07M msg/sec (블로킹 대비 101%)
- 64KB: 70K msg/sec (블로킹 대비 104%)

**결론**: Poller는 블로킹과 동등하거나 더 나은 성능을 제공하며 다중 소켓 기능을 제공합니다.

#### 3. Sleep을 사용한 논블로킹 ❌ 권장하지 않음
```java
while (n < messageCount) {
    RecvResult<Integer> idResult = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
    if (idResult.wouldBlock()) {
        Thread.sleep(1); // sleep을 사용한 바쁜 대기
        continue;
    }
    socket.recv(recvBuffer, RecvFlags.NONE).value();
    n++;
}
```

**특성:**
- sleep 기반 재시도를 사용한 논블로킹 수신
- 비효율적인 CPU 사용
- **프로덕션에서 사용하지 말 것**

**성능:**
- 64B: 1.37M msg/sec (블로킹 대비 95%)
- 512B: 1.23M msg/sec (블로킹 대비 90%)
- 1KB: 977K msg/sec (블로킹 대비 92%)
- 64KB: 34K msg/sec (블로킹 대비 51%) ❌

**결론**: 큰 메시지에서 현저히 나쁨. 대신 Poller를 사용하세요.

### 사용 사례별 권장사항

| 사용 사례 | 권장 모드 | 이유 |
|----------|-----------------|--------|
| 단일 소켓 | **블로킹** | 가장 간단, 오버헤드 없음 |
| 다중 소켓 | **Poller** | 이벤트 기반, 블로킹과 동등한 성능 |
| 고빈도 폴링 | **Poller** | 바쁜 대기 오버헤드 방지 |

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

## 핵심 요점

1. **메모리 전략**:
   - 소형 메시지 (<512B): `ByteArray` 사용 (64B에서 2.94M msg/sec)
   - 중형 메시지 (512B-1KB): `ByteArray` 또는 `ArrayPool` 사용 (512B에서 ~1.5M msg/sec)
   - 대형 메시지 (>8KB): GC 감소를 위해 `ArrayPool` 사용 (>99% 적은 할당량)
   - **절대 사용 금지** `MessageZeroCopy` (63-107배 느림)

2. **수신 모드**:
   - 단일 소켓: `블로킹` 사용 (가장 간단)
   - 다중 소켓: `Poller` 사용 (블로킹 대비 98-104% 성능)
   - **절대 사용 금지** sleep을 사용한 `논블로킹` (대형 메시지에서 2배 느림)

3. **GC 압력**:
   - ArrayPool은 ByteArray 대비 할당량 94-99% 감소
   - 트레이드오프: 중형 메시지에서 처리량 약간 낮음 (6-12%)
   - GC 일시정지가 문제일 때 ArrayPool 고려

4. **지연시간**:
   - 소형 메시지: 서브 마이크로초 지연시간 (340-830 ns)
   - 중형 메시지: ~600-950 ns (512B-1KB)
   - 대형 메시지: 12-15 마이크로초 (64KB)

[![English](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](README.ko.md)

# JVM-ZMQ

[![CI - Build and Test](https://github.com/ulala-x/jvm-zmq/actions/workflows/ci.yml/badge.svg)](https://github.com/ulala-x/jvm-zmq/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API Documentation](https://img.shields.io/badge/API-Documentation-blue)](https://ulala-x.github.io/jvm-zmq/)

JDK 22+ FFM (Foreign Function & Memory) API를 사용하는 현대적인 ZeroMQ (libzmq) Java binding입니다.

## 기능

- **Java 22 FFM API**: 안정적인 Foreign Function & Memory API를 사용하여 JNI overhead 없이 native 라이브러리를 직접 binding
- **Type-safe API**: 강력한 type의 enum, Socket option, Message 처리
- **Resource 안전**: Cleaner 기반 자동 finalization을 갖춘 AutoCloseable resource
- **Cross-platform**: Windows, Linux, macOS (x86_64 및 ARM64)용 번들 native 라이브러리
- **완전한 ZMQ 지원**: CURVE 보안을 포함한 모든 socket type, pattern 및 고급 기능
- **Native 의존성 제로**: runtime 시 native libzmq 라이브러리가 자동으로 추출 및 로드

## 설치

### GitHub Packages

이 라이브러리는 GitHub Packages에 게시됩니다. 리포지토리와 의존성을 추가하세요:

#### Gradle

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ulala-x/jvm-zmq")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.ulalax:zmq:0.1")
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/ulala-x/jvm-zmq</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.github.ulalax</groupId>
    <artifactId>zmq</artifactId>
    <version>0.1</version>
</dependency>
```

`~/.m2/settings.xml`에 자격 증명을 추가하세요:
```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

**중요**: 애플리케이션 실행 시 네이티브 액세스를 활성화하세요:

```bash
java --enable-native-access=ALL-UNNAMED -jar your-app.jar
```

## 빠른 시작

### REQ-REP 패턴

```java
import io.github.ulalax.zmq.*;

// 서버
try (Context ctx = new Context();
     Socket server = new Socket(ctx, SocketType.REP)) {
    server.bind("tcp://*:5555");

    // 블로킹 수신 - RecvResult 반환
    RecvResult<String> result = server.recvString();
    result.ifPresent(request -> {
        System.out.println("Received: " + request);
        server.send("World");
    });
}

// 클라이언트
try (Context ctx = new Context();
     Socket client = new Socket(ctx, SocketType.REQ)) {
    client.connect("tcp://localhost:5555");

    // 송신은 SendResult 반환
    SendResult sendResult = client.send("Hello");
    sendResult.ifPresent(bytes ->
        System.out.println("Sent " + bytes + " bytes")
    );

    // Result API로 수신
    client.recvString().ifPresent(reply ->
        System.out.println("Received: " + reply)
    );
}
```

### PUB-SUB 패턴

```java
import io.github.ulalax.zmq.*;

// 발행자
try (Context ctx = new Context();
     Socket pub = new Socket(ctx, SocketType.PUB)) {
    pub.bind("tcp://*:5556");
    pub.send("topic1 Hello subscribers!");
}

// 구독자
try (Context ctx = new Context();
     Socket sub = new Socket(ctx, SocketType.SUB)) {
    sub.connect("tcp://localhost:5556");
    sub.subscribe("topic1");

    // Result API로 수신
    sub.recvString().ifPresent(message ->
        System.out.println("Received: " + message)
    );
}
```

### Router-to-Router 패턴

```java
import io.github.ulalax.zmq.*;
import java.nio.charset.StandardCharsets;

try (Context ctx = new Context();
     Socket peerA = new Socket(ctx, SocketType.ROUTER);
     Socket peerB = new Socket(ctx, SocketType.ROUTER)) {

    // Router-to-Router를 위한 명시적 식별자 설정
    peerA.setOption(SocketOption.ROUTING_ID, "PEER_A".getBytes(StandardCharsets.UTF_8));
    peerB.setOption(SocketOption.ROUTING_ID, "PEER_B".getBytes(StandardCharsets.UTF_8));

    peerA.bind("tcp://127.0.0.1:5555");
    peerB.connect("tcp://127.0.0.1:5555");

    // Peer B가 Peer A에게 송신 (첫 번째 프레임 = 대상 식별자)
    peerB.send("PEER_A".getBytes(StandardCharsets.UTF_8), SendFlags.SEND_MORE);
    peerB.send("Hello from Peer B!");

    // Peer A가 수신 (첫 번째 프레임 = 송신자 식별자) - Result API 사용
    RecvResult<byte[]> senderIdResult = peerA.recvBytes();
    RecvResult<String> messageResult = peerA.recvString();

    if (senderIdResult.isPresent() && messageResult.isPresent()) {
        byte[] senderId = senderIdResult.value();
        String message = messageResult.value();
        System.out.println("Received from peer: " + message);

        // Peer A가 송신자의 식별자를 사용하여 응답
        peerA.send(senderId, SendFlags.SEND_MORE);
        peerA.send("Hello back from Peer A!");
    }
}
```

### 폴링(Polling)

```java
import io.github.ulalax.zmq.*;

try (Poller poller = new Poller()) {
    int idx1 = poller.register(socket1, PollEvents.IN);
    int idx2 = poller.register(socket2, PollEvents.IN);

    if (poller.poll(1000) > 0) {
        if (poller.isReadable(idx1)) { /* socket1 처리 */ }
        if (poller.isReadable(idx2)) { /* socket2 처리 */ }
    }
}
```

### Result API를 사용한 논블로킹 I/O

```java
import io.github.ulalax.zmq.*;

try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {
    socket.connect("tcp://localhost:5555");

    // 논블로킹 송신
    SendResult sendResult = socket.send(data, SendFlags.DONT_WAIT);
    if (sendResult.wouldBlock()) {
        System.out.println("Socket not ready - would block");
    } else {
        System.out.println("Sent " + sendResult.value() + " bytes");
    }

    // 함수형 스타일의 논블로킹 수신
    socket.recvString(RecvFlags.DONT_WAIT)
        .ifPresent(msg -> System.out.println("Received: " + msg));

    // 수신 데이터 변환
    RecvResult<Integer> length = socket.recvBytes(RecvFlags.DONT_WAIT)
        .map(bytes -> bytes.length);
    length.ifPresent(len -> System.out.println("Length: " + len));
}
```

## 성능

Router-to-Router 패턴 성능 벤치마크 (Ubuntu 24.04 LTS, JDK 22.0.2, 반복당 10,000개 메시지):

### 메모리 전략 성능

**작은 메시지 (64 bytes):**
- **ByteArray**: 2.94M msg/sec (1.51 Gbps) - **작은 메시지에 최적**
- ArrayPool: 1.80M msg/sec (923 Mbps, ByteArray 대비 74% 적은 할당)
- Message: 1.20M msg/sec (614 Mbps)
- ❌ MessageZeroCopy: 28K msg/sec (심각한 성능 저하)

**중간 메시지 (512 bytes):**
- **ByteArray**: 1.60M msg/sec (6.55 Gbps) - **최고 처리량**
- ArrayPool: 1.51M msg/sec (6.19 Gbps, ByteArray 대비 94% 적은 할당)
- Message: 1.05M msg/sec (4.31 Gbps)
- ❌ MessageZeroCopy: 26K msg/sec

**중간 메시지 (1,024 bytes):**
- **ByteArray**: 1.16M msg/sec (9.47 Gbps) - **최고 처리량**
- ArrayPool: 1.09M msg/sec (8.94 Gbps, ByteArray 대비 97% 적은 할당)
- Message: 1.06M msg/sec (8.69 Gbps)
- ❌ MessageZeroCopy: 25K msg/sec

**큰 메시지 (65,536 bytes):**
- **ArrayPool**: 80K msg/sec (5.26 GB/s, >99% 적은 할당) - **큰 메시지에 최적**
- ByteArray: 79K msg/sec (5.19 GB/s)
- Message: 79K msg/sec (5.17 GB/s)
- ❌ MessageZeroCopy: 18K msg/sec

**권장 사항:**
- **작은 메시지 (<512B)**: 최대 처리량을 위해 `socket.send(byte[])`를 사용 (64B에서 2.94M msg/sec)
- **중간 메시지 (512B-1KB)**: `ByteArray` 또는 `ArrayPool` 사용 - 유사한 성능에 94-97% 적은 GC
- **큰 메시지 (>8KB)**: GC 압력을 줄이기 위해 `ArrayPool` 패턴 사용 (>99% 적은 할당)
- **피하기**: `MessageZeroCopy` - Arena 할당 오버헤드로 인해 63-107배 느림

### 수신 모드 성능

| 메시지 크기 | 블로킹 | 폴러 | 논블로킹 |
|--------------|----------|--------|-------------|
| **64 B** | 1.44M msg/sec | **1.43M msg/sec** | 1.37M msg/sec |
| **512 B** | 1.36M msg/sec | **1.33M msg/sec** | 1.23M msg/sec |
| **1,024 B** | 1.06M msg/sec | **1.07M msg/sec** | 977K msg/sec |
| **65,536 B** | 67K msg/sec | **70K msg/sec** | 34K msg/sec |

**권장 사항:**
- **단일 소켓**: 가장 간단한 구현을 위해 `블로킹` 모드 (`socket.recv()`) 사용
- **다중 소켓**: 이벤트 기반 프로그래밍을 위해 `폴러`를 사용 - 블로킹 성능과 동일하거나 초과 (98-104%)
- **피하기**: busy-wait/sleep을 사용하는 `논블로킹` - 프로덕션에 권장하지 않음 (큰 메시지의 경우 2배 느림)

### 벤치마크 실행

```bash
# 모든 JMH 벤치마크 실행
./gradlew :zmq:jmh

# 특정 벤치마크 실행
./gradlew :zmq:jmh -PjmhIncludes='.*MemoryStrategyBenchmark.*'
./gradlew :zmq:jmh -PjmhIncludes='.*ReceiveModeBenchmark.*'

# .NET BenchmarkDotNet 스타일로 결과 포맷
cd zmq && python3 scripts/format_jmh_dotnet_style.py
```

결과는 `zmq/build/reports/jmh/results.json` (JSON) 및 `results-formatted.txt` (사람이 읽을 수 있는 형식)에 저장됩니다.

완전한 벤치마크 분석, 구현 세부사항 및 최적화 가이드라인은 **[성능 벤치마크](docs/BENCHMARKS.ko.md)**를 참조하세요.

## 소켓 타입

| 타입 | 설명 |
|------|-------------|
| `SocketType.REQ` | 요청 소켓 (클라이언트) |
| `SocketType.REP` | 응답 소켓 (서버) |
| `SocketType.PUB` | 발행 소켓 |
| `SocketType.SUB` | 구독 소켓 |
| `SocketType.PUSH` | 푸시 소켓 (파이프라인) |
| `SocketType.PULL` | 풀 소켓 (파이프라인) |
| `SocketType.DEALER` | 비동기 요청 |
| `SocketType.ROUTER` | 비동기 응답 |
| `SocketType.PAIR` | 독점 쌍 |
| `SocketType.XPUB` | 확장 발행 |
| `SocketType.XSUB` | 확장 구독 |
| `SocketType.STREAM` | 원시 TCP 소켓 |

## API 레퍼런스

### 컨텍스트(Context)

```java
Context ctx = new Context();                              // 기본
Context ctx = new Context(ioThreads, maxSockets);         // 사용자 정의

ctx.setOption(ContextOption.IO_THREADS, 4);
int threads = ctx.getOption(ContextOption.IO_THREADS);

int[] version = Context.version();                        // ZMQ 버전 가져오기
boolean hasCurve = Context.has("curve");                  // 기능 확인
```

### 소켓(Socket)

```java
Socket socket = new Socket(ctx, SocketType.REQ);

// 연결
socket.bind("tcp://*:5555");
socket.connect("tcp://localhost:5555");
socket.unbind("tcp://*:5555");
socket.disconnect("tcp://localhost:5555");

// 송신
socket.send("Hello");
socket.send(byteArray);
socket.send(data, SendFlags.SEND_MORE);
boolean sent = socket.trySend(data);

// 수신
String str = socket.recvString();
byte[] data = socket.recvBytes();
boolean received = socket.tryRecvString();

// 옵션
socket.setOption(SocketOption.LINGER, 0);
int linger = socket.getOption(SocketOption.LINGER);
```

## 샘플

`zmq-samples` 모듈은 모든 ZeroMQ 패턴을 시연하는 13개의 샘플 애플리케이션을 포함합니다:

| 샘플 | 패턴 | 설명 |
|--------|---------|-------------|
| ReqRepSample | REQ-REP | 동기 요청-응답 |
| PubSubSample | PUB-SUB | 토픽 필터링이 있는 발행-구독 |
| PushPullSample | PUSH-PULL | 파이프라인 (ventilator-worker-sink) |
| PairSample | PAIR | 독점 1:1 양방향 |
| RouterDealerSample | ROUTER-DEALER | 비동기 브로커 패턴 |
| RouterToRouterSample | ROUTER-ROUTER | 피어-투-피어, 허브-스포크 |
| ProxySample | XPUB-XSUB | 발행-구독 전달을 위한 프록시 |
| SteerableProxySample | Steerable Proxy | 제어 가능한 프록시 (PAUSE/RESUME) |
| PollerSample | Polling | 다중 소켓 폴링 |
| MonitorSample | Monitor | 소켓 이벤트 모니터링 |
| CurveSecuritySample | CURVE | 암호화된 통신 |
| MultipartSample | Multipart | 멀티파트 메시지 처리 |
| RouterBenchmarkSample | Benchmark | 성능 테스트 |

### 샘플 실행

```bash
# 특정 샘플 실행
./gradlew :zmq-samples:runReqRep
./gradlew :zmq-samples:runPubSub
./gradlew :zmq-samples:runPushPull
./gradlew :zmq-samples:runPair
./gradlew :zmq-samples:runRouterDealer
./gradlew :zmq-samples:runRouterToRouter
./gradlew :zmq-samples:runProxy
./gradlew :zmq-samples:runSteerableProxy
./gradlew :zmq-samples:runPoller
./gradlew :zmq-samples:runMonitor
./gradlew :zmq-samples:runCurveSecurity
./gradlew :zmq-samples:runMultipart
./gradlew :zmq-samples:runRouterBenchmark
```

## 지원 플랫폼

| OS | 아키텍처 |
|----|--------------|
| Windows | x64, ARM64 |
| Linux | x64, ARM64 |
| macOS | x64, ARM64 |

## 요구 사항

- JDK 22 이상
- 네이티브 libzmq 라이브러리 (자동 제공)

## 소스에서 빌드

```bash
# 리포지토리 클론
git clone https://github.com/ulala-x/jvm-zmq.git
cd jvm-zmq

# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 로컬 Maven 리포지토리에 설치 (로컬 개발/테스트용)
./gradlew publishToMavenLocal
```

## 프로젝트 구조

```
jvm-zmq/
├── zmq-core/          # 저수준 FFM 바인딩
│   └── src/main/java/io/github/ulalax/zmq/core/
│       ├── LibZmq.java        # FFM 함수 바인딩
│       ├── ZmqConstants.java  # ZMQ 상수
│       ├── ZmqStructs.java    # 메모리 레이아웃
│       ├── ZmqException.java  # 예외 처리
│       └── NativeLoader.java  # 네이티브 라이브러리 로더
│
├── zmq/               # 고수준 API 및 JMH 벤치마크
│   ├── src/main/java/io/github/ulalax/zmq/
│   │   ├── Context.java           # ZMQ 컨텍스트
│   │   ├── Socket.java            # ZMQ 소켓
│   │   ├── Message.java           # ZMQ 메시지
│   │   ├── MultipartMessage.java  # 멀티파트 유틸리티
│   │   ├── Poller.java            # 인스턴스 기반 폴링
│   │   ├── Curve.java             # CURVE 보안
│   │   └── Proxy.java             # 프록시 유틸리티
│   └── src/jmh/java/io/github/ulalax/zmq/benchmark/
│       └── *.java                 # 성능 벤치마크
│
└── zmq-samples/       # 샘플 애플리케이션
    └── src/main/java/io/github/ulalax/zmq/samples/
        └── *.java                 # 13개의 샘플 프로그램
```

## 문서

- **[API 문서](https://ulala-x.github.io/jvm-zmq/)** - 완전한 Javadoc API 레퍼런스
- **[성능 벤치마크](docs/BENCHMARKS.ko.md)** - 상세한 벤치마크 결과 및 분석
- **[샘플 코드](zmq-samples/README.ko.md)** - 13개의 샘플 애플리케이션

## 라이선스

MIT License - 자세한 내용은 [LICENSE](LICENSE)를 참조하세요.

## 관련 프로젝트

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ 핵심 라이브러리
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Windows/Linux/macOS (x64/ARM64)용 크로스 플랫폼 네이티브 바이너리
- [net-zmq](https://github.com/ulala-x/net-zmq) - cppzmq 스타일 API를 갖춘 .NET 8+ ZeroMQ 바인딩

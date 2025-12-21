[![English](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](README.ko.md)

# JVM-ZMQ Samples

이 모듈은 JVM-ZMQ 라이브러리의 사용 예제를 제공합니다.

## 샘플 목록

### PushPullSample

PUSH-PULL 패턴을 시연하는 Ventilator-Worker-Sink 파이프라인 샘플입니다.

**실행 방법:**

```bash
# 전체 파이프라인 실행 (Ventilator + 3 Workers + Sink, 기본값)
./gradlew :zmq-samples:run --args="all"

# Ventilator만 실행
./gradlew :zmq-samples:run --args="ventilator"

# Worker만 실행 (별도 터미널에서, worker-id 지정 가능)
./gradlew :zmq-samples:run --args="worker 1"
./gradlew :zmq-samples:run --args="worker 2"

# Sink만 실행
./gradlew :zmq-samples:run --args="sink"
```

**동작 방식:**

1. **Ventilator (PUSH 소켓)**
   - `tcp://*:5557`에 바인딩하여 작업 분배
   - 100개의 작업을 생성하고 각 작업에 랜덤 워크로드(1-100ms) 할당
   - Sink에 "START" 신호를 보내 배치 시작 알림
   - 작업은 "taskNum:workload" 형식으로 전송 (예: "42:75")
   - 20개 작업마다 진행 상황 출력

2. **Workers (PULL -> PUSH 소켓)**
   - `tcp://localhost:5557`에서 작업 수신
   - `tcp://localhost:5558`로 결과 전송
   - 작업의 workload만큼 sleep하여 처리 시뮬레이션
   - 결과는 "workerId:taskNum:workload" 형식으로 전송
   - 3초 타임아웃 후 자동 종료

3. **Sink (PULL 소켓)**
   - `tcp://*:5558`에 바인딩하여 결과 수집
   - "START" 신호 대기 후 결과 수집 시작
   - 100개의 결과를 수신하거나 5초 타임아웃까지 대기
   - Worker별 작업 분배 통계 출력 (작업 수, 비율, 총 워크로드)

**로드 밸런싱:**
- ZeroMQ의 PUSH-PULL 패턴은 자동으로 라운드로빈 로드 밸런싱 수행
- 각 Worker는 거의 동일한 수의 작업을 처리 (약 33%)
- Worker는 이전 작업 완료 후 다음 작업을 받아 효율적으로 처리

**출력 예시:**

```
JVM-ZMQ PUSH-PULL Pipeline Pattern Sample
==========================================
Demonstrating Ventilator-Worker-Sink Pattern

Starting complete pipeline: 1 Ventilator, 3 Workers, 1 Sink

[Sink] Starting result collector...
[Sink] Bound to tcp://*:5558
[Sink] Waiting for batch start signal...
[Worker-1] Starting...
[Worker-1] Connected and ready for tasks
[Worker-2] Starting...
[Worker-2] Connected and ready for tasks
[Worker-3] Starting...
[Worker-3] Connected and ready for tasks
[Ventilator] Starting task generator...
[Ventilator] Bound to tcp://*:5557
[Ventilator] Distributing 100 tasks...
[Sink] Batch started, collecting results...
[Ventilator] Dispatched 20/100 tasks
[Ventilator] Dispatched 40/100 tasks
[Ventilator] Dispatched 60/100 tasks
[Ventilator] Dispatched 80/100 tasks
[Ventilator] Dispatched 100/100 tasks
[Ventilator] All 100 tasks dispatched
[Ventilator] Total expected workload: 5216ms
[Ventilator] Average per task: 52ms
[Worker-1] Processed 10 tasks (current: task#27, 40ms)
[Worker-2] Processed 10 tasks (current: task#29, 62ms)
[Worker-3] Processed 10 tasks (current: task#28, 12ms)
[Sink] Received 20/100 results
[Sink] Received 40/100 results
...
[Sink] Received 100/100 results

[Sink] ========== Pipeline Statistics ==========
[Sink] Total results received: 100/100
[Sink] Total elapsed time: 1.83s

[Sink] Worker Load Distribution:
[Sink]   Worker-1: 34 tasks (34.0%), 1816ms workload
[Sink]   Worker-2: 33 tasks (33.0%), 1753ms workload
[Sink]   Worker-3: 33 tasks (33.0%), 1647ms workload
[Sink] =============================================
[Sink] Done

Pipeline completed successfully!
```

### PubSubSample

PUB-SUB 패턴을 시연하는 토픽 기반 메시지 브로드캐스팅 샘플입니다.

**실행 방법:**

```bash
# Publisher와 Subscriber를 동시에 실행 (기본값)
./gradlew :zmq-samples:run

# Publisher만 실행
./gradlew :zmq-samples:run --args="pub"

# Subscriber만 실행 (별도 터미널에서 Publisher 실행 후)
./gradlew :zmq-samples:run --args="sub"

# Both 모드 명시적 실행
./gradlew :zmq-samples:run --args="both"
```

**동작 방식:**

1. **Publisher (PUB 소켓)**
   - `tcp://*:5556`에 바인딩
   - 3개의 토픽(weather, sports, news)으로 10개의 메시지 브로드캐스트
   - 각 메시지는 토픽 이름으로 시작 (예: "weather Update #1")
   - 500ms 간격으로 메시지 전송

2. **Subscriber (SUB 소켓)**
   - `tcp://localhost:5556`에 연결
   - "weather"와 "news" 토픽만 구독 (sports는 필터링됨)
   - 2초 수신 타임아웃 설정
   - 구독한 토픽의 메시지만 수신

**토픽 필터링:**
- Subscriber는 "weather"와 "news" 메시지만 수신
- "sports" 메시지는 자동으로 필터링되어 수신되지 않음
- 토픽 필터링은 메시지 접두사 매칭으로 동작

**출력 예시:**

```
JVM-ZMQ PUB-SUB Sample
=====================

[Publisher] Starting...
[Publisher] Binding to tcp://*:5556
[Subscriber] Starting...
[Subscriber] Subscribed to 'weather' and 'news' topics
[Publisher] Sent: weather Update #1
[Subscriber] Received: weather Update #1
[Publisher] Sent: sports Update #2    <- 필터링됨 (수신 안됨)
[Publisher] Sent: news Update #3
[Subscriber] Received: news Update #3
[Publisher] Sent: weather Update #4
[Subscriber] Received: weather Update #4
...
[Publisher] Done
[Subscriber] Timeout, no message received
[Subscriber] Done
```

### ReqRepSample

REQ-REP 패턴을 시연하는 간단한 클라이언트-서버 샘플입니다.

**실행 방법:**

```bash
# 서버와 클라이언트를 동시에 실행 (기본값)
./gradlew :zmq-samples:run

# 서버만 실행
./gradlew :zmq-samples:run --args="server"

# 클라이언트만 실행 (별도 터미널에서 서버 실행 후)
./gradlew :zmq-samples:run --args="client"

# Both 모드 명시적 실행
./gradlew :zmq-samples:run --args="both"
```

**동작 방식:**

1. **Server (REP 소켓)**
   - `tcp://*:5555`에 바인딩
   - 5개의 요청을 수신하고 각각에 대해 응답
   - 각 요청 처리 전 100ms 대기 (처리 시뮬레이션)

2. **Client (REQ 소켓)**
   - `tcp://localhost:5555`에 연결
   - 5개의 "Hello #N" 메시지 전송
   - 각 요청에 대한 "Reply #N" 응답 수신

**출력 예시:**

```
JVM-ZMQ REQ-REP Sample
=====================

[Server] Starting...
[Server] Listening on tcp://*:5555
[Client] Starting...
[Client] Connected to tcp://localhost:5555
[Client] Sent: Hello #1
[Server] Received: Hello #1
[Server] Sent: Reply #1
[Client] Received: Reply #1
[Client] Sent: Hello #2
[Server] Received: Hello #2
[Server] Sent: Reply #2
[Client] Received: Reply #2
...
[Server] Done
[Client] Done
```

### RouterDealerSample

ROUTER-DEALER 패턴을 시연하는 비동기 브로커 샘플입니다.

**실행 방법:**

```bash
# 직접 클래스 실행
java --enable-native-access=ALL-UNNAMED -cp "zmq-samples/build/libs/zmq-samples-0.1.jar:zmq/build/libs/zmq-0.1.jar:zmq-core/build/libs/zmq-core-0.1.jar" io.github.ulalax.zmq.samples.RouterDealerSample
```

**동작 방식:**

1. **Broker (ROUTER 소켓)**
   - Frontend: `tcp://*:5555`에 바인딩하여 클라이언트 요청 수신
   - Backend: `tcp://*:5556`에 바인딩하여 워커와 통신
   - Poller를 사용하여 frontend와 backend의 메시지를 비동기로 처리
   - 클라이언트 요청을 큐에 저장하고 사용 가능한 워커에게 라우팅
   - 워커의 "READY" 메시지를 받으면 사용 가능한 워커로 등록

2. **Client (DEALER 소켓)**
   - ROUTING_ID 설정으로 고유 클라이언트 ID 지정
   - `tcp://localhost:5555`로 브로커에 연결
   - 각 클라이언트는 3개의 요청을 순차적으로 전송
   - 각 요청에 대한 응답을 동기적으로 대기

3. **Worker (DEALER 소켓)**
   - ROUTING_ID 설정으로 고유 워커 ID 지정
   - `tcp://localhost:5556`으로 브로커의 backend에 연결
   - 첫 연결 시 "READY" 메시지를 전송하여 준비 상태 알림
   - 요청을 받으면 300ms 처리 시뮬레이션 후 응답 전송

**메시지 엔벨로프 형식:**

- Client → Broker: `[client-identity][empty][request]`
- Broker → Worker: `[worker-identity][empty][client-identity][empty][request]`
- Worker → Broker: `[worker-identity][empty][client-identity][empty][reply]`
- Broker → Client: `[client-identity][empty][reply]`

**로드 밸런싱:**
- 브로커가 클라이언트 요청 큐와 사용 가능한 워커 큐를 관리
- 워커가 준비 상태일 때만 새로운 요청을 할당 (Lazy Pirate 패턴)
- 요청을 처리한 워커는 다시 사용 가능한 워커 큐에 추가되어 다음 요청 처리

**출력 예시:**

```
JVM-ZMQ ROUTER-DEALER Async Broker Sample
==========================================

This sample demonstrates the async broker pattern:
- Broker with ROUTER frontend and ROUTER backend
- Multiple DEALER clients sending requests
- Multiple DEALER workers processing requests

[Broker] Starting...
[Broker] Frontend listening on tcp://*:5555
[Broker] Backend listening on tcp://*:5556
[Broker] Polling started...
[worker-1] Starting...
[worker-2] Starting...
[worker-1] Connected to broker
[worker-2] Connected to broker
[Broker] Worker worker-1 is ready
[Broker] Worker worker-2 is ready
[client-1] Starting...
[client-2] Starting...
[client-1] Connected to broker
[client-2] Connected to broker
[client-1] Sent: Request #1 from client-1
[Broker] Client client-1 -> Request: Request #1 from client-1
[Broker] Routed to Worker worker-1 for Client client-1
[worker-1] Processing request from client-1: Request #1 from client-1
[worker-1] Sent reply to client-1: Processed by worker-1
[Broker] Worker worker-1 -> Client client-1: Processed by worker-1
[client-1] Received: Processed by worker-1
...
[client-1] Done
[client-2] Done

All clients completed. Press Ctrl+C to exit.
```

### RouterToRouterSample

ROUTER-to-ROUTER 패턴을 시연하는 고급 라우팅 샘플입니다. MultipartMessage API를 사용하여 깔끔한 코드로 구현되었습니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runRouterToRouter

# 또는 직접 클래스 실행
java --enable-native-access=ALL-UNNAMED -cp "zmq-samples/build/install/zmq-samples/lib/*" io.github.ulalax.zmq.samples.RouterToRouterSample
```

**동작 방식:**

이 샘플은 세 가지 Router-to-Router 패턴 예제를 포함합니다:

#### Example 1: Basic Peer-to-Peer (기본 P2P 통신)

두 개의 ROUTER 소켓이 직접 통신하는 패턴입니다.

- **Peer A**: `tcp://127.0.0.1:15700`에 바인딩, ROUTING_ID = "PEER_A"
- **Peer B**: Peer A에 연결, ROUTING_ID = "PEER_B"
- 각 피어는 상대방의 ROUTING_ID를 첫 번째 프레임으로 사용하여 메시지 전송
- MultipartMessage를 사용하여 [target-id][message] 형식으로 전송
- 양방향 통신: Peer B → Peer A → Peer B

**메시지 형식:**
- 전송: `[Target Identity][Message Content]`
- 수신: `[Sender Identity][Message Content]`

#### Example 2: Hub and Spoke Pattern (허브-스포크 패턴)

중앙 허브가 여러 스포크와 통신하는 패턴입니다.

- **Hub**: `tcp://127.0.0.1:15701`에 바인딩, ROUTING_ID = "HUB"
- **Spoke1-3**: Hub에 연결, ROUTING_ID = "SPOKE1", "SPOKE2", "SPOKE3"
- 각 스포크는 Hub에 등록 메시지 전송
- Hub는 등록된 모든 스포크에 브로드캐스트 메시지 전송
- 토폴로지: 중앙 집중식 통신 (모든 통신이 Hub를 거침)

**동작 순서:**
1. 3개의 스포크가 Hub에 "REGISTER:SPOKEX" 메시지 전송
2. Hub가 모든 등록 메시지 수신 및 피어 목록에 추가
3. Hub가 각 스포크에 환영 메시지 브로드캐스트
4. 각 스포크가 자신에게 온 메시지 수신 및 출력

#### Example 3: Broker Pattern (브로커 패턴)

브로커를 통해 클라이언트들이 서로 메시지를 교환하는 패턴입니다.

- **Broker**: `tcp://127.0.0.1:15702`에 바인딩, ROUTING_ID = "BROKER"
- **Client1-2**: Broker에 연결, ROUTING_ID = "CLIENT1", "CLIENT2"
- 클라이언트는 브로커를 통해 다른 클라이언트에게 메시지 전송
- 브로커는 원본 발신자 정보를 포함하여 메시지 포워딩
- 3-프레임 메시지: `[Broker][Target][Message]` → `[Target][Sender][Message]`

**메시지 흐름:**
1. Client1 → Broker: `[BROKER][CLIENT2][Hello Client2, this is Client1!]`
2. Broker → Client2: `[CLIENT2][CLIENT1][Hello Client2, this is Client1!]`
3. Client2 → Broker: `[BROKER][CLIENT1][Got your message! Reply from Client2.]`
4. Broker → Client1: `[CLIENT1][CLIENT2][Got your message! Reply from Client2.]`

**주요 특징:**

- **명시적 Routing ID**: 모든 ROUTER 소켓은 `ROUTING_ID` 옵션으로 명시적 ID 설정 필수
- **MultipartMessage API**: 프레임 단위 메시지를 깔끔하게 구성하고 전송
- **완전 비동기**: 양방향 비동기 통신으로 유연한 네트워크 토폴로지 구현 가능
- **메시지 라우팅**: 첫 번째 프레임이 항상 타겟/발신자 ID로 사용됨

**사용 사례:**
- P2P 네트워크 구축
- 메시지 브로커 및 프록시 구현
- 복잡한 네트워크 토폴로지 (mesh, hub-spoke, broker 등)
- 명시적 주소 지정이 필요한 시스템

**출력 예시:**

```
=== JVM-ZMQ Router-to-Router Examples (MultipartMessage API) ===

--- Example 1: Basic Peer-to-Peer (MultipartMessage) ---
Peer B sending message to Peer A...
Peer A received from [PEER_B]: Hello from Peer B!
Peer A replying to Peer B...
Peer B received from [PEER_A]: Hello back from Peer A!

--- Example 2: Hub and Spoke Pattern (MultipartMessage) ---
Spokes sending registration to Hub...
Hub received: [SPOKE1] -> REGISTER:SPOKE1
Hub received: [SPOKE2] -> REGISTER:SPOKE2
Hub received: [SPOKE3] -> REGISTER:SPOKE3

Hub broadcasting to all spokes...
SPOKE1 received from [HUB]: Welcome SPOKE1! You are connected.
SPOKE2 received from [HUB]: Welcome SPOKE2! You are connected.
SPOKE3 received from [HUB]: Welcome SPOKE3! You are connected.

--- Example 3: Broker Pattern (MultipartMessage) ---
Client1 sending message to Client2 via Broker...
Broker received from [CLIENT1]: forward to [CLIENT2] -> Hello Client2, this is Client1!
Client2 received from [CLIENT1] (via BROKER): Hello Client2, this is Client1!

Client2 replying to Client1 via Broker...
Client1 received from [CLIENT2] (via BROKER): Got your message! Reply from Client2.


All examples completed!
```

### ProxySample

XPub-XSub Proxy 패턴을 시연하는 메시지 브로커 샘플입니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runProxy
```

**동작 방식:**

1. **Proxy (XSub-XPub 소켓)**
   - Frontend (XSUB): `tcp://*:5559`에 바인딩하여 퍼블리셔들이 연결
   - Backend (XPUB): `tcp://*:5560`에 바인딩하여 구독자들이 연결
   - `Proxy.start(frontend, backend)`로 메시지와 구독 정보를 양방향 포워딩
   - 퍼블리셔와 구독자 간의 중개자 역할 수행

2. **Publishers (PUB 소켓)**
   - Publisher-1: "weather" 토픽으로 10개의 메시지 발행
   - Publisher-2: "sports" 토픽으로 10개의 메시지 발행
   - 프록시의 Frontend (XSUB)에 연결
   - 800ms 간격으로 메시지 전송

3. **Subscribers (SUB 소켓)**
   - Subscriber-1: "weather" 토픽만 구독
   - Subscriber-2: "sports" 토픽만 구독
   - Subscriber-3: "weather"와 "sports" 두 토픽 모두 구독
   - 프록시의 Backend (XPUB)에 연결
   - 15초 수신 타임아웃 설정

**프록시 패턴의 장점:**
- 퍼블리셔와 구독자의 완전한 분리 (Decoupling)
- 동적으로 퍼블리셔/구독자 추가/제거 가능
- 구독 정보가 프록시를 통해 자동으로 퍼블리셔에게 전달됨
- 네트워크 토폴로지를 간단하게 유지 (모든 연결이 프록시로 집중)

**메시지 흐름:**
```
Publishers → Frontend (XSUB) → Proxy → Backend (XPUB) → Subscribers
                                  ↓
                        구독 정보 역방향 전달
```

**출력 예시:**

```
JVM-ZMQ XPub-XSub Proxy Pattern Sample
======================================

Architecture:
  Publishers -> XSub (Frontend) -> Proxy -> XPub (Backend) -> Subscribers

This sample demonstrates:
  - XSub socket receiving from multiple publishers
  - XPub socket distributing to multiple subscribers
  - Built-in Proxy forwarding messages and subscriptions
  - Dynamic subscription handling

[Proxy] Starting XPub-XSub proxy...
[Proxy] Frontend XSub bound to tcp://*:5559 (for publishers)
[Proxy] Backend XPub bound to tcp://*:5560 (for subscribers)
[Proxy] Proxy running - forwarding messages and subscriptions...

[Publisher-1] Starting...
[Publisher-1] Connected to proxy frontend (tcp://localhost:5559)
[Publisher-1] Publishing topic: 'weather'

[Publisher-2] Starting...
[Publisher-2] Connected to proxy frontend (tcp://localhost:5559)
[Publisher-2] Publishing topic: 'sports'

[Subscriber-1] Starting...
[Subscriber-1] Connected to proxy backend (tcp://localhost:5560)
[Subscriber-1] Subscribed to topic: 'weather'

[Subscriber-2] Starting...
[Subscriber-2] Connected to proxy backend (tcp://localhost:5560)
[Subscriber-2] Subscribed to topic: 'sports'

[Subscriber-3] Starting...
[Subscriber-3] Connected to proxy backend (tcp://localhost:5560)
[Subscriber-3] Subscribed to topic: 'weather'
[Subscriber-3] Subscribed to topic: 'sports'

[Publisher-1] Sent: weather Update #1 from Publisher-1
[Subscriber-1] Received: weather Update #1 from Publisher-1
[Subscriber-3] Received: weather Update #1 from Publisher-1

[Publisher-2] Sent: sports Update #1 from Publisher-2
[Subscriber-2] Received: sports Update #1 from Publisher-2
[Subscriber-3] Received: sports Update #1 from Publisher-2

...

[Subscriber-1] Received 10 messages. Unsubscribing...
[Subscriber-1] Unsubscribed from topic: 'weather'
[Subscriber-1] Completed

[Subscriber-2] Received 10 messages. Unsubscribing...
[Subscriber-2] Unsubscribed from topic: 'sports'
[Subscriber-2] Completed

[Subscriber-3] Received 15 messages. Unsubscribing...
[Subscriber-3] Unsubscribed from topic: 'weather'
[Subscriber-3] Unsubscribed from topic: 'sports'
[Subscriber-3] Completed

All subscribers completed.
```

### PollerSample

Poller를 사용하여 여러 소켓을 동시에 모니터링하는 샘플입니다. 비블로킹 I/O와 타임아웃 처리를 시연합니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runPoller
```

**동작 방식:**

1. **Receivers (PULL 소켓)**
   - Receiver 1: `tcp://*:5561`에 바인딩
   - Receiver 2: `tcp://*:5562`에 바인딩
   - Poller를 사용하여 두 소켓의 읽기 가능 이벤트를 동시에 모니터링

2. **Senders (PUSH 소켓)**
   - Sender-1: 300ms 간격으로 10개의 메시지 전송
   - Sender-2: 500ms 간격으로 10개의 메시지 전송
   - 데몬 스레드로 실행되어 백그라운드에서 메시지 전송

3. **Polling Loop**
   - `Poller.poll(pollItems, 1000)`: 1초 타임아웃으로 소켓 이벤트 대기
   - `pollItem.isReadable()`: 각 소켓의 읽기 가능 여부 확인
   - 최대 20개의 메시지를 수신할 때까지 폴링 계속

**Poller의 장점:**
- 여러 소켓을 효율적으로 동시 모니터링 (select/poll 시스템 콜 활용)
- 블로킹 없이 메시지 수신 가능
- 타임아웃으로 무한 대기 방지
- 이벤트 기반 아키텍처 구현에 적합

**출력 예시:**

```
JVM-ZMQ Poller Sample
====================

This sample demonstrates:
  - Polling multiple sockets simultaneously
  - Non-blocking receive with timeout
  - Handling multiple message sources

[Main] Receiver 1 bound to tcp://*:5561
[Main] Receiver 2 bound to tcp://*:5562

[Sender-2] Connected to tcp://localhost:5562
[Sender-1] Connected to tcp://localhost:5561
[Main] Starting to poll both receivers...

[Receiver-1] Message #1 from Sender-1
[Receiver-2] Message #1 from Sender-2
[Receiver-1] Message #2 from Sender-1
[Receiver-2] Message #2 from Sender-2
[Receiver-1] Message #3 from Sender-1
[Receiver-1] Message #4 from Sender-1
[Receiver-2] Message #3 from Sender-2
[Receiver-1] Message #5 from Sender-1
[Receiver-2] Message #4 from Sender-2
[Receiver-1] Message #6 from Sender-1
[Receiver-1] Message #7 from Sender-1
[Receiver-2] Message #5 from Sender-2
[Receiver-1] Message #8 from Sender-1
[Receiver-1] Message #9 from Sender-1
[Receiver-2] Message #6 from Sender-2
[Receiver-1] Message #10 from Sender-1
[Receiver-2] Message #7 from Sender-2
[Sender-1] Done sending
[Receiver-2] Message #8 from Sender-2
[Receiver-2] Message #9 from Sender-2
[Receiver-2] Message #10 from Sender-2

[Main] Received 20 total messages
[Main] Done
[Sender-2] Done sending
```

### SteerableProxySample

제어 가능한 Proxy 패턴을 시연하는 동적 프록시 샘플입니다. PAUSE/RESUME/TERMINATE 명령으로 런타임에 프록시를 제어할 수 있습니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runSteerableProxy
```

**동작 방식:**

1. **Steerable Proxy (XSub-XPub-PAIR 소켓)**
   - Frontend (XSUB): `tcp://*:5564`에 바인딩하여 퍼블리셔들이 연결
   - Backend (XPUB): `tcp://*:5565`에 바인딩하여 구독자들이 연결
   - Control (PAIR): `inproc://proxy-control`에 바인딩하여 제어 명령 수신
   - `Proxy.startSteerable(frontend, backend, control)`로 제어 가능한 프록시 시작
   - 제어 명령에 따라 메시지 흐름 제어 가능

2. **Controller (PAIR 소켓)**
   - 프록시의 Control 소켓에 연결
   - 3가지 제어 명령 전송:
     - `PAUSE`: 메시지 흐름 일시 정지 (메시지는 큐잉됨)
     - `RESUME`: 메시지 흐름 재개 (큐잉된 메시지가 전달됨)
     - `TERMINATE`: 프록시를 안전하게 종료
   - 타이밍: 2초 흐름 → PAUSE → 2초 대기 → RESUME → 2초 흐름 → TERMINATE

3. **Publisher (PUB 소켓)**
   - 프록시의 Frontend (XSUB)에 연결
   - "news" 토픽으로 15개의 메시지 발행
   - 500ms 간격으로 메시지 전송
   - PAUSE 중에도 계속 전송하며, 메시지는 프록시에서 큐잉됨

4. **Subscriber (SUB 소켓)**
   - 프록시의 Backend (XPUB)에 연결
   - "news" 토픽 구독
   - 1초 수신 타임아웃 설정
   - PAUSE 중에는 메시지 수신이 중단되고, RESUME 시 큐잉된 메시지를 한꺼번에 수신

**Steerable Proxy의 장점:**
- 런타임에 메시지 흐름 제어 가능
- 백프레셔(Backpressure) 메커니즘 구현 가능
- 유지보수 윈도우 동안 트래픽 일시 정지
- 안전한 종료 시나리오 구현 (큐잉된 메시지 손실 방지)
- 트래픽 제어 및 플로우 관리

**제어 명령:**
```
PAUSE      - 메시지 흐름 일시 정지 (큐잉)
RESUME     - 메시지 흐름 재개
TERMINATE  - 프록시 종료
STATISTICS - 통계 정보 요청 (8개의 uint64 값 반환)
```

**메시지 흐름:**
```
Publisher → Frontend (XSUB) → Proxy → Backend (XPUB) → Subscriber
                                ↑
                                |
                            Controller (PAIR)
                            PAUSE/RESUME/TERMINATE
```

**출력 예시:**

```
JVM-ZMQ Steerable Proxy Sample
=============================

This sample demonstrates:
  - Steerable proxy with control socket
  - PAUSE/RESUME/TERMINATE commands
  - Dynamic proxy control at runtime

[Proxy] Starting steerable proxy...
[Proxy] Frontend XSub: tcp://*:5564
[Proxy] Backend XPub:  tcp://*:5565
[Proxy] Control:       inproc://proxy-control

[Publisher] Starting...
[Controller] Starting...
[Subscriber] Starting...
[Controller] Connected to proxy control socket

[Subscriber] Subscribed to 'news'

[Publisher] Sent: news Message #1
[Subscriber] Received: news Message #1
[Publisher] Sent: news Message #2
[Subscriber] Received: news Message #2
[Publisher] Sent: news Message #3
[Subscriber] Received: news Message #3
[Publisher] Sent: news Message #4
[Subscriber] Received: news Message #4

[Controller] >>> Sending PAUSE command
[Controller] Proxy paused - messages will be queued

[Publisher] Sent: news Message #5  <- 큐잉됨
[Publisher] Sent: news Message #6  <- 큐잉됨
[Publisher] Sent: news Message #7  <- 큐잉됨
[Publisher] Sent: news Message #8  <- 큐잉됨

[Controller] >>> Sending RESUME command
[Controller] Proxy resumed - queued messages will flow

[Subscriber] Received: news Message #5  <- 큐잉된 메시지 수신
[Subscriber] Received: news Message #6
[Subscriber] Received: news Message #7
[Subscriber] Received: news Message #8

[Publisher] Sent: news Message #9
[Publisher] Sent: news Message #10
[Subscriber] Waiting for messages...

[Controller] >>> Sending TERMINATE command
[Controller] Proxy termination requested
[Controller] Done

[Proxy] Terminated
[Subscriber] Received 8 messages total
[Subscriber] Done

[Main] Done
```

### MonitorSample

Socket Monitor 패턴을 시연하는 실시간 이벤트 모니터링 샘플입니다. 소켓의 생애주기 이벤트(바인딩, 연결, 연결 해제 등)를 실시간으로 추적합니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runMonitor
```

**동작 방식:**

1. **Hub (ROUTER 소켓)**
   - `tcp://*:5564`에 바인딩, ROUTING_ID = "HUB"
   - 모니터 소켓을 통해 모든 소켓 이벤트를 `inproc://hub-monitor`로 발행
   - 2개의 Spoke와 Router-to-Router 통신 수행

2. **Monitor (PAIR 소켓)**
   - `inproc://hub-monitor`에 연결하여 Hub의 이벤트 수신
   - 백그라운드 스레드에서 이벤트를 지속적으로 폴링
   - 각 이벤트를 타임스탬프, 아이콘, 색상과 함께 포맷팅하여 출력

3. **Spokes (ROUTER 소켓)**
   - Spoke1, Spoke2: Hub에 연결, 각각 "SPOKE1", "SPOKE2" ID 사용
   - Hub에 등록 메시지 전송 및 브로드캐스트 메시지 수신
   - Spoke1은 연결 해제하여 DISCONNECTED 이벤트 생성

**모니터링 이벤트:**
- **LISTENING** (녹색 ▶): 소켓이 바인딩되어 연결 대기 중
- **ACCEPTED** (녹색 ✓): 새로운 인바운드 연결이 수락됨 (FD 표시)
- **CONNECTED** (녹색 ↗): 아웃바운드 연결이 성공적으로 설정됨 (FD 표시)
- **DISCONNECTED** (노란색 ✗): 연결이 종료됨
- **CLOSED** (노란색 ⊗): 소켓이 닫힘
- **BIND_FAILED** (빨간색 ✗): 바인딩 실패 (에러 코드 표시)
- **ACCEPT_FAILED** (빨간색 ✗): 연결 수락 실패 (에러 코드 표시)
- **MONITOR_STOPPED** (청록색 ■): 모니터링 중지됨

**Monitor API 사용법:**
```java
// 모니터링 시작 - 모든 이벤트를 inproc 엔드포인트로 발행
socket.monitor("inproc://monitor-endpoint", SocketMonitorEvent.ALL);

// 또는 특정 이벤트만 모니터링
SocketMonitorEvent events = SocketMonitorEvent.CONNECTED
    .combine(SocketMonitorEvent.DISCONNECTED);
socket.monitor("inproc://monitor-endpoint", events);

// 모니터 소켓으로 이벤트 수신 (PAIR 소켓 사용)
Socket monitor = new Socket(ctx, SocketType.PAIR);
monitor.connect("inproc://monitor-endpoint");

// 이벤트 수신 (2-프레임 메시지)
byte[] eventFrame = monitor.recvBytes();  // 6바이트: event(uint16) + value(int32)
String address = monitor.recvString();     // 엔드포인트 주소

// 이벤트 파싱
SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, address);
System.out.println("Event: " + eventData.event());
System.out.println("Address: " + eventData.address());
System.out.println("Value: " + eventData.value());  // FD 또는 에러 코드

// 모니터링 중지
socket.stopMonitor();
```

**사용 사례:**
- 연결 상태 디버깅 및 진단
- 네트워크 이벤트 로깅
- 연결 문제 해결 및 모니터링
- 시스템 헬스 체크 및 알림

**출력 예시:**

```
=== JVM-ZMQ Socket Monitor Sample ===
Router-to-Router with Real-time Event Monitoring

[Monitor] Attaching to Hub socket (inproc://hub-monitor)
[Monitor] Watching for: All events

[Hub] Binding to tcp://*:5564...
[09:45:04.188] ▶ EVENT: LISTENING
           Address: tcp://0.0.0.0:5564

[Spoke1] Connecting to Hub...
[09:45:04.382] ✓ EVENT: ACCEPTED
           Address: tcp://127.0.0.1:5564
           FD: 18

[Spoke2] Connecting to Hub...
[09:45:04.582] ✓ EVENT: ACCEPTED
           Address: tcp://127.0.0.1:5564
           FD: 20

--- Message Exchange ---
[Hub] Received from [SPOKE1]: Hello from SPOKE1!
[Hub] Received from [SPOKE2]: Hello from SPOKE2!

[Hub] Broadcasting to all spokes...
[SPOKE1] received: Welcome SPOKE1!
[SPOKE2] received: Welcome SPOKE2!

[Spoke1] Disconnecting...
[09:45:04.886] ✗ EVENT: DISCONNECTED
           Address: tcp://127.0.0.1:5564

[Monitor] Stopping...

Sample completed!
```

### RouterBenchmarkSample

ROUTER-to-ROUTER 패턴의 성능 벤치마크 샘플입니다. inproc 전송을 사용하여 최대 성능을 측정하고 메시지 처리 속도를 분석합니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runRouterBenchmark
```

**동작 방식:**

1. **벤치마크 구성**
   - Transport: inproc (in-process, 최저 레이턴시)
   - Message count: 10,000개
   - Message size: 64바이트 (랜덤 데이터)
   - Pattern: 단방향 라우팅 (router1 → router2)

2. **ROUTER 소켓 설정**
   - Router1: "router1" ROUTING_ID로 `inproc://router-bench`에 바인딩
   - Router2: "router2" ROUTING_ID로 `inproc://router-bench`에 연결
   - 명시적 라우팅 ID 설정으로 상대방에게 직접 메시지 전송 가능

3. **메시지 포맷**
   - 전송: `[Target Identity (SEND_MORE)][Message Payload]`
   - 수신: `[Sender Identity][Message Payload...]`
   - Router1이 Router2의 ID를 사용하여 메시지 전송
   - Router2가 Router1의 ID를 포함한 메시지 수신

4. **성능 측정**
   - 총 소요 시간 (밀리초)
   - 메시지당 평균 시간 (마이크로초)
   - 초당 메시지 처리량 (msg/s)
   - System.nanoTime()으로 정밀 측정

**벤치마크 특징:**

- **Inproc 전송**: 네트워크 오버헤드 없이 순수 ZeroMQ 라우팅 성능 측정
- **프레임 기반 통신**: SendFlags.SEND_MORE를 사용한 멀티프레임 메시지
- **Identity 라우팅**: ROUTING_ID로 명시적 피어 지정
- **메모리 관리**: Message 객체의 적절한 close() 처리

**출력 예시:**

```
=== JVM-ZMQ Router-to-Router Performance Benchmark ===

Configuration:
  Transport:     inproc (in-process)
  Message count: 10,000
  Message size:  64 bytes
  Pattern:       One-way routing (router1 → router2)

Starting benchmark...

  Progress: 10,000/10,000

=== Benchmark Results ===
Total time:       1,234 ms
Average per msg:  123.400 μs/op
Throughput:       8,103 msg/s

Benchmark completed successfully!
```

**사용 사례:**

- Router-to-Router 패턴의 성능 측정
- 다른 전송 프로토콜(tcp, ipc)과의 성능 비교 기준
- 메시지 크기에 따른 처리량 분석
- 시스템 최대 처리 능력 파악

### CurveSecuritySample

CURVE 암호화를 사용한 보안 통신 샘플입니다. ZeroMQ의 CURVE 메커니즘을 사용하여 서버-클라이언트 간 암호화 및 인증을 시연합니다.

**실행 방법:**

```bash
# Gradle 태스크를 사용한 실행
./gradlew :zmq-samples:runCurveSecurity
```

**동작 방식:**

1. **CURVE 지원 확인**
   - `Context.has("curve")`로 libzmq의 CURVE 지원 여부 확인
   - CURVE는 libsodium 라이브러리가 필요하며, libzmq 빌드 시 포함되어야 함

2. **키 쌍 생성**
   - 서버 키 쌍: `Curve.generateKeypair()` - 서버의 공개키/비밀키 생성
   - 클라이언트 키 쌍: `Curve.generateKeypair()` - 클라이언트의 공개키/비밀키 생성
   - 모든 키는 Z85 형식(40자 ASCII 문자열)으로 표현됨

3. **공개키 유도**
   - `Curve.derivePublicKey(secretKey)` - 비밀키에서 공개키 계산
   - 생성된 공개키와 유도된 공개키가 일치하는지 검증

4. **서버 설정 (REP 소켓)**
   - `CURVE_SERVER=1`: 서버 모드로 설정
   - `CURVE_SECRETKEY`: 서버의 비밀키 설정
   - `tcp://*:5563`에 바인딩하여 암호화된 연결 대기
   - 클라이언트의 공개키로 클라이언트를 인증

5. **클라이언트 설정 (REQ 소켓)**
   - `CURVE_SERVERKEY`: 서버의 공개키 설정 (서버 인증용)
   - `CURVE_PUBLICKEY`: 클라이언트의 공개키 설정
   - `CURVE_SECRETKEY`: 클라이언트의 비밀키 설정
   - `tcp://localhost:5563`으로 서버에 암호화 연결

6. **암호화 통신**
   - REQ-REP 패턴으로 3회 메시지 교환
   - 모든 메시지는 CURVE로 암호화되어 전송
   - 5초 수신 타임아웃 설정

**CURVE 보안 메커니즘:**
- **CurveCP 프로토콜**: 타원곡선 암호화 (Curve25519) 기반
- **상호 인증**: 서버와 클라이언트가 서로의 공개키로 인증
- **전방향 비밀성(Forward Secrecy)**: 세션 키가 노출되어도 이전 메시지는 안전
- **암호화**: 모든 메시지가 자동으로 암호화되어 전송
- **재생 공격 방지**: nonce 메커니즘으로 재생 공격 차단

**키 형식:**
- Z85 인코딩: 40자의 출력 가능한 ASCII 문자
- 바이너리 키 크기: 32바이트 (256비트)
- 예시: `WC}N(M?OtYffU-466X[rWiBTl4XDUja[sgvIzGM&`

**사용 사례:**
- 신뢰할 수 없는 네트워크에서의 보안 통신
- 클라이언트 인증이 필요한 시스템
- 메시지 도청 및 변조 방지가 필요한 환경
- IoT 기기와 서버 간 보안 통신

**출력 예시:**

```
JVM-ZMQ CURVE Security Sample
============================

This sample demonstrates:
  - CURVE keypair generation
  - Secure encrypted communication
  - Server and client authentication setup

[Setup] CURVE security is available

[Server] Generated keypair:
  Public:  WC}N(M?OtYffU-466X[rWiBTl4XDUja[sgvIzGM&
  Secret:  O(t0p*O.N!y0:}09SLZ[70^W2BLf@MwCuGV!<7w5

[Client] Generated keypair:
  Public:  H*)T8hlWYMqGwyOwx5dZkjmVPrPzATFZ#=dX>0Y6
  Secret:  ql)h1N#v3)5@t@}[yRp8o2@PmsRZTO4#F5Y5Tz1d

[Client] Derived public key from secret: H*)T8hlWYMqGwyOwx5dZkjmVPrPzATFZ#=dX>0Y6
[Client] Keys match: true

[Server] Starting secure server...
[Server] Bound to tcp://*:5563 with CURVE encryption
[Client] Starting secure client...
[Client] Connected to tcp://localhost:5563 with CURVE encryption

[Client] Sent encrypted: Secure request #1
[Server] Received encrypted: Secure request #1
[Server] Sent encrypted: Secure response #1
[Client] Received encrypted: Secure response #1

[Client] Sent encrypted: Secure request #2
[Server] Received encrypted: Secure request #2
[Server] Sent encrypted: Secure response #2
[Client] Received encrypted: Secure response #2

[Client] Sent encrypted: Secure request #3
[Server] Received encrypted: Secure request #3
[Server] Sent encrypted: Secure response #3
[Client] Received encrypted: Secure response #3

[Server] Done
[Client] Done

[Main] Secure communication completed successfully!
```

## 샘플 실행 요약

모든 샘플은 Gradle 태스크를 통해 쉽게 실행할 수 있습니다:

```bash
# 각 샘플별 전용 태스크
./gradlew :zmq-samples:runReqRep           # REQ-REP 샘플
./gradlew :zmq-samples:runPubSub           # PUB-SUB 샘플
./gradlew :zmq-samples:runPushPull         # PUSH-PULL 샘플
./gradlew :zmq-samples:runPair             # PAIR 샘플
./gradlew :zmq-samples:runRouterDealer     # ROUTER-DEALER 샘플
./gradlew :zmq-samples:runRouterToRouter   # ROUTER-to-ROUTER 샘플
./gradlew :zmq-samples:runRouterBenchmark  # ROUTER-to-ROUTER 벤치마크
./gradlew :zmq-samples:runProxy            # Proxy (XPub-XSub) 샘플
./gradlew :zmq-samples:runSteerableProxy   # Steerable Proxy 샘플
./gradlew :zmq-samples:runPoller           # Poller 샘플
./gradlew :zmq-samples:runMonitor          # Socket Monitor 샘플
./gradlew :zmq-samples:runCurveSecurity    # CURVE Security 샘플
```

## 빌드

```bash
# 샘플 빌드
./gradlew :zmq-samples:build

# 샘플 실행
./gradlew :zmq-samples:run
```

## 요구사항

- JDK 22 이상
- libzmq 라이브러리가 시스템에 설치되어 있어야 함

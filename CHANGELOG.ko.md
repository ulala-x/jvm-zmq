[![English](https://img.shields.io/badge/lang-en-red.svg)](CHANGELOG.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](CHANGELOG.ko.md)

# 변경 로그

이 프로젝트의 모든 주요 변경 사항은 이 파일에 문서화됩니다.

이 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 기반으로 하며,
이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [Unreleased]

## [0.2] - 2025-12-22

### 변경됨
- **Breaking Change**: Socket API를 .NET 스타일로 단순화
  - `send()`가 이제 `boolean` 반환 (true=성공, false=EAGAIN)
  - `recv()`가 이제 `int` 반환 (수신된 바이트 수, -1=EAGAIN)
  - 실제 에러는 `ZmqException` throw

### 추가됨
- `tryRecv(byte[])` - 버퍼로 논블로킹 수신
- `tryRecv(Message)` - Message로 논블로킹 수신
- `tryRecvString()` - Optional<String>으로 논블로킹 수신

### 삭제됨
- `SendResult` wrapper 클래스
- `RecvResult` wrapper 클래스
- `recvBytes()` 메서드 (GC 압력을 줄이기 위해 `recv(buffer)` 사용 권장)

## [0.1] - 2024-12-22

### 추가됨
- 초기 릴리즈
- ZeroMQ용 Java 22 FFM (Foreign Function & Memory) API 바인딩
- 모든 소켓 타입: REQ, REP, PUB, SUB, PUSH, PULL, DEALER, ROUTER, PAIR, XPUB, XSUB, STREAM
- CURVE 보안 지원
- 크로스 플랫폼 네이티브 라이브러리 (Windows, Linux, macOS - x64/ARM64)
- 종합적인 JMH 벤치마크
- 모든 ZeroMQ 패턴을 시연하는 13개의 샘플 애플리케이션
- Javadoc API 문서

[Unreleased]: https://github.com/ulala-x/jvm-zmq/compare/v0.2...HEAD
[0.2]: https://github.com/ulala-x/jvm-zmq/compare/v0.1...v0.2
[0.1]: https://github.com/ulala-x/jvm-zmq/releases/tag/v0.1

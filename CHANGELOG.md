[![English](https://img.shields.io/badge/lang-en-red.svg)](CHANGELOG.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](CHANGELOG.ko.md)

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Breaking Change**: Simplified Socket API to .NET style
  - `send()` now returns `boolean` (true=success, false=EAGAIN)
  - `recv()` now returns `int` (bytes received, -1=EAGAIN)
  - Real errors throw `ZmqException`

### Added
- `tryRecv(byte[])` - Non-blocking receive to buffer
- `tryRecv(Message)` - Non-blocking receive to Message
- `tryRecvString()` - Non-blocking receive as Optional<String>

### Removed
- `SendResult` wrapper class
- `RecvResult` wrapper class
- `recvBytes()` methods (use `recv(buffer)` instead to avoid GC pressure)

## [0.1] - 2024-12-22

### Added
- Initial release
- Java 22 FFM (Foreign Function & Memory) API bindings for ZeroMQ
- All socket types: REQ, REP, PUB, SUB, PUSH, PULL, DEALER, ROUTER, PAIR, XPUB, XSUB, STREAM
- CURVE security support
- Cross-platform native libraries (Windows, Linux, macOS - x64/ARM64)
- Comprehensive JMH benchmarks
- 13 sample applications demonstrating all ZeroMQ patterns
- Javadoc API documentation

[Unreleased]: https://github.com/ulala-x/jvm-zmq/compare/v0.1...HEAD
[0.1]: https://github.com/ulala-x/jvm-zmq/releases/tag/v0.1

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-21

### Added
- **Asynchronous BLE Client (`cmf_watch_client.CmfWatchClient`)**: Complete Bleak-powered GATT driver for discovery, connection, pairing, and health telemetry retrieval.
- **Cryptographic Suite (`cmf_watch_client.crypto`)**: AES-128-CBC encryption/decryption with PKCS7 padding and fixed IV (`5051525354555657606162636465665A`), IEEE 802.3 CRC32 Little-Endian checksums, and SHA-256 key derivation (`authkey` and `sessionKey`).
- **Protocol Framer (`cmf_watch_client.protocol`)**: `0xF5` 11-byte Big-Endian frame encoder, MTU chunker, and multi-part frame reassembler (`FrameAssembler`).
- **Telemetry Decoders (`cmf_watch_client.health`)**: Little-Endian decoders for steps, distance, calories, heart rate, resting HR, SpO2, stress, sleep sessions, and workout summary/GPS tracks into Pydantic v2 data models.
- **Storage Abstraction (`cmf_watch_client.storage`)**: Extensible `BaseStorageExporter` interface and `JSONStorageExporter` implementation.
- **CLI Utility (`cmf-watch-client`)**: Subcommands `scan`, `pair`, and `sync`.
- **Test Suite (`tests/`)**: Automated pytest suite with 100% test coverage across crypto, framing, health decoders, and storage exporters using sanitized synthetic fixtures.
- **Protocol & RE Documentation (`docs/`)**: Specifications covering GATT architecture, packet opcodes, cryptography, binary payload schemas, Nothing X v3.7.3 static reversing findings, and citations.

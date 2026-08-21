# Security & Privacy Policy

## 1. Scope & Intent

The **CMF Watch Client** is an independent, open-source library designed for personal interoperability and ownership of user-generated wearable health data.

This project **does not**:
- Bypass cloud authorization or defeat DRM.
- Circumvent user-facing security prompts on the device screen.
- Exfiltrate private data to unauthorized third-party services.

---

## 2. Sensitive Data & Privacy Assurance

This repository is strictly configured to protect user privacy:
- Real Bluetooth MAC addresses, secret credentials, and session keys are ignored via `.gitignore` and never committed.
- Unit tests run against sanitized synthetic binary payloads (`tests/fixtures/sample_payloads.py`).
- No personal health telemetry or location data is checked into revision control.

---

## 3. Reporting Security Vulnerabilities

If you discover a security vulnerability or privacy flaw within this library:
1. Please do **NOT** open a public GitHub issue detailing the vulnerability.
2. Report the vulnerability privately to the project maintainers.
3. Provide a clear description and steps to reproduce using synthetic test vectors.

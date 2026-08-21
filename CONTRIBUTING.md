# Contributing to CMF Watch Client

Thank you for your interest in contributing to the **CMF Watch Client**!

This project is an open-source Python library and BLE driver for CMF and Nothing Smartwatches. We welcome contributions including bug fixes, protocol analysis, new feature support, and documentation improvements.

---

## 1. Development Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/joshuapassos/CMF-Watch-Pro-2-BLE-Protocol.git
   cd CMF-Watch-Pro-2-BLE-Protocol
   ```

2. **Create a Virtual Environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install Package in Editable Mode with Dev Dependencies**:
   ```bash
   pip install -e ".[dev]"
   ```

4. **Run Test Suite**:
   ```bash
   python -m pytest tests/
   ```

---

## 2. Privacy & Security Rules (STRICT)

When submitting Pull Requests or Issues:

- **NEVER submit real health, biometric, or GPS location data**.
- **NEVER submit real device MAC addresses or authentication key secrets**.
- **NEVER submit proprietary APK files, DEX binaries, or private packet captures**.
- Always use sanitized synthetic fixtures (`tests/fixtures/sample_payloads.py`) for tests and protocol demonstrations.

---

## 3. Pull Request Guidelines

1. Ensure all new features or bug fixes include corresponding unit tests in `tests/`.
2. Maintain type hints and Google/PEP-8 style docstrings across all modules.
3. Verify that `pytest` passes with 100% success before opening a PR.

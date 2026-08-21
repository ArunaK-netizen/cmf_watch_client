# Primary References & Prior Art

This implementation builds upon reverse-engineering research and prior open-source work:

1. **CMF Watch Pro 2 BLE Protocol Repository**:  
   [https://github.com/joshuapassos/CMF-Watch-Pro-2-BLE-Protocol](https://github.com/joshuapassos/CMF-Watch-Pro-2-BLE-Protocol)  
   *Established GATT UUID layouts, 0xF5 11-byte framing, AES-128-CBC fixed IV, and dial authoring container structures.*

2. **fmc_go — Independent Go Client Implementation**:  
   [https://github.com/freethinkel/fmc_go](https://github.com/freethinkel/fmc_go)  
   [https://github.com/freethinkel/fmc_go/blob/main/docs/cmf-protocol.md](https://github.com/freethinkel/fmc_go/blob/main/docs/cmf-protocol.md)  
   *Extracted protocol opcodes, shell secret pairing handshake, and health synchronization payload specs.*

3. **Gadgetbridge CMF Implementation**:  
   `nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro`  
   *Reference Java implementation for CMF Watch Pro and Pro 2 coordinators, frame chunking, and sample providers.*

4. **CMF Watch Firmware RE**:  
   [https://github.com/whatotter/cmf-watch-firmware](https://github.com/whatotter/cmf-watch-firmware)  
   *Decompiled ATS3089C firmware binaries (v1.0.0.73).*

5. **Nothing X Smartwatch App Reverse Engineering Write-up**:  
   [https://ambraglow.org/blog/nothing-x/a-look-into-the-nothing-x-smartwatch-app/](https://ambraglow.org/blog/nothing-x/a-look-into-the-nothing-x-smartwatch-app/)  
   *Frida hooking methodologies and logcat intercept patterns for Nothing X.*

6. **Official Nothing Product Data Information**:  
   [https://nothing.tech/pages/product-data-information](https://nothing.tech/pages/product-data-information)  
   *Official technical descriptions of CMF smartwatch health metrics and data storage formats.*

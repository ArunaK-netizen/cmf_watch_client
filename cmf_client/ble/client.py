"""
Asynchronous BLE Client for Nothing / CMF Smartwatches powered by Bleak.
Handles GATT connection, shell pairing, encrypted session key derivation,
time sync, and health data synchronization.
"""

import asyncio
from datetime import datetime, timezone
import logging
import struct
import time
from typing import Callable, Dict, List, Optional, Tuple, Union

from bleak import BleakClient, BleakScanner
from bleak.backends.device import BLEDevice

from ..crypto.cipher import decrypt_aes_cbc, encrypt_aes_cbc
from ..crypto.keys import derive_auth_key, derive_session_key, generate_random_bytes, sign_challenge
from ..health.decoders import (
    decode_activity_data,
    decode_heart_rate,
    decode_resting_heart_rate,
    decode_sleep_data,
    decode_spo2,
    decode_stress,
    decode_workout_gps,
    decode_workout_summary,
)
from ..health.models import (
    ActivitySample,
    HeartRateSample,
    RestingHeartRateSample,
    SleepSession,
    SpO2Sample,
    StressSample,
    WorkoutGpsPoint,
    WorkoutSummary,
)
from ..protocol.frame import FrameAssembler, build_frames
from ..protocol.opcodes import CmfOpcode
from .constants import (
    DEFAULT_MTU,
    UUID_CHAR_CMD_NOTIFY,
    UUID_CHAR_CMD_WRITE,
    UUID_CHAR_SHELL_NOTIFY,
    UUID_CHAR_SHELL_WRITE,
    UUID_SERVICE_CMD,
)

logger = logging.getLogger("cmf_client.ble")


class CmfWatchClient:
    """
    Production BLE client communicating directly with a CMF / Nothing smartwatch.
    """

    def __init__(
        self,
        device_address_or_ble_device: Union[str, BLEDevice],
        authkey: Optional[bytes] = None,
        phone_model: str = "Personal Life OS Client",
    ):
        """
        Initialize CMF Watch Client.

        Args:
            device_address_or_ble_device: Bluetooth MAC address or Bleak BLEDevice object.
            authkey: 16-byte persisted pairing key (if previously paired).
            phone_model: Device identifier sent during handshake.
        """
        self.target = device_address_or_ble_device
        self.authkey = authkey
        self.session_key: Optional[bytes] = authkey  # Initially set to authkey before nonce exchange
        self.phone_model = phone_model

        self._client: Optional[BleakClient] = None
        self._assembler = FrameAssembler()
        self._response_futures: Dict[Union[CmfOpcode, Tuple[int, int]], asyncio.Future] = {}
        self._shell_future: Optional[asyncio.Future] = None
        self._secret: Optional[bytes] = None
        self.is_authenticated: bool = False

        # Raw received health telemetry buffers
        self.raw_activity_bytes = bytearray()
        self.raw_hr_bytes = bytearray()
        self.raw_resting_hr_bytes = bytearray()
        self.raw_spo2_bytes = bytearray()
        self.raw_stress_bytes = bytearray()
        self.raw_sleep_bytes = bytearray()
        self.raw_workout_summary_bytes = bytearray()
        self.raw_workout_gps_bytes = bytearray()

    @classmethod
    async def discover(cls, timeout: float = 10.0, name_prefix: str = "CMF Watch") -> Optional[BLEDevice]:
        """Discover advertising CMF / Nothing smartwatch peripheral."""
        logger.info(f"Scanning for BLE peripherals matching prefix '{name_prefix}'...")
        devices = await BleakScanner.discover(timeout=timeout)
        for d in devices:
            if d.name and (name_prefix.lower() in d.name.lower() or "watch" in d.name.lower()):
                logger.info(f"Discovered matching watch: {d.name} ({d.address})")
                return d
        return None

    async def connect(self, timeout: float = 20.0) -> bool:
        """Establish GATT connection and subscribe to notification channels."""
        logger.info(f"Connecting to watch GATT peripheral ({self.target})...")
        self._client = BleakClient(self.target, timeout=timeout)
        await self._client.connect()

        if not self._client.is_connected:
            logger.error("Failed to connect to GATT peripheral")
            return False

        logger.info("Connected. Subscribing to command and shell notifications...")
        await self._client.start_notify(str(UUID_CHAR_CMD_NOTIFY), self._on_command_notification)
        
        # Try starting shell notification if characteristic exists
        try:
            await self._client.start_notify(str(UUID_CHAR_SHELL_NOTIFY), self._on_shell_notification)
        except Exception as e:
            logger.debug(f"Shell notification subscription optional: {e}")

        logger.info("GATT connection established successfully.")
        return True

    async def disconnect(self) -> None:
        """Disconnect GATT peripheral connection cleanly."""
        if self._client and self._client.is_connected:
            logger.info("Disconnecting GATT peripheral...")
            await self._client.disconnect()
        self.is_authenticated = False

    async def pair_first_time(self) -> bytes:
        """
        Perform initial first-time pairing exchange via shell AT GETSECRET command.
        
        Returns:
            16-byte derived long-term pairing key (authkey).
        """
        if not self._client or not self._client.is_connected:
            raise RuntimeError("Client must be connected before pairing")

        logger.info("Initiating first-time pairing via shell 'AT GETSECRET'...")
        self._shell_future = asyncio.get_event_loop().create_future()
        await self._client.write_gatt_char(str(UUID_CHAR_SHELL_WRITE), b"AT GETSECRET\r\n")

        shell_reply = await asyncio.wait_for(self._shell_future, timeout=15.0)
        # Parse GETSECRET:<32-hex-chars>,OK
        if "GETSECRET:" not in shell_reply or ",OK" not in shell_reply:
            raise ValueError(f"Unexpected secret response from watch: '{shell_reply}'")

        secret_hex = shell_reply.split("GETSECRET:")[1].split(",OK")[0].strip()
        self._secret = bytes.fromhex(secret_hex)
        logger.info(f"Successfully obtained 16-byte device secret from watch.")

        # Phase 1: AUTH_PAIR_REQUEST
        rnd1 = generate_random_bytes(16)
        signed1 = sign_challenge(rnd1, self._secret)
        pair_payload = rnd1 + signed1

        logger.info("Sending plaintext AUTH_PAIR_REQUEST (cmd: FFFF 8047)...")
        reply_opcode, reply_payload = await self.send_command(
            CmfOpcode.AUTH_PAIR_REQUEST,
            pair_payload,
            wait_for_reply=True,
            reply_opcode=CmfOpcode.AUTH_PAIR_REPLY,
        )

        if len(reply_payload) < 48:
            raise ValueError(f"AUTH_PAIR_REPLY payload length {len(reply_payload)} is invalid")

        rnd2 = reply_payload[:16]
        signed2 = reply_payload[16:48]

        # Verify watch signature
        expected_signed2 = sign_challenge(rnd2, self._secret)
        if signed2 != expected_signed2:
            raise PermissionError("Watch challenge signature mismatch during pairing!")

        logger.info("Watch pairing challenge signature verified.")

        # Compute authkey (K1)
        self.authkey = derive_auth_key(rnd1, rnd2, self._secret)
        self.session_key = self.authkey
        logger.info(f"Derived long-term authkey: {self.authkey.hex()}")
        return self.authkey

    async def authenticate_session(self) -> bool:
        """
        Execute session handshake sequence and derive sessionKey.
        Works for both fresh pairings and reconnects with stored authkey.
        """
        if not self.authkey:
            raise ValueError("No authkey available. Call pair_first_time() first.")

        self.session_key = self.authkey
        logger.info("Starting session handshake (AUTH_PHONE_NAME)...")

        # Step 1: AUTH_PHONE_NAME (cmd: FFFF 8049) -> expect AUTH_WATCH_MAC (FFFF 0049)
        model_payload = b"\xa5" + self.phone_model.encode("utf-8")
        await self.send_command(
            CmfOpcode.AUTH_PHONE_NAME,
            model_payload,
            wait_for_reply=True,
            reply_opcode=CmfOpcode.AUTH_WATCH_MAC,
        )

        # Step 2: AUTH_NONCE_REQUEST (cmd: FFFF 804B) -> expect AUTH_NONCE_REPLY (FFFF 004C)
        logger.info("Requesting session nonce (AUTH_NONCE_REQUEST)...")
        _, nonce_reply = await self.send_command(
            CmfOpcode.AUTH_NONCE_REQUEST,
            b"\xa5",
            wait_for_reply=True,
            reply_opcode=CmfOpcode.AUTH_NONCE_REPLY,
        )

        # Step 3: Derive sessionKey
        self.session_key = derive_session_key(nonce_reply, self.authkey)
        logger.info(f"Session key established: {self.session_key.hex()}")

        # Step 4: AUTHENTICATED_CONFIRM_REQUEST (cmd: FFFF 804D) -> expect AUTHENTICATED_CONFIRM_REPLY (FFFF 0004)
        logger.info("Confirming session authentication (AUTHENTICATED_CONFIRM_REQUEST)...")
        await self.send_command(
            CmfOpcode.AUTHENTICATED_CONFIRM_REQUEST,
            b"\xa5",
            wait_for_reply=True,
            reply_opcode=CmfOpcode.AUTHENTICATED_CONFIRM_REPLY,
        )

        self.is_authenticated = True
        logger.info("Session authentication complete. Device state: Initialized.")

        # Post-Auth mandatory initialization: Set Time
        await self.set_device_time()
        return True

    async def set_device_time(self, dt: Optional[datetime] = None) -> None:
        """
        Send TIME command (FFFF 8004) with current epoch seconds and UTC offset.
        Mandatory post-auth command required to unblock health data queries.
        """
        if dt is None:
            dt = datetime.now(timezone.utc)

        epoch_sec = int(dt.timestamp())
        # Calculate UTC offset in milliseconds
        utc_offset_ms = int(dt.utcoffset().total_seconds() * 1000) if dt.utcoffset() else 0

        payload = struct.pack(">ii", epoch_sec, utc_offset_ms)
        logger.info(f"Setting watch time to {dt.isoformat()} (epoch={epoch_sec}, offset_ms={utc_offset_ms})...")
        await self.send_command(CmfOpcode.TIME, payload, wait_for_reply=False)
        await asyncio.sleep(0.5)  # Allow watch MCU to process TIME initialization

    async def fetch_battery(self, timeout: float = 3.0) -> Tuple[int, bool]:
        """Fetch watch battery level percentage and charging state."""
        try:
            _, reply = await self.send_command(
                CmfOpcode.BATTERY_GET,
                b"\xa5",
                wait_for_reply=True,
                reply_opcode=CmfOpcode.BATTERY,
                timeout=timeout,
            )
            if len(reply) >= 2:
                level = reply[0]
                charging = reply[1] == 1
                logger.info(f"Watch Battery: {level}% (Charging: {charging})")
                return level, charging
        except asyncio.TimeoutError:
            logger.warning("Battery fetch timed out; continuing session...")
        except Exception as e:
            logger.warning(f"Battery fetch error: {e}")
        return 0, False

    async def sync_health_data(self, timeout: float = 30.0) -> Dict[str, list]:
        """
        Trigger historical health data retrieval via ACTIVITY_FETCH_1 and 2.
        Listens to stream notifications and returns normalized Pydantic metric models.
        """
        logger.info("Initiating historical health data sync sequence...")
        
        # Clear telemetry buffers
        self.raw_activity_bytes.clear()
        self.raw_hr_bytes.clear()
        self.raw_resting_hr_bytes.clear()
        self.raw_spo2_bytes.clear()
        self.raw_stress_bytes.clear()
        self.raw_sleep_bytes.clear()
        self.raw_workout_summary_bytes.clear()
        self.raw_workout_gps_bytes.clear()

        # Step 1: Send ACTIVITY_FETCH_1 (FFFF 8005), await ACK_1 (FFFF 0005)
        logger.info("Sending ACTIVITY_FETCH_1...")
        await self.send_command(
            CmfOpcode.ACTIVITY_FETCH_1,
            b"\xa5",
            wait_for_reply=True,
            reply_opcode=CmfOpcode.ACTIVITY_FETCH_ACK_1,
        )

        # Step 2: Send ACTIVITY_FETCH_2 (FFFF 9057), await ACK_2 (FFFF A057)
        logger.info("Sending ACTIVITY_FETCH_2...")
        await self.send_command(
            CmfOpcode.ACTIVITY_FETCH_2,
            b"\xa5",
            wait_for_reply=True,
            reply_opcode=CmfOpcode.ACTIVITY_FETCH_ACK_2,
        )

        logger.info(f"Listening for health telemetry streams for {timeout}s...")
        await asyncio.sleep(timeout)

        # Decode accumulated buffers
        activity_samples = decode_activity_data(bytes(self.raw_activity_bytes))
        hr_samples = decode_heart_rate(bytes(self.raw_hr_bytes))
        resting_hr_samples = decode_resting_heart_rate(bytes(self.raw_resting_hr_bytes))
        spo2_samples = decode_spo2(bytes(self.raw_spo2_bytes))
        stress_samples = decode_stress(bytes(self.raw_stress_bytes))
        sleep_sessions = decode_sleep_data(bytes(self.raw_sleep_bytes))
        workout_summaries = decode_workout_summary(bytes(self.raw_workout_summary_bytes))
        gps_points = decode_workout_gps(bytes(self.raw_workout_gps_bytes))

        logger.info(
            f"Health Sync Completed: {len(activity_samples)} activity, {len(hr_samples)} HR, "
            f"{len(spo2_samples)} SpO2, {len(stress_samples)} stress, {len(sleep_sessions)} sleep, "
            f"{len(workout_summaries)} workouts."
        )

        return {
            "activity": activity_samples,
            "heart_rate": hr_samples,
            "resting_heart_rate": resting_hr_samples,
            "spo2": spo2_samples,
            "stress": stress_samples,
            "sleep": sleep_sessions,
            "workout_summary": workout_summaries,
            "workout_gps": gps_points,
        }

    async def send_command(
        self,
        opcode: Union[CmfOpcode, Tuple[int, int]],
        payload: bytes = b"",
        wait_for_reply: bool = False,
        reply_opcode: Optional[Union[CmfOpcode, Tuple[int, int]]] = None,
        timeout: float = 10.0,
    ) -> Tuple[Union[CmfOpcode, Tuple[int, int]], bytes]:
        """
        Build, fragment, and send framed BLE command to the watch.
        Optionally await reply notification matching reply_opcode.
        """
        if not self._client or not self._client.is_connected:
            raise RuntimeError("Client is not connected")

        cmd1 = opcode.cmd1 if isinstance(opcode, CmfOpcode) else opcode[0]
        cmd2 = opcode.cmd2 if isinstance(opcode, CmfOpcode) else opcode[1]

        frames = build_frames(
            cmd1, cmd2, payload, session_key=self.session_key, mtu=DEFAULT_MTU
        )

        target_reply = reply_opcode if reply_opcode else opcode
        fut = None
        if wait_for_reply:
            fut = asyncio.get_event_loop().create_future()
            self._response_futures[target_reply] = fut

        for frame in frames:
            await self._client.write_gatt_char(str(UUID_CHAR_CMD_WRITE), frame)

        if wait_for_reply and fut:
            try:
                result = await asyncio.wait_for(fut, timeout=timeout)
                return result
            finally:
                self._response_futures.pop(target_reply, None)

        return opcode, b""

    def _on_shell_notification(self, sender: int, data: bytes) -> None:
        """Callback for ASCII responses on shell channel (77d4ff02)."""
        try:
            text = data.decode("utf-8", errors="ignore").strip()
            logger.debug(f"Shell notification received: '{text}'")
            if self._shell_future and not self._shell_future.done():
                self._shell_future.set_result(text)
        except Exception as e:
            logger.error(f"Error handling shell notification: {e}")

    def _on_command_notification(self, sender: int, data: bytes) -> None:
        """Callback for framed responses on command channel (0000fff1)."""
        try:
            res = self._assembler.process_raw_notification(data, session_key=self.session_key)
            if res is None:
                return  # Awaiting more chunks

            opcode, payload = res
            logger.debug(f"Command notification arrived: opcode={opcode}, payload_len={len(payload)}")

            # Route incoming data streams to health buffers
            if opcode == CmfOpcode.ACTIVITY_DATA:
                self.raw_activity_bytes.extend(payload)
            elif opcode in (CmfOpcode.HEART_RATE_MANUAL_AUTO, CmfOpcode.HEART_RATE_WORKOUT):
                self.raw_hr_bytes.extend(payload)
            elif opcode == CmfOpcode.HEART_RATE_RESTING:
                self.raw_resting_hr_bytes.extend(payload)
            elif opcode == CmfOpcode.SPO2:
                self.raw_spo2_bytes.extend(payload)
            elif opcode == CmfOpcode.STRESS:
                self.raw_stress_bytes.extend(payload)
            elif opcode == CmfOpcode.SLEEP_DATA:
                self.raw_sleep_bytes.extend(payload)
            elif opcode in (CmfOpcode.WORKOUT_SUMMARY, CmfOpcode.WORKOUT_SUMMARY_V3):
                self.raw_workout_summary_bytes.extend(payload)
            elif opcode == CmfOpcode.WORKOUT_GPS:
                self.raw_workout_gps_bytes.extend(payload)

            # Resolve awaiting response future if registered
            if opcode in self._response_futures and not self._response_futures[opcode].done():
                self._response_futures[opcode].set_result((opcode, payload))

        except Exception as e:
            logger.error(f"Error processing command notification: {e}")

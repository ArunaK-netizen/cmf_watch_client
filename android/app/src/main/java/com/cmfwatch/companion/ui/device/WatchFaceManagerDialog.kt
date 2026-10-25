package com.cmfwatch.companion.ui.device

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.ui.theme.CmfBackground
import com.cmfwatch.companion.ui.theme.CmfOnPrimary
import com.cmfwatch.companion.ui.theme.CmfOnSurface
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfPrimary
import com.cmfwatch.companion.ui.theme.CmfPrimaryContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceLowest
import com.cmfwatch.companion.ui.theme.CmfTertiaryContainer
import com.cmfwatch.companion.ui.theme.HeadlineFontFamily
import com.cmfwatch.companion.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WatchFacePreset(
    val id: String,
    val name: String,
    val style: String,
    val accentColor: Color,
    val isCircular: Boolean = true,
    val description: String
)

val SampleWatchFaces = listOf(
    WatchFacePreset(
        id = "cmf_digital_bold",
        name = "CMF Digital Bold",
        style = "Dot Matrix Font",
        accentColor = Color(0xFFFF5722),
        description = "Signature Nothing / CMF dot matrix digital clock with activity ring gauges and battery pill."
    ),
    WatchFacePreset(
        id = "studio_bauhaus",
        name = "Studio Bauhaus",
        style = "Minimal Analog",
        accentColor = Color(0xFFE2DFFF),
        description = "Clean geometric analog hands with discrete tick markers for daily minimalism."
    ),
    WatchFacePreset(
        id = "vitals_trio",
        name = "Vitals Trio",
        style = "Health Rings",
        accentColor = Color(0xFF00E676),
        description = "Three concentric rings tracking Move, Exercise, and Stand alongside real-time BPM."
    ),
    WatchFacePreset(
        id = "cyber_matrix",
        name = "Cyber Matrix",
        style = "Amber Monospace",
        accentColor = Color(0xFFFFB300),
        description = "High-contrast tactical dial with date, step progress arc, and battery percentage."
    )
)

/**
 * Full Watch Face Studio & Sideloading Dialog.
 * Allows picking presets, importing local custom `.bin` watchfaces,
 * and reviewing the exact Actions ATS3085/ATS3089 build guidelines.
 */
@Composable
fun WatchFaceManagerDialog(
    onDismiss: () -> Unit,
    onFlashWatchface: (ByteArray, (Float) -> Unit, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFace by remember { mutableStateOf(SampleWatchFaces[0]) }
    var importedFileName by remember { mutableStateOf<String?>(null) }
    var importedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showDevGuide by remember { mutableStateOf(false) }

    var isFlashing by remember { mutableStateOf(false) }
    var flashProgress by remember { mutableFloatStateOf(0f) }
    var flashStatusText by remember { mutableStateOf("") }

    // Android Storage Access Framework (SAF) File Picker for custom .bin watch faces
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.size in 1024..15_000_000) {
                        importedFileBytes = bytes
                        importedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_face.bin"
                        Toast.makeText(context, "Loaded ${importedFileName} (${bytes.size / 1024} KB)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid watch face size (${bytes.size} bytes). Expected 1KB - 15MB .bin", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isFlashing) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = CmfPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Face Studio",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CmfOnSurface
                    )
                }

                IconButton(onClick = { showDevGuide = !showDevGuide }) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = "Build Guide",
                        tint = if (showDevGuide) CmfPrimary else CmfOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                if (showDevGuide) {
                    // Developer Guide Accordion
                    WatchFaceDevGuideView(onBack = { showDevGuide = false })
                } else if (isFlashing) {
                    // Active Flashing Progress View
                    WatchFaceFlashingProgressView(
                        progress = flashProgress,
                        statusText = flashStatusText
                    )
                } else {
                    // Standard Selection View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Select a watch face preset or sideload any custom compiled `.bin` watch face built for CMF Watch Pro 2.",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = CmfOnSurfaceVariant
                            )
                        }

                        // Presets Carousel
                        item {
                            Text(
                                text = "OFFICIAL & CURATED PRESETS",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = CmfOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(SampleWatchFaces) { face ->
                                    val isSelected = (face.id == selectedFace.id && importedFileBytes == null)
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) CmfPrimaryContainer.copy(alpha = 0.15f) else CmfSurfaceLowest)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) CmfPrimary else CmfSurfaceContainer,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selectedFace = face
                                                importedFileBytes = null
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            // Watch Face Preview Circle
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF141416))
                                                    .border(1.dp, face.accentColor.copy(alpha = 0.5f), CircleShape)
                                                    .align(Alignment.CenterHorizontally),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "10:09",
                                                    fontFamily = HeadlineFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = face.accentColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = face.name,
                                                fontFamily = HeadlineFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = CmfOnSurface
                                            )
                                            Text(
                                                text = face.style,
                                                fontFamily = InterFontFamily,
                                                fontSize = 10.sp,
                                                color = CmfOnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sideload Section
                        item {
                            Text(
                                text = "CUSTOM .BIN SIDELOADER",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = CmfOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CmfSurfaceLowest)
                                    .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (importedFileBytes != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = importedFileName ?: "Custom File",
                                                    fontFamily = HeadlineFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = CmfOnSurface
                                                )
                                                Text(
                                                    text = "${importedFileBytes!!.size / 1024} KB · Ready to flash",
                                                    fontFamily = InterFontFamily,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF00E676)
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    importedFileBytes = null
                                                    importedFileName = null
                                                }
                                            ) {
                                                Text("Remove", fontSize = 11.sp)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Test custom watch faces exported from fmc web editor (.bin format):",
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            color = CmfOnSurfaceVariant
                                        )
                                        Button(
                                            onClick = { filePickerLauncher.launch("*/*") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CmfSurfaceContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FileOpen,
                                                contentDescription = null,
                                                tint = CmfOnSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Import Local .bin File",
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = CmfOnSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isFlashing && !showDevGuide) {
                Button(
                    onClick = {
                        isFlashing = true
                        flashProgress = 0.05f
                        flashStatusText = "Initializing transfer with CMF Watch..."

                        // Generate payload or use imported bytes
                        val payload = importedFileBytes ?: generateMockWatchFaceBin(selectedFace)

                        onFlashWatchface(
                            payload,
                            { progress ->
                                flashProgress = progress
                                flashStatusText = when {
                                    progress < 0.2f -> "Handshaking with Watch (Opcode 0x8052)..."
                                    progress < 0.3f -> "Setting Watchface Slot (Opcode 0x9063)..."
                                    progress < 0.95f -> "Streaming Binary Blocks (${(progress * 100).toInt()}%)..."
                                    else -> "Finalizing & Activating (Opcode 0x9065)..."
                                }
                            },
                            { success ->
                                isFlashing = false
                                if (success) {
                                    Toast.makeText(context, "Watch Face Installed & Activated Successfully!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Watch Face transfer failed. Ensure watch is connected.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CmfPrimary)
                ) {
                    Text(
                        text = if (importedFileBytes != null) "Flash Custom .bin" else "Flash ${selectedFace.name}",
                        color = CmfOnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (!isFlashing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = CmfOnSurface)
                }
            }
        }
    )
}

@Composable
private fun WatchFaceFlashingProgressView(
    progress: Float,
    statusText: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.InstallMobile,
                contentDescription = null,
                tint = CmfPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = CmfOnSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CmfPrimary,
                trackColor = CmfSurfaceContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusText,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = CmfOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun WatchFaceDevGuideView(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BUILD & TEST SPECIFICATIONS",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CmfPrimary
                )
                TextButton(onClick = onBack) { Text("Back", fontSize = 12.sp) }
            }
        }

        item {
            GuideSectionCard(
                title = "1. Binary Format (.bin)",
                body = "CMF Watch Pro & Pro 2 use Actions Semiconductor (ATS3085 / ATS3089) MCU firmware packages. The `.bin` file bundles:\n• Component table (analog hands, digital digits, step gauge, battery)\n• Packed RGB565 / ARGB8888 bitmap arrays\n• Embedded font glyphs & JSON layout map."
            )
        }

        item {
            GuideSectionCard(
                title = "2. Canvas Resolutions",
                body = "• CMF Watch Pro 2: 466 x 466 px (Circular AMOLED, 1.32\")\n• CMF Watch Pro 1: 410 x 502 px (Rectangular AMOLED, 1.96\")"
            )
        }

        item {
            GuideSectionCard(
                title = "3. Recommended Tools to Build & Export",
                body = "• FMC Web Editor: https://fmc.freethinkel.dev\nDrag-and-drop live browser editor with canvas simulator and 1-click `.bin` export.\n• FMC GitHub: github.com/freethinkel/fmc\nDecompiles any existing CMF `.bin` into `face.json` + PNG assets, allowing exact custom edits and recompilation."
            )
        }

        item {
            GuideSectionCard(
                title = "4. BLE Protocol Installation Pipeline",
                body = "1. INIT_1 (`0xFFFF 0x8052`): Notify watch of transfer.\n2. INIT_2 (`0xFFFF 0x9063`): Send file size, CRC32, and slot mode (0xFFFFFFFF = auto-append).\n3. CHUNK_WRITE (`0xFFFF 0x9064`): Stream 256-512 byte raw blocks.\n4. FINISH (`0xFFFF 0x9065`): Finalize install & activate display."
            )
        }
    }
}

@Composable
private fun GuideSectionCard(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CmfSurfaceLowest)
            .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = CmfOnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = CmfOnSurfaceVariant
            )
        }
    }
}

private fun generateMockWatchFaceBin(preset: WatchFacePreset): ByteArray {
    // Generates a mock 16KB binary header simulating an ATS3089 watchface package for testing
    val bytes = ByteArray(16384)
    bytes[0] = 0x41 // 'A'
    bytes[1] = 0x54 // 'T'
    bytes[2] = 0x53 // 'S'
    bytes[3] = 0x30 // '0'
    val nameBytes = preset.name.toByteArray(Charsets.UTF_8)
    System.arraycopy(nameBytes, 0, bytes, 4, minOf(nameBytes.size, 32))
    return bytes
}

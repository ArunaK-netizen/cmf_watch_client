# CMF Watch Companion Design System Specification

This document defines the visual direction, typography hierarchy, color tokens, Material 3 card specs, and data visualization standards for the **CMF Watch Companion Android Application**.

---

## 1. Visual Direction & Aesthetic Philosophy

- **Philosophy**: Minimal, data-focused, futuristic, spacious, highly readable, calm, and technically precise.
- **Aesthetic Inspiration**: Nothing OS / CMF visual language combined with the clean information hierarchy of Stride.
- **Color Palette Strategy**: Pure pitch black backgrounds (`#000000`) for OLED power efficiency, deep charcoal card surfaces (`#121212`), high-contrast crisp white metric typography (`#FFFFFF`), muted gray secondary labels (`#8E8E93`), and restrained health-specific accent colors.

---

## 2. Nothing-Style Typography System (`NType82`)

The application uses the **Nothing N-Type font (`NType82-Regular.otf`)** as its primary visual identity anchor.

| Style Name | Font Family | Weight / Format | Size (sp) | Line Height (sp) | Usage / Context |
|---|---|---|---|---|---|
| **NType82 Display** | NType82-Regular | Regular / Dot-Matrix | 36 sp | 44 sp | Main Screen Titles, Dashboard Greetings |
| **NType82 Metric Huge**| NType82-Regular | Regular | 48 sp | 52 sp | Primary Metric Numbers (e.g. `8,421` Steps, `72` BPM) |
| **NType82 Metric Large**| NType82-Regular | Regular | 28 sp | 34 sp | Secondary Card Metric Values |
| **NType82 Headline** | NType82-Regular | Regular | 20 sp | 26 sp | Card Titles, Section Headers |
| **NType82 Body** | NType82-Regular | Regular | 15 sp | 20 sp | Explanatory Text, Status Messages |
| **NType82 Label** | NType82-Regular | Regular | 12 sp | 16 sp | Metric Units (`BPM`, `km`, `spm`, `kcal`), Chart Axis |
| **NType82 Caption** | NType82-Regular | Regular | 11 sp | 14 sp | Timestamps, Battery Badges, Sync Footers |

---

## 3. Color Tokens & Theme System

```kotlin
// Dark Palette (Default OLED Experience)
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF121212)
val SurfaceVariantDark = Color(0xFF1C1C1E)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFF8E8E93)

// Health Feature Accents
val AccentHeartRate = Color(0xFFFF2800)     // Nothing Red
val AccentSleepDeep = Color(0xFF4A3E85)      // Indigo/Deep Violet
val AccentSleepLight = Color(0xFF5B7FFF)     // Soft Blue
val AccentSleepREM = Color(0xFF8E5AFF)       // Electric Purple
val AccentSleepAwake = Color(0xFFFF9500)     // Amber
val AccentActivity = Color(0xFF30D158)      // Vitality Green
val AccentStressLow = Color(0xFF34C759)      // Mint Green
val AccentStressHigh = Color(0xFFFF3B30)     // Coral Red
val AccentSpO2 = Color(0xFF00C7BE)           // Cyan
```

---

## 4. Component Design Standards

### 4.1 Metric Card Specs
- Surface Color: `SurfaceDark` (`#121212`).
- Corner Radius: 16 dp (Material 3 medium shape).
- Padding: 16 dp inside padding.
- Structure: Top row contains Icon + Title (`NType82 Label`), middle row contains Metric Number (`NType82 Metric`), bottom row contains Subtitle / Trend Badge.

### 4.2 Chart Design Rules
- Background: Minimal grid lines (`#1C1C1E`).
- Stroke Width: 2.5 dp smooth bezier line.
- Gradient Fill: Optional 10% opacity vertical fill beneath line.
- Touch Interaction: Drag crosshair overlay showing exact timestamp and metric reading.

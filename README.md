# NeoCalc (The "Why Do We Exist" Fork)

Originally created as a simple calculator to teach kids how to program. It was simple. It was pure. It added 2 and 2 and got 4.

But then **I** came along. I saw a perfectly functional teaching tool and thought, "You know what this needs? Unnecessary complexity and a bad attitude." So I forked it, injected it with Rust, and stripped it of its innocence.

## Features
*   **Rust Backend**: Powered by `neocalc-core` via UniFFI for high performance and safety.
*   **Fraction Support**: Toggle between precise fractional (e.g., `1/2`) and decimal (e.g., `0.5`) outputs in Settings.
*   **4 Calculator Modes**: Standard, Scientific, Programming, and Financial.
*   **Responsive Layout**: Works in Portrait and Landscape (Split View).
*   **Theming Engine**: Import your own `.css` themes (GTK-compatible).
*   **Dark Mode**: By default, because we respect your eyes.

## Installation / Downloads
You can download the latest **Android APK** from the **[Releases Page](../../releases)**.

1.  Download `neocalc-release.apk`.
2.  Install it on your Android device (you may need to allow installation from unknown sources).
3.  Calculate away your existential dread.

## Versioning Scheme
We use a **Date-Based Versioning** scheme to keep things fresh:

`vYEAR.MONTH-REVISION`

*   **YEAR**: The current year (e.g., `2025`).
*   **MONTH**: The current month (e.g., `12`).
*   **REVISION**: An incrementing number for releases within that month (e.g., `1`, `2`, `5`).

Example: `v2025.12-5` is the 5th release in December 2025.

---

## Theming Guide: How to Create Custom Themes

NeoCalc supports custom themes defined in simple `.css` files. These themes are compatible with the original GTK version of the app.

### How to Import a Theme
1.  Create a `.css` file on your phone (e.g., `my-cool-theme.css`).
2.  Open NeoCalc.
3.  Tap the **Brush Icon** in the top bar.
4.  Tap **"Import Theme"**.
5.  Select your `.css` file.

### CSS Format
The theme parser looks for specific `@define-color` lines.
**Format**: `@define-color KEY #HEXCODE;`

### Theme Keys Reference

| Key | Description | Android UI Mapping |
| :--- | :--- | :--- |
| **Window Colors** | | |
| `window_bg_color` | Main background color of the app. | App Background |
| `window_fg_color` | Default text color. | Default Text |
| **Number Buttons** | | |
| `card_bg_color` | Background for number buttons (0-9). | Secondary Container |
| `card_fg_color` | Text color for numbers. | On Secondary Container |
| **Operator Buttons** | | |
| `warning_bg_color` | Background for operators (+, -, /, x). | Tertiary Container |
| `warning_fg_color` | Text color for operators. | On Tertiary Container |
| **Function Buttons** | | |
| `headerbar_bg_color`| Background for functions (Sin, Cos, Log). | Surface Variant |
| `headerbar_fg_color`| Text color for functions. | On Surface Variant |
| **Accent / Equals** | | |
| `accent_bg_color` | The "Equals" (=) button background. | Primary |
| `accent_fg_color` | Text on the Equals button. | On Primary |
| **Destructive** | | |
| `destructive_bg_color`| Delete / Clear button background. | Error Container |
| `destructive_fg_color`| Delete / Clear button text. | On Error Container |

### Example Theme (`dracula.css`)

```css
@define-color window_bg_color #282a36;
@define-color window_fg_color #f8f8f2;

@define-color headerbar_bg_color #44475a;
@define-color headerbar_fg_color #f8f8f2;

@define-color card_bg_color #6272a4;
@define-color card_fg_color #f8f8f2;

@define-color accent_bg_color #bd93f9;
@define-color accent_fg_color #282a36;

@define-color warning_bg_color #ffb86c;
@define-color warning_fg_color #282a36;

@define-color destructive_bg_color #ff5555;
@define-color destructive_fg_color #282a36;
```

---

## Building from Source

### Prerequisites
*   **Android Studio** (Ladybug or newer recommended)
*   **Rust** (`rustup` installed)
*   **JDK 17**

### Build Commands
Ensure you have initialized the submodules before building:
```bash
git submodule update --init --recursive
```

The build process automatically compiles the Rust backend (via the `mobile_backend` wrapper crate) and generates Kotlin bindings.

```bash
cd android
./gradlew assembleDebug
``` 

The APK will be found in `android/app/build/outputs/apk/debug/app-debug.apk`.

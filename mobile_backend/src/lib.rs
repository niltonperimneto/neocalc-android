uniffi::setup_scaffolding!();

mod update;
pub use update::{check_for_updates, download_apk, DownloadResult, UpdateCheckResult};

mod formatting;
pub use formatting::{format_for_display, NumberLocale};

mod session_wrapper;
pub use session_wrapper::{MobileSessionManager, MobileSessionOverview};

/// Initialize the localization subsystem with the device's locale.
/// Should be called once at app startup with the device's language tag (e.g., "pt-BR", "en-US").
#[uniffi::export]
pub fn init_locale(locale: String) {
    neocalc_core::i18n::init_locale(&locale);
}

/// Structured history item for cleaner UI consumption
#[derive(uniffi::Record, Clone, Debug)]
pub struct HistoryItem {
    pub expression: String,
    pub result: String,
    pub timestamp: u64,
    pub is_error: bool,
}

/// Parse a GTK-style CSS theme file and extract color definitions
#[uniffi::export]
pub fn parse_theme_css(css: String) -> std::collections::HashMap<String, i64> {
    let mut map = std::collections::HashMap::new();
    for line in css.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("@define-color") {
            let parts: Vec<&str> = trimmed.split_whitespace().collect();
            if parts.len() >= 3 {
                let key = parts[1];
                let value_str = parts[2].trim_end_matches(';');
                if let Ok(color) = parse_color(value_str) {
                    map.insert(key.to_string(), color);
                }
            }
        }
    }
    map
}

fn parse_color(hex_str: &str) -> Result<i64, ()> {
    let clean_hex = hex_str.trim_start_matches('#');
    let val = i64::from_str_radix(clean_hex, 16).map_err(|_| ())?;
    // Ensure full opacity if valid hex
    if clean_hex.len() == 6 {
        Ok(val | 0xFF000000)
    } else if clean_hex.len() == 8 {
        Ok(val)
    } else {
        Err(())
    }
}

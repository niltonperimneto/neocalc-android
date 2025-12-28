use std::collections::HashMap;

#[uniffi::export]
pub fn parse_theme_css(css: String) -> HashMap<String, i64> {
    let mut color_map = HashMap::new();

    for line in css.lines() {
        let line = line.trim();
        if line.starts_with("@define-color") {
            // Expected format: @define-color key #hex;
            // Split by whitespace
            let parts: Vec<&str> = line.split_whitespace().collect();
            if parts.len() >= 3 {
                let key = parts[1];
                let hex = parts[2].trim_end_matches(';');
                
                if let Some(parsed) = parse_hex_color(hex) {
                    color_map.insert(key.to_string(), parsed);
                }
            }
        }
    }

    color_map
}

fn parse_hex_color(hex_str: &str) -> Option<i64> {
    let mut clean_hex = hex_str.trim_start_matches('#').to_string();

    // Handle short hex: #abc -> #aabbcc
    if clean_hex.len() == 3 {
        let r = &clean_hex[0..1];
        let g = &clean_hex[1..2];
        let b = &clean_hex[2..3];
        clean_hex = format!("{}{}{}{}{}{}", r, r, g, g, b, b);
    }
    
    // Handle alpha: #RRGGBB -> FFRRGGBB (ARGB)
    if clean_hex.len() == 6 {
        clean_hex = format!("FF{}", clean_hex);
    }

    // Parse as u64 first, then cast to i64 (Kotlin Long expects signed 64-bit for 0xFF...)
    u64::from_str_radix(&clean_hex, 16).ok().map(|v| v as i64)
}

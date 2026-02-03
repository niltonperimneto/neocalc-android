uniffi::setup_scaffolding!();

use num_traits::cast::ToPrimitive;

struct CalculatorState {
    context: neocalc_core::Context,
    buffer: String,
    history: Vec<String>,
    show_fractions: bool,
}

#[derive(thiserror::Error, Debug)]
pub enum CalculatorError {
    #[error("Engine error: {0}")]
    EngineError(String),
}

#[derive(uniffi::Object)]
pub struct Calculator {
    state: std::sync::Mutex<CalculatorState>,
}

#[uniffi::export]
impl Calculator {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            state: std::sync::Mutex::new(CalculatorState {
                context: neocalc_core::Context::new(),
                buffer: String::from("0"),
                history: Vec::new(),
                show_fractions: false,
            }),
        }
    }

    pub fn input(&self, text: String) -> String {
        let mut state = self.state.lock().unwrap();
        if state.buffer == "0" && text != "." {
            state.buffer = text;
        } else {
            state.buffer.push_str(&text);
        }
        state.buffer.clone()
    }

    pub fn clear(&self) -> String {
        let mut state = self.state.lock().unwrap();
        state.buffer = String::from("0");
        state.buffer.clone()
    }

    pub fn backspace(&self) -> String {
        let mut state = self.state.lock().unwrap();
        if state.buffer.len() > 0 {
            state.buffer.pop();
            if state.buffer.is_empty() {
                state.buffer = String::from("0");
            }
        }
        state.buffer.clone()
    }

    pub fn evaluate(&self, _input_arg: Option<String>) -> String {
        let mut state = self.state.lock().unwrap();
        let expr = state.buffer.clone();

        state.history.push(format!("{} = ...", expr));

        match neocalc_core::evaluate(&expr, &mut state.context) {
            Ok(num) => {
                let result_str = if state.show_fractions {
                    neocalc_core::utils::format_number(num, false)
                } else {
                    match num {
                        neocalc_core::engine::types::Number::Rational(r) => {
                            if let Some(f) = r.to_f64() {
                                neocalc_core::utils::format_float(f)
                            } else {
                                neocalc_core::utils::format_number(
                                    neocalc_core::engine::types::Number::Rational(r),
                                    true,
                                )
                            }
                        }
                        _ => neocalc_core::utils::format_number(num, true),
                    }
                };

                if let Some(last) = state.history.last_mut() {
                    *last = format!("{} = {}", expr, result_str);
                }
                state.buffer = result_str.clone();
                result_str
            }
            Err(e) => {
                let err_msg = format!("Error: {:?}", e);
                if let Some(last) = state.history.last_mut() {
                    *last = format!("{} = Error", expr);
                }
                state.buffer = String::from("0");
                err_msg
            }
        }
    }

    pub fn get_buffer(&self) -> String {
        self.state.lock().unwrap().buffer.clone()
    }

    pub fn get_history(&self) -> Vec<String> {
        self.state.lock().unwrap().history.clone()
    }

    pub fn convert_to_hex(&self) -> String {
        let mut state = self.state.lock().unwrap();
        let expr = state.buffer.clone();
        match neocalc_core::evaluate(&expr, &mut state.context) {
            Ok(neocalc_core::engine::types::Number::Integer(i)) => {
                let hex = format!("0x{:X}", i);
                state.buffer = hex.clone();
                hex
            }
            _ => "Not an integer".to_string(),
        }
    }

    pub fn convert_to_bin(&self) -> String {
        let mut state = self.state.lock().unwrap();
        let expr = state.buffer.clone();
        match neocalc_core::evaluate(&expr, &mut state.context) {
            Ok(neocalc_core::engine::types::Number::Integer(i)) => {
                let bin = format!("0b{:b}", i);
                state.buffer = bin.clone();
                bin
            }
            _ => "Not an integer".to_string(),
        }
    }

    pub fn set_fraction_display(&self, enabled: bool) {
        let mut state = self.state.lock().unwrap();
        state.show_fractions = enabled;
    }
}

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
        // Rust i64 from hex might interpret as positive, but ARGB can be negative if cast directly?
        // Kotlin expects Color(Long). 0xFF... is a large positive Long.
        Ok(val)
    } else {
        Err(())
    }
}

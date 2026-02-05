uniffi::setup_scaffolding!();

mod update;
pub use update::{check_for_updates, download_apk, DownloadResult, UpdateCheckResult};

mod formatting;
pub use formatting::{format_for_display, NumberLocale};

mod storage;
pub use storage::{load_sessions, save_sessions, LoadResult, PersistedSession, StorageResult};

use num_traits::cast::ToPrimitive;
use serde::{Deserialize, Serialize};
use std::time::{SystemTime, UNIX_EPOCH};

/// Maximum number of history items to keep (prevents unbounded memory growth)
const MAX_HISTORY_SIZE: usize = 100;

/// Characters that are considered operators
const OPERATORS: &[char] = &['+', '-', '*', '/', '^', '%'];

/// Structured history item for cleaner UI consumption
#[derive(uniffi::Record, Clone, Debug, Serialize, Deserialize)]
pub struct HistoryItem {
    pub expression: String,
    pub result: String,
    pub timestamp: u64,
    pub is_error: bool,
}

struct CalculatorState {
    context: neocalc_core::Context,
    buffer: String,
    history: Vec<HistoryItem>,
    last_result: Option<String>,
    show_fractions: bool,
}

#[derive(uniffi::Object)]
pub struct Calculator {
    state: std::sync::Mutex<CalculatorState>,
}

/// Check if a character is an operator
fn is_operator(c: char) -> bool {
    OPERATORS.contains(&c)
}

/// Get current timestamp in milliseconds
fn now_millis() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
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
                last_result: None,
                show_fractions: false,
            }),
        }
    }

    /// Smart input with validation to prevent invalid sequences
    pub fn input(&self, text: String) -> String {
        let mut state = self.state.lock().unwrap();

        // Get the last character of the current buffer
        let last_char = state.buffer.chars().last();
        let input_char = text.chars().next();

        // Apply smart input rules
        if let (Some(last), Some(inp)) = (last_char, input_char) {
            // Rule 1: Prevent multiple consecutive decimal points
            if last == '.' && inp == '.' {
                return state.buffer.clone();
            }

            // Rule 2: Prevent multiple consecutive operators (except minus for negative)
            if is_operator(last) && is_operator(inp) && inp != '-' {
                // Replace the last operator with the new one
                state.buffer.pop();
            }

            // Rule 3: Prevent operator after opening parenthesis (except minus)
            if last == '(' && is_operator(inp) && inp != '-' {
                return state.buffer.clone();
            }

            // Rule 4: Prevent decimal point right after operator
            if is_operator(last) && inp == '.' {
                // Auto-insert 0 before decimal
                state.buffer.push('0');
            }

            // Rule 5: Prevent closing paren after operator
            if is_operator(last) && inp == ')' {
                return state.buffer.clone();
            }
        }

        // Handle initial state: replace "0" with the new input (unless it's a decimal)
        if state.buffer == "0" {
            if let Some(inp) = input_char {
                if inp == '.' {
                    state.buffer.push('.');
                } else if is_operator(inp) {
                    // Keep 0 and add operator
                    state.buffer.push(inp);
                } else {
                    state.buffer = text;
                }
            }
        } else {
            state.buffer.push_str(&text);
        }

        state.buffer.clone()
    }

    /// Clear the buffer and reset to "0"
    pub fn clear(&self) -> String {
        let mut state = self.state.lock().unwrap();
        state.buffer = String::from("0");
        state.buffer.clone()
    }

    /// Remove the last character from the buffer
    pub fn backspace(&self) -> String {
        let mut state = self.state.lock().unwrap();
        if !state.buffer.is_empty() {
            state.buffer.pop();
            if state.buffer.is_empty() {
                state.buffer = String::from("0");
            }
        }
        state.buffer.clone()
    }

    /// Evaluate the current buffer expression
    pub fn evaluate(&self) -> String {
        let mut state = self.state.lock().unwrap();
        let expr = state.buffer.clone();
        let timestamp = now_millis();

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

                // Add structured history item
                state.history.push(HistoryItem {
                    expression: expr.clone(),
                    result: result_str.clone(),
                    timestamp,
                    is_error: false,
                });

                // Trim history to max size
                if state.history.len() > MAX_HISTORY_SIZE {
                    state.history.remove(0);
                }

                // Store last result for ANS functionality
                state.last_result = Some(result_str.clone());
                state.buffer = result_str.clone();
                result_str
            }
            Err(e) => {
                let err_msg = format!("{:?}", e);

                // Add error to history
                state.history.push(HistoryItem {
                    expression: expr,
                    result: err_msg.clone(),
                    timestamp,
                    is_error: true,
                });

                // Trim history to max size
                if state.history.len() > MAX_HISTORY_SIZE {
                    state.history.remove(0);
                }

                state.buffer = String::from("0");
                format!("Error: {}", err_msg)
            }
        }
    }

    /// Get the current buffer content
    pub fn get_buffer(&self) -> String {
        self.state.lock().unwrap().buffer.clone()
    }

    /// Returns structured history items
    pub fn get_history(&self) -> Vec<HistoryItem> {
        self.state.lock().unwrap().history.clone()
    }

    /// Returns history as legacy string format for backward compatibility
    pub fn get_history_strings(&self) -> Vec<String> {
        self.state
            .lock()
            .unwrap()
            .history
            .iter()
            .map(|item| {
                if item.is_error {
                    format!("{} = Error", item.expression)
                } else {
                    format!("{} = {}", item.expression, item.result)
                }
            })
            .collect()
    }

    /// Get the last successful calculation result (for ANS key functionality)
    pub fn get_last_result(&self) -> Option<String> {
        self.state.lock().unwrap().last_result.clone()
    }

    /// Clear all history
    pub fn clear_history(&self) {
        let mut state = self.state.lock().unwrap();
        state.history.clear();
    }

    /// Convert the evaluated result to hexadecimal
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

    /// Convert the evaluated result to binary
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

    /// Convert the evaluated result to octal
    pub fn convert_to_oct(&self) -> String {
        let mut state = self.state.lock().unwrap();
        let expr = state.buffer.clone();
        match neocalc_core::evaluate(&expr, &mut state.context) {
            Ok(neocalc_core::engine::types::Number::Integer(i)) => {
                let oct = format!("0o{:o}", i);
                state.buffer = oct.clone();
                oct
            }
            _ => "Not an integer".to_string(),
        }
    }

    /// Set whether to display results as fractions
    pub fn set_fraction_display(&self, enabled: bool) {
        let mut state = self.state.lock().unwrap();
        state.show_fractions = enabled;
    }
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_smart_input_prevents_double_decimal() {
        let calc = Calculator::new();
        calc.input("1".to_string());
        calc.input(".".to_string());
        let result = calc.input(".".to_string());
        assert_eq!(result, "1.");
    }

    #[test]
    fn test_smart_input_replaces_consecutive_operators() {
        let calc = Calculator::new();
        calc.input("5".to_string());
        calc.input("+".to_string());
        let result = calc.input("*".to_string());
        assert_eq!(result, "5*");
    }

    #[test]
    fn test_smart_input_allows_negative_after_operator() {
        let calc = Calculator::new();
        calc.input("5".to_string());
        calc.input("+".to_string());
        let result = calc.input("-".to_string());
        assert_eq!(result, "5+-");
    }

    #[test]
    fn test_backspace_resets_to_zero() {
        let calc = Calculator::new();
        calc.input("5".to_string());
        let result = calc.backspace();
        assert_eq!(result, "0");
    }

    #[test]
    fn test_clear_resets_buffer() {
        let calc = Calculator::new();
        calc.input("123".to_string());
        let result = calc.clear();
        assert_eq!(result, "0");
    }

    #[test]
    fn test_history_item_structure() {
        let calc = Calculator::new();
        calc.input("2".to_string());
        calc.input("+".to_string());
        calc.input("2".to_string());
        calc.evaluate();

        let history = calc.get_history();
        assert_eq!(history.len(), 1);
        assert_eq!(history[0].expression, "2+2");
        assert!(!history[0].is_error);
    }

    #[test]
    fn test_last_result() {
        let calc = Calculator::new();
        calc.input("5".to_string());
        calc.input("+".to_string());
        calc.input("3".to_string());
        calc.evaluate();

        let last = calc.get_last_result();
        assert!(last.is_some());
        assert_eq!(last.unwrap(), "8");
    }

    #[test]
    fn test_history_limit() {
        let calc = Calculator::new();
        // Add more than MAX_HISTORY_SIZE items
        for i in 0..=MAX_HISTORY_SIZE + 10 {
            calc.clear();
            calc.input(format!("{}", i));
            calc.evaluate();
        }

        let history = calc.get_history();
        assert_eq!(history.len(), MAX_HISTORY_SIZE);
    }

    #[test]
    fn test_clear_history() {
        let calc = Calculator::new();
        calc.input("1".to_string());
        calc.evaluate();
        calc.clear();
        calc.input("2".to_string());
        calc.evaluate();

        assert_eq!(calc.get_history().len(), 2);
        calc.clear_history();
        assert_eq!(calc.get_history().len(), 0);
    }
}

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
                    neocalc_core::utils::format_number(num)
                } else {
                    match num {
                        neocalc_core::engine::types::Number::Rational(r) => {
                            if let Some(f) = r.to_f64() {
                                neocalc_core::utils::format_float(f)
                            } else {
                                neocalc_core::utils::format_number(
                                    neocalc_core::engine::types::Number::Rational(r),
                                )
                            }
                        }
                        _ => neocalc_core::utils::format_number(num),
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

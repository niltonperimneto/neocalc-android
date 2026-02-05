//! Number formatting module with locale support.
//!
//! Provides display formatting for numbers with configurable
//! thousands and decimal separators.

/// Locale configuration for number formatting
#[derive(uniffi::Enum, Clone, Copy, Debug, PartialEq)]
pub enum NumberLocale {
    /// US/UK format: 1,234,567.89 (comma thousands, dot decimal)
    EnUs,
    /// PT-BR/European format: 1.234.567,89 (dot thousands, comma decimal)
    PtBr,
}

impl NumberLocale {
    fn thousands_separator(&self) -> char {
        match self {
            NumberLocale::EnUs => ',',
            NumberLocale::PtBr => '.',
        }
    }

    fn decimal_separator(&self) -> char {
        match self {
            NumberLocale::EnUs => '.',
            NumberLocale::PtBr => ',',
        }
    }
}

/// Characters that indicate the input is an expression, not a plain number
/// Note: '-' is excluded because it can be a negative sign
const EXPRESSION_CHARS: &[char] = &['+', '*', '/', '^', '%', '(', ')', '=', ' '];

/// Format a number string for display with thousands separators.
///
/// # Arguments
/// * `input` - The raw number or expression string
/// * `locale` - The locale to use for formatting
///
/// # Returns
/// * Formatted string with appropriate separators, or original input if not a number
///
/// # Examples
/// ```
/// // US format
/// assert_eq!(format_for_display("1234567.89".to_string(), NumberLocale::EnUs), "1,234,567.89");
///
/// // PT-BR format
/// assert_eq!(format_for_display("1234567.89".to_string(), NumberLocale::PtBr), "1.234.567,89");
/// ```
#[uniffi::export]
pub fn format_for_display(input: String, locale: NumberLocale) -> String {
    // Don't format if it looks like an expression
    if input.chars().any(|c| EXPRESSION_CHARS.contains(&c)) {
        return input;
    }

    // Check for minus sign not at the start (subtraction operator)
    let trimmed = input.trim();
    if let Some(pos) = trimmed.find('-') {
        if pos > 0 {
            return input; // Minus in middle = subtraction
        }
    }

    // Don't format hex/binary/octal literals
    if input.starts_with("0x") || input.starts_with("0b") || input.starts_with("0o") {
        return input;
    }

    // Try to parse as a number
    let trimmed = input.trim();
    if trimmed.is_empty() {
        return input;
    }

    // Handle negative numbers
    let (sign, number_part) = if trimmed.starts_with('-') {
        ("-", &trimmed[1..])
    } else {
        ("", trimmed)
    };

    // Split into integer and decimal parts
    let (integer_part, decimal_part) = if let Some(dot_pos) = number_part.find('.') {
        (&number_part[..dot_pos], Some(&number_part[dot_pos + 1..]))
    } else {
        (number_part, None)
    };

    // Validate integer part (should only contain digits)
    if !integer_part.chars().all(|c| c.is_ascii_digit()) {
        return input;
    }

    // Validate decimal part if present
    if let Some(dec) = decimal_part {
        if !dec.chars().all(|c| c.is_ascii_digit()) {
            return input;
        }
    }

    // Format integer part with thousands separators
    let formatted_integer = add_thousands_separator(integer_part, locale.thousands_separator());

    // Build result
    let mut result = String::with_capacity(input.len() + 10);
    result.push_str(sign);
    result.push_str(&formatted_integer);

    if let Some(dec) = decimal_part {
        result.push(locale.decimal_separator());
        result.push_str(dec);
    }

    result
}

/// Add thousands separators to an integer string
fn add_thousands_separator(integer: &str, separator: char) -> String {
    // Input is already validated as ascii digits, so len() in bytes == len() in chars
    let len = integer.len();

    if len <= 3 {
        return integer.to_string();
    }

    let mut result = String::with_capacity(len + len / 3);
    for (i, c) in integer.chars().enumerate() {
        if i > 0 && (len - i) % 3 == 0 {
            result.push(separator);
        }
        result.push(c);
    }

    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_format_integer_en_us() {
        assert_eq!(
            format_for_display("1234567".to_string(), NumberLocale::EnUs),
            "1,234,567"
        );
    }

    #[test]
    fn test_format_decimal_en_us() {
        assert_eq!(
            format_for_display("1234567.89".to_string(), NumberLocale::EnUs),
            "1,234,567.89"
        );
    }

    #[test]
    fn test_format_integer_pt_br() {
        assert_eq!(
            format_for_display("1234567".to_string(), NumberLocale::PtBr),
            "1.234.567"
        );
    }

    #[test]
    fn test_format_decimal_pt_br() {
        assert_eq!(
            format_for_display("1234567.89".to_string(), NumberLocale::PtBr),
            "1.234.567,89"
        );
    }

    #[test]
    fn test_expression_not_formatted() {
        assert_eq!(
            format_for_display("1+2".to_string(), NumberLocale::EnUs),
            "1+2"
        );
        assert_eq!(
            format_for_display("5 * 3".to_string(), NumberLocale::PtBr),
            "5 * 3"
        );
    }

    #[test]
    fn test_hex_not_formatted() {
        assert_eq!(
            format_for_display("0xFF".to_string(), NumberLocale::EnUs),
            "0xFF"
        );
    }

    #[test]
    fn test_negative_number() {
        assert_eq!(
            format_for_display("-1234567".to_string(), NumberLocale::EnUs),
            "-1,234,567"
        );
        assert_eq!(
            format_for_display("-1234567.89".to_string(), NumberLocale::PtBr),
            "-1.234.567,89"
        );
    }

    #[test]
    fn test_small_numbers() {
        assert_eq!(
            format_for_display("123".to_string(), NumberLocale::EnUs),
            "123"
        );
        assert_eq!(
            format_for_display("12".to_string(), NumberLocale::PtBr),
            "12"
        );
    }
}

//! Session persistence module for saving/loading calculator state.
//!
//! Provides JSON-based storage for sessions including:
//! - Calculator buffer
//! - History items
//! - Variables (Context scopes)
//! - User-defined functions

use serde::{Deserialize, Serialize};
use std::fs;
use std::path::Path;

use crate::HistoryItem;

/// Persisted session data structure
#[derive(uniffi::Record, Clone, Debug, Serialize, Deserialize)]
pub struct PersistedSession {
    pub id: String,
    pub name: String,
    pub buffer: String,
    pub history: Vec<HistoryItem>,
    /// Serialized Context as JSON (variables and functions)
    pub context_json: String,
    pub mode: String,
}

/// All sessions container for save/load
#[derive(Serialize, Deserialize)]
struct SessionsData {
    version: u32,
    sessions: Vec<PersistedSession>,
    current_session_id: Option<String>,
}

/// Result of a storage operation
#[derive(uniffi::Enum, Clone, Debug)]
pub enum StorageResult {
    /// Operation succeeded
    Success,
    /// Operation failed with message
    Error { message: String },
}

/// Result of loading sessions
#[derive(uniffi::Enum, Clone, Debug)]
pub enum LoadResult {
    /// Sessions loaded successfully
    Loaded {
        sessions: Vec<PersistedSession>,
        current_session_id: Option<String>,
    },
    /// No saved data found (fresh start)
    NoData,
    /// Error loading data
    Error { message: String },
}

/// Save sessions to a JSON file
#[uniffi::export]
pub fn save_sessions(
    path: String,
    sessions: Vec<PersistedSession>,
    current_session_id: Option<String>,
) -> StorageResult {
    let data = SessionsData {
        version: 1,
        sessions,
        current_session_id,
    };

    match serde_json::to_string_pretty(&data) {
        Ok(json) => {
            if let Err(e) = fs::write(&path, json) {
                return StorageResult::Error {
                    message: format!("Failed to write file: {}", e),
                };
            }
            StorageResult::Success
        }
        Err(e) => StorageResult::Error {
            message: format!("Failed to serialize: {}", e),
        },
    }
}

/// Load sessions from a JSON file
#[uniffi::export]
pub fn load_sessions(path: String) -> LoadResult {
    let path = Path::new(&path);

    if !path.exists() {
        return LoadResult::NoData;
    }

    match fs::read_to_string(path) {
        Ok(content) => match serde_json::from_str::<SessionsData>(&content) {
            Ok(data) => LoadResult::Loaded {
                sessions: data.sessions,
                current_session_id: data.current_session_id,
            },
            Err(e) => LoadResult::Error {
                message: format!("Failed to parse: {}", e),
            },
        },
        Err(e) => LoadResult::Error {
            message: format!("Failed to read file: {}", e),
        },
    }
}

/// Serialize a calculator context to JSON string
#[uniffi::export]
pub fn serialize_context(context_json: String) -> String {
    // Pass-through for now - context is already JSON from Kotlin side
    context_json
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use std::path::PathBuf;

    fn test_path() -> PathBuf {
        PathBuf::from("/tmp/neocalc_test_sessions.json")
    }

    #[test]
    fn test_save_and_load() {
        let path = test_path();
        let _ = fs::remove_file(&path);

        let sessions = vec![PersistedSession {
            id: "test-1".to_string(),
            name: "Calc 1".to_string(),
            buffer: "42".to_string(),
            history: vec![],
            context_json: "{}".to_string(),
            mode: "STANDARD".to_string(),
        }];

        let result = save_sessions(
            path.to_string_lossy().to_string(),
            sessions.clone(),
            Some("test-1".to_string()),
        );
        assert!(matches!(result, StorageResult::Success));

        let loaded = load_sessions(path.to_string_lossy().to_string());
        match loaded {
            LoadResult::Loaded {
                sessions: loaded_sessions,
                current_session_id,
            } => {
                assert_eq!(loaded_sessions.len(), 1);
                assert_eq!(loaded_sessions[0].id, "test-1");
                assert_eq!(current_session_id, Some("test-1".to_string()));
            }
            _ => panic!("Expected Loaded result"),
        }

        let _ = fs::remove_file(&path);
    }

    #[test]
    fn test_load_nonexistent() {
        let result = load_sessions("/tmp/nonexistent_neocalc.json".to_string());
        assert!(matches!(result, LoadResult::NoData));
    }
}

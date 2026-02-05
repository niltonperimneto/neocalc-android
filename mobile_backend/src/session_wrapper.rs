use crate::HistoryItem;
use neocalc_core::session_manager::{AppSessionManager, HistoryEntry, SessionOverview};
use uniffi;

#[derive(uniffi::Record)]
pub struct MobileSessionOverview {
    pub id: String,
    pub name: String,
    pub is_active: bool,
}

impl From<SessionOverview> for MobileSessionOverview {
    fn from(s: SessionOverview) -> Self {
        Self {
            id: s.id,
            name: s.name,
            is_active: s.is_active,
        }
    }
}

// Convert core HistoryEntry to wrapper HistoryItem
impl From<HistoryEntry> for HistoryItem {
    fn from(e: HistoryEntry) -> Self {
        Self {
            expression: e.expression,
            result: e.result,
            timestamp: e.timestamp,
            is_error: e.is_error,
        }
    }
}

#[derive(uniffi::Object)]
pub struct MobileSessionManager {
    inner: AppSessionManager,
}

#[uniffi::export]
impl MobileSessionManager {
    #[uniffi::constructor]
    pub fn new(storage_path: String) -> Self {
        Self {
            inner: AppSessionManager::new(storage_path),
        }
    }

    pub fn get_sessions_overview(&self) -> Vec<MobileSessionOverview> {
        self.inner
            .get_sessions_overview()
            .into_iter()
            .map(MobileSessionOverview::from)
            .collect()
    }

    pub fn create_session(&self) -> String {
        self.inner.create_session()
    }

    pub fn switch_session(&self, id: String) -> bool {
        self.inner.switch_session(id)
    }

    pub fn delete_session(&self, id: String) -> bool {
        self.inner.delete_session(id)
    }

    pub fn rename_session(&self, id: String, new_name: String) -> bool {
        self.inner.rename_session(id, new_name)
    }

    pub fn input(&self, text: String) -> String {
        self.inner.input(text)
    }

    pub fn clear(&self) -> String {
        self.inner.clear()
    }

    pub fn backspace(&self) -> String {
        self.inner.backspace()
    }

    pub fn evaluate(&self) -> String {
        self.inner.evaluate()
    }

    pub fn convert_to_hex(&self) -> String {
        self.inner.convert_to_hex()
    }

    pub fn convert_to_bin(&self) -> String {
        self.inner.convert_to_bin()
    }

    pub fn convert_to_oct(&self) -> String {
        self.inner.convert_to_oct()
    }

    pub fn get_buffer(&self) -> String {
        self.inner.get_buffer()
    }

    pub fn get_history(&self) -> Vec<HistoryItem> {
        self.inner
            .get_history()
            .into_iter()
            .map(HistoryItem::from)
            .collect()
    }

    pub fn get_last_result(&self) -> Option<String> {
        self.inner.get_last_result()
    }

    pub fn set_fraction_display(&self, enabled: bool) {
        self.inner.set_fraction_display(enabled);
    }
}

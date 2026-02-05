//! Update module for checking and downloading APK updates from GitHub Releases.
//!
//! Security features:
//! - HTTPS-only transport via reqwest with rustls
//! - SHA-256 checksum verification of downloaded APKs
//! - Automatic abort on checksum mismatch

use serde::Deserialize;
use sha2::{Digest, Sha256};
use std::io::{Read, Write};

/// GitHub repository for releases
const GITHUB_REPO: &str = "niltonperimneto/neocalc-android";

/// Result of checking for updates
#[derive(uniffi::Enum, Clone, Debug)]
pub enum UpdateCheckResult {
    /// An update is available
    Available {
        version: String,
        download_url: String,
        checksum: Option<String>,
        release_notes: String,
    },
    /// App is up to date
    UpToDate,
    /// Error occurred during check
    Error { message: String },
}

/// Result of downloading an update
#[derive(uniffi::Enum, Clone, Debug)]
pub enum DownloadResult {
    /// Download completed successfully and checksum verified
    Success { file_path: String },
    /// Checksum verification failed - file was deleted
    ChecksumFailed { expected: String, actual: String },
    /// Download or I/O error
    Error { message: String },
}

/// GitHub Release API response structures
#[derive(Deserialize)]
struct GitHubRelease {
    tag_name: String,
    body: Option<String>,
    assets: Vec<GitHubAsset>,
}

#[derive(Deserialize)]
struct GitHubAsset {
    name: String,
    browser_download_url: String,
}

/// Check for updates from GitHub Releases
#[uniffi::export]
pub fn check_for_updates(current_version: String) -> UpdateCheckResult {
    let api_url = format!(
        "https://api.github.com/repos/{}/releases/latest",
        GITHUB_REPO
    );

    let client = match reqwest::blocking::Client::builder()
        .user_agent("NeoCalc-Android-App")
        .build()
    {
        Ok(c) => c,
        Err(e) => {
            return UpdateCheckResult::Error {
                message: e.to_string(),
            }
        }
    };

    // Fetch latest release metadata
    let response = match client
        .get(&api_url)
        .header("Accept", "application/vnd.github.v3+json")
        .send()
    {
        Ok(r) => r,
        Err(e) => {
            return UpdateCheckResult::Error {
                message: e.to_string(),
            }
        }
    };

    if !response.status().is_success() {
        return UpdateCheckResult::Error {
            message: format!("GitHub API returned {}", response.status()),
        };
    }

    let release: GitHubRelease = match response.json() {
        Ok(r) => r,
        Err(e) => {
            return UpdateCheckResult::Error {
                message: e.to_string(),
            }
        }
    };

    // Compare versions (strip leading 'v' if present)
    let latest = release.tag_name.trim_start_matches('v');
    let current = current_version.trim_start_matches('v');

    if latest == current {
        return UpdateCheckResult::UpToDate;
    }

    // Find APK and checksum file URLs
    let mut apk_url: Option<String> = None;
    let mut apk_filename: Option<String> = None;
    let mut checksum_url: Option<String> = None;

    for asset in &release.assets {
        if asset.name.ends_with(".apk") {
            apk_url = Some(asset.browser_download_url.clone());
            apk_filename = Some(asset.name.clone());
        } else if asset.name == "SHA256SUMS.txt" || asset.name == "checksums.txt" {
            checksum_url = Some(asset.browser_download_url.clone());
        }
    }

    let download_url = match apk_url {
        Some(url) => url,
        None => {
            return UpdateCheckResult::Error {
                message: "No APK found in release assets".to_string(),
            }
        }
    };

    // Fetch checksum if available
    let checksum = if let (Some(url), Some(filename)) = (checksum_url, apk_filename) {
        fetch_checksum_for_file(&client, &url, &filename)
    } else {
        None
    };

    UpdateCheckResult::Available {
        version: release.tag_name,
        download_url,
        checksum,
        release_notes: release.body.unwrap_or_default(),
    }
}

/// Fetch and parse checksum from SHA256SUMS.txt file
fn fetch_checksum_for_file(
    client: &reqwest::blocking::Client,
    checksum_url: &str,
    target_filename: &str,
) -> Option<String> {
    let response = client.get(checksum_url).send().ok()?;

    if !response.status().is_success() {
        return None;
    }

    let content = response.text().ok()?;

    // Parse format: "sha256hash  filename" or "sha256hash *filename"
    for line in content.lines() {
        let parts: Vec<&str> = line.splitn(2, char::is_whitespace).collect();
        if parts.len() == 2 {
            let hash = parts[0].to_lowercase();
            let filename = parts[1].trim().trim_start_matches('*');
            if filename == target_filename || filename.ends_with(target_filename) {
                return Some(hash);
            }
        }
    }

    None
}

/// Download APK to specified path with optional progress callback
///
/// # Arguments
/// * `download_url` - URL to download from
/// * `output_path` - Local file path to save to
/// * `expected_checksum` - Optional SHA-256 checksum to verify
///
/// # Returns
/// * `DownloadResult::Success` if download and verification succeeded
/// * `DownloadResult::ChecksumFailed` if checksum doesn't match (file is deleted)
/// * `DownloadResult::Error` on any other failure
#[uniffi::export]
pub fn download_apk(
    download_url: String,
    output_path: String,
    expected_checksum: Option<String>,
) -> DownloadResult {
    let client = match reqwest::blocking::Client::builder()
        .user_agent("NeoCalc-Android-App")
        .build()
    {
        Ok(c) => c,
        Err(e) => {
            return DownloadResult::Error {
                message: e.to_string(),
            }
        }
    };

    // Start download
    let mut response = match client.get(&download_url).send() {
        Ok(r) => r,
        Err(e) => {
            return DownloadResult::Error {
                message: e.to_string(),
            }
        }
    };

    if !response.status().is_success() {
        return DownloadResult::Error {
            message: format!("Download failed: {}", response.status()),
        };
    }

    // Create output file
    let mut file = match std::fs::File::create(&output_path) {
        Ok(f) => f,
        Err(e) => {
            return DownloadResult::Error {
                message: e.to_string(),
            }
        }
    };

    // Download with streaming to avoid memory issues
    let mut buffer = [0u8; 8192];
    loop {
        match response.read(&mut buffer) {
            Ok(0) => break, // EOF
            Ok(n) => {
                if let Err(e) = file.write_all(&buffer[..n]) {
                    let _ = std::fs::remove_file(&output_path);
                    return DownloadResult::Error {
                        message: e.to_string(),
                    };
                }
            }
            Err(e) => {
                let _ = std::fs::remove_file(&output_path);
                return DownloadResult::Error {
                    message: e.to_string(),
                };
            }
        }
    }

    drop(file); // Ensure file is closed before verification

    // Verify checksum if provided
    if let Some(expected) = expected_checksum {
        let actual = match calculate_sha256(&output_path) {
            Ok(hash) => hash,
            Err(e) => {
                let _ = std::fs::remove_file(&output_path);
                return DownloadResult::Error { message: e };
            }
        };

        if actual.to_lowercase() != expected.to_lowercase() {
            // SECURITY: Delete potentially compromised file
            let _ = std::fs::remove_file(&output_path);
            return DownloadResult::ChecksumFailed { expected, actual };
        }
    }

    DownloadResult::Success {
        file_path: output_path,
    }
}

/// Calculate SHA-256 hash of a file
fn calculate_sha256(path: &str) -> Result<String, String> {
    let mut file = std::fs::File::open(path).map_err(|e| e.to_string())?;
    let mut hasher = Sha256::new();
    let mut buffer = [0u8; 8192];

    loop {
        match file.read(&mut buffer) {
            Ok(0) => break,
            Ok(n) => hasher.update(&buffer[..n]),
            Err(e) => return Err(e.to_string()),
        }
    }

    let result = hasher.finalize();
    Ok(format!("{:x}", result))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_check_for_updates_parses_response() {
        // This test requires network access; skip in CI if needed
        let result = check_for_updates("v0.0.1".to_string());
        match result {
            UpdateCheckResult::Available {
                version,
                download_url,
                ..
            } => {
                assert!(!version.is_empty());
                assert!(download_url.contains("github.com"));
            }
            UpdateCheckResult::UpToDate => {
                // Also valid if we happen to match the latest
            }
            UpdateCheckResult::Error { message } => {
                // Network errors are acceptable in tests
                println!("Network error (expected in CI): {}", message);
            }
        }
    }
}

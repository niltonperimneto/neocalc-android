//! Update module for checking and downloading APK updates from GitHub Releases.
//!
//! Security features:
//! - HTTPS-only transport with a pinned rustls crypto provider and bundled
//!   webpki roots (no OS trust-store lookups, no background event-loop thread)
//! - Host allowlist enforced on every request, including each redirect hop
//! - Mandatory minisign signature verification of the checksum manifest
//! - Mandatory SHA-256 checksum verification of downloaded APKs
//! - Response size limits on every fetch (metadata, checksums, APK)
//! - CalVer/semver-aware version comparison to prevent rollback attacks
//! - Downloads staged to a `.part` file and atomically renamed only after
//!   the checksum verifies; failed files are deleted
//!
//! Reliability features:
//! - No async runtime: `ureq` is a plain blocking client, so there is no
//!   "event loop" thread that can panic and poison the whole updater
//! - Every exported function is wrapped in a panic shield that converts
//!   panics into `Error` results instead of crashing the app
//! - Explicit connect/response timeouts on every request
//! - Automatic retry with backoff for transient network failures
//! - Interrupted APK downloads resume via HTTP Range requests
//! - Real progress reporting through a foreign callback interface

use minisign_verify::{PublicKey, Signature};
use semver::Version;
use serde::Deserialize;
use sha2::{Digest, Sha256};
use std::io::{Read, Write};
use std::panic::AssertUnwindSafe;
use std::sync::Arc;
use std::time::Duration;

/// GitHub repository for releases
const GITHUB_REPO: &str = "niltonperimneto/neocalc-android";

/// Minisign Public Key for verifying updates.
/// The key must include the "untrusted comment" line.
const MINISIGN_PUBLIC_KEY: &str = "untrusted comment: minisign public key 45649A13B8B61959
RWSaE7i2GVxl9w6xy88JQhq4E+QH/msvu+eGL/YdWZ9e2Lv/vUhvLOQz";

const USER_AGENT: &str = "NeoCalc-Android-App";

// Hardening limits
const MAX_METADATA_BYTES: u64 = 2 * 1024 * 1024; // release JSON
const MAX_CHECKSUM_BYTES: u64 = 64 * 1024; // SHA256SUMS.txt
const MAX_SIGNATURE_BYTES: u64 = 4 * 1024; // .minisig
const MAX_APK_BYTES: u64 = 512 * 1024 * 1024;
const MAX_REDIRECTS: u32 = 5;
const MAX_ATTEMPTS: u32 = 3;
const CONNECT_TIMEOUT: Duration = Duration::from_secs(10);
const METADATA_TIMEOUT: Duration = Duration::from_secs(30);
const DOWNLOAD_BODY_TIMEOUT: Duration = Duration::from_secs(30 * 60);
const IO_BUFFER_SIZE: usize = 64 * 1024;
/// Emit a progress callback at most once per this many bytes.
const PROGRESS_GRANULARITY: u64 = 256 * 1024;

/// Result of checking for updates
#[derive(uniffi::Enum, Clone, Debug)]
pub enum UpdateCheckResult {
    /// An update is available. The checksum is always present and has been
    /// authenticated against the release's minisign signature.
    Available {
        version: String,
        download_url: String,
        checksum: String,
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

/// Progress callback implemented on the Kotlin side.
/// `total_bytes` is `None` when the server did not report a size, except in
/// the final callback of a completed download, where the total is known to
/// equal `bytes_downloaded` and is reported as such (so UIs can land on 100%).
#[uniffi::export(with_foreign)]
pub trait UpdateProgressListener: Send + Sync {
    fn on_progress(&self, bytes_downloaded: u64, total_bytes: Option<u64>);
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

// ---------------------------------------------------------------------------
// Panic shield
// ---------------------------------------------------------------------------

/// Run `f`, converting any panic into an error value instead of letting it
/// cross the FFI boundary or kill a worker thread.
fn run_shielded<T>(f: impl FnOnce() -> T, on_panic: impl FnOnce(String) -> T) -> T {
    match std::panic::catch_unwind(AssertUnwindSafe(f)) {
        Ok(value) => value,
        Err(payload) => {
            let msg = payload
                .downcast_ref::<&str>()
                .map(|s| (*s).to_string())
                .or_else(|| payload.downcast_ref::<String>().cloned())
                .unwrap_or_else(|| "unknown panic".to_string());
            on_panic(format!("Internal updater error: {msg}"))
        }
    }
}

// ---------------------------------------------------------------------------
// HTTP plumbing
// ---------------------------------------------------------------------------

/// Only these hosts may ever be contacted. Release asset downloads redirect
/// from github.com to *.githubusercontent.com; every hop is re-validated.
fn is_trusted_host(host: &str) -> bool {
    let host = host.to_ascii_lowercase();
    host == "github.com"
        || host == "api.github.com"
        || host == "objects.githubusercontent.com"
        || host.ends_with(".githubusercontent.com")
}

/// Require an https URL pointing at a trusted host. Returns the parsed URI.
fn validate_url(url: &str) -> Result<ureq::http::Uri, String> {
    let uri: ureq::http::Uri = url
        .parse()
        .map_err(|_| format!("Invalid URL: {url}"))?;
    if uri.scheme_str() != Some("https") {
        return Err("Refusing non-HTTPS URL".to_string());
    }
    match uri.host() {
        Some(host) if is_trusted_host(host) => Ok(uri),
        Some(host) => Err(format!("Refusing untrusted host: {host}")),
        None => Err("URL has no host".to_string()),
    }
}

/// Resolve a redirect Location against the current URL.
fn resolve_redirect(current: &ureq::http::Uri, location: &str) -> Result<String, String> {
    if location.starts_with("https://") {
        Ok(location.to_string())
    } else if location.starts_with('/') {
        let authority = current
            .authority()
            .ok_or("Current URL has no authority")?;
        Ok(format!("https://{authority}{location}"))
    } else {
        Err(format!("Unsupported redirect location: {location}"))
    }
}

fn new_agent(body_timeout: Duration) -> ureq::Agent {
    let builder = ureq::Agent::config_builder()
        .user_agent(USER_AGENT)
        .timeout_connect(Some(CONNECT_TIMEOUT))
        .timeout_recv_response(Some(METADATA_TIMEOUT))
        .timeout_recv_body(Some(body_timeout))
        // Redirects are followed manually so each hop can be validated
        // against the host allowlist.
        .max_redirects(0)
        // Non-2xx statuses are handled explicitly rather than as errors.
        .http_status_as_error(false);

    // Test-only escape hatch for CI sandboxes behind TLS-intercepting
    // proxies. Not compiled into the shipped library.
    #[cfg(test)]
    let builder = if std::env::var_os("NEOCALC_TEST_INSECURE_TLS").is_some() {
        builder.tls_config(
            ureq::tls::TlsConfig::builder()
                .disable_verification(true)
                .build(),
        )
    } else {
        builder
    };

    builder.build().new_agent()
}

fn is_transient_status(status: u16) -> bool {
    matches!(status, 408 | 429 | 500 | 502 | 503 | 504)
}

/// Perform a GET, validating the host on the initial URL and on every
/// redirect hop. Optionally sends a Range header for resumable downloads.
fn get_validated(
    agent: &ureq::Agent,
    url: &str,
    range_from: Option<u64>,
    extra_headers: &[(&str, &str)],
) -> Result<ureq::http::Response<ureq::Body>, String> {
    let mut current = url.to_string();
    for redirects in 0..=MAX_REDIRECTS {
        let uri = validate_url(&current)?;
        let mut request = agent.get(&current);
        for (name, value) in extra_headers {
            request = request.header(*name, *value);
        }
        if let Some(offset) = range_from {
            request = request.header("Range", format!("bytes={offset}-"));
        }
        let response = request.call().map_err(|e| format!("Network error: {e}"))?;
        if response.status().is_redirection() {
            if redirects == MAX_REDIRECTS {
                return Err("Too many redirects".to_string());
            }
            let location = response
                .headers()
                .get("location")
                .and_then(|v| v.to_str().ok())
                .ok_or("Redirect without Location header")?;
            current = resolve_redirect(&uri, location)?;
            continue;
        }
        return Ok(response);
    }
    Err("Too many redirects".to_string())
}

/// GET a small resource as text, with retries for transient failures and a
/// hard size limit.
fn get_text_with_retry(
    agent: &ureq::Agent,
    url: &str,
    max_bytes: u64,
    extra_headers: &[(&str, &str)],
) -> Result<String, String> {
    // Fail fast on non-HTTPS or untrusted hosts instead of retrying with backoff.
    let _ = validate_url(url)?;
    let mut last_error = String::new();
    for attempt in 0..MAX_ATTEMPTS {
        if attempt > 0 {
            std::thread::sleep(Duration::from_secs(1 << (attempt - 1)));
        }
        match get_validated(agent, url, None, extra_headers) {
            Ok(mut response) => {
                let status = response.status();
                if status.is_success() {
                    // A mid-body failure (connection reset while streaming)
                    // is transient too: fall through to the next attempt.
                    match response
                        .body_mut()
                        .with_config()
                        .limit(max_bytes)
                        .read_to_string()
                    {
                        Ok(text) => return Ok(text),
                        Err(e) => last_error = format!("Failed to read response: {e}"),
                    }
                } else {
                    last_error = format!("Server returned HTTP {status} for {url}");
                    if !is_transient_status(status.as_u16()) {
                        return Err(last_error);
                    }
                }
            }
            Err(e) => last_error = e,
        }
    }
    Err(last_error)
}

// ---------------------------------------------------------------------------
// Version comparison
// ---------------------------------------------------------------------------

/// Parse a CalVer tag like "2025.06-3" into (year, month, revision).
fn parse_calver(version: &str) -> Option<(u32, u32, u32)> {
    let (date, rev) = version.split_once('-')?;
    let (year, month) = date.split_once('.')?;
    Some((
        year.parse().ok()?,
        month.parse().ok()?,
        rev.parse().ok()?,
    ))
}

/// Decide whether `latest` is strictly newer than `current`.
///
/// Understands this project's CalVer scheme ("v2025.06-3") and semver.
/// Comparable versions are ordered numerically, which blocks rollback
/// attacks that replay an older signed release as "latest". If the two
/// versions use different (or unrecognized) schemes, any difference is
/// treated as an update so a future versioning change doesn't strand users.
fn is_newer_version(latest: &str, current: &str) -> bool {
    let latest = latest.trim().trim_start_matches('v');
    let current = current.trim().trim_start_matches('v');

    if let (Some(l), Some(c)) = (parse_calver(latest), parse_calver(current)) {
        return l > c;
    }
    if let (Ok(l), Ok(c)) = (Version::parse(latest), Version::parse(current)) {
        return l > c;
    }
    latest != current
}

// ---------------------------------------------------------------------------
// Update check
// ---------------------------------------------------------------------------

/// Check for updates from GitHub Releases
///
/// # Arguments
/// * `current_version` - Current app version (e.g., "v2025.06-3")
/// * `device_abi` - Device ABI (e.g., "arm64-v8a", "armeabi-v7a", "x86_64", "x86")
///                  Pass empty string to always use universal APK
#[uniffi::export]
pub fn check_for_updates(current_version: String, device_abi: String) -> UpdateCheckResult {
    run_shielded(
        || check_for_updates_inner(&current_version, &device_abi),
        |message| UpdateCheckResult::Error { message },
    )
}

fn check_for_updates_inner(current_version: &str, device_abi: &str) -> UpdateCheckResult {
    let api_url = format!("https://api.github.com/repos/{GITHUB_REPO}/releases/latest");
    let agent = new_agent(METADATA_TIMEOUT);

    let body = match get_text_with_retry(
        &agent,
        &api_url,
        MAX_METADATA_BYTES,
        &[
            ("Accept", "application/vnd.github.v3+json"),
            ("X-GitHub-Api-Version", "2022-11-28"),
        ],
    ) {
        Ok(body) => body,
        Err(message) => return UpdateCheckResult::Error { message },
    };

    let release: GitHubRelease = match serde_json::from_str(&body) {
        Ok(release) => release,
        Err(e) => {
            return UpdateCheckResult::Error {
                message: format!("Malformed release metadata: {e}"),
            }
        }
    };

    if !is_newer_version(&release.tag_name, current_version) {
        return UpdateCheckResult::UpToDate;
    }

    let selection = select_assets(&release, device_abi);

    let Some((download_url, apk_filename)) = selection.apk.map(|(url, name)| {
        (url.to_string(), name.to_string())
    }) else {
        return UpdateCheckResult::Error {
            message: "No compatible APK found in release assets".to_string(),
        };
    };

    // SECURITY: signature and checksum are mandatory. A release without them
    // is treated as invalid rather than installed unverified.
    let (Some(chk_url), Some(sig_url)) = (
        selection.checksum_url.map(str::to_string),
        selection.signature_url.map(str::to_string),
    ) else {
        return UpdateCheckResult::Error {
            message: "Release is missing signed checksums; refusing unverified update".to_string(),
        };
    };

    let checksums = match verify_and_fetch_checksums(&agent, &chk_url, &sig_url) {
        Ok(content) => content,
        Err(e) => {
            return UpdateCheckResult::Error {
                message: format!("Signature verification failed: {e}"),
            }
        }
    };

    let Some(checksum) = parse_checksum(&checksums, &apk_filename) else {
        return UpdateCheckResult::Error {
            message: format!("No checksum listed for {apk_filename}; refusing unverified update"),
        };
    };

    UpdateCheckResult::Available {
        version: release.tag_name,
        download_url,
        checksum,
        release_notes: release.body.unwrap_or_default(),
    }
}

#[derive(Default)]
struct AssetSelection<'a> {
    /// (download_url, filename) of the best APK for this device
    apk: Option<(&'a str, &'a str)>,
    checksum_url: Option<&'a str>,
    signature_url: Option<&'a str>,
}

/// True if `filename` is the APK built for exactly this ABI. Delimiter-aware
/// so that e.g. "x86" does not match "app-x86_64-release.apk".
fn matches_abi(filename: &str, abi: &str) -> bool {
    filename.contains(&format!("-{abi}-")) || filename.ends_with(&format!("-{abi}.apk"))
}

/// Pick release assets: the ABI-specific APK if available, falling back to
/// the universal APK, then the legacy single-APK name; plus the checksum
/// manifest and its signature.
fn select_assets<'a>(release: &'a GitHubRelease, device_abi: &str) -> AssetSelection<'a> {
    let mut arch_apk = None;
    let mut universal_apk = None;
    let mut legacy_apk = None;
    let mut selection = AssetSelection::default();

    for asset in &release.assets {
        if asset.name == "SHA256SUMS.txt" || asset.name == "checksums.txt" {
            selection.checksum_url = Some(asset.browser_download_url.as_str());
        } else if asset.name.ends_with(".minisig") {
            selection.signature_url = Some(asset.browser_download_url.as_str());
        } else if asset.name.ends_with(".apk") {
            let entry = Some((asset.browser_download_url.as_str(), asset.name.as_str()));
            if !device_abi.is_empty() && matches_abi(&asset.name, device_abi) {
                arch_apk = entry;
            } else if asset.name.contains("universal") {
                universal_apk = entry;
            } else if asset.name == "app-release.apk" {
                legacy_apk = entry;
            }
        }
    }

    selection.apk = arch_apk.or(universal_apk).or(legacy_apk);
    selection
}

/// Fetch the checksum manifest and its minisign signature, verify the
/// signature, and return the authenticated manifest content.
fn verify_and_fetch_checksums(
    agent: &ureq::Agent,
    checksum_url: &str,
    signature_url: &str,
) -> Result<String, String> {
    let pub_key =
        PublicKey::decode(MINISIGN_PUBLIC_KEY).map_err(|_| "Invalid public key format")?;

    let signed_content = get_text_with_retry(agent, checksum_url, MAX_CHECKSUM_BYTES, &[])?;
    let signature_str = get_text_with_retry(agent, signature_url, MAX_SIGNATURE_BYTES, &[])?;
    let signature = Signature::decode(&signature_str).map_err(|_| "Invalid signature format")?;

    pub_key
        .verify(signed_content.as_bytes(), &signature, false)
        .map_err(|_| "Cryptographic signature verification failed!".to_string())?;

    Ok(signed_content)
}

fn is_valid_sha256_hex(hash: &str) -> bool {
    hash.len() == 64 && hash.chars().all(|c| c.is_ascii_hexdigit())
}

/// Parse checksum from verified content. Accepts the exact filename or a
/// path-prefixed variant ("release/<name>"); only well-formed SHA-256
/// values are returned.
fn parse_checksum(content: &str, target_filename: &str) -> Option<String> {
    for line in content.lines() {
        let parts: Vec<&str> = line.splitn(2, char::is_whitespace).collect();
        if parts.len() == 2 {
            let hash = parts[0].to_lowercase();
            let filename = parts[1].trim().trim_start_matches('*');
            let matches = filename == target_filename
                || filename.ends_with(&format!("/{target_filename}"));
            if matches && is_valid_sha256_hex(&hash) {
                return Some(hash);
            }
        }
    }
    None
}

// ---------------------------------------------------------------------------
// Download
// ---------------------------------------------------------------------------

/// Download APK to specified path, verifying its SHA-256 checksum.
///
/// The download is staged to `<output_path>.part` and only renamed to
/// `output_path` after the checksum verifies. Interrupted downloads are
/// resumed with HTTP Range requests on retry, including across separate
/// calls if the `.part` file is still on disk.
///
/// # Arguments
/// * `download_url` - HTTPS URL on a trusted host to download from
/// * `output_path` - Local file path to save to
/// * `expected_checksum` - SHA-256 checksum the file must match (from a
///   signature-verified manifest)
/// * `progress` - Optional listener receiving (bytes_downloaded, total_bytes)
#[uniffi::export]
pub fn download_apk(
    download_url: String,
    output_path: String,
    expected_checksum: String,
    progress: Option<Arc<dyn UpdateProgressListener>>,
) -> DownloadResult {
    run_shielded(
        || download_apk_inner(&download_url, &output_path, &expected_checksum, progress),
        |message| DownloadResult::Error { message },
    )
}

fn download_apk_inner(
    download_url: &str,
    output_path: &str,
    expected_checksum: &str,
    progress: Option<Arc<dyn UpdateProgressListener>>,
) -> DownloadResult {
    if let Err(message) = validate_url(download_url) {
        return DownloadResult::Error { message };
    }
    if !is_valid_sha256_hex(expected_checksum) {
        return DownloadResult::Error {
            message: "Invalid expected checksum (must be 64 hex chars)".to_string(),
        };
    }

    let part_path = format!("{output_path}.part");
    let agent = new_agent(DOWNLOAD_BODY_TIMEOUT);

    let mut last_error = String::new();
    for attempt in 0..MAX_ATTEMPTS {
        if attempt > 0 {
            std::thread::sleep(Duration::from_secs(1 << (attempt - 1)));
        }
        match download_attempt(&agent, download_url, &part_path, progress.as_deref()) {
            Ok(()) => {
                return finalize_download(&part_path, output_path, expected_checksum);
            }
            Err(DownloadAttemptError::Fatal(message)) => {
                let _ = std::fs::remove_file(&part_path);
                return DownloadResult::Error { message };
            }
            Err(DownloadAttemptError::Transient(message)) => {
                last_error = message;
            }
        }
    }
    // Keep the .part file: a future call can resume from it.
    DownloadResult::Error {
        message: format!("Download failed after {MAX_ATTEMPTS} attempts: {last_error}"),
    }
}

enum DownloadAttemptError {
    /// Worth retrying (network hiccup, 5xx); partial data is kept for resume.
    Transient(String),
    /// Not worth retrying (4xx, size cap exceeded, local I/O failure).
    Fatal(String),
}

fn download_attempt(
    agent: &ureq::Agent,
    download_url: &str,
    part_path: &str,
    progress: Option<&dyn UpdateProgressListener>,
) -> Result<(), DownloadAttemptError> {
    use DownloadAttemptError::{Fatal, Transient};

    let mut offset = std::fs::metadata(part_path).map(|m| m.len()).unwrap_or(0);
    let range = if offset > 0 { Some(offset) } else { None };

    let mut response = get_validated(agent, download_url, range, &[])
        .map_err(Transient)?;
    let status = response.status();

    let resuming = match status.as_u16() {
        200 => false,             // full body; restart from scratch
        206 if offset > 0 => true, // server honored our Range request
        // Range no longer satisfiable (asset changed?); restart clean.
        416 | 206 => {
            let _ = std::fs::remove_file(part_path);
            return Err(Transient(format!("Unexpected range response (HTTP {status})")));
        }
        s if is_transient_status(s) => {
            return Err(Transient(format!("Server returned HTTP {status}")));
        }
        _ => return Err(Fatal(format!("Download failed: HTTP {status}"))),
    };
    if !resuming {
        offset = 0;
    }

    // Total size: Content-Range total for 206, Content-Length for 200.
    let header = |name: &str| -> Option<String> {
        response
            .headers()
            .get(name)
            .and_then(|v| v.to_str().ok())
            .map(str::to_string)
    };
    let total_bytes: Option<u64> = if resuming {
        // "Content-Range: bytes <start>-<end>/<total>". The body must start
        // exactly where our partial file ends, or appending would corrupt it.
        let content_range = header("content-range").unwrap_or_default();
        if !content_range
            .trim()
            .starts_with(&format!("bytes {offset}-"))
        {
            let _ = std::fs::remove_file(part_path);
            return Err(Transient(format!(
                "Server returned unexpected range '{content_range}'"
            )));
        }
        content_range
            .rsplit('/')
            .next()
            .and_then(|total| total.trim().parse().ok())
    } else {
        header("content-length").and_then(|v| v.parse().ok())
    };
    if let Some(total) = total_bytes
        && total > MAX_APK_BYTES
    {
        return Err(Fatal(format!("Update is implausibly large ({total} bytes)")));
    }

    let mut file = std::fs::OpenOptions::new()
        .create(true)
        .write(true)
        .append(resuming)
        .truncate(!resuming)
        .open(part_path)
        .map_err(|e| Fatal(format!("Cannot open output file: {e}")))?;

    if let Some(listener) = progress {
        listener.on_progress(offset, total_bytes);
    }

    let mut reader = response.body_mut().as_reader();
    let mut buffer = [0u8; IO_BUFFER_SIZE];
    let mut written = offset;
    let mut last_reported = offset;
    loop {
        let n = match reader.read(&mut buffer) {
            Ok(0) => break,
            Ok(n) => n,
            Err(e) => return Err(Transient(format!("Connection interrupted: {e}"))),
        };
        written += n as u64;
        if written > MAX_APK_BYTES {
            let _ = std::fs::remove_file(part_path);
            return Err(Fatal("Download exceeded maximum allowed size".to_string()));
        }
        file.write_all(&buffer[..n])
            .map_err(|e| Fatal(format!("Write failed: {e}")))?;
        if let Some(listener) = progress
            && written - last_reported >= PROGRESS_GRANULARITY
        {
            listener.on_progress(written, total_bytes);
            last_reported = written;
        }
    }

    file.sync_all()
        .map_err(|e| Fatal(format!("Sync failed: {e}")))?;
    if let Some(listener) = progress {
        listener.on_progress(written, total_bytes.or(Some(written)));
    }

    // If the server told us the size, catch truncated bodies here so the
    // next attempt resumes instead of failing checksum verification.
    if let Some(total) = total_bytes
        && written < total
    {
        return Err(Transient(format!(
            "Connection closed early ({written}/{total} bytes)"
        )));
    }
    Ok(())
}

/// Verify the staged download's checksum and atomically move it into place.
fn finalize_download(
    part_path: &str,
    output_path: &str,
    expected_checksum: &str,
) -> DownloadResult {
    let actual = match calculate_sha256(part_path) {
        Ok(hash) => hash,
        Err(e) => {
            let _ = std::fs::remove_file(part_path);
            return DownloadResult::Error { message: e };
        }
    };

    if !actual.eq_ignore_ascii_case(expected_checksum) {
        // SECURITY: Delete potentially compromised file
        let _ = std::fs::remove_file(part_path);
        return DownloadResult::ChecksumFailed {
            expected: expected_checksum.to_lowercase(),
            actual,
        };
    }

    if let Err(e) = std::fs::rename(part_path, output_path) {
        let _ = std::fs::remove_file(part_path);
        return DownloadResult::Error {
            message: format!("Could not move verified file into place: {e}"),
        };
    }

    DownloadResult::Success {
        file_path: output_path.to_string(),
    }
}

/// Calculate SHA-256 hash of a file
fn calculate_sha256(path: &str) -> Result<String, String> {
    let mut file = std::fs::File::open(path).map_err(|e| e.to_string())?;
    let mut hasher = Sha256::new();
    let mut buffer = [0u8; IO_BUFFER_SIZE];
    loop {
        match file.read(&mut buffer) {
            Ok(0) => break,
            Ok(n) => hasher.update(&buffer[..n]),
            Err(e) => return Err(e.to_string()),
        }
    }
    let digest = hasher.finalize();
    Ok(digest.iter().map(|b| format!("{b:02x}")).collect())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_public_key_format() {
        PublicKey::decode(MINISIGN_PUBLIC_KEY).expect("bundled public key must be valid");
    }

    #[test]
    fn test_calver_comparison() {
        assert!(is_newer_version("v2025.07-1", "v2025.06-3"));
        assert!(is_newer_version("v2025.06-4", "v2025.06-3"));
        assert!(is_newer_version("v2026.01-1", "v2025.12-9"));
        assert!(!is_newer_version("v2025.06-3", "v2025.06-3"));
        // Rollback attempts must be rejected
        assert!(!is_newer_version("v2025.05-9", "v2025.06-1"));
        assert!(!is_newer_version("v2024.12-1", "v2025.01-1"));
    }

    #[test]
    fn test_semver_comparison() {
        assert!(is_newer_version("v1.2.3", "v1.2.2"));
        assert!(!is_newer_version("v1.2.3", "v1.2.3"));
        assert!(!is_newer_version("v1.0.0", "v1.2.3"));
    }

    #[test]
    fn test_mixed_scheme_falls_back_to_inequality() {
        // A scheme migration should still offer the update
        assert!(is_newer_version("v2025.06-1", "v1.0"));
        assert!(!is_newer_version("v1.0", "v1.0"));
    }

    #[test]
    fn test_host_allowlist() {
        assert!(validate_url("https://github.com/x/y/releases/download/v1/app.apk").is_ok());
        assert!(validate_url("https://api.github.com/repos/x/y/releases/latest").is_ok());
        assert!(validate_url("https://objects.githubusercontent.com/asset").is_ok());
        assert!(validate_url("https://release-assets.githubusercontent.com/asset").is_ok());

        assert!(validate_url("http://github.com/insecure").is_err());
        assert!(validate_url("https://evil.com/apk").is_err());
        assert!(validate_url("https://evilgithubusercontent.com/apk").is_err());
        assert!(validate_url("https://github.com.evil.com/apk").is_err());
        assert!(validate_url("not a url").is_err());
    }

    #[test]
    fn test_redirect_resolution() {
        let base: ureq::http::Uri = "https://github.com/a/b".parse().unwrap();
        assert_eq!(
            resolve_redirect(&base, "https://objects.githubusercontent.com/x").unwrap(),
            "https://objects.githubusercontent.com/x"
        );
        assert_eq!(
            resolve_redirect(&base, "/relative/path").unwrap(),
            "https://github.com/relative/path"
        );
        assert!(resolve_redirect(&base, "http://insecure.com/x").is_err());
        assert!(resolve_redirect(&base, "relative-no-slash").is_err());
    }

    #[test]
    fn test_parse_checksum() {
        let hash_a = "a".repeat(64);
        let hash_b = "B".repeat(64);
        let hash_c = "c".repeat(64);
        let content = format!(
            "{hash_a}  app-arm64-v8a-release.apk\n\
             {hash_b} *app-release.apk\n\
             {hash_c}  release/app-x86-release.apk\n"
        );
        assert_eq!(
            parse_checksum(&content, "app-arm64-v8a-release.apk"),
            Some(hash_a)
        );
        // '*' binary-mode marker and uppercase hash are normalized
        assert_eq!(
            parse_checksum(&content, "app-release.apk"),
            Some(hash_b.to_lowercase())
        );
        // Path prefix is accepted only at a '/' boundary
        assert_eq!(
            parse_checksum(&content, "app-x86-release.apk"),
            Some(hash_c)
        );
        assert_eq!(parse_checksum(&content, "x86-release.apk"), None);
        assert_eq!(parse_checksum(&content, "missing.apk"), None);
    }

    #[test]
    fn test_parse_checksum_rejects_malformed_hash() {
        // Signature-verified or not, a manifest entry that isn't a
        // well-formed SHA-256 must not cross the FFI boundary.
        let content = "abc123  app-release.apk\n";
        assert_eq!(parse_checksum(content, "app-release.apk"), None);
        let content = format!("{}  app-release.apk\n", "g".repeat(64));
        assert_eq!(parse_checksum(&content, "app-release.apk"), None);
    }

    /// Asset names and ordering taken from the real v2026.07-6 release.
    fn real_release_fixture() -> GitHubRelease {
        let names = [
            "app-arm64-v8a-release.apk",
            "app-armeabi-v7a-release.apk",
            "app-universal-release.apk",
            "app-x86-release.apk",
            "app-x86_64-release.apk",
            "SHA256SUMS.txt",
            "SHA256SUMS.txt.minisig",
        ];
        GitHubRelease {
            tag_name: "v2026.07-6".to_string(),
            body: Some("notes".to_string()),
            assets: names
                .iter()
                .map(|name| GitHubAsset {
                    name: name.to_string(),
                    browser_download_url: format!(
                        "https://github.com/{GITHUB_REPO}/releases/download/v2026.07-6/{name}"
                    ),
                })
                .collect(),
        }
    }

    #[test]
    fn test_asset_selection_per_abi() {
        let release = real_release_fixture();
        for abi in ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"] {
            let selection = select_assets(&release, abi);
            let (_, filename) = selection.apk.expect("APK must be found");
            assert_eq!(filename, format!("app-{abi}-release.apk"), "ABI {abi}");
            assert!(selection.checksum_url.is_some());
            assert!(selection.signature_url.is_some());
        }
        // Unknown or empty ABI falls back to the universal APK
        for abi in ["", "riscv64"] {
            let selection = select_assets(&release, abi);
            assert_eq!(selection.apk.unwrap().1, "app-universal-release.apk");
        }
    }

    #[test]
    fn test_release_json_parses() {
        // Shape of the GitHub API response, trimmed to relevant fields
        let json = r#"{
            "tag_name": "v2026.07-6",
            "body": null,
            "assets": [
                {"name": "app-arm64-v8a-release.apk",
                 "browser_download_url": "https://github.com/x/y/releases/download/v2026.07-6/app-arm64-v8a-release.apk",
                 "size": 13760176}
            ],
            "draft": false
        }"#;
        let release: GitHubRelease = serde_json::from_str(json).unwrap();
        assert_eq!(release.tag_name, "v2026.07-6");
        assert_eq!(release.assets.len(), 1);
    }

    #[test]
    fn test_checksum_format_validation() {
        let result = download_apk(
            "https://github.com/x/y.apk".to_string(),
            "/tmp/never-written.apk".to_string(),
            "not-a-checksum".to_string(),
            None,
        );
        assert!(matches!(result, DownloadResult::Error { .. }));
    }

    /// Requires network access; run with `cargo test -- --ignored`.
    #[test]
    #[ignore]
    fn test_check_for_updates_live() {
        let result = check_for_updates("v0.0.1".to_string(), "arm64-v8a".to_string());
        match result {
            UpdateCheckResult::Available { checksum, .. } => {
                assert_eq!(checksum.len(), 64);
            }
            UpdateCheckResult::UpToDate => {}
            UpdateCheckResult::Error { message } => panic!("live check failed: {message}"),
        }
    }
}

# Roadmap: AI-powered Vulnerability Analysis

## Overview
AI analysis capabilities for SAST Android application with on-device (AICore/Gemini Nano) and remote (OpenAI-compatible) inference.

---

## Current Implementation Status: Phase 10 Initial Implementation Complete ✅

Remote AI with chunking, Settings UI, AI insights display, AICore fallback chain, and initial smali-based AI discovery are implemented.

Next focus: harden AI discovery quality, make long-running analysis observable/cancellable, and add tests around deterministic parsing/deduplication.

---

## Phase 6: Enhanced Remote AI Analyzer (Complete ✅)
**Goal**: Production-ready OpenAI-compatible API integration with chunking.

- [x] Enhance `RemoteAIAnalyzer.kt`:
  - [x] Real JSON parsing from LLM response
  - [x] Configurable chunk size setting in `AppSettings`
  - [x] Chunked processing for large vulnerability lists
  - [x] Improved security-focused prompt template

- [x] Update `AppSettings.kt`:
  - [x] Add `aiChunkSize` field (default 20)

- [x] Update `SettingsRepository.kt` - add `aiChunkSize` CRUD

- [x] Update `SettingsScreen.kt` - add chunk size slider

---

## Phase 7: AICore (Gemini Nano) Integration (Complete ✅)
**Goal**: On-device AI on flagship devices via ML Kit Prompt API.

**Implementation Decision (2026-04-29)**:
- Use ML Kit Prompt API (`com.google.mlkit:genai-prompt:1.0.0-beta2`) with Gemini Nano
- Chunking approach: one request per vulnerability (output ≤256 tokens)
- Fallback chain: AICore → Remote → Heuristics

**Constraints**:
- Output limited to 256 tokens per request
- Quota enforced per app (implement exponential backoff)
- Only foreground inference supported
- Input must be under 4,000 tokens

**Supported Devices (as of 2026)**:
- Pixel 7+ (Gemini Nano)
- Pixel 10+ (Gemini Nano v3)
- Samsung Galaxy S26 series
- Other flagship devices with AICore

**Implementation Plan**:
- [x] Create `AICoreAnalyzer.kt`:
  - [x] ML Kit Prompt API wrapper (single-vuln-per-request)
  - [x] Compact JSON output parsing
  - [x] Fallback to remote/heuristics on failure

- [x] Create `DeviceCapabilities.kt`:
  - [x] Check AICore availability via ML Kit Prompt API status and `getBaseModelName()`
  - [x] Device model detection
  - [x] Memory check (for large vulnerability lists)

- [x] Update `AppModule.kt`:
  - [x] Smart analyzer selection based on device capabilities
  - [x] Fallback chain with proper error handling

**Implementation Note**:
- `minSdk` raised from 24 to 26 because `com.google.mlkit:genai-prompt:1.0.0-beta2` declares API 26 as its minimum.

---

## Phase 8: Device Warning UI (Pending)
**Goal**: Inform users when on-device AI is unavailable.

- [ ] Update `SettingsScreen.kt`:
  - [ ] Show warning banner for unsupported devices
  - [ ] Provide "Switch to Remote AI" button
  - [ ] Link to Remote AI settings

- [ ] Update warning text:
  ```
  "Your device doesn't support on-device AI (Pixel 9+ required).
   Switch to Remote AI for AI-powered analysis."
  ```

---

## Phase 9: Enhanced Security Analysis (Complete ✅)
**Goal**: Better prompts and analysis for security-focused use cases.

- [x] Enhance prompt in `RemoteAIAnalyzer.kt`:
  - [x] Security-focused system prompt with OWASP categories
  - [x] JSON parsing for structured responses
  - [x] Risk scores and severity levels

- [x] Update `DetailsComponents.kt`:
  - [x] Single "AI Security" category display
  - [x] Show AI badges and recommendations

---

## Phase 10: On-device AI Smali Discovery (Initial Implementation Complete ✅)
**Goal**: Let AI discover new vulnerabilities from smali code slices, not only classify existing rule-based findings.

**Implementation Decision (2026-04-29)**:
- Use a hybrid SAST pipeline: deterministic smali candidate extraction first, on-device AI reasoning second.
- Never send whole APK/smali tree to the model. Only analyze small security-relevant method slices.
- Validate AI output and keep heuristic fallback for unsupported devices or parsing failures.

**Pipeline**:
1. `SmaliAnalyzer` keeps producing rule-based findings.
2. `SmaliCandidateExtractor` finds security-relevant smali methods around APIs such as WebView, crypto, storage, networking, reflection, dynamic loading, and process execution.
3. `AICoreSmaliDiscoverer` analyzes each candidate slice with ML Kit Prompt API when available.
4. `HeuristicSmaliDiscoverer` provides deterministic fallback findings.
5. `AnalyzerInteractor` merges and deduplicates discovered findings before AI insight classification.

**Constraints**:
- Candidate slices must stay below the AICore input limit.
- One model request should analyze one slice and return at most one compact JSON finding.
- Findings require validation: known file, positive line, non-empty type/description, confidence threshold.
- This is best-effort discovery, not a replacement for deterministic SAST/dataflow analysis.

**Implementation Plan**:
- [x] Add smali slice model and candidate extractor.
- [x] Add `SmaliVulnerabilityDiscoverer` interface.
- [x] Add heuristic discoverer fallback.
- [x] Add AICore discoverer for on-device candidate analysis.
- [x] Integrate discovery into `AnalyzerInteractor` behind `aiAnalysisEnabled`.
- [x] Surface AI-discovered findings in existing details UI.
- [ ] Tune candidate ranking and prompt quality with real APK samples.
- [ ] Add device-facing warning/copy for experimental AI discovery confidence.

---

## Phase 11: AI Discovery Hardening (Recommended Next)
**Goal**: Reduce false positives and make AI-discovered findings explainable.

- [ ] Add explicit `source`/`confidence` metadata to findings instead of encoding `AI-discovered:` in `description`.
- [ ] Rank `SmaliCodeSlice` candidates before AICore calls using signal severity and method size.
- [ ] Deduplicate before AI calls, not only after findings are generated.
- [ ] Add allow/deny rules for noisy generated/package namespaces.
- [ ] Validate finding lines against the original slice and prefer the exact signal line over method start.
- [ ] Add structured evidence to reports: signal, matched API, method, and confidence.
- [ ] Add sample APK/smali fixtures for discovery regression tests.

**Known risks**:
- Current heuristic TLS detection is intentionally broad and can over-report.
- `SmaliCandidateExtractor` uses `readLines()` and should be revisited for very large smali files.
- AICore discovery currently analyzes at most 40 candidates; quality depends heavily on candidate ordering.

---

## Phase 12: Analysis UX and Lifecycle
**Goal**: Make analysis progress, errors, cancellation, and repeated runs predictable.

- [ ] Split progress states: decompile, rule scan, AI discovery, AI insight classification, report generation.
- [ ] Add cancel action for analysis and ensure cleanup runs once.
- [ ] Prevent launching multiple analyses concurrently from repeated file picker results.
- [ ] Show actionable error state instead of only resetting loading.
- [ ] Keep previous successful report visible when a new analysis fails.
- [ ] Add unsupported-device warning for on-device AI and discovery separately.

---

## Phase 13: Reports and Export Quality
**Goal**: Make exported reports useful for triage.

- [ ] Include AI severity, risk score, recommendation, discovery source, and confidence in TXT/CSV/PDF exports.
- [ ] Escape CSV fields consistently for file, type, and description.
- [ ] Add stable report IDs/timestamps and app version metadata.
- [ ] Improve PDF pagination and line wrapping for long paths/descriptions.
- [ ] Add export failure details to snackbar/error UI.

---

## Phase 14: Test Coverage and Fixtures
**Goal**: Lock down deterministic analysis behavior before expanding rules.

- [ ] Unit-test `SmaliAnalyzer` rules with tiny smali fixtures.
- [ ] Unit-test `SmaliCandidateExtractor` method slicing and signal detection.
- [ ] Unit-test deduplication in `AnalyzerInteractor` or move it to a pure helper.
- [ ] Unit-test `RemoteAIAnalyzer` and `AICoreSmaliDiscoverer` JSON parsing with malformed responses.
- [ ] Add ViewModel tests for loading, success, error, and export result states.

---

## Phase 15: Rule-Based SAST Expansion
**Goal**: Add high-signal deterministic findings before involving AI.

- [ ] Manifest analysis: exported components, debuggable, cleartext traffic, backup flags.
- [ ] Network rules: permissive `TrustManager`, permissive `HostnameVerifier`, hardcoded HTTP URLs.
- [ ] WebView rules: JS bridge, file access, universal access from file URLs.
- [ ] Crypto rules: ECB mode, hardcoded IV/key, MD5/SHA1, weak random.
- [ ] Storage rules: sensitive writes to external storage, logs, SharedPreferences without encryption.
- [ ] Dynamic behavior: `DexClassLoader`, reflection, `Runtime.exec`, native library loading.

---

## Notes

### Supported Devices
- **On-device AI**: Pixel 9+, Samsung S24+ (with AICore/NPU)
- **Remote AI**: Any device with internet
- **Heuristic**: Fallback for all devices

### Chunk Size Guidelines
- Default: 20 vulnerabilities per chunk
- Adjust based on:
  - API rate limits
  - Model context window
  - Device memory

### AI Categories (UI)
Display all AI analysis results under unified "AI Security Analysis" section - let the model categorize internally.

### Current Architecture Notes
- Rule-based findings are represented by `Vulnerability`.
- AI enrichment is represented by `VulnerabilityWithAIInsight`.
- AI discovery currently reuses `Vulnerability` and prefixes descriptions with `AI-discovered:`; Phase 11 should replace this with structured metadata.
- `AnalyzerInteractor` currently owns orchestration, deduplication, report generation, and cleanup; consider extracting pure helpers before adding more complexity.

---

## Implementation Order
1. ~~Phase 6: Remote AI enhancement~~ (Complete)
2. ~~Phase 7: AICore integration~~ (Complete)
3. ~~Phase 10: On-device AI smali discovery~~ (Initial implementation complete)
4. **Phase 11: AI discovery hardening** ← RECOMMENDED NEXT
5. **Phase 12: Analysis UX and lifecycle**
6. **Phase 8: Warning UI for unsupported devices**
7. **Phase 13: Reports and export quality**
8. **Phase 14: Test coverage and fixtures**
9. **Phase 15: Rule-based SAST expansion**
10. ~~Phase 9: Enhanced prompts~~ (Complete)

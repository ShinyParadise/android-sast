# Roadmap: AI-powered Vulnerability Analysis

## Overview
AI analysis capabilities for SAST Android application with on-device (AICore/Gemini Nano) and remote (OpenAI-compatible) inference.

---

## Current Implementation Status: Phase 7 Complete ✅

Remote AI with chunking, Settings UI, AI insights display, and AICore fallback chain all working.

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

## Phase 8: Device Warning UI (Pending after Phase 7)
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

---

## Implementation Order
1. ~~Phase 6: Remote AI enhancement~~ (Complete)
2. ~~Phase 7: AICore integration~~ (Complete)
3. **Phase 8: Warning UI for unsupported devices** ← NEXT
4. ~~Phase 9: Enhanced prompts~~ (Complete)

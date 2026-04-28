# Roadmap: AI-powered Vulnerability Analysis

## Overview
AI analysis capabilities for SAST Android application with on-device (AICore/Gemini Nano) and remote (OpenAI-compatible) inference.

---

## Current Implementation Status: Phase 6 Complete ✅

Remote AI with chunking, Settings UI, AI insights display all working.

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

## Phase 7: AICore (Gemini Nano) Integration
**Goal**: On-device AI on Pixel 9+ via ML Kit Prompt API.

- [ ] Create `AICoreAnalyzer.kt`:
  - [ ] ML Kit Prompt API wrapper
  - [ ] Device capability detection
  - [ ] Fallback to remote if AICore unavailable

- [ ] Create `DeviceCapabilities.kt`:
  - [ ] Check AICore availability
  - [ ] Detect device model (Pixel 9+, Samsung S24+)
  - [ ] NPU/GPU availability

- [ ] Update `AppModule.kt`:
  - [ ] AICore analyzer provider
  - [ ] Fallback chain: AICore → Remote → Heuristic

---

## Phase 8: Device Warning UI
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
1. Phase 6: Remote AI enhancement (chunking + settings)
2. Phase 7: AICore integration
3. Phase 8: Warning UI for unsupported devices
4. Phase 9: Enhanced prompts
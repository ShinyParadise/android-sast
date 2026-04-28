# Roadmap: AI-powered Vulnerability Analysis

## Overview
AI analysis capabilities for SAST Android application with on-device (AICore/Gemini Nano) and remote (OpenAI-compatible) inference.

---

## Current Implementation Status: Phase 1-5 Complete ✅

On-device heuristic analysis is working with Settings UI and AI insights display.

---

## Phase 6: Enhanced Remote AI Analyzer
**Goal**: Production-ready OpenAI-compatible API integration with chunking.

- [ ] Enhance `RemoteAIAnalyzer.kt`:
  - [ ] Real JSON parsing from LLM response
  - [ ] Configurable chunk size setting in `AppSettings`
  - [ ] Chunked processing for large vulnerability lists
  - [ ] Improved security-focused prompt template

- [ ] Update `AppSettings.kt`:
  ```kotlin
  data class AppSettings(
      val aiAnalysisEnabled: Boolean = false,
      val aiAnalysisMode: AIMode = AIMode.ON_DEVICE,
      val remoteModelUrl: String? = null,
      val remoteApiKey: String? = null,
      val selectedRemoteModel: String? = null,
      val aiChunkSize: Int = 20, // vulnerabilities per chunk
  )
  ```

- [ ] Update `SettingsRepository.kt` - add `aiChunkSize` CRUD

- [ ] Update `SettingsScreen.kt` - add chunk size slider/input

---

## Phase 7: AI Core (Gemini Nano) Integration
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

## Phase 9: Enhanced Security Analysis
**Goal**: Better prompts and analysis for security-focused use cases.

- [ ] Enhance prompt in `RemoteAIAnalyzer.kt`:
  ```markdown
  You are a security expert analyzing Android applications.
  Analyze vulnerability findings and provide:
  - Risk score (0.0-1.0)
  - Severity (LOW/MEDIUM/HIGH/CRITICAL)
  - Security recommendation for Android app

  Categories to detect (let model decide):
  - Hardcoded secrets/credentials
  - Insecure cryptographic practices
  - WebView security issues
  - Data storage vulnerabilities
  - Network security issues
  - Permission abuse
  - Intent/activity leakage
  ```

- [ ] Update `DetailsComponents.kt`:
  - [ ] Single "AI Security" category (let model categorize)
  - [ ] Show all AI insights under one section

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
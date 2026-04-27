# Roadmap: AI-powered Vulnerability Analysis

## Overview
Add AI analysis capabilities to the existing SAST Android application. The feature includes on-device heuristic analysis (for VCR) and remote model integration.

## Phase 1: Settings Storage (DataStore)
**Goal**: Persistent app settings storage.

- [ ] Add DataStore Preferences dependency (`androidx.datastore:datastore-preferences`)
- [ ] Create `domain/AppSettings.kt`:
  ```kotlin
  data class AppSettings(
      val aiAnalysisEnabled: Boolean = false,
      val aiAnalysisMode: AIMode = AIMode.ON_DEVICE,
      val remoteModelUrl: String? = null,
      val remoteApiKey: String? = null,
      val selectedRemoteModel: String? = null,
  )
  
  enum class AIMode { ON_DEVICE, REMOTE }
  ```
- [ ] Create `data/SettingsRepository.kt` with DataStore CRUD operations
- [ ] Register in Koin (`AppModule.kt`): `single { SettingsRepository(androidContext()) }`

---

## Phase 2: AI Analysis Domain Layer
**Goal**: Abstract AI analysis with pluggable implementations.

- [ ] Create `domain/VulnerabilityWithAIInsight.kt`:
  ```kotlin
  data class VulnerabilityWithAIInsight(
      val originalVulnerability: Vulnerability,
      val aiRiskScore: Float?, // 0.0..1.0
      val aiSeverity: String?, // "LOW", "MEDIUM", "HIGH", "CRITICAL"
      val aiRecommendation: String?,
  )
  ```

- [ ] Create `domain/VulnerabilityAIAnalyzer.kt` interface:
  ```kotlin
  interface VulnerabilityAIAnalyzer {
      suspend fun analyzeVulnerabilities(
          vulnerabilities: List<Vulnerability>
      ): Result<List<VulnerabilityWithAIInsight>>
  }
  ```

- [ ] Implement `domain/OnDeviceAIAnalyzer.kt` (heuristic-based):
  - `HARDCODED_SECRET` → risk 0.9, recommendation "Use Android Keystore"
  - `INSECURE_CRYPTO` → risk 0.8, "Migrate to AES-GCM"
  - Others → risk 0.5, generic recommendation

- [ ] Implement `domain/RemoteAIAnalyzer.kt`:
  - Add Retrofit dependency
  - HTTP client for remote API calls (OpenAI-compatible endpoint)
  - Request/Response DTOs for vulnerability analysis

---

## Phase 3: Settings Screen
**Goal**: UI for managing AI analysis settings.

- [ ] Add to `navigation/Destination.kt`:
  ```kotlin
  @Serializable data object Settings : Routes
  ```

- [ ] Create `ui/screens/settings/`:
  - `SettingsScreen.kt` - Compose UI
  - `SettingsViewModel.kt` - settings logic (inject `SettingsRepository`)
  - `SettingsComponents.kt` - reusable UI components

- [ ] Add entry in `MainNavGraph.kt` for Settings route

- [ ] Add settings icon (gear) to `MainScreen` and `DetailsScreen` top bars

- [ ] Register in Koin: `viewModel { SettingsViewModel(get()) }`

---

## Phase 4: Integration with Analysis Flow
**Goal**: Trigger AI analysis automatically after successful APK analysis.

- [ ] Extend `AnalysisReport.kt`:
  ```kotlin
  data class AnalysisReport(
      val apkPath: String,
      val vulnerabilities: List<Vulnerability>,
      val summary: String,
      val aiInsights: List<VulnerabilityWithAIInsight>? = null,
  )
  ```

- [ ] Modify `AnalyzerInteractor.kt`:
  - Inject `VulnerabilityAIAnalyzer` and `SettingsRepository`
  - After Smali analysis completes, check if AI enabled in settings
  - If enabled, run AI analysis and update report
  - Emit new state: `AnalysisState.Completed(report)` with AI insights

- [ ] Update `MainViewModel.kt` to pass new dependencies

---

## Phase 5: UI for AI Results
**Goal**: Display AI insights in the details screen.

- [ ] Modify `DetailsScreen.kt` / `DetailsComponents.kt`:
  - Show AI risk score indicator (color-coded)
  - Display AI recommendations per vulnerability
  - Add "AI Analysis" section in Statistics
  - Show AI badge/icon for vulnerabilities with insights

---

## Phase 6: Dependency Injection
**Goal**: Wire everything with Koin.

- [ ] Update `di/AppModule.kt`:
  ```kotlin
  viewModel { SettingsViewModel(get()) }
  
  single<VulnerabilityAIAnalyzer> {
      val settings = get<SettingsRepository>().getSettings()
      if (settings.aiAnalysisMode == AIMode.REMOTE) {
          RemoteAIAnalyzer(get())
      } else {
          OnDeviceAIAnalyzer()
      }
  }
  
  single { SettingsRepository(androidContext()) }
  ```

---

## Implementation Order
1. Phase 1 (DataStore) - foundation
2. Phase 2 (AI Domain) - core logic
3. Phase 3 (Settings UI) - user configuration
4. Phase 4 (Integration) - automatic triggering
5. Phase 5 (Results UI) - visualization
6. Phase 6 (DI) - wiring

## Notes for VCR
- On-device heuristic analysis is sufficient for demonstration
- Remote model integration provides extensibility points
- Keep UI simple but functional
- All AI processing runs on background threads (via Coroutines)

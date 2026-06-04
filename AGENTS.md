# AGENTS

## Minimum map
- Единственный модуль: `:app` (не монорепа).
- Точка входа UI: `app/src/main/java/dev/shinyparadise/sast/ui/MainActivity.kt`; `Application`: `app/src/main/java/dev/shinyparadise/sast/SASTApp.kt`.
- DI только через Koin: `app/src/main/java/dev/shinyparadise/sast/di/AppModule.kt`.
- Основной pipeline анализа: `app/src/main/java/dev/shinyparadise/sast/domain/AnalyzerInteractor.kt`.
- Rule-based smali scan: `app/src/main/java/dev/shinyparadise/sast/data/SmaliAnalyzer.kt`.
- AI discovery: `SmaliCandidateExtractor.kt`, `SmaliVulnerabilityDiscoverer.kt`, `AICoreSmaliDiscoverer.kt`, `HeuristicSmaliDiscoverer.kt`.
- AI enrichment: `VulnerabilityAIAnalyzer.kt`, `AICoreAnalyzer.kt`, `RemoteAIAnalyzer.kt`, `OnDeviceAIAnalyzer.kt`.
- Reports/export: `app/src/main/java/dev/shinyparadise/sast/domain/ReportGenerator.kt`.

## Runbook
- Всегда через wrapper: `./gradlew`.
- Быстрая проверка компиляции: `./gradlew :app:compileDebugKotlin`.
- Базовая локальная проверка: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- `:app:lint` запускать отдельно при необходимости (может выполняться очень долго).

## Android device flow
- Проверка подключения устройства: `adb devices -l`.
- Установка debug-сборки на устройство: `./gradlew :app:installDebug`.
- Ручной запуск приложения: `adb shell am start -n dev.shinyparadise.sast/dev.shinyparadise.sast.ui.MainActivity`.
- Логи только по приложению: `adb logcat --pid $(adb shell pidof -s dev.shinyparadise.sast)`.
- Android CLI: использовать `android` для device-only проверок (screenshot, layout, screen capture), а сборку/тесты через `./gradlew`.
- Локальные скиллы: `.opencode/skills/android-cli/`.
- Глобальные скиллы: `~/.config/opencode/skills/`.

## Don't break
- Не убирать копирование `content://` в файл в `AnalyzerInteractor.getUri(...)` — это путь для SAF URI.
- `ApkDecompiler.cleanup()` удаляет весь `context.cacheDir`; учитывай это при любом добавлении кеша.
- Тестов сейчас нет (`app/src/test`, `app/src/androidTest` пустые), поэтому `:app:testDebugUnitTest` обычно проходит без выполнения тест-кейсов.
- AI discovery по smali должен быть best-effort: сначала deterministic candidate extraction, затем AICore/heuristic анализ маленьких slices. Не отправлять весь smali tree в LLM.
- AI-discovered findings обязательно валидировать и дедуплицировать с rule-based findings перед показом в отчёте.
- Predictive back: системный back должен идти через `NavDisplay`; кастомные блокировки использовать только точечно (`PredictiveBackHandler` во время loading).
- Не делать долгие операции в UI/ViewModel на main thread; decompile/smali/AI/report операции должны оставаться suspend/IO-friendly.

## Current gaps
- У `Vulnerability` нет structured metadata для `source`, `confidence`, `evidence`; AI discovery временно кодирует источник в `description`.
- `SmaliCandidateExtractor` и `SmaliAnalyzer` читают файлы через `readLines()`; для больших APK лучше перейти на streaming/sequence чтение.
- `AnalyzerInteractor` сейчас совмещает orchestration, deduplication, report summary и cleanup; перед расширением pipeline стоит вынести pure helpers.
- Экспорты TXT/CSV/PDF пока почти не включают AI insight/discovery metadata.
- Ошибка анализа сейчас только сбрасывает loading; нет отдельного error UI/retention предыдущего отчёта.

## Suggested next work
1. **AI discovery hardening**: structured finding metadata, candidate ranking, pre-AI dedupe, fixture tests.
2. **Analysis lifecycle UX**: granular progress stages, cancel action, explicit error state, single active analysis guard.
3. **Report quality**: include AI severity/risk/recommendation/source/confidence in TXT/CSV/PDF.
4. **Rule expansion**: manifest scanner, TLS/WebView/crypto/storage deterministic rules.
5. **Performance**: streaming smali reads, candidate caps by severity, avoid duplicated extraction for heuristic and AICore paths.
6. **Testing**: add tiny smali fixtures and pure unit tests before adding more rules.

## Roadmap
Полный план реализации AI-анализа уязвимостей: `ROADMAP.md`.
Кратко:
1. **Phase 1-7**: настройки AI, remote analyzer, AICore analyzer, fallback chain, UI инсайтов.
2. **Phase 8**: warning UI для устройств без on-device AI.
3. **Phase 9**: улучшенные security prompts и structured AI insights.
4. **Phase 10**: AI discovery новых уязвимостей из smali slices (`SmaliCandidateExtractor` + `SmaliVulnerabilityDiscoverer`) с интеграцией в `AnalyzerInteractor`.
5. **Phase 11-15**: hardening discovery, lifecycle UX, report quality, tests, rule-based expansion.

Tech stack: DataStore Preferences, Retrofit (HTTP клиент), ML Kit Prompt API/AICore, Navigation 3, Koin.

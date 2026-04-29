# AGENTS

## Minimum map
- Единственный модуль: `:app` (не монорепа).
- Точка входа UI: `app/src/main/java/dev/shinyparadise/sast/ui/MainActivity.kt`; `Application`: `app/src/main/java/dev/shinyparadise/sast/SASTApp.kt`.
- DI только через Koin: `app/src/main/java/dev/shinyparadise/sast/di/AppModule.kt`.

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

## Roadmap
Полный план реализации AI-анализа уязвимостей: `ROADMAP.md`.
Кратко:
1. **Phase 1-7**: настройки AI, remote analyzer, AICore analyzer, fallback chain, UI инсайтов.
2. **Phase 8**: warning UI для устройств без on-device AI.
3. **Phase 9**: улучшенные security prompts и structured AI insights.
4. **Phase 10**: AI discovery новых уязвимостей из smali slices (`SmaliCandidateExtractor` + `SmaliVulnerabilityDiscoverer`) с интеграцией в `AnalyzerInteractor`.

Tech stack: DataStore Preferences, Retrofit (HTTP клиент), ML Kit Prompt API/AICore, Navigation 3, Koin.

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

## Roadmap
Полный план реализации AI-анализа уязвимостей: `ROADMAP.md`.
Кратко:
1. **Phase 1**: DataStore для настроек (AI on/off, режим on-device/remote, URL модели)
2. **Phase 2**: Модуль AI-анализа (`VulnerabilityAIAnalyzer` интерфейс + `OnDeviceAIAnalyzer` и `RemoteAIAnalyzer` реализации)
3. **Phase 3**: Экран настроек (`SettingsScreen.kt`, `SettingsViewModel.kt`) с навигацией
4. **Phase 4**: Интеграция с `AnalyzerInteractor` — автозапуск AI после обычного анализа
5. **Phase 5**: UI для показа AI-инсайтов в `DetailsScreen`
6. **Phase 6**: Обновление Koin DI (`AppModule.kt`)

Tech stack: DataStore Preferences, Retrofit (HTTP клиент), Navigation 3, Koin.

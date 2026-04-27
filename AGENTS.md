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
- Если на устройстве есть Android CLI, использовать его для device-only проверок (permissions/files/logs), а сборку/тесты оставлять через `./gradlew`.

## Don't break
- Не убирать копирование `content://` в файл в `AnalyzerInteractor.getUri(...)` — это путь для SAF URI.
- `ApkDecompiler.cleanup()` удаляет весь `context.cacheDir`; учитывай это при любом добавлении кеша.
- Тестов сейчас нет (`app/src/test`, `app/src/androidTest` пустые), поэтому `:app:testDebugUnitTest` обычно проходит без выполнения тест-кейсов.

# Питомец Финни

Мобильное приложение для Android — игровой сервис для формирования базовых финансовых навыков у детей 7–11 лет. Прототип по техническому заданию Департамента финансов города Москвы (конкурс «Лидеры цифровой трансформации», 2026).

Ребёнок создаёт виртуального питомца, каждую игровую неделю получает 100 монет и делит их на три направления — обязательное, желаемое, копилка. Покупки, задания и накопления меняют состояние и рост питомца, а итог недели объясняет, что произошло и почему. Без регистрации, интернета, рекламы и реальных денег.

## Состав репозитория

```
app/                      Android-приложение (Kotlin, Jetpack Compose)
  src/main/java/ru/finny/pet/
    domain/               правила игры и модели — чистый Kotlin, без Android
    data/                 чтение контента, сохранение профиля в JSON
    ui/                   экраны, ViewModel, спрайт питомца и его анимации
  src/main/res/drawable-nodpi/   108 пререндеренных 3D-спрайтов питомца (WebP)
  src/main/assets/content/content.json   учебный контент и параметры экономики
  src/test/               JVM-тесты экономики и контента
docs/                     документация (см. ниже), сводный DOCX, презентация PPTX, видео demo.mp4
assets/icon/              иконка 512×512 для магазина
screenshots/store/        скриншоты финального прогона (карточка RuStore, презентация)
tools/ui.py, demo_run.sh  автопрогон сквозного сценария на эмуляторе с записью видео
tools/office/             генераторы DOCX и PPTX из docs/*.md
tools/art/                3D-модель питомца для Blender и импорт спрайтов в ресурсы
release/                  подписанный APK финальной сборки
```

## Быстрый запуск

Нужны JDK 17 и Android SDK (platform 36, build-tools 36.0.0). Android Studio не обязательна.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME=~/Library/Android/sdk
./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Подписанная релизная сборка, демонстрационный режим и сквозной сценарий — в [docs/BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md).

## Стек

Kotlin 2.4 · Jetpack Compose + Material 3 (адаптивная навигация, светлая и тёмная тема, Material Symbols) · палитра и шрифт Montserrat из шаблона ЛЦТ-2026 · kotlinx.serialization · один Gradle-модуль · minSdk 26 (Android 8.0) · без серверной части.

## Документация

| Документ | Содержание |
|---|---|
| [docs/BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md) | окружение, сборка APK, подпись, установка, демо-режим, сброс, тестовые данные |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | компоненты, поток данных, схема обновления контента |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | структура профиля, экономики, заданий и прогресса |
| [docs/ECONOMY.md](docs/ECONOMY.md) | формулы баланса, наград, состояния и роста питомца |
| [docs/CONTENT_MAP.md](docs/CONTENT_MAP.md) | карта образовательного контента: темы, навыки, сценарии, объяснения |
| [docs/REQUIREMENTS_MATRIX.md](docs/REQUIREMENTS_MATRIX.md) | матрица соответствия требованиям 2.5–2.7 ТЗ |
| [docs/UX_ACCESSIBILITY.md](docs/UX_ACCESSIBILITY.md) | UX-решения и настройки доступности |
| [docs/PRIVACY_PERMISSIONS.md](docs/PRIVACY_PERMISSIONS.md) | разрешения Android, данные, удаление профиля |
| [docs/TEST_CASES.md](docs/TEST_CASES.md) | тест-кейсы и отчёт о проверке |
| [docs/LIMITATIONS_ROADMAP.md](docs/LIMITATIONS_ROADMAP.md) | ограничения прототипа и план развития |
| [docs/LICENSES.md](docs/LICENSES.md) | библиотеки, шрифты, изображения и их лицензии |
| [docs/RUSTORE_CARD.md](docs/RUSTORE_CARD.md) | черновик карточки приложения |
| [docs/PLAN.md](docs/PLAN.md) | план работ и принятые решения |

Сводный документ: `docs/Finny_Documentation.docx`; презентация: `docs/Finny_Presentation.pptx`; резервная видеозапись демонстрации: `docs/demo.mp4` (2 мин 50 с). Пересобрать офисные файлы: `node tools/office/build_docx.js && node tools/office/build_pptx.js` (после `npm install` в `tools/office`).

## Реализованные требования

Все обязательные функциональные требования раздела 2.5 ТЗ реализованы — подробности и ссылки на экраны, файлы и тесты в матрице соответствия. Проверка выполнена на эмуляторе Android 16 (arm64); прогон на физическом устройстве команде предстоит повторить перед сдачей (см. TEST_CASES.md).

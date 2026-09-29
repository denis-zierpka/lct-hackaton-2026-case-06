# Питомец Финни — Android-проект

Исходники игры «Питомец Финни». Что это за игра, как поставить готовый APK и включить
демо-режим — в корневом [README.md](../README.md). Здесь — стек, варианты сборки, сборка
и тесты, устройство каталогов и генераторы картинок и звуков.

## Стек

| Что | Чем |
|---|---|
| Язык и интерфейс | Kotlin 2.4.20, Jetpack Compose (BOM 2026.06.01), Material 3, activity-compose 1.12.4, lifecycle-viewmodel-compose 2.10.0 |
| Данные | kotlinx.serialization-json 1.11.0 |
| Тесты | JUnit 4.13.2 |
| Сборка | Gradle 9.4.1 (wrapper, архив сверяется по `distributionSha256Sum`), Android Gradle Plugin 9.2.1, JDK 17 |
| Android | minSdk 26 (Android 8.0), compileSdk и targetSdk 36 |
| Release | R8 и сжатие ресурсов включены |

Один Gradle-модуль `app`, сервера нет. Правила игры (`domain/`) — чистый Kotlin без Android:
без системного времени, случайность — только от переданного seed. Учебный контент и все числа
экономики — в `content.json`, новое событие, товар или мечта добавляются записью в нём, без правки
логики ([ARCHITECTURE.md](docs/ARCHITECTURE.md#схема-обновления-учебного-контента)). Интернета
и запросов разрешений нет — [PRIVACY_PERMISSIONS.md](docs/PRIVACY_PERMISSIONS.md).

## Варианты сборки

Два варианта (flavor `edition`) собираются из одного кода; у них разные пакеты, поэтому на одном
телефоне они ставятся рядом.

| Вариант | Пакет | Название | Что это |
|---|---|---|---|
| `game` | `ru.finny.pet` | Питомец Финни | сдаваемая игра: комната, улица, лавки, работа, события, звуки; только портрет |
| `classic` | `ru.finny.pet.classic` | Финни classic | прежний интерфейс на Material 3, в сдачу не входит |

Общий код — в `app/src/main/`, свой у каждого варианта — в `app/src/game/` и `app/src/classic/`.

## Сборка и тесты

Нужны JDK 17 и Android SDK: `platforms;android-36`, `build-tools;36.0.0`, `platform-tools`.
Android Studio не обязательна: wrapper (`gradlew`, `gradlew.bat`) сам скачает Gradle. Все команды —
из каталога `finny-pet/`.

Путь к SDK — в `local.properties` (в `.gitignore`) или в `ANDROID_HOME`. На Windows двоеточие
после буквы диска экранируется, иначе lint выдаст ошибку `PropertyEscape`:

```
sdk.dir=C\:/Users/<имя>/AppData/Local/Android/Sdk
```

| Что | Команда | Результат |
|---|---|---|
| Тесты `game` | `./gradlew :app:testGameDebugUnitTest` | отчёт `app/build/reports/tests/testGameDebugUnitTest/index.html` |
| Lint `game` | `./gradlew :app:lintGameDebug` | отчёт `app/build/reports/lint-results-gameDebug.txt` |
| Debug `game` | `./gradlew :app:assembleGameDebug` | `app/build/outputs/apk/game/debug/app-game-debug.apk` |
| Release `game` | `./gradlew :app:assembleGameRelease` | `app/build/outputs/apk/game/release/app-game-release.apk` |
| AAB `game` | `./gradlew :app:bundleGameRelease` | `app/build/outputs/bundle/gameRelease/app-game-release.aab` |
| `classic` | те же задачи с `Classic` вместо `Game` | `app/build/outputs/apk/classic/…` |

На Windows — `gradlew.bat` вместо `./gradlew`. Установка на телефон или эмулятор:
`adb install -r <путь к APK>`.

**Тесты.** 600 JVM-тестов в [app/src/test/](app/src/test/java/ru/finny/pet/domain/) проверяют только
`domain/` на настоящем `content.json` — без Android и эмулятора, поэтому одинаковы для обоих
вариантов. Что проверяет каждый класс — [ARCHITECTURE.md](docs/ARCHITECTURE.md#тесты), ручные
сценарии — [TEST_CASES.md](docs/TEST_CASES.md).

**Подпись release.** Параметры ключа берутся из `keystore.properties` (в `.gitignore`, образец —
[keystore.properties.example](keystore.properties.example)) или из переменных `FINNY_STOREFILE`,
`FINNY_STOREPASSWORD`, `FINNY_KEYALIAS`, `FINNY_KEYPASSWORD`. Без них release подписывается
отладочным ключом, и сборка об этом предупреждает. Создание ключа, проверка подписи и установка —
[BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md).

## Структура каталогов

```
app/src/
  main/                              общее для обоих вариантов
    java/ru/finny/pet/domain/        правила игры, чистый Kotlin; town/ — правила варианта game
    java/ru/finny/pet/data/          ContentRepository (читает content.json), StateStore (профиль в state.json)
    java/ru/finny/pet/PetSprites.kt  таблица спрайтов питомца, генерируется tools/art/import_sprites.py
    assets/content/content.json      учебный контент и числа экономики
    res/drawable-nodpi/              108 спрайтов питомца: 3 вида × 3 цвета × 3 стадии × 4 выражения
    res/font/, mipmap-*, values*/    шрифт Montserrat, иконка приложения, общие ресурсы
  game/                              сдаваемый вариант; манифест закрепляет портрет
    java/ru/finny/pet/game/          GameApp (навигация), GameViewModel, screens/, ui/, audio/, mock/ (макеты, только в debug)
    res/drawable-nodpi/              комната и мебель, улица и фасады, фоны мест, жители, товары, мечты, иконки
    res/raw/                         13 звуков и фоновая музыка (OGG)
  classic/                           вариант classic
  test/java/ru/finny/pet/domain/     JVM-тесты правил; town/ — тесты варианта game
assets/icon/icon-512.png             иконка 512 × 512 для магазина приложений
docs/                                документация для сдачи
release/                             готовый APK
screenshots/game/                    ключевые экраны
screenshots/town/, screenshots/device/  рабочие снимки проверок
tools/art/                           генераторы картинок и звуков
tools/office/, tools/ui.py, tools/demo_run.sh   служебные скрипты варианта classic (документы DOCX/PPTX, автопрогон на эмуляторе)
```

## Генераторы ассетов

Все картинки и звуки уже лежат в репозитории; генераторы нужны, только чтобы их изменить. Картинки
рендерит Blender 5.2 скриптами из [tools/art/](tools/art/), звуки синтезирует Python.

| Скрипт | Что делает |
|---|---|
| `pet.py` | питомец (108 кадров) и жители городка (`--residents`) |
| `room.py` | комната и мебель |
| `place.py` | фоны мест: рынок у реки, «У Фомы», пекарня |
| `facade.py` | фасады мест на улице и фон улицы |
| `props.py` | товары, мечты, вещи комнаты, выпечка пекарни, плитки мини-игры, монета |
| `uiprops.py` | иконки интерфейса: банки, копилка, кошелёк и др. |
| `sounds.py` | 13 звуковых эффектов и музыкальная петля в `app/src/game/res/raw/` (нужен `ffmpeg`) |
| `import_sprites.py`, `to_webp.py` | перевод PNG в WebP (нужен Pillow); `import_sprites.py` ещё обновляет `PetSprites.kt` |
| `lib.py`, `smoke.py` | общие помощники Blender и их проверка |

Пример — питомец:

```
<blender> -b -P tools/art/pet.py -- --all <каталог> --size 512
python tools/art/import_sprites.py <каталог>
```

Команды для каждого скрипта — [BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md#генерация-ассетов),
что куда попадает — [ARCHITECTURE.md](docs/ARCHITECTURE.md#генераторы-ассетов).

## Документация

Для разработчика — [ARCHITECTURE.md](docs/ARCHITECTURE.md), [DATA_MODEL.md](docs/DATA_MODEL.md),
[ECONOMY.md](docs/ECONOMY.md), [BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md),
[TEST_CASES.md](docs/TEST_CASES.md); история выпусков — [CHANGELOG.md](CHANGELOG.md); архив планов
разработки — [PLAN.md](docs/PLAN.md), [GAME_PLAN.md](docs/GAME_PLAN.md). Полный список
документов — в [корневом README](../README.md#документация).

## Лицензии

Открытой лицензии нет. Сторонние библиотеки, шрифт, изображения и звуки с их лицензиями —
[docs/LICENSES.md](docs/LICENSES.md).

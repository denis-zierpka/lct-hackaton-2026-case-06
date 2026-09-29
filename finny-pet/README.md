# Питомец Финни

Игра для Android, в которой ребёнок 7–11 лет учится распоряжаться карманными деньгами, ухаживая
за виртуальным питомцем. Кейс 06 конкурса «Лидеры цифровой трансформации» 2026, заказчик —
Департамент финансов города Москвы. Без регистрации, интернета, рекламы и реальных денег.

В выпуске 1.4.1 вариант `game` играет по концепции «Городок» ([../docs/GAME_CONCEPT.md](../docs/GAME_CONCEPT.md)).
Главный экран — комната с питомцем. Каждую неделю почтальон приносит конверт со 100 монетами, ребёнок
раскладывает их по банкам «Нужное», «Хочу» и «В копилку», покупает в двух лавках с разными ценами через
кассу, работает у соседей (заработок приходит со следующим конвертом), копит на мечту и решает события
городка поступком. Правила — `domain/town`, формулы — [docs/ECONOMY.md](docs/ECONOMY.md), что изменилось
с 1.3.0 — [CHANGELOG.md](CHANGELOG.md). Выпуск 1.3.0 (план, магазин, задания) остался в теге `v1.3.0`,
вариант `classic` работает по его правилам.

Описание для эксперта и статусы требований — в корневом [README.md](../README.md) и в
[матрице требований](docs/REQUIREMENTS_MATRIX.md). Здесь — стек, сборка и устройство каталогов.

## Промежуточная сдача (ТЗ 7.1, до 2026-09-29)

| Пункт 7.1 | Где |
|---|---|
| 2. README: стек, запуск, реализованные требования | корневой [README.md](../README.md); стек, запуск и каталоги — этот файл |
| 3. Рабочая сборка APK | [release/finny-pet-1.4.1-release.apk](release/finny-pet-1.4.1-release.apk) |
| 4. Архитектура и структура данных | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DATA_MODEL.md](docs/DATA_MODEL.md) |
| 5. Ключевые экраны | [создание питомца](screenshots/game/01_create.png), [комната](screenshots/game/02_room.png), [банки](screenshots/game/03_jars.png), [копилка](screenshots/game/04_savings.png), [улица](screenshots/game/05_street.png), [пекарня](screenshots/game/06_bakery.png) |
| 6. Требования со статусами и планом до финала | статусы — [docs/REQUIREMENTS_MATRIX.md](docs/REQUIREMENTS_MATRIX.md), план — раздел «План до финала» в [docs/LIMITATIONS_ROADMAP.md](docs/LIMITATIONS_ROADMAP.md) |
| 7. Вопросы к заказчику | [docs/QUESTIONS.md](docs/QUESTIONS.md) |

Сдаётся вариант сборки `game`:

| Параметр | Значение |
|---|---|
| Пакет, название | `ru.finny.pet`, «Питомец Финни» |
| Версия | 1.4.1 (versionCode 6), minSdk 26 (Android 8.0), targetSdk 36 |
| Экран | только портрет |
| Подпись | отладочный ключ (`CN=Android Debug`), так как `keystore.properties` нет. Постоянный ключ появится к финалу |
| Разрешения | пользователь не видит ни одного запроса. В итоговом манифесте есть одно signature-разрешение `ru.finny.pet.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, его добавляет `androidx.core`. Интернета нет, `allowBackup=false`, подробнее в [PRIVACY_PERMISSIONS.md](docs/PRIVACY_PERMISSIONS.md) |
| Размер APK | 4 637 942 байт |

## Стек

Kotlin 2.4.20, Jetpack Compose (BOM 2026.06.01) и Material 3, kotlinx.serialization 1.11.0, JUnit 4.13.2.
Сборка: AGP 9.2.1, Gradle 9.4.1 (wrapper, дистрибутив проверяется по `distributionSha256Sum`), JDK 17,
compileSdk и targetSdk 36. Один Gradle-модуль `app`, сервера нет. Картинки — питомец, жители, комната
и мебель, фоны мест, улица, вещи — рендерятся процедурно в Blender скриптами из [tools/art/](tools/art/),
звуки синтезирует `tools/art/sounds.py`.

Варианты сборки (flavor `edition`) собираются из одного кода:

| Вариант | Пакет | Название | Версия | Статус |
|---|---|---|---|---|
| `game` | `ru.finny.pet` | Питомец Финни | 1.4.1 | сдаётся: «Городок» — комната, улица, лавки, работа, события, звуки |
| `classic` | `ru.finny.pet.classic` | Финни classic | 1.4.1-classic | остаётся в репозитории, в сдачу не входит: обычный интерфейс на Material 3, правила 1.3.0 |

## Структура каталогов

```
app/src/
  main/                              общее для обоих вариантов
    java/ru/finny/pet/domain/        общая модель и правила: Economy (1.3.0 и основа «Городка»), GameState, Content, Match3 (чистый Kotlin)
    java/ru/finny/pet/domain/town/   «Городок» — правила game: Town, Tray, Prices, TownEvents, Migration, PetTalk
    java/ru/finny/pet/data/          ContentRepository (читает content.json), StateStore (профиль в JSON)
    java/ru/finny/pet/PetSprites.kt  таблица спрайтов питомца, её генерирует tools/art/import_sprites.py
    assets/content/content.json      учебный контент и числа экономики (rules); «Городок» — ключ town
    res/drawable-nodpi/              108 спрайтов питомца: 3 вида × 3 цвета × 3 стадии × 4 выражения
    res/font/, mipmap-*, values*/    шрифт Montserrat, иконка приложения, общие ресурсы
  game/                              сдаваемый вариант; AndroidManifest.xml закрепляет портрет
    java/ru/finny/pet/game/          GameApp (навигация), GameViewModel, screens/, ui/, audio/, mock/ (макеты, видны только в debug)
    res/drawable-nodpi/              комната и мебель, фоны мест и улицы, фасады, жители, товары, мечты, выпечка, иконки
    res/raw/                         13 звуков и фоновая музыка (OGG)
  classic/java/ru/finny/pet/ui/      альтернативный вариант на правилах 1.3.0
  test/java/ru/finny/pet/domain/     JVM-тесты: EconomyTest, MvpRulesTest, ContentTest, Match3Test; town/ — тесты «Городка»
  test/resources/town/               профиль 1.3.0 для проверки переноса сохранения (MigrationTest)
assets/icon/icon-512.png             иконка 512×512 для магазина
docs/                                документация для сдачи (список ниже)
release/                             APK для сдачи
screenshots/game/                    ключевые экраны game для 7.1
screenshots/town/, screenshots/device/  листы проверок «Городка» и снимки 1.3.0 с телефона S23
tools/art/                           генераторы ассетов: pet.py (питомец и жители), room.py, place.py, facade.py,
                                     props.py, uiprops.py (Blender), sounds.py; общие lib.py и to_webp.py
tools/office/                        build_docx.js: DOCX из README.md и docs/*.md; build_pptx.js: PPTX, текст слайдов
                                     в самом скрипте. Скриншоты оба берут из screenshots/store/, удалённого в 5dace68
tools/ui.py, tools/demo_run.sh       автопрогон сценария на эмуляторе, написан под classic и macOS, для game не адаптирован
```

Уровнем выше лежат процесс разработки, ТЗ и спеки задач: [../docs/](../docs/), спеки —
`../docs/tasks/<ID>.md` (MVP-* — выпуск 1.3.0, TOWN-* — «Городок»), план 3D-питомца — [../docs/ART_PIPELINE.md](../docs/ART_PIPELINE.md).
Скрипты проверки экранов и арта — в [../tools/](../tools/): `adbui.sh`, `art_check.py`, `emu.sh`.

## Быстрый запуск

Нужны JDK 17 и Android SDK (platform 36, build-tools 36.0.0). Android Studio не обязательна: wrapper
(`gradlew`, `gradlew.bat`) лежит в репозитории и сам скачает Gradle 9.4.1 с проверкой
`distributionSha256Sum`. Все команды выполняются из каталога `finny-pet/`.

**Windows.** `JAVA_HOME` должен указывать на JDK 17. В `local.properties` двоеточие нужно экранировать,
иначе lint выдаст ошибку PropertyEscape:

```
sdk.dir=C\:/Users/<имя>/AppData/Local/Android/Sdk
```

```bat
gradlew.bat :app:testGameDebugUnitTest :app:assembleGameDebug
adb install -r app\build\outputs\apk\game\debug\app-game-debug.apk
```

**Linux.**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=~/android-sdk
./gradlew :app:testGameDebugUnitTest :app:assembleGameDebug
adb install -r app/build/outputs/apk/game/debug/app-game-debug.apk
```

Релизная сборка: `:app:assembleGameRelease` собирает `app/build/outputs/apk/game/release/app-game-release.apk`
(R8 и сжатие ресурсов включены). Без `keystore.properties` (образец — `keystore.properties.example`) или
переменных `FINNY_STOREFILE`, `FINNY_STOREPASSWORD`, `FINNY_KEYALIAS`, `FINNY_KEYPASSWORD` release
подписывается отладочным ключом, и сборка предупреждает об этом. Вариант classic собирается так:
`:app:testClassicDebugUnitTest`, `:app:assembleClassicDebug`.

Демо-режим: «Для взрослого» (на титульном экране — кнопка, в комнате — значок замка), пример на умножение,
затем «Создать тестовый профиль (демо)» или переключатель «Демо-режим». Календаря нет в любом режиме: в неделе
3 игровых дня, день кончается кроватью «Сон», последний — итогом недели, поэтому недели идут подряд. Демо-режим
добавляет на доске «События» блок «Все события (демо)» с кнопкой «Начать», а на экране ночи после раскладки —
«Сразу к итогу недели». Там же, в разделе для взрослого, — сброс и удаление профиля. Сценарий показа —
в [docs/BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md).

Настройки доступности (ТЗ 3.6) — переключатели «Звуки», «Музыка» (по умолчанию выключена) и «Анимации»
в разделе для взрослого, см. [docs/UX_ACCESSIBILITY.md](docs/UX_ACCESSIBILITY.md).

## Документы

- [BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md) — окружение, сборка release APK, подпись, установка, демо-режим, сброс, тестовые данные (ТЗ 5.2)
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — компоненты, варианты сборки, поток данных, обновление контента (5.3)
- [DATA_MODEL.md](docs/DATA_MODEL.md) — структура профиля, контента, «Городка» и прогресса (5.4)
- [REQUIREMENTS_MATRIX.md](docs/REQUIREMENTS_MATRIX.md) — матрица соответствия (5.5) для `game` на «Городке», со статусами и способом проверки; проверки 1.3.0 помечены отдельно
- [ECONOMY.md](docs/ECONOMY.md) — формулы баланса, наград, состояния и роста питомца (5.6)
- [CONTENT_MAP.md](docs/CONTENT_MAP.md) — карта контента: темы, навыки, события, объяснения (5.7)
- [UX_ACCESSIBILITY.md](docs/UX_ACCESSIBILITY.md) — обоснование UX/UI и настройки доступности (5.8)
- [PRIVACY_PERMISSIONS.md](docs/PRIVACY_PERMISSIONS.md) — разрешения, собираемые данные, удаление профиля (5.9)
- [TEST_CASES.md](docs/TEST_CASES.md) — тест-кейсы и отчёт о проверке (5.10)
- [LIMITATIONS_ROADMAP.md](docs/LIMITATIONS_ROADMAP.md) — ограничения прототипа и план развития (5.11)
- [LICENSES.md](docs/LICENSES.md) — библиотеки, шрифты, изображения, звуки и их лицензии (5.12)
- [QUESTIONS.md](docs/QUESTIONS.md) — вопросы и ограничения для консультации с заказчиком (7.1 п. 7)
- [USER_TESTING.md](docs/USER_TESTING.md) — протокол пользовательской и экспертной проверки (8.4)
- [RUSTORE_CARD.md](docs/RUSTORE_CARD.md) — черновик карточки RuStore
- [GAME_PLAN.md](docs/GAME_PLAN.md) — исторический: план перехода «из приложения в игру» до 1.3.0
- [PLAN.md](docs/PLAN.md) — исторический: план работ до 1.2.0

К финалу будут: сводный документ DOCX, презентация, видео демонстрации, скриншоты для RuStore и 3D-питомец.
Материалы classic (DOCX, PPTX, видео, скриншоты, APK 1.2.0) удалены из дерева коммитом `5dace68`.
В истории git они остались, например в коммите `83404be`.

## Лицензии

Сторонние библиотеки, шрифт, изображения и звуки перечислены с лицензиями в
[docs/LICENSES.md](docs/LICENSES.md). Под открытой лицензией код не публикуется.

## Команда

«Сингулярность Бытия»: Ким Владимир, Цирпка Денис. Прототипа в Figma не было.

Актуально на версию 1.4.1 (2026-09-29)

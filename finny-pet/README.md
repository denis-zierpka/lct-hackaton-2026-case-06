# Питомец Финни

Игра для Android, в которой ребёнок 7–11 лет учится распоряжаться карманными деньгами, ухаживая
за виртуальным питомцем. Кейс 06 конкурса «Лидеры цифровой трансформации» 2026, заказчик —
Департамент финансов города Москвы. Без регистрации, интернета, рекламы и реальных денег.

В выпуске 1.3.0 каждую игровую неделю ребёнок получает 100 монет и до покупок делит их на три части:
**обязательное** (еда и уход), **желаемое** (игрушки), **копилка** (на цель). Дальше он покупает
товары, решает задания (+10 монет за верный ответ, +5 за неверный, объяснение даётся в обоих случаях)
и копит на цель. Итог недели сравнивает план с фактом и объясняет, как решения сказались на питомце.
Мини-игра «Монетки в ряд» открывается только после плана: 1 монета за 20 очков, не больше 30 в
неделю. Взрослый может дать бонус +10 монет, не больше 3 раз в неделю. Эти доходы попадают в итог
недели отдельными строками «Вне плана». Все формулы — в [docs/ECONOMY.md](docs/ECONOMY.md).

В ветке `feat/town` (задача TOWN-S1d — реализовано, ждёт ревью) вариант `game` переведён на концепцию «Городок»
([../docs/GAME_CONCEPT.md](../docs/GAME_CONCEPT.md)) и движок `domain/town`: главный экран — комната,
монеты раскладываются по банкам «Нужное», «Хочу», «В копилку», покупки — в двух лавках с разными
ценами через кассу, заработок — смены у соседей (приходит с новым конвертом), вместо заданий —
события городка. Экранов плана, магазина и заданий 1.3.0 в `game` нет, `classic` работает по правилам 1.3.0.

## Промежуточная сдача (ТЗ 7.1, до 2026-09-29)

| Пункт 7.1 | Где |
|---|---|
| 2. README: стек, запуск, реализованные требования | этот файл |
| 3. Рабочая сборка APK | [release/finny-pet-1.3.0-release.apk](release/finny-pet-1.3.0-release.apk) |
| 4. Архитектура и структура данных | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DATA_MODEL.md](docs/DATA_MODEL.md) |
| 5. Ключевые экраны | [создание питомца](screenshots/game/01_create.png), [главный экран](screenshots/game/02_room.png), [план](screenshots/game/03_plan.png), [копилка](screenshots/game/04_savings.png) |
| 6. Требования со статусами и планом до финала | статусы — таблица «Реализованные требования» ниже, план — раздел «План до финала» в [docs/LIMITATIONS_ROADMAP.md](docs/LIMITATIONS_ROADMAP.md) |
| 7. Вопросы к заказчику | [docs/QUESTIONS.md](docs/QUESTIONS.md) |

Сдаётся вариант сборки `game`:

| Параметр | Значение |
|---|---|
| Пакет, название | `ru.finny.pet`, «Питомец Финни» |
| Версия | 1.3.0 (versionCode 4), minSdk 26 (Android 8.0), targetSdk 36 |
| Подпись | отладочный ключ (`CN=Android Debug`), так как `keystore.properties` нет. Постоянный ключ появится к финалу |
| Разрешения | пользователь не видит ни одного запроса. В итоговом манифесте есть одно signature-разрешение `ru.finny.pet.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, его добавляет `androidx.core`. Интернета нет, `allowBackup=false`, подробнее в [PRIVACY_PERMISSIONS.md](docs/PRIVACY_PERMISSIONS.md) |
| Размер APK | ≈ 3,9 МБ |

## Стек

Kotlin 2.4.20, Jetpack Compose (BOM 2026.06.01) и Material 3, kotlinx.serialization 1.11.0, JUnit 4.13.2.
Сборка: AGP 9.2.1, Gradle 9.4.1 (wrapper, дистрибутив проверяется по `distributionSha256Sum`), JDK 17,
compileSdk и targetSdk 36. Один Gradle-модуль `app`, сервера нет. Графика питомца, комнаты и предметов
рендерится процедурно в Blender скриптами из [tools/art/](tools/art/), звуки синтезирует
`tools/art/sounds.py`.

Варианты сборки (flavor `edition`) собираются из одного кода:

| Вариант | Пакет | Название | Версия | Статус |
|---|---|---|---|---|
| `game` | `ru.finny.pet` | Питомец Финни | 1.3.0 | сдаётся: комната, звуки, мини-игра |
| `classic` | `ru.finny.pet.classic` | Финни classic | 1.3.0-classic | остаётся в репозитории, в сдачу не входит: обычный интерфейс на Material 3 |

## Структура каталогов

```
app/src/
  main/                              общее для обоих вариантов
    java/ru/finny/pet/domain/        правила игры: Economy, GameState, Content, Match3 (чистый Kotlin)
    java/ru/finny/pet/domain/town/   «Городок»: Town, Prices, TownEvents, Migration, PetTalk — правила game
    java/ru/finny/pet/data/          ContentRepository (читает content.json), StateStore (профиль в JSON)
    java/ru/finny/pet/PetSprites.kt  таблица спрайтов питомца, её генерирует tools/art/import_sprites.py
    assets/content/content.json      учебный контент и числа экономики (rules)
    res/drawable-nodpi/              108 спрайтов питомца: 3 вида × 3 цвета × 3 стадии × 4 выражения
    res/font/, mipmap-*, values*/    шрифт Montserrat, иконка приложения, общие ресурсы
  game/                              сдаваемый вариант
    java/ru/finny/pet/game/          GameApp (навигация), GameViewModel, screens/, ui/, audio/
    res/drawable-nodpi/              комната, товары, цели, фишки мини-игры, элементы интерфейса
    res/raw/                         звуки и фоновая музыка (OGG)
  classic/java/ru/finny/pet/ui/      альтернативный вариант
  test/java/ru/finny/pet/domain/     JVM-тесты: EconomyTest, MvpRulesTest, ContentTest, Match3Test; town/ — тесты «Городка»
assets/icon/icon-512.png             иконка 512×512 для магазина
docs/                                документация для сдачи (список ниже)
release/                             APK для сдачи
screenshots/game/                    ключевые экраны game для 7.1
tools/art/                           генераторы ассетов: pet.py, room.py, props.py, uiprops.py (Blender), sounds.py
tools/office/                        build_docx.js: DOCX из README.md и docs/*.md; build_pptx.js: PPTX, текст слайдов
                                     в самом скрипте. Скриншоты оба берут из screenshots/store/, удалённого в 5dace68
tools/ui.py, tools/demo_run.sh       автопрогон сценария на эмуляторе, написан под classic и macOS, для game не адаптирован
```

Уровнем выше лежат процесс разработки, ТЗ и спеки задач: [../docs/](../docs/), спеки этого этапа —
`../docs/tasks/MVP-T*.md`, план 3D-питомца — [../docs/ART_PIPELINE.md](../docs/ART_PIPELINE.md).

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

Демо-режим: «Для взрослого», затем пример на умножение, затем «Создать тестовый профиль (демо)» или
переключатель «Демо-режим» (в 1.3.0 все задания открыты сразу; в ветке `feat/town` — события кнопкой
«Начать» в разделе «Все события (демо)» доски «События», неделя — «Сразу к итогу недели» на экране ночи после раскладки). Календаря нет в любом режиме: неделю завершает
кнопка, поэтому недели идут подряд. Там же находятся сброс и удаление профиля. Подробнее — в [docs/BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md).

## Реализованные требования (ТЗ 2.5, вариант game)

Статусы — по ТЗ 7.1 п. 6: «реализовано», «в работе», «не начато» (последнего нет). План до финала — раздел
«План до финала» в [docs/LIMITATIONS_ROADMAP.md](docs/LIMITATIONS_ROADMAP.md). Таблица — по выпуску 1.3.0,
экраны — в `app/src/game/java/ru/finny/pet/game/screens/` на теге `v1.3.0`: в ветке `feat/town`
`PlanScreen.kt`, `ShopScreen.kt` и `TaskScreens.kt` удалены (TOWN-S1d).

| ТЗ | Статус | Что есть | Где |
|---|---|---|---|
| 2.5.1 | реализовано | знакомство, гостевой профиль без персональных данных, подсказка с главного экрана | `StartScreens.kt`, `RoomScreen.kt` |
| 2.5.2 | реализовано | 3 вида (кот, зайка, щенок) × 3 цвета, игровое имя | `StartScreens.kt` |
| 2.5.3 | реализовано | главный экран-комната: питомец, баланс, копилка, цель, плашка «Сейчас», переходы ко всем разделам | `RoomScreen.kt` |
| 2.5.4 | реализовано | только игровые монеты, у каждого начисления указаны источник и сумма («Откуда монеты») | `ProgressScreens.kt`, `Economy.kt` |
| 2.5.5 | реализовано | план по 3 направлениям, контроль бюджета, план и факт, строки «Вне плана» в итоге недели | `PlanScreen.kt`, `ProgressScreens.kt` |
| 2.5.6 | реализовано | 10 товаров двух типов, покупка с подтверждением, при нехватке монет — объяснение | `ShopScreen.kt` |
| 2.5.7 | реализовано | 4 готовые цели и своя, взносы, срок по среднему взносу, снятие с предпросмотром | `SavingsScreen.kt` |
| 2.5.8 | в работе: нет заданий-действий, план п. 5 | 10 заданий по 3 темам (бюджет, сбережения, покупки), объяснение после любого ответа | `TaskScreens.kt`, `content.json` |
| 2.5.9 | реализовано | обратная связь после действия, итог недели с причинами и следующим шагом | `ProgressScreens.kt`, `Economy.kt` |
| 2.5.10 | реализовано | 3 стадии роста, 4 выражения, объяснение настроения | `ui/PetSprite.kt`, `Economy.kt` |
| 2.5.11 | реализовано | прогресс, итоги недели, справка из 10 терминов | `ProgressScreens.kt` |
| 2.5.12 | реализовано | раздел для взрослого за барьером (двузначное × однозначное), бонус ребёнку: 4 причины, до 3 раз в неделю | `ParentScreen.kt` |
| 2.5.13 | реализовано | профиль сохраняется в JSON, демо-режим, тестовый профиль, сброс и удаление | `data/StateStore.kt`, `ParentScreen.kt` |
| 2.5.14 | реализовано | новое задание с существующими темой и типом — запись в `content.json` без правки кода. Товар и цель — тоже, но картинку выбирают `itemRes`/`goalRes`: без новой строки там товар рисуется палаткой, цель — картинкой своей цели. `ContentTest.kt` проверяет формат и минимумы записей | `content.json`, `RoomScreen.kt` |

Настройки доступности (ТЗ 3.6) — переключатели «Звуки», «Музыка» (по умолчанию выключена) и «Анимации»
в разделе для взрослого, см. [docs/UX_ACCESSIBILITY.md](docs/UX_ACCESSIBILITY.md).

## Проверка (2026-09-25)

| Что | Результат |
|---|---|
| JVM-тесты | 117 тестов в каждом варианте, 0 падений |
| Lint | 0 ошибок |
| Эмулятор Android 16 x86_64 | портрет 360×640 dp и 411×914 dp, холодный старт release ≈ 0,45–0,49 с (`am start -W`, 3 замера), проверены создание питомца, главный экран, план, мини-игра, раздел для взрослого, бонус, звуки; задания, покупки, копилка, завершение недели, перезапуск, сброс не проверялись — [docs/TEST_CASES.md](docs/TEST_CASES.md) |
| Физическое устройство | Samsung Galaxy S23 (Android 14, 8 ГБ, 360 dp), 2026-09-25: сценарий Приложения А (шаги 1–12), холодный старт 0,22–0,37 с — [TEST_CASES.md](docs/TEST_CASES.md#отчёт-с-физического-устройства) |
| Не проверено | звук и плавность на слух и на глаз. Пользовательская проверка (ТЗ 8.4) не проводилась, её протокол — [docs/USER_TESTING.md](docs/USER_TESTING.md) |

## Документы

- [BUILD_AND_DEMO.md](docs/BUILD_AND_DEMO.md) — окружение, сборка release APK, подпись, установка, демо-режим, сброс, тестовые данные (ТЗ 5.2)
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — компоненты, варианты сборки, поток данных, обновление контента (5.3)
- [DATA_MODEL.md](docs/DATA_MODEL.md) — структура профиля, экономики, заданий и прогресса (5.4)
- [REQUIREMENTS_MATRIX.md](docs/REQUIREMENTS_MATRIX.md) — матрица соответствия (5.5) версии 1.0, вариант classic; под game 1.3.0 не обновлена, актуальные статусы — в таблице выше
- [ECONOMY.md](docs/ECONOMY.md) — формулы баланса, наград, состояния и роста питомца (5.6)
- [CONTENT_MAP.md](docs/CONTENT_MAP.md) — карта контента: темы, навыки, сценарии, объяснения (5.7)
- [UX_ACCESSIBILITY.md](docs/UX_ACCESSIBILITY.md) — обоснование UX/UI и настройки доступности (5.8)
- [PRIVACY_PERMISSIONS.md](docs/PRIVACY_PERMISSIONS.md) — разрешения, собираемые данные, удаление профиля (5.9)
- [TEST_CASES.md](docs/TEST_CASES.md) — тест-кейсы и отчёт о проверке (5.10)
- [LIMITATIONS_ROADMAP.md](docs/LIMITATIONS_ROADMAP.md) — ограничения прототипа и план развития (5.11)
- [LICENSES.md](docs/LICENSES.md) — библиотеки, шрифты, изображения, звуки и их лицензии (5.12)
- [QUESTIONS.md](docs/QUESTIONS.md) — вопросы и ограничения для консультации с заказчиком (7.1 п. 7)
- [USER_TESTING.md](docs/USER_TESTING.md) — протокол пользовательской и экспертной проверки (8.4)
- [RUSTORE_CARD.md](docs/RUSTORE_CARD.md) — черновик карточки RuStore
- [GAME_PLAN.md](docs/GAME_PLAN.md) — план перехода «из приложения в игру»: экраны, трудоёмкость, ассеты
- [PLAN.md](docs/PLAN.md) — план работ и принятые решения

К финалу будут: сводный документ DOCX, презентация, видео демонстрации, скриншоты для RuStore и 3D-питомец.
Материалы classic (DOCX, PPTX, видео, скриншоты, APK 1.2.0) удалены из дерева коммитом `5dace68`.
В истории git они остались, например в коммите `83404be`.

## Лицензии

Сторонние библиотеки, шрифт, изображения и звуки перечислены с лицензиями в
[docs/LICENSES.md](docs/LICENSES.md). Под открытой лицензией код не публикуется.

## Команда

«Сингулярность Бытия»: Ким Владимир, Цирпка Денис. Прототипа в Figma не было.

Актуально на версию 1.3.0 (2026-09-25)

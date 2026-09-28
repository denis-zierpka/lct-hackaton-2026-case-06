# Бэклог и открытые вопросы

Живой список. Статус на 2026-09-27, ветка `feat/town` («Городок»); выпуск — 1.3.0 (`main`, тег `v1.3.0`).
Промежуточная сдача (ТЗ 7.1) — до 2026-09-29. Задачи берутся в работу через контур
(`docs/WORKFLOW.md`), спека — в `docs/tasks/<ID>.md`.

## Решено

- **Сдаём вариант `game`** (решение сеньора 2026-09-23). Выполнено MVP-T05
  ([`app/build.gradle.kts`](../finny-pet/app/build.gradle.kts)): `game` — пакет `ru.finny.pet`,
  «Питомец Финни», `versionCode 4`, `versionName 1.3.0`; `classic` — `ru.finny.pet.classic`,
  «Финни classic», `1.3.0-classic`, остаётся в репозитории, в сдачу не входит.
- Питомец переходит на сгенерированные 3D-модели с ручной анимацией в Blender — к финалу,
  план — `docs/ART_PIPELINE.md`. До промежуточной сдачи — процедурный рендер `tools/art/pet.py`.
- **Питомцы на четырёх лапах; щенок вместо дракона** (решения сеньора 2026-09-25).
  Третий вид обязателен: ТЗ 2.6 — «Не менее 9 визуально различимых комбинаций»,
  без него 2 × 3 = 6. Выполнено MVP-T02: 108 кадров `pet_*` (кот, зайка, щенок × 3 цвета ×
  3 стадии × 4 выражения) в `app/src/main/res/drawable-nodpi/`, слово `dragon` в `app/src` и
  `tools/art` не встречается. Сохранённый профиль с `dragon` не падает: в game
  [`GameViewModel`](../finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt) при загрузке
  заменяет неизвестные вид и цвет первыми из контента ([`Content.species()`](../finny-pet/app/src/main/java/ru/finny/pet/domain/Content.kt),
  `color()`) — питомец рисуется котом со всеми выражениями и стадиями (MVP-T12); в classic
  [`PetSprites.id()`](../finny-pet/app/src/main/java/ru/finny/pet/PetSprites.kt) даёт запасной кадр (рыжий кот).
- **Мини-игра — только после подтверждённого плана** (решение сеньора, ТЗ 2.8, спека MVP-T03) — правило
  выпуска 1.3.0. С TOWN-S1d в `game` оплачиваемые смены открыты и до плана, заработок уходит в следующий
  конверт; после трёх оплачиваемых смен работа закрыта до нового конверта, игры «ради рекорда» нет
  ([`Town.shiftQuote`](../finny-pet/app/src/main/java/ru/finny/pet/domain/town/Town.kt), решения владельца 4 и 47 б,
  GAME_CONCEPT §5.2, §6.1).

## Закрыто прогоном MVP 7.1

### Следствия выбора `game`

| # | Было (2026-09-23) | Стало (сверено с кодом 2026-09-25) |
|---|---|---|
| 1 | Подписанный релиз собран из `classic` (`finny-pet-1.2.0-release.apk`) | MVP-T05: релиз — `assembleGameRelease`; суффикс `.classic` у `classic`, у `game` суффикса нет. `aapt2 dump badging`: `ru.finny.pet`, versionCode 4, 1.3.0, targetSdk 36. Файл сдачи — `finny-pet/release/finny-pet-1.3.0-release.apk` (≈ 3,9 МБ). Подписан debug-ключом — см. «Осталось» п. 2 |
| 2 | Мини-игра может подменить основную механику | MVP-T01, MVP-T03: игра закрыта до плана ([`Economy.miniGameLock`](../finny-pet/app/src/main/java/ru/finny/pet/domain/Economy.kt)), 1 монета за 20 очков, не больше 30 за неделю при карманных 100; заработок игры — строка «Вне плана» в итоге недели. Обоснование — ТЗ 2.8: дополнительные возможности «не компенсируют отсутствие обязательных функций» |
| 3 | Один переключатель «Звук» в разделе для взрослого | MVP-T03: два — «Звуки» и «Музыка» ([`ParentScreen.kt:121–122`](../finny-pet/app/src/game/java/ru/finny/pet/game/screens/ParentScreen.kt)); музыка по умолчанию выключена (`GameState.kt:26`); при выключенных эффектах [`Sfx.play`](../finny-pet/app/src/game/java/ru/finny/pet/game/audio/Sfx.kt) выходит сразу. Что переключатели за барьером для взрослого — вопрос заказчику № 4 (`finny-pet/docs/QUESTIONS.md`) |
| 4 | Холодный старт `game` — 3,8 с | release на эмуляторе ≈ 0,45–0,49 с (`am start -W`, 3 замера; норматив ТЗ 3.4 — 5 с). Замер на телефоне — «Осталось» п. 1 |
| 5 | Скриншоты, DOCX, PPTX, видео — со старого состояния | MVP-T06: материалы `classic` убраны из дерева (есть в истории, коммит 83404be). Для 7.1 — 4 экрана `game` в `finny-pet/screenshots/game/`: `01_create.png`, `02_room.png`, `03_plan.png`, `04_savings.png` (ТЗ 7.1 п. 5). Видео для 7.1 не требуется: есть рабочий APK (ТЗ 7.1 п. 3). DOCX, PPTX, видео — «Осталось» п. 3 |

### Расхождения документации (найдены 2026-09-22)

| # | Где | Что было | Статус 2026-09-25 |
|---|---|---|---|
| 1 | `PLAN.md`, `LIMITATIONS_ROADMAP.md` | «питомец рисуется на Canvas» | `LIMITATIONS_ROADMAP.md` — закрыто; `PLAN.md` — переписывание доков 1.3.0 |
| 2 | `LICENSES.md`, `LIMITATIONS_ROADMAP.md` | «звуков нет» | закрыто: в `LICENSES.md` раздел «Звуки» — 14 файлов Ogg/Opus в `game/res/raw/` (13 эффектов и `music_loop`), генератор `tools/art/sounds.py` |
| 3 | все доки | не сказано, что вариантов два и какой сдаётся | закрыто в `README.md`, `ARCHITECTURE`, `BUILD_AND_DEMO`, `CONTENT_MAP`, `DATA_MODEL`, `ECONOMY`, `LICENSES`, `LIMITATIONS_ROADMAP`, `PRIVACY_PERMISSIONS`, `REQUIREMENTS_MATRIX`, `RUSTORE_CARD`, `TEST_CASES`, `UX_ACCESSIBILITY`; в `QUESTIONS` и `USER_TESTING` назван только `game`, что вариантов два — нет; `GAME_PLAN`, `PLAN` — переписывание доков 1.3.0 |
| 4 | `LICENSES.md` | рамка компетенций с пометкой «базовый уровень» | закрыто: рамка — «каталог формулировок», ссылка на `docs/competencies.md` |
| 5 | `PRIVACY_PERMISSIONS.md` | нет `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | закрыто: описано как `signature`-разрешение, которое добавляет androidx при слиянии манифестов; пользователю не показывается |
| 6 | `README.md`, `BUILD_AND_DEMO.md` | сборка под macOS | закрыто: Windows — `gradlew.bat`, `sdk.dir=C\:/…`, Linux — `./gradlew`. macOS в `BUILD_AND_DEMO.md` остался только у старых demo-скриптов `classic` (в инструкции не используются) и у GPU в Blender |

Найдено при сверке 2026-09-25:

| # | Где | Что не так |
|---|---|---|
| 7 | `REQUIREMENTS_MATRIX.md:14` | «+20/+5 за задания» — в `rules` `rewardCorrect` 10, `rewardWrong` 5. Исправлено |
| 8 | `QUESTIONS.md:24` | «холодный старт `game` — 3,8 с» — release ≈ 0,45–0,49 с. Исправлено |
| 9 | `GAME_PLAN.md:43` | «у дракона крылья больше» — дракона нет. Исправлено; `PLAN.md` и `GAME_PLAN.md` помечены историческими |

Перед коммитом прогона вывод должен быть пуст (из корня репозитория):

```
grep -rnE 'Рисуется на Canvas|примитивами \(Canvas\)|Нет звуков|java_home|Library/Android|\+20/\+5|3,8 с|у дракона' finny-pet/README.md finny-pet/docs
```

## Осталось

| # | Что | ТЗ | Срок | Где / как |
|---|---|---|---|---|
| 1 | Телефон: первый прогон сделан (Samsung S23, 2026-09-25, отчёт в `TEST_CASES.md`); повтор после MVP-T12 — 2026-09-26 (TC-12b, окна, «Назад», альбом); осталось — звук и плавность на слух и на глаз; `game` на «Городке»: экраны и плавность проверены на S23 2026-09-26 (зонд 0, Match3 janky 0,83 %), демо-путь §11 целиком — на эмуляторе | 3.1.3, 3.4 | до финала | эмулятор звук и плавность не показывает |
| 2 | Постоянный ключ подписи | 3.3, 3.4 | к финалу | `keystore.properties` или переменные `FINNY_*` ([`app/build.gradle.kts`](../finny-pet/app/build.gradle.kts)), ключ вне репозитория. 1.3.0 подписан debug-ключом (`CN=Android Debug`): релиз с новым ключом поверх него не встанет — удалить и поставить заново. Кто хранит ключ — вопрос заказчику № 3 |
| 3 | DOCX, PPTX, видео, скриншоты карточки RuStore для `game` | 3.3, 4, 4.1, 7.2 | к финалу | генераторы `finny-pet/tools/office/` под `classic`: `build_docx.js:158` — «Версия 1.2.0», снимки берутся из `screenshots/store/` с именами экранов `classic` |
| 4 | [`tools/demo_run.sh`](../finny-pet/tools/demo_run.sh) под `game` | 4.1 | вместе с п. 3 | сейчас APK по умолчанию — `classic` (стр. 8), запуск — `ru.finny.pet` (теперь это `game`), тапы по надписям `classic`, путь к `platform-tools` — macOS (стр. 7) |
| 5 | 3D-питомец с ручной анимацией | 2.6 | к финалу | `docs/ART_PIPELINE.md`; вопросы сеньору — ниже |
| 6 | Задания-действия: ребёнок решает задачу покупкой, переводом в копилку, правкой плана | 2.5.8 | к финалу | вопрос заказчику № 1. В `game` с TOWN-S1d заданий 1.3.0 нет: их место заняли события «Городка» (`town.events`, движок TOWN-S1c), исход которых определяется поступком — покупкой (где и из какой банки), раскладкой, взносом, «Сделать мечтой», «Пройти мимо» (`Fact` в `domain/town/Dictionary.kt`); в срезе 1 живы 7 событий и 2 заказа, остальные — в `town.eventsOff` до среза 2. В `classic` — по-прежнему задания с выбором и вводом числа |
| 7 | Род в текстах о питомце | — | к финалу | питомец грамматически мужского рода («вырос», «доволен») — ограничение прототипа (MVP-T01a п. 4, MVP-T03 п. 16). Обращение к ребёнку без рода уже сделано (MVP-T01a, T01b, T03, T08) |
| 8 | `StateStore`: запасная ветка копирования | 3.4 | низкий приоритет | [`StateStore.kt:45–48`](../finny-pet/app/src/main/java/ru/finny/pet/data/StateStore.kt): если `renameTo` не сработал, `copyTo` не атомарен — обрыв посреди копии портит `state.json`, и загрузка начнёт с нуля (файл уйдёт в `state.json.bad`). Помечено `ponytail:` (стр. 13–14). Лечение — при битом `state.json` читать уцелевший `state.json.tmp`. На внутренней памяти Android не наблюдалось |
| 9 | Пользовательская проверка | 8.4 | до финала | протокол `finny-pet/docs/USER_TESTING.md`; участники — вопрос заказчику № 8 |
| 10 | Альбомная ориентация `game` | 3.1.2, 3.6 | срез 3 «Городка» | с TOWN-S1d `game` закреплён в портрете ([`src/game/AndroidManifest.xml`](../finny-pet/app/src/game/AndroidManifest.xml), `screenOrientation="portrait"`), вопросов в пузыре питомца нет. Альбомная раскладка с альбомными фонами — срез 3 (GAME_CONCEPT §17.3). Замечания 1.3.0 к компактному альбому — [`MVP-REVIEW-UI.md`](tasks/MVP-REVIEW-UI.md), «Остаётся» |
| 11 | Полный прогон TalkBack | 3.6 | до финала | описания, `paneTitle`, переключатели сделаны в MVP-T12 и проверены uiautomator; живого прогона с TalkBack не было, экраны «Городка» (TOWN-S1d) тоже не проверены. Итог смены в сцене (TOWN-J1-1b1): объявляется ли без касания (`paneTitle` облачка), дублируется ли чтение при фокусе на облачке, объявляет ли TalkBack строку питомца после «Почему?» (её текст уже в описании облачка) — записать |
| 12 | Фон улицы `game` | — | арт среза 1, задача A1g ([TOWN-A1.md](tasks/TOWN-A1.md)) | с TOWN-A1c места (рынок, «У Фомы», пекарня), их раунды и итог смены — на своих фонах (слой места над комнатой, `placeBackground` в `GameApp.kt`); улица — ещё на фоне комнаты; доска, банки и копилка остаются на комнате по решению эпика TOWN-A1 |
| 13 | Карточка заказа «Помочь Марте» на рынке — под полкой | — | срез 1 → A1s (продавец с облачком заказа на полу витрины, решение 39) | после [К работе] с доски открывается лавка, `OrderCard` стоит после полки товаров (`PlaceScreen.kt:164–166`) — карточку ищут прокруткой (живая проверка 2026-09-26) |
| 14 | Тяжёлая смена экранов на слабом устройстве (кадр перехода до ≈ 340 мс на урезанном эмуляторе и без фонов мест), `store.save` вне главного потока | 3.4 | если плейтест или слабый телефон покажут подвисание; загрузчик фонов не делаем (решение владельца 2026-09-27) | S1f ([`TOWN-S1f.md`](tasks/TOWN-S1f.md)): release на урезанном эмуляторе — старт 1,2–2,2 с, кадров > 250 мс нет. Замер A1c с тремя фонами мест ([`TOWN-A1c.md`](tasks/TOWN-A1c.md)): урезанный эмулятор, release, A/B с BASE по 3 прогона, кадр перехода — `perf.sh max` не позже 0,7 с после касания: первый вход на рынок 201–251 мс (BASE без фонов мест 137–187), в «У Фомы» 213–230 (168–184), в пекарню 102–121 (92–112), повторный вход 118–170 (118–136); смена экрана без смены фона — комната → улица 208–236 (BASE 187–285), место → ⌂ 169–308 (BASE 161–336): кадры > 250 мс бывают и без фонов мест, фоны добавляют к первому входу ≈ 15–55 мс; S23 release — вход в место 102–142 мс, действия 89–91 мс, p99 ≤ 11 мс, холодный старт 0,20–0,23 с. Debug стартует 4,3–6 с — без R8 и с отладкой, в сдачу не идёт |
| 15 | Кнопка «Макеты „Городка“» в debug перекрывает нижнюю кнопку бонуса на экране взрослого | — | низкий приоритет | `TownMockHost` (`mock/TownMock.kt:89`) показывается только в debug (`BuildConfig.DEBUG`, `GameApp.kt:222`); в release кнопки нет |

## Технический долг

- **Устаревший `LocalLifecycleOwner` — закрыто в TOWN-S1d** (dc8b193):
  [`GameApp.kt`](../finny-pet/app/src/game/java/ru/finny/pet/game/GameApp.kt) импортирует
  `androidx.lifecycle.compose.LocalLifecycleOwner` вместо `@Deprecated`
  `androidx.compose.ui.platform.LocalLifecycleOwner`; новой зависимости нет.
- **Линт:** 0 ошибок; без сетевых проверок версий 5 (`classic`) / 6 (`game`) предупреждений —
  потолок, а не цель (`docs/ACCEPTANCE_PRESETS.md`). Сейчас 5 / 5 (всего по 8, ещё 3 — сетевые `GradleDependency`), состав на 12a4394 (2026-09-26):

  | Правило | Где | Вариант |
  |---|---|---|
  | `OldTargetApi` | `app/build.gradle.kts:25` (доступен SDK 37) | оба |
  | `DataExtractionRules` | `main/AndroidManifest.xml:7` (`allowBackup`) | оба |
  | `ObsoleteSdkInt` | `main/res/mipmap-anydpi-v26` при minSdk 26 | оба |
  | `MonochromeLauncherIcon` | `main/res/mipmap-anydpi-v26/ic_launcher.xml` | `classic` |
  | `ConfigurationScreenWidthHeight` | `classic/…/ui/App.kt:72` | `classic` |
  | `LockedOrientationActivity`, `DiscouragedApi` | `game/AndroidManifest.xml:9` (`screenOrientation="portrait"`, TOWN-S1d) | `game` |

- **Картинка «Ванны с пеной» — ещё витаминки** (TOWN-S0b, 2026-09-26). `care_vitamins` переименован
  (решение 16), а `game/res/drawable-nodpi/item_care_vitamins.webp` рисует таблетки: новых ассетов в
  срезе 0 нет. Пересобрать пропом ванны в `tools/art/props.py` в блоке арта среза 1 (GAME_CONCEPT
  §17.3), запись в `LICENSES.md`. До этого у ванны в `game` старая картинка — на полке лавки «У Фомы»
  (`itemRes` в `ui/TownUi.kt`).
- Закрыто: число 10 в совете про копилку (`Economy.kt:348` на 83404be) — теперь
  `rules.savingsAmounts.firstOrNull() ?: rules.planStep` (`Economy.kt:395`).
  `docs/EXAMPLE_TASK.md` описывает его как образец на коммите 83404be.

## Открытые вопросы сеньору

См. конец `docs/ART_PIPELINE.md`: подтвердить список анимаций MVP
(`idle`, `happy`, `sad`, `eat`), тариф Meshy, разрешение кадров.
Вопросы заказчику (ТЗ 7.1 п. 7) — `finny-pet/docs/QUESTIONS.md`.

Актуально на ветку `feat/town`, 2fd65f5 (2026-09-27); выпуск — 1.3.0

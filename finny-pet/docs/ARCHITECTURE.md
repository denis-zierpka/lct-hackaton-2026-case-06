# Архитектура

Android-приложение без сервера. Один Gradle-модуль `app`, два варианта сборки (product flavor, измерение `edition`) поверх общего кода `src/main/`:

| Вариант | Пакет | Статус |
|---|---|---|
| `game` | `ru.finny.pet` | **сдаётся**: комната с питомцем, звуки, мини-игра |
| `classic` | `ru.finny.pet.classic` | альтернативный вариант сборки (обычный интерфейс Material 3: [src/classic/](../app/src/classic/)), в сдачу не входит |

Дальше описан только общий код и вариант `game`.

## Раскладка исходников

```
app/src/
├── main/                          общий код обоих вариантов
│   ├── java/ru/finny/pet/
│   │   ├── domain/                правила: Economy, Match3, Content, GameState — чистый Kotlin
│   │   ├── data/                  ContentRepository (assets) · StateStore (filesDir/state.json)
│   │   └── PetSprites.kt          таблица спрайтов питомца (генерируется)
│   ├── assets/content/content.json   учебный контент и числа правил (`Rules`)
│   ├── res/drawable-nodpi/        108 WebP питомца pet_<вид>_<цвет>_<стадия>_<лицо>
│   └── AndroidManifest.xml        без uses-permission
├── game/                          вариант game
│   ├── java/ru/finny/pet/MainActivity.kt
│   ├── java/ru/finny/pet/game/    GameApp, GameViewModel (+ Screen, Effect), audio/Sfx, ui/, screens/
│   └── res/                       drawable-nodpi: комната, товары, цели, плитки, иконки (40 WebP);
│                                  raw: 13 звуков + музыка (OGG)
├── classic/                       вариант classic (не сдаётся)
└── test/java/ru/finny/pet/domain/ JVM-тесты правил
```

## Слои и зависимости

```
 ┌──────────────────────── src/game ────────────────────────┐
 │ screens/*, ui/* ──событие──▶ GameViewModel ──▶ audio/Sfx │
 └───────────────────────────────┬──────────────────────────┘
                                 │ зовёт
             ┌───────────────────┼─────────────────────┐
             ▼                   ▼                     ▼
   main/data                 main/domain           main/PetSprites.kt
   ContentRepository ──────▶ Economy, Match3,      (id спрайта → R.drawable)
   StateStore        ──────▶ Content, GameState
      ▲       ▲
      │       └──▶ filesDir/state.json (чтение и запись)
      └── assets/content/content.json (только чтение)
```

Зависимости направлены вниз: `game → data → domain`, `game → domain`. `domain/` не импортирует `android.*`/`androidx.*` — только stdlib Kotlin и аннотацию `@Serializable`; не читает время и не создаёт случайность сама (`Match3` получает seed снаружи).

## Компоненты

Общий код, [src/main/java/ru/finny/pet/](../app/src/main/java/ru/finny/pet/):

| Компонент | Файл | Ответственность |
|---|---|---|
| Правила экономики | [domain/Economy.kt](../app/src/main/java/ru/finny/pet/domain/Economy.kt) | Профиль, план, покупки, копилка и цели, задания, вопросы питомца, итог мини-игры, конец недели, рост и выражение питомца, склонение «монет» (`Economy.coins`). Действия — функции `(GameState, …) → Outcome`, кроме `newGame`, `resetProfile`, `deleteProfile`: они сразу возвращают новое `GameState`; запросы (`stageIndex`, `goalEta`, `availableTasks`, `canEndPeriod` …) ничего не меняют. Формулы — [ECONOMY.md](ECONOMY.md) |
| Мини-игра | [domain/Match3.kt](../app/src/main/java/ru/finny/pet/domain/Match3.kt) | «Три в ряд» 7×6: обмен, бомба 3×3, каскады с множителем. Ход возвращает `Turn` — новое `Match3State` и список шагов анимации `Step` |
| Модель контента | [domain/Content.kt](../app/src/main/java/ru/finny/pet/domain/Content.kt) | Схема `content.json`: `Rules` (числа экономики), виды и цвета питомца, товары, цели, задания, глоссарий, вопросы питомца, реплики |
| Модель профиля | [domain/GameState.kt](../app/src/main/java/ru/finny/pet/domain/GameState.kt) | `GameState` и вложенные `Pet`, `BudgetPlan`, `Purchase`, `Goal`, `LedgerEntry`, `TaskResult`, `PeriodSummary` — неизменяемые `@Serializable data class`. `Outcome` (`Ok`/`Error`) — `sealed interface` результата действия, в `state.json` не пишется. Поля — [DATA_MODEL.md](DATA_MODEL.md) |
| Загрузка контента | [data/ContentRepository.kt](../app/src/main/java/ru/finny/pet/data/ContentRepository.kt) | `load(context)` читает `assets/content/content.json`; `parse(text)` не трогает Android — через неё контент грузят тесты |
| Хранение профиля | [data/StateStore.kt](../app/src/main/java/ru/finny/pet/data/StateStore.kt) | `load()` / `save()` файла `filesDir/state.json`, см. «Хранение» |
| Спрайты питомца | [PetSprites.kt](../app/src/main/java/ru/finny/pet/PetSprites.kt) | `PetSprites.id(вид, цвет, стадия, лицо)` → `R.drawable`; при неизвестном id — оранжевый кот-малыш. Генерируется `tools/art/import_sprites.py`, вручную не правится |

Вариант game, [src/game/java/ru/finny/pet/](../app/src/game/java/ru/finny/pet/):

| Компонент | Файл | Ответственность |
|---|---|---|
| Точка входа | [MainActivity.kt](../app/src/game/java/ru/finny/pet/MainActivity.kt) | Edge-to-edge, `setContent { GameApp() }` |
| Оболочка | [game/GameApp.kt](../app/src/game/java/ru/finny/pet/game/GameApp.kt) | Тема, фон комнаты (день/вечер), смена экранов `AnimatedContent`, корневой `BackHandler`, окно обратной связи `FeedbackOverlay`, слой частиц; сбор эффектов VM; `CompositionLocal`: `LocalVm`, `LocalLayout`, `LocalSfx`, `LocalParticles`, `LocalPetAction`, `LocalAnimate` |
| Состояние UI | [game/GameViewModel.kt](../app/src/game/java/ru/finny/pet/game/GameViewModel.kt) | `AndroidViewModel`: `state`, `screen`, `feedback`, `bubble` (реплика или вопрос питомца), `match` (текущая партия), `matchOver` (показана панель «Игра окончена» — во ViewModel, чтобы пережить пересоздание Activity и не попасть в следующую партию после смерти процесса). При загрузке неизвестные `speciesId`/`colorId` питомца заменяются первыми из контента. Типы `Screen`, `Effect`, `Feedback`, `Bubble`, `PetAct`. Правил не содержит — только вызывает `Economy`/`Match3` и сохраняет |
| Звук | [game/audio/Sfx.kt](../app/src/game/java/ru/finny/pet/game/audio/Sfx.kt) | 13 эффектов `Sound` на `SoundPool` и музыкальная петля на `MediaPlayer`; `effects` = тумблер «Звуки», `music` = тумблер «Музыка» (на заставке музыка не играет); `GameApp` ставит музыку на паузу, когда приложение свёрнуто |
| Общие виджеты | [game/ui/](../app/src/game/java/ru/finny/pet/game/ui/) | `GameTheme.kt` (палитра `G`, шрифт Montserrat, тема M3), `Widgets.kt` (кнопки, панели, HUD, полосы, пузырь речи), `GameTextField.kt`, `PetSprite.kt` (спрайт с дыханием, морганием, прыжком и действиями), `Particles.kt` (одна система частиц поверх всего экрана) |
| Экраны | [game/screens/](../app/src/game/java/ru/finny/pet/game/screens/) | См. таблицу ниже; `Common.kt` — каркас панельного экрана, адаптивная раскладка, панель подтверждения |

## Функциональная архитектура

Экраны читают `vm.state`, `vm.content` и функции-запросы `vm.economy`; менять состояние могут только через методы `GameViewModel`.

| Функция | `Screen` | Файл экрана | Методы VM | Правило |
|---|---|---|---|---|
| Заставка, знакомство, создание питомца | `Title`, `Intro`, `CreatePet` | `StartScreens.kt` | `start`, `createPet` | `Economy.createPet` |
| Комната: питомец, подсказка следующего шага, реплики и вопросы питомца, конец недели | `Room` | `RoomScreen.kt` | `petTapped`, `askQuestion`, `answerBubble`, `nextStep`, `endPeriod` | `answerQuiz`, `endPeriod` |
| План недели | `Plan` | `PlanScreen.kt` | `setPlan`, `confirmPlan` | `setPlan`, `confirmPlan` |
| Магазин | `Shop` | `ShopScreen.kt` | `buy` | `buy` |
| Копилка и цель | `Savings` | `SavingsScreen.kt` | `deposit`, `withdraw`, `chooseGoal`, `achieveGoal` | те же |
| Задания | `Tasks`, `Task(id)` | `TaskScreens.kt` | `answerChoice`, `answerNumber` | те же |
| Мини-игра | `MiniGame` | `MiniGameScreen.kt` | `startMiniGame`, `matchSwap`, `matchBomb`, `finishMiniGame` | `Match3.*`, `Economy.finishMiniGame` |
| Итог недели, прогресс, справочник | `WeekEnd`, `Progress`, `Glossary` | `ProgressScreens.kt` | — (только чтение) | `stageTitle`, `stageIndex`, `nextStageLeft`, `completedTasks` |
| Раздел для взрослого | `Parent` | `ParentScreen.kt` | `setDemo`, `setAnimations`, `setSounds`, `setMusic`, `parentBonus`, `createTestProfile`, `resetProfile`, `deleteProfile` | `parentBonus`, `newGame`, `resetProfile`, `deleteProfile` |

## Поток данных

```
экран ──событие──▶ GameViewModel.buy(id) ──▶ Economy.buy(state, id) ──▶ Outcome
                                                                         │
      ┌──────────────────── Outcome.Ok(newState, messages) ◀─────────────┤
      │  commit: state = newState; StateStore.save(newState)             │
      │  feedback = «что изменилось и почему» (или переход на next)      │
      │  effects.tryEmit(Sfx / Coins / Confetti / PetAction …)           │
      │                                                                  │
      │                     Outcome.Error(message, hints) ◀──────────────┘
      │                     состояние не меняется; звук FAIL;
      │                     feedback «Пока не получится» + «Вариант: …»
      ▼
GameApp: vm.effects.collect → Sfx.play · ParticleController · LocalPetAction → PetSprite
Compose перерисовывает экраны по state / screen / feedback / bubble
```

1. Экран вызывает метод VM (например `buy(itemId)`).
2. VM передаёт текущее `GameState` в `Economy` и получает `Outcome`.
3. `Ok` → `commit`: новое состояние заменяет старое целиком и сразу пишется `StateStore.save`. Если запись не удалась, `commit` ставит окно «Не удалось сохранить», но `apply` тут же заменяет его сообщениями `Ok`, если они есть. Поэтому ошибка записи видна только после действий без сообщений: `setPlan`, ответ на вопрос питомца, конец недели, тумблеры и действия с профилем в разделе для взрослого.
4. Сообщения `Ok` показываются в `FeedbackOverlay`; у `Feedback` может быть `next` — экран, куда перейти после «Понятно».
5. Одноразовые эффекты идут отдельно от состояния: `MutableSharedFlow<Effect>(extraBufferCapacity = 32)`. VM не касается Android-представлений; `GameApp` проигрывает звук, запускает частицы между именованными точками экрана (`coins`, `piggy`, `pet`, `center`) и передаёт действие питомцу (`EAT`, `WASH`, `PLAY`, `HOP`, `SLEEP`). Монеты плана `PlanScreen` пускает сам, без `Effect`: из кошелька `purse` в банки `jar0`–`jar2`.

Отступления от общей схемы: тумблеры раздела для взрослого сохраняют `state.copy(...)` без `Economy`, а тестовый профиль, сброс и удаление — готовое `GameState` от `Economy`; `answerBubble` и `endPeriod` обрабатывают `Outcome` сами, но сохраняют тем же `commit`.

## Навигация

`sealed interface Screen` (14 экранов) в `GameViewModel.kt`; смена экрана — присваивание `vm.screen` через `navigate()`, анимированная `AnimatedContent` в `GameApp`. Библиотека navigation-compose не используется.

Системная «Назад» (`BackHandler`) работает на всех экранах, кроме `Title` и `Room` — там она закрывает приложение, если не открыто окно. Окна регистрируют свой `BackHandler`, и он срабатывает первым, потому что добавлен позже корневого: `ConfirmPanel` ([Common.kt](../app/src/game/java/ru/finny/pet/game/screens/Common.kt)) закрывается как «Отмена», окно обратной связи — как «Понятно» (его `BackHandler` компонуется только пока окно открыто, поэтому перекрывает и `BackHandler` мини-игры), вопрос питомца в комнате и в мини-игре — закрывается без ответа. Затемнение окон поглощает касания (`pointerInput`), для TalkBack окно — панель с `paneTitle`. `vm.back()` ведёт на известного родителя: `Task` → `Tasks`, `Glossary` → `Progress`, `CreatePet` → `Title`, остальное (в том числе `Intro`) → `Room`, а без профиля → `Title`. На `MiniGame` `vm.back()` ничего не делает: «Назад» перехватывает `BackHandler` самого экрана — первое нажатие открывает окно «игра окончена», второе вызывает `finishMiniGame`.

## Мини-игра

- Правила — [domain/Match3.kt](../app/src/main/java/ru/finny/pet/domain/Match3.kt), детерминированы: вся случайность из `Random(seed)`, следующий seed хранится в `Match3State.seed`. Одинаковый seed — одинаковое поле (`Match3Test`).
- Seed передаёт VM: `startMiniGame()` вызывает `Match3.newGame(moves = rules.miniGameMoves, bombs = state.bombs, seed = System.nanoTime())`.
- `matchSwap` / `matchBomb` возвращают `Turn`; `MiniGameScreen` проигрывает шаги `Swap → Match → Fall → Refill` по порядку.
- Партия (`vm.match`) живёт только в памяти VM и в `state.json` не пишется. В профиль попадает только итог: `finishMiniGame` → `Economy.finishMiniGame(state, score, bombsUsed)` → монеты с недельным лимитом, списание бомб, запись в журнал.

## Анимации и доступность

`LocalAnimate` = тумблер «Анимации» (`state.animations`) **и** системный масштаб анимаций > 0 (`ANIMATOR_DURATION_SCALE`, перечитывается при возврате в приложение; `systemAnimates` в [GameApp.kt](../app/src/game/java/ru/finny/pet/game/GameApp.kt)). Сам тумблер показывает `state.animations`, а при системном запрете неактивен и подписан «Выключены в настройках Android». При `LocalAnimate = false`:

- смена экранов и всплывающие окна — без перехода (`EnterTransition.None`, расширения `orNone()`);
- `ParticleController.enabled = false` — новые частицы не создаются;
- `PetSprite` не дышит и не прыгает, текст реплик появляется сразу;
- мини-игра ставит плитки мгновенно (`snap()` вместо пружин).

`LocalLayout` сообщает ориентацию и «компактность» (узкая сторона < 420 dp). Экраны с фиксированной геометрией (`Room`, `MiniGame`) ограничивают системный масштаб шрифта 1,3; панельные экраны следуют ему полностью. Эффекты отключаются тумблером «Звуки», фоновая музыка — тумблером «Музыка»; звук не несёт уникальной информации. Подробно — [UX_ACCESSIBILITY.md](UX_ACCESSIBILITY.md).

## Хранение

`filesDir/state.json` — один JSON-документ `GameState` (kotlinx.serialization, `ignoreUnknownKeys`, `encodeDefaults`): у новых полей есть значения по умолчанию, поэтому старые профили читаются.

- Запись: `state.json.tmp` → переименование в `state.json`; это атомарно, при сбое прежний файл цел. Если переименование не удалось, `tmp` копируется поверх `state.json` — эта ветка не атомарна, сбой посреди копирования может оставить файл неполным. При любой ошибке `save` возвращает `false`.
- Нет файла → пустое состояние (`GameState()`). Не читается (`IOException`) → пустое состояние, файл не трогается. Повреждён (`SerializationException`, `IllegalArgumentException`) → переименовывается в `state.json.bad`, а не затирается.
- Сетевых вызовов, разрешений и аккаунтов нет; `android:allowBackup="false"`.

## Схема обновления учебного контента

Задания, товары, цели, глоссарий, вопросы и реплики питомца, виды и цвета питомца, числовые правила — в [app/src/main/assets/content/content.json](../app/src/main/assets/content/content.json). Контент читается при запуске, код экранов от конкретных записей не зависит.

Добавить задание = добавить объект в массив `tasks`:

```json
{
  "id": "savings_new", "theme": "SAVINGS", "title": "Новое задание", "type": "CHOICE", "unlockPeriod": 2,
  "situation": "Описание ситуации...",
  "options": [
    { "text": "Вариант А", "correct": true,  "explanation": "Почему верно" },
    { "text": "Вариант Б", "correct": false, "explanation": "Почему нет" }
  ]
}
```

- Типы: `CHOICE` (варианты с объяснением каждого) и `NUMBER` (поля `answer`, `explanationCorrect`, `explanationWrong`). Темы: `BUDGET`, `SAVINGS`, `SHOPPING`.
- `ContentTest` проверяет реальный `content.json`: у задания `CHOICE` ≥ 2 варианта, ровно один верный, объяснения непустые; у `NUMBER` есть ответ и оба объяснения; id заданий и товаров уникальны; `unlockPeriod` в 1..5; соблюдён минимальный объём из ТЗ (≥ 9 сочетаний питомца, ≥ 6 заданий по 3 темам, ≥ 8 товаров обоих типов, ≥ 3 цели и 3 стадии).
- Новый товар, цель, вид или цвет питомца работают без правки кода, но с запасной картинкой: `item_fun_tent`, `goal_custom`, оранжевый кот. Своя картинка товара или цели — WebP в `app/src/game/res/drawable-nodpi/` и строка в `itemRes`/`goalRes` ([RoomScreen.kt](../app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt)); питомца — рендер `pet.py` и `import_sprites.py`.
- Правки кода требуют только новая тема (`enum Theme` + иконка и цвет в `TaskScreens.kt`) и новый тип задания.

## Генераторы ассетов

Все картинки и звуки игры собираются скриптами из [tools/art/](../tools/art/); лицензии — [LICENSES.md](LICENSES.md).

| Скрипт | Что делает | Куда попадает |
|---|---|---|
| `pet.py` → `import_sprites.py` | Blender рендерит питомца (3 вида × 3 цвета × 3 стадии × 4 лица = 108 PNG); `import_sprites.py` конвертирует в WebP и перегенерирует `PetSprites.kt` | `app/src/main/res/drawable-nodpi/pet_*.webp`, `app/src/main/java/ru/finny/pet/PetSprites.kt` |
| `room.py` | Комната: альбом/портрет × день/вечер (PNG) | `app/src/game/res/drawable-nodpi/room_*` |
| `props.py` | Товары, цели, плитки мини-игры, монета (PNG) | там же: `item_*`, `goal_*`, `tile_*`, `ui_coin` |
| `uiprops.py` | Иконки интерфейса: банка и крышки бюджета, копилка, кошелёк и др. (PNG) | там же: `ui_*` |
| `sounds.py` | Синтез эффектов и музыки на stdlib Python, кодирование ffmpeg в OGG | `app/src/game/res/raw/*.ogg` |

`lib.py` — общие хелперы Blender (материалы, свет, камера), `smoke.py` — проверка `lib.py`. `room.py`, `props.py`, `uiprops.py` выдают PNG; скрипта их конвертации в WebP в репозитории нет.

## Тесты

JVM-тесты (JUnit 4) в [app/src/test/java/ru/finny/pet/domain/](../app/src/test/java/ru/finny/pet/domain/) проверяют только `domain`, без Android и эмулятора, на реальном `content.json` (`TestContent` читает его через `ContentRepository.parse`).

| Класс | Что проверяет |
|---|---|
| `EconomyTest` | создание питомца, план, покупки, копилка и цели, задания, конец недели и рост (5 демо-недель подряд), сброс и удаление профиля, лимит мини-игры, вопросы питомца, склонение «монет» |
| `MvpRulesTest` | правила промежуточной сдачи: числа `Rules` совпадают с `content.json`, старые сохранения читаются, выражение питомца, тексты итога недели и копилки, мини-игра открыта только после плана, бонус взрослого, склонение «монет» и тексты без рода ребёнка, поле мини-игры по seed |
| `Match3Test` | поле без готовых совпадений, отказ хода без совпадения, очки и ходы, бомба, конец партии, детерминизм по seed |
| `ContentTest` | корректность и минимальный объём контента (см. выше) |

Запуск из `finny-pet/`: `gradlew.bat :app:testGameDebugUnitTest` (Linux: `./gradlew`). Сценарии, которые проверяются вручную, — [TEST_CASES.md](TEST_CASES.md).

## Сборка

| | |
|---|---|
| Инструменты | Gradle 9.4.1 (wrapper), AGP 9.2.1 со встроенной поддержкой Kotlin, Kotlin 2.4.20 (плагины compose и serialization), JDK 17 |
| Android | minSdk 26 (Android 8.0), targetSdk и compileSdk 36; `versionCode 4`, `versionName 1.3.0` |
| Библиотеки | Compose BOM 2026.06.01 (ui, material3, material3-adaptive-navigation-suite — использует только classic, material-icons-extended, ui-tooling-preview), activity-compose 1.12.4, lifecycle-viewmodel-compose 2.10.0, kotlinx-serialization-json 1.11.0; тесты — junit 4.13.2 |
| Release | R8 (`isMinifyEnabled`, `isShrinkResources`), правила для сериализаторов — [app/proguard-rules.pro](../app/proguard-rules.pro). Подпись — `keystore.properties` или переменные `FINNY_*`; без них release подписывается debug-ключом с предупреждением |
| Сдаваемый APK | `gradlew.bat :app:assembleGameRelease` → `app/build/outputs/apk/game/release/` |

Пошаговая инструкция — [BUILD_AND_DEMO.md](BUILD_AND_DEMO.md).

Актуально на версию 1.3.0 (2026-09-25)

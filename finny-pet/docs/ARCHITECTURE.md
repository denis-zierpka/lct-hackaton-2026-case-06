# Архитектура

Android-приложение без сервера. Один Gradle-модуль `app`, два варианта сборки (product flavor, измерение `edition`) поверх общего кода `src/main/`:

| Вариант | Пакет | Статус |
|---|---|---|
| `game` | `ru.finny.pet` | **сдаётся**: комната с питомцем, звуки, мини-игра |
| `classic` | `ru.finny.pet.classic` | альтернативный вариант сборки (обычный интерфейс Material 3: [src/classic/](../app/src/classic/)), в сдачу не входит |

Дальше описан только общий код и вариант `game` ветки `feat/town`: с TOWN-S1d `game` работает на движке «Городка» (`domain/town`), `classic` — на `Economy` по правилам 1.3.0. Экраны `game` выпуска 1.3.0 (план, магазин, задания) и `FeedbackOverlay` — на теге `v1.3.0`.

## Раскладка исходников

```
app/src/
├── main/                          общий код обоих вариантов
│   ├── java/ru/finny/pet/
│   │   ├── domain/                правила: Economy, Match3, Content, GameState — чистый Kotlin
│   │   │   └── town/              «Городок»: Town, Prices, TownEvents, Migration, PetTalk — правила game
│   │   ├── data/                  ContentRepository (assets) · StateStore (filesDir/state.json)
│   │   └── PetSprites.kt          таблица спрайтов питомца (генерируется)
│   ├── assets/content/content.json   учебный контент и числа правил (`Rules`)
│   ├── res/drawable-nodpi/        108 WebP питомца pet_<вид>_<цвет>_<стадия>_<лицо>
│   └── AndroidManifest.xml        без uses-permission
├── game/                          вариант game
│   ├── java/ru/finny/pet/MainActivity.kt
│   ├── java/ru/finny/pet/game/    GameApp, GameViewModel (+ Screen, Effect), audio/Sfx, ui/, screens/, mock/ (debug-макеты «Городка»)
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
   StateStore        ──────▶ Content, GameState, town/
      ▲       ▲
      │       └──▶ filesDir/state.json (чтение и запись)
      └── assets/content/content.json (только чтение)
```

Зависимости направлены вниз: `game → data → domain`, `game → domain`. `domain/` не импортирует `android.*`/`androidx.*` — только stdlib Kotlin и kotlinx.serialization (`@Serializable`, `@SerialName` и строковые сериализаторы словаря событий в `town/TownCodec.kt`); не читает время и не создаёт случайность сама (`Match3` получает seed снаружи).

## Компоненты

Общий код, [src/main/java/ru/finny/pet/](../app/src/main/java/ru/finny/pet/):

| Компонент | Файл | Ответственность |
|---|---|---|
| Правила экономики | [domain/Economy.kt](../app/src/main/java/ru/finny/pet/domain/Economy.kt) | Профиль, план, покупки, копилка и цели, задания, вопросы питомца, итог мини-игры, конец недели, рост и выражение питомца, склонение «монет» (`Economy.coins`). Действия — функции `(GameState, …) → Outcome`, кроме `newGame`, `resetProfile`, `deleteProfile`: они сразу возвращают новое `GameState`; запросы (`stageIndex`, `goalEta`, `availableTasks`, `canEndPeriod` …) ничего не меняют. Формулы — [ECONOMY.md](ECONOMY.md) |
| Правила «Городка» | [domain/town/](../app/src/main/java/ru/finny/pet/domain/town/) | `Town` — все денежные и временные действия `game`: раскладка по банкам, касса с вариантами оплаты (`quote`, `buyAt`), перенос между банками, копилка и мечта, места для вещей, дни, сон, конец недели и «что поменяем», смены (`shiftQuote`, `finishShift`), «Загадка Бори», бонус взрослого в конверт. Результат — `TownResult`: `Done(TownOutcome)` с состоянием, строкой `line` и `why` или `Refused(line)`. `Prices` — цена в лавке, `TownEvents` — приход и исход событий, `Migration` — профиль 1.3.0 → «Городок», `PetTalk` — реплика питомца по тапу. Поверх `Economy`: зовёт его `buy` (с ценой лавки — параметр `price`, TOWN-S1a), `confirmPlan`, `deposit`, `withdraw`, `endPeriod`, `chooseGoal` (для «Сделать мечтой»), `achieveGoal`, `goalEta`. Формулы — [ECONOMY.md](ECONOMY.md), раздел «Городок» |
| Мини-игра | [domain/Match3.kt](../app/src/main/java/ru/finny/pet/domain/Match3.kt) | «Три в ряд» `w × h` (по умолчанию 7×6; смена в пекарне `game` — 6×6 из `town.jobs[].board`): обмен, бомба 3×3, каскады с множителем. Ход возвращает `Turn` — новое `Match3State` и список шагов анимации `Step` |
| Модель контента | [domain/Content.kt](../app/src/main/java/ru/finny/pet/domain/Content.kt) | Схема `content.json`: `Rules` (числа экономики), виды и цвета питомца, товары, цели, задания, глоссарий, вопросы питомца, реплики; ключ `town` — `TownContent` (места, лавки, товары, мечты, работы, жители, события, реплики `chatter`, загадки, свои `rules`) |
| Модель профиля | [domain/GameState.kt](../app/src/main/java/ru/finny/pet/domain/GameState.kt) | `GameState` и вложенные `Pet`, `BudgetPlan`, `Purchase`, `Goal`, `LedgerEntry`, `TaskResult`, `PeriodSummary` — неизменяемые `@Serializable data class`. `Outcome` (`Ok`/`Error`) — `sealed interface` результата действия, в `state.json` не пишется. Поля — [DATA_MODEL.md](DATA_MODEL.md) |
| Загрузка контента | [data/ContentRepository.kt](../app/src/main/java/ru/finny/pet/data/ContentRepository.kt) | `load(context)` читает `assets/content/content.json`; `parse(text)` не трогает Android — через неё контент грузят тесты |
| Хранение профиля | [data/StateStore.kt](../app/src/main/java/ru/finny/pet/data/StateStore.kt) | `load()` / `save()` файла `filesDir/state.json`, см. «Хранение» |
| Спрайты питомца | [PetSprites.kt](../app/src/main/java/ru/finny/pet/PetSprites.kt) | `PetSprites.id(вид, цвет, стадия, лицо)` → `R.drawable`; при неизвестном id — оранжевый кот-малыш. Генерируется `tools/art/import_sprites.py`, вручную не правится |

Вариант game, [src/game/java/ru/finny/pet/](../app/src/game/java/ru/finny/pet/):

| Компонент | Файл | Ответственность |
|---|---|---|
| Точка входа | [MainActivity.kt](../app/src/game/java/ru/finny/pet/MainActivity.kt) | Edge-to-edge, `setContent { GameApp() }` |
| Оболочка | [game/GameApp.kt](../app/src/game/java/ru/finny/pet/game/GameApp.kt) | Тема, фон комнаты (день/вечер), смена экранов `AnimatedContent`, корневой `BackHandler`, строка `LineHost` ([ui/TownUi.kt](../app/src/game/java/ru/finny/pet/game/ui/TownUi.kt)) поверх экранов, слой частиц; сбор эффектов VM; `CompositionLocal`: `LocalLayout`, `LocalParticles`, `LocalPetAction`, `LocalAnimate`, `LocalClipped` (зонд обрезки текста; узел `overflow` — только в debug); в debug поверх экранов ещё `TownMockHost` — макеты «Городка» с входом на экране `Parent` |
| Состояние UI | [game/GameViewModel.kt](../app/src/game/java/ru/finny/pet/game/GameViewModel.kt) | `AndroidViewModel`: `state`; стек экранов (`screen` — верхний); очередь строк `lines` для `LineHost`; `petLine` — реплика по тапу; `night` и `weekEnd` — отчёт ночи и итога недели; `fresh` — «новое» на доске событий; раунд работы — `match`, `matchOver` (во ViewModel, чтобы пережить пересоздание Activity), `taps`, `roundResult`. При загрузке неизвестные `speciesId`/`colorId` питомца заменяются первыми из контента, затем `Migration.migrate` и `Town.tick`. Типы `Screen`, `Effect`, `Line`, `Report`, `PetAct`. Правил не содержит — вызывает `Town` (деньги и время), `Economy` (только `createPet`, `setPlan`, `chooseGoal`, профиль и запросы) и `Match3`, сохраняет |
| Звук | [game/audio/Sfx.kt](../app/src/game/java/ru/finny/pet/game/audio/Sfx.kt) | 13 эффектов `Sound` на `SoundPool` и музыкальная петля на `MediaPlayer`; `effects` = тумблер «Звуки», `music` = тумблер «Музыка» (на заставке музыка не играет); `GameApp` ставит музыку на паузу, когда приложение свёрнуто |
| Общие виджеты | [game/ui/](../app/src/game/java/ru/finny/pet/game/ui/) | `GameTheme.kt` (палитра `G`, шрифт Montserrat, тема M3), `Widgets.kt` (кнопки, панели, HUD, полосы, пузырь речи), `GameTextField.kt`, `PetSprite.kt` (спрайт с дыханием, морганием, прыжком и действиями), `Particles.kt` (одна система частиц поверх всего экрана), `TownUi.kt` (текст `TText` с зондом обрезки, картинки `itemRes`/`goalRes`, HUD `Hud1`/`Hud2`, строка `LineHost`) |
| Экраны | [game/screens/](../app/src/game/java/ru/finny/pet/game/screens/) | См. таблицу ниже; `Common.kt` — каркас панельного экрана, адаптивная раскладка, панель подтверждения |

## Функциональная архитектура

Экраны читают `vm.state`, `vm.content`, `vm.tc` (ключ `town`) и функции-запросы `vm.economy` и `vm.town`; менять состояние могут только через методы `GameViewModel`.

| Функция | `Screen` | Файл экрана | Методы VM | Правило |
|---|---|---|---|---|
| Заставка, знакомство, создание питомца | `Title`, `Intro`, `CreatePet` | `StartScreens.kt` | `start`, `createPet` | `Economy.createPet`, `Migration.migrate`, `Town.tick` |
| Комната: HUD, «В городке», полка-банки, холодильник, ящик, кровать, реплика питомца по тапу | `Room` | `RoomScreen.kt` | `petTapped`, `sleep`, `endWeek`, `openPlace`, `goEvent` | `Town.petLine`, `card`, `sleep`, `endWeek`, `weekEndPreview` |
| Банки: раскладка, план и факт, перенос; «Обустроить» | `Jars`, `Arrange` | `JarsScreen.kt` | `setPlan`, `confirmPlan`, `transfer`, `place` | `Economy.setPlan`; `Town.confirmPlan`, `transfer`, `place` |
| Копилка и мечта | `Savings` | `SavingsScreen.kt` | `deposit`, `withdraw`, `chooseGoal`, `achieveGoal` | `Town.deposit`, `withdraw`, `achieveGoal`; `Economy.chooseGoal` |
| Ночь, итог недели и «что поменяем» | `Night`, `WeekEnd` | `NightScreens.kt` | `wake`, `endWeek`, `chooseTweak` | `Town.wake`, `endWeek`, `planTweaks`, `chooseTweak` |
| Улица | `Street` | `StreetScreen.kt` | `openPlace`, `chooseGoal` | `Town.visit`; `Economy.chooseGoal` |
| Лавка с кассой; работа: заказ, «Загадка Бори» | `Place(placeId)` | `PlaceScreen.kt` | `openPlace`, `buy`, `makeGoal`, `pass`, `startRound`, `answerRiddle` | `Town.visit`, `quote`, `buyAt`, `makeGoal`, `pass`, `shiftQuote`, `nextQuestion`, `answerQuestion` |
| Смена: «три в ряд» или поручения, итог смены | `Round(jobId)` | `RoundScreen.kt`, `MiniGameScreen.kt` | `matchSwap`, `matchBomb`, `tapTask`, `finishRound`, `closeRound` | `Match3.*`, `Town.finishShift` |
| Доска событий | `Board` | `BoardScreen.kt` | `goEvent`, `startEvent`, `openPlace` | `Town.activeEvents`, `orders`, `demoBoard`, `startEvent` |
| Прогресс, справочник | `Progress`, `Glossary` | `ProgressScreens.kt` | — (только чтение) | `Economy.stageIndex`, `nextStageLeft`; `state`: `history`, `records`, `envelope`, `ledger` |
| Раздел для взрослого | `Parent` | `ParentScreen.kt` | `setDemo`, `setAnimations`, `setSounds`, `setMusic`, `parentBonus`, `createTestProfile`, `resetProfile`, `deleteProfile` | `Town.parentBonus`; `Economy.newGame`, `resetProfile`, `deleteProfile` |

## Поток данных

```
экран ──событие──▶ GameViewModel.buy(item, shop, source) ──▶ Town.buyAt(state, …) ──▶ TownResult
                                                                                      │
      ┌──── Done(TownOutcome: state, line, why, eventResults, arrived) ◀──────────────┤
      │  commit: state = outcome.state; StateStore.save(state)                        │
      │  lines += line (+ why), строки исходов событий, intro пришедшего события      │
      │  effects.tryEmit(Sfx / CoinsFrom / Confetti / Hearts / PetAction)             │
      │                                                                               │
      │                     Refused(line) ◀───────────────────────────────────────────┘
      │                     состояние не меняется; lines += line; звук FAIL
      ▼
GameApp: vm.effects.collect → Sfx.play · ParticleController · LocalPetAction → PetSprite
Compose перерисовывает экраны по state / screen / lines / petLine / night / weekEnd
```

1. Экран вызывает метод VM (например `buy(itemId, shopId, source)`).
2. VM передаёт текущее `GameState` в `Town` и получает `TownResult`.
3. `Done` → `commit`: новое состояние заменяет старое целиком и сразу пишется `StateStore.save`. Если запись не удалась, в очередь строк встаёт «Не удалось сохранить» с причиной «Проверь, есть ли свободное место на устройстве».
4. Строка живёт до следующего шага ребёнка: каждое действие через `Town` и каждый переход (`navigate`, `back`, `openPlace`) сначала очищают `lines`; вызовы через `Economy` (`setPlan`, `chooseGoal`, ошибка `createPet`) и тумблеры взрослого очередь не очищают — их строка встаёт за уже показанной. Затем в очередь встают строка действия, строки исходов событий, которые действие закрыло, и вступление первого пришедшего события (при входе на место — только если карточки события там не видно); одинаковый текст дважды не встаёт. `LineHost` показывает первую: портрет питомца, строка целиком (без `maxLines`; выше 40 % экрана — прокрутка внутри), «Почему?» раскрывает все строки `why`; тап закрывает её. В комнате пузырь стоит над нижним рядом (отступ 72 dp); пока открыта касса (`cashOpen`), пузыря нет, а открытие кассы очищает строки. Сон, конец недели и смена кладут свой отчёт не в очередь, а в `night`, `weekEnd` и `roundResult` — его показывают экраны `Night`, `WeekEnd` и `Round`.
5. Одноразовые эффекты идут отдельно от состояния: `MutableSharedFlow<Effect>(extraBufferCapacity = 32)`. VM не касается Android-представлений; `GameApp` проигрывает звук, пускает монеты между именованными точками экрана (`CoinsFrom`: `coins`, `piggy`, `pet`, `mail`, `jar_save`, `job_result`) — только если обе точки есть на экране, конфетти (`center`) и сердечки (`pet`), и передаёт действие питомцу (`EAT`, `WASH`, `PLAY`, `HOP`, `SLEEP`).

Отступления от общей схемы: тумблеры раздела для взрослого сохраняют `state.copy(...)` без движка; тестовый профиль, сброс и удаление берут готовое `GameState` от `Economy`, затем `Migration.migrate` и `Town.tick`; `createPet`, `setPlan` и `chooseGoal` идут через `Economy` и его `Outcome` (ошибка — строка в `lines` и звук FAIL); все сохраняют тем же `commit`.

## Навигация

`sealed interface Screen` (16 экранов) в `GameViewModel.kt`; навигация — стек `stack` во ViewModel: `navigate()` кладёт экран наверх (переход из одного `Place` в другой заменяет верхний `Place`), `Room`, `Night` и `Title` очищают стек, `back()` снимает верхний экран. Пока питомец спит (`state.asleep`), вместо `Room` открывается `Night`. ⌂ (`home()`) очищает стек до `Room`, в раунде — «Закончить». Смена экрана анимирована `AnimatedContent` в `GameApp`. Библиотека navigation-compose не используется.

Системная «Назад» (`BackHandler` в `GameApp`) включена, пока в стеке больше одного экрана или идёт раунд; на дне стека (`Room`, `Night`, `Title`) она закрывает приложение без диалога. В раунде «Назад» = «Закончить» (`finishRound` → итог смены), на итоге — `closeRound` возвращает к месту работы. Окна (`Ask`, `ConfirmPanel` в [Common.kt](../app/src/game/java/ru/finny/pet/game/screens/Common.kt)) регистрируют свой `BackHandler` — он срабатывает первым, потому что добавлен позже корневого, и закрывает окно как «Отмена». Затемнение окон поглощает касания (`pointerInput`), для TalkBack окно — панель с `paneTitle`.

## Мини-игра

- Правила — [domain/Match3.kt](../app/src/main/java/ru/finny/pet/domain/Match3.kt), детерминированы: вся случайность из `Random(seed)`, следующий seed хранится в `Match3State.seed`. Одинаковый seed — одинаковое поле (`Match3Test`).
- Seed передаёт VM: `startRound(jobId)` для работы `MATCH3` вызывает `Match3.newGame(board.w, board.h, moves, state.bombs + levelBombs, seed = System.nanoTime())`; поле и ходы — из `town.jobs[]` (в демо — `demoMoves`), бомбы уровня — из `Town.shiftQuote`.
- `matchSwap` / `matchBomb` возвращают `Turn`; `MiniGameScreen` проигрывает шаги `Swap → Match → Fall → Refill` по порядку.
- Партия (`vm.match`) живёт только в памяти VM и в `state.json` не пишется. В профиль попадает только итог: `finishRound` → `Town.finishShift(state, jobId, score, bombsUsed)` → оплата смены в конверт (`envelope`, придёт с новым конвертом), счётчики смен, рекорд, списание бомб; строка итога — `roundResult` на экране `Round`. Работа `TAPS` («Помочь Марте») — три кнопки-поручения, счёт — число отмеченных.

## Анимации и доступность

`LocalAnimate` = тумблер «Анимации» (`state.animations`) **и** системный масштаб анимаций > 0 (`ANIMATOR_DURATION_SCALE`, перечитывается при возврате в приложение; `systemAnimates` в [GameApp.kt](../app/src/game/java/ru/finny/pet/game/GameApp.kt)). Сам тумблер показывает `state.animations`, а при системном запрете неактивен и подписан «Выключены в настройках Android». При `LocalAnimate = false`:

- смена экранов и всплывающие окна — без перехода (`EnterTransition.None`, расширения `orNone()`);
- `ParticleController.enabled = false` — новые частицы не создаются;
- `PetSprite` не дышит и не прыгает, текст реплик появляется сразу;
- мини-игра ставит плитки мгновенно (`snap()` вместо пружин).

`LocalLayout` сообщает ориентацию и «компактность» (узкая сторона < 420 dp). Экраны с фиксированной геометрией — все, кроме `Parent`, `Progress` и `Glossary`, — и строка `LineHost` ограничивают системный масштаб шрифта 1,3; эти три панели следуют ему полностью. Эффекты отключаются тумблером «Звуки», фоновая музыка — тумблером «Музыка»; звук не несёт уникальной информации. Подробно — [UX_ACCESSIBILITY.md](UX_ACCESSIBILITY.md).

## Хранение

`filesDir/state.json` — один JSON-документ `GameState` (kotlinx.serialization, `ignoreUnknownKeys`, `encodeDefaults`): у новых полей есть значения по умолчанию, поэтому старые профили читаются.

- Запись: `state.json.tmp` → переименование в `state.json`; это атомарно, при сбое прежний файл цел. Если переименование не удалось, `tmp` копируется поверх `state.json` — эта ветка не атомарна, сбой посреди копирования может оставить файл неполным. При любой ошибке `save` возвращает `false`.
- Нет файла → пустое состояние (`GameState()`). Не читается (`IOException`) → пустое состояние, файл не трогается. Повреждён (`SerializationException`, `IllegalArgumentException`) → переименовывается в `state.json.bad`, а не затирается.
- Сетевых вызовов, разрешений и аккаунтов нет; `android:allowBackup="false"`.

## Схема обновления учебного контента

Задания, товары, цели, глоссарий, вопросы и реплики питомца, виды и цвета питомца, числовые правила — в [app/src/main/assets/content/content.json](../app/src/main/assets/content/content.json). Контент читается при запуске, код экранов от конкретных записей не зависит.

«Городок» `game` — ключ `town`: места, лавки и цены, товары, мечты, работы, жители, события, реплики, загадки, свои `rules`; новое событие существующего вида — запись в `town.events` без правки кода. Задания `tasks` с TOWN-S1d показывает только `classic`. Добавить задание `classic` = добавить объект в массив `tasks`:

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
- Новый товар, мечта, вид или цвет питомца работают без правки кода: товар и мечта без своей картинки рисуются эмодзи из записи (своя мечта и демо-мечта — `goal_custom`), неизвестный вид — оранжевым котом. Своя картинка товара или мечты — WebP в `app/src/game/res/drawable-nodpi/` и строка в `itemRes`/`goalRes` ([ui/TownUi.kt](../app/src/game/java/ru/finny/pet/game/ui/TownUi.kt)); питомца — рендер `pet.py` и `import_sprites.py`.
- В `classic` правки кода требуют только новая тема задания (`enum Theme` + иконка в [TasksScreen.kt](../app/src/classic/java/ru/finny/pet/ui/screens/TasksScreen.kt)) и новый тип задания; в `game` — новый вид события, факт или эффект вне закрытого словаря (`domain/town/Enums.kt`, `Dictionary.kt`; GAME_CONCEPT §7.3).

## Генераторы ассетов

Все картинки и звуки игры собираются скриптами из [tools/art/](../tools/art/); лицензии — [LICENSES.md](LICENSES.md).

| Скрипт | Что делает | Куда попадает |
|---|---|---|
| `pet.py` → `import_sprites.py` | Blender рендерит питомца (3 вида × 3 цвета × 3 стадии × 4 лица = 108 PNG); `import_sprites.py` конвертирует в WebP и перегенерирует `PetSprites.kt` | `app/src/main/res/drawable-nodpi/pet_*.webp`, `app/src/main/java/ru/finny/pet/PetSprites.kt` |
| `pet.py --residents` | Жители города из `town.residents` (`content.json`): своя палитра `RESIDENT_COLORS`, силуэт взрослого, аксессуар из контента и предмет роли `ROLE_PROPS`, 1 кадр на жителя (PNG) | `res_<id>` — в приложение с TOWN-A1f |
| `room.py` | Комната: альбом/портрет × день/вечер (PNG) | `app/src/game/res/drawable-nodpi/room_*` |
| `place.py` | Фоны мест «Городка» — «кит интерьера»: рынок у реки, «У Фомы», пекарня; портрет 1080 × 1920, RGB (PNG) | `bg_<place>_port` — в приложение с TOWN-A1c |
| `facade.py` | Фасады карточек улицы 384 × 384 RGBA: дом, рынок, «У Фомы», пекарня, калитки парка, леса, зоопарка (замок — оверлей UI) (PNG) | `fac_<place>` — с TOWN-A1g |
| `props.py` | Товары, цели, плитки мини-игры, монета (PNG) | там же: `item_*`, `goal_*`, `tile_*`, `ui_coin` |
| `uiprops.py` | Иконки интерфейса: банка и крышки бюджета, копилка, кошелёк и др. (PNG) | там же: `ui_*` |
| `sounds.py` | Синтез эффектов и музыки на stdlib Python, кодирование ffmpeg в OGG | `app/src/game/res/raw/*.ogg` |

`lib.py` — общие хелперы Blender (материалы, свет, камера, выбор GPU: OptiX, CUDA, HIP, oneAPI, Metal, иначе CPU; оболочка интерьера — пол из досок и стены, навес, табличка с объёмными буквами статического Montserrat ExtraBold `montserrat_extrabold.ttf`), `smoke.py` — проверка `lib.py`. `room.py`, `place.py`, `facade.py`, `props.py`, `uiprops.py` выдают PNG; их можно импортировать (CLI — под `__main__`), на деле импортируются два: `facade.py` берёт цвета мест из `place.py`, `place.py` — декор из `props.py`; `to_webp.py` переводит PNG в WebP (фоны `--rgb`, спрайты RGBA). Приёмочные зонды арта — [tools/art_check.py](../../tools/art_check.py) в корне репозитория (регрессия кадра дампом сцены, композиты UI на фон места с контрастом и пересветом, bbox, палитра жителей).

## Тесты

JVM-тесты (JUnit 4) в [app/src/test/java/ru/finny/pet/domain/](../app/src/test/java/ru/finny/pet/domain/) проверяют только `domain`, без Android и эмулятора, на реальном `content.json` (`TestContent` читает его через `ContentRepository.parse`).

| Класс | Что проверяет |
|---|---|
| `EconomyTest` | создание питомца, план, покупки, копилка и цели, задания, конец недели и рост (5 демо-недель подряд), сброс и удаление профиля, лимит мини-игры, вопросы питомца, склонение «монет» |
| `MvpRulesTest` | правила промежуточной сдачи: числа `Rules` совпадают с `content.json`, старые сохранения читаются, выражение питомца, тексты итога недели и копилки, мини-игра открыта только после плана, бонус взрослого, склонение «монет» и тексты без рода ребёнка, поле мини-игры по seed |
| `Match3Test` | поле без готовых совпадений, отказ хода без совпадения, очки и ходы, бомба, конец партии, детерминизм по seed |
| `ContentTest` | корректность и минимальный объём контента (см. выше) |
| `town/*Test` (15 файлов: `TownDayTest`, `WalletTest`, `CheckoutTest`, `PlanRuleTest`, `PricesTest`, `MigrationTest`, `ShiftTest`, `EventCatalogTest`, `FactParserTest`, `BalanceSimTest`, `FiveDemoWeeksTownTest`, `PetTalkTest`, `ContentValidationTest`, `TownContentTest`, `GameStateCompatTest`) | движок «Городка»: дни и неделя, кошелёк и банки, касса, правило плана, цены лавок, миграция и чтение `state.json` 1.3.0, смены, события и симуляция баланса, 5 демо-недель подряд, реплики питомца, разбор словаря событий и проверка `town` в `content.json` |

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

Актуально на ветку `feat/town`: код TOWN-S1d на a262f13 (2026-09-26), после правки по живой проверке и ревью (PASS); `versionName` 1.3.0 (`versionCode 4`) не менялся

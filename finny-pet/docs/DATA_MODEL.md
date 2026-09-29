# Структура данных

Вся модель — `@Serializable` data-классы (kotlinx.serialization) в чистом Kotlin:
профиль — [`domain/GameState.kt`](../app/src/main/java/ru/finny/pet/domain/GameState.kt),
контент — [`domain/Content.kt`](../app/src/main/java/ru/finny/pet/domain/Content.kt).
Игровые изменения профиля делает [`domain/Economy.kt`](../app/src/main/java/ru/finny/pet/domain/Economy.kt):
каждое действие возвращает `Outcome` — `Ok` с новым `GameState` или `Error` с объяснением; `newGame`, `resetProfile`, `deleteProfile` сразу возвращают `GameState` (формулы — [ECONOMY.md](ECONOMY.md)). В `game` денежные и временные действия идут
через [`domain/town/Town.kt`](../app/src/main/java/ru/finny/pet/domain/town/Town.kt) поверх `Economy` (новый `GameState` — в `TownOutcome.state`, отказ — `TownResult.Refused`). Исключение —
переключатели раздела для взрослого: `demo`, `animations`, `sounds`, `music` `GameViewModel`
меняет напрямую (`state.copy`).

Описан сдаваемый вариант `game` (`ru.finny.pet`). Альтернативная сборка `classic`
(`ru.finny.pet.classic`, в сдачу не входит) использует те же классы и тот же формат файла.
Поля и классы концепции «Городок» (срез 1, движок `domain/town`) — в разделе
[«Городок»](#городок-срез-1-движок) ниже; на них с TOWN-S1d работает `game`, `classic` движок не вызывает.

## Профиль — `GameState`

| Поле | Тип | По умолчанию | Назначение |
|---|---|---|---|
| `demo` | Boolean | `false` | Демо-режим: `unlockPeriod` не учитывается — в `classic` все задания открыты сразу, в `game` товары лавок не ждут своей недели; в «Городке» ещё конец недели с любого дня и доска всех живых событий с «Начать», работы с `opensBy.week` открыты сразу, в пекарне размеры заказов — `demoSizes` ([ECONOMY.md](ECONOMY.md), «Городок») |
| `animations` | Boolean | `true` | Анимации; UI анимирует, только если они включены и здесь, и в системных настройках |
| `pet` | `Pet?` | `null` | Питомец; `null` — профиль не создан |
| `period` | Int | `1` | Номер игровой недели |
| `balance` | Int | `0` | Монеты на руках (без копилки), не бывает меньше 0 |
| `savings` | Int | `0` | Монеты в копилке |
| `plan` | `BudgetPlan` | `BudgetPlan()` | План текущей недели ⟲ |
| `purchases` | List<`Purchase`> | `[]` | Покупки текущей недели ⟲ |
| `depositedThisPeriod` | Int | `0` | Положено в копилку за неделю ⟲ |
| `withdrawnThisPeriod` | Int | `0` | Взято из копилки за неделю ⟲ |
| `depositHistory` | List<Int> | `[]` | Чистый взнос каждой завершённой недели (отрицательный — как 0) — для срока цели |
| `goal` | `Goal?` | `null` | Текущая цель |
| `achievedGoals` | List<`Goal`> | `[]` | Достигнутые цели |
| `taskResults` | List<`TaskResult`> | `[]` | Решённые задания, одна запись на задание |
| `history` | List<`PeriodSummary`> | `[]` | Итоги завершённых недель |
| `ledger` | List<`LedgerEntry`> | `[]` | Журнал недели «откуда монеты» ⟲ — новая неделя начинается с записей «Остаток с прошлой недели» (если > 0) и «Карманные деньги на неделю» |
| `sounds` | Boolean | `true` | Переключатель «Звуки» в разделе для взрослого: звуковые эффекты при действиях |
| `music` | Boolean | `false` | Переключатель «Музыка» в разделе для взрослого: фоновая мелодия |
| `bombs` | Int | `0` | Бомбочки для поля Match3: `+rules.quizBombReward` за верный ответ — в 1.3.0 на вопрос питомца, в `game` на «Загадку Бори» (`Town.answerQuestion`), только если пекарня — `MATCH3` (со среза 1а TOWN-J1 она `TRAY`, бомб загадка не даёт); минус использованные в завершённом раунде (в смене `MATCH3` — сверх бомб уровня `jobLevelBombs`) |
| `miniGameEarned` | Int | `0` | Монеты из мини-игры 1.3.0 за неделю, не больше `rules.miniGameCap` ⟲; интерфейс 1.4.0 мини-игру 1.3.0 не вызывает — смена пекарни платит в `envelope` |
| `quizResults` | List<`TaskResult`> | `[]` | Ответы на вопросы питомца 1.3.0 (`Economy.answerQuiz`; интерфейс 1.4.0 их не пишет, ответы на «Загадку Бори» — в `riddles`), `reward` всегда 0. Неверный ответ можно повторить, поэтому на вопрос бывает несколько записей |
| `parentBonusesThisPeriod` | Int | `0` | Число бонусов от взрослого за неделю ⟲. Растёт в `Town.parentBonus` — в `game` кнопки причин в разделе «Бонус ребёнку» для взрослого (`Economy.parentBonus` 1.3.0 интерфейс не вызывает); не больше `rules.parentBonusPerPeriod` |

⟲ — сбрасывается в `Economy.endPeriod` при завершении недели.

Вычисляемые свойства (в файл не пишутся): `hasProfile` = `pet != null`;
`factMandatory`, `factOptional` — сумма покупок категории; `factSavings` =
`depositedThisPeriod − withdrawnThisPeriod`.

### Вложенные классы

| Класс | Поля: тип = по умолчанию | Примечание |
|---|---|---|
| `Pet` | `name`, `speciesId`, `colorId`: String; `hunger`, `clean`, `mood`: Int = 70; `growth`: Int = 0 | Показатели 0–100; при создании — `rules.startStat`, после недели не ниже `rules.statFloor`. `growth` — очки роста, стадия вычисляется (см. ниже) |
| `BudgetPlan` | `mandatory`, `optional`, `savings`: Int = 0; `confirmed`: Boolean = false | `total` — сумма трёх частей (вычисляется) |
| `Purchase` | `itemId`, `title`: String; `category`: `Category`; `need`: `Need`; `price`: Int; `shop`: String? = null; `source`: `Source`? = null | Копия товара на момент покупки; `price` — цена лавки; `shop` и `source` (из какой банки) пишет «Городок» |
| `Goal` | `id`, `title`, `emoji`: String; `price`: Int | Копия `GoalTemplate` (`classic`) или `TownGoal` из `town.goals` (`game`); своя цель — `id = custom_<hashCode названия>_<цена>`, `emoji = ⭐`; вещь, сделанная мечтой (`Town.makeGoal`), — `id = item:<id товара>` по базовой цене; демо-событие — `id = demo_goal` («Мечта для показа») |
| `LedgerEntry` | `text`: String; `amount`: Int | Источник и сумма (расход — отрицательный, достижение цели — 0) |
| `TaskResult` | `taskId`: String; `correct`: Boolean; `reward`: Int; `period`: Int | Общий для заданий и вопросов питомца |
| `PeriodSummary` | `period`: Int; `plan`: `BudgetPlan`; `factMandatory`, `factOptional`, `factSavings`: Int; `mandatoryCovered`, `planKept`, `saved`: Boolean; `score`, `growthBefore`, `growthAfter`, `stageBefore`, `stageAfter`: Int; `messages`: List<String>; `miniGameEarned`: Int = 0; `parentBonus`: Int = 0; `shiftEarned`: Int = 0 | Итог недели: три проверки, `score` = число выполненных (0–3), рост и тексты «что случилось и почему». `miniGameEarned`, `parentBonus`, `shiftEarned` (заработок смен «Городка») — монеты вне плана, по умолчанию 0; остальные поля обязательны |

Перечисления пишутся в JSON по имени: `Category` — `MANDATORY`, `OPTIONAL`, `UNPLANNED` (непредвиденное «Городка»: не входит в
`factMandatory` и `factOptional`); `Need` — `FOOD`, `CARE`, `FUN`, `UNPLANNED` (еда и уход проверяются в конце недели);
`Theme` — `BUDGET`, `SAVINGS`, `SHOPPING`; `TaskType` — `CHOICE`, `NUMBER`.

Персональных данных нет: имя питомца — игровое, вводится ребёнком; реального имени,
телефона, почты, даты рождения не запрашивается.

### Пример файла

Профиль 1.3.0 после первой недели — сохранение до «Городка», без его полей; `game` переносит такое при загрузке через `Migration.migrate` (в файле — одна строка, здесь отформатировано, `messages` сокращены):

```json
{
  "demo": false, "animations": true,
  "pet": { "name": "Финни", "speciesId": "cat", "colorId": "orange", "hunger": 70, "clean": 75, "mood": 98, "growth": 3 },
  "period": 2, "balance": 125, "savings": 30,
  "plan": { "mandatory": 0, "optional": 0, "savings": 0, "confirmed": false },
  "purchases": [], "depositedThisPeriod": 0, "withdrawnThisPeriod": 0, "depositHistory": [30],
  "goal": { "id": "goal_scooter", "title": "Самокат", "emoji": "🛴", "price": 150 },
  "achievedGoals": [],
  "taskResults": [ { "taskId": "budget_first", "correct": true, "reward": 10, "period": 1 } ],
  "history": [ { "period": 1, "plan": { "mandatory": 50, "optional": 20, "savings": 30, "confirmed": true },
                 "factMandatory": 50, "factOptional": 15, "factSavings": 30,
                 "mandatoryCovered": true, "planKept": true, "saved": true, "score": 3,
                 "growthBefore": 0, "growthAfter": 3, "stageBefore": 0, "stageAfter": 0,
                 "messages": ["Прошла неделя: сытость −30, чистота −25, настроение −10.", "…"],
                 "miniGameEarned": 10, "parentBonus": 0 } ],
  "ledger": [ { "text": "Остаток с прошлой недели", "amount": 25 }, { "text": "Карманные деньги на неделю", "amount": 100 } ],
  "sounds": true, "music": false, "bombs": 0, "miniGameEarned": 0,
  "quizResults": [ { "taskId": "q_budget_1", "correct": true, "reward": 0, "period": 1 } ],
  "parentBonusesThisPeriod": 0
}
```

## Хранение — `StateStore`

[`data/StateStore.kt`](../app/src/main/java/ru/finny/pet/data/StateStore.kt): один файл
`filesDir/state.json` в приватной папке приложения. JSON kotlinx.serialization с
`ignoreUnknownKeys = true` и `encodeDefaults = true` (в файл пишутся все поля, включая
равные умолчанию). `GameViewModel.commit` сохраняет весь профиль после каждого изменения.

| Ситуация | Поведение |
|---|---|
| Файла нет (первый запуск) | `GameState()` |
| Файл не читается (`IOException`) | `GameState()`, файл не трогается |
| Битый JSON, нет обязательного поля, недопустимое значение | Файл переименовывается в `state.json.bad`, игра стартует с `GameState()` |
| Запись | Во временный `state.json.tmp`, затем переименование поверх `state.json`; если переименование не удалось — копирование tmp поверх и удаление tmp |
| Ошибка записи | `save` возвращает `false`, игра показывает «Не удалось сохранить». Если не записался `state.json.tmp`, прежний файл цел. Если сорвалось копирование tmp поверх, прежний файл потерян: `copyTo(overwrite = true)` сначала удаляет `state.json`, и файла нет или он записан не полностью (при следующем запуске — `GameState()`, неполный файл уходит в `state.json.bad`) |

**Версия схемы** — поле `stateVersion` (раздел «Городок»): `game` при загрузке прогоняет `Migration.migrate`, `classic` миграций не делает. В остальном совместимость старых
сохранений держится на двух правилах:

- новое поле добавляется только со значением по умолчанию — старый файл без него читается
  (тест `старое сохранение без новых полей читается` в
  [`MvpRulesTest.kt`](../app/src/test/java/ru/finny/pet/domain/MvpRulesTest.kt));
- `ignoreUnknownKeys` — лишние ключи в файле пропускаются.

Смена типа поля или новое поле без умолчания делают старые файлы нечитаемыми (→ `state.json.bad`);
переименованное поле молча получает значение по умолчанию.
Ссылки на контент не ломают загрузку: в game `GameViewModel` при загрузке заменяет неизвестные
`speciesId`/`colorId` питомца первыми из контента (`Content.species()`, `color()`), и питомец
рисуется со всеми стадиями и выражениями; в файл новые id попадут при следующем сохранении.
В classic такой питомец рисуется запасным спрайтом `PetSprites.id` — рыжий кот стадии 0 с довольной
мордой (`pet_cat_orange_0_happy`); результаты удалённых заданий пропускаются (`Economy.completedTasks`).

Не сохраняются: стек экранов, строки LINE, реплика питомца, отчёт ночи и итога недели (`night`, `weekEnd`), метки «новое» на доске и раунд смены (`TrayRound`) — они
живут в памяти `GameViewModel`. «Назад» и ⌂ в раунде завершают смену (`Town.finishShift`); раунд,
оборванный закрытием приложения, не тратит бомбочки и не даёт монет. Ночь (`asleep`) сохраняется: после перезапуска игра открывается на экране ночи (без строки дня), «Проснуться» ведёт в комнату; итог недели после перезапуска заново не показывается.

### Сброс и удаление (раздел для взрослого)

| Действие | Функция | Что остаётся от прежнего профиля |
|---|---|---|
| Сбросить профиль | `Economy.resetProfile` | `demo`, `animations`, `sounds`, `music` |
| Удалить профиль и данные | `Economy.deleteProfile` | `animations`, `sounds`, `music`; `demo = false` |
| Создать тестовый профиль | `GameViewModel.createTestProfile` → `Economy.newGame` | `animations`, `sounds`, `music`; `demo = true` |

Остальные поля получают значения по умолчанию (в `game` затем новое `seed` и `Migration.migrate`), и `state.json` перезаписывается.

## Учебный контент — `Content`

Файл [`assets/content/content.json`](../app/src/main/assets/content/content.json) читается
[`data/ContentRepository.kt`](../app/src/main/java/ru/finny/pet/data/ContentRepository.kt)
при старте, `ignoreUnknownKeys = true`. Разделы без умолчания обязательны: без них разбор
падает. Тесты читают тот же файл
([`TestContent.kt`](../app/src/test/java/ru/finny/pet/domain/TestContent.kt));
[`ContentTest.kt`](../app/src/test/java/ru/finny/pet/domain/ContentTest.kt) проверяет
минимумы ТЗ 2.6: ≥ 9 комбинаций питомца, ≥ 6 заданий по 3 темам, ≥ 8 товаров обоих
типов, ≥ 3 цели, ≥ 3 стадии.

| Раздел | Тип | По умолчанию | В content.json |
|---|---|---|---|
| `rules` | `Rules` | `Rules()` | все 29 полей, значения совпадают с умолчаниями |
| `species` | List<`PetSpecies`> | обязателен | 3: `cat`, `bunny`, `puppy` |
| `colors` | List<`PetColor`> | обязателен | 3: `orange`, `blue`, `green` — 9 комбинаций |
| `items` | List<`ShopItem`> | обязателен | 10: 5 `MANDATORY` (2 `FOOD`, 3 `CARE`) + 5 `OPTIONAL` (`FUN`) |
| `goals` | List<`GoalTemplate`> | обязателен | 4 |
| `customGoal` | `CustomGoalOptions` | обязателен | 5 названий × 4 цены (50–200) |
| `tasks` | List<`Task`> | обязателен | 10: `BUDGET` 3, `SAVINGS` 3, `SHOPPING` 4; 7 `CHOICE` + 3 `NUMBER` |
| `glossary` | List<`GlossaryEntry`> | обязателен | 10 терминов |
| `quiz` | List<`QuizQuestion`> | `[]` | 15, по 5 на тему |
| `chatter` | `Chatter` | `Chatter()` | `idle` 6, `hungry` 2, `dirty` 2, `bored` 2, `proud` 2, `facts` 4 |
| `parentBonusReasons` | List<String> | `[]` | 4 причины |
| `town` | `TownContent?` | `null` | «Городок» — см. раздел ниже |

| Класс | Поля: тип = по умолчанию | Примечание |
|---|---|---|
| `PetSpecies` | `id`, `title`: String | |
| `PetColor` | `id`, `title`, `hex`: String | `hex` — `#RRGGBB` |
| `ShopItem` | `id`, `title`, `emoji`: String; `category`: `Category`; `need`: `Need`; `price`: Int; `hunger`, `clean`, `mood`: Int = 0; `description`, `reaction`: String; `keep`: Boolean = false; `slot`: String? = null; `unlockPeriod`: Int = 1; `tags`: List<String> = []; `eventOnly`: Boolean = false | `hunger`/`clean`/`mood` — изменение показателей при покупке; в `reaction` `{pet}` заменяется именем. Поля с `keep` — «Городок»: вещь остаётся в доме на месте типа `slot`, открывается с недели `unlockPeriod`, `eventOnly` — только в сцене события |
| `GoalTemplate` | `id`, `title`, `emoji`: String; `price`: Int | Готовая цель |
| `CustomGoalOptions` | `titles`: List<String>; `prices`: List<Int> | Конструктор своей цели |
| `Task` | `id`: String; `theme`: `Theme`; `title`, `situation`: String; `type`: `TaskType`; `options`: List<`TaskOption`> = []; `answer`: Int? = null; `explanationCorrect`, `explanationWrong`: String = ""; `unlockPeriod`: Int = 1 | `CHOICE` — `options`, `NUMBER` — `answer` и два объяснения; открывается с недели `unlockPeriod` (в демо — сразу) |
| `TaskOption` | `text`: String; `correct`: Boolean; `explanation`: String | |
| `GlossaryEntry` | `term`, `definition`: String | Справочник |
| `QuizQuestion` | `id`: String; `theme`: `Theme`; `question`: String; `options`: List<String>; `correct`: Int (индекс); `explanation`: String | Вопрос питомца в комнате `game` 1.3.0 (`quiz`) и «Загадка Бори» (`town.quiz`); все поля обязательны |
| `Chatter` | `idle`, `hungry`, `dirty`, `bored`, `proud`, `facts`: List<String> = [] | Реплики питомца в комнате `game` 1.3.0: `hungry`/`dirty`/`bored` — при показателе ниже 40, иначе `idle`, `facts` и подсказка следующего шага; `{pet}` → имя. В 1.4.0 `chatter` код не читает: у `game` реплики — `town.chatter` |

`parentBonusReasons` — причины бонуса от взрослого 1.3.0 (в `game` — `town.parentBonusReasons`, их берёт `Town.parentBonus`
и пишет причину в конверт). `Economy.parentBonus` принимает индекс
причины и пишет её в журнал.

### `Rules` — числа экономики

Все поля Int, кроме списков. Как они применяются — [ECONOMY.md](ECONOMY.md).

| Группа | Поля = по умолчанию |
|---|---|
| Доход | `allowance` = 100, `rewardCorrect` = 10, `rewardWrong` = 5 |
| Конец недели | `decayHunger` = 30, `decayClean` = 25, `decayMood` = 10, `uncoveredExtraDrop` = 20, `uncoveredMoodDrop` = 20, `statFloor` = 10 |
| Бонусы настроения | `planKeptMoodBonus` = 10, `savedMoodBonus` = 10, `goalMoodBonus` = 30, `correctMoodBonus` = 5, `quizMoodBonus` = 3 |
| Питомец | `startStat` = 70, `faceSadBelow` = 30, `faceHappyAvg` = 60, `needLowBelow` = 40, `maxNameLength` = 12 |
| Стадии | `stageThresholds` = [0, 4, 9], `stageTitles` = [«Малыш», «Подросток», «Взрослый»] |
| Мини-игра и вопросы | `miniGameCap` = 30, `miniGameScorePerCoin` = 20, `miniGameMoves` = 15, `quizBombReward` = 1 |
| Бонус от взрослого | `parentBonusAmount` = 10, `parentBonusPerPeriod` = 3 |
| Подсказка копилки | `savingsAmounts` = [10, 20, 30, 50], `planStep` = 10 — итог недели без накоплений советует отложить первую сумму `savingsAmounts`, при пустом списке — `planStep` |

## «Городок» (срез 1, движок)

Концепция — [`docs/GAME_CONCEPT.md`](../../docs/GAME_CONCEPT.md) §16, спеки —
[`docs/tasks/TOWN-S0a.md`](../../docs/tasks/TOWN-S0a.md), [`TOWN-S1a.md`](../../docs/tasks/TOWN-S1a.md).
Движок — классы `Town`, `Prices`, `Migration` в
[`domain/town/`](../app/src/main/java/ru/finny/pet/domain/town/), формулы — [ECONOMY.md](ECONOMY.md),
раздел «Городок». С TOWN-S1d ([`TOWN-S1d.md`](../../docs/tasks/TOWN-S1d.md)) на движке работает `game`, `classic` его не вызывает.

### Поля `GameState` «Городка» (все с умолчаниями, старое сохранение читается)

| Поле | Тип = по умолчанию | Назначение |
|---|---|---|
| `stateVersion` | Int = 0 | Версия схемы: `Migration.migrate` при 0 переносит сохранение 1.3.0 и ставит 1; `Town.confirmPlan` тоже ставит 1 |
| `day` | Int = 1 | День игровой недели 1..`town.rules.daysPerWeek`; двигает только «Проснуться» после обычной ночи |
| `asleep` | Boolean = false | Ночь: после «Спать» до «Проснуться»; ночью касса, план и копилка отказывают |
| `seed` | Long = 0 | Зерно пула событий; миграция ставит 20260926, если 0 |
| `jarNeed`, `jarWant` | Int = 0 | Банки плана «Нужное» и «Хочу»; до подтверждения плана — 0 |
| `envelope` | List<`LedgerEntry`> = [] | Конверт следующей недели: заработок смен («Смена: …»), бонус взрослого («Бонус от взрослого: …»); в `balance` не входит, в конце недели дописывается в `ledger` |
| `owned` | List<String> = [] | Все вещи ребёнка (сундук), никогда не уменьшается |
| `placed` | Map<String, String> = {} | Место → вещь на шести местах `town.spots`; стартовые вещи сюда не пишутся |
| `broken` | List<String> = [] | Сломанные стартовые вещи (срез 2) |
| `houseColor` | String? = null | Цвет дома (срез 2) |
| `shiftsThisPeriod` | Int = 0 | Оплачиваемые смены недели, не больше `town.rules.shiftsPerWeek`; обнуляется в конце недели |
| `jobShifts`, `records` | Map<String, Int> = {} | Оплачиваемые смены (уровень мастерства) и лучший счёт по работам; у подноса `TRAY` — звёзды (`Town.bestStars`: число больше `shiftBonusMax` — наследие `Match3`, не рекорд); «Дневник» показывает только звёзды подноса от одной — «{работа}: лучшая смена ★★★» |
| `events` | List<`EventState`> = [] | События: `id`, `status` (ACTIVE / DONE), `verdict`, `outcome` (индекс сработавшего исхода, −1 — закрыто без исхода), `period` и `day` прихода |
| `stickers`, `visited` | List<String> = [] | Наклейки (события — за любой исход, места — при первом входе, заказ — за оплачиваемую смену) и посещённые места |
| `seenPrices` | Map<String, `SeenPrice`> = {} | Холодильник: товар → (лавка, цена, неделя) — самая низкая цена недели |
| `notes` | List<String> = [] | Заметки на холодильнике (эффект `NOTE` исхода события) |
| `planDraft` | `BudgetPlan`? = null | Не используется: заготовка — сам неподтверждённый `plan` новой недели |
| `freeFunDay` | Int = 0 | Качели раз в день (срез 2) |
| `diary` | List<`DiaryLine`> = [] | Дневник: `period`, `day`, `text` («Купили у реки: каша», «Заработали 9: «Помочь Боре»», «Мечта сбылась: …») |
| `riddles` | List<`TaskResult`> = [] | Ответы на «Загадку Бори» (`town.quiz`), отдельно от `quizResults` викторины 1.3.0 |
| `riddleAsked` | Boolean = false | Загадка в этом заказе пекарни уже была; сбрасывает конец смены пекарни |

Вычисляемое свойство `reserve` = `balance − jarNeed − jarWant` — запас (до плана — «Не разложено»).

### `content.json → town` (`TownContent`)

Все разделы обязательны: `rules` (`TownRules`, 11 полей без умолчаний, с TOWN-J1-0 — `riddleHint`), `places`, `shops`
(`id`, `place`, `title`, `at` — «у реки», `sells` — товар и цена лавки), `items`, `homeItems`,
`spots` (6 мест: `id`, `slot` — `floor`, `wall`, `table`), `goals`, `jobs`, `residents`, `events`,
`stickers`, `quiz`, `parentBonusReasons`; `eventsOff` (по умолчанию пусто) — id событий, которые в этой
сборке не приходят (сцены среза 2); `chatter` (по умолчанию пусто) — реплики питомца по нажатию `needFood`, `needCare`,
`sad`, `calm` (`Town.petLine`). Проверяет `ContentValidationTest`. `Content.item(id)`
ищет товар и в `items`, и в `town.items`.

Работа `jobs[]` (TOWN-J1-0): `game` — `MATCH3`, `TAPS`, `CHANGE` или `TRAY`; у подноса `TRAY` — `menu` (изделия `id`,
`title`, `emoji`; в `game` изделие рисуется спрайтом `pastry_<id>` через `pastryRes`, `emoji` — запасной рисунок изделия
без него), `steps` (ступени меню: `fromShift` — после скольких оплачиваемых смен, `kinds` — сколько первых
изделий на витрине, `sizes` — размеры заказов покупателей, `intro` — реплика на первой смене ступени) и `demoSizes` —
заказы демо-режима; `board`, `moves`, `demoMoves` — поле `MATCH3`. Пекарня — `TRAY` со среза 1а TOWN-J1, работ `MATCH3` в контенте нет.

## Прогресс и стадии

Стадия = наибольший индекс `i`, для которого `growth ≥ rules.stageThresholds[i]` (не меньше 0);
название — `rules.stageTitles[i]`, а если его нет — «Стадия N». За неделю `growth` растёт
на `score` итога (0–3). По умолчанию `[0, 4, 9]` → «Малыш», «Подросток», «Взрослый».

Актуально на выпуск 1.4.0 (2026-09-29); отметки «1.3.0» — выпуск 1.3.0

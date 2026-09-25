```
TASK: MVP-T01 — правила игры для промежуточной сдачи
BASE: 0221972
BRANCH: feat/mvp-7-1

## КОНТЕКСТ
Решения сеньора 2026-09-25 меняют экономику (награда за задание, бонус взрослого,
мини-игра после плана) и закрывают подтверждённые ошибки правил (снятие из копилки
ломает «траты по плану», повторный ответ на вопрос питомца, случайность в domain,
числа в коде). UI варианта `game` правится отдельной задачей MVP-T03 поверх этого API.

## ТРЕБОВАНИЕ ТЗ
- 2.1: «ограниченность ресурсов: за один игровой период нельзя купить все сразу» —
  сейчас в демо за неделю 100 + 10×20 + 30 = 330 монет при каталоге 280.
- 2.5.12: «Правила начисления родителем дополнительных баллов и их реализацию команда
  определяет самостоятельно» — единственный пункт 2.5 без реализации.
- 2.8: дополнительные возможности «не компенсируют отсутствие обязательных функций» —
  мини-игра только после плана, её монеты видны отдельно.
- 2.5.7: «Можно регулярно переводить часть игровой валюты в накопления…» — снятие по
  комментарию `Economy.kt:148` и `ECONOMY.md:20` — способ профинансировать план,
  а сейчас любое снятие проваливает «траты по плану» даже при плане копилки 0.
- 3.5: тексты не стыдят («Ты не купил еду…»).
- CLAUDE.md: числа экономики — только в `content.json`; случайность в domain — только
  от переданного seed.

## CONTRACT

### A. Rules (`Content.kt`, значения по умолчанию = значения в content.json)
Новые поля `Rules`:
| поле | тип | значение |
|---|---|---|
| `rewardCorrect` | Int | **10** (было 20; и в `Rules()`, и в content.json) |
| `parentBonusAmount` | Int | 10 |
| `parentBonusPerPeriod` | Int | 3 |
| `quizBombReward` | Int | 1 |
| `startStat` | Int | 70 |
| `faceSadBelow` | Int | 30 |
| `faceHappyAvg` | Int | 60 |
| `needLowBelow` | Int | 40 |
| `planStep` | Int | 10 |
| `savingsAmounts` | List<Int> | [10, 20, 30, 50] |

Новое поле `Content`: `parentBonusReasons: List<String> = emptyList()`; в content.json —
ровно 4 причины: «Помог по дому», «Довёл дело до конца», «Сам навёл порядок»,
«Придумал, как сэкономить».

### B. Состояние (`GameState.kt`; у всех новых полей значения по умолчанию — старые файлы сохранений читаются)
- `GameState.music: Boolean = false` — музыка (отдельно от `sounds` = эффекты, `true`).
- `GameState.parentBonusesThisPeriod: Int = 0`.
- `PeriodSummary.miniGameEarned: Int = 0`, `PeriodSummary.parentBonus: Int = 0` (монеты).

### C. Economy
1. `newGame(demo: Boolean, animations: Boolean = true, sounds: Boolean = true, music: Boolean = false)`.
2. `resetProfile(s)` = `newGame(s.demo, s.animations, s.sounds, s.music)`.
3. `deleteProfile(s)` = `GameState(animations = s.animations, sounds = s.sounds, music = s.music)`.
4. `createPet`: сытость, чистота, настроение = `rules.startStat`.
5. `face(pet)`: SAD, если min < `faceSadBelow`; HAPPY, если avg ≥ `faceHappyAvg` и
   min ≥ `needLowBelow`; иначе NEUTRAL. `faceReason`: показатель «низкий», если <
   `needLowBelow`; слово «грязный» заменить на «хочет помыться».
6. Конец недели, проверка «траты по плану», часть копилки:
   `max(factSavings, 0) ≥ plan.savings`. При плане копилки 0 снятие проверку не
   проваливает. Если часть копилки не выполнена, сообщение:
   - снятий не было: `В копилку отложил D, а планировал S.`
   - снятия были: `В копилку чистыми N (положил D, взял W), а планировал S.`
   где D = `depositedThisPeriod`, W = `withdrawnThisPeriod`, N = `max(D − W, 0)`.
7. Конец недели, тексты без упрёка:
   - нет еды: `Еды на этой неделе не было — {pet} проголодался сильнее: сытость ещё −{uncoveredExtraDrop}.`
   - нет ухода: `Ухода на этой неделе не было — {pet} запачкался сильнее: чистота ещё −{uncoveredExtraDrop}.`
   - копилка не выросла: `На этой неделе копилка не выросла. Попробуй отложить хотя бы {X} монет — так цель приблизится.`, X = `savingsAmounts.first()`, а если список пуст — `planStep`.
   - `Рост: +{score} из {K} (всего {growth}).`, K — число проверок (3), не литерал.
   - если за неделю мини-игра дала M > 0: `Вне плана: игра «Монетки в ряд» принесла M монет.`
   - если бонусов взрослого B > 0 монет: `Вне плана: бонус от взрослого — B монет.`
   - обе строки «вне плана» — перед строкой `Новая неделя: …`.
   - `PeriodSummary.miniGameEarned` = `miniGameEarned` недели, `parentBonus` =
     `parentBonusesThisPeriod × parentBonusAmount`; в новой неделе `parentBonusesThisPeriod = 0`.
8. `confirmPlan` при обязательном 0: `На обязательное в плане 0 монет, а {pet} всё равно понадобятся еда и уход — оставь на них монеты.`
9. `buy` при нехватке монет — подсказки:
   - `Выполни задание — за него дают монеты` — только если `availableTasks(s)` не пуст;
   - `Можно взять из копилки, но цель отодвинется` — только если `s.savings ≥ недостающей суммы`;
   - остальные подсказки — как сейчас.
10. `answerQuiz`: вопрос не из `availableQuiz(s)` → `Outcome.Error("На этот вопрос ты уже ответил")`,
    состояние не меняется. Верный ответ даёт `bombs + rules.quizBombReward`.
11. Мини-игра:
    - `fun miniGameLock(s: GameState): String?` — `null`, если играть можно;
      нет питомца → `Сначала создай питомца`; план не подтверждён →
      `Сначала составь план на неделю — игра откроется после него`.
    - `finishMiniGame` при `miniGameLock(s) != null` → `Outcome.Error(<тот же текст>)`.
12. Бонус взрослого:
    - `fun parentBonusesLeft(s: GameState): Int` = `max(parentBonusPerPeriod − parentBonusesThisPeriod, 0)`.
    - `fun parentBonus(s: GameState, reasonIndex: Int): Outcome`:
      - нет питомца → Error `Сначала создай питомца`;
      - индекс вне `parentBonusReasons` → Error `Выберите причину бонуса`;
      - лимит исчерпан → Error `На этой неделе все бонусы уже начислены — новые будут со следующей недели.`
      - Ok: баланс `+parentBonusAmount`, `parentBonusesThisPeriod + 1`, в журнал
        `LedgerEntry("Бонус от взрослого: <причина>", parentBonusAmount)`; сообщения:
        `Бонус от взрослого: +A монет — «<причина>». На балансе B.` и
        `На этой неделе можно начислить ещё K.` (K после начисления; при K = 0 —
        `Лимит бонусов на эту неделю исчерпан.`).
    - План бонус не трогает (как и награды за задания).

### D. Match3
`Match3.newGame(width, height, moves, bombs, seed: Long)` — у `seed` нет значения по
умолчанию. Вызов в `game/…/GameViewModel.kt` передаёт seed (например, `System.nanoTime()`) —
это единственная правка в UI-коде, чтобы сборка оставалась зелёной.

### E. content.json
- `rules`: `rewardCorrect: 10` и все новые поля из A.
- `parentBonusReasons`: 4 строки из A.
- задание `shop_unexpected`: «Лекарство стоит 25 монет» → «Лекарство стоит 15 монет»
  (бантик в магазине стоит 15).

### Не определено (спросить, а не решать)
Ничего сверх перечисленного. Любой новый текст для ребёнка, не указанный здесь дословно, — BLOCKED.

## SCOPE
variant: main (общий код)
allow:
  finny-pet/app/src/main/java/ru/finny/pet/domain/Economy.kt
  finny-pet/app/src/main/java/ru/finny/pet/domain/GameState.kt
  finny-pet/app/src/main/java/ru/finny/pet/domain/Content.kt
  finny-pet/app/src/main/java/ru/finny/pet/domain/Match3.kt
  finny-pet/app/src/main/assets/content/content.json
  finny-pet/app/src/game/java/ru/finny/pet/game/GameViewModel.kt   (только seed в startMiniGame)
protect: finny-pet/app/src/test/, *.gradle.kts, gradle/, gradle.properties, .claude/, CLAUDE.md, docs/

## ANTI-SCOPE
- Экраны и ViewModel сверх seed — задача MVP-T03 (кнопка бонуса, музыка, блокировка игры).
- Документация (ECONOMY.md, DATA_MODEL.md) — оркестратор после приёмки.
- Константы движка Match3 (размер поля, очки за клетку) — не трогать.
- StateStore, classic, новые зависимости, переименования существующих полей.

## БЮДЖЕТ
диff (без тестов): ≤ 350 строк; новые файлы: 0; зависимости: 0

## ORACLE
test-author: `finny-pet/app/src/test/java/ru/finny/pet/domain/MvpRulesTest.kt` (новый) и
правка существующих тестов, где зашито 20 за задание или старые тексты.

## ACCEPTANCE (из finny-pet/, на Windows — .\gradlew.bat и python)
  1. ./gradlew testClassicDebugUnitTest testGameDebugUnitTest --console=plain -> exit 0
  2. tests = 34 + <N оракула> в каждом варианте, failed = 0, skipped = 0 (подсчёт по XML)
  3. ./gradlew assembleClassicDebug assembleGameDebug          -> exit 0
  4. ./gradlew lintClassicDebug lintGameDebug                  -> 0 ошибок, предупреждений ≤ 8 / 9
  5. git diff --name-only <ORACLE_SHA> -- app/src/test/        -> пусто
  6. git diff --name-only <BASE> -- '*.gradle.kts' gradle/ gradle.properties -> пусто
  7. git diff --shortstat <ORACLE_SHA> -- app/src/main          -> ≤ 350 строк
  8. grep -rnE '^import (android|androidx)' app/src/main/java/ru/finny/pet/domain/ -> пусто
  9. grep -n 'Random.nextLong\|Random()' app/src/main/java/ru/finny/pet/domain/ -> пусто

## ЖИВАЯ ПРОВЕРКА
Экранов задача не меняет, кроме текстов в итоге недели и подсказках — их проверит MVP-T03.

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED / QUESTION: <один вопрос>.
```

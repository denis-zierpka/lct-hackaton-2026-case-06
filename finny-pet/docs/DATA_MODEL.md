# Структура данных

## Локальный профиль — `GameState` (`filesDir/state.json`)

| Поле | Тип | Назначение |
|---|---|---|
| `demo` | Boolean | Демо-режим: все задания открыты сразу |
| `animations` | Boolean | Включены ли анимации питомца |
| `pet` | `Pet?` | `null`, пока профиль не создан |
| `period` | Int | Номер игровой недели, с 1 |
| `balance` | Int | Доступные монеты (без копилки) |
| `savings` | Int | Копилка |
| `plan` | `BudgetPlan` | `mandatory`, `optional`, `savings`, `confirmed` |
| `purchases` | List<`Purchase`> | Покупки текущей недели: `itemId`, `title`, `category`, `need`, `price` |
| `depositedThisPeriod`, `withdrawnThisPeriod` | Int | Движения по копилке за неделю |
| `depositHistory` | List<Int> | Чистые взносы по завершённым неделям (для расчёта срока цели) |
| `goal` | `Goal?` | Текущая цель: `id`, `title`, `emoji`, `price` |
| `achievedGoals` | List<`Goal`> | Достигнутые цели |
| `taskResults` | List<`TaskResult`> | `taskId`, `correct`, `reward`, `period` |
| `history` | List<`PeriodSummary`> | Итоги завершённых недель (план/факт, три проверки, рост, объяснения) |
| `ledger` | List<`LedgerEntry`> | Журнал недели: `text`, `amount` — источник каждой монеты |

`Pet`: `name`, `speciesId`, `colorId`, `hunger`, `clean`, `mood` (0–100), `growth` (очки роста; стадия вычисляется из порогов правил).

Персональных данных нет: имя питомца — игровое, вводится ребёнком; реального имени, телефона, почты, даты рождения не запрашивается.

Пример файла:

```json
{
  "demo": true, "animations": true,
  "pet": { "name": "Финни", "speciesId": "cat", "colorId": "orange", "hunger": 70, "clean": 75, "mood": 90, "growth": 2 },
  "period": 2, "balance": 155, "savings": 20,
  "plan": { "mandatory": 0, "optional": 0, "savings": 0, "confirmed": false },
  "purchases": [], "depositedThisPeriod": 0, "withdrawnThisPeriod": 0, "depositHistory": [20],
  "goal": { "id": "goal_scooter", "title": "Самокат", "emoji": "🛴", "price": 150 },
  "achievedGoals": [],
  "taskResults": [ { "taskId": "budget_first", "correct": true, "reward": 20, "period": 1 } ],
  "history": [ { "period": 1, "plan": {...}, "factMandatory": 50, "factOptional": 15, "factSavings": 20,
                 "mandatoryCovered": true, "planKept": false, "saved": true, "score": 2,
                 "growthBefore": 0, "growthAfter": 2, "stageBefore": 0, "stageAfter": 0, "messages": ["..."] } ],
  "ledger": [ { "text": "Остаток с прошлой недели", "amount": 55 }, { "text": "Карманные деньги на неделю", "amount": 100 } ]
}
```

## Учебный контент — `Content` (`assets/content/content.json`)

| Раздел | Поля | Минимум по ТЗ | В прототипе |
|---|---|---|---|
| `rules` | числовые параметры экономики (см. ECONOMY.md) | — | — |
| `species` | `id`, `title` | 9 комбинаций питомца | 3 вида × 3 цвета = 9 |
| `colors` | `id`, `title`, `hex` | | |
| `items` | `id`, `title`, `emoji`, `category` (MANDATORY/OPTIONAL), `need` (FOOD/CARE/FUN), `price`, `hunger`, `clean`, `mood`, `description`, `reaction` | 8 позиций двух типов | 10 (5 + 5) |
| `goals` | `id`, `title`, `emoji`, `price` | 3 цели | 4 готовых + конструктор своей цели (5 названий × 4 цены) |
| `customGoal` | `titles[]`, `prices[]` | | |
| `tasks` | `id`, `theme` (BUDGET/SAVINGS/SHOPPING), `title`, `situation`, `type` (CHOICE/NUMBER), `options[]` или `answer` + объяснения, `unlockPeriod` | 6 заданий по 3 темам | 10 (3 + 3 + 4) |
| `glossary` | `term`, `definition` | справочный раздел | 10 терминов |

`reaction` поддерживает подстановку `{pet}` — имя питомца.

## Прогресс и стадии

Стадия = наибольший индекс `i`, для которого `growth ≥ rules.stageThresholds[i]`; название — `rules.stageTitles[i]`. Значения по умолчанию: `[0, 4, 9]` → «Малыш», «Подросток», «Взрослый».

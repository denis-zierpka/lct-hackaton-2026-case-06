```
TASK: TOWN-S1b — смены, «Загадка Бори», бонус взрослого в конверт
EPIC: TOWN-S1 (docs/tasks/TOWN-S1.md)
BASE: <sha коммита оракула> (для кодера и ревьювера)
BRANCH: feat/town

## КОНТЕКСТ
Срез 1, блок «Смены» (GAME_CONCEPT §17.3, §5.2, §5.5). Работа у жителя — заказ-задание: база
гарантирована, надбавка за результат, лимит оплачиваемых смен в неделю, мастерство растёт от
числа смен; вся оплата — зарплата в конверте следующей недели (решение 1). Продолжает класс Town
из S1a; UI смены — S1e.

## ТРЕБОВАНИЕ ТЗ
2.5.4 «валюта начисляется за выполнение заданий… для каждого начисления указываются источник и
сумма»; 2.1 «за один игровой период нельзя купить всё сразу» (лимит смен); 2.8 «дополнительные
возможности… не компенсируют отсутствие обязательных» (игра ради рекорда — после плана);
2.5.12 бонус взрослого; Приложение А шаг 6 «выполнение задания и получение игровой валюты…
с объяснением результата».

## CONTRACT
Методы класса Town (S1a), пакет ru.finny.pet.domain.town. R = town.rules, job — запись town.jobs.

### 1. Открыта ли работа и сколько она платит
data class ShiftQuote(val jobId: String, val open: Boolean, val paid: Boolean, val canPlay: Boolean,
    val level: Int, val base: Int, val levelBombs: Int, val shiftsLeft: Int, val line: String)
fun Town.shiftQuote(s: GameState, jobId: String): ShiftQuote
- open: работа есть в town.jobs, её место открыто и она сама открыта: opensBy.week → s.period ≥ week
  или s.demo; opensBy.goal → цель с этим id есть в s.achievedGoals (и в демо); место — те же
  правила по places[job.place].opensBy.
- shiftsLeft = max(0, R.shiftsPerWeek − s.shiftsThisPeriod); paid = open и shiftsLeft > 0
  (до плана тоже: решение 4).
- canPlay = open и (paid или s.plan.confirmed): игра «ради рекорда» без монет — только после плана.
- level = последний индекс i, где R.jobLevelShifts[i] ≤ s.jobShifts[jobId] ?: 0 (0 — первый уровень);
  base = job.baseByLevel[level]; levelBombs = R.jobLevelBombs[level] для MATCH3, 0 для TAPS и CHANGE.
- line: !open «Эта работа пока закрыта»; paid «База {base} + до {R.shiftBonusMax} за результат»
  (TAPS — «База {base} за поручения»); !paid и canPlay «Смены на неделе закончились — можно играть
  ради рекорда»; !paid и !canPlay «Смены на неделе закончились. Ради рекорда — после раскладки».
  Для неизвестной работы open = false, остальные поля 0 / false.

### 2. Конец смены
fun Town.finishShift(s: GameState, jobId: String, score: Int, bombsUsed: Int): TownResult
Refused по порядку: pet «Сначала создай питомца»; !open «Эта работа пока закрыта»; !canPlay
«Смены на неделе закончились. Ради рекорда — после раскладки»; score < 0 или bombsUsed < 0 или
bombsUsed > levelBombs + s.bombs «Так закончить смену нельзя».
Надбавка bonus: MATCH3 — min(R.shiftBonusMax, score / R.shiftScorePerBonus); TAPS — 0;
CHANGE — min(R.shiftBonusMax, score) (score — число верных сдач). Бомбы на надбавку не влияют
иначе как через score: потолок bonus ≤ shiftBonusMax при любом числе бомб.
Done:
- paid: total = base + bonus; envelope += LedgerEntry(«Смена: {job.title}», total) (префикс
  «Смена: » читает endWeek S1a); shiftsThisPeriod += 1; jobShifts[jobId] += 1;
  DiaryLine(period, day, «Заработали {total}: «{job.title}»»). balance, банки и копилка НЕ меняются.
- всегда: records[jobId] = max(прежний или 0, score); s.bombs −= max(0, bombsUsed − levelBombs)
  (сначала тратятся бомбы уровня, потом накопленные за вопросы).
- показатели питомца не меняются (благодарность и булочка — только анимация UI).
line:
- paid MATCH3, bonus > 0: «База {base} + {bonus} за булочки. ✉ +{total} — придёт с новым конвертом»;
  bonus == 0: «База {base}. ✉ +{total} — придёт с новым конвертом»;
- paid TAPS: «База {base} за поручения. ✉ +{total} — придёт с новым конвертом»;
- paid CHANGE: «База {base} + {bonus} за сдачу. ✉ +{total} — придёт с новым конвертом» (bonus == 0 — как MATCH3);
- не paid: «Счёт {score}. Это игра ради рекорда» + (« Новый рекорд!», если score > прежнего рекорда).
why (paid): [«{resident.name}: смен {n} / {следующий порог} до уровня {level + 2}» или, на последнем
уровне, «{resident.name}: смен {n}, высший уровень», «Зарплату приносит почтальон вместе с
карманными — сначала работа, потом зарплата»]; n — jobShifts после смены, уровень — после смены.
why (не paid): [].
В S1c finishShift дополнительно даст наклейку заказа; в S1b наклеек нет.

### 3. «Загадка Бори» (решение 10)
fun Town.nextQuestion(s: GameState): QuizQuestion?
fun Town.answerQuestion(s: GameState, questionId: String, optionIndex: Int): TownResult
- Доступные — вопросы town.quiz, на которые в s.quizResults нет верного ответа. Порядок: сначала
  ни разу не заданные (в порядке town.quiz), потом по возрастанию индекса ПОСЛЕДНЕЙ попытки в
  quizResults. nextQuestion — первый; нет доступных → null (кнопка исчезает).
- answerQuestion. Refused: pet; вопроса нет в town.quiz или на него уже есть верный ответ «Этот
  вопрос уже разобран»; optionIndex вне options «Выбери ответ». Done: quizResults += TaskResult(id,
  correct, 0, s.period); верно → bombs += content.rules.quizBombReward. Показатели питомца и монеты
  не меняются (quizMoodBonus в game не используется). line = question.explanation; why: верно →
  [«Бомбочка +{quizBombReward} — для поля «Булочки в ряд»»], неверно → [«Вопрос вернётся позже»].
- Неверно отвеченный вопрос следующим не задаётся, если доступных больше одного (свойство порядка).

### 4. Бонус взрослого (решение 21)
fun Town.parentBonus(s: GameState, reasonIndex: Int): TownResult
Refused: pet «Сначала создай питомца»; reasonIndex вне town.parentBonusReasons «Выберите причину
бонуса»; s.parentBonusesThisPeriod ≥ content.rules.parentBonusPerPeriod «На этой неделе все бонусы
уже начислены — новые будут со следующей недели». Done: envelope += LedgerEntry(«Бонус от
взрослого: {причина}», content.rules.parentBonusAmount); parentBonusesThisPeriod += 1; balance не
меняется. line «Придёт в новом конверте ребёнка»; why [«На этой неделе можно ещё {осталось}»]
или [«Лимит бонусов на эту неделю исчерпан»].

## SCOPE
variant: main
allow:   finny-pet/app/src/main/java/ru/finny/pet/domain/town/   (Town.kt и новый файл ≤ 1)
protect: базовый + finny-pet/app/src/main/ кроме allow + finny-pet/docs/ + content.json

## ANTI-SCOPE
UI смены и Match3 (S1e); наклейки и JOB-события (S1c); Economy (answerQuiz, parentBonus,
finishMiniGame в game не вызываются и не меняются); content.json; доки (оркестратор).

## БЮДЖЕТ
≤ 300 вставок в app/src/main; новые файлы ≤ 1; зависимости 0

## ORACLE (test-author до кодера): app/src/test/java/ru/finny/pet/domain/town/ShiftTest.kt
Лимит 3 оплачиваемые смены в неделю на все работы вместе и сброс в endWeek; база без ловкости
(TAPS и MATCH3 со счётом 0 дают базу); формулы надбавки по game с порогами (29 / 30 / 120 / 1000
очков); надбавка ≤ shiftBonusMax при любом числе бомб; уровни по порогам 0 / 6 / 15 и база по
уровню; бомбы уровня и накопленные; оплачиваемая смена до плана открыта, игра ради рекорда до
плана — отказ, после плана — без монет, рекорд растёт; зарплата — в envelope, balance и
банки не меняются, на planKept не влияет (endWeek); строки line/why дословно; открытие работ
(неделя, демо, мечта-ключ); загадка: порядок очереди, неверный не повторяется сразу, верный даёт
бомбу и не меняет показатели и монеты, после 15 верных — null; бонус взрослого — в envelope,
лимит, строки. Стоп-слова и род — на всех строках выдачи.

## ACCEPTANCE (из finny-pet/, оркестратор) — как в S1a, п. 1–7; п. 8 — ≤ 300 вставок;
  9. мутанты: надбавка без потолка; неоплачиваемая смена пишет в envelope; заработок в balance;
     бомбы уровня не выдаются; неверный вопрос задаётся снова первым
```

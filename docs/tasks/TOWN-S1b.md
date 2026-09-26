```
TASK: TOWN-S1b — смены, «Загадка Бори», бонус взрослого в конверт
EPIC: TOWN-S1 (docs/tasks/TOWN-S1.md)
BASE: <sha коммита оракула> (для кодера и ревьювера); старт — 31044ea (S1a сделан)
BRANCH: feat/town

## КОНТЕКСТ
Срез 1, блок «Смены» (GAME_CONCEPT §17.3, §5.2, §5.5). Работа у жителя — заказ-задание: база
гарантирована, надбавка за результат, лимит оплачиваемых смен в неделю, мастерство растёт от
числа смен; вся оплата — зарплата в конверте следующей недели (решение 1). Продолжает класс Town
из S1a. Сейчас game ещё зовёт Economy.parentBonus / answerQuiz / finishMiniGame — на методы Town их
переводят S1d (бонус взрослого, пузырь без вопросов) и S1e (смена и карточка загадки).
Спека прошла 2 критиков (контракт, концепция и тексты): 22 находки учтены.

## ТРЕБОВАНИЕ ТЗ
2.5.4 «валюта начисляется за выполнение заданий… для каждого начисления указываются источник и
сумма»; 2.1 «за один игровой период нельзя купить всё сразу» (лимит смен); 2.8 «дополнительные
возможности… не компенсируют отсутствие обязательных» (игра ради рекорда — после плана);
2.5.12 бонус взрослого; Приложение А шаг 6 «выполнение задания и получение игровой валюты…
с объяснением результата».

## CONTRACT
Методы класса Town (S1a; запись `Town.f` — обозначение метода), пакет ru.finny.pet.domain.town.
R = town.rules, job — запись town.jobs, name — имя жителя job.resident. Внешняя пара «» у строк —
разделитель, как в S1a. NIGHT = «Сейчас ночь — сначала проснёмся». Ночью отказывает ТОЛЬКО
работа (shiftQuote.canPlay = false, finishShift — отказ); загадка и бонус взрослого ночью работают.

### 0. Состояние (единственная правка вне domain/town)
GameState — дописать в конец, с умолчаниями (старое сохранение читается, миграция не нужна):
  val riddles: List<TaskResult> = emptyList(),   // ответы на «Загадку Бори» (отдельно от quizResults 1.3.0)
  val riddleAsked: Boolean = false,              // в этом заказе загадка уже была; сбрасывает конец смены MATCH3
Ответы викторины 1.3.0 (quizResults) на загадки не влияют: town.quiz — своя копия (решение 10).

### 1. Открыта ли работа и сколько она платит
data class ShiftQuote(val jobId: String, val open: Boolean, val paid: Boolean, val canPlay: Boolean,
    val level: Int, val base: Int, val levelBombs: Int, val shiftsLeft: Int, val line: String)
fun Town.shiftQuote(s: GameState, jobId: String): ShiftQuote
- open: работа есть в town.jobs, её место открыто и она сама открыта: opensBy.week → s.period ≥ week
  или s.demo; opensBy.goal → цель с этим id есть в s.achievedGoals (и в демо); место — те же
  правила по places[job.place].opensBy. (Какие открытые работы показывать в срезе 1 — без CHANGE —
  решает S1e.)
- shiftsLeft = max(0, R.shiftsPerWeek − s.shiftsThisPeriod); paid = open и shiftsLeft > 0 (до плана
  тоже: решение 4).
- canPlay = open и !s.asleep и (paid или (s.plan.confirmed и job.game != TAPS)): игра «ради рекорда»
  без монет — только после плана и только у игр со счётом (MATCH3, CHANGE); у TAPS её нет.
- level = последний индекс i, где R.jobLevelShifts[i] ≤ (s.jobShifts[jobId] ?: 0) (0 — первый уровень;
  jobShifts считает только оплачиваемые смены этой работы); base = job.baseByLevel[level];
  levelBombs = R.jobLevelBombs[level] для MATCH3, 0 для TAPS и CHANGE.
- line по порядку: !open «Эта работа пока закрыта»; asleep NIGHT; paid: MATCH3 и CHANGE «База {base} +
  до {R.shiftBonusMax} за результат», TAPS «База {base} за три поручения»; !paid и canPlay «Смены на
  неделе закончились — можно играть ради рекорда»; !paid и TAPS «Смены на неделе закончились — новые
  с новым конвертом»; иначе «Смены на неделе закончились. Ради рекорда — после раскладки».
- Неизвестная работа: open = paid = canPlay = false, level = base = levelBombs = 0, shiftsLeft — как выше.

### 2. Конец смены
fun Town.finishShift(s: GameState, jobId: String, score: Int, bombsUsed: Int): TownResult
Refused по порядку (состояние не меняется): pet «Сначала создай питомца»; asleep NIGHT; !open «Эта
работа пока закрыта»; !canPlay — строка shiftQuote.line; score < 0, или bombsUsed < 0, или
bombsUsed > 0 у TAPS и CHANGE (у кнопочных работ бомб нет), или bombsUsed > levelBombs + s.bombs,
или у TAPS score > job.tasks.size — «Так закончить смену нельзя».
Все величины — по shiftQuote ДО смены (base, levelBombs, paid, уровень оплаты).
Надбавка bonus: MATCH3 — min(R.shiftBonusMax, score / R.shiftScorePerBonus); TAPS — 0;
CHANGE — min(R.shiftBonusMax, score) (score — число верных сдач). Бомбы влияют на надбавку только
через score: bonus ≤ shiftBonusMax при любом числе бомб.
Done:
- paid: total = base + bonus; envelope += LedgerEntry(«Смена: {job.title}», total) (префикс «Смена: »
  читает endWeek S1a); shiftsThisPeriod += 1; jobShifts[jobId] += 1; DiaryLine(s.period, s.day,
  «Заработали {total}: «{job.title}»»). balance, банки и копилка НЕ меняются.
- MATCH3 и CHANGE: records[jobId] = max(records[jobId] ?: 0, score); TAPS records не пишет.
- s.bombs −= max(0, bombsUsed − levelBombs) (сначала тратятся бомбы уровня, потом накопленные).
- MATCH3: riddleAsked = false (следующий заказ — новая загадка).
- показатели питомца не меняются (благодарность и булочка — только анимация UI).
НОВЫЙ = score > (records[jobId] ?: 0) до смены (только MATCH3 и CHANGE).
line:
- paid MATCH3: bonus > 0 «База {base} + {bonus} за булочки. ✉ +{total} — придёт с новым конвертом»
  (MATCH3 — поле «Булочки в ряд»), bonus == 0 «База {base}. ✉ +{total} — придёт с новым конвертом»;
- paid TAPS: «База {base} за три поручения. ✉ +{total} — придёт с новым конвертом» (у TAPS ровно три
  поручения — ContentValidationTest; строка шага 6 демо-пути §11);
- paid CHANGE: «База {base} + {bonus} за сдачу. ✉ +{total} — придёт с новым конвертом» (bonus == 0 —
  как у MATCH3);
- не paid: «Счёт {score}. Это игра ради рекорда.» + (« Новый рекорд!», если НОВЫЙ).
why (paid), по порядку, не больше 3:
  1) уровень вырос этой сменой (level после > level до) → «{name}: новый уровень {level после + 1}!
     База теперь {baseByLevel[level после]}»; иначе, если есть следующий порог → «{name}: {n} из
     {R.jobLevelShifts[level + 1]} смен до уровня {level + 2}» (n — jobShifts после смены; пример
     «Боря: 4 из 6 смен до уровня 2»); на высшем уровне → «{name}: высший уровень мастерства»;
  2) «Карманные приходят каждую неделю, зарплата — когда поработаешь»;
  3) «Новый рекорд!», если НОВЫЙ.
why (не paid): [].
В S1c finishShift оплачиваемой смены дополнительно даст наклейку заказа; в S1b наклеек нет.

### 3. «Загадка Бори» (решение 10)
fun Town.nextQuestion(s: GameState): QuizQuestion?
fun Town.answerQuestion(s: GameState, questionId: String, optionIndex: Int): TownResult
- Доступные — вопросы town.quiz, на которые в s.riddles нет верного ответа. Порядок: сначала ни разу
  не заданные (в порядке town.quiz), потом по возрастанию индекса ПОСЛЕДНЕЙ попытки в s.riddles.
  nextQuestion: s.riddleAsked → null (в этом заказе загадка уже была); иначе первый доступный или
  null, если доступных нет (кнопка исчезает после 15 верных).
- answerQuestion. Refused по порядку: pet «Сначала создай питомца»; riddleAsked «Следующая загадка —
  после смены»; вопроса нет в town.quiz или на него уже есть верный ответ в riddles «Этот вопрос уже
  разобран»; optionIndex вне options «Выбери ответ». Done: riddles += TaskResult(id, correct, 0,
  s.period); riddleAsked = true; верно → bombs += content.rules.quizBombReward. Показатели питомца,
  монеты и quizResults не меняются (quizMoodBonus в game не используется).
  line: верно → «Верно! {explanation}», неверно → «{explanation}». why: верно → [«Бомбочка
  +{quizBombReward} — для поля «{M}»»], неверно → [«Вопрос вернётся позже»]; M — title первой работы
  town.jobs с game == MATCH3.
- Неверно отвеченный вопрос следующим не задаётся, если доступных больше одного (свойство порядка).

### 4. Бонус взрослого (решение 21)
fun Town.parentBonus(s: GameState, reasonIndex: Int): TownResult
Refused: pet «Сначала создай питомца»; reasonIndex вне town.parentBonusReasons «Выберите причину
бонуса»; s.parentBonusesThisPeriod ≥ content.rules.parentBonusPerPeriod «На этой неделе все бонусы
уже начислены — новые будут со следующей недели». Ночью разрешено (действие взрослого).
Done: envelope += LedgerEntry(«Бонус от взрослого: {причина}», content.rules.parentBonusAmount);
parentBonusesThisPeriod += 1; balance не меняется. line «Придёт в новом конверте ребёнка»;
why: k = parentBonusPerPeriod − parentBonusesThisPeriod ПОСЛЕ начисления; k > 0 → [«На этой неделе
можно начислить ещё {k}»], k == 0 → [«Лимит бонусов на эту неделю исчерпан»].

## SCOPE
variant: main
allow:   finny-pet/app/src/main/java/ru/finny/pet/domain/town/   (Town.kt и новый файл ≤ 1)
         finny-pet/app/src/main/java/ru/finny/pet/domain/GameState.kt   (только два поля §0)
protect: базовый + finny-pet/app/src/main/ кроме allow + finny-pet/docs/ + content.json

## ANTI-SCOPE
UI смены, Match3, карточка загадки, раздел взрослого (S1d, S1e); наклейки и JOB-события (S1c);
Economy (answerQuiz, parentBonus, finishMiniGame не меняются — game перестанет их звать в S1d/S1e);
content.json; доки (оркестратор).

## БЮДЖЕТ
≤ 350 вставок в app/src/main; новые файлы ≤ 1; зависимости 0

## ORACLE (test-author до кодера): app/src/test/java/ru/finny/pet/domain/town/ShiftTest.kt
Отказы по порядку, включая ночь, бомбы у кнопочных работ и score TAPS больше числа поручений;
shiftQuote на всех ветках line (в т. ч. ночь, TAPS без игры ради рекорда, неизвестная работа);
лимит 3 оплачиваемые смены в неделю на все работы вместе и сброс в endWeek; база без ловкости (TAPS
и MATCH3 со счётом 0 дают базу); формулы надбавки по game с порогами (29 / 30 / 120 / 1000 очков);
надбавка ≤ shiftBonusMax при любом числе бомб; уровни по порогам 0 / 6 / 15: оплата по уровню ДО
смены (jobShifts 5 → «База 6», why «Боря: новый уровень 2! База теперь 7»), строка прогресса и
высший уровень; бомбы уровня и накопленные; оплачиваемая смена до плана открыта, игра ради рекорда
до плана — отказ, после плана — без монет, рекорд растёт, «Новый рекорд!» в line и why; зарплата —
в envelope, balance и банки не меняются, на planKept не влияет (endWeek); строки line/why дословно;
дневник; открытие работ (неделя, демо, мечта-ключ, место); загадка: порядок очереди, неверный не
повторяется сразу, одна загадка за заказ (riddleAsked, сброс концом смены MATCH3 — не TAPS),
ответы quizResults 1.3.0 не снимают загадки, верный даёт бомбу и не меняет показатели и монеты,
после 15 верных — null; бонус взрослого — в envelope, лимит, ночью работает, строки.
ContentValidationTest (дописать): R.jobLevelBombs.size == R.jobLevelShifts.size.
Стоп-слова и род — на всех строках выдачи (списки ContentValidationTest).

## ACCEPTANCE (из finny-pet/, оркестратор) — как в S1a, п. 1–7; п. 2 — tests = 406 + <оракул>;
  п. 8 — ≤ 350 вставок;
  9. мутанты: надбавка без потолка; неоплачиваемая смена пишет в envelope; заработок в balance;
     бомбы уровня не выдаются; неверный вопрос задаётся снова первым; оплата по уровню после смены;
     riddleAsked не сбрасывается сменой; ночью смена проходит; quizResults снимают загадку
```

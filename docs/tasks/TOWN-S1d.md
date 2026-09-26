```
TASK: TOWN-S1d — game на «Городке»: комната, банки, копилка, ночь и итог недели, улица, лавки, работа
EPIC: TOWN-S1 (docs/tasks/TOWN-S1.md)
BASE: <sha коммита оракула> (для кодеров и ревьювера)
BRANCH: feat/town

## КОНТЕКСТ
Движок «Городка» готов (S1a кошелёк и неделя, S1b смены, S1c события) и покрыт тестами, но game
играет по правилам 1.3.0. Задача переводит game целиком на Town: главный экран — комната (одобренный
MockHome), улица, две лавки с кассой (MockShop), работа (MockJob), раскладка по банкам, копилка, сон,
ночь, итог недели, доска событий. Переход атомарный (план на банках + покупка на Economy.buy уводят
запас в минус), поэтому бывшие S1d и S1e — одна задача: КОДЕР 1 (VM целиком, дом, неделя, заглушки
экранов города) → КОДЕР 2 (экраны города по API VM из §B.4) → одна приёмка. classic не трогаем.
Спека прошла 3 критиков (реализуемость, UX и концепция, тексты): 42 находки учтены.

## ТРЕБОВАНИЕ ТЗ
2.5.1, 2.5.3, 2.5.4–2.5.10, 3.1.2 (портрет от 360 dp), 3.5, 3.6 (мишени ≥ 48 dp, текст ≥ 16 sp, цвет не
единственный признак, анимации и звук отключаемы, подтверждение действий).

## ОБЩИЕ ПРАВИЛА ТЕКСТА
Строки ниже — дословно (внешняя пара «» — разделитель; {…} — подстановки). Числа экономики — только
подстановками из content (planStep, parentBonusAmount, shiftsPerWeek, PlanTweak.n …), в коде литералов
чисел экономики нет. Без рода ребёнка и питомца (решение 24), без стоп-слов §9.2, без дней недели
(«понедельник», «воскресенье» — только слова документа). Мечта в строке — «мечта «{title}»» (без
согласования рода и падежа). LINE и реплики переносятся до 3 строк, maxLines = 1 у фраз нет.

## CONTRACT

### A. Домен (кодер 1; оракул PetTalkTest)
- content.json → town.chatter (заводит оркестратор до оракула): {"needFood", "needCare", "sad", "calm"}
  — списки реплик по тапу, каждая кончается на «.», «!» или «?»; реплики нужды говорят о неделе, а не о
  голоде тела. TownContent: поле chatter: TownChatter(needFood, needCare, sad, calm: List<String>).
- data class PetLine(val text: String, val need: Need? = null, val shop: String? = null)
  fun Town.petLine(s: GameState, tap: Int): PetLine — чистая функция состояния и счётчика тапов, по
  порядку: pet == null → PetLine(""); план подтверждён и нет покупки FOOD недели → need = FOOD,
  text = needFood[tap % size] + HINT(FOOD); иначе план подтверждён и нет CARE → need = CARE,
  needCare[…] + HINT(CARE); иначе Economy.face(pet) == SAD → sad[tap % size], need = null; иначе
  calm[tap % size]. {pet} → имя. HINT(n) = « {T} {shop.at} — {цена}» по самой низкой seenPrices недели
  (period == s.period) среди товаров need n (равенство — первый по town.items/items), shop = id этой
  лавки; ничего не видели → HINT = «» и shop = null. Питомец не ведёт на план, копилку, события, итог.

### B. ViewModel, навигация, общие виджеты (кодер 1)
1. Все денежные и временные действия — через Town. Из game НЕ вызываются Economy.confirmPlan, buy,
   deposit, withdraw, endPeriod, answerQuiz, parentBonus, finishMiniGame, answerChoice, answerNumber,
   availableTasks, miniGameLock, faceReason, withdrawPreview, goalProgressMessage (греп). Можно:
   setPlan, chooseGoal, customGoal, stageIndex, stageTitle, face, goalEta, newGame, resetProfile,
   deleteProfile, coins, weeks.
2. Загрузка: state = Migration.migrate(store.load()); затем Town.tick. createPet, createTestProfile,
   resetProfile: seed = System.nanoTime() при seed == 0, затем migrate и tick. Seed раунда Match3 —
   System.nanoTime() (исключение §9.2 №1). Других обращений ко времени в game нет.
3. Действие Town: Done → commit + эффекты §F + LINE(line, why); Refused → LINE(line) и звук FAIL.
   Пришли события (outcome.arrived) → после строки действия LINE(intro первого пришедшего), а его
   id — в список «новое» Board. LINE — пузырь поверх экрана: портрет питомца 48 dp, строка (перенос до
   3 строк), [Почему?] раскрывает why (до 3 строк), закрывается тапом; пустая line — пузыря нет.
   FeedbackOverlay, nextStep, автоговорение комнаты, вопросы в пузыре (askQuestion, answerBubble),
   экраны Tasks/Task, Plan, Shop, Savings, WeekEnd 1.3.0 и кнопка «Игра» удаляются.
4. API VM для экранов города (реализует кодер 1, зовёт кодер 2): openPlace(placeId) = Town.visit →
   commit → LINE для arrived → navigate по шаблону места (HOME → Room, SHOP/JOB → Place(placeId));
   lastShop — var VM (не сохраняется, по умолчанию market), openPlace лавки его обновляет;
   quote(itemId, shopId): Quote; buy(itemId, shopId, source); makeGoal(itemId); pass(eventId);
   startEvent(eventId); chooseGoal(goalId); shiftQuote(jobId); startRound(jobId); matchSwap(a, b);
   matchBomb(cell); tapTask(i); finishRound(); nextQuestion(); answerRiddle(questionId, i);
   eventsAt(placeId): List<EventDef> — активные события с place == placeId или с триггером
   Enter(placeId); ordersAt(placeId). Любой переход в место — только через openPlace (seenPrices и
   ENTER-триггеры, §11 шаг 5).
5. Экраны: Title, Intro, CreatePet, Room, Jars, Savings, Arrange, Night, WeekEnd, Street, Place(placeId),
   Round(jobId), Board, Progress, Glossary, Parent. Кодер 1 заводит все Screen и заглушки StreetScreen,
   PlaceScreen, RoundScreen, BoardScreen (сборка зелёная после кодера 1) и отвязывает
   MiniGameScreen.kt от пузыря и вопросов.
6. Навигация — стек: back() возвращает на предыдущий экран; Room — дно стека, «Домой» (⌂) очищает
   стек до Room; на Room системное «Назад» закрывает приложение без диалога (§9.2 №15). Round: back()
   и ⌂ = «Закончить» (finishRound → итог). ПОКА s.asleep: стартовый экран, «Домой» и back() ведут в
   Night (с WeekEnd — в Night); Room при asleep не показывается (§9.2 №19).
7. Место события: place == home → Room (intro в пузыре питомца); place == null → Board (строка с intro,
   без [Перейти]); иначе openPlace(place). Card: с eventId → «!» + title + место; без → без «!».
8. Ориентация — portrait: новый файл finny-pet/app/src/game/AndroidManifest.xml с
   <activity android:name=".MainActivity" android:screenOrientation="portrait" tools:node="merge"/>;
   main-манифест не трогается (classic свободен).
9. Картинки: itemRes(id): Int? — drawable только для id с файлом item_*, иначе null → эмодзи товара
   (фолбэк на палатку удалить); goalRes: goal_* для id мечт с файлом, item:{x} → вещь x, demo_goal и
   custom_* → goal_custom, иначе эмодзи. Житель: Image(PetSprites.id(look.species, look.color, 0,
   "happy")), аксессуар не рисуется. «Житель недели» — последний в town.residents с arrivesWeek ≤
   s.period.
10. Измеримость (WORKFLOW №14, №17): на корне GameApp semantics { testTagsAsResourceId = true };
    testTag у рядов — hud1, hud2, town_card, room, bottom_row, jars, savings, arrange, night, weekend,
    street, shop_tabs, shelf, pay_panel, job_order, board, job_row, job_result, events, line, cell_{r}_{c};
    в debug — зонд обрезки как в TownMock (MockText): тексты новых экранов идут через общий композабл,
    который по onTextLayout.hasVisualOverflow пишет обрезку в общий список; невидимый узел testTag
    «overflow» с contentDescription «Обрезано: N — {тексты}» (в release узла нет).

### C. Комната (кодер 1; раскладка и размеры — MockHome, замер TOWN-S0c)
HUD-1 56 dp: кошелёк (ui_purse + balance; тап — лист «Нужное N · Хочу N · Запас N» после плана или
«Не разложено N» до плана, и «✉ +N», если конверт не пуст), копилка (goalRes мечты + «{sv}/{price}» и
«≈N✉» при известном сроке, при шрифте > 1,15 «≈N✉» второй строкой 14 sp; без мечты — ui_piggy + «{sv}» и
под ним «Выбери мечту» 14 sp, при шрифте > 1,15 — только значок, слово в contentDescription), «?» → Intro,
🔑 → Parent. HUD-2 48 dp: три StatChip (значок + число + полоса; слово — в contentDescription).
«В городке» 56 dp — Town.card (§B.7).
Комната (растягивается, как макет): окно 112 × 80 (житель недели; «!» при активном событии с place на
улице; тап → Street), полка 152 × 72 (до плана — три пустые банки и «Разложи»; после — Нужное, Хочу,
Запас числами; → Jars), свинка 64 × 64 (→ Savings), холодильник 64 × 96 «Список» (→ панель §D.4), сундук
64 × 48 (→ Arrange), словарик 48 × 48 (→ Glossary), дверь 64 × 136 «Улица» (→ Street), ящик 48 × 48 «✉ +N»
(→ панель §D.3; пульсирует только конверт, §9.2 №17), кровать 128 × 64 «Сон» (§D.6), питомец ≈ 120
(PetSprite; contentDescription «{pet}. {стадия}. Нажми — что на уме»; тап → petLine: пузырь над питомцем;
shop != null → кнопка «{shop.title}» → openPlace(shop.place); need != null и shop == null → «На улицу» →
Street). Вещи мест: стена (spot_2) — на стене над кроватью; стол (spot_3) — под холодильником; пол
spot_1 — слева у пола, spot_4 — слева от кровати, spot_5 — справа от кровати, spot_6 — справа у пола;
вещь — itemRes или эмодзи ≈ 40 dp, без касания; пустые места не рисуются. Нижний ряд 5 × 72 dp:
«Банки», «Лавки» (openPlace(lastShop)), «Копилка», «События» (Board), «Дневник» (Progress) — ui_jar,
ui_bag, ui_piggy, ui_book, ui_trophy; подпись 14 sp, при шрифте > 1,15 — только значок.

### D. Экраны и панели дома (кодер 1)
1. Jars. До плана — «Разложи монеты»: UNALLOC = s.balance − s.plan.total. Три банки «Нужное», «Хочу»,
   «В копилку» (ui_jar + ui_lid_*, ≥ 64 dp): тап +planStep (Economy.setPlan), кнопка «−» у банки −planStep;
   при UNALLOC < planStep тап неактивен и подпись «Больше, чем есть, положить нельзя». Строка
   «Не разложено: {UNALLOC} — будет запасом на всякий случай». Под «Нужным» — список первого живого
   события с непустым params.list: все цены видны → «Список: {t1} ~{p1} + {t2} ~{p2} = {сумма}», иначе
   «Список: {t1} ~{p1} + {t2} ? — загляни в лавки» (цены — seenPrices); при M == 0 — «Еда и уход нужны
   каждую неделю». HUD-1 на экране (цель частиц «piggy»). [Готово] → подтверждение «Готово?» / «План не
   меняется до нового конверта — потом сравним, как вышло» / [Да] [Ещё подумаю] → Town.confirmPlan
   (монеты «jar_save» → «piggy»).
   После плана — «Банки: план и факт»: «Нужное: план {M}, потрачено {fM}», «Хочу: план {O}, потрачено
   {fO}», «Копилка: план {S}, отложено {max(fs, 0)}» — значок, числа, полоса; «✓» у строки, где план
   держится, у остальных знака нет (не табель). «Запас на всякий случай: {r}». Кнопки «Из запаса в
   «Нужное» +{planStep}» и «Из «Хочу» в «Нужное» +{planStep}» (видны, если источник ≥ planStep) →
   подтверждение с transferPreview → Town.transfer.
2. Savings «Копилка»: мечта — картинка, «{sv} / {price}, ещё {left}, ≈ {eta} ✉» (без срока — без «≈…»);
   витрина town.goals («{title} — {price}») и «Своя мечта» (customGoal) → Economy.chooseGoal; после
   плана «В копилку {n} из запаса» и «В копилку {n} из «Хочу»» (n = planStep; видны, если источник ≥ n)
   → подтверждение с depositPreview → Town.deposit; до плана «Взять {n} из копилки» (sv ≥ n) →
   подтверждение с withdrawPreview → Town.withdraw; sv ≥ price → «Получить мечту» → «Получить мечту
   «{title}»?» → Town.achieveGoal (конфетти). Ниже — достигнутые мечты.
3. Панель конверта: «Придёт с новым конвертом: +{сумма}», строки envelope «{text} +{amount}»;
   [Почему?] → «Зарплату приносит почтальон вместе с карманными — сначала работа, потом зарплата».
4. Панель холодильника «Список нужного»: товары того же params.list — «{T} — {цена} {shop.at}» по
   seenPrices или «{T} — ?»; ниже — s.notes.
5. Arrange «Обустроить комнату»: сетка 3 × 2 мест town.spots по 64 dp с подписью типа («пол»,
   «стена», «стол»); стартовые — с вещью, без касания; тап по месту → список вещей owned с тем же
   slot и «Убрать в сундук» → Town.place; [Готово].
6. Кровать: s.asleep — недостижима (Room при asleep не показан); до плана → Jars; день < daysPerWeek →
   Town.sleep → Night; последний день → подтверждение «Закончить неделю?» + weekEndPreview; нужды не
   покрыты — [{shop.title} для нужды] (petLine.shop, иначе «На улицу») и [Всё равно спать]; покрыты —
   [Спать] [Отмена] → Town.endWeek → WeekEnd.
7. Night: evening-фон и луна, питомец спит (кадр blink), строка sleep, результаты событий ночи
   (EventResult.line списком, без знаков вердикта и цвета), «Можно закрыть игру — всё сохранено»,
   [Проснуться] → Town.wake → Room и LINE утра (новый конверт: монеты «mail» → «coins»). В демо при
   plan.confirmed — [Сразу к итогу недели] → подтверждение «Закончить неделю?» + weekEndPreview,
   [Закончить неделю] [Отмена] → Town.endWeek → WeekEnd.
8. WeekEnd «Итог недели»: три банки как Jars после плана по числам итога; полученные штампы значком и
   словом («Нужное куплено», «По плану», «Отложено»), неполученных нет; «Рост +{score}» только при
   score > 0, стадия; строка итога и [Почему?]; «События недели» — EventResult.line итога списком (без
   вердикта и цвета); затем «Что поменяем в новом плане?» — кнопки planTweaks («Нужное +{n}», «Хочу
   +{n}», «В копилку +{n}», «Запас +{n}») и «Как было» → Town.chooseTweak; затем «Неделя позади! Всё
   сохранено» с [Выйти] (закрыть приложение) и [Играть дальше] одного размера (§9.2 №20) → Night.
9. Progress (до «Дневника»): рост и стадии, итоги недель, журнал ledger и блок «Придёт с новым
   конвертом» из envelope; «Справка» → Glossary.
10. Parent: бонус — причины town.parentBonusReasons, подтверждение «Начислить {parentBonusAmount}
    монет?» / «За: «{причина}»» / «Придёт в новом конверте ребёнка» → Town.parentBonus; тестовый
    профиль, сброс, удаление — как сейчас + migrate и tick.
11. Intro (§11 шаг 1): страница 1 «Помоги питомцу вырасти: заботься о нём и копи на мечту»; страница 2
    три карточки со значками банок: «Нужное — еда и мыло», «Хочу — то, что радует», «В копилку — на
    мечту»; страница 3 «Каждую неделю почтальон приносит конверт. Заработанное придёт в следующем».

### E. Улица, лавки, работа, события (кодер 2; раскладка — MockShop, MockJob)
Во всех экранах E: HUD-1 = ⌂, кошелёк, копилка, «?» (без 🔑).
1. Street «Улица»: ряд мест (LazyRow): Дом, Рынок у реки, Лавка «У Фомы», Пекарня; мишени ≥ 64 dp;
   у места — картинка жителя и имя, «!» при eventsAt(place) не пустом; тап → openPlace. Места мечт
   (places с opensBy.goal) — калитка «🔒 Мечта «{title}»: {sv} / {price}»; тап → подтверждение «Копить на
   мечту «{title}»?» → chooseGoal (если это уже текущая мечта — «Копим на «{title}»: {sv} / {price}» без
   подтверждения). Места и работы среза 2 (мастерская, дом Тоши, касса, курьер) не показываются.
2. Place SHOP (market, foma). HUD-2: после плана — три отделения как MockShopJob (крышка банки + число,
   слово 14 sp под числом, при шрифте > 1,15 — только в contentDescription) и свёрнутые показатели; до
   плана — «Не разложено {UNALLOC}». Переключатель «Рынок у реки» / «У Фомы» → openPlace. Полка — shelf:
   карточка ≥ 64 dp (itemRes или эмодзи, название, цена или «{was}→{цена}» и «такая распродажа бывает»,
   «🍎 нужно» / «🎈 хочу», эффект значком и числом). У кассы — питомец 64 dp (цель «pet», проигрывает
   PetAction). Тап по товару → касса (нижний лист, pay_panel): «{T} · {цена} · {метка} · {эффект}»,
   quote.line, quote.note, варианты: основные кнопками, под каждой — preview[0]; остальные под «Ещё ▾».
   PAY из родной банки → buy сразу (касса с preview — подтверждение); PAY из другого источника
   (RESERVE для нужного, TRANSFER_*, SAVINGS, WANT/RESERVE для непредвиденного, если не родной) →
   подтверждение: заголовок label, строки preview, [Да] [Отмена] → buy. CHEAPER → касса этого товара;
   WAIT → закрыть; MAKE_GOAL → makeGoal. До плана — строка quote «Покупки — после того как разложишь
   монеты». События места (eventsAt): карточка (title, intro) и [Пройти мимо], если у события есть исход
   Skip → pass. На рынке — карточка заказа из ordersAt(market) прямо в лавке (как §E.3), смена Марты →
   Round(job_market).
3. Place JOB (пекарня) и карточка заказа: житель, intro заказа, shiftQuote.line, жетоны «Смены на неделе:
   ●●○» (заполнено shiftsThisPeriod из shiftsPerWeek; contentDescription «Смены: {n} из {shiftsPerWeek}»),
   [Начать смену] (canPlay) → startRound; у пекарни — «Загадка Бори: отгадаешь — бомбочка» [Ответить]
   [Нет, спасибо] одного размера, если nextQuestion != null → вопрос с вариантами → answerRiddle → LINE.
4. Round: HUD-1 (⌂ = «Закончить»), HUD-2 (свёрнутые показатели и чип «✉ +{envelope}», цель «mail»);
   MATCH3 — поле Match3.newGame(job.board.w, job.board.h, demo ? demoMoves : moves, s.bombs +
   levelBombs, seed) из нынешнего MiniGameScreen (управление и каскады), а колонка его HUD (заголовок,
   строка монет 1.3.0, «Вопрос → бомбочка») удаляется; job_row 56 dp: жетоны · «Счёт {score} · Ходы {m}» ·
   [💣 ×{n}] 48 dp · [Закончить]; клетка ≥ 48 dp при 360 × 640 и шрифте 1,3. TAPS — три кнопки job.tasks
   (тап — ✓, счёт = число отмеченных) и [Закончить]. «Закончить» → finishRound → Town.finishShift → итог
   (job_result): житель, «Спасибо за помощь!», строка finishShift и why, [Готово] → место работы
   (монеты «job_result» → «mail»).
5. Board «События»: activeEvents — title, место, intro; [Перейти] по §B.7 (у place == null — без кнопки);
   orders — [К работе] и жетоны; в демо — «Все события (демо)» из demoBoard с [Начать] → startEvent →
   LINE(intro) и переход по §B.7.

### F. Эффекты, звук, доступность
| действие | эффект | цели (testTag / particleTarget) |
| покупка | PetAction по need, COIN, монеты | «coins» (HUD-1) → «pet» (питомец у кассы) |
| взнос / снятие | COIN, монеты | «coins» → «piggy» / обратно (HUD-1) |
| confirmPlan | SUCCESS, монеты S | «jar_save» → «piggy» |
| смена | SUCCESS, монеты оплаты | «job_result» → «mail» (HUD-2 раунда) |
| утро нового конверта | COIN, монеты | «mail» → «coins» (HUD-1 комнаты) |
| мечта | FANFARE, конфетти | «center» |
| сон | SLEEP | — |
Эффект запускается только на экране, где есть обе цели. Без анимаций — всё сразу, пульса нет. Все
мишени ≥ 48 dp (дверь, окно, товары, банки, места улицы и «Обустроить» ≥ 64 dp), текст ≥ 16 sp, кроме
списка 14 sp из UX_ACCESSIBILITY.md (+ «Выбери мечту», «≈N✉», подписи отделений HUD-2); у мишеней
contentDescription и Role.Button; вердикт события цветом не показывается; «!» — только событие.

## SCOPE
кодер 1: game/java/ru/finny/pet/game/ (GameApp.kt, GameViewModel.kt, screens/ — экраны дома, заглушки
  экранов города, MiniGameScreen.kt — только отвязка от пузыря; ui/ — HUD, LINE, зонд, itemRes/goalRes),
  finny-pet/app/src/game/AndroidManifest.xml (новый), domain/town/ (petLine, PetLine, TownChatter);
кодер 2: game/java/ru/finny/pet/game/screens/ StreetScreen, PlaceScreen, RoundScreen, BoardScreen
  (файлы-заглушки кодера 1) и MiniGameScreen.kt; VM и ui/ — только чтение.
protect: базовый + finny-pet/app/src/main/ кроме domain/town + classic/ + content.json + finny-pet/docs/ +
  game/mock/

## ANTI-SCOPE
Новые ассеты и генераторы; производительность фонов и save вне главного потока (S1f); «Дневник» и
раздел взрослого среза 2; сцены среза 2; альбомная раскладка; classic; Economy; правка тестов.

## БЮДЖЕТ
≤ 4000 вставок в app/src (game + domain/town); новые файлы ≤ 16; зависимости 0.

## ORACLE (test-author до кодера 1): app/src/test/java/ru/finny/pet/domain/town/PetTalkTest.kt
petLine: чистота; цикл по tap; до плана нужды не называет; FOOD раньше CARE; SAD → sad (и до плана) без
нужды и лавки; HINT только по seenPrices этой недели (до visit — без цены и лавки), лавка — та, что в
строке; после покупки нужды — calm или sad; строки без стоп-слов и рода (списки ContentValidationTest +
голодный, голоден, сыт, рад, грустный, чистый, грязный), без «скучал», «где ты был», «хочу-хочу».
ContentValidationTest (дописать): town.chatter — в каждом списке ≥ 2 строк, каждая кончается на .!?,
без {…} кроме {pet}, без «пока не».

## ACCEPTANCE (из finny-pet/, оркестратор)
  1. testClassicDebugUnitTest testGameDebugUnitTest -> exit 0; tests = <после S1c> + <оракул>, skipped 0
  2. assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0; lint 0 ошибок, ≤ 5 / 6
  3. git diff --name-only BASE -> только allow; app/src/test, gradle, main-манифест — пусто
  4. grep -rnE 'economy\.(confirmPlan|buy|deposit|withdraw|endPeriod|answerQuiz|parentBonus|finishMiniGame|answerChoice|answerNumber|availableTasks|miniGameLock|faceReason|withdrawPreview|goalProgressMessage)\(' app/src/game -> пусто
  5. grep -rnE 'nextStep|askQuestion|answerBubble|repeatOnLifecycle|currentTimeMillis|понедельник|воскресенье|помощник!' app/src/game -> пусто;
     grep -c 'nanoTime' app/src/game -> только сиды (VM)
  6. grep 'screenOrientation="portrait"' app/src/game/AndroidManifest.xml -> 1; aapt2 dump badging game-debug: orientation portrait, classic — нет
  7. ЖИВАЯ ПРОВЕРКА оркестратора (эмулятор 360 × 640 dp, шрифт 1,0 и 1,3; S23 360 × 780; tools/ui_measure.py,
     узел overflow «Обрезано: 0»): Room (с мечтой и без, с пузырём), Jars до и после плана, Savings, Arrange,
     Night, WeekEnd, Street, Place рынок и Фома, касса с нехваткой и «Ещё», подтверждение не-родного
     источника, заказ, раунд 6 × 6 (клетка ≥ 48 dp), итог смены, Board, LINE самой длинной строки — ряды не
     шире экрана, мишени ≥ 48 dp; потоки: демо-путь §11 шаги 1–12; закрыть приложение ночью → открыть →
     Night → «Проснуться»; Room → «Лавки» → «Назад» = Room; П1, П3, Пк1 проходятся (карточка, intro,
     исход); скриншоты — finny-pet/screenshots/town/ листами с подписями
```

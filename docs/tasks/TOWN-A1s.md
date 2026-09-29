# TOWN-A1s — витрина лавки: товары на досках, ценник на ниточке, касса-ценник, плакат события над товаром, продавец на полу, «У Фомы» страницами по 9

```
TASK: TOWN-A1s — в UI `game` лавка-витрина вместо сетки карточек (решение № 39 а): доски по 3 товара, последняя доска
      страницы — прилавок; ценник на ниточке (монета, цена 19 sp, крышка отдела); отдел с новой доски; «У Фомы» по 9 на
      странице, «Ещё N ▶» / «◀ Назад»; касса-ценник по нажатию; плакат события над его товаром; продавец крупно на полу
      (Марта — облачко заказа, ящики, «Начать смену»); чистая функция раскладки `Showcase` в `domain/town`
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md: строка A1s :44, бюджет :70-72, правила эпика :74-…); макеты
      finny-pet/screenshots/town/a1s_mock_1…5 (одобрены владельцем), их источник — mock.py (scratch сессии 15, ниже)
BASE: 470db8b (код = выпуск 1.4.1, тег v1.4.1). HEAD feat/town на сверке — 50b77a7: сверх 470db8b только доки и README
      (GAME_CONCEPT §18 № 120–128, GATE_QUEUE раздел 12, HANDOFF, README.md, finny-pet/README.md), `git diff --stat 470db8b
      50b77a7 -- finny-pet/app tools` пуст. Номера строк кода — 470db8b, строк доков (ДОКИ) — 50b77a7 (те же, что на 673bb3d).
      Кодер — от коммита оракула test-author поверх коммита этой спеки (`git diff --stat 470db8b HEAD -- finny-pet/app/src/main
      finny-pet/app/src/game` перед спавном кодера пуст)
BRANCH: feat/town, основное дерево; test-author → кодер (один) → приёмка → живая → доки → ревьювер
ЗАВИСИТ ОТ: № 39 а (docs/GAME_CONCEPT.md §18, строка 39; §1814 SHOP); рекомендации R1–R11 оркестратора — владелец: «делай
      всё по рекомендации, дальше посмотрю по скринам»; A1e2 слит (картинки 23 товаров, `posterRes`); выпуск 1.4.2 — пуш
      только после «да» владельца по листам finny-pet/screenshots/town/
```

## КОНТЕКСТ
Владелец 2026-09-29: «я же писал, чтобы магазин сделали вместо списка — предметы на полках». Сейчас под HUD и вкладками —
сетка белых карточек `ItemCard` (FlowRow 104 dp), над ней белая карточка события, под ней карточка заказа Марты (её ищут
прокруткой — BACKLOG п. 13), по нажатию — `PayPanel`. A1s меняет только экран лавки: HUD-1, HUD-2 (`JarsHud`) и вкладки
не трогаются (R9), домен, ViewModel, `content.json`, ресурсы и манифест — тоже (R11); новый файл — чистая функция
раскладки в `domain/town` (R4), её оракул пишет test-author.

### Что решено (R1–R11, круг критиков 1) и как это сделано в прототипе
- **R1** доски, ценники, прилавок, стенка «У Фомы», плакатик, облачка, ящики, стрелки пейджера — рисование Compose по
  формулам `mock.py` (`plastic()`: тень, тёмная «губа», блик; `board()`, `tag()`, `poster()`, `bubble()`, `crates()`). Новых
  ассетов 0. Генератора досок в `finny-pet/tools/art/` нет (`uiprops.py` — крышки и монета, `props.py` — товары и плакат,
  `place.py` — фоны) — Blender-генератор `shopprops.py` в ANTI-SCOPE и вопросом № 110.
- **R2** продавец — `ResidentPic` 170 dp (фигура ≈ 154 dp у Марты, ≈ 134 dp у Фомы) у левого края прилавка, ступни у низа
  прилавка; без семантики (иначе его коробка 170 dp срезает узел первого товара прилавка до 41 dp — замер прототипа).
  Облачко продавца при шрифте 1,0 (`fontScale ≤ 1`) — в пустом левом слоте доски над прилавком, над головой (макет 1); при
  любом масштабе > 1,0 (и при 1,15 — стандартном шаге Android) — на полу справа от ступней, до 240 dp (в слоте 124 dp при
  1,15 слова рвались и облачко росло на ценник доски выше). Ряд пола (облачко, ящики, кнопки) поднят на 10 dp на губу
  прилавка — трёхстрочное облачко Фомы целиком над низом окна 360 × 640 при 1,0. В видимом тексте облачка пробел после
  однобуквенного слова — неразрывный (escape `\u00A0`: «Свежее / с утра!», «в лавку!» не рвутся); описание TalkBack —
  прежний текст. Марта с заказом: облачко «{заголовок заказа}» + монета «+{плата}» (`order.title` = «Помочь Марте»,
  `shiftQuote.base` = 6; TalkBack — заголовок, вступление, строка платы, смены), на полу — ящики и `GameButton` с ТЕКСТОМ
  «Начать смену» (`startRound`), только при `canPlay`. После лимита смен заказа нет (№ 62 б) — облачко говорит реплику жителя
  «Свежее с утра! Заглядывай.» (content), ящиков и кнопки нет. «У Фомы» — реплика `lines[0]` «Добро пожаловать в лавку!»
  (content) только на первой странице (макет 3 — без облачка). Заказ — один: первый заказ места, по жителю не
  фильтруется (`ordersAt(place.id).firstOrNull()`; живой заказ лавки — только у Марты); заказ чужого жителя встал бы в
  облачко продавца (ANTI-SCOPE).
- **R3** «У Фомы» по 9: кнопки «Ещё N» (N — товаров на следующей странице: в демо и на неделе 5 — 9, на неделе 1 — 6) и
  «Назад», `GameButton` PAPER 48 dp на полу; стрелка — малиновый треугольник `G.magenta` ≈ 13 × 18 dp (макеты 2–3): у «Ещё» —
  после слов, у «Назад» — перед ними (иконка `GameButton` стоит первой; кнопка «Ещё» — в `LayoutDirection.Rtl`, слота справа в
  `Widgets.kt` нет). Видимый текст без глифов ▶ / ◀ (TalkBack читает «Ещё 9» / «Назад»). Страница — `remember(place.id)`
  (сброс при смене лавки), номер зажат на последнюю страницу (купили последний keep-товар страницы); смена страницы
  прокручивает витрину к началу; «!» на кнопке, если на той странице товар события (робот C1 и змей C3 стоят на стр. 2), и
  заголовок этого события — `stateDescription` кнопки («Ещё 9, Распродажа робота»; слова из content, нового текста нет).
- **R4** `Showcase.pages(shelf)` — страница → доска → 3 слота; `Showcase.eventItem(e)` — товар события (§ 1, ORACLE).
- **R5** касса-ценник по макету 4: название 26 sp и ✕ 48 dp в строке заголовка — строка закреплена, под ней прокручивается
  тело кассы (`pay_body`; WORKFLOW № 38: выход — в строке заголовка); чип отдела (крышка + «Нужное» / «Хочу» / «Запас» — слова
  HUD-2); цена 34 sp с монетой (распродажа — старая зачёркнута); товар на подставке (`ShelfPic` 80 dp); облачко «+N {стат}» —
  ячейка с весом (меряется после товара и питомца), питомец 80 dp (цель частиц «pet») сохраняет размер и при 1,3; слово стата —
  только при 1,0, выше — значок и «+N», слова — в описании облачка (`effectWords()`; правило `bigFont`: слова уступают
  значкам); строка `q.line`, `q.note`, вступление события; «Разложить» до плана; варианты `q.options`
  с прежней логикой `onOption`/`Ask`, у варианта значок (CHEAPER — картинка товара, PAY — крышка источника, копилка —
  `ui_piggy`); превью — только у варианта, который действует сразу (родная банка, «Сделать мечтой»), у остальных превью
  показывает вопрос `Ask`; «Ещё ▾»; «Пройти мимо» события. **Ряд монет «есть / не хватает» не делаем** — при цене 60
  (палатка) сетка по 10 даёт 6 рядов (+≈ 100 dp), строка `q.line` говорит то же числами. Затемнение под вкладками, касание
  по нему и «Назад» (`BackHandler`) закрывают кассу; пока касса открыта, витрина под ней вне дерева TalkBack
  (`clearAndSetSemantics {}` после `verticalScroll`: узлы полки, облачко, плакаты и пейджер под затемнением не читаются и не
  нажимаются двойным касанием; HUD и вкладки остаются — R9). Ниточка от доски к кассе и пунктир на месте товара (макет 4) — нет.
- **R6** плакат события — над слотом его товара на той доске, где товар стоит: жёлтый «пластик» с белой кромкой, хвостик к
  центру слота, «!» в углу. Текст: слова в «ёлочках» из вступления (реклама: Пк3 — «Супер-корм!» тёмно-малиновым и «Все
  питомцы в восторге!» лиловым; P3 — «Привоз задержался — корм подорожал»), иначе у события с setup PRICE/OFFER — заголовок
  (C1 — «Распродажа робота»), иначе первое предложение вступления (C3 — «В витрине новинка — воздушный змей за 30.»:
  заголовок «Мечта почти твоя» над змеем назвал бы змея мечтой, а урок C3 — пройти мимо). Ширина 250 dp (два плаката на
  доске — по половине). Касание — касса товара (там вступление и «Пройти мимо», если у события есть исход Skip). TalkBack —
  «{заголовок}. {вступление}», Role.Button. Картинка плаката 64 dp (`posterRes`, `poster_food_super.webp`, решение № 96 б)
  в лавке больше не показывается — макет 1 рисует плакат без картинки (№ 115).
- **R7** распродажа — на ценнике старая цена 16 sp (#5C426C) с розовой косой чертой → новая #BE003A, кромка ценника розовая;
  в кассе то же. Строка «такая распродажа бывает» из кода убрана: в кассе робота — вступление C1 «Робот! Было 40 — стало 25.
  Такая распродажа бывает.» (content). Закрывает BACKLOG п. 25 (перенос посреди слова на карточке).
- **R8** один узел на товар: `clickable(Role.Button)` + `clearAndSetSemantics { contentDescription = desc }`, `desc` —
  дословно как у `ItemCard` (`listOf(item.title, "${price} монет", item.tag().drop(3), item.effectWords())…`).
- **R10** при 1,15 и 1,3 витрина прокручивается под HUD и вкладками; на 46 кадрах прототипа «Обрезано: 0», кроме трёх при
  1,15 — «Обрезано: 1 — Выбери мечту»: подпись HUD-1 (R9 — не трогаем; код HUD-1 тот же, что в BASE) — BACKLOG (ДОКИ, новый пункт 4).
- **Товар на доске** (круг критиков 1) — `ShelfPic`: картинка обрезана по непрозрачной части (альфа ≥ 40, как `sprite()`
  mock.py — у `item_fun_balloon` полупрозрачная подложка), вписана в 66 × 60 dp низом к доске, под ней мягкая тень-овал
  (0,84 ширины, `place()` mock.py); рамка считается один раз на картинку (`readPixels`, кэш по id ресурса). Общий `Pic` не
  меняется (другие экраны), ресурсы не обрезаются. Без картинки — эмодзи (`Pic`), как было.
- **Задник рынка** (круг критиков 1) — фон места неподвижен под прокруткой, а в `bg_market_port` нарисован прилавок с миской
  корма: при шрифте > 1,0 (облачко ушло из слота) и при прокрутке миска вставала в пустой слот доски — «товар без ценника»,
  нажатие — ничего. Витрина рынка рисует за 2-й доской и прилавком светло-мятную стенку (`lerp(front, White, 0.55)`,
  19..341 dp), ряд 1 — на фоне неба, как в макете 1; перерендер фона без прилавка и миски — № 111.

### Карта кода (470db8b)
- `PlaceScreen.kt` (467 строк): импорты :3-92; `tag()` :104, `effects()` :111, `effectWords()` :113, `homeJar()` :116 — не
  меняются; `ShopPlace` :122-187 → § 2; `JarsHud` :189-214, `ShopTabs` :216-227 — не меняются; `ItemCard` :229-264 и
  `PayPanel` :266-305 → § 3 (удаляются, на их месте — витрина); `JobPlace`, `TrayJobScene`, `MailChip`, `ShiftTokens`,
  `PlaceEvents` :409-425 (зовёт `JobPlace` :324, :331), `OrderCard` :427-445 (зовёт `JobPlace` :332), `Riddle` — не меняются.
- Используются без правки: `Pic`, `itemRes`, `ResidentPic`, `TText`, `bigFont` (ui/TownUi.kt), `GameButton` (ui/Widgets.kt),
  `Ask` (screens/Common.kt), `PetSprite`, `STATS`; `vm.eventsAt`, `ordersAt`, `shiftQuote`, `startRound`, `quote`, `pass`,
  `makeGoal`, `item`; `Prices.shelf`, `Prices.was`; `ShelfItem` (Prices.kt:8).
- Фон места — неподвижный слой `bg_*_port` под экраном (GameApp.kt:189-190, Crop) — не меняется; витрина рисуется поверх.

### Замер прототипа (эмулятор 360 × 640, 3 px = 1 dp; окно витрины 191..616 при 1,0, 204..616 при 1,15, 209..616 при 1,3)
| кадр | что | где (dp экрана) |
|---|---|---|
| рынок до плана, 1,0 | 3 доски (3 / 2 / 3), Марта, облачко в слоте 319..386 | всё на первом экране, прокрутки нет |
| рынок с заказом, 1,0 | облачко «Помочь Марте +6» 313..386 | «Начать смену» 560..580 (текст) — без прокрутки |
| рынок с Пк3, 1,0 | плакат в 2 строки над банкой; облачко 377..451 | «Начать смену» — после прокрутки (№ 114) |
| рынок, 1,15 / 1,3 | облачко на полу от 564 / 575, стенка за 2-й доской | прокрутка; «Начать смену» — после неё |
| «У Фомы» стр. 1, 1,0 | облачко Фомы 546..613 (3 строки, целиком), «Ещё 9 ▶» | «Ещё 9» 560..580 (текст) — без прокрутки |
| «У Фомы», 1,15 | облачко на полу в 2 строки, «Ещё 9» — второй строкой пола | после прокрутки |
| «У Фомы» стр. 2, 1,0 | «◀ Назад» | 560..580; с плакатами C1 / C3 — за краем (прокрутка) |
| касса, 1,0 | ✕ 48 dp в закреплённой строке (верх 211; у «Каши» 310 — высота по содержимому) | прокрутка тела, dp: «Каша» 0, Пк3 33, «Вкусный обед» 80, змей C3 177, «Домик-палатка» 183, робот C1 205 |
| касса, 1,3 | то же; питомец 80 dp, в облачке значок и «+N» | «Каша» 0, Пк3 74, «Вкусный обед» 77, «Домик-палатка» 247, робот C1 275 |
| 1,3 | вкладки в 2 строки, витрина прокручивается | облачко на полу; узлы первой доски после прокрутки заходят на вкладки на 24 dp (touch bounds, как на BASE) |
Прилавок фона рынка (`bg_market_port`, верх ≈ 386, стенка 408..485 dp на 360 × 640) закрыт стенкой витрины за 2-й доской и
прилавком; стеллаж фона «У Фомы» закрыт стенкой витрины (#F0DECC, стойки #E2B076).

## ЧТО УВИДИТ РЕБЁНОК
| Момент | Что на экране | Что понимает ребёнок |
|---|---|---|
| рынок | под вкладками — две деревянные доски и мятный прилавок, за нижними — светлая стенка: миска корма, каша, мыло, шампунь, шарик, мороженое, карусель стоят на досках крупно, с тенью; под каждым — бумажный ценник на ниточке с монетой и числом, в углу ценника крышка «яблоко» или «подарок»; «хочу» — с новой доски | «Всё видно сразу, цены под товарами — сравню» |
| рынок, Марта | зайка-продавщица крупно у прилавка; над головой облачко «Помочь Марте 🪙+6»; у ног ящики и кнопка «Начать смену» | «Марта зовёт помочь — заработаю 6» |
| рынок, Пк3 | жёлтый плакатик «! Супер-корм! Все питомцы в восторге!» с хвостиком к банке супер-корма | «Эту банку рекламируют» |
| «У Фомы» | стеллаж с тремя досками, кот Фома у прилавка, «Добро пожаловать в лавку!», кнопка «Ещё 9» с малиновым треугольником; на второй странице — «Назад» с треугольником влево | «Здесь больше вещей, листаю» |
| распродажа (C1) | на кнопке «Ещё 9» — «!»; на стр. 2 у робота ценник «~~40~~ 25» с розовой кромкой, над ним плакат «Распродажа робота» | «Робот подешевел» |
| новинка (C3) | на стр. 2 над змеем плакат «В витрине новинка — воздушный змей за 30.»; в кассе змея — вступление про мечту и «Пройти мимо» | «Змей — новинка; а я коплю на мечту» |
| нажал на товар | снизу поднимается касса-ценник: «Корм» и ✕, крышка «Нужное», крупно «20 🪙», корм на подставке, облачко «+40 Сытость», питомец; строка «сколько есть / не хватает»; кнопки вариантов (длинная касса листается под строкой с ✕) | «Цена, отдел и что будет с питомцем — до покупки» |
| шрифт 1,15 / 1,3 | то же, витрина листается под шапкой, облачко продавца — на полу; в кассе у «+N» — значок стата без слова | «Крупно, всё читается» |

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
- 2.5.6 — «Перед покупкой пользователь видит цену, категорию и предполагаемое влияние на питомца»: цена — на ценнике и в кассе,
  категория — крышкой на ценнике и чипом «крышка + слово» в кассе, влияние — облачком в кассе (при шрифте > 1,0 — значок
  стата и «+N», слово — в описании TalkBack); «Покупка требует подтверждения» — кнопка варианта в кассе (и `Ask` для
  неродной банки), как было; «объясняет, чего не хватает и какие есть варианты» — `q.line` и варианты `q.options` без
  изменений.
- 3.6 — мишени ≥ 48 dp (слот товара ≈ 89..107 × 115..123 dp, плакат ≥ 48, кнопки 48); текст ≥ 16 sp (цена 19 sp, старая цена,
  облачка, плакат 16 sp; 14 sp на витрине нет — исключение «Полка лавки» уходит); цвет не единственный признак — отдел
  крышкой (яблоко / подарок), распродажа — зачёркнутым числом, страница — словом и треугольником; наклон ценников статичен
  (не анимация); TalkBack — узел на товар, касса модальна и для TalkBack, событие на другой странице — состояние кнопки
  страницы.
- 2.5.14 — контент данными: новый товар — запись в `content.json`, раскладка по отделам и страницам — `Showcase.pages`, тексты
  плаката, облачков — из событий и жителей. Событие лавки без товара на её полке витрина не показывает (ANTI-SCOPE) — его
  ловит контентное правило оракула (ORACLE `eventItem` п. 4), а не ребёнок.
- 3.5 — реклама — игровой урок: плакат со словами из события, без марок.

## CONTRACT
Домен (кроме нового `Showcase.kt`), ViewModel, `content.json`, тесты (их пишет test-author), ресурсы, манифест, `TownUi.kt`,
`Widgets.kt`, `JarsHud`, `ShopTabs`, `PlaceEvents`, `OrderCard`, `JobPlace` — не меняются. Каждый блок ниже вставляется
**дословно** (с отступами; сверка — `contract_check.py`, ACCEPTANCE п. 7). Импорты — только § 4, точечные (WORKFLOW № 45).

### § 1. Новый файл `finny-pet/app/src/main/java/ru/finny/pet/domain/town/Showcase.kt` — целиком
<!-- file: main/java/ru/finny/pet/domain/town/Showcase.kt -->
```kotlin
package ru.finny.pet.domain.town

import ru.finny.pet.domain.Category

/** Витрина лавки (TOWN-A1s, решение 39 а): товары на досках по 3, по 3 доски на странице. Чистый Kotlin. */
object Showcase {
    const val PER_ROW = 3
    const val ROWS = 3

    /**
     * Страницы → доски → слоты (null — пустой слот). Отделы по порядку [Category] (нужное → хочу → прочее), внутри
     * отдела — порядок [shelf]; каждый отдел с новой доски, неполная доска прижата вправо (пустые слоты слева);
     * по [ROWS] досок на странице. Пустая полка — пустой список.
     */
    fun pages(shelf: List<ShelfItem>): List<List<List<ShelfItem?>>> =
        Category.entries.flatMap { c ->
            shelf.filter { it.item.category == c }.chunked(PER_ROW) { row -> List(PER_ROW - row.size) { null } + row }
        }.chunked(ROWS)

    /** Товар события на витрине: из setup PRICE/OFFER, иначе первый исход BUY:<товар>; нет — null. */
    fun eventItem(e: EventDef): String? =
        e.setup.firstNotNullOfOrNull { (it as? EventEffect.Price)?.item ?: (it as? EventEffect.Offer)?.item }
            ?: e.outcomes.firstNotNullOfOrNull { ((it.fact as? Fact.Buy)?.target as? BuyTarget.Item)?.id }
}
```

### § 2. `PlaceScreen.kt`: заменить :122-187 (`ShopPlace` от `@Composable` до закрывающей `}`) этим блоком
<!-- file: game/java/ru/finny/pet/game/screens/PlaceScreen.kt -->
```kotlin
@Composable
private fun ShopPlace(vm: GameViewModel, place: Place) {
    val shop = vm.tc.shops.firstOrNull { it.place == place.id } ?: return
    val prices = remember { Prices(vm.content) }
    val animate = LocalAnimate.current
    var pick by remember(place.id) { mutableStateOf<String?>(null) }
    var ask by remember { mutableStateOf<PayOption?>(null) }
    var page by remember(place.id) { mutableIntStateOf(0) }
    // after a purchase the pay panel stays while the pet at the counter plays the action, then closes
    var paid by remember { mutableIntStateOf(0) }
    LaunchedEffect(paid) { if (paid > 0) { if (animate) delay(1500); pick = null } }
    // the pay sheet hides LINE and clears the old one; a purchase's line waits until it closes (правка №2)
    LaunchedEffect(pick) { vm.cashOpen = pick != null; if (pick != null) vm.lines.clear() }
    DisposableEffect(Unit) { onDispose { vm.cashOpen = false } }
    fun pay(id: String, o: PayOption) { o.source?.let { vm.buy(id, shop.id, it); paid++ } }

    val pages = Showcase.pages(prices.shelf(vm.state, shop.id))
    // the last keep item of the last page bought — that page is gone, stay on the one before
    val p = page.coerceAtMost(pages.lastIndex).coerceAtLeast(0)
    // item → the event of this place about it: its poster hangs over the item, its intro and «Пройти мимо» are in the pay sheet
    val events = vm.eventsAt(place.id).mapNotNull { e -> Showcase.eventItem(e)?.let { it to e } }.toMap()
    // the title of the first event whose item is on page i: «!» on the pager, the button's state for TalkBack
    fun eventOn(i: Int) = pages.getOrNull(i).orEmpty().flatten().firstNotNullOfOrNull { it?.item?.id?.let(events::get)?.title }
    val scroll = rememberScrollState()
    LaunchedEffect(p) { scroll.scrollTo(0) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud1(vm, inPlace = true)
            Hud2 { JarsHud(vm); Spacer(Modifier.weight(1f)); StatsCollapsed(vm) }
            ShopTabs(vm, place.id)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // under the pay sheet the showcase leaves the TalkBack tree (the scrim takes taps only); after the scroll — it keeps its own
                val under = if (pick != null) Modifier.clearAndSetSemantics {} else Modifier
                ShopScene(vm, place, pages.getOrElse(p) { emptyList() }, events, greet = p == 0, Modifier.verticalScroll(scroll).then(under), onPick = { pick = it }) {
                    if (p > 0) Pager("Назад", next = false, eventOn(p - 1)) { page = p - 1 }
                    if (p < pages.lastIndex) Pager("Ещё ${pages[p + 1].flatten().count { it != null }}", next = true, eventOn(p + 1)) { page = p + 1 }
                }
                pick?.let { id ->
                    Box(Modifier.fillMaxSize().background(SCRIM).pointerInput(Unit) { detectTapGestures { pick = null } })
                    Kassa(vm, id, shop.id, prices.was(vm.state, id, shop.id), events[id], Modifier.align(Alignment.BottomCenter), onClose = { pick = null }) { o ->
                        val item = vm.item(id)
                        when (o.kind) {
                            PayKind.PAY -> if (o.source == item?.homeJar()) pay(id, o) else ask = o
                            PayKind.CHEAPER -> pick = o.itemId
                            PayKind.WAIT -> pick = null
                            PayKind.MAKE_GOAL -> { vm.makeGoal(o.itemId ?: id); pick = null }
                        }
                    }
                }
            }
        }
        ask?.let { o ->
            val close = { ask = null }
            Ask(o.label, o.preview, listOf("Да" to { close(); pick?.let { pay(it, o) }; Unit }, "Отмена" to close), close)
        }
    }
}
```

### § 3. `PlaceScreen.kt`: заменить :229-305 (`ItemCard` и `PayPanel` целиком, от `@Composable` на :229 до `}` на :305) этим блоком
Пустая строка до блока (:228) и после (:306, перед `// ---------- JOB`) остаются.
<!-- file: game/java/ru/finny/pet/game/screens/PlaceScreen.kt -->
```kotlin
// ---------- showcase (A1s, решение 39 а, макеты a1s_mock_1–5; формы — mock.py plastic/board/tag/poster/bubble) ----------

private val ITEM = 60.dp        // an item standing on a board
private val HANG = 20.dp        // board line → top of its tag
private val HANG_C = 6.dp       // counter line → top of its tag: the counter row is shorter, the floor stays on the first screen
private val SELLER = 170.dp     // ResidentPic box: the figure ≈ 150 dp (решение 39 а: продавец крупно)
private val TILT = floatArrayOf(-3f, 2f, -2f, 3f, -1f, 2f, -2f, 1f, 3f)   // tags hang a little crooked; static, not an animation
private val SHADE = Color(0xFF462A5A)
private val DEEP = Color(0xFFBE003A)     // sale price and the poster's first line: contrast ≥ 4.5 on the tag and the poster
private val OLD = Color(0xFF5C426C)      // the struck old price
private val STRING = Color(0xFF966E5A)
private val SCRIM = Color(0x78281446)
private enum class Tail { DOWN, LEFT, RIGHT }

/** Paper and edge of a tag and of the pay sheet by department; the lid tells the department, the colour only helps (ТЗ 3.6). */
private fun Category.paper(): Pair<Color, Color> = when (this) {
    Category.MANDATORY -> Color(0xFFFFF3F4) to Color(0xFFFF8AA0)
    Category.OPTIONAL -> Color(0xFFFFF7D6) to Color(0xFFF0B834)
    Category.UNPLANNED -> Color.White to Color(0xFFC8B4EB)
}

/** The jar a department is planned from (HUD-2): its lid on the tag, lid and word in the pay sheet. */
private fun Category.jar(): Pair<Int, String> = when (this) {
    Category.MANDATORY -> R.drawable.ui_lid_mandatory to "Нужное"
    Category.OPTIONAL -> R.drawable.ui_lid_optional to "Хочу"
    Category.UNPLANNED -> R.drawable.ui_lid_savings to "Запас"
}

/** Icon of a pay option's source: the lid of the jar the coins come from, the piggy for the savings. */
private fun Source.lid(): Int = when (this) {
    Source.NEED, Source.TRANSFER_NEED -> R.drawable.ui_lid_mandatory
    Source.WANT, Source.TRANSFER_WANT -> R.drawable.ui_lid_optional
    Source.RESERVE -> R.drawable.ui_lid_savings
    Source.SAVINGS -> R.drawable.ui_piggy
}

/** mock.py plastic(): a shade under it, a darker «lip» below the face, a white gloss fading to the middle. */
private fun DrawScope.plastic(at: Offset, sz: Size, color: Color, r: Dp, lip: Dp, border: Color? = null, bw: Dp = 0.dp, shade: Boolean = true) {
    val rr = CornerRadius(r.toPx())
    val face = Size(sz.width, sz.height - lip.toPx())
    if (shade) drawRoundRect(SHADE.copy(alpha = 0.2f), at + Offset(0f, lip.toPx() + 3.dp.toPx()), face, rr)
    if (lip > 0.dp) drawRoundRect(lerp(border ?: color, Color.Black, 0.22f), at + Offset(0f, lip.toPx()), face, rr)
    drawRoundRect(color, at, face, rr)
    if (border != null) {
        val b = bw.toPx()
        drawRoundRect(border, at + Offset(b / 2, b / 2), Size(face.width - b, face.height - b), CornerRadius(r.toPx() - b / 2), style = Stroke(b))
    }
    val i = bw.toPx() + 2.dp.toPx()
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), at.y + i, at.y + face.height / 2),
        at + Offset(i, i), Size(face.width - 2 * i, face.height / 2 - i), CornerRadius((r.toPx() - i).coerceAtLeast(1f)),
    )
}

/** Plastic behind the node; the content keeps off the lip. */
private fun Modifier.plastic(color: Color, r: Dp, lip: Dp, border: Color? = null, bw: Dp = 0.dp) =
    drawBehind { plastic(Offset.Zero, size, color, r, lip, border, bw) }.padding(bottom = lip)

/** A board (mock.py board): the lighter top face — a trapezoid behind the items' feet at [y] — and the front below. */
private fun DrawScope.board(y: Float, x0: Float, x1: Float, wood: Color) {
    val d = 1.dp.toPx()
    drawPath(Path().apply { moveTo(x0 + 5.3f * d, y - 8.7f * d); lineTo(x1 - 5.3f * d, y - 8.7f * d); lineTo(x1, y + 1.3f * d); lineTo(x0, y + 1.3f * d); close() }, lerp(wood, Color.White, 0.35f))
    plastic(Offset(x0, y + 0.7f * d), Size(x1 - x0, 20 * d), wood, 5.3.dp, 2.dp)
}

/** The counter the last board stands on: the top face, a wooden edge and the front the tags hang over, down to [bottom]. */
private fun DrawScope.counter(y: Float, bottom: Float, wood: Color, front: Color) {
    val d = 1.dp.toPx()
    val (x0, x1) = 8 * d to size.width - 8 * d
    drawPath(Path().apply { moveTo(x0 + 5.3f * d, y - 8.7f * d); lineTo(x1 - 5.3f * d, y - 8.7f * d); lineTo(x1, y + 1.3f * d); lineTo(x0, y + 1.3f * d); close() }, lerp(wood, Color.White, 0.35f))
    plastic(Offset(x0, y + 6 * d), Size(x1 - x0, bottom - y - 6 * d), front, 6.dp, 3.dp)
    plastic(Offset(x0, y + 0.7f * d), Size(x1 - x0, 9 * d), wood, 4.dp, 0.dp, shade = false)
}

/** «У Фомы»: the back wall of the rack with two posts; hides the rack drawn into bg_foma_port (mock.py foma_rack). */
private fun DrawScope.backWall(bottom: Float) {
    val d = 1.dp.toPx()
    drawRect(Color(0xFFF0DECC), Offset(20 * d, 0f), Size(size.width - 40 * d, bottom))
    for (x in listOf(10 * d, size.width - 24 * d)) plastic(Offset(x, 0f), Size(14 * d, bottom), Color(0xFFE2B076), 4.dp, 0.dp, shade = false)
}

/**
 * The showcase of a shop page: boards of 3 items with price tags on strings, the last board of the page is the counter;
 * event posters over their items; the seller large on the floor with a bubble, the place's first order (Marta: crates,
 * «Начать смену»; not filtered by resident — another resident's order would stand in the seller's bubble, ANTI-SCOPE), the
 * pager. One page is at most 3 boards (Showcase.pages).
 */
@Composable
private fun ShopScene(
    vm: GameViewModel, place: Place, rows: List<List<ShelfItem?>>, events: Map<String, EventDef>, greet: Boolean,
    modifier: Modifier, onPick: (String) -> Unit, pager: @Composable () -> Unit,
) {
    val tagH = with(LocalDensity.current) { 19.sp.toDp() } + 22.dp
    val slab = HANG_C + tagH + 6.dp
    // ponytail: art by place id, like placeBackground — «У Фомы» is a room with a rack, the market is open air
    val indoor = place.id == "foma"
    val (wood, front) = if (indoor) Color(0xFFECBE86) to Color(0xFFFAE07F) else Color(0xFFF5C892) to Color(0xFF8FD9BE)
    val resident = vm.tc.residents.firstOrNull { it.id == place.resident }
    val order = vm.ordersAt(place.id).firstOrNull()
    val job = order?.let { o -> vm.tc.jobs.firstOrNull { it.id == o.params.job } }
    val talk = order != null || greet
    // the seller's bubble in the empty left slot of the board over the counter (макет 1); no such slot or any font above 1.0 —
    // on the floor, up to 240 dp (at 1.15 the slot of 124 dp broke words and the bubble grew over the tag above)
    val scaled = LocalDensity.current.fontScale > 1f
    val inSlot = talk && !scaled && rows.size >= 2 && rows[rows.size - 2][0] == null
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val w = maxWidth
        Column {
            Column(Modifier.testTag("shelf").drawBehind { if (indoor && rows.isNotEmpty()) backWall(size.height - slab.toPx()) }) {
                // ponytail: the seller reaches ≈ 160 dp over the floor — a page of one board gets room above it
                Spacer(Modifier.height(maxOf(2.dp, 160.dp - 125.dp * rows.size)))
                rows.forEachIndexed { r, row ->
                    val counter = r == rows.lastIndex
                    val (start, end) = if (counter) 76.dp to 16.dp else 19.dp to 19.dp
                    val slot = (w - start - end) / 3
                    val posters = row.withIndex().mapNotNull { (i, s) -> s?.let { events[it.item.id] }?.let { Triple(i, s, it) } }
                    if (posters.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        val pw = if (posters.size == 1) minOf(250.dp, w - 24.dp) else (w - 24.dp) / 2 - 4.dp
                        posters.forEach { (i, s, e) ->
                            val cx = start + slot * (i + 0.5f)
                            val x = (cx - pw / 2).coerceIn(12.dp, w - 12.dp - pw)
                            Poster(e, cx - x, Modifier.offset(x = x).width(pw)) { onPick(s.item.id) }
                        }
                    }
                    val h = ITEM + 2.dp + if (counter) slab else HANG + tagH
                    Box(
                        Modifier.fillMaxWidth().height(h).drawBehind {
                            val y = (ITEM + 2.dp).toPx()
                            // the market's lower back board behind the 2nd board and the counter: the counter and bowl painted into
                            // bg_market_port stand still under the scroll and looked like an item without a tag (круг критиков 1)
                            if (!indoor && r > 0) drawRect(lerp(front, Color.White, 0.55f), Offset(19.dp.toPx(), 0f), Size(size.width - 38.dp.toPx(), size.height))
                            if (counter) counter(y, size.height, wood, front) else board(y, 19.dp.toPx(), size.width - 19.dp.toPx(), wood)
                        },
                    ) {
                        Row(Modifier.fillMaxSize().padding(start = start, end = end)) {
                            row.forEachIndexed { i, s ->
                                if (s == null) Spacer(Modifier.weight(1f))
                                else Slot(s, TILT[(r * 3 + i) % TILT.size], if (counter) HANG_C else HANG, tagH, Modifier.weight(1f)) { onPick(s.item.id) }
                            }
                        }
                        // over the seller's head: the bubble's bottom (tail tip) meets the top of the figure
                        if (inSlot && r == rows.size - 2) Box(Modifier.offset(x = 8.dp).size(124.dp, h + ITEM + 2.dp + slab - SELLER + 2.dp)) {
                            SellerBubble(vm, resident, order, job, Tail.DOWN, Modifier.align(Alignment.BottomStart).wrapContentHeight(Alignment.Bottom, unbounded = true))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth()) {
                // the figure's feet on the floor at the counter; it takes no room, the boards above keep their places. No
                // semantics: its 170 dp box would cover the counter's first item and cut that item's TalkBack bounds
                if (resident != null) ResidentPic(resident, SELLER, Modifier.clearAndSetSemantics {}.layout { m, _ ->
                    val pl = m.measure(Constraints())
                    layout(0, 0) { pl.place((-46).dp.roundToPx(), -(SELLER - 10.dp).roundToPx()) }
                })
                // 10 dp up onto the counter's lip: Foma's three-line greeting stays whole over the bottom of 360 × 640 at 1.0
                FlowRow(
                    Modifier.offset(y = (-10).dp).fillMaxWidth().padding(start = 92.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 136 dp: the bubble and «Ещё 9» share one line at 1.0 on 360 dp (макет 2); above 1.0 they take two lines
                    if (talk && !inSlot) SellerBubble(vm, resident, order, job, Tail.LEFT, Modifier.padding(end = 8.dp).widthIn(max = if (scaled) 240.dp else 136.dp))
                    if (job != null && vm.shiftQuote(job.id).canPlay) {
                        Crates()
                        GameButton("Начать смену", style = ButtonStyle.PRIMARY, minHeight = 48.dp) { vm.startRound(job.id) }
                    }
                    pager()
                }
            }
        }
    }
}

/** One item on a board: the picture, the string, the tag. TalkBack: one node with the words of the former card (A1s R8). */
@Composable
private fun Slot(s: ShelfItem, tilt: Float, hang: Dp, tagH: Dp, modifier: Modifier, onClick: () -> Unit) {
    val item = s.item
    val desc = listOf(item.title, "${s.price} монет", item.tag().drop(3), item.effectWords()).filter { it.isNotBlank() }.joinToString(", ")
    Column(
        modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = desc }
            .drawBehind {
                val x = size.width / 2
                val y = (ITEM + 2.dp).toPx()
                drawLine(STRING, Offset(x, y + if (hang == HANG) 18.dp.toPx() else 6.dp.toPx()), Offset(x, y + (hang + 7.dp).toPx()), 1.3.dp.toPx())
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShelfPic(item, ITEM)
        Spacer(Modifier.height(2.dp + hang))
        PriceTag(s, tilt, tagH)
    }
}

/** Opaque bounds of the item pictures (alpha ≥ 40 as mock.py sprite: item_fun_balloon has a see-through pad), once per drawable. */
private val opaque = HashMap<Int, IntRect>()

/**
 * An item standing on a board (mock.py sprite + place): the picture cropped to its opaque part, fitted into [size] × 1.1 by
 * [size], its bottom on the bottom edge, a soft contact shade under it; no picture — the emoji (Pic). Pic keeps its margins
 * for the other screens.
 */
@Composable
private fun ShelfPic(item: ShopItem, size: Dp, modifier: Modifier = Modifier) {
    val res = itemRes(item.id)
    if (res == null) { Pic(null, item.emoji, size, modifier); return }
    val img = ImageBitmap.imageResource(res)
    val box = opaque.getOrPut(res) {
        val px = IntArray(img.width * img.height).also { img.readPixels(it) }
        var (l, t, r, b) = intArrayOf(img.width, img.height, -1, -1)
        for (i in px.indices) if (px[i] ushr 24 >= 40) { val x = i % img.width; val y = i / img.width; l = minOf(l, x); t = minOf(t, y); r = maxOf(r, x); b = maxOf(b, y) }
        if (r < 0) IntRect(0, 0, img.width, img.height) else IntRect(l, t, r + 1, b + 1)
    }
    Image(
        BitmapPainter(img, box.topLeft, box.size), null,
        modifier.size(size * 1.1f, size).drawBehind {
            val w = box.width * minOf(this.size.width / box.width, this.size.height / box.height) * 0.84f
            drawOval(SHADE.copy(alpha = 0.18f), Offset((this.size.width - w) / 2, this.size.height - 4.dp.toPx()), Size(w, 8.dp.toPx()))
        },
        alignment = Alignment.BottomCenter,
    )
}

/** mock.py tag(): paper on a string, the ring at the top, coin and price 19 sp; a sale — the old price struck, the new dark red. */
@Composable
private fun PriceTag(s: ShelfItem, tilt: Float, h: Dp) {
    val (paper, edge) = s.item.category.paper()
    val coin = with(LocalDensity.current) { 17.sp.toDp() }
    val price = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
    Box(Modifier.rotate(tilt)) {
        Row(
            Modifier.height(h).widthIn(min = 67.dp).plastic(paper, 10.dp, 0.dp, if (s.was != null) G.magenta else edge, 2.dp)
                .drawBehind {
                    val c = Offset(size.width / 2, 6.7.dp.toPx())
                    drawCircle(Color.White, 3.3.dp.toPx(), c)
                    drawCircle(edge, 3.3.dp.toPx(), c, style = Stroke(1.3.dp.toPx()))
                }
                .padding(start = 11.dp, end = 11.dp, top = 11.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            if (s.was != null) {
                TText("${s.was}", Modifier.strike(), MaterialTheme.typography.titleSmall, OLD, maxLines = 1)
                Spacer(Modifier.width(5.dp))
            }
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(coin))
            Spacer(Modifier.width(3.dp))
            TText("${s.price}", style = price, color = if (s.was != null) DEEP else G.purpleDeep, maxLines = 1)
        }
        Image(painterResource(s.item.category.jar().first), null, Modifier.offset((-10).dp, (-13).dp).size(32.dp))
    }
}

/** The old price of a sale: a pink stroke from bottom left to top right (mock.py tag). */
private fun Modifier.strike() = drawWithContent {
    drawContent()
    drawLine(G.magenta, Offset(-2.dp.toPx(), size.height * 0.72f), Offset(size.width + 2.dp.toPx(), size.height * 0.3f), 2.dp.toPx())
}

/**
 * The poster of an event over its item; the tap opens the item. Words: the ad's quoted words of the intro (Пк3, P3), else the
 * title of a price event (C1 «Распродажа робота»), else the intro's first sentence — C3's title «Мечта почти твоя» over the
 * kite would call the kite the dream, and C3's lesson is to pass it by.
 */
@Composable
private fun Poster(e: EventDef, tailX: Dp, modifier: Modifier, onClick: () -> Unit) {
    val words = Regex("«(.+)»").find(e.intro)?.groupValues?.get(1)
        ?: e.title.takeIf { e.setup.any { it is EventEffect.Price || it is EventEffect.Offer } }
        ?: Regex("^.+?[.!?](?=\\s|$)").find(e.intro)?.value ?: e.intro
    val style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp)
    Box(modifier.clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = "${e.title}. ${e.intro}" }) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .drawBehind {
                    val x = tailX.toPx()
                    val d = 1.dp.toPx()
                    drawPath(Path().apply { moveTo(x - 7 * d, size.height - 3 * d); lineTo(x + 7 * d, size.height - 3 * d); lineTo(x, size.height + 9 * d); close() }, Color.White)
                }
                .plastic(Color(0xFFFFE278), 7.dp, 2.dp, Color.White, 2.3.dp).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            words.split(Regex("(?<=[!?.]) ")).forEachIndexed { i, t -> TText(t, style = style, color = if (i == 0) DEEP else G.purple, align = TextAlign.Center) }
        }
        Badge(Modifier.offset((-5).dp, (-5).dp))
    }
}

/** «!» of an event (mock.py badge), drawn: no text, its words are in the poster's description and the pager's state. */
@Composable
private fun Badge(modifier: Modifier) = Canvas(modifier.size(22.dp)) {
    val c = size.width / 2
    drawCircle(G.magenta, c)
    drawCircle(Color.White, c - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
    drawLine(Color.White, Offset(c, size.height * 0.26f), Offset(c, size.height * 0.58f), 3.dp.toPx(), StrokeCap.Round)
    drawCircle(Color.White, 1.7.dp.toPx(), Offset(c, size.height * 0.76f))
}

/** mock.py bubble(): white, round, a soft shade; the tail points down at the start (the head below) or to a side. */
@Composable
private fun Cloud(modifier: Modifier, tail: Tail, content: @Composable ColumnScope.() -> Unit) = Column(
    modifier.drawBehind {
        val d = 1.dp.toPx()
        val (w, h) = size.width to size.height
        drawRoundRect(SHADE.copy(alpha = 0.2f), Offset(0f, 3 * d), size, CornerRadius(13 * d))
        val t = when (tail) {
            Tail.DOWN -> listOf(Offset(18 * d, h - 2 * d), Offset(34 * d, h - 2 * d), Offset(24 * d, h + 9 * d))
            Tail.LEFT -> listOf(Offset(2 * d, h / 2 - 8 * d), Offset(2 * d, h / 2 + 8 * d), Offset(-10 * d, h / 2 + 12 * d))
            Tail.RIGHT -> listOf(Offset(w - 2 * d, h / 2 - 8 * d), Offset(w - 2 * d, h / 2 + 8 * d), Offset(w + 10 * d, h / 2 + 12 * d))
        }
        drawPath(Path().apply { moveTo(t[0].x, t[0].y); lineTo(t[1].x, t[1].y); lineTo(t[2].x, t[2].y); close() }, Color.White)
        drawRoundRect(Color.White, cornerRadius = CornerRadius(13 * d))
    }.padding(horizontal = 10.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp), content = content,
)

/** The seller's bubble: Marta's order (its title, coin +pay; TalkBack — the intro, the pay line, the shifts) or the resident's line. */
@Composable
private fun SellerBubble(vm: GameViewModel, resident: Resident?, order: EventDef?, job: Job?, tail: Tail, modifier: Modifier) {
    val text = order?.title ?: resident?.lines?.firstOrNull() ?: return
    val q = job?.let { vm.shiftQuote(it.id) }
    val max = vm.tc.rules.shiftsPerWeek
    val desc = if (order != null && q != null) "${dot(order.title)} ${dot(order.intro)} ${dot(q.line)} Смены: ${vm.state.shiftsThisPeriod.coerceIn(0, max)} из $max" else text
    Cloud(modifier.clearAndSetSemantics { contentDescription = desc }, tail) {
        // 18 sp lines, Medium for a line: three short lines fit over the seller's head at 1.0 (макет 1)
        val style = if (order != null) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        TText(text.replace(Regex("(?<=(^|\\s)[А-Яа-яЁёA-Za-z]) "), "\u00A0"), style = style.copy(lineHeight = 18.sp), color = if (order != null) G.purple else G.ink)
        if (q != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(painterResource(R.drawable.ui_coin), null, Modifier.size(22.dp))
            TText(if (q.top > q.base) "+${q.base}–${q.top}" else "+${q.base}", style = MaterialTheme.typography.titleMedium, color = G.purple, maxLines = 1)
        }
    }
}

/** Marta's cardboard boxes by her order (mock.py crates), drawn: no box emoji, the debug emoji probe stays 0. */
@Composable
private fun Crates() = Canvas(Modifier.size(52.dp, 48.dp)) {
    val d = 1.dp.toPx()
    val (card, tape) = Color(0xFFD9A066) to Color(0xFFB5793F)
    plastic(Offset(2 * d, 18 * d), Size(46 * d, 30 * d), card, 3.dp, 2.dp)
    drawRect(tape, Offset(21 * d, 18 * d), Size(8 * d, 12 * d))
    rotate(8f, Offset(26 * d, 10 * d)) {
        plastic(Offset(10 * d, 0f), Size(32 * d, 20 * d), card, 3.dp, 2.dp)
        drawRect(tape, Offset(22 * d, 0f), Size(8 * d, 9 * d))
    }
}

/**
 * «Ещё N ▶» / «◀ Назад» of «У Фомы»: 48 dp, the arrow drawn in magenta (макет 2), after the words forward, before them back —
 * ponytail: GameButton's icon stands first, right to left puts it last (no trailing slot in Widgets.kt). «!» when that page
 * has an event's item; the [event]'s title is the button's state for TalkBack.
 */
@Composable
private fun Pager(label: String, next: Boolean, event: String?, onClick: () -> Unit) = Box {
    CompositionLocalProvider(LocalLayoutDirection provides if (next) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        val arrow = remember(next) { Arrow(next) }
        GameButton(label, if (event != null) Modifier.semantics { stateDescription = event } else Modifier, ButtonStyle.PAPER, icon = arrow, iconSize = 18.dp, minHeight = 48.dp, onClick = onClick)
    }
    if (event != null) Badge(Modifier.offset((-6).dp, (-6).dp))
}

/** The pager's triangle (макет 2: ≈ 13 × 18 dp), forward or back. */
private class Arrow(val next: Boolean) : Painter() {
    override val intrinsicSize = Size.Unspecified
    override fun DrawScope.onDraw() {
        val (x0, x1) = size.width * 0.14f to size.width * 0.86f
        val (tip, base) = if (next) x1 to x0 else x0 to x1
        drawPath(Path().apply { moveTo(base, 0f); lineTo(tip, size.height / 2); lineTo(base, size.height); close() }, G.magenta)
    }
}

/** Касса-ценник (макет a1s_mock_4): name and ✕ stay put (WORKFLOW № 38), under them the body scrolls: department (lid +
 * word), price, the item and the pet with its effect; the quote's line, the event's intro, the options (main, the rest under
 * «Ещё ▾»), «Пройти мимо» of the event. */
@Composable
private fun Kassa(vm: GameViewModel, itemId: String, shopId: String, was: Int?, event: EventDef?, modifier: Modifier, onClose: () -> Unit, onOption: (PayOption) -> Unit) {
    val item = vm.item(itemId) ?: return
    val q = vm.quote(itemId, shopId)
    val pet = vm.state.pet
    val particles = LocalParticles.current
    val act = LocalPetAction.current
    val text = MaterialTheme.typography.bodyMedium
    var more by remember(itemId) { mutableStateOf(false) }
    val (paper, edge) = item.category.paper()
    val (lid, word) = item.category.jar()
    BackHandler(onBack = onClose)
    Column(modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth().plastic(paper, 20.dp, 4.dp, edge, 2.7.dp).testTag("pay_panel")) {
        Row(Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TText(item.title, Modifier.weight(1f), MaterialTheme.typography.headlineMedium, G.purpleDeep)
            Box(Modifier.size(48.dp).background(Color.White, CircleShape).border(1.5.dp, Color(0xFFDCC8F0), CircleShape).clickable(role = Role.Button, onClick = onClose).semantics { contentDescription = "Закрыть кассу" }, contentAlignment = Alignment.Center) {
                TText("✕", style = MaterialTheme.typography.titleMedium, color = G.purpleDeep, maxLines = 1)
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 14.dp).testTag("pay_body"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.heightIn(min = 36.dp).background(Color.White, RoundedCornerShape(50)).border(2.dp, edge, RoundedCornerShape(50)).padding(start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(painterResource(lid), null, Modifier.size(28.dp))
                    TText(word, style = MaterialTheme.typography.titleSmall, color = G.purple, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                if (was != null) {
                    TText("$was", Modifier.strike(), MaterialTheme.typography.titleMedium, OLD, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                }
                TText("${q.price}", style = MaterialTheme.typography.displaySmall, color = if (was != null) DEEP else G.purpleDeep, maxLines = 1)
                Image(painterResource(R.drawable.ui_coin), null, Modifier.size(34.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Box(Modifier.width(96.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.size(96.dp, 18.dp).plastic(Color(0xFFCDB9F0), 9.dp, 3.dp))
                    ShelfPic(item, 80.dp, Modifier.padding(bottom = 8.dp))
                }
                // weighted: measured after the item and the pet — the cloud gives way, the pet keeps its 80 dp at 1.3; above 1.0 the
                // stat's word gives way to its icon (the words — in the cloud's description, bigFont's rule)
                Box(Modifier.weight(1f).padding(bottom = 18.dp), contentAlignment = Alignment.BottomEnd) {
                    Cloud(Modifier.clearAndSetSemantics { contentDescription = item.effectWords() }, Tail.RIGHT) {
                        item.effects().forEach { (i, v) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Image(painterResource(STATS[i].icon), null, Modifier.size(24.dp))
                                TText("+$v", style = MaterialTheme.typography.titleLarge, color = G.greenDark, maxLines = 1)
                            }
                            if (LocalDensity.current.fontScale <= 1f) TText(STATS[i].word, style = text, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (pet != null) PetSprite(
                    speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = vm.face, animate = LocalAnimate.current,
                    size = 80.dp, action = act.action, actionKey = act.key, seen = act, description = pet.name,
                    modifier = Modifier.particleTarget(particles, "pet"),
                )
            }
            if (q.line.isNotBlank()) TText(q.line, style = text)
            if (q.note.isNotBlank()) TText(q.note, style = text, color = G.inkSoft)
            event?.let { TText(it.intro, style = text) }
            // before the plan: [Разложить] under the quote's line, no jar to pay from yet (правка №5)
            if (!vm.state.plan.confirmed) GameButton("Разложить", Modifier.fillMaxWidth(), minHeight = 48.dp) { onClose(); vm.navigate(Screen.Jars) }
            val (main, rest) = q.options.partition { !it.more }
            val option: @Composable (PayOption) -> Unit = { o ->
                val home = o.kind == PayKind.PAY && o.source == item.homeJar()
                val icon = when (o.kind) { PayKind.CHEAPER -> o.itemId?.let(::itemRes); PayKind.PAY -> o.source?.lid(); else -> null }
                GameButton(o.label, Modifier.fillMaxWidth(), if (home) ButtonStyle.PRIMARY else ButtonStyle.PAPER, icon = icon?.let { painterResource(it) }, minHeight = 48.dp) { onOption(o) }
                // an option that asks first shows its preview in the question (Ask); one that acts at once — here
                if (o.kind != PayKind.PAY || home) o.preview.firstOrNull()?.let { TText(it, style = text, color = G.inkSoft) }
            }
            main.forEach { option(it) }
            if (rest.isNotEmpty()) {
                if (!more) GameButton("Ещё ▾", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { more = true }
                else rest.forEach { option(it) }
            }
            event?.takeIf { e -> e.outcomes.any { it.fact == Fact.Skip } }?.let { e ->
                GameButton("Пройти мимо", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.pass(e.id); onClose() }
            }
        }
    }
}
```

### § 4. Импорты `PlaceScreen.kt`
Удалить 2: `androidx.compose.ui.text.style.Hyphens`, `androidx.compose.ui.text.style.LineBreak`. Добавить 36 (каждый — в
алфавитное место среди существующих, порядок существующих не менять; acc.sh сверяет множество строк `+import` с этим блоком):
```
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import ru.finny.pet.domain.town.EventEffect
import ru.finny.pet.domain.town.Resident
import ru.finny.pet.domain.town.Showcase
```
`Showcase.kt` импортирует только `ru.finny.pet.domain.Category`.

### § 5. Явно НЕ определено (кодер спрашивает, а не решает)
Любые числа dp / sp / цвета, кроме блоков § 2–3; иной текст облачков, плаката, кнопок; `contentDescription` продавца и
ящиков; ряд монет в кассе; ниточка к кассе; отражение Фомы; жесты листания; анимации; правка `TownUi.kt`, `Widgets.kt`,
`GameApp.kt`, `PlaceEvents`, `OrderCard`; новые файлы, кроме § 1; `testTag` сверх `shelf`, `pay_panel`, `pay_body`.
В блоке § 3 неразрывный пробел — escape `"\u00A0"` в исходнике, литерал U+00A0 не вставлять (acc.sh проверка 26).

## SCOPE
variant: game (UI) + `main/` (одна чистая функция)
test-author (до кодера): allow `finny-pet/app/src/test/java/ru/finny/pet/domain/town/ShowcaseTest.kt` (новый файл);
  пишет по ORACLE и сигнатуре § 1, запускает его красным (класса `Showcase` в BASE нет — не компилируется, это «красный»).
coder: allow `finny-pet/app/src/game/java/ru/finny/pet/game/screens/PlaceScreen.kt`,
  `finny-pet/app/src/main/java/ru/finny/pet/domain/town/Showcase.kt` (новый).
protect: базовый (docs/WORKFLOW.md «Базовый protect») + поимённо все файлы `game/java`, кроме PlaceScreen.kt;
  `game/res/`, `game/AndroidManifest.xml`; `app/src/main/` — всё, кроме `Showcase.kt`; `app/src/test/`; `classic/`;
  `finny-pet/docs/`; `finny-pet/tools/`, корневые `tools/` — генератор `scope.py` («Зонды приёмки»; allow ∩ protect — пусто,
  скрипт проверяет). `base` в task-scope — sha коммита оракула (`O`, ДО СПАВНА п. 4).
Кодеру: команды с путями из protect — без `>` и `2>&1` (WORKFLOW № 29); сборку не запускать параллельно с оркестратором.

## ANTI-SCOPE
- **Blender-доски, ценники, касса** (`shopprops.py`, строка эпика A1s) — № 110; до ответа — Compose (R1).
- **Перерендер `bg_market_port` без прилавка и миски** (открытое небо за 2–3 рядом, как в макете 1) — № 111; до ответа —
  стенка витрины за 2-й доской.
- **Ряд монет «есть / не хватает»** в кассе (макет 4) — не масштабируется до цены 60 (R5); строка `q.line`.
- **Картинка плаката** `poster_food_super.webp` (9 468 Б, № 96 б) на экране больше не показывается: Пк3 — на рынке (лавка), а
  `PlaceEvents` с `posterRes` осталась только в `JobPlace`. Ссылка в коде есть — lint UnusedResources её не видит, R8 файл
  оставит. Удалить `posterRes` и WebP (тогда же `LICENSES.md:55`) или держать для событий мест-работ — № 115.
- **Событие лавки без товара на её полке** (`eventItem` = null или товара нет в `pages`) и второе событие про тот же товар
  витрина не показывает (ни плаката, ни вступления, ни «Пройти мимо»; интро прибытия `openPlace` глушит). Живых таких нет:
  кандидаты `pk2_change` («Сдача», CHANGE, `eventsOff` до сцен среза 2, GAME_CONCEPT.md:946) и `job_kassa_help` — в
  `eventsOff`. Включение такого события правкой одного `content.json` роняет контентное правило оракула (ORACLE `eventItem`
  п. 4), место показа (облачко продавца / касса) решает сцена среза 2. «!» улицы (`StreetScreen.kt:95`) ставится по любому
  событию места — разойдётся с пустой лавкой при включении; проверка среза 2. Пунктом в BACKLOG (ДОКИ).
- **Заказ чужого жителя в лавке** (`job_kassa_help`: место foma, работа `job_kassa`, житель Кеша; `eventsOff`): витрина берёт
  первый заказ места без фильтра по жителю (`ShopScene`, KDoc) — при включении у Фомы встанут его облачко с заголовком заказа
  Кеши («Касса у Кеши» и плата), ящики и «Начать смену» (`job_kassa`); прежний `OrderCard` показывал все заказы места с
  `job.resident`. Эпик кассы CHANGE (TOWN-J1:350) пересматривает витрину вместе с раундом; подменять Фому Кешей нельзя
  (композиция макета 1).
- **Неразрывный пробел после однобуквенного слова** — только облачко продавца (`SellerBubble`); прочие облачки
  (`SpeechBubble`, `TText` в `TownUi.kt`) — вне A1s, пунктом в BACKLOG.
- **Сравнение кормов Пк3 на полке** (в 1.4.1 «+40» стояло у обоих на карточках): эффект — только в кассе (№ 39 а); подпись под
  кадром Пк3 на листе владельцу. Вернуть эффект на полку — отдельная задача после 1.4.2, если владелец скажет.
- Ниточка от доски к кассе, пунктир на месте товара (макет 4); отражение Фомы (макет 2 — хвост к стене); размер кассы и
  место «Пройти мимо» на 360 × 640 (№ 112); облачко-кнопка Марты (№ 114); «Помоги с ящиками!», жетоны смен в облачке и другие
  тексты макета (№ 113); подпись HUD-1 «Выбери мечту» при 1,15 (HUD — R9; BACKLOG).
- Звук, анимации витрины и кассы (появление, покачивание ценников) — нет.
- Листы и судьи, S23, `perf.sh` на лавке (строка эпика) — живая проверка оркестратора и человека после приёмки.
- Классика, домен (кроме § 1), ViewModel, `content.json`, ресурсы, зависимости.

## БЮДЖЕТ
- Код: без строк import и пустых — ≤ 450 вставок и ≤ 75 удалений; прототип **+436 −67** (`PlaceScreen.kt` +417 −67,
  `Showcase.kt` +19); импорты game +36 −2 (+ `Category` в `Showcase.kt`); сырой `git diff --shortstat` (с `git add -N` нового
  файла) — 2 файла, +522 −91. Счёт — `git diff -w 470db8b -- finny-pet/app/src/main finny-pet/app/src/game | grep '^+[^+]' |
  grep -v '^+import ' | grep -vc '^+\s*$'` и то же для `'^-[^-]'` / `'^-import '` (acc.sh 21–22; новый файл виден после шага 0).
- APK: release BASE, собранный в worktree на 470db8b, — 4 637 942 Б (= файл finny-pet/release/finny-pet-1.4.1-release.apk);
  прототип **4 654 326 Б, +16 384 Б** (ассетов 0, только код; APK растёт ступенями 16 КиБ). Предел — +32 768 Б. Остаток бюджета
  эпика = 4 081 493 + 1 048 576 − размер: прототип **475 743 Б** на A1h и код.
- Время: test-author — один заход; кодер — один заход (дословный контракт); живая — маршрут + 46 кадров скриптом.

## ORACLE (test-author: `app/src/test/java/ru/finny/pet/domain/town/ShowcaseTest.kt`)
Сигнатура (§ 1): `object Showcase { const val PER_ROW = 3; const val ROWS = 3; fun pages(shelf: List<ShelfItem>):
List<List<List<ShelfItem?>>>; fun eventItem(e: EventDef): String? }`, пакет `ru.finny.pet.domain.town`.
Контент — реальный (`TestContent.content`, полка — `Prices(content).shelf(state, shopId)`, состояние — как в PricesTest:
`GameState(demo, period, owned)`), синтетика — `ShelfItem(ShopItem(...), price)`, `EventDef(...)` с нужными `setup` / `outcomes`.
Правила `pages`:
1. отделы по порядку `Category` (MANDATORY → OPTIONAL → UNPLANNED); внутри отдела — порядок входного списка (стабильно);
2. каждый отдел начинается с новой доски; в доске ровно 3 слота; в доске один отдел;
3. неполная доска прижата вправо: пустые слоты (null) — слева, товары — справа, в порядке входа;
4. по 3 доски на странице, последняя страница может быть короче; пустая полка → пустой список страниц;
5. ни одна доска не пуста; `pages(s).flatten().flatten().filterNotNull()` = вход, стабильно отсортированный по отделу.
Примеры (ids товаров; `_` — null):
| полка | ожидание |
|---|---|
| рынок, неделя 1, не демо (`food_super` закрыт до 2) | 1 страница: `[food_basic, food_porridge, care_soap] [_, _, care_shampoo_simple] [fun_balloon, fun_icecream, fun_carousel]` |
| рынок, неделя 2 (и демо) | `[food_basic, food_porridge, food_super] [_, care_soap, care_shampoo_simple] [fun_balloon, fun_icecream, fun_carousel]` |
| «У Фомы», неделя 1 (закрыты картина 5, змей 2, лампа-звёздочка 3) | стр. 1: `[food_basic, food_lunch, care_soap] [care_shampoo, care_brush, care_vitamins] [fun_balloon, fun_rug, fun_ball]`; стр. 2: `[fun_spinner, fun_book, fun_robot] [fun_tent, gift_card, gift_toy]` |
| «У Фомы», неделя 5 (и демо) | стр. 1: `… [fun_balloon, fun_picture, fun_rug]`; стр. 2: `[fun_ball, fun_spinner, fun_kite] [fun_book, fun_robot, fun_starlamp] [fun_tent, gift_card, gift_toy]` |
| «У Фомы», неделя 5, не демо, куплены `fun_robot`, `fun_tent` (keep) | стр. 2: `[fun_ball, fun_spinner, fun_kite] [fun_book, fun_starlamp, gift_card] [_, _, gift_toy]` |
| пустая | `[]` |
| 1 отдел, 10 товаров a..j | `[[a,b,c],[d,e,f],[g,h,i]]`, `[[_,_,j]]` |
| вход `[OPT a, MAN b, OPT c, UNPL d]` | 1 страница: `[_, _, b] [_, a, c] [_, _, d]` |
| вход `[MAN m1, OPT o1, UNPL u1, MAN m2]` | 1 страница: `[_, m1, m2] [_, _, o1] [_, _, u1]` |
Правила `eventItem`:
1. setup `PRICE` / `OFFER` — товар ПЕРВОГО такого эффекта (PRICE и OFFER равноправны, порядок — порядок setup);
2. иначе первый исход `BUY` с целью «товар» (`BuyTarget.Item`); `BUY:FOOD` (нужда), `BUY:#тег`, `BUY_AT` — не товар;
3. ничего — null;
4. контент (живой `content.json`): у каждого live-события (`town.events` без `town.eventsOff`, `kind ≠ JOB`), у которого
   `place` или триггер `ENTER:<place>` — место лавки (`town.shops.any { it.place == place }`), `eventItem(e) ≠ null` и этот товар
   есть в `sells` лавки этого места; у одного места нет двух таких событий с одним `eventItem`. Сейчас это p3 (рынок и «У Фомы»,
   `food_basic`), c1 (`fun_robot`), c3 (`fun_kite`), pk3 (`food_super`); включение `pk2_change` должно ронять правило.
Примеры по `content.json`: `pk3_super_food` → `food_super` (первый исход — `BUY_AT`, пропускается), `p3_price_up` →
`food_basic` (реальный контент), `c1_robot_sale` → `fun_robot`, `c3_almost` → `fun_kite`, `job_market_help` → null,
`pk1_cheaper_food` → null. Синтетика: setup `[PRICE(a), PRICE(b)]` → `a` (не последний); setup `[OFFER(a), PRICE(b)]` → `a`
(PRICE не важнее OFFER); setup `OFFER(x)` и исход `BUY:y` → `x` (setup важнее исходов).
Отчёт test-author (test-author.md п. 5) называет для каждого правила тест, который его ловит, — по этим именам п. 4а.
Красный на BASE — компиляция (класса `Showcase` нет); сила оракула — ДО СПАВНА п. 4а (мутанты M1–M4 завалены на ассертах).
Тестов всего станет 600 + K (K — число тестов ShowcaseTest).

## ДО СПАВНА (оркестратор)
`$T` — `T="$(cygpath -m "<scratchpad сессии>")/a1s"; mkdir -p "$T"`; `A=tools/adbui.sh`; `SRC=finny-pet/app/src`;
`GD=$SRC/game/java/ru/finny/pet/game`. Прототип автора — `<scratch автора>/a1s/wt` (worktree на 470db8b, не удалять до
ревью; в нём же — `wt/.claude/task-scope.json` от `scope.py`), эталоны — `…/a1s/ref/{PlaceScreen,Showcase}.kt`, дифф —
`…/a1s/proto.diff`, машинная приёмка — `…/a1s/acc/acc.sh`; `<scratch автора>` — scratchpad сессии 4e5b0844
(`C:/Users/SINGUL~1/AppData/Local/Temp/claude/C--Users-Singularity-Documents-Claude-lct-hackaton-2026-case-06/4e5b0844-ab9a-43f7-ac12-0d10ab1c1887/scratchpad`).
1. Спека → `docs/tasks/TOWN-A1s.md`; маршрут — правка `tools/town_route.sh` (:152 комментарий, :173 две проверки
   `marketlimit`, текст — «Зонды приёмки», `route.diff`; файл в protect кодера — правит оркестратор); один коммит
   `docs(TOWN-A1s): спека витрины лавки; маршрут — «Начать смену» нет после лимита …`.
2. Номера строк: `grep -n '^private fun ShopPlace\|^private fun ItemCard\|^private fun PayPanel\|^// ---------- JOB' $GD/screens/PlaceScreen.kt`
   → 123 / 230 / 268 / 307 (у `@Composable` — на 1 меньше: 122 / 229 / 267; над PayPanel ещё KDoc :266, он входит в диапазон § 3).
3. Зонды в `$T` — тексты раздела «Зонды приёмки» (`contract_check.py`, `nodes_check.py`, `a1sp.sh`, `scope.py`) и `acc.sh`
   (копия `<scratch автора>/a1s/acc/acc.sh` → `$T/acc/acc.sh`): `python $T/contract_check.py docs/tasks/TOWN-A1s.md $SRC` на
   BASE → `CONTRACT FAIL` 3 из 3; на прототипе (`…/a1s/wt/finny-pet/app/src`) → `CONTRACT OK 3`; `bash -n $T/acc/acc.sh` → 0.
4. test-author: task-scope с allow `ShowcaseTest.kt`; задание — «TOWN-A1s ORACLE: сигнатура § 1, правила `pages` 1–5 и
   `eventItem` 1–4 и примеры ORACLE; красный = не компилируется на BASE; в отчёте — какой тест ловит какое правило; отчёт по
   CLAUDE.md». Коммит `test(TOWN-A1s): …`; его sha — `O` (для acc.sh и ревьювера) и `base` task-scope кодера.
4а. Сила оракула — мутанты в ОДНОРАЗОВОМ worktree на `O` (не в основном дереве и не в `…/a1s/wt`):
   `git worktree add "$T/mut" $O` (+ `local.properties`), `cp <scratch автора>/a1s/ref/Showcase.kt "$T/mut/$SRC/main/java/ru/finny/pet/domain/town/"`,
   эталон: `./gradlew testGameDebugUnitTest --tests '*ShowcaseTest*' --console=plain` → в
   `build/test-results/testGameDebugUnitTest/TEST-*ShowcaseTest.xml` failures 0, errors 0, tests K. Затем по одному мутанту
   (правка одной строки эталона, прогон той же командой, возврат эталона):
   - M1 (прижатие вправо): `row + List(PER_ROW - row.size) { null }` вместо `List(PER_ROW - row.size) { null } + row`;
   - M2 (отдел с новой доски): `shelf.sortedBy { it.item.category }.chunked(PER_ROW) { row -> List(PER_ROW - row.size) { null } + row }.chunked(ROWS)`
     вместо `Category.entries.flatMap { … }.chunked(ROWS)`;
   - M3 (приоритет setup): в `eventItem` ветки `?:` местами — исходы раньше setup;
   - M4 (`BUY_AT` и нужда — не товар): второй строкой `e.outcomes.firstNotNullOfOrNull { (it.fact as? Fact.Buy)?.let { b -> (b.target as? BuyTarget.Item)?.id } ?: (it.fact as? Fact.BuyAt)?.item }`.
   Ожидание по XML: у каждого мутанта failures ≥ 1, errors = 0, и среди упавших — тест, который отчёт test-author назвал для
   этого правила (WORKFLOW № 37). Выживший мутант — FAIL оракула: задача возвращается test-author с именем правила. После —
   `git worktree remove --force "$T/mut"`.
5. task-scope кодера: `python $T/scope.py . $O > .claude/task-scope.json` (allow 2 пути, protect — 65 путей на 470db8b;
   allow ∩ protect — assert скрипта); `node .claude/hooks/guard-paths.test.js` → exit 0.
6. Задание кодеру: «TOWN-A1s: CONTRACT § 1–3 дословно (неразрывный пробел — escape `\u00A0`), § 4 — импорты, § 5 — не решать;
   самопроверка — `./gradlew assembleGameDebug lintGameDebug testGameDebugUnitTest` из finny-pet/ → exit 0 и
   `python <$T>/contract_check.py docs/tasks/TOWN-A1s.md finny-pet/app/src` → `CONTRACT OK 3`».

## ACCEPTANCE (гоняет оркестратор; машинные — `O=<sha оракула> GRADLE=1 bash $T/acc/acc.sh 2>&1 | tee $T/acc/acc_out.txt`)
Машинные — **34 проверки** (п. 0–9 ниже, номера проверок acc.sh — в скобках): acc.sh печатает по строке PASS / FAIL на
каждую и `ИТОГ PASS=a FAIL=b ВСЕГО=c`; c = 34 (WORKFLOW № 49), иначе «СВЕРКА FAIL» — дефект скрипта, недостающие пункты —
руками и в журнал. Живые п. 10–13 — в журнале отдельно. Команды 1–9 — из finny-pet/ (acc.sh делает `cd` сам).
0. из корня: `git add -N finny-pet/app/src/main/java/ru/finny/pet/domain/town/Showcase.kt` — кодер не коммитит, без этого
   `git diff` не видит новый файл (WORKFLOW № 20); acc.sh делает это первым. Метка меняет только индекс, коммит
   оркестратора её поглотит.
1. (1–2) `./gradlew testClassicDebugUnitTest --rerun --console=plain` → exit 0; по XML tests 600 + K, failed 0, skipped 0.
2. (3–5) `./gradlew testGameDebugUnitTest --rerun --console=plain` → exit 0; 600 + K / 0 / 0; ShowcaseTest — K ≥ 1 / 0.
3. (6–7) `./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease` → exit 0; `./gradlew compileGameDebugKotlin
   --rerun --console=plain | grep '^w: ' | grep -v '/test/'` → ровно одна строка `PlaceScreen.kt:210:89 Expression is unused.`
   (давняя, `"Да" to { …; Unit }` в `ShopPlace`; на BASE — `:184:89`).
4. (8–13) `./gradlew lintClassicDebug lintGameDebug` → exit 0; ошибок 0 в каждом варианте; предупреждений без сетевых —
   команда ACCEPTANCE_PRESETS.md:50-53 (`grep ': Warning:' …/lint-results-<variant>.txt | grep -vcE
   '\[(GradleDependency|AndroidGradlePluginVersion|NewerVersionAvailable)\]'`) → game 5, classic 5 (как BASE);
   `grep -c 'UnusedResources' app/build/reports/lint-results-gameDebug.xml` → 0.
5. (15–17) `git diff --name-only 470db8b -- app/src/main/ app/src/classic/ app/src/game/res/ app/src/game/AndroidManifest.xml
   '*.gradle.kts' gradle/ gradle.properties gradlew gradlew.bat` → ровно `finny-pet/app/src/main/java/ru/finny/pet/domain/town/Showcase.kt`;
   `git show --name-only --format= $O -- app/src/test/` → ровно `finny-pet/app/src/test/…/domain/town/ShowcaseTest.kt`;
   `git diff --name-only $O -- app/src/test/` → пусто (до коммита кодера — рабочее дерево; после — `$O HEAD`).
6. (18–24) `git status --porcelain -uall -- finny-pet/app/src` до коммита кодера → только 2 пути allow (` M …/PlaceScreen.kt`,
   ` A …/Showcase.kt`; строка `??` — пропущен шаг 0); `git diff --name-only 470db8b -- app/src` ⊆ allow ∪ ShowcaseTest.kt;
   `node .claude/hooks/assert-oracle-intact.js` → exit 0; счёт диффа (БЮДЖЕТ) → ≤ 450 / ≤ 75; `git diff -w 470db8b --
   $SRC/game | grep '^+import '` → ровно 36 строк § 4 (множество), `grep '^-import '` → ровно `Hyphens` и `LineBreak`.
7. (25–26) `python $T/contract_check.py docs/tasks/TOWN-A1s.md $SRC` → `CONTRACT OK 3` (блоки посимвольно, CRLF → LF);
   литерал U+00A0 в добавленных строках диффа → 0 (в CONTRACT — escape `\u00A0`; 2 давних литерала в `tag()` — BASE).
8. (27–34) греп по коду без строк import (№ 45), `P=$GD/screens/PlaceScreen.kt`, `S=$SRC/main/java/ru/finny/pet/domain/town/Showcase.kt`:
   `grep -v '^import' $P | grep -cE 'ItemCard|PayPanel'` → 0; `grep -v '^import' $P | grep -c 'Showcase\.pages('` → 1;
   `grep -v '^import' $P | grep -c 'Showcase\.eventItem('` → 1; `grep -c 'такая распродажа бывает' $P` → 0;
   `grep -c '📦' $P` → 0; `grep -cE 'android|java\.util\.Random|System\.' $S` → 0 (сырой файл, с import: чистоту
   домена выдают именно строки import — исключение из № 45); `grep -rnE '^import .*\*$' $GD $SRC/main/java` → пусто;
   `grep -c '"Начать смену"' $P` → 3 (TrayJobScene, OrderCard, витрина).
9. (14) release `game`: `stat -c%s finny-pet/app/build/outputs/apk/game/release/app-game-release.apk` − 4 637 942 ≤ 32 768
   (прототип +16 384); остаток эпика = 5 130 069 − размер (прототип 475 743) — в отчёт и TOWN-A1.md:70-72.
10. **живая — маршрут** (эмулятор `finni`, 360 × 640 — `$A wm360`, `mute`; debug п. 3 — `install -r` путём `cygpath -w` (стоит release —
    подпись другая: сначала `$A uninstall ru.finny.pet`), md5 установленного `base.apk` = локальному, № 33, — в лог; `ps -ef | grep -E "[t]own_route|[b]akery_states|[a]1sp"` пусто):
    `DUMP=1 tools/town_route.sh a1sr 2>&1 | tee "$T/a1s_route.log"` (префикс `a1sr` — glob `emu_a1s_*` п. 11 его не берёт) →
    `grep -cE "not found|gate not passed|GEOM FAIL|CLOSE FAIL"` → 0; `grep -c "GEOM OK"` → 8; `grep -c "CLOSE OK"` → 2;
    `grep -cE "Обрезано: [1-9]"` → 0. «Начать смену» нажимается ТЕКСТОМ после `swipe_up 4` при 1,0 (:152-155); после лимита
    смен (:174-175) — якорь `has "Свежее с утра! Заглядывай."` и отрицательная проверка «нет узла с текстом «Начать смену»»:
    обе печатают «not found» при нарушении и входят в тот же счётчик → 0 (№ 49: число вердиктов п. 10 не меняется).
11. **живая — кадры витрины**: `rm -f finny-pet/screenshots/emu_a1s_*` (хвосты прерванного прогона), затем из корня
    `SC="$T/shots" bash $T/a1sp.sh a1s setup shop plan kassa events c3` (новый демо-профиль — только эмулятор; кадры
    `emu_a1s_*.png` + дампы `.xml` в finny-pet/screenshots, копии в `$T/shots`) → в логе нет «not found» (в т. ч. проверки
    R3 без кадров: «Назад» и сброс страницы при смене лавки); 46 кадров: «Обрезано: 0», кроме market115 / foma115 / order115 —
    «Обрезано: 1 — Выбери мечту» (HUD-1, R9, BACKLOG).
    `python $T/nodes_check.py finny-pet/app/src/main/assets/content/content.json finny-pet/screenshots/emu_a1s_*.xml` →
    `NODES OK N (FRAMES 46)` (прототип — 201 узел): товар — один кликабельный узел с описанием R8; товаров на кадре при 1,0
    ровно 8 (рынок) / 9 («У Фомы»), при 1,15 / 1,3 — от 1 до этого числа; в кадрах кассы — 0 узлов товаров (витрина под кассой
    вне дерева TalkBack) и «Закрыть кассу» 144 × 144 px (48 dp) — и после прокрутки тела (`*e`); касса «Каша» (`kassa_ok*`) —
    `pay_body scrollable="false"`. Строки `KASSA` (прокрутка тела остальных касс: прототип — true у 18 из 22 дампов касс) —
    в журнал: принятое отступление от № 38 до ответа № 112. Мутанты зонда (прототип — FAIL 4 из 4): узел «Каша» без « монет»
    (7 из 8), нет «Закрыть кассу», узел товара под кассой, `pay_body` «Каши» прокручивается.
12. `python tools/ui_measure.py <дамп> <кадр> 3` по дампам `emu_a1sr_*.xml` (п. 10) и `emu_a1s_*.xml` (п. 11) → `small_targets`
    пусто, «Обрезано: 0», `offscreen: []` — **кроме** (1) узлов, срезанных краем окна прокрутки витрины или тела кассы
    (прототип: кнопка «Начать смену» у низа при Пк3 — 29 dp, «Назад» у низа с плакатом C1 — 34 dp, нижний вариант тела кассы
    C3 — 46 dp, первая доска после прокрутки при 1,3 — 45 dp); (2) узлов в пределах 24 dp (72 px) за краем окна прокрутки:
    Compose расширяет границы касания узла у края обрезки на половину мишени 48 dp (order13e / pk3_13e: верх узлов первой доски
    556 px при окне от 628; эффект есть и на BASE — `emu_e2_foma_p2.xml`); узел, заходящий дальше 24 dp, — ошибка. Исключение
    как TOWN-A1g2.md:771-775. Слот товара — от 89 × 115 dp.
13. **глазами** (листы — `python` склейка `emu_a1s_*` в `finny-pet/screenshots/town/a1s_gate_*.jpg`): доски и прилавок как на
    макетах 1–3; товары крупно, стоят на досках (миска, каша, мыло не висят), у шарика нет подложки; ценники с монетой и
    числом, крышка в углу; «Хочу» с новой доски; продавец целиком виден на первом экране при 1,0 (при Пк3 — ступни у края,
    № 114); облачко Марты над головой при 1,0, на полу при 1,15 и 1,3, в облачке нет разрыва слова и висящих «с», «в»;
    облачко Фомы целиком при 1,0; «Ещё 9» с малиновым треугольником на первом экране «У Фомы» при 1,0, «Назад» — треугольник
    перед словом; плакат Пк3 над банкой супер-корма, хвостик к ней; C3 — плакат «В витрине новинка — …» над змеем; распродажа —
    зачёркнутая цена; на рынке нет миски без ценника (стенка за 2-й доской); касса — название и ✕ остаются при прокрутке,
    крышка + слово, цена, облачко «+N», питомец 80 dp и при 1,3; стеллаж фона «У Фомы» закрыт; эмодзи нет («Эмодзи: 0» в
    дампах). Подпись под кадром Пк3 на листе: «Пк3: эффект (+40 у обоих кормов) — в кассе, как решено в № 39 а; на полке
    только цены».

## ЖИВАЯ ПРОВЕРКА (человек и оркестратор после приёмки)
TalkBack на release (узел товара — «название, N монет, нужно/хочу, Стат +N»; плакат — «заголовок. вступление»; облачко
Марты — «Помочь Марте. Марта просит… 6 за три поручения. Смены: n из 3»; продавец без узла; кнопка страницы с событием —
«Ещё 9, Распродажа робота»; при открытой кассе полка не читается); S23 при 1,0 и 1,3 — рынок с Пк3 и Мартой, «У Фомы» обе
страницы, касса «Вкусный обед» и касса робота C1 (худший случай: три варианта, «Ещё ▾», «Пройти мимо»); `tools/perf.sh` на
входе в лавку (строка эпика). Владелец смотрит листы finny-pet/screenshots/town/; пуш 1.4.2 — после его «да».

## ВОПРОСЫ НА ВОРОТА (GATE_QUEUE — раздел 13 «Витрина — TOWN-A1s» (на 50b77a7 последний — 12), № 110–115, пачкой; в §18 номера 110–119 на 50b77a7 свободны — сверить перед записью)
**№ 110. Доски и ценники — Compose или Blender.** а) Compose по формулам макета (как сейчас: вид одобренного макета, 0 Б
ассетов; release +16 384 Б за весь код A1s); б) генератор `shopprops.py` (доска, ценник, прилавок, стенка — 4–5 WebP ≈ 20–40 КБ,
круг арта, листы). **Рекомендую а**: макет одобрен именно в этом виде, б даёт тот же вид дороже и позже сдачи.
**№ 111. Задник рынка за витриной.** Фон места неподвижен, в `bg_market_port` нарисованы прилавок (верх ≈ 386, стенка до
485 dp на 360 × 640) и миска корма; при шрифте > 1,0 и при прокрутке миска вставала в пустой слот доски — «товар без ценника»
(кадры прототипа до правки market13, order13e). Сделано: светло-мятная стенка витрины за 2-й доской и прилавком (0 Б), ряд 1 —
на небе. На S23 (расчёт: фон ×1,219, прилавок фона ≈ 470..591 dp, витрина ≈ 439..554 dp; не снято — кадр в живой проверке)
стенка закрывает прилавок фона в основном. а) принять стенку; б) перерендер `bg_market_port` без прилавка и миски
(`place.py`, Blender ≈ 45 КБ, ворота фона) — небо и река за всеми рядами, как в макете 1. **Рекомендую а** до сдачи, б — после,
если владелец захочет видеть реку за досками.
**№ 112. Касса на 360 × 640.** Строка названия с ✕ закреплена, прокручивается тело кассы. Прокрутка тела при 1,0 — в 5 видах
из 6: «Вкусный обед» (не хватает) 80 dp, «Домик-палатка» («Хочу») 183, Пк3 33 (прокручивается и при «хватает», «Пройти
мимо» у края), змей C3 177, робот C1 205; без прокрутки — только простая покупка при «хватает» («Каша»). При 1,3: «Вкусный
обед» 77, Пк3 74, палатка 247, C1 275. У C1 до прокрутки виден 1 вариант из 4 («Дешевле»), «Подождать», «Ещё ▾» и «Пройти
мимо» (исход события) — после прокрутки; при 1,3 до прокрутки вариантов нет. У C1 и C3 умолчание `WEEK_END → SKIP → GOOD`,
так что скрытая прокруткой «Пройти мимо» не отнимает верный исход. Питомец в кассе 80 dp (макет — ≈ 67 × 82 dp фигура) и
при 1,3 сохраняет размер. Касса под вкладками, как на макете 4 (прежняя `PayPanel` стояла от 56 dp, над HUD-2; дампа BASE с
открытой кассой нет — «как раньше» не утверждаю). S23 (оценка, не проверено): окно ≈ 560–567 dp, касса C1 ≈ 630 dp —
прокручивается и там, остальные помещаются. а) принять (прокрутка тела, ✕ закреплён); б) ужать ряд «товар · облачко ·
питомец» (−≈ 30 dp) и вступление события в кассе одной строкой; б′) поднять исходы события (Skip) над вариантами оплаты или
сделать «Подождать» у товара события равным «Пройти мимо» (касса на 48 dp короче; не проверял); в) касса поверх вкладок и
HUD-2 (подъём ≈ 141 dp, прокрутку C1 не убирает, банки HUD-2 закрыты — расходится с макетом 4). **Рекомендую а**.
**№ 113. Текст облачка Марты.** Макет: «Помоги с ящиками! 🪙 +12» — такого текста в content нет, «+12» не совпадает с платой
(6). Сейчас: «Помочь Марте» (заголовок заказа) и «🪙 +6» (`shiftQuote.base`), вступление, строка платы и смены — в TalkBack
(в 1.4.1 «6 за три поручения» и «Смены: n из 3» были видны глазами). а) принять; б) короткая реплика заказа в content от
первого лица (новое поле — оракул test-author) и текст «Помоги с ящиками!»; в) жетоны смен `ShiftTokens(label = false)` в
ряд с «+6» (≈ 36 dp, облачко растёт не больше чем на строку; «за три поручения» — жетонами) — принимать по снимкам 360 × 640
при 1,0 и 1,3 (облачко не перекрывает ценник и товар первой доски). Вступление «Марта просит…» в облачко не выводить — оно в
третьем лице и растит облачко на ≈ 36–54 dp. **Рекомендую а** (или в по листу).
**№ 114. «Начать смену» и ступни Марты на 360 × 640.** Без плаката при 1,0 всё на первом экране. С плакатом Пк3 (2 строки)
при 1,0 кнопка — после прокрутки, ступни Марты у края; при 1,15 и 1,3 облачко Марты на полу, кнопка — после прокрутки, при
1,3 вместе с Пк3 после прокрутки весь заказ (кадры pk3_10, order115, order13, pk3_13). S23 — по оценке всё на первом экране
при 1,0 (запас ≈ 140 dp), проверим снимком. а) принять; б) облачко Марты — кнопкой (в слот над головой облачко с кнопкой не
встаёт). **Рекомендую а**.
**№ 115. Картинка плаката Пк3 (`poster_food_super.webp`, 9 468 Б, № 96 б).** Витрина рисует плакат по макету 1 — словами, без
картинки; `posterRes` зовётся только из `PlaceEvents` мест-работ, событий с плакатом там нет. а) оставить WebP и `posterRes` для
будущих событий мест-работ; б) удалить `posterRes` и WebP (−9 468 Б, правка `LICENSES.md:55`, `ARCHITECTURE.md:76`,
`CONTENT_MAP.md:269`); в) вернуть картинку 40 dp внутрь плаката над банкой (плакат выше, № 114 хуже). **Рекомендую а** до
сдачи — ноль правок; б — вместе с чисткой ассетов после.

## ТЕКСТЫ ДЛЯ РЕБЁНКА (на утверждение оркестратору)
Новые в коде: «Ещё N» (N — товаров на следующей странице; в макете и в решении № 39 а — «Ещё 9 ▶», стрелка теперь нарисована)
и «Назад» (решение № 39 а — «◀ Назад»). Слова «Нужное» / «Хочу» / «Запас» в кассе — те же, что в HUD-2; «Начать смену»,
«Пройти мимо», «Разложить», «Ещё ▾», «Закрыть кассу» — прежние. Облачка и плакат — из content: «Помочь Марте», «Свежее с утра!
Заглядывай.», «Добро пожаловать в лавку!», «Супер-корм! Все питомцы в восторге!» (из вступления Пк3), «Привоз задержался —
корм подорожал», «Распродажа робота», «В витрине новинка — воздушный змей за 30.» (первое предложение вступления C3); состояние
кнопки страницы для TalkBack — заголовок события из content. Тексты макета, которых в коде НЕТ (нужно решение, № 113): «Помоги
с ящиками!», «+12», «Добро пожаловать!», «Все в восторге!». Убрана строка кода «такая распродажа бывает» (её говорит
вступление C1).

## ДОКИ (оркестратор, после приёмки и живой, ДО ревью — № 46; сверить строки грепом)
- `finny-pet/docs/UX_ACCESSIBILITY.md`: :49 «Товар на полке … 104 dp … две колонки … `ItemCard`» → «Товар на доске: слот ≈ 89..107
  × 115..123 dp (картинка 66 × 60 dp по непрозрачной части + ценник), по 3 на доске, страницы по 9; «Ещё N» / «Назад» 48 dp
  с нарисованной стрелкой; плакат события ≥ 48 dp — `PlaceScreen.kt`, `ShopScene`»; :50 `PayPanel` → касса-ценник `Kassa`
  (строка названия с ✕ закреплена, тело прокручивается; при шрифте > 1,0 в облачке влияния — значок и «+N»); :105 строка
  «Полка лавки (`ItemCard`) | названия» в «Исключения: 14 sp» — удалить (названий на витрине нет, 14 sp нет); :121 «метка
  «🍎 нужно» / «🎈 хочу» — только словом» → отдел — крышкой на ценнике, в кассе — крышка + слово; :130 «Категория расхода»: «на товаре — «🍎 нужно» / «🎈 хочу»» → «на ценнике — крышка
  отдела, в кассе — крышка и слово»; :363 «товар на полке —
  карточка» → «узел товара на доске» (описание прежнее); :365–367 фразу «Плакат события 64 dp (`posterRes`, Пк3) — … «Пройти
  мимо»» заменить на «Плакат события над товаром (`Poster`, `ShopScene`) — один узел «заголовок. вступление», «Пройти мимо» —
  в кассе товара; картинки 64 dp в лавке нет»; узел продавца убран (без семантики); при открытой кассе витрина вне дерева
  TalkBack; кнопка страницы с событием — `stateDescription` = заголовок события.
- `finny-pet/docs/REQUIREMENTS_MATRIX.md`: :64 (2.5.5) «на товаре — «🍎 нужно» / «🎈 хочу»» → «на ценнике — крышка отдела, в
  кассе — крышка и слово»; :65–66 «карточка … метка «🍎 нужно»…; касса: «{товар} · …»» → «витрина: ценник на
  ниточке (цена, крышка отдела; распродажа — зачёркнутая цена), касса-ценник: название, отдел (крышка + слово), цена,
  «+N» влияния, варианты»; :167 (3.6, последняя колонка) «карточка заказа Марты на рынке стоит под полкой…» → «заказ Марты —
  облачко над Мартой, «Начать смену» у её ног; при Пк3 и при шрифте > 1,0 — после прокрутки (№ 114)»; :171 «категория
  товара словом («🍎 нужно» / «🎈 хочу»)» → «отдел товара — крышкой на ценнике, в кассе — крышка и слово»; :169 (перечень
  14 sp) — без полки.
- `docs/BACKLOG.md`: п. 13 (:83) → «частично (A1s): облачко заказа над Мартой, «Начать смену» у её ног. На 360 × 640 при 1,0
  без плаката — на первом экране; при Пк3 и при шрифте > 1,0 (облачко на полу) — после прокрутки, при 1,3 вместе с Пк3 за
  краем весь заказ (№ 114); на S23 — по оценке на первом экране. Если владелец выберет № 114 б — закрыть»; п. 25 (:95) →
  «Закрыто» (строки «такая распродажа бывает» на полке нет, вступление C1 — в кассе). Новые пункты: (1) событие лавки без
  товара на полке витрина не показывает, «!» улицы ставится по любому событию места (ANTI-SCOPE A1s; включать Пк2 — со сценой
  среза 2); (2) `eventItem` не смотрит на лавку эффекта (`PRICE(a, shop_market)` + `PRICE(b, shop_foma)` повесит плакат над
  `a` в обеих лавках; живых таких нет); (3) неразрывный пробел после однобуквенного слова — только облачко продавца, остальные
  облачки — общей правкой `TText`; (4) подпись HUD-1 «Выбери мечту» режется при шрифте 1,15 («Выбер/и», «Обрезано: 1»,
  кадры a1s market115 / foma115 / order115; HUD-1 не менялся с BASE).
- `finny-pet/docs/LIMITATIONS_ROADMAP.md`: :35 п. 19 не удалять — заголовок → «Кнопка заказа Марты при крупном шрифте и
  плакате — ниже края»; «как сделано» — облачко заказа над Мартой и «Начать смену» на полу витрины (`ShopScene`,
  `PlaceScreen.kt`) вместо `OrderCard` и `PlaceScreen.kt:159`; последствие — «на 360 × 640 при Пк3 и при шрифте > 1,0 «Начать
  смену» — после прокрутки, при 1,3 вместе с Пк3 — весь заказ (№ 114 а); на S23 и при 1,0 без плаката прокрутка не нужна»;
  :56 «осталось — витрина лавки (A1s)» — убрать.
- `docs/GAME_CONCEPT.md`: :130-131 «Тап по товару — карточка: цена, «нужно» или «хочу» словом и значком…» → касса-ценник;
  :1007 «На ценнике написано «такая распродажа бывает»» → «ценник зачёркнут, в кассе — вступление C1»; §18 строка 39 — отметка
  «сделано A1s»; строки № 110–115 (в таблице §18 — перед № 120) — после ответа.
- `finny-pet/docs/TEST_CASES.md`: :111 TC-28 «прокрутить рынок вниз: карточка заказа под полкой → «Начать смену»» → «на рынке
  облачко «Помочь Марте +6» над Мартой, «Начать смену» у её ног (при Пк3 и при 1,3 — после прокрутки)»; :112 TC-29 «Касса:
  «Корм · 20 · 🍎 нужно · Сытость +40»» → вид кассы-ценника; `finny-pet/docs/BUILD_AND_DEMO.md`:201 — та же строка кассы; TEST_CASES :140 «Известное»:
  «карточка заказа Марты стоит под полкой рынка, после «К работе» её ищут прокруткой» → «при Пк3 и при шрифте > 1,0
  «Начать смену» у Марты — после прокрутки (№ 114)».
- `finny-pet/docs/ARCHITECTURE.md`:91 «Лавка с кассой … PlaceScreen.kt» — плюс строка `domain/town/Showcase.kt` (раскладка витрины,
  оракул ShowcaseTest); :76 (`posterRes` — функция TownUi.kt) остаётся правдой.
- `tools/art_check.py:180` (LEAK_SNAPS: «окно фона целиком под карточками») — допущение для новых снимков ложно; известное
  ограничение, строка в журнал эпика.
- `docs/tasks/TOWN-A1.md`: :44 статус A1s; :70-72 факт APK и остаток; `docs/tasks/GATE_QUEUE.md` — раздел 13 с № 110–115;
  `docs/HANDOFF.md`; CHANGELOG 1.4.2.
- Проверка: `git grep -nE "ItemCard|PayPanel|104 dp|🍎 нужно|под полкой|такая распродажа бывает|posterRes|Плакат события 64 dp|пустым
  узлом перед заголовком" -- docs finny-pet/docs ':!docs/tasks'` → только строки, которые остаются правдой (номера — 50b77a7; после
  правок ДОКИ сверять по тексту): `ARCHITECTURE.md:76` и `:174`, `CONTENT_MAP.md:269`, `GAME_CONCEPT.md:1988` (`posterRes` —
  функция TownUi.kt, остаётся), `GAME_CONCEPT.md:1276` («такая распродажа бывает» — вступление C1, его показывает касса
  робота), заголовки пунктов 13 и 25 `docs/BACKLOG.md` (история замечания; правится статус). Прочие строки грепа на
  50b77a7 (24) — в правках выше. `finny-pet/CHANGELOG.md` в область грепа не входит.

## ПРИ БЛОКЕРЕ
`STATUS: BLOCKED` с одним вопросом и остановка, если: блок CONTRACT не встаёт дословно (строки BASE не те — назвать файл и
строку); сборка, lint или ShowcaseTest падают на дословном коде; нужен импорт, файл или правка вне allow. Размеры, цвета,
тексты, семантика продавца — не блокер, а § 5: не делать.

## Зонды приёмки (тексты; копии — `<scratch автора>/a1s/`; acc.sh — `<scratch автора>/a1s/acc/acc.sh`, 34 проверки)
`contract_check.py`:
```python
# contract_check.py — TOWN-A1s: каждый блок CONTRACT (```kotlin сразу после строки «<!-- file: PATH -->») — непрерывная
# подстрока файла SRC/PATH (CRLF → LF), то есть код вставлен дословно, с отступами. SRC — finny-pet/app/src.
# python contract_check.py SPEC SRC → CONTRACT OK N / CONTRACT FAIL [...], exit 0/1
import os, re, sys
spec = open(sys.argv[1], encoding='utf-8').read().replace('\r\n', '\n')
blocks = re.findall(r'<!-- file: (\S+) -->\n```kotlin\n(.*?)```', spec, re.S)
src = lambda f: open(os.path.join(sys.argv[2], f), encoding='utf-8').read().replace('\r\n', '\n') if os.path.exists(os.path.join(sys.argv[2], f)) else ''
bad = [f + ': ' + code.splitlines()[0].strip()[:70] for f, code in blocks if code not in src(f)]
print('CONTRACT OK %d' % len(blocks) if blocks and not bad else 'CONTRACT FAIL %s' % (bad or 'no blocks'))
sys.exit(1 if bad or not blocks else 0)
```
`nodes_check.py`:
```python
# nodes_check.py — TOWN-A1s R8 по дампам uiautomator кадров emu_<PREFIX>_<кадр>.xml (360 × 640, 3 px = 1 dp):
# витрина — каждый товар ОДНИМ кликабельным узлом с описанием «{название}, {цена} монет, {нужно|хочу}, {Стат} +N[, …]»
# по content.json, товаров на кадре: рынок (market*, order*, pk3_*) 8, «У Фомы» (foma*, c1*, c3*) 9 — ровно при 1,0, при 1,15 / 1,3
# (*115, *13, *13e) от 1 до этого числа (доска за краем окна в дамп не попадает); касса (*kassa*) —
# 0 узлов товаров (витрина под кассой вне дерева TalkBack) и узел «Закрыть кассу» 48 × 48 dp; касса «хватает» (kassa_ok*)
# без прокрутки тела (pay_body scrollable="false"); у остальных касс прокрутка печатается строкой KASSA (№ 112).
# python nodes_check.py CONTENT DUMP... → NODES OK N (FRAMES k) / NODES FAIL [...], exit 0/1
import json, os, re, sys
c = json.load(open(sys.argv[1], encoding='utf-8'))
items = {i['title']: i for i in c['items'] + c['town'].get('items', [])}
word = {'MANDATORY': 'нужно', 'OPTIONAL': 'хочу'}
stats = [('hunger', 'Сытость'), ('clean', 'Чистота'), ('mood', 'Настроение')]
bad, n, k = [], 0, 0
for path in sys.argv[2:]:
    frame = re.sub(r'^emu_[^_]+_|\.xml$', '', os.path.basename(path))
    exp = 0 if 'kassa' in frame else 8 if frame.startswith(('market', 'order', 'pk3')) else 9 if frame.startswith(('foma', 'c1', 'c3')) else None
    if exp is None: bad.append(f'{path}: unknown frame «{frame}»'); continue
    k += 1; seen = {}
    ns = re.findall(r'<node [^>]*>', open(path, encoding='utf-8').read())
    for node in ns:
        d = re.search(r'content-desc="([^"]*)"', node).group(1)
        if ' монет' not in d or d.startswith(('Кошелёк', 'Копилка')): continue
        title = d.split(', ')[0]; it = items.get(title)
        if it is None: bad.append(f'{path}: unknown item «{title}»'); continue
        eff = ', '.join(f'{w} +{it[s]}' for s, w in stats if it.get(s))
        want = re.escape(title) + r', \d+ монет' + (', ' + word[it['category']] if it['category'] in word else '') + (', ' + re.escape(eff) if eff else '')
        if not re.fullmatch(want, d): bad.append(f'{path}: «{d}»')
        if 'clickable="true"' not in node: bad.append(f'{path}: not clickable «{title}»')
        seen[title] = seen.get(title, 0) + 1; n += 1
    bad += [f'{path}: {t} × {m}' for t, m in seen.items() if m > 1]
    # at 1.15 / 1.3 a whole board can be below the window — uiautomator leaves its nodes out: at most, not exactly
    got = sum(seen.values()); loose = exp and re.search(r'(13e?|115)$', frame)
    if (got > exp or got == 0) if loose else got != exp: bad.append(f'{path}: {got} items, want {"≤ " if loose else ""}{exp}')
    if exp == 0:
        box = lambda node: list(map(int, re.findall(r'\d+', re.search(r'bounds="([^"]*)"', node).group(1))))
        x = [box(m) for m in ns if 'content-desc="Закрыть кассу"' in m]
        if len(x) != 1 or x[0][3] - x[0][1] != 144 or x[0][2] - x[0][0] != 144: bad.append(f'{path}: «Закрыть кассу» {x} — want one 144 × 144 px')
        body = [m for m in ns if 'resource-id="pay_body"' in m]
        sc = re.search(r'scrollable="(\w+)"', body[0]).group(1) if body else 'нет pay_body'
        print(f'KASSA {frame}: pay_body scrollable={sc}')
        if frame.startswith('kassa_ok') and sc != 'false': bad.append(f'{path}: pay_body scrollable={sc}, want false')
print(f'NODES OK {n} (FRAMES {k})' if k and not bad else 'NODES FAIL %s' % (bad or 'no frames'))
sys.exit(1 if bad or not k else 0)
```
`scope.py`:
```python
# scope.py — TOWN-A1s: .claude/task-scope.json кодера по SCOPE спеки (allow 2 пути; protect — базовый + поимённо всё
# остальное в game/java и app/src/main, game/res, манифест game, test, classic, finny-pet/docs, finny-pet/tools, tools).
#   python scope.py ROOT BASE_SHA > ROOT/.claude/task-scope.json   (ROOT — корень репозитория, BASE_SHA — коммит оракула)
import json, os, subprocess, sys
root, base = sys.argv[1], sys.argv[2]
allow = ['finny-pet/app/src/game/java/ru/finny/pet/game/screens/PlaceScreen.kt',
         'finny-pet/app/src/main/java/ru/finny/pet/domain/town/Showcase.kt']
basic = ['finny-pet/app/src/test/', 'finny-pet/app/build.gradle.kts', 'finny-pet/build.gradle.kts', 'finny-pet/settings.gradle.kts',
         'finny-pet/gradle/', 'finny-pet/gradle.properties', 'finny-pet/gradlew', 'finny-pet/app/proguard-rules.pro',
         'finny-pet/app/src/main/AndroidManifest.xml', '.claude/', 'CLAUDE.md', 'docs/']   # WORKFLOW «Базовый protect»
files = subprocess.run(['git', '-C', root, 'ls-files', 'finny-pet/app/src/game/java', 'finny-pet/app/src/main'],
                       capture_output=True, text=True, encoding='utf-8').stdout.split()
named = sorted(f for f in files if f not in allow and f not in basic and not f.startswith('finny-pet/app/src/main/res/')
               and not f.startswith('finny-pet/app/src/main/assets/'))
protect = basic + ['finny-pet/app/src/game/res/', 'finny-pet/app/src/game/AndroidManifest.xml', 'finny-pet/app/src/main/res/',
                   'finny-pet/app/src/main/assets/', 'finny-pet/app/src/classic/', 'finny-pet/docs/', 'finny-pet/tools/', 'tools/'] + named
assert not any(a.startswith(p) or p == a for a in allow for p in protect), 'allow ∩ protect'
print(json.dumps({'task': 'TOWN-A1s — витрина лавки (docs/tasks/TOWN-A1s.md)', 'base': base, 'allow': allow, 'protect': protect},
                 ensure_ascii=False, indent=2))
```
`a1sp.sh` (кадры п. 11; из корня, `SC=<каталог копий>`):
```bash
#!/usr/bin/env bash
# TOWN-A1s: кадры витрины на эмуляторе 360 × 640 — демо-профиль → рынок, «У Фомы», касса, события при 1,0 / 1,15 / 1,3;
# кадр — emu_PREFIX_<имя>.png + дамп .xml в finny-pet/screenshots, копии в $SC. Запуск из корня репозитория:
#   SC=<каталог копий> bash a1sp.sh PREFIX [шаги...]   шаги: setup shop plan kassa events c3 (go — после переустановки)
# setup создаёт тестовый профиль («Создать тестовый профиль» стирает прогресс) — только эмулятор.
set -u
P=${1:?PREFIX}; shift
STEPS=${*:-setup shop plan kassa events c3}
SC=${SC:?SC — каталог копий кадров}
R=$(git rev-parse --show-toplevel)
A="$R/tools/adbui.sh"
OUT="$(cygpath -m "$R/finny-pet/screenshots")"
mkdir -p "$SC"
find_xy() { "$A" ui | awk -F'\t' -v p="$1" 'index($1,p)==1 || index($2,p)==1 {print $3, $4; exit}'; }
tp() { local xy i; for i in 1 2 3 4 5 6; do xy=$(find_xy "$1"); [ -n "$xy" ] && break; sleep 1.5; done
  [ -z "$xy" ] && { echo "  not found: $1"; return 1; }; "$A" shell input tap $xy; sleep ${2:-1.5}; }
probe() { "$A" ui | grep -o 'Обрезано: [0-9]*' | head -1; }
swipe_up() { for i in $(seq ${1:-3}); do "$A" shell input swipe 540 1450 540 450 300; done; sleep 1; }
shot() { sleep 0.8; "$A" shot ${P}_$1 >/dev/null; cp "$OUT/emu_${P}_$1.png" "$SC/"; "$A" shell uiautomator dump /sdcard/ui.xml >/dev/null
  "$A" exec-out cat /sdcard/ui.xml > "$OUT/emu_${P}_$1.xml"; cp "$OUT/emu_${P}_$1.xml" "$SC/"; echo "$1: $(probe)"; }
font() { "$A" font $1; sleep 2.5; }
gate() { local q a b j
  for j in $(seq 10); do q=$("$A" ui | grep -oE '[0-9]+ × [0-9]+ = \?' | head -1); [ -n "$q" ] && break; sleep 1.5; done
  a=${q%% ×*}; b=${q#*× }; b=${b%% =*}; tp "Ответ" 1.5; "$A" text $((a * b)); sleep 1; tp "Войти" 2.5; }
nudge() { "$A" shell input swipe 540 1450 540 1050 600; sleep 1; }   # ≈ 400 px without a fling: a long swipe skipped cards
scroll_to() { local k; for k in $(seq 15); do [ -n "$(find_xy "$1")" ] && return; nudge; done; }
plan() { tp "Банки"; for i in 1 2 3 4; do tp "Нужное: " 0.3; done; for i in 1 2; do tp "Хочу: " 0.3; done; tp "В копилку: " 0.3; tp "Готово" 1; tp "Да"; }
start_ev() {  # the board's «Начать» of the card titled $1 (demo: every event can be started); a short swipe keeps the title on screen
  local xy k; "$A" tap "Домой" >/dev/null 2>&1; sleep 1.5; tp "События"; scroll_to "$1"
  for k in 1 2 3; do xy=$("$A" ui | awk -F'	' -v t="$1" '$1==t {f=1} f && $1=="Начать" {print $3, $4; exit}'); [ -n "$xy" ] && break; nudge; done
  [ -z "$xy" ] && { echo "  not found: Начать $1"; return 1; }; "$A" shell input tap $xy; sleep 3; }
to_shop() { tp "Домой"; tp "Лавки"; tp "$1"; }
kassa() { tp "$1" && { shot $2; swipe_up 3; shot ${2}e; "$A" back; sleep 1; }; }   # $1 — item/poster node prefix, $2 — frame

for s in $STEPS; do case $s in
go)     # after a reinstall: title → room → street
  font 1.0; "$A" launch >/dev/null; sleep 3; tp "Продолжить" 2.5; tp "Дверь: на улицу" ;;
setup)
  font 1.0; "$A" launch >/dev/null; sleep 2.5
  tp "Для взрослого"; gate; scroll_to "Создать тестовый профиль (демо)"; tp "Создать тестовый профиль (демо)"; tp "Да, продолжить" 2
  for i in 1 2 3 4 5; do "$A" tap "Дальше" >/dev/null 2>&1 || break; sleep 1; done
  tp "Создать питомца"; tp "Зайка" 0.5; tp "Рыжий" 0.5; tp "Финни" 0.5; swipe_up 1; tp "Начать!" 2.5
  tp "Дверь: на улицу"; tp "Рынок у реки" ;;
shop)   # before the plan: market (demo, 8 items), Foma p1/p2; a font change recreates the screen (page 1, top)
  tp "Рынок у реки"; shot market10; font 1.15; shot market115; font 1.3; shot market13; swipe_up 4; shot market13e; font 1.0
  tp "У Фомы"; shot foma10; font 1.15; shot foma115; font 1.3; shot foma13; swipe_up 4; shot foma13e; font 1.0
  tp "Ещё 9"; shot fomap2_10; font 1.3; swipe_up 4; tp "Ещё 9"; shot fomap2_13; swipe_up 4; shot fomap2_13e; font 1.0
  # R3 without shots: «Назад», and the page back to 1 on a shop change — «Вкусный обед» stands on page 1 only
  tp "У Фомы"; tp "Ещё 9"; tp "Назад"; tp "Вкусный обед, "; "$A" back; sleep 1
  tp "Ещё 9"; tp "Рынок у реки"; tp "У Фомы"; tp "Вкусный обед, "; "$A" back; sleep 1 ;;
plan)   # after the plan: Marta's order
  tp "Домой"; plan; to_shop "Рынок у реки"; shot order10; swipe_up 4; shot order10e
  font 1.15; shot order115; font 1.3; shot order13; swipe_up 4; shot order13e; font 1.0 ;;
kassa)  # pay sheet: affordable (Каша), short (Вкусный обед), «Хочу» (Домик-палатка); «e» — after a swipe inside the sheet
  to_shop "Рынок у реки"; kassa "Каша, " kassa_ok10
  tp "У Фомы"; kassa "Вкусный обед, " kassa_short10
  tp "Ещё 9"; kassa "Домик-палатка, " kassa_want10
  font 1.3; tp "Рынок у реки"; kassa "Каша, " kassa_ok13
  tp "У Фомы"; kassa "Вкусный обед, " kassa_short13
  swipe_up 4; tp "Ещё 9"; swipe_up 4; kassa "Домик-палатка, " kassa_want13; font 1.0 ;;
events) # Пк3 and C1 from the board (demo)
  start_ev "Супер-корм"; shot pk3_10; font 1.3; shot pk3_13; swipe_up 4; shot pk3_13e; font 1.0
  kassa "Супер-корм. " pk3_kassa10; font 1.3; kassa "Супер-корм. " pk3_kassa13; font 1.0
  start_ev "Распродажа робота"; shot c1_10; tp "Ещё 9"; shot c1p2_10; font 1.3; swipe_up 4; tp "Ещё 9"; shot c1p2_13; font 1.0
  to_shop "У Фомы"; tp "Ещё 9"; kassa "Робот, " c1_kassa10; font 1.3; swipe_up 4; tp "Ещё 9"; kassa "Робот, " c1_kassa13; font 1.0 ;;
c3)     # C3 needs 35 in the piggy bank (savingsPct>=70 of the demo dream): 3 × 10 from the reserve, then the kite on page 2
  tp "Домой"; tp "Копилка "; for i in 1 2 3; do tp "В копилку 10 из запаса" 1; tp "Да" 1; done
  start_ev "Мечта почти твоя"; to_shop "У Фомы"; tp "Ещё 9"; shot c3p2_10; font 1.3; swipe_up 4; tp "Ещё 9"; shot c3p2_13; font 1.0
  to_shop "У Фомы"; tp "Ещё 9"; kassa "Воздушный змей, " c3_kassa10 ;;
esac; done
echo "done $P"
```
Правка маршрута (ДО СПАВНА п. 1; `route.diff` рядом со спекой):
```diff
--- a/tools/town_route.sh
+++ b/tools/town_route.sh
@@ -149,7 +149,8 @@ serve 2.0; serve 2.5; has "Заработали " result; has "Почему?" re
 tapx "Почему?" 1.2; [ -n "$(text_xy "Карманные приходят каждую неделю, зарплата — когда поработаешь")" ] || echo "  not found: LINE on why"; shotg why --line; tp "Карманные" 0.8; tapx "Готово" 1.5
 tp "Домой"; plan; shot1 jars
 tp "Домой"; tp "Лавки"; tp "Рынок у реки"; shot2 marketp
-# the order card sits below the fold; a font change recreates the screen and loses the scroll — scroll after each
+# Marta's «Начать смену» is on the floor of the showcase (below the fold with the Пк3 poster or at 1,3); a font change
+# recreates the screen and loses the scroll — scroll after each
 swipe_up 4; shot1 order10; "$A" font 1.3; sleep 2.2; swipe_up 4; shot1 order13; "$A" font 1.0; sleep 1.5; swipe_up 4
 tp "Начать смену" 2; no_hud_hint "Закончить" taps; shot2 taps              # shift 2 of the week — Marta
 tp "Разложить яблоки" 0.4; tp "Подмести у прилавка" 0.4; tp "Отнести ящик" 0.4; tp "Закончить" 2; no_hud_hint "Готово" tresult
@@ -170,7 +171,8 @@ tapx "Закончить" 2; has "Заработали " result2; has "Поче
 has "Смены на неделе закончились — приходи на новой неделе" limit; shot2 limit   # bakery after the limit: ●●●, «Домой» (№ 54 б, 59 б)
 xy=$(text_xy "Домой"); [ -z "$xy" ] && echo "  not found: button «Домой» on limit" || { "$A" shell input tap $xy; sleep 2; }
 "$A" ui | grep -q "Окно: улица" || { echo "  not found: room after «Домой»"; tp "Домой"; }
-tp "Лавки"; tp "Рынок у реки"; swipe_up 4; shot1 marketlimit             # market after the limit: no Marta's order card
+tp "Лавки"; tp "Рынок у реки"; swipe_up 4; has "Свежее с утра! Заглядывай." marketlimit   # market after the limit: Marta's line, no order (№ 62 б)
+"$A" ui | awk -F'\t' '$1=="Начать смену"' | grep -q . && echo "  not found: no «Начать смену» on marketlimit"; shot1 marketlimit
 tp "Домой"; shot1 home_from_place
 tp "Дневник"; has "Помочь Боре: лучшая смена, звёзд " diary; shot1 diary; tp "Назад"   # TOWN-J1-1b2: bestStars
 tp "События"; shot1 board
```

## Журнал спеки
- **2026-09-29, сессия 16 — спека по прототипу** [до круга критиков 1: пейджер тогда был глифами «Ещё 9 ▶» / «◀ Назад», облачко
  продавца в слоте до шрифта 1,15; оба отменены кругом — подтверждённые 8 и 12; действующее — КОНТЕКСТ] (автор; worktree `<scratch автора>/a1s/wt` detached на 470db8b,
  `local.properties` скопирован). Прототип — 2 файла allow (`PlaceScreen.kt`, новый `Showcase.kt`):
  - сборка `./gradlew assembleGameDebug assembleGameRelease lintGameDebug testGameDebugUnitTest` → exit 0; тесты 600 / 0 / 0
    (ShowcaseTest ещё нет — его пишет test-author); предупреждения в изменённых файлах — одно давнее `PlaceScreen.kt:197:89`
    (BASE `:184:89`), в `Showcase.kt` — 0; lint 0 ошибок, 8 предупреждений = 5 + 3 сетевых `GradleDependency` (как BASE),
    `UnusedResources` 0;
  - дифф без import и пустых +372 −67 (`PlaceScreen.kt` +353 −67, `Showcase.kt` +19), импорты +26 −2; `proto.diff` и
    `ref/{PlaceScreen,Showcase}.kt` — рядом со спекой;
  - release: BASE (сборка в wt на 470db8b) 4 637 942 Б = файл выпуска 1.4.1; прототип 4 654 326 Б (+16 384); остаток эпика
    475 743 Б; новых ассетов 0;
  - живая (emulator-5554, 360 × 640 `wm360`, debug md5 установленного `base.apk` = локальному): 30 кадров `emu_a1sp_*` при 1,0
    и 1,3 (рынок до плана, с заказом Марты, с Пк3; «У Фомы» стр. 1–2; касса «Каша» / «Вкусный обед» / «Домик-палатка»;
    касса Пк3 с «Пройти мимо»; распродажа C1 — «!» на «Ещё 9 ▶», плакат над роботом, ценник 40→25, касса робота) —
    «Обрезано: 0» на всех; `nodes_check.py` → `NODES OK 198` (на дампе BASE `emu_e2_market10.xml` → `NODES OK 8`: описание R8
    не изменилось; мутанты — FAIL 3 из 3); `ui_measure` — 0 мелких целей на кадрах без кассы и края прокрутки; на остальных —
    только узлы под кассой (полоски 3–10 dp) и у края окна; маршрут `DUMP=1 tools/town_route.sh a1sp_route` (на той же debug) — промахов 0, `GEOM OK` 8, `CLOSE OK` 2,
    «Обрезано: [1-9]» 0; «Начать смену» нажата по тексту после `swipe_up 4` при 1,0 (дальше `taps`, «Заработали 6: 6 за три
    поручения…»), `marketlimit` — заказа нет, Марта говорит свою реплику; 28 сырых дампов — `ui_measure` мелких целей 0;
    `nodes_check` по 6 дампам лавок (market, foma, marketp × 1,0 / 1,3) → `NODES OK 50`;
  - круги по снимкам (5): облачко «Свежее с утра! Заглядывай.» при 1,0 переносилось посреди слова и наезжало на ценник
    первого ряда → `bodyMedium` с межстрочным 18 sp для реплики, отступ облачка 6 dp; «Ещё 9 ▶» уходил под облачком Фомы на
    вторую строку → облачко на полу ≤ 136 dp при 1,0 (≤ 240 при 1,3 — иначе «пожаловат/ь»); питомец в кассе 64 → 80 dp;
    **узел первого товара прилавка был 41 × 115 dp** — коробку `ResidentPic` 170 dp (она выше слота) Compose вычитает из
    видимой области узла товара под ней → продавец без семантики (`clearAndSetSemantics {}`), после — 0 мелких целей;
    комментарий с эмодзи коробки заменён словами (греп п. 8).
- **Решения по R1–R11** (обоснование — КОНТЕКСТ «Что решено»): R1 — Compose (генератора досок нет; № 110); R2 — продавец
  170 dp без узла, облачко в слоте только при 1,0, на полу при > 1,0 (`fontScale > 1f`; круг 1, подтверждённая 12 — было ≤ 1,15); текст облачка Марты — заголовок заказа и плата из
  `shiftQuote` (данные; «Помоги с ящиками!» и «+12» макета в content нет — № 113); «Начать смену» на полу — на первом экране
  360 × 640 без плаката, при Пк3 — после прокрутки (№ 114); у Фомы — `lines[0]` на первой странице; R3 — «Ещё N» с честным N и нарисованной стрелкой (круг 1, подтверждённая 8 — был глиф ▶),
  «!» на кнопке страницы с товаром события; R4 — `Showcase.pages` + `eventItem` (плакату нужен товар события — это правило
  домена, поэтому в той же чистой функции и под оракулом); R5 — касса по макету 4 без ряда монет (при 60 — 6 рядов), превью
  только у вариантов без вопроса, «Назад» закрывает кассу; R6 — плакат над товаром, текст — слова в «ёлочках» вступления
  (Пк3 — «Супер-корм! Все питомцы в восторге!», у макета короче), вступление и «Пройти мимо» — в кассе товара; R7 — строка
  «такая распродажа бывает» из кода убрана (её говорит вступление C1), BACKLOG п. 25 закрывается; R8 — описание дословно;
  R9 — HUD и вкладки не тронуты; R10 — прокрутка при 1,3, облачко на полу; R11 — вне `PlaceScreen.kt` только новый `Showcase.kt`.
- **Отклонения от макета, записанные в спеку**: фон не деформируется (макет растягивал небо, ужимал прилавок, стирал миску) —
  у рынка прилавок фона виден за 2-м рядом (№ 111), у «У Фомы» стеллаж фона закрыт стенкой витрины; нижняя доска — свой
  прилавок витрины (мята у рынка, жёлтый у Фомы), а не прилавок фона; плакат шире (250 dp), чтобы слоган из данных встал в
  2 строки; Фома не отражён; ниточки к кассе и пунктира нет; ящики нарисованы (не эмодзи).
- classic: `assembleClassicDebug lintClassicDebug testClassicDebugUnitTest` → exit 0, 600 / 0 / 0, lint 0 ошибок, 5 + 3 сетевых.
- **Не проверял**: S23 (дампа нет — окно ≈ 570 dp по оценке разведки), TalkBack вживую, `perf.sh`, P3 и C3 на витрине (правило
  то же, что у Пк3 и C1).
- **2026-09-29, сессия 16 — круг критиков 1** (линзы contract / child / a11y / oracle, по два скептика на находку). Находок
  41: contract 6, child 15, a11y 9, oracle 11 (журнал workflow `wf_636eaeec-320`); отклонены обоими скептиками 6 — contract 1,
  child 2, a11y 2, oracle 1 — в задание автору не передавались; переданы 35: подтверждённых (оба скептика не опровергли) 20 —
  contract 3, child 6, a11y 4, oracle 7; спорных 15 — contract 2, child 7, a11y 3, oracle 3.
  **Подтверждённые — внесено:**
  1. BACKLOG п. 13 — «частично», LIMITATIONS п. 19 сужен, TC-28 «и при 1,3» (ДОКИ) — доки не врут о № 114.
  2. `poster_food_super.webp` больше не на экране — вариант б скептика 2 (код не трогаю: макет рисует плакат без картинки,
     картинка растила бы № 114): ANTI-SCOPE, № 115, UX_ACCESSIBILITY :365–367, греп ДОКИ с `posterRes|Плакат события 64 dp`.
  3. ДО СПАВНА п. 2: `@Composable` PayPanel — :267, KDoc :266.
  4. `ShelfPic` — рамка по альфе ≥ 40 (`readPixels`, кэш), 66 × 60 dp низом к доске, тень-овал; вариант скептика 2 с полем
     66 × 60 и `BitmapPainter(srcOffset, srcSize)` — через `Image(alignment = BottomCenter)`, тень — `drawBehind` той же
     картинки (0,84 ширины); в кассе — тот же `ShelfPic` 80 dp. Ниточка ценника (y = ITEM + 2 dp) не сдвинулась. Критерий
     находки — расчётом по рамкам альфы ≥ 40: миска корма 339 × 221 px → 66 × 43 dp (ширина ≥ 60 ✓), шарик 243 × 435 px →
     34 × 60 dp (высота ≥ 50 ✓, подложка в рамку не входит), низ картинки — на ITEM, линия доски — ITEM + 2 dp (зазор 2 dp ✓);
     глазами — кадры market10, foma10, marketlimit маршрута (миска, каша, мыло стоят на доске).
  5. Миска фона рынка — выбран путь скептика 1 в урезанном виде: стенка витрины рынка только за 2-й доской и прилавком
     (ряд 1 на небе, как макет 1); путь скептика 2 (ящики / плашка в пустом слоте) не закрывает прокрутку — миска фона
     неподвижна и вылезала рядом с шариком (order13e). № 111 переписан.
  6. Облачко Фомы при 1,0 — `offset(y = -10 dp)` ряда пола (скептик 2): облачко 546..613 dp при окне до 616 (было срезано на
     ≈ 6 dp); «Ещё 9» на той же строке. Вертикальная проверка — `measure` журнала: низ облачка < низа окна на кадрах без
     прокрутки (foma10 613 < 616, order10 / market10 — облачко в слоте).
  7. № 112 / № 114 — S23 как оценка («не проверено»); в живой проверке — касса робота C1 на S23.
  8. Стрелка пейджера — треугольник `G.magenta` (Painter `Arrow`, иконка `GameButton`; для «Ещё» — `LayoutDirection.Rtl`,
     трейлинг-слота в `Widgets.kt` нет, он в protect); видимый текст «Ещё N» / «Назад» без глифов (вариант скептика 2).
     Маршрут `town_route.sh` пейджер не трогает; `a1sp.sh` касается по префиксу «Ещё 9» и «Назад».
  9. + 13. «!» на кнопке страницы для TalkBack — `stateDescription` = заголовок первого события страницы (из content, нового
     текста нет — вариант скептика 2 находки 9 и 2 находки 13); KDoc `Badge` поправлен. Известное: `silentArrivals` глушит
     прибытие события, плакат которого на другой странице, — не чиню в A1s.
  10. Касса: строка названия с ✕ закреплена, тело (`pay_body`) прокручивается, `testTag("pay_panel")` на внешней колонке;
      проверка — `nodes_check`: «Закрыть кассу» 144 × 144 px во всех 22 дампах касс, включая `*e` после прокрутки.
  11. Витрина под кассой вне дерева TalkBack: `Modifier.verticalScroll(scroll).then(clearAndSetSemantics {})` при открытой
      кассе (модификатор после прокрутки — своя семантика прокрутки остаётся); `nodes_check` — 0 узлов товаров в кадрах касс
      (прототип до правки — 1–6 на дамп), узлов «… Плакат…», облачка продавца и пейджера в дампах касс тоже нет (0 описаний «… . Плакат: …» в 22 дампах касс при 1 в pk3_10; строка «Плакат: …» в кассе Пк3 — её вступление);
      исключение «закрытые кассой» из п. 12 убрано. Давний пробел `Ask` (Common.kt:95) — вне A1s.
  12. Порог облачка — `fontScale > 1f` в `ShopScene` (слот только при 1,0; на полу до 240 dp при > 1,0); `bigFont()` не тронут.
      Кадры 1,15: market115, foma115, order115 — облачко на полу, слово не рвётся. Попутно: «Обрезано: 1 — Выбери мечту» —
      HUD-1 при 1,15 (не A1s, R9) — BACKLOG.
  14. п. 4а ДО СПАВНА — мутанты M1–M4 в одноразовом worktree на коммите оракула (вариант скептика 2: M2 «сортировка без
      новой доски», M4 «BUY_AT как товар»); строка «Красный» ORACLE — «компиляция; сила — п. 4а».
  15. ACCEPTANCE п. 0 — `git add -N` нового `Showcase.kt` (acc.sh делает сам); п. 6 — ` M` / ` A`.
  16. Маршрут приёмки — префикс `a1sr` (glob `emu_a1s_*` его не берёт), `rm -f emu_a1s_*` перед п. 11, п. 12 — по обоим.
  17. № 38 для кассы — в `nodes_check`: «Каша» — `pay_body scrollable="false"`, строки `KASSA` по остальным (true у 18 из 22
      дампов касс); № 112 переписан по замеру (прокрутка тела 33–205 dp при 1,0, 74–275 при 1,3). Правило № 38 в WORKFLOW не
      меняю до ответа № 112.
  18. `town_route.sh` :174-175 — якорь `has "Свежее с утра! Заглядывай."` и отрицательная проверка «Начать смену» на
      marketlimit (правит оркестратор ДО СПАВНА п. 1; `route.diff`); прогон на прототипе — ниже.
  19. ORACLE `eventItem`: синтетика `[PRICE(a), PRICE(b)] → a` и `[OFFER(a), PRICE(b)] → a` (вариант скептика 2 — убивает и
      «последний», и «PRICE важнее OFFER»), p3 — «(реальный контент)»; «eventItem не смотрит на лавку эффекта» — BACKLOG.
  20. п. 5 — `git show --name-only --format= $O -- app/src/test/` (ровно ShowcaseTest.kt) и `git diff --name-only $O --
      app/src/test/` (пусто); `O` — sha коммита оракула, он же `base` task-scope.
  **Спорные — решения:**
  1. + 8. Событие лавки без товара на полке — код не трогаю (живых таких нет; запасной показ требует менять `PlaceEvents`),
     ANTI-SCOPE + BACKLOG (с «!» улицы) + контентное правило оракула `eventItem` п. 4 (скептик 1): включение Пк2 правкой
     `content.json` роняет тест, а не прячет событие от ребёнка. Правило проверено на живом content: 4 события, все проходят.
  2. Заказ чужого жителя — ANTI-SCOPE + KDoc `ShopScene`; подмена Фомы Кешей ломает композицию (оба скептика).
  3. Плакат C3 «Мечта почти твоя» — правило текста плаката: «ёлочки» → заголовок при setup PRICE/OFFER → первое предложение
     вступления (скептик 2): над змеем «В витрине новинка — воздушный змей за 30.»; Пк3, P3, C1 — как было. Кадры C3
     (c3p2_10 / 13, c3_kassa10 / e) — шаг `c3` в a1sp.sh (C3 требует 35 в копилке: 3 × «В копилку 10 из запаса»).
  4. «Пройти мимо» в кассе — порядок кнопок не меняю (скептик 2): у C1 и C3 умолчание `WEEK_END → SKIP → GOOD`; поднять
     исход над оплатой — подсказка ответа урока; вариант б′ в № 112 для владельца.
  5. Облачко Марты — код не трогаю, в № 113 вариант в (жетоны смен в ряд с «+6») по снимкам; вступление в облачко — нет.
  6. Питомец в кассе при 1,3 сжимался до ≈ 25 dp — облачко в ячейке с весом (скептик 2), питомец 80 dp сохраняется; чтобы
     облачко не резало «Настроение» при > 1,0 — слово стата только при 1,0 (правило `bigFont`: слова уступают значкам, слова —
     в описании облачка). Питомец крупнее (110 dp, скептик 1) — против прокрутки кассы, в № 112 не вношу отдельно.
  7. Предлог в конце строки — неразрывный пробел после однобуквенного слова в `SellerBubble` (скептик 1, одна строка; в
     исходнике escape `\u00A0`, литерал ловит acc.sh 26); общая правка `TText` — BACKLOG. «Добро / пожаловать / в лавку!».
  9. Сравнение кормов Пк3 — подпись под кадром на листе владельцу (скептик 2), без вопроса ворот.
  10. Касса под вкладками — не двигаю (макет 4, скептик 2); № 112 — замер прокрутки по дампам, «как раньше» убрано, вариант в
      честно (+≈ 141 dp, C1 всё равно прокручивается, HUD-2 закрыт); сняты kassa_want13, pk3_kassa13, c1_kassa13 (+ `e`).
  11. S23 в № 111 — расчётом с пометкой «не снято» (скептик 2); № 112 / 114 — «оценка, не проверено» (подтверждённая 7).
  12. `clipToBounds` не добавляю: узлы за краем — расширение границ касания на 24 dp (половина 48 dp), `verticalScroll` и так
      режет рисование; исключение п. 12 — порог 24 dp (72 px), дальше — ошибка (скептик 2). Замер: верх узлов первой доски
      556 px при окне от 628 (order13e, pk3_13e).
  13. 34 атомарные проверки acc.sh с EXP = 34 и сверкой (№ 49), п. 4 — команда ACCEPTANCE_PRESETS.md:50-53 (оба скептика по
      сути: скрипт теперь есть).
  14. `nodes_check` — ожидаемое число товаров по имени кадра (скептик 1): при 1,0 ровно 8 / 9, при 1,15 / 1,3 — от 1 до 8 / 9
      (доска за краем окна в дамп не попадает — c3p2_13: 6), в кассах 0. Мутанты зонда FAIL 4 из 4.
  15. `a1sp.sh` — текстом в «Зонды приёмки» с `SC` параметром и корнем `git rev-parse` (скептик 1), плюс две проверки R3 без
      кадров: «Назад» и сброс страницы при смене лавки («Вкусный обед» есть только на стр. 1); шаг `c3`; `start_ev` листает
      коротким свайпом (длинный уносил заголовок карточки за край).
  **Прототип после круга** (`wt`, 2 файла allow + `tools/town_route.sh` на время прогона маршрута, после — откат):
  - сборка `./gradlew assembleGameDebug assembleGameRelease lintGameDebug testGameDebugUnitTest` → exit 0 (лог
    `build_c1.log`); дифф без import и пустых **+436 −67** (`PlaceScreen.kt` +417 −67, `Showcase.kt` +19), импорты game +36 −2;
    release 4 654 326 Б (+16 384 — ступень 16 КиБ та же), остаток эпика 475 743 Б; единственное предупреждение game —
    `PlaceScreen.kt:210:89` (давнее); CONTRACT перегенерирован скриптом из `wt` (`gen/contract.py`), `contract_check` →
    `CONTRACT OK 3` на wt, `CONTRACT FAIL` 3 из 3 на BASE; `ref/`, `proto.diff` обновлены;
  - кадры [переснято сверкой целостности — ниже; прежние — `gen/shots_c1b/`] (emulator-5554, 360 × 640, debug установлен `install -r`): полный прогон `a1sp.sh a1sp setup shop plan kassa events
    c3` с нового демо-профиля (`final_capture3.log`) — 46 кадров `emu_a1sp_*` (+ 1,15 × 3, кассы при 1,3 и `e`, C3 × 4),
    «Обрезано: 0», кроме market115 / foma115 / order115 (HUD-1 «Выбери мечту»); «not found» — один: `start_ev "Мечта почти
    твоя"` (длинный свайп `scroll_to` проскакивал карточку) → `scroll_to` и `start_ev` листают `nudge` ≈ 400 px, шаг `c3`
    перепрогнан исправленным скриптом (без пополнения — копилка уже 40) — «not found» 0, плакат C3 в дампе есть. `setup` с
    `nudge` заново не прогонял. `nodes_check` → `NODES OK 201 (FRAMES 46)`, `pay_body` прокручивается в 18 из 22 дампов касс;
    `ui_measure` — мелкие цели только у края окна (п. 12: 5 кадров, 1–3 узла), `offscreen` пусто; листы
    `shots/a1sp_sheet_{1_scene10,2_scene13,3_kassa,4_events}.jpg` (подпись Пк3 — «эффект — в кассе (№ 39 а)»);
  - маршрут [перепрогнан сверкой целостности — ниже; прежний лог — `gen/route_a1sr.c1.log`]: `DUMP=1 tools/town_route.sh a1sr` на этой debug с правкой :173 — промахов 0 (`grep -cE "not found|gate not passed|GEOM FAIL|CLOSE FAIL"` → 0), `GEOM OK` 8, `CLOSE OK` 2, «Обрезано: [1-9]» 0 (`route_a1sr.log`); «Начать смену» нажата текстом после `swipe_up 4` при 1,0; marketlimit — якорь «Свежее с утра! Заглядывай.» есть, «Начать смену» нет (кадр: Марта с репликой в слоте, миски фона нет); 28 дампов `emu_a1sr_*` — `ui_measure` мелких целей 0, `offscreen` пусто; `nodes_check` по 6 дампам лавок (market, marketp, foma × 1,0 / 1,3) → `NODES OK 50 (FRAMES 6)`; после прогона `tools/town_route.sh` в wt откатан (`gen/route.diff`);
  - acc.sh (`R=<wt> SPEC=<эта спека> GRADLE=1`, без `O`) — `ИТОГ PASS=31 FAIL=3 ВСЕГО=34 (заявлено 34)` (`acc/acc_out.txt`): FAIL — только проверки оракула 5 (нет ShowcaseTest), 16 и 17 (нет `O`) — по определению до коммита test-author; `assert-oracle-intact` — PASS с `wt/.claude/task-scope.json` от `scope.py` (65 путей protect); мутант «литерал U+00A0 вместо escape» → FAIL 25 и 26, ВСЕГО 34.
  **Не проверял**: S23, TalkBack вживую (порядок и чтение `stateDescription`), `perf.sh`, P3 на витрине (правило «ёлочек» то
  же, что у Пк3); жест касания по вкладке у края прокрутки при 1,3 (узлы доски заходят на вкладки на 24 dp — Compose отдаёт
  касание прямому попаданию, вкладке; не мерил); вид 1,0 после лимита смен без заказа (кадр marketlimit маршрута — глазами).
- **2026-09-29, сессия 16 — сверка целостности** (9 пунктов; п. 9 сверки — свод без расхождений, правок не требует):
  1. Заказ чужого жителя — код не трогаю: фильтр по жителю спрятал бы заказ `job_kassa` у Фомы совсем. По коду переписаны
     KDoc `ShopScene` («the place's first order … not filtered by resident — another resident's order would stand in the
     seller's bubble, ANTI-SCOPE»; 4 строки → 4, бюджет тот же +436 −67), КОНТЕКСТ R2 и ANTI-SCOPE (`job_kassa_help`: foma,
     `job_kassa`, Кеша). Замена одна (`gen/kdoc_fix.py`) в `ref/PlaceScreen.kt`, `wt` и § 3; `cmp ref wt` — равны; `git -C wt
     diff` → `proto.diff`; `contract_check` на wt → `CONTRACT OK 3`; BASE + § 2–3 + § 4 = `ref` (тело посимвольно, импорты
     множеством).
  2. ДОКИ: + UX_ACCESSIBILITY :130, REQUIREMENTS_MATRIX :64 и явные :167, :171, TEST_CASES :140; у итогового грепа — полный
     список строк, которые остаются (ARCHITECTURE :76, :174; CONTENT_MAP :269; GAME_CONCEPT :1276, :1988; заголовки BACKLOG
     п. 13 и 25). Все 24 строки грепа на 50b77a7 (номера те же, что на 673bb3d) — в правках или в этом списке.
  3. Литерал U+00A0 в тексте (КОНТЕКСТ R2, спорная 7) → запись `\u00A0`; `LC_ALL=C grep -c $'\xc2\xa0'` по спеке → 0.
  4. acc.sh, проверка 32 — по сырому `Showcase.kt` (`tr -d '\r' < $S | grep -cE …`), не через `C()`; ACCEPTANCE п. 8 —
     «сырой файл, с import». Мутант «+ `import android.util.Log`, `import java.util.Random`» (сухой прогон, `gen/acc_mut32.txt`)
     → FAIL 32 «2 (= 0)» и FAIL 25; эталон возвращён (`cmp ref wt`).
  5. Журнал: первая запись и «Решения по R1–R11» помечены — порог облачка `fontScale > 1f`, «Ещё N» без глифа (подтверждённые
     12 и 8).
  6. Счёт круга 1 — по журналу `wf_636eaeec-320`: 41 находка, 6 отклонены обоими скептиками, переданы 35 (20 + 15).
  7. BASE — HEAD 50b77a7 (сверх 470db8b только доки и README, `-- finny-pet/app tools` пусто); раздел ворот — 13; № 110–119 в §18 свободны; висячая «№ 117» (R10) → пункт BACKLOG;
     ACCEPTANCE п. 10 — `uninstall` release перед установкой debug (подпись другая).
  8. Пересборка и переснимки — с артефактами:
     - `R=wt SPEC=<спека> GRADLE=1 bash acc/acc.sh` → `ИТОГ PASS=31 FAIL=3 ВСЕГО=34 (заявлено 34)` (`acc/acc_out.txt`; FAIL 5,
       16, 17 — нет оракула); debug пересобран 18:34:31 (md5 `755cb381…`, 23 510 641 Б); release 4 654 326 Б (+16 384):
       `compileGameReleaseKotlin` и R8 прошли, `packageGameRelease` UP-TO-DATE — dex тот же, KDoc байт-код не меняет;
     - установка — `gen/install_c2.log`: `uninstall` 1.4.1 release, `install` debug, md5 `base.apk` на устройстве = локальному
       (`755cb381…`);
     - кадры: `SC=shots bash a1sp.sh a1sp setup shop plan kassa events c3` — финальный скрипт целиком (setup и события с `nudge`,
       пополнение копилки в c3), новый демо-профиль, 18:36:34–18:56:13 (`final_capture4.log`, exit 0): 46 кадров и 46 дампов,
       «not found» 0, «Обрезано: 1» — только market115 / foma115 / order115 (HUD-1); плакат C3 «В витрине новинка…» — в 4
       дампах c3; `nodes_check` → `NODES OK 201 (FRAMES 46)`, `pay_body` прокручивается в 18 из 22 (`gen/nodes_c2.txt`);
       `ui_measure` (`gen/measure_c2.txt`) — `offscreen` пусто в 46, мелкие цели только в 5 кадрах исключений п. 12 (pk3_10
       «Начать смену» 29 dp, c1p2_10 «Назад» 34, c3_kassa10 — нижний вариант 46, order13e / pk3_13e — первая доска 45); облачка
       как в «Замер прототипа» (foma10 546..613, market10 319..386, pk3_10 377..451). Дампы против прежних (`gen/shots_c1b/`):
       30 из 46 совпали по (text, desc, bounds, scrollable) целиком, в 16 кадрах касс отличается один безымянный узел — питомец
       на 1–4 px (дыхание спрайта). Листы `shots/a1sp_sheet_*` пересобраны 18:57;
     - маршрут: `gen/route_c2.sh` (`route.diff` в wt на время прогона, после — откат, `git -C wt status` — 2 файла allow) на той
       же установке, 18:56:28–19:17:29 (`route_a1sr.log`: md5 установленного = локальному `755cb381…`) — промахов 0 (`grep -cE
       "not found|gate not passed|GEOM FAIL|CLOSE FAIL"`), `GEOM OK` 8, `CLOSE OK` 2, «Обрезано: [1-9]» 0; marketlimit — якорь
       «Свежее с утра! Заглядывай.» есть, «Начать смену» нет (обе проверки — в счётчике промахов); 28 дампов `emu_a1sr_*` —
       `ui_measure` мелких целей 0, `offscreen` пусто; `nodes_check` по 6 дампам лавок → `NODES OK 50 (FRAMES 6)`; прежний лог —
       `gen/route_a1sr.c1.log`;
     - после — на emulator-5554 снова 1.4.1 release: md5 на устройстве = `finny-pet/release/finny-pet-1.4.1-release.apk`
       (`1fc9614c…`), шрифт 1,0, 360 × 640 оставлен (`gen/install_c2.log`).

**Выпуск 1.4.2, ворота владельца (2026-09-29, лист `town/r142_release.jpg`).** Владелец: облачка продавцов «съехали вниз экрана» при 1,3, у Фомы при 1,0 облачко «ниже и показывает на пол». Решение № 129 (GAME_CONCEPT §18): облачко только над головой — `ShopScene`: `inSlot = slotFree && (order != null || (greet && !scaled))`, `onFloor = order != null && !slotFree`; правило R2 «при шрифте > 1,0 — на полу» отменено; на полной странице «У Фомы» реплики нет. Пересборка: тесты 619/0, lint 0 ошибок; маршрут на release — промахов 0, GEOM OK 8, CLOSE OK 2, обрезки 0.

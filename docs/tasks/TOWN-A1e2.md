# TOWN-A1e2 — встройка картинок вещей и товаров: 19 веток `itemRes`, мечта `goal_camp`, стартовые вещи картинками, плакат Пк3 строкой, отладочный счётчик эмодзи

```
TASK: TOWN-A1e2 — в UI `game`: картинки у всех 23 товаров двух лавок (14 новых веток `itemRes`, из них `gift_toy` —
      готовая `tile_gift`), у стартовых `home_*` и псевдонимов `lamp_new`, `fun_lego`; мечта «Походная палатка» —
      `goal_camp`; стартовые цветок и кресло в комнате и в «Обустроить» — картинками; плакат `poster_food_super` 64 dp
      строкой в карточке события Пк3; отладочный узел `emoji` («Эмодзи: N …») рядом с «overflow», только в debug
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md: строка A1e :43, доля A1e :66-67, правила эпика :69-83); генератор и набросок —
      docs/tasks/TOWN-A1e.md (КОНТЕКСТ :44-115, набросок A1e2 :752-788, ДОКИ :790-802, журнал круга A1e1-2 :917-928)
BASE: 4e6746c (коммит ассетов: 22 WebP круга A1e1-2 — 18 новых, 4 замены; LICENSES, ARCHITECTURE:28, REQUIREMENTS_MATRIX;
      `app/src/game/java` не менялся). Кодер — от коммита этой спеки поверх BASE (`git diff --stat 4e6746c HEAD --
      finny-pet/app/src` перед спавном пуст). Номера строк — 4e6746c (код `app/src/game/java` = ca21799 = выпуск 1.4.0)
BRANCH: feat/town, основное дерево, один кодер
ЗАВИСИТ ОТ: круг A1e1-2 принят и `props.py` слит (bf1f161, журнал TOWN-A1e.md:917-928); решения владельца № 93 а (картинки
      у всех 23 товаров, GAME_CONCEPT.md:2132), № 96 б и «всё по рекомендации» по входам A1e2 (GATE_QUEUE.md:287-298,
      :317): бра — «а, но поднять на высоту часов», плакат — строкой, размер вещей — «б, если место найдётся, иначе а»;
      на быстром пути берём **размер а (36 dp, как сейчас)** — запись в журнал и доки; № 101–102 — после сдачи.
      TOWN-A1d2 и TOWN-A1g2 слиты (fef3e9c): раскладка комнаты и улицы — их. TOWN-J1-1b5 (allow — только TrayScreen.kt)
      с этой задачей файлов не делит; если он слит раньше — ДО СПАВНА п. 2
```

## КОНТЕКСТ
Владелец: «встрой картинки вещей». Генератор, вид и ассеты приняты (A1e1, круг A1e1-2, BASE); A1e2 — только ссылки на
готовые картинки в `game` и приёмка того, что видит ребёнок. Быстрый путь, как A1g2/A1d2 (журнал TOWN-A1g2.md:1018-1043):
дословный CONTRACT → кодер → машинная приёмка → живая на эмуляторе → доки → ревьювер; строгий зонд `art_check.py items`,
листы, судьи и S23 — после сдачи (ANTI-SCOPE, журнал «отложено»).

### Что решено и что из этого делает A1e2
- **№ 93 а** — у всех 23 товаров двух лавок своя картинка (на витрине A1s названий нет, № 39). Сейчас эмодзи у 14
  (TOWN-A1e.md:45-52): `food_porridge` 🍚, `food_super` 🥫, `care_soap` 🧼, `care_shampoo_simple` 🫧, `fun_icecream` 🍦,
  `fun_carousel` 🎠, `fun_picture` 🖼️, `fun_rug` 🧶, `fun_spinner` 🌀, `fun_kite` 🪁, `fun_robot` 🤖, `fun_starlamp` ⭐,
  `gift_card` 💌, `gift_toy` 🎁 → § 1 (у `gift_toy` — готовая `tile_gift`, коробка с бантом; свой рендер
  отброшен — TOWN-A1e.md:825, № 93 а).
- **Стартовые** `home_flower` 🪴 (`spot_1`), `home_armchair` 🛋️ (`spot_4`) рисуются эмодзи всегда — `Pic(null, …)`
  (RoomScreen.kt:257, JarsScreen.kt:204) → § 4, § 5; ветки `itemRes` — § 1.
- **Псевдонимы** (набросок TOWN-A1e.md:761-762): `lamp_new` → `item_home_lamp` (мастерская закрыта в срезе 1),
  `fun_lego` (`eventOnly`, приходит мечтой `goal_lego` и без ветки встал бы в комнату эмодзи 🧱) → `goal_lego`,
  `gift_toy` → `tile_gift`. Ресурсы есть в BASE (`ls`: `item_home_lamp.webp`, `goal_lego.webp`, `tile_gift.webp`).
- **Мечта «Походная палатка»** `goal_camp` 🏕️ — `goalRes` её не знает (TownUi.kt:120-128) → § 2. Видна в копилке (72 / 48 /
  56 dp), на калитке леса (48 dp, StreetScreen.kt:127), в окне комнаты у забора (24 dp, RoomScreen.kt:158), в шапке (28 dp).
- **Плакат Пк3** `poster_food_super` — «плакат строкой» (GATE_QUEUE.md:291-293): 64 dp слева от заголовка и текста
  карточки события (макет К3а — окно выросло на 24 dp; колонкой — на 74 dp). Карточка — `PlaceEvents`
  (PlaceScreen.kt:408-418, `Panel`, вызовы :148 лавка, :323 и :330 работы) → § 6. Описания у плаката нет: текст карточки
  «Плакат: «Супер-корм! …»» уже читается. `posterRes(eventId)` — § 1 (явный `R.drawable`, R8 — TOWN-A1.md:73-75).
- **Размер вещей в комнате** — а, 36 dp (как сейчас; вариант б «кресло 56–64, коврик 72» — место на макете A1d2 не
  проверено, быстрый путь его не берёт).
- **Бра** `item_home_lamp` — **не встраивается** (ANTI-SCOPE, вопрос № 108): на высоте часов у кровати ни одна сторона от
  часов не свободна на обоих экранах приёмки, а место привязано к фону (часы — в оболочке `room_port_day`, масштаб Crop),
  то есть нужна новая координата от фона и выбор, которого владелец не видел. Замер — ниже.
- **Счётчик эмодзи** — живой зонд набросок TOWN-A1e.md:769-776: `testTag` внутри родителя с `clearAndSetSemantics` uiautomator не
  видит, поэтому узел — на корне рядом с «overflow» (GameApp.kt:228-233, только `BuildConfig.DEBUG`) → § 3. 7 строк
  + 1 изменённая + импорт.

### Карта кода (4e6746c)
- `TownUi.kt` (327 строк): `LocalClipped` :67-68; `itemRes` :93-106 (10 веток, `fun_tent` :104, `else -> null` :105);
  `pastryRes` :108-117; `goalRes` :119-128 (`goal_zoo` :124, `item:` :125, `demo_goal`/`custom_` :126); `Pic` :130-137 (запасная
  ветка — `Box` с `clearAndSetSemantics {}` и `Text(emoji)` :134-136); `GoalPic` :139-140; `PiggyChip` → `GoalPic` 28 dp
  :237. Импорты `DisposableEffect` :30, `SnapshotStateList` :35, `staticCompositionLocalOf` :36 уже есть.
- `GameApp.kt` (264): импорт `LocalClipped` :82; `clipped` :131; `CompositionLocalProvider` :178-181 (`LocalClipped provides`
  :180); отладочный блок :228-233 («overflow» :230-232).
- `RoomScreen.kt` (360): `SpotThing` :251-260 (стартовая :257, поставленная :258); раскладка средней зоны :169-217 (часы,
  холодильник, `spot_2` — замер «Бра»).
- `JarsScreen.kt` (223): `ArrangeScreen` :184-223, ячейка 64 dp — стартовая :204, вещь :205.
- `PlaceScreen.kt` (460): `ItemCard` → `Pic(itemRes(item.id), item.emoji, 40.dp)` :239 (не меняется); импорты `itemRes`
  :90, `particleTarget` :91; `PlaceEvents` :408-418.
- Не меняются: `SavingsScreen.kt` (`GoalPic` :55, :88, :100), `StreetScreen.kt` (`Pic(goalRes(g.id), g.emoji, 48.dp)` :127),
  `TrayScreen.kt` (`Pic(null, "☝️", 40.dp, …)` :230 — указатель первой смены, единственный `Pic(null,` после задачи).
- `app/src/test/`: ссылок на `ru.finny.pet.game` и `R.drawable` нет (`grep -rln 'ru.finny.pet.game\|R\.drawable'
  finny-pet/app/src/test` → пусто).

### Ассеты (BASE 4e6746c, сделано)
22 WebP = `acc2_w` приёмки круга A1e1-2 побайтно (`cmp` 22 × без разницы): 18 новых 90 052 Б (`goal_camp`,
`item_care_shampoo_simple`, `item_care_soap`, `item_food_porridge`, `item_food_super`, `item_fun_carousel`,
`item_fun_icecream`, `item_fun_kite`, `item_fun_picture`, `item_fun_robot`, `item_fun_rug`, `item_fun_spinner`,
`item_fun_starlamp`, `item_gift_card`, `item_home_armchair`, `item_home_flower`, `item_home_lamp`, `poster_food_super`) и 4
замены 20 774 Б (`item_care_vitamins` — ванна с пеной, 38 478 → 5 174; `item_fun_ball`, `item_fun_book`, `item_fun_tent`).
В `game/res/drawable-nodpi/` 91 WebP. На BASE 18 новых без ссылки: lint `UnusedResources` 18, R8 их выкидывает
(aapt2 release BASE — 4 из 22 на месте).

### Бра — замер (почему ANTI-SCOPE)
Часы — в оболочке `room_port_day` (1080 × 1920, Crop на весь экран, `k = max(W/1080, H/1920)`): золотой обод
x 335..447 × y 928..1039 px. Бра 36 dp: видимое (α > 127) 5,3..30,7 × 1,7..33,7 dp кадра. Центр бра по высоте — центр часов,
зазор 2 dp. Раскладка средней зоны — TOWN-A1d2.md:91-100 (360 × 640: зона 286..540 dp, `spot_2` 162..198 × 286..322,
холодильник 8..72 × 312..408, видимый до x 68; S23 / эмулятор 1080 × 2340: зона 279..689, холодильник 402,9..498,9).

| Экран | Часы, dp | Справа от часов | Слева от часов |
|---|---|---|---|
| 360 × 640 (k 1) | 111,7..149,3 × 309,3..346,7 | бра 151,3..187,3 × 310..346: под картиной `spot_2` (видимое 163,8..196,2 × 291,8..316) — наезд 18 × 4 dp, под змеем — 9 × 8 dp | 73,7..109,7: до холодильника 11 dp — свободно |
| S23 (k 1,21875) | 96,7..142,6 × 377,0..422,5 | 144,6..180,6 × 381,8..417,8 — свободно (`spot_2` до 315, питомец с 505) | 58,7..94,7 × 381,8..417,8: угол абажура на холодильнике (видимый с 406,2) — 4 × 9 dp |

Условие задачи «только если встаёт в раскладку RoomScreen без её перестройки» не выполнено: свободной стороны на обоих
экранах нет, положение зависит от масштаба фона — нужна координата от фона (как `fridgeTop`/`doorY`, RoomScreen.kt:169-177)
и выбор наложения, которого владелец на листе не видел (К2 показал бра у плинтуса). Вопрос № 108 — ниже.

## ЧТО УВИДИТ РЕБЁНОК
| Момент | Что на экране | Что понимает ребёнок |
|---|---|---|
| рынок | миска корма, каша, мыло, простой шампунь, шарик, мороженое, карусель, с недели 2 — банка супер-корма: картинки 40 dp, кружков-эмодзи нет | «Тут еда, мыло и радость — узнаю без подписи» |
| рынок, Пк3 | в карточке «! Супер-корм» слева плакат 64 dp — та же банка супер-корма, что на полке, в лучах, над ней сердце, под ней миска корма, без букв; справа заголовок и «Плакат: «Супер-корм! Все питомцы в восторге!» …», ниже «Пройти мимо» | «Банку хвалят — это реклама; а корм тот же» |
| «У Фомы» | ванночка с пеной (не таблетки), коврик, мячик, вертушка, книжка, робот, домик-палатка, открытка, подарок; по мере открытия (`unlockPeriod`) — змей (нед. 2), лампа-звёздочка (3), картина (5) | «Вещи для дома и для игры» |
| купил и поставил | на месте в комнате — та же картинка 36 dp; в «Обустроить» — 40 dp | «Моя вещь — в моём доме» |
| дом с первого запуска | цветок в горшке и кресло — картинки 36 dp, как мебель; бра нет (№ 108) | «Дом обжитой» |
| копилка, калитка леса, окно, шапка | мечта «Походная палатка» — палатка (72 / 48 / 56 / 48 / 24 / 28 dp) | «Коплю на поход» |
| пустое место | ничего (стоп-лист № 17) | не приманка «купи» |

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
- 3.5 — реклама только игровым уроком, без марок и логотипов: плакат без букв, слоган — живым текстом рядом; картинки не
  пугают (ванна без медицины — круг A1e1-2).
- 3.6 — цвет не единственный признак (пары различаются силуэтом — судьи A1e1); текст растёт со шрифтом (на картинках букв
  нет); TalkBack — картинки без описания (название читает карточка/мишень, двойного чтения нет); мишени ≥ 48 dp не меняются.
- 2.5.14 — контент данными: новый товар без ветки по-прежнему рисуется эмодзи из записи, код не нужен.
- 3.3 — права на изображения: строка LICENSES — в BASE.

## CONTRACT
Домен, ViewModel, `content.json`, тесты, ресурсы, `Pic` для настоящей картинки, `GoalPic`, `pastryRes`, `residentRes`,
`ItemCard`, `SavingsScreen`, `StreetScreen`, `TrayScreen` — не меняются. Новых строк ребёнку — 0, новых описаний TalkBack — 0
(узел `emoji` — только debug). Каждый блок ниже вставляется **дословно** (с отступами; сверка — `contract_check.py`,
ACCEPTANCE п. 7). Импорты — только два точечных (§ 3, § 6), wildcard нельзя (WORKFLOW № 45).

### § 1. `itemRes` + `posterRes` — TownUi.kt: заменить :93-106 (KDoc и функцию `itemRes`) этим блоком целиком
Ветки 1–10 — BASE без изменений; новые — после `fun_tent`; `else -> null` — последней. Только явный `R.drawable` в `when`
(`getIdentifier` и имена строкой — нельзя: R8 `isShrinkResources` выкинет картинку молча). `posterRes` — сразу после
`itemRes`, перед KDoc `pastryRes` (:108).
<!-- file: ui/TownUi.kt -->
```kotlin
/** Drawable of a shop item or a starter home thing (item_<id>; gift_toy, lamp_new, fun_lego — a picture of another name), or null — then the emoji is drawn. */
fun itemRes(id: String): Int? = when (id) {
    "food_basic" -> R.drawable.item_food_basic
    "food_lunch" -> R.drawable.item_food_lunch
    "care_shampoo" -> R.drawable.item_care_shampoo
    "care_brush" -> R.drawable.item_care_brush
    "care_vitamins" -> R.drawable.item_care_vitamins
    "fun_ball" -> R.drawable.item_fun_ball
    "fun_bow" -> R.drawable.item_fun_bow
    "fun_balloon" -> R.drawable.item_fun_balloon
    "fun_book" -> R.drawable.item_fun_book
    "fun_tent" -> R.drawable.item_fun_tent
    "food_porridge" -> R.drawable.item_food_porridge
    "food_super" -> R.drawable.item_food_super
    "care_soap" -> R.drawable.item_care_soap
    "care_shampoo_simple" -> R.drawable.item_care_shampoo_simple
    "fun_icecream" -> R.drawable.item_fun_icecream
    "fun_carousel" -> R.drawable.item_fun_carousel
    "fun_picture" -> R.drawable.item_fun_picture
    "fun_rug" -> R.drawable.item_fun_rug
    "fun_spinner" -> R.drawable.item_fun_spinner
    "fun_kite" -> R.drawable.item_fun_kite
    "fun_robot" -> R.drawable.item_fun_robot
    "fun_starlamp" -> R.drawable.item_fun_starlamp
    "gift_card" -> R.drawable.item_gift_card
    "gift_toy" -> R.drawable.tile_gift
    "home_lamp" -> R.drawable.item_home_lamp
    "home_flower" -> R.drawable.item_home_flower
    "home_armchair" -> R.drawable.item_home_armchair
    "lamp_new" -> R.drawable.item_home_lamp
    "fun_lego" -> R.drawable.goal_lego
    else -> null
}

/** Drawable of an event's poster (poster_<itemId>, props.py), or null — the event card has no picture. */
fun posterRes(eventId: String): Int? = when (eventId) {
    "pk3_super_food" -> R.drawable.poster_food_super
    else -> null
}
```
Ветка `home_lamp` сейчас не вызывается (у лампы нет `spot`), но держит соглашение «id `town.homeItems` = ветка = файл»
(набросок TOWN-A1e.md:760, зонд `items` после сдачи) и нужна бра по № 108.

### § 2. `goalRes` — TownUi.kt: одна строка после `goal_zoo` (:124)
<!-- file: ui/TownUi.kt -->
```kotlin
    id == "goal_zoo" -> R.drawable.goal_zoo
    id == "goal_camp" -> R.drawable.goal_camp
    id.startsWith("item:") -> itemRes(id.removePrefix("item:"))
```

### § 3. Счётчик эмодзи — TownUi.kt (`LocalEmoji` и запасная ветка `Pic`, заменить :130-137) и GameApp.kt
<!-- file: ui/TownUi.kt -->
```kotlin
/** Emoji drawn instead of a picture right now; the debug «emoji» probe reports them (TOWN-A1e2). */
val LocalEmoji = staticCompositionLocalOf<SnapshotStateList<String>?> { null }

/** Picture or emoji in a [size] box. */
@Composable
fun Pic(res: Int?, emoji: String, size: Dp, modifier: Modifier = Modifier) {
    if (res != null) Image(painterResource(res), null, modifier.size(size))
    else Box(modifier.size(size).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val shown = LocalEmoji.current
        DisposableEffect(emoji) { shown?.add(emoji); onDispose { shown?.remove(emoji) } }
        Text(emoji, style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.6f).sp, lineHeight = (size.value * 0.7f).sp))
    }
}
```
GameApp.kt — импорт после `LocalClipped` (:82):
<!-- file: GameApp.kt -->
```kotlin
import ru.finny.pet.game.ui.LocalClipped
import ru.finny.pet.game.ui.LocalEmoji
import ru.finny.pet.game.ui.LocalParticles
```
список — после `clipped` (:131):
<!-- file: GameApp.kt -->
```kotlin
    val clipped = remember { mutableStateListOf<String>() }
    val emoji = remember { mutableStateListOf<String>() }
```
провайдер — строка :180 целиком:
<!-- file: GameApp.kt -->
```kotlin
                LocalAnimate provides animate, LocalClipped provides clipped, LocalEmoji provides emoji,
```
узел — в блоке `if (BuildConfig.DEBUG)` сразу после «overflow» (:232):
<!-- file: GameApp.kt -->
```kotlin
                        Box(Modifier.align(Alignment.TopEnd).size(1.dp).testTag("overflow").semantics { contentDescription = probe })
                        // debug probe (TOWN-A1e2): how many emoji stand in for a picture right now, and which
                        Box(Modifier.align(Alignment.TopStart).size(1.dp).testTag("emoji").semantics { contentDescription = "Эмодзи: ${emoji.size}" + if (emoji.isEmpty()) "" else " — " + emoji.joinToString(" ") })
                    }
```
Список общий для всех `Pic` (как `clipped` у `TText`); в релизе узла нет, список живёт и там (как `clipped`). Два `Pic` с
одним эмодзи дают две записи, `remove` снимает одну.

### § 4. Стартовые вещи в комнате — RoomScreen.kt:257 (строка :258 — без изменений)
<!-- file: screens/RoomScreen.kt -->
```kotlin
        starter != null -> Pic(itemRes(starter.id), starter.emoji, 36.dp, modifier)
        placed != null -> Pic(itemRes(placed.id), placed.emoji, 36.dp, modifier.clearAndSetSemantics {})
```

### § 5. Стартовые вещи в «Обустроить» — JarsScreen.kt:204 (строка :205 — без изменений)
<!-- file: screens/JarsScreen.kt -->
```kotlin
                            starter != null -> Pic(itemRes(starter.id), starter.emoji, 40.dp)
                            item != null -> Pic(itemRes(item.id), item.emoji, 40.dp)
```

### § 6. Плакат строкой — PlaceScreen.kt: `PlaceEvents` (:408-418) целиком этим блоком + импорт после `particleTarget` (:91)
У события без плаката раскладка прежняя: колонка во всю ширину с тем же шагом 10 dp (`Panel` — `spacedBy(10.dp)`,
Widgets.kt:225), порядок узлов TalkBack тот же (заголовок, текст, «Пройти мимо»).
<!-- file: screens/PlaceScreen.kt -->
```kotlin
/** Events of this place: the poster if the event has one, title, intro and «Пройти мимо» when the event has a Skip outcome. */
@Composable
internal fun PlaceEvents(vm: GameViewModel, placeId: String) {
    vm.eventsAt(placeId).forEach { e ->
        Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
            // № 96, плакат строкой: 64 dp left of the title and intro, no description — the card's text is read (TOWN-A1e2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                posterRes(e.id)?.let { Image(painterResource(it), null, Modifier.size(64.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TText("! ${e.title}", style = MaterialTheme.typography.titleMedium)
                    TText(e.intro)
                }
            }
            if (e.outcomes.any { it.fact == Fact.Skip }) GameButton("Пройти мимо", Modifier.fillMaxWidth(), ButtonStyle.PAPER, minHeight = 48.dp) { vm.pass(e.id) }
        }
    }
}
```
<!-- file: screens/PlaceScreen.kt -->
```kotlin
import ru.finny.pet.game.ui.particleTarget
import ru.finny.pet.game.ui.posterRes
```

### § 7. Импорты
Добавляются ровно два: `import ru.finny.pet.game.ui.LocalEmoji` (GameApp.kt) и `import ru.finny.pet.game.ui.posterRes`
(PlaceScreen.kt). Остальное уже импортировано (сборка прототипа — журнал). Удалённых импортов нет.

### § 8. Явно НЕ определено (кодер спрашивает, а не решает)
Бра и любое `Image(R.drawable.item_home_lamp)` в комнате; размеры 36 / 40 / 64 dp, шаг 12 dp, выравнивание плаката, плакат
колонкой; `contentDescription` картинкам и плакату; порядок и форма веток, KDoc и комментарии; формат строки узла
`emoji`, его угол и размер; счётчик в релизе; ветка и файл `fun_bow`; `care_doctor` 🩺; правки `Pic` для настоящей
картинки, `GoalPic`, `ItemCard`, `StatChip`, `TrayScreen`; `getIdentifier`; новые файлы.

## SCOPE
variant: game (только UI)
allow: finny-pet/app/src/game/java/ru/finny/pet/game/ui/TownUi.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/GameApp.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/JarsScreen.kt,
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/PlaceScreen.kt
protect: как `.claude/task-scope.json` A1g2 + A1d2 (base c110135), но: PlaceScreen.kt и JarsScreen.kt — из protect в allow;
  StreetScreen.kt, NightScreens.kt, StartScreens.kt, GameViewModel.kt — из allow в protect. Итог: в protect поимённо все
  файлы `game/java` и `game/AndroidManifest.xml`, кроме пяти allow; `game/res/` каталогом (ассеты — в BASE); `docs/`,
  `finny-pet/docs/`, `app/src/main/`, `app/src/test/`, `classic/`, `finny-pet/tools/`, корневые `tools/` поимённо, `wt/`.
  Пишется ДО СПАВНА п. 4 (`scope2.py`), пересечение allow ∩ protect — пусто.
Кодеру: команды с путями из protect — без `>` и `2>&1` (WORKFLOW № 29); сборку не запускать параллельно с оркестратором.

## ANTI-SCOPE
- **Бра** (`item_home_lamp` в комнате) — вопрос № 108: замер «Бра» в КОНТЕКСТЕ (справа от часов — под вещью стены на
  360 × 640, слева — угол на холодильнике S23; координата от фона — перестройка, вида владелец не видел).
- **Размер вещей в комнате** — вариант а, 36 dp (запись); б (кресло 56–64, коврик ≈ 72 dp) — после плейтеста или по
  макету раскладки A1d2.
- **Отложено до после сдачи** (журнал «отложено», как у A1g2/A1d2): строгий зонд `python tools/art_check.py items
  [--selfcheck]` (набросок TOWN-A1e.md:758-767 — сейчас его место держит `ids_check.py` п. 9 с отрицательным контролем),
  листы и судьи (кромка на `paperTint` #F4F2F8 и `pink` #FFD6E4, серый и дейтеранопия комнат Б, В и «У Фомы» —
  GATE_QUEUE.md:308-311), S23 (рынок с Пк3 — строка эпика :43), `perf.sh` на лавке. Сдвиг полки Пк3 против BASE — не
  отложен: считается в п. 12 г по дампам BASE без сборки BASE.
- № 101 (шампунь с помпой), № 102 (кайма коврика) — малый круг `props.py` после сдачи; генератор, WebP, LICENSES — не здесь.
- Витрина A1s (плакат над банкой, № 39 а) — A1s. Мастерская, `lamp_new` на полке, С2 «Погасла лампа» — срез 2.
- `care_doctor` 🩺 (`eventOnly`, событие С4 выключено) — без картинки (ассета нет, срез 2).
- Ветка и файл `fun_bow` не трогать (в лавках нет; удаление меняет APK — набросок TOWN-A1e.md:767).
- `classic` рисует эмодзи из JSON — не меняется. Домен, ViewModel, тесты, `content.json`, ресурсы; новые зависимости.

## БЮДЖЕТ
- Код: ≤ 48 вставок и ≤ 8 удалений без строк import и пустых — прототип **+43 −5** (TownUi +31 −1: `itemRes` +19 и KDoc
  ±1, `posterRes` +5, `goalRes` +1, счётчик +4; GameApp +3 −1 +1 изменённая; PlaceScreen +7 −1: ряд и колонка +6,
  KDoc ±1 — строки заголовка и текста только сдвинуты, `-w` их не считает; RoomScreen ±1; JarsScreen ±1). Импорт +2.
  Счёт — `git diff -w BASE -- app/src | grep '^+[^+]' | grep -vc '^+import '` и то же с `'^-[^-]'` / `'^-import '`.
  Новых файлов 0, зависимостей 0.
- APK: прирост release к release BASE (4 545 064 Б) ≤ Σ 18 новых WebP 90 052 + 6 000 = **96 052 Б** — прототип +92 882
  (4 637 946 Б; dex, arsc и заголовки zip — 2 830). Доля A1e (TOWN-A1.md:66-67, ≤ 176 432 Б) — к release до ассетов A1e
  (код BASE с 4 прежними WebP = выпуск 1.4.0: 4 601 860 Б): прототип **+36 086 Б**. Остаток бюджета эпика = 4 081 493 +
  1 048 576 − размер release: прототип **492 123 Б**. Превышение — вопрос оркестратору.
- Время: кодер — один заход (дословный контракт); живая — маршрут + 6 экранов при 1,0 и 1,3.

## ORACLE
Не нужен. (1) `itemRes`, `goalRes`, `posterRes`, `LocalEmoji` живут в `game/` и возвращают `R.drawable`, а `app/src/test/`
общий для обоих вариантов: ссылок на `ru.finny.pet.game` и `R.drawable` в тестах нет, набора `src/testGame/` нет.
(2) Правило эпика: соглашение живёт в `game/`, `content.json` и `main/` не меняются — test-author не нужен (TOWN-A1.md:77;
как `residentRes`, `pastryRes`, `facadeRes`). (3) Связку «id = ветка = файл» держит `ids_check.py` (п. 9: прототип —
`IDS OK`, BASE — 38 записей FAIL, 4 мутанта — FAIL каждый), дословность — `contract_check.py` (п. 7), попадание в APK —
цикл `aapt2` (п. 10) и lint `UnusedResources` 0 (п. 4), эффект на экране — узел `emoji` с положительным контролем
(указатель ☝️ пекарни) и снимки (п. 11–14). Тестов — столько же, сколько на BASE (600 / 600).

## ДО СПАВНА (оркестратор)
`$T` — `T="$(cygpath -m "<scratchpad сессии>")/a1e2"; mkdir -p "$T"` в начале каждого вызова с `$T`; `A=tools/adbui.sh`;
`GD=finny-pet/app/src/game/java/ru/finny/pet/game`. Прототип спеки — `<scratchpad автора>/a1e2/wt` (worktree на 4e6746c,
не удалять до ревью), эталоны файлов — `…/a1e2/ref/*.kt`, дифф — `…/a1e2/proto.diff`, зонды — `…/a1e2/ids_check.py`,
`…/a1e2/contract_check.py` (тексты — «Зонды приёмки»).
0. **BASE — сделано** (4e6746c): 22 WebP = `acc2_w` побайтно; LICENSES.md:55, :61, ARCHITECTURE.md:28, REQUIREMENTS_MATRIX.md:150.
   **Поправить одним коммитом со спекой:** LICENSES.md:55 «39 WebP» → «40 WebP» (26 + 6 + 1 + 6 + 1 = 40; итог :61 «91 файл»
   верен: 11 + 3 + 9 + 40 + 6 + 8 + 14).
1. Спека → `docs/tasks/TOWN-A1e2.md` (+ LICENSES п. 0), коммит `docs(TOWN-A1e2): спека встройки вещей и товаров …`;
   `git diff --stat 4e6746c HEAD -- finny-pet/app/src` → пусто.
2. Номера строк: `grep -n '^fun itemRes' $GD/ui/TownUi.kt` → 94, `grep -n 'fun PlaceEvents' $GD/screens/PlaceScreen.kt`
   → 410, `grep -n 'starter != null' $GD/screens/RoomScreen.kt $GD/screens/JarsScreen.kt` → :257 / :204. Если до кодера
   слит 1b5 (только TrayScreen.kt) — CONTRACT не задет; сверить строку `Pic(null, "☝️"` (п. 8) и положительный контроль п. 11.
3. Зонды: `ids_check.py`, `contract_check.py` — в `$T` (копия из scratch автора или из раздела «Зонды приёмки»);
   `python $T/contract_check.py docs/tasks/TOWN-A1e2.md $GD` на BASE → `CONTRACT FAIL` — все 11 блоков (в каждом есть новая
   или изменённая строка), на прототипе (`…/a1e2/wt/finny-pet/app/src/game/java/ru/finny/pet/game`) → `CONTRACT OK 11`.
   Скрипт приёмки: `…/a1e2/acc/acc.sh` и `acc/items.py` → `$T/acc/` (зонды он берёт уровнем выше — из `$T`; спеку —
   `docs/tasks/TOWN-A1e2.md`, литералы узла `emoji` — из неё). Дампы BASE для п. 12 г:
   `cp <scratch 2368e429…>/scratchpad/dumps/{s23_shop13,longline10}.xml "$T/"` — scratch временный, копировать до прогона.
4. task-scope: `python "<scratchpad сессии 14>/scope2.py" <sha п. 1> "TOWN-A1e2 — встройка картинок вещей и товаров
   (docs/tasks/TOWN-A1e2.md)" $GD/ui/TownUi.kt $GD/GameApp.kt $GD/screens/RoomScreen.kt $GD/screens/JarsScreen.kt
   $GD/screens/PlaceScreen.kt` (из корня; `<scratchpad сессии 14>` = `…/57df72da-…/scratchpad`) → «N protect; allow […5]»;
   `node .claude/hooks/guard-paths.test.js` → exit 0.
5. Задание кодеру: «TOWN-A1e2: CONTRACT § 1–6 дословно, § 7 — импорты, § 8 — не решать; самопроверка — `./gradlew
   assembleGameDebug lintGameDebug` из finny-pet/ → exit 0 и `python <$T>/contract_check.py docs/tasks/TOWN-A1e2.md $GD`
   → `CONTRACT OK 11`; отчёт по CLAUDE.md».

## ACCEPTANCE (команды 1–6 — из finny-pet/, 7–14 — из корня; гоняет оркестратор; `$T`, `A`, `GD` — ДО СПАВНА)
П. 1–10 гоняет `GRADLE=1 bash $T/acc/acc.sh 2>&1 | tee $T/acc_out.txt` (сверх них — `items.py`, `art_check` residents /
pastries, семантика, литералы, стоп-слова, прокрутка): число строк PASS/FAIL = EXP скрипта = 45 (п. 1–10; WORKFLOW № 49),
иначе — дефект скрипта. П. 11–14 — живые, считаются в журнале отдельно.
1. `./gradlew testClassicDebugUnitTest --rerun --console=plain` → exit 0; tests 600, failed 0, skipped 0 (по XML)
2. `./gradlew testGameDebugUnitTest --rerun --console=plain` → exit 0; 600 / 0 / 0
3. `./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease` → exit 0; `./gradlew compileGameDebugKotlin
   --rerun --console=plain` → `grep '^w: ' | grep -v '/test/'` — ровно одна строка `PlaceScreen.kt:184:89 Expression is
   unused.` (давняя: на BASE `:183:89`, `"Да" to { …; Unit }` в `ShopPlace`; новых предупреждений 0)
4. `./gradlew lintClassicDebug lintGameDebug` → ошибок 0; без сетевых (ACCEPTANCE_PRESETS.md:50-53): game 5 (на BASE 23 =
   5 + 18 `UnusedResources`), classic 5; `grep -c 'UnusedResources' app/build/reports/lint-results-gameDebug.xml` → 0 (BASE 18)
5. `git diff --name-only 4e6746c -- app/src/main/ app/src/classic/ app/src/test/ app/src/game/res/ '*.gradle.kts' gradle/
   gradle.properties gradlew gradlew.bat` → пусто
6. `git status --porcelain -uall -- app/src` → только 5 путей allow; из корня `node .claude/hooks/assert-oracle-intact.js` →
   exit 0; `git diff -w 4e6746c -- app/src | grep '^+[^+]' | grep -vc '^+import '` → ≤ 48 (прототип 43);
   `… | grep '^-[^-]' | grep -vc '^-import '` → ≤ 8 (прототип 5); `git diff -w 4e6746c -- app/src | grep '^+import '` → ровно
   `+import ru.finny.pet.game.ui.LocalEmoji` и `+import ru.finny.pet.game.ui.posterRes`; `grep -c '^-import '` → 0
7. `python $T/contract_check.py docs/tasks/TOWN-A1e2.md $GD` → `CONTRACT OK 11`
8. греп по коду без строк import (№ 45): `grep -rn 'Pic(null,' $GD | grep -v ':import'` → одна строка `TrayScreen.kt:230`
   (☝️; на BASE — ещё RoomScreen.kt:257, JarsScreen.kt:204); `grep -rn 'getIdentifier' $GD` → пусто; `grep -rnE '^import
   .*\*$' $GD` → пусто; `grep -v '^import' $GD/ui/TownUi.kt | grep -c 'R.drawable.item_home_lamp'` → 2 (ветки `home_lamp` и
   `lamp_new`); `grep -rn 'item_home_lamp' $GD/screens` → пусто (бра не встроено, № 108)
9. `python $T/ids_check.py finny-pet` → `IDS OK 28 ids, 5 goals, 1 posters` (на BASE — `IDS FAIL`, 38 записей: 19 «no
   itemRes», «no goalRes: goal_camp», 18 «no ref»)
10. release `game` — R8 оставил все 22 картинки (образец — TOWN-A1g2.md:776-786):
    ```
    APK=finny-pet/app/build/outputs/apk/game/release/app-game-release.apk
    D=$("$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2.exe" dump resources $APK)
    for n in goal_camp item_care_shampoo_simple item_care_soap item_care_vitamins item_food_porridge item_food_super \
      item_fun_ball item_fun_book item_fun_carousel item_fun_icecream item_fun_kite item_fun_picture item_fun_robot \
      item_fun_rug item_fun_spinner item_fun_starlamp item_fun_tent item_gift_card item_home_armchair item_home_flower \
      item_home_lamp poster_food_super; do
      p=$(echo "$D" | grep -A1 "drawable/$n\$" | grep -o 'res/[^ ]*')
      s=$([ -n "$p" ] && unzip -l $APK "$p" | awk 'NR==4{print $1}'); w=$(stat -c%s finny-pet/app/src/game/res/drawable-nodpi/$n.webp)
      [ -n "$p" ] && [ "$s" = "$w" ] && echo "$n OK $p $s" || echo "$n FAIL path=$p size=$s src=$w"
    done
    ```
    → 22 × OK (release BASE — 4 × OK, 18 × FAIL). Размер: прирост к release BASE 4 545 064 Б ≤ 96 052 Б; доля A1e к
    4 601 860 Б ≤ 176 432 Б; остаток эпика = 5 130 069 − размер (прототип: +92 882, +36 086, 492 123) — в отчёт и TOWN-A1.md.
11. **живая — маршрут** (эмулятор AVD `finni`, обычный запуск; debug п. 3 ставится путём `cygpath -w`, флаг `DEBUGGABLE`
    в `pkgFlags` — № 33; `$A wm360`, `mute`; перед прогоном `ps -ef | grep -E "town_route|bakery_states"` пусто):
    `DUMP=1 tools/town_route.sh e2 2>&1 | tee "$T/e2_route.log"` → `grep -cE "not found|gate not passed|GEOM FAIL|CLOSE
    FAIL"` → 0; `grep -c "GEOM OK"` → 8; `grep -c "CLOSE OK"` → 2; `grep -cE "Обрезано: [1-9]"` → 0. Узел `emoji` в сырых
    дампах: `for f in market foma marketp; do for s in 10 13; do grep -o 'content-desc="Эмодзи: [^"]*"'
    finny-pet/screenshots/emu_e2_$f$s.xml; done; done` → 6 × `content-desc="Эмодзи: 0"` (на BASE узла нет; по коду BASE
    рынок демо-профиля — 6 эмодзи: в демо `unlockPeriod` не действует, Prices.kt:40); **положительный контроль**:
    `emu_e2_round10.xml` → `Эмодзи: 1 — ☝️` (указатель первой смены пекарни — единственный `Pic(null,`; не 1 — зонд или
    маршрут сломан, FAIL)
12. **живая — экраны вручную** (после маршрута, профиль недели 5; `em() { $A ui | grep -o 'Эмодзи: [^	]*' | head -1; }`;
    снимки `$A shot e2_<кадр>`, дамп рядом; каждый кадр при 1,0 и 1,3 — `$A font`):
    а) комната (`room10/13`): `em` → «Эмодзи: 0»; цветок в `spot_1` и кресло в `spot_4` — картинки;
    б) «У Фомы»: купить картину и коврик (вещь стены — в `spot_2`, вещь пола — в `spot_5`/`spot_6`: на `spot_1`, `spot_4`
       стартовые), сундук → «Обустроить» (`arrange10/13`): «Эмодзи: 0», в ячейках — картинки, у стартовых тоже; расставить →
       комната (`room_things10/13`): «Эмодзи: 0», вещи на местах;
    в) «Копилка» (`savings10/13`): «Эмодзи: 0»; в списке 5 мечт с картинками, «Походная палатка» — палатка; выбрать её →
       шапка 28 dp — палатка; улица (`street10`): «Эмодзи: 0», калитка леса — палатка 48 dp;
    г) Пк3 — новый демо-профиль по TC-40 (г) (TEST_CASES.md:123; «Создать тестовый профиль» стирает прогресс — только на
       эмуляторе): план 40 / 20 / 30 → «События» → «Супер-корм» → «Начать» → рынок (`pk3_10/13`): «Эмодзи: 0»; в
       дампе узла 192 × 192 px нет (плакат без узла TalkBack: с описанием или касанием он стал бы узлом) — `python -c "import
       re,sys; print(sum(1 for b in re.findall(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', open(sys.argv[1],
       encoding='utf-8').read()) if int(b[2])-int(b[0])==192 and int(b[3])-int(b[1])==192))" <дамп>` → 0 (положительный
       контроль — тот же счёт по дампу комнаты п. 12 а → ≥ 1: мишень «Копилка N» 64 × 64 dp, RoomScreen.kt:165);
       последняя кнопка карточки на экране без прокрутки (WORKFLOW № 38): `grep -c 'text="Пройти мимо"'` по `pk3_10.xml` и
       `pk3_13.xml` → 1 и 1;
       в журнал — для GATE_QUEUE № 96 «на сколько сдвинулась полка» (px ÷ 3 = dp при `wm360`; порога нет, контракт
       дословный — расхождение с ожиданием не FAIL кодера, а запись и вопрос № 109):
       — высота карточки = (верх `shelf` − 24 px) − (верх «! Супер-корм» − 36 px) — так же, как у BASE. Ожидание по
         нелинейной шкале шрифта (строки — по дампу, не умножением): 1,3 — вступление 5 строк, карточка ≈ 267 dp (BASE 238:
         `s23_shop13.xml` 631..1345 px, 4 строки); 1,0 — 4 строки, ≈ 212 dp (BASE 187,7: `longline10.xml` 597..1160 px);
       — сдвиг полки по К3 = верх `shelf` в `pk3_10/13` − верх `shelf` в `emu_e2_market10/13` (п. 11, рынок без карточки).
         BASE — по тем же двум дампам, сборка BASE не нужна: верх `shelf` − низ `shop_tabs` − 24 px — 1,3: 1369 − 607 − 24 =
         738 px = 246 dp, 1,0: 1184 − 573 − 24 = 587 px = 195,7 dp. Ожидание ≈ 275 и ≈ 220 dp: прирост от плаката ≈ 29 и
         ≈ 24 dp (макет К3а при 1,0 — 24 dp);
       — первый ряд полки при 1,3 (Корм и Каша, ряд 153 dp): видимая часть = низ `shelf` − верх `shelf` в `pk3_13.xml`;
         BASE ≈ 153 dp (1848 − (652 + 738) = 458 px — ряд целиком), ожидание ≈ 123 dp — дальше п. 14.
13. `python tools/ui_measure.py <дамп> <кадр> 3` по дампам п. 11–12 → `small_targets` пусто, `overflow` «Обрезано: 0»,
    `offscreen: []` (исключения — как TOWN-A1g2.md:771-775).
14. глазами (снимки п. 11–12): картинки квадратные, не растянуты и не обрезаны; стартовые стоят на своих местах, как на
    листе К2; плакат слева от текста по центру карточки, текст карточки целиком (при 1,3 — тоже), «Пройти мимо» видна;
    карточки лавок: картинка, название, цена — как на BASE; нигде нет кружков-эмодзи, кроме ☝️ в пекарне.
    По `pk3_13.png` — видна ли строка «Сытость +40» у «Корма» и «Каши» (на ней держится урок Пк3). Ожидаемо — нет: ряд
    виден ≈ 123 dp, «+40» начинается с ≈ 130 dp от верха карточки товара. Это не FAIL кодера (контракт дословный) —
    запись в журнал и вопрос № 109 (ВОПРОСЫ НА ВОРОТА).

## ЖИВАЯ ПРОВЕРКА (человек: docs/BACKLOG.md п. 1 и 11)
TalkBack вручную (картинки не читаются, карточка события — заголовок, текст, «Пройти мимо»); S23 — рынок с Пк3 и комната
(строка эпика :43) — после сдачи.
TalkBack — на release из п. 3 (`install -r` поверх debug, подпись та же, профиль сохраняется). На debug первыми в верхнем
ряду читаются отладочные узлы «Эмодзи: N» (слева сверху) и «Обрезано: N» (справа сверху) — ожидаемо, находкой не считаются.
На S23 бэкап профиля (`run-as`, HANDOFF.md:150-152) снять ДО установки release — у release нет `run-as`; назад — `install -r`
debug. Код зонда не трогать: `invisibleToUser` спрятал бы узел и от uiautomator, счётчик п. 11–12 сломался бы.

## ВОПРОСЫ НА ВОРОТА (в GATE_QUEUE — следующий свободный номер после разделов 9–10: A1d2 № 103–104, A1g2 № 105–107 → № 108; сверить перед записью)
**№ 108. Бра на высоте часов** (решение «а, но поднять на высоту часов» не встало в раскладку — замер «Бра»).
а) слева от часов, под мебелью: 360 × 640 — свободно (до холодильника 11 dp); S23 — холодильник закрывает угол абажура
   4 × 9 dp («лампа за холодильником»); цена — координата от фона, как у холодильника (≈ 4 строки), лист 360 × 640 + S23;
б) справа от часов: S23 свободно; на 360 × 640 картина или змей места «стена» ложится на верх бра (18 × 4 / 9 × 8 dp) —
   две вещи одной стены наезжают;
в) не показывать до среза 2: в С2 «Погасла лампа» бра станет мишенью 48 dp «К Степану» и получит место в раскладке.
**Рекомендую а**: заслон мебелью читается глубиной (холодильник стоит перед стеной), а наезд двух вещей стены — ошибкой;
бра узнано судьёй при 36 dp. Условие — лист на двух экранах до встройки.

**№ 109. Плакат Пк3 при шрифте 1,3** (№ 108 + 1; сверить перед записью). Плакат строкой сужает колонку текста карточки,
при 1,3 вступление растёт на строку (4 → 5), карточка — ≈ на 29 dp (при 1,0 — на 24 dp, как на макете К3а). На 360 × 640
первый ряд полки (Корм и Каша) виден ≈ на 123 dp из 153 (BASE — целиком), строка «Сытость +40», на которой держится урок
Пк3, — под краем (факт — п. 12 г и п. 14, журнал).
а) принять: «Супер-корм» при 1,3 и в BASE стоит под краем — урок Пк3 и так идёт через прокрутку;
б) плакат только при `!bigFont()` — одно условие в § 6; при 1,3 прирост 0 dp, но ребёнок с крупным шрифтом плаката не видит.
Вариант «плакат 48 dp» не ставится: при 1,3 он оставляет те же 5 строк вступления.
**Рекомендую а**: сравнение «+40 у обоих» при 1,3 и без плаката требует прокрутки, а б даёт ребёнку с крупным шрифтом
урок без картинки рекламы.

## ДОКИ (оркестратор, после приёмки и живой, ДО ревью — № 46; черновик в scratch; `git grep` по `docs`, `finny-pet/docs`)
- `docs/BACKLOG.md:113-117` «Картинка «Ванны с пеной» — ещё витаминки» → в «Закрыто» (ванна с пеной — `item_care_vitamins`
  круга A1e1-2, BASE 4e6746c; в полке «У Фомы» с A1e2); `git grep -n "ещё витаминки" -- docs finny-pet/docs ':!docs/tasks'` → пусто.
- `finny-pet/docs/LIMITATIONS_ROADMAP.md:32` «Товары — без своего арта … Товар без картинки — эмодзи» → товары и вещи — свои
  картинки (A1e2), эмодзи — только у товара без ветки и у бра нет картинки в комнате (№ 108); :56 «осталось — витрина лавки
  (A1s), товары и вещи (A1e2)» → «осталось — витрина лавки (A1s)».
- `finny-pet/docs/QUESTIONS.md:39` «Не у всех вещей свой рисунок: … эмодзи и прежние картинки» → свой рисунок у всех
  товаров двух лавок и вещей комнаты; нет только бра (№ 108) и вещей среза 2 (мастерская, доктор).
- `finny-pet/docs/ARCHITECTURE.md:174` и `CONTENT_MAP.md:269-271` — плакат события: WebP `poster_<itemId>` и строка в
  `posterRes` ([ui/TownUi.kt]); стартовая вещь — `item_<homeItemId>` в `itemRes`; без строки — эмодзи (события — без плаката).
- `finny-pet/docs/UX_ACCESSIBILITY.md` — у таблицы картинок без описания (:68 / :351 — рядом): плакат события 64 dp без
  описания (текст карточки читается), стартовые вещи и вещи мест — без описания (мишень или ячейка читает название);
  рядом с зондом обрезки (:279-282, «в release узла нет») — узел `emoji` «Эмодзи: N — …» слева сверху, только debug; «в
  debug оба узла читаются TalkBack; TalkBack проверять на release».
- `docs/BACKLOG.md:81` (п. 11 «Полный прогон TalkBack») — дописать: «TalkBack — на release (`install -r` поверх debug,
  подпись та же, профиль сохраняется; на S23 бэкап профиля через `run-as` — до установки release). На debug первыми в
  верхнем ряду читаются отладочные узлы «Эмодзи: N» (слева сверху) и «Обрезано: N» (справа сверху) — ожидаемо, находкой не
  считаются».
- `docs/BACKLOG.md` «Осталось» — новая строка (следующий номер после 23; было до A1e2, не регрессия задачи): «Место
  «Обустроить» — выбор только цветом | 3.6 | срок не назначен | выбранная ячейка (`JarsScreen.kt:197-200`) отмечена только
  фоном `G.pink` вместо `paperTint`, в семантике нет `selected`; с A1e2 в розовой ячейке — картинки вещей, выделение
  сливается сильнее. Опора — инвариант CLAUDE.md «цвет не единственный признак» и `CheckBadge` (`Widgets.kt:161`). Рецепт —
  как у списка мечт (`SavingsScreen.kt:84`, :91): в тело ячейки (`Box`, BoxScope) — `if (picked == spot.id) CheckBadge()`,
  в `clearAndSetSemantics` — `selected = picked == spot.id`; или `clickable(role = Role.Button)` → `selectable(selected =
  picked == spot.id, role = Role.RadioButton)`, как в `GameButton` (`Widgets.kt:123`). После правки — «место
  «Обустроить»» в `finny-pet/docs/UX_ACCESSIBILITY.md:127`».
- `finny-pet/docs/ARCHITECTURE.md:73` — в списке `CompositionLocal` после `LocalClipped` — `LocalEmoji` (счётчик эмодзи
  вместо картинки; узел `emoji` «Эмодзи: N — …» — только в debug).
- `docs/GAME_CONCEPT.md:2135` (№ 96 «Ждёт листа A1e1…») → решено: б (круг A1e1-2), входы A1e2 — бра а с подъёмом (не
  встало → № 108), плакат строкой, размер а (36 dp); § 18 — строки № 108 и № 109 (после ответа); GATE_QUEUE № 96 «на
  сколько сдвинулась полка» — факт п. 12 г.
- `docs/tasks/TOWN-A1.md:43` статус «A1e2 — встройка» → «A1e2 сделано (…; бра — № 108)»; :66-67 — факт доли A1e и остаток
  (п. 10); `docs/tasks/GATE_QUEUE.md` раздел 7 — входы A1e2 решены, раздел 11 «Вещи — TOWN-A1e2» с № 108 и № 109;
  `docs/HANDOFF.md`.
- `finny-pet/docs/LICENSES.md:55` — «39» → «40» (ДО СПАВНА п. 0, если не сделано).
- Проверка: `git grep -n -i "эмодзи" -- docs/BACKLOG.md finny-pet/docs/LIMITATIONS_ROADMAP.md finny-pet/docs/QUESTIONS.md` —
  каждая строка про товары и вещи верна по коду.

## ПРИ БЛОКЕРЕ
`STATUS: BLOCKED` с одним вопросом и остановка, если: блок CONTRACT не встаёт дословно (строки BASE не те — назвать
файл и строку); сборка или lint падают на дословном коде; нужен импорт, файл или правка вне allow; `R.drawable.<имя>` не
находится (ресурса нет в BASE). Бра, размеры, описания картинок — не блокер, а § 8: не делать.

## Зонды приёмки (тексты)
`ids_check.py` (в scratch автора — рядом со спекой):
```python
# TOWN-A1e2: ids of the two shops + lamp_new + fun_lego + town.homeItems have an itemRes branch, town.goals — a goalRes
# branch; every branch's file exists; every item_*/goal_*/poster_* file is referenced. Exceptions: branch fun_bow (not on
# the shelves), file goal_custom (own/demo dream branch). python ids_check.py ROOT [TOWNUI] → IDS OK / IDS FAIL [...], exit 0/1
import json, os, re, sys
root = sys.argv[1]; ui = sys.argv[2] if len(sys.argv) > 2 else root + '/app/src/game/java/ru/finny/pet/game/ui/TownUi.kt'
c = json.load(open(root + '/app/src/main/assets/content/content.json', encoding='utf-8'))['town']
t = open(ui, encoding='utf-8').read().replace('\r\n', '\n')
body = lambda f: t.split('fun %s(' % f)[1].split('\n}\n')[0]
item = dict(re.findall(r'^    "(\w+)" -> R\.drawable\.(\w+)$', body('itemRes'), re.M))
goal = dict(re.findall(r'^    id == "(\w+)" -> R\.drawable\.(\w+)$', body('goalRes'), re.M))
poster = re.findall(r'^    "\w+" -> R\.drawable\.(poster_\w+)$', body('posterRes'), re.M) if 'fun posterRes(' in t else []
ids = {s['item'] for sh in c['shops'] if sh['id'] in ('shop_market', 'shop_foma') for s in sh['sells']} | {'lamp_new', 'fun_lego'} | {h['id'] for h in c['homeItems']}
res = set(item.values()) | set(goal.values()) | set(poster) | {'goal_custom'}
d = root + '/app/src/game/res/drawable-nodpi/'
bad = ['no itemRes: ' + i for i in sorted(ids - set(item))] + ['no goalRes: ' + g['id'] for g in c['goals'] if g['id'] not in goal]
bad += ['no file: ' + r for r in sorted(res) if not os.path.exists(d + r + '.webp')]
bad += ['no ref: ' + f[:-5] for f in sorted(os.listdir(d)) if f.startswith(('item_', 'goal_', 'poster_')) and f[:-5] not in res]
bad += ['extra branch: ' + i for i in sorted(set(item) - ids - {'fun_bow'})]
print('IDS OK %d ids, %d goals, %d posters' % (len(ids), len(goal), len(poster)) if not bad else 'IDS FAIL %s' % bad)
sys.exit(1 if bad else 0)
```
`contract_check.py`:
```python
# contract_check.py — TOWN-A1e2: каждый блок CONTRACT (```kotlin сразу после строки «<!-- file: PATH -->») — непрерывная
# подстрока файла GAME_DIR/PATH (CRLF → LF), то есть код вставлен дословно, с отступами.
# python contract_check.py SPEC GAME_DIR → CONTRACT OK N / CONTRACT FAIL [...], exit 0/1
import re, sys
spec = open(sys.argv[1], encoding='utf-8').read().replace('\r\n', '\n')
blocks = re.findall(r'<!-- file: (\S+) -->\n```kotlin\n(.*?)```', spec, re.S)
bad = [f + ': ' + code.splitlines()[0].strip()[:70] for f, code in blocks
       if code not in open(sys.argv[2] + '/' + f, encoding='utf-8').read().replace('\r\n', '\n')]
print('CONTRACT OK %d' % len(blocks) if blocks and not bad else 'CONTRACT FAIL %s' % (bad or 'no blocks'))
sys.exit(1 if bad or not blocks else 0)
```

## Журнал спеки
- **2026-09-29, сессия 15 — спека по прототипу** (автор; worktree `<scratchpad автора>/a1e2/wt` detached на 4e6746c,
  `local.properties` скопирован; ассеты уже в BASE — `cmp` с `acc2_w` 22 × без разницы). Прототип — 5 файлов allow:
  - сборка `./gradlew assembleGameDebug assembleGameRelease lintGameDebug testGameDebugUnitTest --rerun-tasks` и отдельно
    `assembleClassicDebug lintClassicDebug testClassicDebugUnitTest` → exit 0; тесты 600 / 0 / 0 × 2; `compileGameDebugKotlin
    --rerun` — одно предупреждение `PlaceScreen.kt:184:89` (на BASE то же — `:183:89`); lint без сетевых — game 5, classic 5,
    `UnusedResources` 0 (BASE — 23 и 18);
  - дифф `-w` без import и пустых: +43 −5; import +2; сырой `git diff --shortstat` — 5 файлов, +49 −7 (две строки
    `PlaceEvents` только сдвинуты); `proto.diff` и `ref/*.kt` — рядом со спекой;
  - release: BASE 4 545 064 Б, прототип 4 637 946 Б (+92 882 = 18 WebP 90 052 + 2 830); код BASE с 4 прежними WebP (выпуск
    1.4.0) — 4 601 860 Б → доля A1e +36 086 Б из 176 432; остаток эпика 492 123 Б; `aapt2` — прототип 22 × OK, BASE 4 × OK;
  - `ids_check.py`: прототип → `IDS OK 28 ids, 5 goals, 1 posters`; BASE → `IDS FAIL` (38 записей); мутанты — `gift_toy` на
    несуществующий `item_gift_toy` → «no file», лишняя ветка `zzz` → «extra branch», `posterRes` на чужой ресурс → «no ref:
    poster_food_super», ветка `goal_camp` в комментарии → «no goalRes» + «no ref»;
  - `contract_check.py` по этой спеке: прототип → `CONTRACT OK 11`; BASE → `CONTRACT FAIL` 11 из 11; мутанты — отступ ветки
    `fun_kite` 3 пробела вместо 4 → FAIL блока § 1, U+00A0 вместо пробела в строке узла `emoji` → FAIL блока § 3 (урок 3
    сессии 15);
  - счётчик эмодзи — 7 новых строк + 1 изменённая + импорт (≤ 10 — по наброску); живьём не проверялся (эмулятор — не у автора);
  - бра — замер по `room_port_day` (обод часов 335..447 × 928..1039 px), `item_home_lamp` (α > 127: 32..184 × 10..202 из 216),
    раскладке A1d2 (TOWN-A1d2.md:91-100) и спрайтам мест (`item_fun_picture` 11..205 × 35..180, `item_fun_kite` 68..148 ×
    11..204, `furn_fridge` 13..180 × 10..285 из 192 × 288) → таблица КОНТЕКСТА, ANTI-SCOPE и № 108;
  - найдено в BASE: LICENSES.md:55 «39 WebP» при 26 + 6 + 1 + 6 + 1 = 40 (итог :61 «91» верен) — ДО СПАВНА п. 0.
- **2026-09-29, сессия 15 — круг критиков спеки** (8 записей: 7 подтверждённых находок и 1 «не находка»; приняты все 7;
  код прототипа не менялся — `proto.diff` и `ref/` прежние):
  - 1–3 (`acc.sh` против CONTRACT и ACCEPTANCE): массив CL убран — дословность держит `contract_check.py` (п. 7); новые
    `ok` — `ids_check` (п. 9), `R.drawable.item_home_lamp` в TownUi = 2 и `screens` без `item_home_lamp` (п. 8),
    предупреждения компилятора — одно `PlaceScreen.kt:184:89` (п. 3), lint classic без сетевых ≤ 5 (п. 4), доля A1e к
    4 601 860 ≤ 176 432 (п. 10); `LIT_EXTRA` — из строки узла § 3 этой спеки (5 литералов, иное — FAIL); пороги по БЮДЖЕТУ:
    48 / 8, `+import` — ровно `LocalEmoji` и `posterRes`, APK_BASE 4 545 064, APK_MAX 96 052; EXP 44 → 45 (38 + 4 + 3);
    ACCEPTANCE «= 14 пунктам» → «= EXP скрипта (п. 1–10); п. 11–14 — живые». Попутно по прогону на wt: `ITEMS_ROOT` из `R`
    (`items.py` читал основное дерево — ложный MISMATCH), XML тестов — с префиксом `\?\` (путь в wt 278 символов > 260).
    Прогон: прототип (`R=…/wt SPEC=…/TOWN-A1e2.md GRADLE=1`, task-scope A1e2 от `scope2.py` в wt) → `ИТОГ PASS 45 / FAIL
    0`, exit 0; BASE (основное дерево 4e6746c, без GRADLE) → 18 FAIL: п. 7 (CONTRACT), 8 (`Pic(null,` — 3 строки, бра — 0),
    9 (ids_check, items.py), 10 (aapt2 0 × OK), плюс ожидаемые — gradle «на сухую», пустой дифф, импорты, литералы,
    описание; SPEC на несуществующий файл → FAIL литералов («LIT_EXTRA 0») и CONTRACT.
  - 4 (Пк3 при 1,3): п. 12 г — ожидания по нелинейной шкале, высота карточки и сдвиг полки против BASE по дампам
    `s23_shop13.xml` и `longline10.xml` (сборка BASE не нужна), видимая часть первого ряда; п. 14 — «+40» у Корма и Каши
    глазами; вопрос № 109 (а — принять, б — `!bigFont()`; рекомендую а); ANTI-SCOPE — сдвиг полки больше не отложен.
  - 5 (TalkBack на debug читает «Эмодзи: N»): ЖИВАЯ ПРОВЕРКА — TalkBack на release, отладочные узлы на debug ожидаемы,
    бэкап S23 до release; ДОКИ — BACKLOG п. 11 и UX_ACCESSIBILITY (:279-282); код зонда не меняется.
  - 6 (описание плаката): ЧТО УВИДИТ РЕБЁНОК — банка супер-корма в лучах, как TOWN-A1e.md:121; п. 14 не менялся.
  - 7 (выбор места «Обустроить» только цветом — было до A1e2): объём не расширен, строка BACKLOG с рецептом — в ДОКИ.
  - 8 (не находка): двойного чтения, новых мишеней и наложения бра нет; карточка события без плаката — прежняя раскладка.
- **2026-09-29, сессия 15 — доки после приёмки, оговорка о TalkBack.** Формулировки «на debug первыми в верхнем ряду
  читаются» (ЖИВАЯ ПРОВЕРКА, :441) и «в debug оба узла читаются TalkBack» (ДОКИ, :479) — вывод по коду, а не наблюдение:
  узлы `overflow` и `emoji` — `Box` 1 dp с `contentDescription` в блоке `BuildConfig.DEBUG` (`GameApp.kt`, `TopEnd` и
  `TopStart`), uiautomator их видит, живого прогона TalkBack не было. «Первыми» неточно: «Обрезано: N» — справа сверху, не
  в начале ряда. В доки ушло с оговоркой «вероятно; не проверялось» — `finny-pet/docs/UX_ACCESSIBILITY.md` (счётчик эмодзи),
  `docs/BACKLOG.md` п. 11. Текст спеки выше не менялся.
- **2026-09-29, сессия 15 — кодер, приёмка, живая.** Кодер — 5 файлов allow, +43 −5 без import, import +2, `CONTRACT OK 11`.
  **Машинная приёмка** (`<scratchpad сессии 15>/a1e2/acc/acc.sh`, `GRADLE=1`): PASS 45 / FAIL 0, проверок 45 из 45; release
  4 637 946 Б (+92 882 к BASE, доля A1e +36 086 из 176 432), R8 оставил 22 файла из 22. **Живая** (emulator-5554, 360 × 640,
  debug md5 f6b916b2… = установленный, DEBUGGABLE): п. 11 — `town_route.sh e2`: промахов 0, `GEOM OK` 8, `CLOSE OK` 2,
  обрезки 0; узел `emoji` — 6 × «Эмодзи: 0» (market, foma, marketp при 1,0 и 1,3), положительный контроль round10/13 —
  «Эмодзи: 1 — ☝️». П. 12 а–в — «Эмодзи: 0» и «Обрезано: 0» на комнате, «Обустроить», комнате с покупками (картина → spot_2,
  коврик → spot_5), копилке (5 мечт картинками, палатка в шапке), улице (калитка леса — палатка). П. 12 г (Пк3, TC-40 г):
  карточка 211,7 dp при 1,0 (4 строки) и 267,7 dp при 1,3 (5 строк); сдвиг полки 219,7 / 275,7 dp (BASE 195,7 / 246) —
  плакат +24,0 / +29,7 dp; первый ряд полки при 1,3 виден 123 из 153 dp — «Сытость +40» у Корма и Каши уходит под край
  (ожидание спеки, № 109); «Пройти мимо» без прокрутки 1 и 1. **Расхождение посылки зонда (не кода):** узел 192 × 192 в
  pk3_10/13 — 1 (ожидание 0): пустой узел плаката без text/content-desc, clickable и focusable false — TalkBack, вероятно,
  его пропустит; проверить TalkBack вручную (BACKLOG п. 11). П. 13 — `ui_measure` 45 дампов: мелких целей 0 на 42 кадрах, на 3
  доп. кадрах (foma_p2, savings_goals, savings_camp13) — карточки, срезанные прокруткой у края (исключение TOWN-A1g2.md:771-775).
  П. 14 — глазами: картинки квадратные, стартовые на местах, плакат слева от текста, эмодзи нет (кроме ☝️). Лист —
  `town/a1e2_gate.jpg`. Вне задачи: на карточке «Робот» (распродажа недели 5) «такая распродажа бывает» переносится посреди
  слова — строка BACKLOG.
- **2026-09-29, сессия 15 — ревью.** Круг 1 — FAIL только за доки: `UX_ACCESSIBILITY.md` утверждал, что у вещей мест в
  комнате нет своего узла и порядок узлов карточки события прежний; дампы приёмки — пустые узлы без описания и фокуса, плакат —
  пустым узлом перед заголовком. Абзац переписан (порядок ЧИТАЕМЫХ узлов прежний; TalkBack вживую — BACKLOG п. 11). Круг 2 —
  **PASS**, FINDINGS пусто; код не менялся (5 файлов allow, +43 −5, `CONTRACT OK 11`, `IDS OK 28 ids, 5 goals, 1 posters`).

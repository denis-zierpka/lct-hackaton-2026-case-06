```
TASK: TOWN-J1-1b1-3 — итог смены и значок события по ответам ворот j1b12: сторона питомца по замеру облачка (№ 83 в),
      питомец на 14 dp ниже — лапы на полу (№ 87 б), незаработанная звезда — контур ☆ вместо ● (№ 88 б), «!» события
      32 dp и `#E0004A` на плашке «В городке» (№ 85 б)
EPIC: TOWN-J1 (docs/tasks/TOWN-J1.md; срез 1б — TOWN-J1-1b.md; образцы — TOWN-J1-1b1-2.md, TOWN-J1-1b4.md; ворота j1b12 —
      GATE_QUEUE.md раздел 2 :27-68, лист finny-pet/screenshots/town/j1b12_questions.jpg)
BASE: коммит спеки и зонда «tools(TOWN-J1-1b1-3): …» (для кодера и ревьювера); код `app/src` = 8235e47 (1b4). HEAD при
      сведении b98298b (после 0538492 — только A1d1 / A1e / A1g1: `room.py`, `facade.py`, `tools/art_check.py`, доки A1*,
      HANDOFF), дерево чистое; `git diff --stat 8235e47 HEAD -- finny-pet/app/src` пуст. Номера строк — рабочее дерево
      2026-09-29 на b98298b (доки эпика с 0538492 не менялись, HANDOFF — сдвинут)
BRANCH: feat/town
ЗАВИСИТ ОТ: TOWN-J1-1b1-2 (70928df, PASS), TOWN-J1-1b4 (8235e47, PASS); решения владельца 2026-09-29 «короче делай все по
      рекомендации, дальше посмотрю по скринам уже» — № 83 в, 84 а, 85 б, 86 а, 87 б, 88 б (GAME_CONCEPT.md:2116-2121)
```

## КОНТЕКСТ
Код меняют четыре ответа:
- **№ 83 в** (GATE_QUEUE.md:36-42) — сторона питомца на итоге **по месту**: справа под облачком, если облачко помещается;
  перед жителем — только когда нет. «Облачко мерить без потолка, сторона — по замеру облачка, а не по шрифту». Сейчас —
  `val side = !bigFont()` (RoundScreen.kt:107): при 1,3 питомец всегда перед жителем, «ребёнок» в 6 из 16 кадров принял
  его за детёныша (WORKFLOW № 42). После правки питомец перед жителем, только когда облачко выше места справа
  (`floor − 120 dp`): на 360 × 640 при 1,3 это любой итог пекарни со ★ (1–4★ — 308 dp, с новым уровнем — 336 dp);
  справа — 0★ без нового уровня (273 dp) и Марта (249 dp); на S23 — справа всегда. Остаток «детёныша» на маленьком
  экране — почти каждая смена пекарни (нужна хоть одна ★), а не два крайних итога: владельцу — строкой листа (п. 11).
- **№ 87 б** (:57-60) — питомец ниже на 14 dp: у спрайта снизу прозрачное поле, лапы «висят» на стенке прилавка;
  «1 строка + правило зонда `result_geom.py`».
- **№ 88 б** (:61-63) — незаработанная звезда в `StarRow` — контурная ☆ того же цвета вместо ● («потерянная звезда или
  шарик?» — 4 кадра); признак — форма, а не цвет.
- **№ 85 б** (:47-51) — «!» события на карточке места улицы 32 dp вместо 28: белая обводка 2 dp рисуется поверх заливки
  (`border` раньше `background`), видимый красный круг сейчас 24 dp (снимок `emu_s13er_street.png`: рамка плоских
  пикселей `#E0004A` 72 × 72 px) — станет 28; тот же `#E0004A` на плашке «В городке» в комнате (RoomScreen.kt:115, сейчас
  `G.magenta` `#FF0053`: белый «!» 3,91 : 1 → 4,92 : 1).

Без кода: **№ 84 а** (круг ✕ строки питомца `#F4F2F8` — как есть), **№ 86 а** («+N всего» и «✉ +N» — плейтест 1, BACKLOG
п. 18). Что видит ребёнок: при крупном шрифте питомец стоит справа от жителя везде, где облачко помещается; лапы — на
полу, перед прилавком; в ряду звёзд пустая звёздочка ☆ вместо чёрного кружка; красный круг «!» на улице снова 28 dp,
в комнате — того же красного. Тексты, TalkBack, домен — как были.

### Замер для № 83 в (дампы 1b1-2 `$TEMP/s13{eb,er,sb,sr}_*_geom.xml`; раскладка итога с 1b1-2 не менялась)
Высота облачка `job_result`, dp (ширина облачка от стороны не зависит — `maxWidth − bx`):

| итог | 360 × 640, 1,0 | 360 × 640, 1,3 | S23, 1,3 |
|---|---|---|---|
| levelup (4★, «новый уровень 2») | 267,7 | **336,3** | 336,3 |
| result / result2 (3★ / досрочный 1★) | 267,7 | **308,3** | 308,3 |
| zero (0★) | 216,0 | 273,0 | 273,0 |
| tresult (Марта) | 214,0 | 249,0 | 249,0 |
| потолок справа `floor − 120 dp` | 278,0 | 278,0 | 434,0 |

Совпадает с GATE_QUEUE.md:36-40. Не помещаются справа жирные: итог хотя бы с одной ★ и итог с новым уровнем. Высота ряда
зависит от того, есть ли ★ (★ с «+1» под ней выше ● / ○ — TrayScreen.kt:422-426), а не от их числа; текст оплаты у 1–4★
одной формы (Town.kt:762): 1★ (`result2`) = 3★ = 308,3 dp. Ближе всех к порогу zero при 1,3 — 5 dp (15 px). Не мерились
(на кадрах приёмки их нет): 0★ в смену нового уровня (оценка ≈ 301 dp — перед жителем) и Марта с новым уровнем (≈ 277 dp —
в полосе зонда ± 1 dp, возможен ложный красный — тогда правится зонд, п. 1).
**Следствие для № 88 б:** ряд zero (0★) — четыре значка без ★, его высота = высота значка. ☆ 24 dp подняла бы ряд с 16
до 24 dp, облачко zero при 1,3 — до 281 dp > 278, и zero ушёл бы «перед жителем» — против решения № 83 в. Поэтому ☆ —
16 dp, как ● и ○ (§ 3).

### Замер для № 87 б
Все 108 спрайтов `pet_*` (`main/res/drawable-nodpi`, 512 px; вид × окрас × стадия × лицо): нижняя строка с α > 128 —
440 / 512 = 0,859 кадра. `PetSprite` масштабирует стадию вокруг `TransformOrigin(0.5f, 0.86f)` (PetSprite.kt:126; стадии
0,86 / 0,93 / 1 — :103) — линия лап от стадии не зависит. Поле под лапами при 96 dp — 13,5 dp; опускание на 14 dp ставит
лапы на `floor + 0,5 dp` (низ полосы прилавка и низ кадра жителя). Тень — 0,835…0,89 кадра (:118): `floor − 1,8 … +3,4 dp`.
Ноги жителей — 0,957 кадра (`res_*.webp`): у Марты (прилавка нет) лапы питомца на ≈ 10 dp ниже её ног — «ближе к
зрителю»; вид — судьям (п. 11).

### Карта кода (рабочее дерево, проверено)
| Что | Где сейчас | Правка |
|---|---|---|
| сторона по шрифту | RoundScreen.kt:107 `val side = !bigFont() // …` | удалить (§ 1) |
| `floor`, кадр жителя, `bx`, `head` | :109-112 (`floor = maxHeight − 64.dp`; `if (bigFont()) 216.dp else 232.dp` — № 63 в3) | без изменений |
| привязки хвоста | :114 `residentRect`, `bubbleRect` | без изменений |
| житель, прилавок | :115, :116 | без изменений |
| питомец | :117-120, `Modifier.offset(x = if (side) maxWidth - 104.dp else 8.dp, y = floor - 96.dp)` | первое содержимое `Layout` (§ 1, 2) |
| облачко | :121-143; `Modifier.layout { … }` :122-126; `onGloballyPositioned`, потолок `heightIn(max = floor - 16.dp - (if (side) 104.dp else 0.dp))` :127; содержимое :133-142 | второе содержимое `Layout` (§ 1) |
| хвост, кнопки | :144-152, :153-156 | без изменений |
| импорт | RoundScreen.kt:32 `import androidx.compose.ui.layout.layout` | → `…layout.Layout` перед `boundsInParent` (:31) |
| звезда | TrayScreen.kt:138-143 `Star(size: Dp)` (`starPath` :129-136); `StarRow` :414-430 (KDoc :414, ● :425, ○ :426, ★ :422-424); вызовы `StarRow` — ряд раунда TrayScreen.kt:218, итог RoundScreen.kt:134; `Star(24.dp)` — :370 («Спасибо! ★ +1»), :423 | § 3 |
| «!» улицы | StreetScreen.kt:91 `.padding(4.dp).size(28.dp).border(2.dp, Color.White, CircleShape).background(Color(0xFFE0004A), CircleShape)` | `28.dp` → `32.dp` (§ 4 а) |
| «!» комнаты | RoomScreen.kt:115 (`TownCard`) `Box(Modifier.size(28.dp).background(G.magenta, CircleShape), …)` на белой `chip()` (TownUi.kt:89) | → `Color(0xFFE0004A)` (§ 4 б); «!» окна :142 — не трогать (A1d2) |

`PetSprite` (PetSprite.kt:55-…) — один корневой `Box(modifier.size(size))`, `onTap` итог не передаёт (узла-действия нет).
`bigFont()` — TownUi.kt:86; после правки в `ShiftResult` читается только кадром жителя (:110). Узел шапки `hud2` —
TownUi.kt:294 (`testTag("hud2")`, нужен зонду).

## ТРЕБОВАНИЕ ТЗ (docs/sources/ТЗ_текст.txt)
2.1 «короткую и понятную обратную связь по итогам принятых решений» — итог смены: кто благодарит, сколько заработано,
сколько звёзд; 3.5 «не должны запугивать, стыдить» — незаработанная звезда пустая, а не «чёрная метка»; питомец стоит на
полу, а не висит; 3.6 «Цвет не является единственным способом передать ошибку, успех…» — ★ / ☆ / ○ различаются формой и
заливкой, «!» — знаком на `#E0004A` (4,92 : 1); мишени 48 dp и текст 16 sp не меняются (значки не кликабельны, клик — у
карточки); «читаемость сохраняется при системном увеличении шрифта» — сторона питомца при 1,3 по месту, без обрезки.

## CONTRACT
Новых строк ребёнку и описаний TalkBack — 0; строковых литералов в диффе — 0 (п. 8). Домен, ViewModel, `content.json`,
ресурсы, `PetSprite`, `SpeechBubble`, `Counter`, `LineHost`, хвост (:144-152), кнопки итога (:153-156), кадр жителя — не
меняются. «Крупный шрифт» — только кадр жителя (`bigFont()` :110, № 63 в3); для стороны питомца шрифт не читается.

**Как uiautomator видит узлы Compose** (как TOWN-J1-1b1-2 CONTRACT): `bounds` — непокрытая часть узла; режут только узлы,
важные для TalkBack и нарисованные позже; узел с одним `testTag` никого не режет. Следствие № 87 б: низ кадра питомца
(`floor + 14`) уходит под ряд кнопок (верх `floor + 3`), в дампе у `result_pet` низ = верх кнопки, высота 85 dp — правило
зонда «высота ≥ 96·d − 2» заменено (ДО СПАВНА п. 1). По ширине кнопка закрывает кадр питомца целиком при обеих сторонах
(«Готово» 184…352 dp экрана против 248…344; «Почему?» 8…176 против 16…112; без «Почему?» «Готово» во всю ширину).

### 1. Сторона питомца по замеру облачка (№ 83 в) — RoundScreen.kt, `ShiftResult`
**а) Как мерить.** Облачко мерится ОДИН раз — под общим потолком сцены «перед жителем» `floor − 16 dp`
(`heightIn(max = floor - 16.dp)`); сторона справа, если измеренная высота ≤ `(floor − 120 dp).roundToPx()` (потолок справа
прежний: `floor − 16 − 104`, над питомцем 96 dp и зазором 8). Это и есть «мерить без потолка»: потолок справа меньше
общего, поэтому `natural ≤ capSide ⇔ min(natural, capFront) ≤ capSide` — решение то же, второй замер не нужен (дважды
мерить один `Measurable` в проходе нельзя). Мерить с потолком стороны нельзя: замер всегда ≤ порога — питомец справа
всегда, а облачко обрезано (мутант зонда «всегда справа», ДО СПАВНА п. 1).

**б) Где решать.** Сторону знает только замер облачка, а место нужно и облачку, и питомцу. Поэтому питомец и облачко —
два содержимых одного `Layout(contents = …)`: облачко мерится первым, сторона вычисляется в том же `MeasurePolicy`, оба
ставятся в одном проходе раскладки — одно решение, без состояния, без второго кадра. Порядок размещения (питомец, затем
облачко) = порядок отрисовки, как сейчас. Отклонено:
- питомец по `bubbleRect` (`onGloballyPositioned` → состояние) и облачко по своему `pl.height` в `Modifier.layout` — одно
  решение в двух местах: питомец появляется кадром позже облачка (до первого замера его нет), а при смене высоты облачка
  кадр стоит по прошлому замеру; с умолчанием стороны вместо «нет питомца» — перескок со стороны на сторону;
- запись в состояние из `Modifier.layout` облачка и чтение в `offset { }` питомца — backwards write, лишний проход,
  зависимость от порядка детей `Box`;
- `SubcomposeLayout` — лишняя подкомпозиция: оба содержимых известны при композиции, от замера зависит только место;
- intrinsic (`maxIntrinsicHeight`) — `heightIn(max)` в цепочке его зажимает, и соседу всё равно нужен ответ.

**в) Хвост и прилавок.** `Layout` — без `Modifier`: заполняет `BoxWithConstraints` (`layout(c.maxWidth, c.maxHeight)`) и
стоит в (0, 0), поэтому `boundsInParent()` облачка (родитель — `Layout`) в тех же координатах, что `residentRect` жителя и
`offset` хвоста (родитель — `Box`). Облачко больше не двигает `Modifier.layout`, поэтому `onGloballyPositioned` — первый
модификатор облачка (правило 1b1-2 § 3.6 «после позиционирующего модификатора» выполняется: позиционирует родитель).
Хвост не меняется: при стороне справа и 1,3 его центр зажат в `bubble.bottom − 31 dp` (zero на 360 × 640: морда 262 dp,
зажим 255 — на 7 dp выше), это правило зонда держит. Первый кадр — питомец и облачко на местах сразу, хвост — со второго
(нужны привязки, как в 1b1-2). `Counter` (:116) рисуется до `Layout` — питомец перед прилавком при любой стороне; при
стороне «перед жителем» облачко (низ `floor − 8`) по-прежнему поверх прилавка. Касаний `Layout` не перехватывает
(модификаторов нет), кнопки рисуются позже.

**г) Код — дословно** (проверено: копия `git archive HEAD finny-pet` с § 1–4 — `$S/clone`, дифф `$S/clone.diff` —
`assembleGameDebug` exit 0, APK собран, предупреждений компилятора в четырёх файлах 0). Строку :107 удалить. Строки
:117-143 (питомец и облачко целиком) заменить на:
```kotlin
        // № 83 в: сторона питомца — по замеру облачка, не по шрифту. Облачко мерится один раз под потолком «перед жителем»
        // (floor − 16 dp); не выше потолка справа (floor − 120 = floor − 16 − 104 dp над питомцем) — питомец справа, под
        // облачком, иначе перед жителем. Питомец и облачко — в одном Layout: сторона и места обоих — в одном проходе.
        Layout(contents = listOf<@Composable () -> Unit>(
            { vm.state.pet?.let { pet -> PetSprite(
                speciesId = pet.speciesId, colorId = pet.colorId, stage = vm.economy.stageIndex(pet.growth), face = Face.HAPPY, animate = LocalAnimate.current,
                modifier = Modifier.clearAndSetSemantics { testTag = "result_pet" }, size = 96.dp, bounceKey = vm.bounce, action = act.action, actionKey = act.key, seen = act,
            ) } },
            { SpeechBubble(
                Modifier.onGloballyPositioned { c -> bubbleRect = c.boundsInParent() }.width(maxWidth - bx).heightIn(max = floor - 16.dp).clearAndSetSemantics {
                    contentDescription = desc; testTag = "job_result"
                    paneTitle = if (pay != null) "${vm.residentName(job?.resident ?: "")}: спасибо! Заработали ${pay.total}" else result.text
                },
                tail = false,
            ) {
                // ← строки :133-142 без изменений, отступ на уровень глубже (сама эта строка в код не идёт)
            } },
        )) { (pets, bubbles), c ->
            val b = bubbles.first().measure(c)
            val side = b.height <= (floor - 120.dp).roundToPx()
            val bottom = if (side) floor - 8.dp - 104.dp else floor - 8.dp
            val p = pets.firstOrNull()?.measure(c)
            layout(c.maxWidth, c.maxHeight) {
                p?.place((if (side) maxWidth - 104.dp else 8.dp).roundToPx(), (floor - 82.dp).roundToPx()) // № 87 б: лапы спрайта — 0,86 кадра, на полу
                b.place(bx.roundToPx(), minOf(head.roundToPx(), bottom.roundToPx() - b.height).coerceAtLeast(8.dp.roundToPx()))
            }
        }
```
Импорт: `import androidx.compose.ui.layout.layout` (:32) → `import androidx.compose.ui.layout.Layout` перед
`boundsInParent` (`layout(…)` внутри `MeasureScope` — член, импорта не требует); wildcard нельзя (WORKFLOW № 45).
Формулы места облачка (`bottom`, `minOf(head, …)`, `coerceAtLeast(8 dp)`, `x = bx`) — прежние из :124-126; `place` (не
`placeRelative`) — как у облачка сейчас. Пустое `pets` (питомца нет) законно: облачко ставится так же.

### 2. Питомец на полу (№ 87 б) — та же правка § 1
`y` питомца — `floor − 82 dp` вместо `floor − 96 dp` (96 − 14) у всех работ и при обеих сторонах. Кадр уходит под ряд
кнопок на 11 dp: кнопки рисуются позже и закрывают прозрачное поле; мишени не меняются. Потолок и место облачка справа не
меняются (зазор облачко → верх кадра питомца 30 dp вместо 16): поднимать облачко на 14 dp — не решение владельца (§ 5).

### 3. Незаработанная звезда — контур ☆ (№ 88 б) — TrayScreen.kt
- `Star` (:138-143) →
  ```kotlin
  @Composable
  private fun Star(size: Dp, outline: Boolean = false) = Canvas(Modifier.size(size)) {
      val p = starPath(center, this.size.minDimension / 2 - 1.dp.toPx())
      if (!outline) drawPath(p, G.gold)
      drawPath(p, G.purpleDeep, style = if (outline) Stroke(2.dp.toPx(), join = StrokeJoin.Round) else Stroke(1.5.dp.toPx()))
  }
  ```
  Импорт `import androidx.compose.ui.graphics.StrokeJoin` (после `PathEffect`, :59). ★ (`Star(24.dp)` :370, :423) — та же.
- `StarRow` :425 → `false -> Star(16.dp, outline = true)`; ○ (:426) и ★ (:422-424) — без изменений. KDoc :414 →
  `/** ★ «+1» — с первого раза, ☆ — обслужен без звезды (контур, № 88 б), ○ — ещё придёт (раунд) / не пришёл (итог); один узел TalkBack [starsText]. */`
- Размер ☆ — 16 dp, как ● и ○: высота ряда и облачка не меняются (24 dp увели бы zero при 1,3 «перед жителем» —
  КОНТЕКСТ). Штрих 2 dp — как у ○ (`border(2.dp)`); скруглённые углы — у 16-dp звезды острый угол со штрихом 2 dp
  (miter 1 / sin 18° ≈ 3,2 dp) вылез бы за кадр 1 dp на соседа. Макет 16 dp / 2 dp / Round читается звездой
  (`$S/star_mock.png`, `$S/syn_zero_crop.png`); контраст ☆ `#310F53` на белом облачке 15,86 : 1, на `#F4F2F8` ряда раунда
  14,28 : 1.
- `StarRow` один на раунд (TrayScreen.kt:218) и итог (RoundScreen.kt:134) — ☆ появится в обоих (один смысл: «обслужен без
  звезды»); TalkBack ряда — прежний один узел `starsText` (:413).

### 4. «!» события (№ 85 б)
а) StreetScreen.kt:91 — только `.size(28.dp)` → `.size(32.dp)`; порядок `border` → `background` прежний (обводка поверх
   заливки: видимый красный круг 32 − 2 × 2 = 28 dp); `padding(4.dp)`, `TopEnd`, «!», семантика карточки — как были.
   Значок заходит на кадр жителя/домика на 8 dp вместо 4 — смотрят судьи.
б) RoomScreen.kt:115 (`TownCard`) — только `G.magenta` → `Color(0xFFE0004A)`; 28 dp, без обводки (плашка белая,
   непрозрачная). `Color` импортирован (:38). «!» в окне (:142, `G.magenta`) не трогать — окно переделывает A1d2. Порядок
   с A1d2: 1b1-3 раньше (A1d2 ждёт круга A1d1-2 и переноса `room.py`, её спека пишется от дерева после 1b1-3); если A1d2
   стартует первой — § 4 б переходит в неё одной строкой, RoomScreen.kt выпадает из allow.
   Литерал `Color(0xFFE0004A)` — во втором файле, токен в `G` не заводится (два места держит греп п. 7; при третьем —
   «!» окна в A1d2 — токен в `GameTheme.kt`, WORKFLOW № 15).

### 5. Явно НЕ определено (кодер спрашивает, а не решает)
Другие числа раскладки итога (104, 120, 16, 8, 64, 82), размер питомца, кадр жителя, хвост, прилавок; сторона по чему-либо,
кроме замера облачка; модификатор у `Layout`; иной способ замера (§ 1 б); анимация смены стороны; ★, «+1», ○, `starPath`;
размер, цвет, штрих ☆ сверх § 3; `padding` и стиль «!», обводка у плашки комнаты, «!» в окне; токен цвета; любые строки,
семантика, комментарии сверх § 1 г; правки вне allow.

## SCOPE
variant: game (только UI)
allow: finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoundScreen.kt (§ 1, 2),
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/TrayScreen.kt (§ 3: `Star`, `StarRow`, импорт),
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/StreetScreen.kt (§ 4 а, одна строка :91),
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt (§ 4 б, одна строка :115)
protect: как `.claude/task-scope.json` 1b4 (базовый + `finny-pet/docs/`, `…/src/main/…` поимённо, `…/classic/`,
  `…/game/res/`, `…/game/mock/`, `finny-pet/tools/…`, корневые `tools/` поимённо — сверить с `ls tools/`: adbui.sh,
  art_check.py, bakery_states.sh, content_map_events.py, emu.sh, line_close.py, mock_bakery.py, mutation_probe.py, perf.sh,
  rec.sh, result_geom.py, sheets.py, town_route.sh, ui_measure.py; `wt/`), но: RoundScreen.kt, StreetScreen.kt,
  RoomScreen.kt — из protect в allow, TrayScreen.kt остаётся в allow; TownUi.kt и GameViewModel.kt (allow 1b4) — в
  protect. Итог: в protect поимённо все файлы `game/java/…` и `game/AndroidManifest.xml`, кроме четырёх из allow;
  + `finny-pet/gradlew.bat`. task-scope — после коммита спеки и зонда (ДО СПАВНА п. 6); пересечение с allow — пусто (№ 23).

## ANTI-SCOPE
№ 84 а (круг ✕), № 86 а (плейтест 1, BACKLOG п. 18), № 89 а и № 97 а (BACKLOG п. 19–20); подъём облачка справа на 14 dp;
«+1» под ★ (№ 83 б не выбран); спрайт прилавка (BACKLOG п. 17); «!» в окне и вся встройка комнаты (A1d2); обводка «!» на
улице — заработает на фоне фасадов (A1g2); `SpeechBubble`, `LineHost`, хвост; 1b5 (полёт изделия); доки и `tools/`
(оркестратор); тесты (оракул не нужен); новые зависимости, ресурсы и инструменты.

## БЮДЖЕТ
≤ 30 вставок без строк import, ожидается 26 (+) и 18 (−) по `git diff -w` копии (`$S/clone`, сверено): RoundScreen +19 −11
(+: 3 строки комментария, `Layout(…)`, 5 изменённых строк питомца и облачка, `} },`, `)) { (pets, bubbles), c ->`,
7 строк замера и размещения, `}`; −: :107, 3 строки питомца, `SpeechBubble(`, 6 строк :122-127), TrayScreen +5 −5,
StreetScreen +1 −1, RoomScreen +1 −1. Импорты: +2 (`Layout`, `StrokeJoin`), −1 (`layout`). Новых файлов 0; зависимостей 0.

## ORACLE
Не нужен: правка только в UI `game/`; `grep -rln 'StarRow\|ShiftResult\|TownCard\|PlaceCard\|ru.finny.pet.game'
finny-pet/app/src/test` → пусто, `src/testGame/` нет; домен, ViewModel и контент не меняются — тестов 600, как на BASE.
Эффект держат: зонд `result_geom.py` (сторона по месту, питомец на полу) с самопроверкой и мета-проверкой; пиксельные
зонды сессии (`b13_badge.py`, `b13_star.py`), сторона по дампу (`b13_side.py`) и сравнение значимых узлов TalkBack (`b13_tb.py`); грепы конструкции (п. 7,
WORKFLOW № 17); отрицательные контроли — на сборке BASE, положительный — на настоящей сборке клона (ДО СПАВНА п. 3–4).

## ДО СПАВНА (оркестратор; один коммит «tools(TOWN-J1-1b1-3): …» вместе со спекой, до записи task-scope)
В начало КАЖДОГО вызова оболочки (здесь и в ACCEPTANCE; состояние оболочки не сохраняется):
`S="$(cygpath -m "C:/Users/SINGUL~1/AppData/Local/Temp/claude/C--Users-Singularity-Documents-Claude-lct-hackaton-2026-case-06/57df72da-e82b-4b3d-bd66-c3e0ebfb1bd2/scratchpad")/1b13"; G="$(cygpath -m "$TEMP")"`
(`$S` — скрипты, логи, профиль, APK; `$G` — сырые дампы итогов `<P>_<кадр>_geom.xml`, их пишут `bakery_states.sh` и
`town_route.sh`; снимки и `DUMP=1`-дампы — `finny-pet/screenshots/emu_<P>_*`, в git не идут).

0. **Дерево, устройство, профиль.** `git status --porcelain -uall -- finny-pet/app/src tools` → пусто; `git diff --stat
   8235e47 HEAD -- finny-pet/app/src` → пусто, `… -- tools` → только `tools/art_check.py` (A1d1, 7571ef7); `git log
   --oneline 8235e47..HEAD -- finny-pet/app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt`
   → пусто (A1d2 не начата). Копия debug BASE: из `finny-pet/` `./gradlew assembleGameDebug` → `cp
   app/build/outputs/apk/game/debug/app-game-debug.apk "$S/b13_base_debug.apk"`. Эмулятор (AVD `finni`, обычный запуск).
   **Установка APK** (здесь, в п. 4 и в ACCEPTANCE п. 9; `adb` не в PATH Git Bash — только `tools/adbui.sh`, HANDOFF
   «Окружение»; флаг `DEBUGGABLE` две debug-сборки не различает — сверяется md5 установленного `base.apk`):
   `A="$S/b13_base_debug.apk"; tools/adbui.sh install -r "$(cygpath -w "$A")" | tee -a "$S/b13_install.log"; tools/adbui.sh shell dumpsys package ru.finny.pet | grep pkgFlags | tee -a "$S/b13_install.log"; [ "$(tools/adbui.sh exec-out 'cat $(pm path ru.finny.pet | cut -d: -f2)' | md5sum | cut -c1-32)" = "$(md5sum "$A" | cut -c1-32)" ] && echo "installed = $A"`
   (чтение md5 с устройства проверено при сведении: совпало с локальной debug-сборкой). Нет строки `installed = …` — дальше
   не идти, кадры после неё не засчитываются. Здесь — debug BASE (`A` как выше; если md5 уже совпал — без установки);
   `tools/adbui.sh wm360`, `mute`. Висящих прогонов нет (№ 40) — ОТДЕЛЬНЫМ вызовом оболочки, в котором нет имён скриптов
   маршрута и их запуска (иначе строка обёртки `bash -c … eval '…'` совпадёт и со скобками):
   `ps -ef | grep -E "[t]own_route|[b]akery_states"` → пусто (exit 1). Профиль —
   `tools/adbui.sh exec-out run-as ru.finny.pet cat files/state.json > "$S/b13_profile.json"`, md5 — в журнал.
   **Копия со щенком** (№ 42: «ребёнку» — кадры с разными видами питомца; на эмуляторе питомец — зайка, `town_route.sh:134`
   и профиль; худший остаток № 83 в — щенок перед Борей-щенком, на S23 при 1,3 он справа и не снимается):
   `python -c "import json,sys; s=json.load(open(sys.argv[1],encoding='utf-8')); s['pet']['speciesId']='puppy'; json.dump(s,open(sys.argv[2],'w',encoding='utf-8'),ensure_ascii=False)" "$S/b13_profile.json" "$S/b13_puppy.json"`
   (`colorId` прежний — `pet_puppy_orange_*` есть, 12 шт.). **Щенок на устройство** — как возврат профиля ниже, но из
   `"$S/b13_puppy.json"` и с `echo "profile = puppy"`.
   **Возврат профиля** (`town_route.sh` стирает прогресс; перед каждым `bakery_states.sh` здесь и в ACCEPTANCE, после
   каждого `town_route.sh`; `bakery_states.sh` сам возвращает свой бэкап — «profile restored», после прогона со щенком это
   щенок, поэтому после `b13pupb` / `b13pup` — явный возврат до следующего `bakery_states.sh` / `town_route.sh`):
   `tools/adbui.sh shell am force-stop --user 0 ru.finny.pet; tools/adbui.sh exec-in run-as ru.finny.pet sh -c 'cat > files/state.json' < "$S/b13_profile.json"; tools/adbui.sh exec-out run-as ru.finny.pet cat files/state.json | cmp - "$S/b13_profile.json" && echo "profile = b13"`.
1. **Зонд `tools/result_geom.py` — правила питомца** (правка оркестратора; черновик сделан и проверен —
   `$S/result_geom_proto.py`, дифф к HEAD `$S/result_geom.diff`):
   - пол `floor = верх кнопок − 3·d` (ряд 61 dp у низа сцены, RoundScreen.kt:109); потолок справа `cap = floor − (низ hud2 +
     8·d) − 120·d` (сцена — `padding(8.dp)` под шапкой `hud2`). По дампам 1b1-2: 360 × 640 — 834 px = 278 dp, S23 — 434 dp;
   - `hud2` — в обязательных узлах (`no nodes: hud2`);
   - центр `result_pet` правее центра `result_resident` (справа): высота `job_result` ≤ cap − 3 px, иначе `pet right but
     bubble at side ceiling` (ловит и облачко, мерянное с потолком стороны); верх питомца ≥ низ облачка, иначе `pet over
     bubble`;
   - иначе (перед жителем): высота `job_result` ≥ cap + 3 px, иначе `pet in front though bubble fits`. Полоса ± 3 px —
     красный при любой стороне (ponytail: ложный красный, если облачко совпало с потолком до 1 dp; на наших кадрах запас
     ≥ 15 px — тогда правится зонд, а не кодер). Правила `pet not right of resident` и `pet not in front of resident`
     снимаются;
   - верх `result_pet` = floor − 82·d ± 2 px (`pet not on floor`), низ = верх кнопок ± 2 px (`pet bottom not at buttons`) —
     вместо `pet cut` (CONTRACT: кадр уходит под кнопки);
   - `--big` — только кадр жителя (≤ 216·d + 2 px, без флага ≤ 232·d + 2 px); вызовы в `bakery_states.sh` и
     `town_route.sh` не меняются; `--line`, хвост, прилавок, пустые узлы `result_*` — без изменений; вывод `GEOM OK` прежний;
   - самопроверка — 41 случай: 6 верных OK (1,0 пекарня и Марта; 1,3 levelup — перед жителем; 1,3 zero — справа; облачко
     50 dp; строка над кнопками) и 35 мутантов, каждый краснеет своим правилом: «1b1-2 при 1,3: перед жителем, хотя
     помещается»; полоса по пикселю с обеих сторон — «1,3: перед жителем, облачко = потолок + 2 px» (836 px) → `pet in
     front though bubble fits` и «1,3: справа, облачко = потолок − 2 px» (832 px) → `pet right but bubble at side
     ceiling`; «всегда справа: облачко у потолка» (834 px); «справа при облачке перед жителем»; «перед жителем при 1,0»;
     «не опущен (1b1-2)» → `pet not on floor` + `pet bottom not at buttons`; «опущен на 28 dp»; «кадр кончается на полу»; допуск ± 2 px — «выше пола на 1 dp» → `pet not
     on floor`, «низ кадра на 1 dp выше кнопок» → `pet bottom not at buttons`;
     «на полу из-под угла облачка, облачко ниже его верха» → `pet over bubble` (облачко режет кадр не
     по всей ширине — bbox непокрытой части = весь кадр; облачко во всю ширину кадра режет его верх — в дампе не бывает); «нет hud2»; прочие 1b1-2 (хвост, кадр 240 / 232, прилавок,
     `--line`) — с новой синтетикой те же ответы. `python tools/result_geom.py --selfcheck` → `SELFCHECK OK`;
   - мета-проверка (№ 44; scratch-копия, по одному): выключить каждое из 10 условий (потолок справа, `pet over bubble`,
     «перед жителем, хотя помещается», ветвление по центру жителя, пол, низ у кнопок, `hud2` в обязательных, кадр 216 при
     `--big`, полоса справа 3 → 0, полоса перед жителем 3 → 0) — каждое роняет самопроверку (черновик: 9 ×
     `SELFCHECK FAIL`, `hud2` — `KeyError`, exit 1); полоса по пикселю — `cap + 3` → `cap + 0 / 1 / 2` и `cap - 3` →
     `cap - 0 / 1 / 2` — 6 × exit 1 (прогон при сведении). Расширение полосы (3 → 4) самопроверка не ловит — ложный красный
     зонда, а не пропуск брака. Итог — в журнал; критик по коду зонда — до коммита (№ 44);
   - красный на реальных дампах BASE (№ 14, 27): дампы 1b1-2 — `GEOM FAIL: pet not on floor; pet bottom not at buttons` на
     всех кадрах без `--line`, плюс `pet in front though bubble fits` на `s13eb_zero13`, `s13er_tresult13` и на всех 1,3
     S23 (`s13sb_levelup13`, `s13sb_zero13`, `s13sr_result13`, `s13sr_result213`, `s13sr_tresult13`); `s13eb_levelup13`, `s13er_result13`,
     `s13er_result213` — без него (перед жителем законно); `--line` (`levelwhy`, `why`) — `GEOM OK`. Повторить на свежих
     дампах BASE (п. 3). Docstring зонда — правила выше (в черновике).
2. **Скрипты сессии** (лежат в `$S`, проверены; в репозиторий не входят; тексты — ровно эти блоки):
   ```python
   # b13_badge.py PX SHOT… — № 85 б: красный круг «!» — #E0004A, видимая заливка 28 dp (bbox плоских пикселей), прежнего #FF0053 нет
   # ponytail: один значок на снимок и нет «!» окна комнаты (событие маршрута — у «Дома»); иначе — связные круги (scipy.ndimage.label)
   import sys, numpy as np
   from PIL import Image
   px = float(sys.argv[1]); bad = 0
   for f in sys.argv[2:]:
       a = np.asarray(Image.open(f).convert("RGB")).astype(int)
       m = np.abs(a - (224, 0, 74)).max(-1) <= 3; old = int((np.abs(a - (255, 0, 83)).max(-1) <= 3).sum())
       ys, xs = np.nonzero(m); w, h = (int(np.ptp(xs)) + 1, int(np.ptp(ys)) + 1) if len(xs) else (0, 0)
       ok = abs(w - 28 * px) <= 3 and abs(h - 28 * px) <= 3 and old == 0; bad += not ok
       print(f, f"{'OK' if ok else 'FAIL'} E0004A {int(m.sum())} px bbox {w}x{h} (28 dp = {28 * px:.0f}), FF0053 {old} px")
   print(f"{len(sys.argv) - 2} shots, {bad} FAIL"); sys.exit(1 if bad or len(sys.argv) < 3 else 0)
   ```
   Проверка: BASE `emu_s13er_street.png` — FAIL (рамка 72 × 72), `emu_s13er_room.png` — FAIL (`#E0004A` 0, `#FF0053` 5086 px);
   те же снимки с нарисованным кругом 28 dp — 2 × OK.
   ```python
   # b13_star.py SHOT DUMP [SHOT DUMP] — № 88 б: в облачке итога 0★ ● ушли, на их месте 4 контура с дыркой внутри: плоских #310F53 (±3)
   # в bounds job_result — от 4 × 250 до 4 × 1000 (● 16 dp ≈ 1750 каждый; другого этого цвета в облачке zero нет) и дырок маски ≥ 4 × 150
   # (☆ ≈ 316 каждая на синтетике; у ● и сплошной тёмной звезды — 0…16). ☆ от ○ и от золотой Star(16.dp) не отличает (у них тоже дырка) — греп п. 7, судьи
   import re, sys, numpy as np
   from PIL import Image
   from scipy import ndimage
   bad = 0
   for shot, dump in zip(sys.argv[1::2], sys.argv[2::2]):
       b = list(map(int, re.findall(r"\d+", re.search(r'resource-id="job_result"[^>]*?bounds="([^"]*)"', open(dump, encoding="utf-8").read())[1])))
       a = np.asarray(Image.open(shot).convert("RGB")).astype(int)[b[1]:b[3], b[0]:b[2]]
       m = np.abs(a - (0x31, 0x0F, 0x53)).max(-1) <= 3; n = int(m.sum()); h = int((ndimage.binary_fill_holes(m) & ~m).sum())
       ok = 4 * 250 <= n <= 4 * 1000 and h >= 4 * 150; bad += not ok
       print(shot, f"{'OK' if ok else 'FAIL'} purpleDeep {n} px (4 x 250..1000), holes {h} px (>= 4 x 150)")
   print(f"{len(sys.argv[1:]) // 2} frames, {bad} FAIL"); sys.exit(1 if bad or len(sys.argv) < 3 else 0)
   ```
   Проверка (при сведении): BASE `emu_s13eb_zero10/13` — 2 × FAIL (6992 px, holes 0); синтетика `$S/syn_s13eb_zero1{0,3}.png`
   (● закрашены, ☆ 16 dp / 2 dp / Round нарисованы PIL, `$S/_synstar.py`) — 2 × OK (2292 px, holes 1264); мутант «4 сплошные
   тёмные звезды 16 dp» `$S/synsolid_s13eb_zero1{0,3}.png` (`$S/_synsolid.py`) — 2 × FAIL по holes (3540 px — в коридоре,
   holes 16). Мерит «● ушли, на их месте 4 контура с дыркой», и только это: ☆ от ○ и от золотой `Star(16.dp)` он не
   отличает (дырка есть у всех трёх; на кадрах zero ○ нет) — эти ветки держат греп п. 7 (`false -> Star(16.dp, outline =
   true)` → 1, ○ → 1) и судьи (п. 11). Пороги сверить на клоне (п. 4), px и holes — в журнал. Вне коридора на верной
   сборке — правится порог, а не код; holes < 600 на верной сборке (маска ± 3 не сомкнулась на сглаженных углах) — дырки
   считать по более широкой маске тёмных пикселей (`<= 60`), `n` — по прежней; проверку дырок не снимать.
   ```python
   # b13_side.py DIR FRAME… — № 83 в: сторона питомца на итоге по сырому дампу DIR/FRAME_geom.xml: right (центр result_pet правее
   # центра result_resident) или front, и высота job_result в dp (d = 3, эмулятор wm360); нет узла или дампа — exit 1 с именем кадра
   import re, sys
   bad = 0
   for f in sys.argv[2:]:
       try:
           x = open(f"{sys.argv[1]}/{f}_geom.xml", encoding="utf-8").read()
           g = lambda k: list(map(int, re.findall(r"\d+", re.search(r'resource-id="' + k + r'"[^>]*?bounds="([^"]*)"', x)[1])))
           p, r, b = g("result_pet"), g("result_resident"), g("job_result")
           print(f, "right" if p[0] + p[2] > r[0] + r[2] else "front", round((b[3] - b[1]) / 3, 1))
       except (OSError, TypeError) as e:
           bad += 1; print(f, "FAIL", type(e).__name__)
   sys.exit(1 if bad or len(sys.argv) < 3 else 0)
   ```
   Проверка (при сведении): `python "$S/b13_side.py" "$G" s13eb_zero10 s13eb_zero13 s13er_tresult13` → `right 216.0`,
   `front 273.0`, `front 249.0`, exit 0; `s13eb_{levelup,zero}1{0,3} s13er_{result,result2,tresult}1{0,3}` — 10 строк,
   высоты — таблица КОНТЕКСТА; кадр без дампа — `FAIL FileNotFoundError`, без `result_pet` — `FAIL TypeError`, exit 1.
   Вызывается файлом (не копией текста из спеки — отступ пункта ломает `python -c`); кадры — фигурными скобками bash.
   ```python
   # b13_tb.py DIR BASE NEW — значимые узлы TalkBack (text, content-desc, clickable, focusable) на одноимённых кадрах — те же, что на BASE;
   # bounds не сравниваются: облачко при 1,3 законно меняет место (№ 83 в)
   import re, sys, glob, os
   from collections import Counter
   d, a, b = sys.argv[1], sys.argv[2], sys.argv[3]  # каталог дампов, префикс BASE, префикс 1b1-3
   R = re.compile(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?clickable="(\w+)"[^>]*?focusable="(\w+)"')
   N = lambda p: Counter(n for n in R.findall(open(p, encoding="utf-8").read()) if n[0] or n[1] or "true" in n[2:4]) if os.path.exists(p) else Counter()
   fr = sorted(os.path.basename(p)[len(f"emu_{a}_"):-4] for p in glob.glob(f"{d}/emu_{a}_*.xml"))
   bad = 0
   for f in fr:
       x, y = N(f"{d}/emu_{a}_{f}.xml"), N(f"{d}/emu_{b}_{f}.xml")
       ok = x == y and sum(x.values()) > 0; bad += not ok
       print(f, f"OK sig {sum(x.values())}" if ok else f"FAIL sig {sum(x.values())}/{sum(y.values())} -{list((x - y).elements())[:2]} +{list((y - x).elements())[:2]}")
   print(f"{len(fr)} frames, {bad} FAIL")
   sys.exit(1 if bad or not fr else 0)
   ```
   Проверка: `emu_s13eb_*` против себя — 6 frames, 0 FAIL; подмена «Боря: спасибо!» → «…спасибо! спасибо!» — 6 FAIL, exit 1.
3. **«До» на эмуляторе — отрицательные контроли на сборке BASE** (№ 14, 27, 28; новый `result_geom.py` из п. 1; профиль
   = `$S/b13_profile.json`):
   - `DUMP=1 tools/bakery_states.sh b13b levelup levelwhy zero 2>&1 | tee "$S/b13b_states.log"` → `grep -cE "not
     found|BACKUP"` → 0; `GEOM FAIL` 6 (levelup10/13 дважды, zero10/13; zero13 — ещё `pet in front though bubble fits`),
     `GEOM OK` 2 (`--line`), `CLOSE OK` 2, `NODES OK` 2; `profile restored`;
   - `DUMP=1 tools/town_route.sh b13rb 2>&1 | tee "$S/b13rb_route.log"`, затем возврат профиля (п. 0) → `grep -cE "not
     found|gate not passed"` → 0; `GEOM FAIL` 6 (tresult13 — ещё `pet in front though bubble fits`), `GEOM OK` 2 (`why`);
     «Обрезано: 0»;
   - `python "$S/b13_badge.py" 3 finny-pet/screenshots/emu_b13rb_street.png finny-pet/screenshots/emu_b13rb_room.png` → 2 FAIL;
     `python "$S/b13_star.py" finny-pet/screenshots/emu_b13b_zero10.png "$G/b13b_zero10_geom.xml" finny-pet/screenshots/emu_b13b_zero13.png "$G/b13b_zero13_geom.xml"`
     → 2 FAIL (holes 0); стороны — `python "$S/b13_side.py" "$G" b13b_{levelup,zero}1{0,3} b13rb_{result,result2,tresult}1{0,3}`
     → все 1,3 `front`, все 1,0 `right`, exit 0;
   - щенок (№ 42): «Щенок на устройство» (п. 0) → `DUMP=1 tools/bakery_states.sh b13pupb levelup zero 2>&1 | tee
     "$S/b13pupb_states.log"` → `grep -cE "not found|BACKUP"` → 0; `GEOM FAIL` 4 (zero13 — ещё `pet in front though bubble
     fits`); `profile restored` (это щенок) → возврат профиля (п. 0, `profile = b13`); `python "$S/b13_side.py" "$G"
     b13pupb_{levelup,zero}1{0,3}` → 1,3 — `front`, 1,0 — `right`.
   Числа — в журнал. Снимки `emu_b13b_*`, `emu_b13rb_*`, `emu_b13pupb_*` — ряд «до» листа (№ 28).
4. **Положительный контроль на настоящей сборке клона** (№ 14, 44; без него п. 9 не засчитывается): клон — `$S/clone`
   (`git archive` HEAD с § 1–4 дословно); сверка — из корня `for f in RoundScreen TrayScreen StreetScreen RoomScreen; do git
   diff --no-index -w finny-pet/app/src/game/java/ru/finny/pet/game/screens/$f.kt "$S/clone/finny-pet/app/src/game/java/ru/finny/pet/game/screens/$f.kt"; done`
   → +26 −18 без import, литералов новых 0, семантика равна (команды п. 6, 8 по этому диффу — сверено при сведении).
   Из `$S/clone/finny-pet` `./gradlew assembleGameDebug` → «Установка APK» (п. 0) с
   `A="$S/clone/finny-pet/app/build/outputs/apk/game/debug/app-game-debug.apk"` → `installed = …clone…`; возврат профиля
   (п. 0); из основного дерева `DUMP=1 tools/bakery_states.sh b13p levelup levelwhy zero`
   и `DUMP=1 tools/town_route.sh b13rp` (логи в `$S`), затем возврат профиля: счёт — как ACCEPTANCE п. 9 а, б (все OK, 0 FAIL,
   промахов 0, «Обрезано: 0»); стороны — `python "$S/b13_side.py" "$G" b13p_{levelup,zero}1{0,3} b13rp_{result,result2,tresult}1{0,3}`
   → как п. 9 в; `b13_badge.py` на `emu_b13rp_{street,room}.png` → 2 OK; `b13_star.py` на `emu_b13p_zero10/13` → 2 OK (px и
   holes — в журнал); `python "$S/b13_tb.py" finny-pet/screenshots b13b b13p` → 6 frames, 0 FAIL.
   Затем «Установка APK» с `A="$S/b13_base_debug.apk"` → `installed = …b13_base_debug.apk`, возврат профиля. Красный на
   верной реализации — чинится зонд или спека (оркестратор), а не кодер.
5. Коммит спеки и `tools/result_geom.py` — «tools(TOWN-J1-1b1-3): …»; `python tools/result_geom.py --selfcheck` на коммите →
   `SELFCHECK OK`.
6. task-scope (base = коммит п. 5; allow/protect — SCOPE); пересечение — пусто. Сразу перед спавном — снимок всего дерева
   (в нём работают и сессии A1*): из корня `git status --porcelain -uall > "$S/b13_st0.txt"`, число строк — в журнал.

## ACCEPTANCE (команды 1–8 и 10 — из finny-pet/, 9 и 11 — из корня; гоняет оркестратор; `S`, `G` — шапка ДО СПАВНА)
1. ./gradlew testClassicDebugUnitTest --rerun --console=plain -> exit 0; tests = 600, failed 0, skipped 0 (по XML, пресет)
2. ./gradlew testGameDebugUnitTest --rerun --console=plain -> exit 0; 600 / 0 / 0
3. ./gradlew assembleClassicDebug assembleGameDebug assembleGameRelease -> exit 0
4. ./gradlew lintClassicDebug lintGameDebug -> ошибок 0; без сетевых (ACCEPTANCE_PRESETS.md:51) game ≤ 5, classic ≤ 5 (как после 1b4)
5. git diff --name-only BASE -- app/src/main/ app/src/classic/ app/src/test/ app/src/game/res/ '*.gradle.kts' gradle/ gradle.properties gradlew gradlew.bat -> пусто
6. git status --porcelain -uall -- app/src -> только пути allow; из корня `node .claude/hooks/assert-oracle-intact.js` -> exit 0;
   всё дерево (shell-запись вне allow и protect хук пропускает — WORKFLOW.md:29-32; гонять до п. 9 и 11, они пишут
   `town/j1b13_gate.jpg`): из корня `git status --porcelain -uall | grep -vxFf "$S/b13_st0.txt"` -> ровно 4 строки ` M` —
   пути allow; лишнее — разобрать (запись кодера — FAIL; чужая сессия — в журнал);
   `git diff -w BASE -- app/src | grep '^+[^+]' | grep -vc '^+import '` -> ≤ 30 (ожидается 26);
   `git diff -w BASE -- app/src | grep '^-[^-]' | grep -vc '^-import '` -> ожидается 18 (сверх 20 — разобрать дифф);
   `git diff -w BASE -- app/src | grep -cE '^\+import '` -> 2, `… grep -cE '^-import '` -> 1;
   `git diff --numstat BASE -- app/src/game/java/ru/finny/pet/game/screens/StreetScreen.kt app/src/game/java/ru/finny/pet/game/screens/RoomScreen.kt` -> `1 1` у каждого
7. греп по коду без строк import (№ 45); `D=app/src/game/java/ru/finny/pet/game/screens; RS=$D/RoundScreen.kt TS=$D/TrayScreen.kt SS=$D/StreetScreen.kt RM=$D/RoomScreen.kt`;
   `c(){ grep -v '^import' "$1" | grep -cF -- "$2"; }` (в скобках — BASE; числа сверены на копии и на BASE):
   - `c $RS 'bigFont()'` -> 1 (2); `c $RS 'Layout(contents = listOf<@Composable () -> Unit>('` -> 1 (0);
     `c $RS 'val side = b.height <= (floor - 120.dp).roundToPx()'` -> 1 (0); `c $RS 'heightIn(max = floor - 16.dp).clearAndSetSemantics {'` -> 1 (0);
     `c $RS '(floor - 82.dp).roundToPx()'` -> 1 (0); `c $RS 'floor - 96.dp'` -> 0 (1); `c $RS 'Modifier.layout'` -> 0 (1);
     `grep -v '^import' $RS | grep -o '\.onGloballyPositioned' | wc -l` -> 2 (2); `grep -v '^import' $RS | grep -o 'clearAndSetSemantics' | wc -l` -> 3 (3);
   - регресс 1b1-2: `c $RS 'TText("всего"'` -> 1; `c $RS 'if (bigFont()) 216.dp else 232.dp'` -> 1; `c $RS 'tail = false'` -> 1;
     `c $RS 'vm.lines.clear(); vm.closeRound()'` -> 1;
     `grep -v '^import' $RS | grep -oE 'testTag\("result_(pet|resident|tail|counter)"\)|testTag = "result_(pet|resident)"' | wc -l` -> 4;
   - порядок содержимого и размещения (= отрисовки: при равном zIndex — порядок `place`, LayoutNode ZComparator,
     compose-ui 1.11.4): `awk '/fun ShiftResult/{f=1} f&&/ResidentPic\(/{r=NR} f&&/Counter\(/{c=NR} f&&/Layout\(contents/{l=NR} f&&/PetSprite\(/{p=NR} f&&/SpeechBubble\(/{b=NR} f&&/p\?\.place\(/{pp=NR} f&&/b\.place\(/{bp=NR} END{print (r<c && c<l && l<p && p<b && pp && pp<bp) ? "ORDER OK" : "ORDER FAIL"}' $RS`
     -> ORDER OK (BASE — FAIL; клон с переставленными `p?.place` / `b.place` — FAIL, проверено при сведении);
     `c $RS 'p?.place('` -> 1 (0); `c $RS 'b.place(bx.roundToPx(), '` -> 1 (0) (без них `?.let { it.place(…) }` обходит `pp`);
   - `c $TS 'private fun Star(size: Dp, outline: Boolean = false) = Canvas(Modifier.size(size)) {'` -> 1 (0);
     `c $TS 'if (!outline) drawPath(p, G.gold)'` -> 1 (0); `c $TS 'Stroke(2.dp.toPx(), join = StrokeJoin.Round)'` -> 1 (0);
     `c $TS 'false -> Star(16.dp, outline = true)'` -> 1 (0); `c $TS 'false -> Box(Modifier.size(16.dp).background(G.purpleDeep, CircleShape))'` -> 0 (1);
     `c $TS 'null -> Box(Modifier.size(16.dp).border(2.dp, G.purpleDeep, CircleShape))'` -> 1 (1); `c $TS 'Star(24.dp)'` -> 2 (2);
   - `c $SS '.padding(4.dp).size(32.dp).border(2.dp, Color.White, CircleShape).background(Color(0xFFE0004A), CircleShape)'` -> 1 (0); `c $SS 'size(28.dp)'` -> 0 (1);
   - `c $RM 'if (card.eventId != null) Box(Modifier.size(28.dp).background(Color(0xFFE0004A), CircleShape)'` -> 1 (0); `c $RM 'G.magenta'` -> 1 (2; окно :142);
   - импорты: `grep -cx 'import androidx.compose.ui.layout.Layout' $RS` -> 1; `grep -cx 'import androidx.compose.ui.layout.layout' $RS` -> 0;
     `grep -cx 'import androidx.compose.ui.graphics.StrokeJoin' $TS` -> 1; `grep -c '^import .*\.\*$' $RS $TS $SS $RM` -> 0 у каждого;
   - из корня: `python tools/result_geom.py --selfcheck`, `python tools/line_close.py --selfcheck` -> `SELFCHECK OK` × 2
8. новые литералы как разность множеств:
   `comm -23 <(git diff -w BASE -- app/src | grep '^+[^+]' | grep -oE '"[^"]*"' | sort -u) <(git diff -w BASE -- app/src | grep '^-[^-]' | grep -oE '"[^"]*"' | sort -u)` -> пусто;
   семантика не тронута: `diff <(git diff -w BASE -- app/src | grep '^+[^+]' | grep -oE '(clearAndSetSemantics|semantics|contentDescription|stateDescription|paneTitle|testTag)[^,)]*' | sort) <(git diff -w BASE -- app/src | grep '^-[^-]' | grep -oE '(clearAndSetSemantics|semantics|contentDescription|stateDescription|paneTitle|testTag)[^,)]*' | sort)` -> пусто
   (обе — сверены на диффе копии)
9. живая проверка — эмулятор (AVD `finni`, обычный запуск); debug п. 3 — «Установка APK» (ДО СПАВНА п. 0; `pkgFlags` в лог —
   № 33) с `A=finny-pet/app/build/outputs/apk/game/debug/app-game-debug.apk` → `installed = …`, без неё кадры не
   засчитываются; `tools/adbui.sh wm360`, `mute`; отдельным вызовом без имён скриптов `ps -ef | grep -E "[t]own_route|[b]akery_states"`
   → пусто, exit 1 (№ 40); профиль возвращён и = `$S/b13_profile.json`
   (ДО СПАВНА п. 0; иначе п. 9 е не засчитывается — пару `b13b`/`b13` снять заново):
   а) `DUMP=1 tools/bakery_states.sh b13 levelup levelwhy zero 2>&1 | tee "$S/b13_states.log"`:
      `grep -cE "not found|BACKUP|GEOM FAIL|CLOSE FAIL|NODES FAIL" "$S/b13_states.log"` -> 0; `grep -c "GEOM OK"` -> 8;
      `grep -c "CLOSE OK"` -> 2; `grep -c "NODES OK"` -> 2; `grep -cE "Обрезано: [1-9]|^[A-Za-z0-9_]+: *$"` -> 0; `profile restored`;
      «Готово» и ✕ при открытой строке (`levelwhy`) — как 1b1-2 (промахов 0);
   б) `DUMP=1 tools/town_route.sh b13r 2>&1 | tee "$S/b13r_route.log"` (после а; затем возврат профиля):
      `grep -cE "not found|gate not passed|GEOM FAIL|CLOSE FAIL|NODES FAIL" "$S/b13r_route.log"` -> 0 (№ 32); `GEOM OK` -> 8;
      `CLOSE OK` -> 2; «Обрезано» — как в а);
   б2) щенок (№ 42; после б и возврата профиля): «Щенок на устройство» (ДО СПАВНА п. 0) → `DUMP=1 tools/bakery_states.sh
      b13pup levelup zero 2>&1 | tee "$S/b13pup_states.log"` (свой лог — счёты а) не смешивать): `grep -cE "not
      found|BACKUP|GEOM FAIL"` -> 0; `grep -c "GEOM OK"` -> 4; `profile restored` (это щенок) → возврат профиля (п. 0,
      `profile = b13`) до любого следующего прогона; `python "$S/b13_side.py" "$G" b13pup_{levelup,zero}1{0,3}` ->
      `front` только у `b13pup_levelup13` (щенок перед Борей-щенком — остаток № 83 в), остальные — `right`;
   в) стороны по месту (№ 83 в) — по сырым дампам, файлом `$S/b13_side.py` (ДО СПАВНА п. 2):
      `python "$S/b13_side.py" "$G" b13_{levelup,zero}1{0,3} b13r_{result,result2,tresult}1{0,3}` -> exit 0;
      `front` ровно у `b13_levelup13`, `b13r_result13`, `b13r_result213` (336 и 308 dp > 278), остальные 7 — `right`;
      высоты — таблица КОНТЕКСТА ± 1 dp (zero13 — 273: ☆ высоту ряда не меняет). На BASE (те же дампы 1b1-2) — `front` у
      всех 1,3 (проверено при сведении);
   г) `python "$S/b13_badge.py" 3 finny-pet/screenshots/emu_b13r_street.png finny-pet/screenshots/emu_b13r_room.png` -> `2 shots, 0 FAIL`
      (BASE — 2 FAIL, ДО СПАВНА п. 3);
   д) `python "$S/b13_star.py" finny-pet/screenshots/emu_b13_zero10.png "$G/b13_zero10_geom.xml" finny-pet/screenshots/emu_b13_zero13.png "$G/b13_zero13_geom.xml"`
      -> `2 frames, 0 FAIL` (BASE — 2 FAIL); px и holes — в журнал;
   е) TalkBack тот же: `python "$S/b13_tb.py" finny-pet/screenshots b13b b13` -> `6 frames, 0 FAIL`; FAIL — повтор ПАРЫ подряд
      (debug BASE → `b13b`, debug 1b1-3 → `b13`; каждая сборка — «Установка APK» ДО СПАВНА п. 0: `A="$S/b13_base_debug.apk"`,
      затем `A` — как в шапке п. 9, строка `installed = …` у обеих; профиль возвращён перед каждым); повторился — FAIL с
      напечатанными узлами;
   ж) `python tools/ui_measure.py finny-pet/screenshots/emu_b13_<кадр>.xml <кадр> 3` по 6 дампам а) — кликабельных < 48 dp нет,
      за краем нет;
   з) глазами: `zero10/13` (☆☆☆☆; при 1,3 питомец справа под облачком), `levelup13` (перед жителем, лапы у низа прилавка),
      `levelup10` / `b13r_result10` (справа, лапы на линии низа прилавка, не на стенке; ★★★☆), `b13r_thanks` (ряд раунда
      ★ ☆ ○ ○ — ☆ рядом с ○), `b13r_tresult13` (Марта, питомец справа), `emu_b13r_street` («!» 28 dp с белым кольцом),
      `emu_b13r_room` (красный «!» на плашке);
   и) **S23 — «доснять при владельце»** (GATE_QUEUE раздел 6, вместе с 1b4): а), б), г) с `ANDROID_SERIAL`, плотность из
      `wm density`; разблокирует владелец; громкость и шрифт до и после; профиль — бэкап и `cmp` (`town_route.sh` стирает
      прогресс — HANDOFF «Окружение»). Ожидание S23: потолок 434 dp — `right` у всех итогов, прочие счёты — как а), б).
      Отступление — в журнал.
10. стоп-слова в литералах диффа (команда TOWN-J1-1a2.md:116-117) -> 0
11. лист `finny-pet/screenshots/town/j1b13_gate.jpg` (`python tools/sheets.py town/j1b13_gate.jpg "…" …`; заголовок — с
    пометкой «S23 — доснять»): пары «до / после» — `emu_b13b_*` / `emu_b13_*` на `zero13` (сторона, ☆), `levelup13` (перед
    жителем, лапы), `zero10` (☆); `emu_b13rb_*` / `emu_b13r_*` на `tresult13` (Марта), `result10` (★★★☆, лапы у прилавка),
    `thanks` (ряд раунда ★ ☆ ○ ○), `street`, `room`; `emu_b13pupb_*` / `emu_b13pup_*` на `levelup13` (щенок перед
    Борей-щенком — худший остаток № 83 в) и `zero13` (справа). В подпись листа и в строку GATE_QUEUE — остаток № 83 в
    одной строкой: «на 360 × 640 при 1,3 питомец перед жителем почти в каждой смене пекарни (нужна хоть одна ★ или новый
    уровень); на S23 — справа всегда»; пересмотр № 83 — вопрос № 98, только если владелец захочет. Контраст — `python -c "L=lambda h:(lambda c:.2126*c[0]+.7152*c[1]+.0722*c[2])([x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in [int(h[i:i+2],16)/255 for i in (0,2,4)]]);r=lambda a,b:round((max(L(a),L(b))+.05)/(min(L(a),L(b))+.05),2);print(r('FFFFFF','E0004A'),r('310F53','FFFFFF'),r('310F53','F4F2F8'),r('FFFFFF','FF0053'))"`
    -> `4.92 15.86 14.28 3.91` (белый «!» на новом круге; ☆ на облачке и в ряду раунда; было в комнате). Судьи одним
    workflow по кадрам «после» (WORKFLOW № 35, 42):
    - **«ребёнок» вслепую** (вырезки без текста облачка, нейтральные метки, «до» и «после» вперемешку, ключ у оркестратора):
      «Чей это зверёк рядом с продавцом — продавца или твой?» по `zero13`, `tresult13`, `result10`, `b13pup_zero13`
      (справа) и `levelup13`, `result213`, `b13pup_levelup13` (перед жителем; зайка и щенок — № 42) — «детёныш» на «после»
      справа — 0; на кадрах перед жителем — в журнал отдельно для зайки и для щенка (остаток № 83 в — частый случай на
      360 × 640, а не крайний);
      «Что значат значки в ряду под „Спасибо!“?» по `zero10`, `result10`, `thanks` — ☆ «не получил звезду / пустая звезда»,
      не «шарик», ☆ и ○ не спутаны; «Где стоят лапки зверька?» по `levelup10`, `result10` — «на полу / перед прилавком».
      Самопроверка линзы: на «до» ● не названа пустой звездой (иначе линза подсказана — заново);
    - **«доступность»**: серый (luma) и дейтеранопия по `zero10`, `result10`, `thanks`, `street`, `room`: ★ / ☆ / ○
      различимы формой (☆ и ○ одного цвета — контур звезды против окружности 16 dp), «!» — белый знак на круге 28 dp,
      4,92 : 1; значок комнаты на белой плашке различим в сером;
    - **«сцена и владелец»**: при 1,3 справа питомец не касается облачка (зазор 30 dp) и края, хвост — к морде жителя; лапы
      на полу у прилавка; у Марты (без прилавка) лапы ≈ 10 dp ниже её ног — «ближе к зрителю» или «провалился»?; «!» 32 dp
      на карточке «Дом» заходит на домик на 8 dp; кнопки итога не перекрыты.
    Ворота — «посмотреть» (владелец: «дальше посмотрю по скринам»). Вопросы — только по находкам судей, пачкой в GATE_QUEUE
    с № 98 (№ 97 — последний занятый, GAME_CONCEPT §18); без находок — строка «1b1-3: лист `j1b13_gate.jpg`, судьи без
    находок, рекомендация — принять». Кодер вид сам не меняет (§ 5).

## ЖИВАЯ ПРОВЕРКА (человек: docs/BACKLOG.md п. 1 и 11)
S23 при владельце — п. 9 и. TalkBack: итог — облачко одним узлом («…Обслужено 4 из 4, звёзд 0…» — ☆ отдельно не звучит),
путь облачко → «Почему?» → «Готово»; ряд раунда — тот же текст; карточка улицы — «…, есть событие»; плашка комнаты — одна
фраза «В городке: …», «!» отдельно не звучит. Появление итога: питомец и облачко появляются вместе, без перескока стороны
(решение в одном проходе, § 1 б) — глазами, запись `tools/rec.sh` — по желанию (№ 31).

## ВОПРОСЫ НА ВОРОТА j1b13 (только по находкам судей п. 11; номера — с № 98)
Кандидаты, если найдут: (1) Марта — питомец «провалился» ниже её ног: а) оставить (перспектива); б) у работ без прилавка
`floor − 92 dp` (лапы на уровне ног жителя) — одна строка; (2) ☆ путают с ○: а) оставить (форма); б) ☆ 20 dp (zero при 1,3
станет 277 dp — в 1 dp от потолка, полоса зонда краснеет — нужен замер); (3) «!» закрывает домик на карточке «Дом»:
а) оставить до фасадов A1g2; б) `padding(2.dp)`. Без находки не задаются.

## ДОКИ (оркестратор: после приёмки и живой, до ревью — WORKFLOW № 46; черновик — в scratch)
Грепы по `docs/`, `finny-pet/docs/` и `tools/`: `★ ● ○|● \(круг\)|●●●●|★★★●|● — обслужен|в ряду ●|● без звезды` (только ряд
`StarRow`; жетоны смен «Смены ●○○», «●●●» — GAME_CONCEPT.md:362-366, BUILD_AND_DEMO.md:212, TEST_CASES.md:115 «Смены ●○○» — не трогать), `перед жител|перед
Борей|при шрифте > 1,15|при крупном шрифте|справа места нет|вопрос ворот j1b12`, `#FF0053|G\.magenta|фукси` рядом с «!»,
`28 dp` рядом с «!», `floor − 96|pet cut`, `1b1-3`, `ps -ef \| grep`, `adb install`. Править то, что стало ложью (строки —
рабочее дерево на b98298b; номера сдвигаются — перечень ниже сверять грепом, а не номером):
- GAME_CONCEPT: §3.1 :138 (`★ ● ○` → `★ ☆ ○`); §5.5 :535-537 («при крупном шрифте — перед жителем (справа места нет;
  решение спеки до ответа ворот j1b12)» → сторона по месту: справа под облачком, если облачко помещается, иначе перед
  жителем, когда облачко выше места справа (`floor − 120 dp`): на 360 × 640 при 1,3 это любой итог пекарни со ★ (1–4★:
  308 dp, с новым уровнем — 336 dp); справа — 0★ без нового уровня (273 dp) и Марта (249 dp); на S23 — справа всегда
  (№ 83 в); лапы питомца на линии пола (№ 87 б)); §14 :1556
  (`★ ● ○` → `★ ☆ ○`); §18 № 83, 85, 87, 88 (:2116, :2118, :2120, :2121) — «реализовано TOWN-J1-1b1-3»; № 63 (:2095) —
  история решения, не трогать.
- finny-pet/docs/UX_ACCESSIBILITY.md: :128 («● (круг)» → «☆ (контур звезды 16 dp того же цвета)»); :151 («круг 28 dp с
  белой обводкой 2 dp» → «значок 32 dp: круг `#E0004A` 28 dp и белая обводка 2 dp поверх края (№ 75 а, № 85 б); тот же
  `#E0004A` 28 dp — «!» на плашке «В городке» в комнате»); :159 (из строки `#FF0053` убрать «!» на карточке «В городке»);
  :250-253 (сторона по замеру облачка, не по шрифту; ряд `★ ☆ ○`; лапы на полу).
- REQUIREMENTS_MATRIX :41 (2.1: сторона при шрифте > 1,15, `★ ● ○`), :168 (3.6: `★ ● ○`); BUILD_AND_DEMO :201 и
  TEST_CASES TC-32 :115 (ряд `★ ☆ ○` дважды, сторона питомца), TC-28 :111 («(при шрифте > 1,15 — перед Мартой)» →
  «справа под облачком и при 1,3 (облачко Марты 249 dp помещается, № 83 в)»; статусную ячейку прогона не трогать);
  ARCHITECTURE :92 — по грепу (● не называет — без правки).
- docs/tasks/TOWN-J1.md (раздел «2. Экраны» — живой документ эпика): :146 («● — обслужен» → «☆ — обслужен без звезды»),
  :162 («в ряду ● без звезды» → «в ряду ☆ (без звезды)»), :182 (сторона при крупном шрифте — по месту, № 83 в), :184
  (`★★★●` → `★★★☆`), :208 («★ ● ○ ✓» → «★ ☆ ○ ✓»), :359 (статус 1b1-3); :403 («★● в итоге как в раунде» — журнал проверки,
  история) не трогать. GATE_QUEUE раздел 2 — № 83, 85, 87, 88 «реализовано 1b1-3» (у № 83 — остаток одной строкой, как
  в подписи листа п. 11), S23 — в раздел 6; docs/HANDOFF.md раздел «Дальше» (:33) и :82 (№ 40 — команда со скобками,
  ниже). Журнал TOWN-J1-1b1-2 уже называет 1b1-3 (:385) — не трогать; спеки прошлых задач не трогать (TOWN-J1-1b.md:103,
  TOWN-J1-1b4.md:232-233, :359-360 — `ps -ef | grep` без скобок — остаются как история).
- docs/WORKFLOW.md: № 40 (:257) — `ps -ef | grep -E "[t]own_route|[b]akery_states"` → пусто, отдельным вызовом оболочки
  без имён скриптов маршрута (строка обёртки `bash -c … eval '…'` в Git Bash совпадает с шаблоном без скобок всегда, а со
  скобками — если в том же вызове есть имя скрипта); № 33 (:215-218) — строка: «флаг `DEBUGGABLE` не различает две
  debug-сборки — сверять md5 установленного `base.apk` (`tools/adbui.sh exec-out 'cat $(pm path <пакет> | cut -d: -f2)'`)
  с локальным APK; в спеках — `tools/adbui.sh install`, не голый `adb`».
- tools/bakery_states.sh:25 («ряд ●●●●» → «ряд ☆☆☆☆»); `tools/result_geom.py` — docstring в коммите ДО СПАВНА п. 5;
  прочие шапки `bakery_states.sh`, `town_route.sh` не лгут.
Число, закреплённое тестом, не обобщать (WORKFLOW № 48): тестов на UI итога нет — проверить грепом по `app/src/test`.

## ПРИ БЛОКЕРЕ
STATUS: BLOCKED + один конкретный вопрос. Не изобретать: код § 1 г не компилируется в дереве BASE (на копии собрался);
`boundsInParent` облачка и жителя в разных координатах (хвост уехал) — вопрос, а не `boundsInRoot`/поправка; хочется
модификатор у `Layout`, иной порог стороны, умолчание стороны, размер ☆ ≠ 16 dp, токен цвета, правка «!» в окне; зонд
показывает «Обрезано ≥ 1» на итоге — вопрос, а не обход.

## Журнал спеки
- 2026-09-29 (сессия 14): сведение двух черновиков — угол «от кода» (`$S/draft_code.md`) и «от приёмки»
  (`$S/draft_accept.md`) — по ответам владельца № 83–88 («всё по рекомендации»). Проверено по дереву 0538492 (код =
  8235e47): RoundScreen.kt :31-32, :107-156; TrayScreen.kt :59, :129-143, :218, :370, :413-430; StreetScreen.kt:91;
  RoomScreen.kt:38, :115, :142; PetSprite.kt:103, :118, :126 (один корневой `Box`); TownUi.kt:86, :89, :294. Замеры: высоты
  облачка — однострочником п. 9 в по дампам 1b1-2 (`s13eb_*`, `s13er_*`: 1,3 — все `front`, высоты — таблица КОНТЕКСТА);
  линия лап — 108 `pet_*`, все 440 / 512; контраст — 4.92 15.86 14.28 3.91. Прототип зонда `$S/result_geom_proto.py` —
  `SELFCHECK OK` (37; после критики — 39; после критика зонда — 41), на дампах BASE — красный там, где ожидается. Дифф копии `$S/clone` (-w): +26 −18, литералов новых 0,
  семантика равна, ORDER OK; APK клона собран.
  Решения сведения: **§ 1 — `Layout(contents)` черновика кода** (одно решение в одном проходе, собран), а не «питомец по
  `bubbleRect`» черновика приёмки (питомец кадром позже облачка, решение в двух местах, § 1 б). **☆ 16 dp** (черновик
  кода), а не 24 dp (черновик приёмки): ряд zero выше на 8 dp → zero при 1,3 281 dp > 278 — «перед жителем», против
  решения № 83 в; довод «16 dp читается кольцом» — артефакт классификатора по размеру, макеты читаются звездой.
  **Зонды — сессии** (`b13_*`, тексты в спеке — scratch сессионный), а не новый `tools/marks.py` и правки
  `bakery_states.sh`/`town_route.sh`: классификатор по размеру не пропустил бы ☆ 16 dp; ☆ рядом с ○ даёт уже снимаемый
  кадр маршрута `thanks` (ряд раунда ★ ☆ ○ ○), новый кадр `zero_round` не нужен. Из черновика приёмки: явный бэкап и
  возврат профиля (у черновика кода отпечаток сверялся после `town_route.sh`, который стирает прогресс, — не сошёлся бы
  никогда), копия debug BASE для возврата после клона, проверка семантики п. 8, контраст одной командой, вопросы
  «ребёнку» и самопроверка линзы, `git log` RoomScreen.kt (A1d2 не начата), журнал 1b1-2 уже называет 1b1-3. Шапка BASE
  — черновика кода (HEAD 0538492, дерево чистое). Доки — объединение списков + TOWN-J1.md :182, :184 и
  `bakery_states.sh:25` по грепу. Устройство не трогалось.
  **Критика r1** (`$S/TOWN-J1-1b1-3.r1.md`): три критика — контракт, приёмка, «ребёнок»; 13 находок и перечень «не
  опровергнуто» (не находка); на каждую — два скептика. Итог: 7 подтверждены обоими, 4 — частично, 2 опровергнуты.
  Принято (подтверждено):
  - **K2 (+ подтверждённая часть F3)**: `b13_star.py` мерил «пикселей столько-то», а шапка обещала контур — сплошная
    тёмная звезда 16 dp (≈ 2600–3830 px на 4 по четырём моделям скептиков) и золотая `Star(16.dp)` (≈ 1730–1930) лежали
    в коридоре 1000–4000. Добавлены дырки маски (`binary_fill_holes`, ≥ 4 × 150): BASE — holes 0 (FAIL), синтетика ☆ —
    2292 px / holes 1264 (OK), мутант «4 сплошные тёмные звезды» — 3540 px / holes 16 (FAIL). Шапка — «☆ от ○ и от
    золотой `Star(16.dp)` не отличает» (дырка у всех), их держит греп п. 7. Из F3 отклонено: проверка золота `#FFC94D` = 0
    — в облачко летят золотые монеты (`CoinsFrom("job_result")`, GameViewModel.kt:499), красный зависел бы от момента
    снимка; золотую ветку ловит греп точной строки.
  - **K3**: TOWN-J1.md :181 / :183 → :182 / :184; добавлены :146, :162 (их не находил ни один греп — новый шаблон
    `● — обслужен|в ряду ●|● без звезды`), :208 и TEST_CASES TC-28 :111 («перед Мартой» станет ложью).
  - **F2**: полоса ± 3 px держалась только справа — мутант `cap + 3` → `cap` перед жителем проходил (запас самопроверки
    до `cap − 15`). +2 случая (836 и 832 px): 39 случаев, `SELFCHECK OK`; `cap ± 3` → `± 0 / 1 / 2` — 6 × exit 1;
    мета-проверка — 10 условий. Расширение 3 → 4 не закреплено (ложный красный, не пропуск брака).
  - **F4**: awk проверял порядок текста содержимого, а не `place`; перестановка `p?.place` / `b.place` проходила. awk с
    `pp` / `bp` и грепы `p?.place(`, `b.place(bx.roundToPx(), ` (закрывают `?.let`): BASE FAIL, клон OK, перестановка
    FAIL. На 360 × 640 и S23 кадры питомца и облачка не пересекаются — перестановку видит только греп, живые зонды нет
    (видна при высоте содержимого < ≈ 285 dp — многооконный режим).
  - **F5**: в Git Bash `ps -ef | grep -E "town_route|bakery_states"` всегда находит строку обёртки `bash -c … eval '…'`
    (rc 0 без прогонов); со скобками отдельным вызовом — пусто, rc 1 (сведение); со скобками рядом с именем скрипта в том
    же вызове — снова находит. Спека — скобки и отдельный вызов; WORKFLOW № 40 и HANDOFF:82 — в ДОКИ; закрытая
    TOWN-J1-1b4.md (:232-233, :359-360) — без правки.
  - **C1**: остаток № 83 в шире «3★ и новый уровень»: 1★ (`result213`) = 3★ = 308,3 dp, перед жителем любой итог
    пекарни со ★ на 360 × 640 при 1,3 (спека противоречила себе: п. 9 в ждал `front` у `result213`). КОНТЕКСТ, §5.5,
    GATE_QUEUE, ключ судей и строка листа — через замер. Не мерились: 0★ в смену нового уровня (≈ 301 dp), Марта с
    новым уровнем (≈ 277 dp — в полосе зонда); необязательный замер 0★ + уровень не взят.
  - **C2**: на эмуляторе питомец — зайка в обоих прогонах; щенок перед Борей-щенком (кадр, из-за которого № 83) не
    снимался нигде, «ребёнок» занизил бы остаток (№ 42). Копия профиля со щенком одной командой (на копии профиля 1b4
    меняется только `pet`), прогоны `b13pupb` (п. 3) и `b13pup` (п. 9 б2) со своим логом, `GEOM OK` 4, явный возврат
    профиля после каждого; кадры — «ребёнку» и на лист.
  Частично:
  - **K4**: факты верны (101 отслеживаемый путь вне allow и protect — README, CHANGELOG, release APK, screenshots; guard,
    стоп-хук и п. 5–6 их не видят), «не заметят» опровергнуто — ревьювер смотрит всё дерево; ссылка на № 29 не по
    адресу. Принято: всё дерево в п. 6 — разностью со снимком перед спавном `$S/b13_st0.txt` (в дереве работают A1*;
    «ровно 4 пути» без снимка дал бы ложный FAIL). Отклонено: расширять protect.
  - **F1**: голый `adb` (нет в PATH Git Bash, exit 127) и «…» в пути — громкая ошибка, прецеденты 1a / 1b / 1b4 (REF);
    подтверждено: `pkgFlags` две debug-сборки не различает. Принято: «Установка APK» через `tools/adbui.sh install` и
    md5 установленного `base.apk` (чтение проверено при сведении: 1601d52a… = локальная debug-сборка); строка к № 33 —
    в ДОКИ.
  - **F3** — см. K2.
  - **F6**: однострочник п. 9 в с отступом пункта падает `IndentationError` — громко, счёт ложным не становится (REF);
    вариант через `;` — `SyntaxError`. Принято: файл `$S/b13_side.py` (как `b13_badge` / `star` / `tb`), exit 1 с именем
    кадра без дампа или узла, кадры — фигурными скобками bash; на BASE — `right 216.0 / front 273.0 / front 249.0`.
  Опровергнуты, не взяты: K1 (APK клона из ранней версии — компиляция финального текста подтверждена отдельно); C3
  (№ 87 б у работ без прилавка хуже BASE) — вопрос «провалился?» и так у судей «сцена» и в кандидатах ворот j1b13.
  При сведении критики: HEAD 0538492 → b98298b (A1d1 / A1e / A1g1), `git diff 8235e47 HEAD -- tools` = `tools/art_check.py`
  — п. 0 проверяет `app/src` и `tools` раздельно; HANDOFF «Дальше» :40 → :33, № 40 — :82; WORKFLOW:28-31 → :29-32.
  Прототип зонда (+2 случая) и `$S/result_geom.diff` пересобраны; прежние версии скриптов — `$S/*.r1.py`.
  **Критик зонда** (по коду `tools/result_geom.py` на bf68648; блокирующих нет, 3 находки — все подтверждены): (1) список
  1,3 S23 с `pet in front though bubble fits` без `s13sr_result213` — дополнен (п. 1); (2) случай «на полу, облачко ниже
  его верха» (питомец справа во всю ширину под облачком) в дампе невозможен — облачко режет верх кадра, зонд даёт `pet not
  on floor`; заменён на «из-под угла облачка» (питомец 200…488 px — bbox непокрытой части = весь кадр) → ровно `pet over
  bubble`; (3) допуск ± 2 px не был закреплён (мутанты `> 40`, `> 8` проходили) — +2 случая по 1 dp. 41 случай,
  `SELFCHECK OK`; мутанты автора 16/16 и критика 15/15 (с допусками и `cx(bub)` — теперь убит) — exit 1.
- 2026-09-29 (сессия 14), ДО СПАВНА → кодер → приёмка → живая → доки; **ревью ещё не было**. Логи — `$S` (`acc_1b13.log`,
  `acc_1b13_post.log`, `b13*_states.log`, `b13*_route.log`, `b13_install.log`). Снимок дерева `b13_st0.txt` — 0 строк;
  профиль `b13_profile.json` — md5 cba6afd3 (на BASE-копии debug md5 уже совпадал — без установки).
  **Красный на BASE** (п. 3, эмулятор 360 × 640): `b13b` — `GEOM FAIL` 6 (`pet not on floor; pet bottom not at buttons`;
  zero13 — ещё `pet in front though bubble fits`), `GEOM OK` 2 (`--line`), `CLOSE OK` 2, `NODES OK` 2; `b13rb` —
  `GEOM FAIL` 6 (tresult13 — ещё `pet in front…`), `GEOM OK` 2 (`why`); `b13pupb` — `GEOM FAIL` 4 (zero13 — ещё `pet in
  front…`); промахов 0, «Обрезано: 0» везде. По тем же дампам и снимкам (пересчёт при доках): стороны — все 1,3 `front`,
  все 1,0 `right`, у щенка так же; `b13_badge` — 2 FAIL (улица `#E0004A` bbox 72 × 72, комната `#E0004A` 0 и `#FF0053`
  5086 px); `b13_star` — 2 FAIL (6992 px, holes 0).
  **Положительный контроль** (п. 4): клон `$S/wclone` (§ 1–4 из текста спеки скриптом `b13_apply.py`, не `$S/clone`),
  `assembleGameDebug` — BUILD SUCCESSFUL, предупреждение компилятора одно — `PlaceScreen.kt:183` (вне четырёх файлов);
  `installed = …wclone…`: `b13p` — `GEOM OK` 8, `CLOSE OK` 2, `NODES OK` 2; `b13rp` — `GEOM OK` 8, `CLOSE OK` 2; стороны
  — `front` у `b13p_levelup13`, `b13rp_result13`, `b13rp_result213`, `right` 7; `b13_badge` 2 OK (5086 px, bbox 84 × 84);
  `b13_star` 2 OK (2876 px, holes 1060); `b13_tb` b13b → b13p 6 frames, 0 FAIL; затем debug BASE вернулась
  (`installed = …b13_base_debug.apk`).
  **Кодер:** 4 файла allow, `git diff -w` +26 −18 без import, import +2 −1; всё дерево минус снимок — 4 строки ` M` allow.
  **Машинная приёмка** (`acc_1b13.sh`, BASE 0c8a8fc): 60 проверок, 0 красных — 600/0/0 в обоих вариантах, сборки exit 0,
  lint ошибок 0, без сетевых 5 / 5, защищённые пути 0, греп п. 7 (с ORDER OK), `SELFCHECK OK` × 2, новых литералов и
  семантики 0, стоп-слов 0, контраст 4.92 15.86 14.28 3.91.
  **Живая** (п. 9; повтор скрипта `POST=1 NOGRADLE=1` — 74 проверки, 0 красных, п. 1–4 — SKIP, счёт первого прогона):
  APK 07:51:54 новее `app/src`, без пересборки; `installed = finny-pet/app/build/outputs/apk/game/debug/app-game-debug.apk`,
  pkgFlags DEBUGGABLE; `ps rc=1`; профиль b13 → b13 → puppy → b13, md5 cba6afd3 прежний. а) `GEOM OK` 8, `CLOSE OK` 2,
  `NODES OK` 2, промахов 0, «Обрезано: 0»; б) `GEOM OK` 8, `CLOSE OK` 2, промахов 0, «Обрезано: 0»; б2) щенок `GEOM OK` 4,
  промахов 0; в) `front` — `b13_levelup13` (336,3 dp), `b13r_result13`, `b13r_result213` (308,3), `right` 7 (zero13 273,0,
  tresult13 249,0), высоты — таблица КОНТЕКСТА ± 1 dp; щенок — `front` только `b13pup_levelup13`; г) «!» `#E0004A` 5086 px,
  bbox 84 × 84 на улице и в комнате, `#FF0053` 0; д) ☆ — 2876 px, holes 1060 (zero10 и zero13; синтетика давала 2292 /
  1264); е) TalkBack b13b → b13 — 6 frames, 0 FAIL, без повтора пары; ж) `ui_measure` 6 из 6. з) глазами — zero13,
  levelup13, levelup10, result10, thanks, tresult13, щенок levelup13, street, room (zero10 — нет). Отступления: команда
  POST — из шапки скрипта, gradle не перезапускался; лог маршрута б) — через `>`, не `tee`. Эмулятор после прогона: шрифт
  1,0, громкость 5, профиль = `b13_profile.json` (`cmp`), `wm360` оставлен.
  **Лист** `town/j1b13_gate.jpg` (1896 × 3660): 10 пар «до / после» по п. 11, заголовок «S23 — доснять», внизу примечания —
  остаток № 83 в строкой п. 11, значки, контраст; ★ и ☆ в подписях — словами (в Arial их нет). Проверка листа в POST шла
  по версии 1 042 370 Б (08:54), в дереве — пересобранный 08:58 (1 143 799 Б, с примечаниями).
  **Не сделано:** судьи п. 11, S23 (п. 9 и; GATE_QUEUE раздел 6), ревью, коммит. **Доки** (ДОКИ): GAME_CONCEPT §3.1, §5.5,
  §14, §18 № 83, 85, 87, 88; UX_ACCESSIBILITY; REQUIREMENTS_MATRIX 2.1, 3.6; BUILD_AND_DEMO шаг 6; TEST_CASES TC-28,
  TC-32; TOWN-J1.md (STATUS, «2. Экраны», срез 1б); WORKFLOW № 33, 40; `tools/bakery_states.sh:25`; HANDOFF — коммит
  оркестратора 1d4f9b3. Тестов на
  UI итога нет (`grep` по `app/src/test` пуст, № 48). GATE_QUEUE — ведёт оркестратор.
- 2026-09-29 (сессия 15) — **судьи, лист, ревью PASS**. Workflow сессии 14 `wf_c878dc50-5d6` (живая → лист и судьи ∥ доки →
  линзы доков и скептики → правка → ревьювер) доработал до конца; журнал — `…/57df72da-…/subagents/workflows/wf_c878dc50-5d6/journal.jsonl`.
  **Судьи** (п. 11): «доступность» — PASS по ТЗ 3.6: ☆ (15,3 × 14,7 dp, дырка 247 px) и ○ (16 dp, дырка 1044 px) одного
  цвета различаются формой в сером и при дейтеранопии; «!» — видимый круг 28 dp на улице и в комнате, белый знак 4,92 : 1
  (дейтеранопия 4,03); `ui_measure` по 10 дампам маршрута — мелких целей и узлов за краем 0; порядок значимых узлов 14 кадров
  итога — прежний. «Сцена» — все 4 решения (№ 83 в, 85 б, 87 б, 88 б) видны; попиксельная разность до/после — только значок,
  звёзды и питомец; новое: в why10, why13, levelwhy13 из-под строки «Почему?» видны лапы питомца (полоса ≈ 11 dp; на BASE
  карточка строки закрывала их целиком) → № 99; остаток № 83 в шире посылки ответа («два итога»): при 1,3 на 360 × 640 —
  любой итог пекарни с ★ и «новый уровень» → № 98; ☆ 16 dp в смешанном ряду на 10 dp ниже центра ★ → № 100. «Ребёнок»
  (12 кадров вслепую): ☆☆☆☆ — «ничего не заработано», ★★☆★ — «одну не заработал», контроль «до» ●●●● — не понят; перед
  жителем того же вида (зайка перед Мартой, щенок перед Борей) — «похож на детёныша» (2 из 8 кадров с питомцем); на ★☆○○
  раунда — «чем кружок отличается от пустой звёздочки?». Дефекты материалов судьи (не кода): в пачке «ребёнка» не было кадра со
  «!» (значок проверили «доступность» и «сцена»), в наборе «доступности» не было zero10/zero13 (судья снял сам, ☆ при 1,3
  читается), `b13_tb.py` по маршруту b13rb → b13r даёт 6 FAIL `round*`/`wrong*` от разных заказов прогонов (п. 9 е гоняет его
  только по пекарне — 6/0). «!» в комнате при 1,3 не снимался ни здесь, ни раньше (коробка 28 dp, знак 23,4 sp) — вход
  живой проверки A1d2.
  **Лист v2** (`mk_sheet2.py` в `$S`, сессия 15): легенда ○ — «ещё придёт (раунд) / не пришёл (итог)» (было «ещё не
  обслужен» — неверно для итога, находка «сцены»), «эмулятор» полностью, ряд 5 — пары why10 и levelwhy13 до/после, в
  примечаниях — увеличение ×2 низа строки «Почему?» (лапы видны только «после»). **Доки**: редактор + 2 линзы + скептики —
  правки внесены `docs-apply` до ревью (№ 46). **Ревью** (reviewer, BASE 0c8a8fc): **VERDICT PASS**, FINDINGS пусто — 600/0/0
  × 2, сборки, lint 0 / без сетевых 5 / 5, allow ровно 4 файла (+26 −18 без import), 31 счёт греп п. 7, `SELFCHECK OK` × 2,
  литералы и семантика — пусто, стоп-слов 0, контраст 4.92 15.86 14.28 3.91; `assert-oracle-intact` красный только от
  docs/ и `tools/bakery_states.sh` (§ ДОКИ); доки не врут (греп по обоим `docs/` и `tools/`). Вопросы № 98–100 — GATE_QUEUE
  раздел 8. S23 (п. 9 и) — доснять при владельце.

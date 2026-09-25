```
TASK: MVP-T04 — доступность и отключаемые анимации варианта game
BASE: 43eb809
BRANCH: отдельный worktree, мердж в feat/mvp-7-1

## КОНТЕКСТ
Сдаём вариант `game`. Проверка кода показала нарушения ТЗ 3.6: кнопки 44 dp, текст
12–14 sp во фразах, выбранный вариант отмечен только цветом, тумблер «Анимации» не
гасит частицы, переходы экранов и анимации мини-игры. Поведение игры (экономика,
звук, мини-игра) эта задача не меняет — это MVP-T03.

## ТРЕБОВАНИЕ ТЗ
3.6: «Размер основных кнопок — не менее 48 dp»; «основной текст — не менее 16 sp, если
иное не обосновано»; «Цвет не является единственным способом передать ошибку, успех,
категорию расхода или состояние питомца»; «Звуки и анимации можно отключить».

## CONTRACT
1. **Касания ≥ 48 dp** во всех экранах `game`: кружки цвета (сейчас 44), кнопки «−/+»
   плана (ширина 44 в портрете), чипы имён, чипы своей цели и цены, «Выбрать» в
   комнате (minHeight 44). Кнопка с краем 5 dp считается по видимой высоте ≥ 48.
2. **Текст.** Все фразы и подсказки — ≥ 16 sp: `bodySmall` в теме → 16 sp. Однословные
   подписи, бейджи, числа — ≥ 14 sp: `labelSmall` 12 → 14 sp; `labelMedium` (14) остаётся
   для однословных подписей. Места, где фраза набрана `labelMedium`/`labelSmall`
   (строка цели в комнате, эффект товара и «не хватает» в магазине, подпись монет
   мини-игры и т.п.), перевести на стиль ≥ 16 sp. Исключения (14 sp) перечислить в
   отчёте — оркестратор впишет обоснование в UX_ACCESSIBILITY.md.
3. **Крупный системный шрифт.** Фиксированные `width(170/200.dp)` панелей комнаты →
   `widthIn(min, max)` с переносом строк; `maxLines = 1` у фраз убрать или дать
   многоточие + полный текст в `contentDescription`. Сцена комнаты и мини-игра
   ограничивают масштаб шрифта 1,3 (через `LocalDensity` с `fontScale = min(системный, 1.3f)`);
   панели (план, магазин, копилка, задания, прогресс, взрослый) следуют системному
   масштабу полностью.
4. **Выбор не только цветом.** Выбранный вид, цвет, имя-чип, вкладка магазина, сумма
   копилки, своя цель/цена отмечены галочкой «✓» (или иконкой) в дополнение к цвету, и
   модификатор `selectable(selected = …, role = Role.RadioButton)` / `semantics { selected = … }`
   — TalkBack объявляет выбор.
5. **Анимации отключаются целиком.** `CompositionLocal` `LocalAnimate` в `GameApp`:
   `vm.state.animations && системный ANIMATOR_DURATION_SCALE > 0`
   (`Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)`).
   При `false`: частицы (монеты, конфетти, сердечки, искры) не запускаются; переход
   экранов `AnimatedContent` и `FeedbackOverlay` — без fade/scale (мгновенно);
   фон вечера — без crossfade; `GameBar`, `HudChip` — без анимации числа и полосы;
   `PropButton`/`GameButton` — без пружины нажатия; мини-игра: обмен плиток, всплывающие
   очки, панели «игра окончена»/вопрос — мгновенно (падение и исчезновение уже учитывают
   флаг); пузырь речи — без scale. Все существующие места, читающие `state.animations`,
   перевести на `LocalAnimate`.

## SCOPE
variant: game
allow:
  finny-pet/app/src/game/java/ru/finny/pet/game/GameApp.kt
  finny-pet/app/src/game/java/ru/finny/pet/game/ui/*.kt
  finny-pet/app/src/game/java/ru/finny/pet/game/screens/*.kt
protect: всё остальное (domain, data, content.json, GameViewModel.kt, Sfx.kt, тесты, gradle, docs)

## ANTI-SCOPE
- Поведение: навигация, экономика, звук, мини-игра, бонус взрослого, барьер — MVP-T03.
- Новые экраны, редизайн, новые цвета палитры, новые зависимости, ресурсы.
- classic.

## БЮДЖЕТ
диff ≤ 400 строк; новые файлы: 0 (кроме, при необходимости, одного `ui/Animate.kt`); зависимости: 0

## ACCEPTANCE (из finny-pet/)
  1. testGameDebugUnitTest testClassicDebugUnitTest            -> exit 0, число тестов не меньше, чем на BASE
  2. assembleClassicDebug assembleGameDebug                     -> exit 0
  3. lintClassicDebug lintGameDebug                             -> 0 ошибок, ≤ 8 / 9 предупреждений
  4. git diff --name-only BASE -- ':!finny-pet/app/src/game/'   -> пусто
  5. grep -rn "44.dp" app/src/game/java | grep -i "minHeight\|clickable\|width" -> пусто
  6. grep -rn "state.animations\|s.animations" app/src/game/java/ru/finny/pet/game/screens app/src/game/java/ru/finny/pet/game/ui -> пусто (всё через LocalAnimate)
     [с MVP-T12 п.7 исключение: тумблер «Анимации» в ParentScreen читает s.animations намеренно]

## ЖИВАЯ ПРОВЕРКА (оркестратор, эмулятор finni)
- портрет 411×914 и 360×640 dp, шрифт 100% и 130%: комната, создание питомца, план, магазин,
  копилка, мини-игра — без наложений и обрезанных фраз;
- тумблер «Анимации» выкл → покупка, ответ на задание, конец недели, мини-игра без частиц
  и переходов;
- TalkBack (или дамп `uiautomator`): у выбранного цвета/вида есть selected.
```

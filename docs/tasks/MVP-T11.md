```
TASK: MVP-T11 — согласование чисел в интерфейсе game и «очков роста»
BASE: fe2072d
BRANCH: feat/mvp-7-1

## КОНТЕКСТ
Живая проверка итога недели: «До следующей стадии: 3 очков роста». В UI game 13 мест вида
«${n} монет» с произвольным n (баланс, цена): «разделим 101 монет», «Стоит 21 монет».
В domain уже есть `Economy.coins(n, Case)` (MVP-T01a).

## ТРЕБОВАНИЕ ТЗ
8.3: возрастная уместность текстов; 2.5.14: тексты верны при любых числах из content.json.

## CONTRACT
1. `Economy.points(n: Int): String` (companion) → «1 очко», «2 очка», «5 очков», «11 очков»,
   «21 очко», «22 очка», «0 очков» (правило форм — как у `coins`). Сообщение конца недели:
   `До следующей стадии: {points(left)} роста.`
2. UI game — все места «${число} монет» через `Economy.coins(n, Case)`:
   | место | новый текст |
   |---|---|
   | GameViewModel.nextStep, нет плана | `Давай разделим {coins(balance, ACC)}: обязательное, желаемое и копилка!` |
   | MiniGameScreen, подпись лимита | `Монет за игру: до {N}. Очков на монету: {M}.` |
   | ParentScreen, бонус (текст) | `Бонусов в неделю: до {parentBonusPerPeriod}, каждый — +{coins(parentBonusAmount)}. За дела, а не за оценки. Осталось на этой неделе: {left}.` |
   | ParentScreen, подтверждение | `Начислить {coins(A, ACC)}?` |
   | PlanScreen, нехватка | `Не хватает {coins(n, GEN)}` |
   | RoomScreen, цель | `{savings} из {coins(price, GEN)}` |
   | RoomScreen, конец недели | `{pet} получит итог недели, а ты — карманные деньги: +{coins(allowance)}.` |
   | SavingsScreen, цена цели | `Стоит {coins(price, ACC)}` |
   | SavingsScreen, забрать цель | `Из копилки уйдёт {coins(price)}, …` (остальное без изменений) |
   | SavingsScreen, contentDescription цели | `{title}, {coins(price)}` |
   | ShopScreen, contentDescription | `{title}, {coins(price)}, {эффект}` |
   | ShopScreen, подтверждение | `Цена: {coins(price)}, у тебя {balance}. Останется {…}.` |
   | TaskScreens, награда | `Верный ответ: +{coins(rewardCorrect)}, попытка: +{coins(rewardWrong)}` |
   | ProgressScreens:163 и любые другие найденные | по смыслу через coins() с правильным падежом |
3. Проверка: `grep -rnE '\} монет' app/src/game/java app/src/main/java` → пусто.
4. Альбом (640×360 и 914×411 dp): плашка «Сейчас» в правой колонке показывает текст шага
   целиком («Сейчас: Составить план», «Сейчас: Завершить неделю») — сейчас обрезается до
   «Сейчас: Составить». Допустимо 2–3 строки или подпись «Сейчас:» над кнопкой; кнопка ≥ 48 dp.

## SCOPE
allow: domain/Economy.kt (points и одна строка сообщения); finny-pet/app/src/game/java/**
protect: тесты, content.json, classic, gradle, docs

## БЮДЖЕТ
диff ≤ 120 строк

## ORACLE
test-author: тесты points() и текста «До следующей стадии: … роста» (числа 1, 3, 5, 21 через rules).

## ACCEPTANCE
  1. тесты обоих вариантов -> exit 0, 113 + N; сборки; lint ≤ 5 / 6 без сетевых
  2. git diff --name-only <ORACLE_SHA> -- app/src/test/ -> пусто
  3. grep из CONTRACT 3 -> пусто
  4. эмулятор: итог недели, копилка, магазин, раздел для взрослого — оркестратор
```

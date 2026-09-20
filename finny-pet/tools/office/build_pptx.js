// Builds docs/Finny_Presentation.pptx (10 slides per section 4 of the ТЗ).
// Usage: node tools/office/build_pptx.js
const fs = require("fs");
const path = require("path");
const pptxgen = require("pptxgenjs");

const ROOT = path.resolve(__dirname, "..", "..");
const SHOTS = path.join(ROOT, "screenshots", "store");
const OUT = path.join(ROOT, "docs", "Finny_Presentation.pptx");

const P = "520978"; // brand purple (ЛЦТ template)
const O = "FF0053"; // accent magenta (ЛЦТ template)
const INK = "1C1D22";
const LILAC = "F5D9FF";
const WHITE = "FFFFFF";
const MUTED = "6B6B6B";
const FONT = "Arial";

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10 x 5.625 in
pres.author = "Finny team";
pres.title = "Питомец Финни";

const shot = (name) => {
  const f = path.join(SHOTS, name);
  return fs.existsSync(f) ? f : null;
};
const phone = (slide, file, x, y, h) => {
  const w = h * 1080 / 2340;
  if (file) slide.addImage({ path: file, x, y, w, h, rounding: false });
  else slide.addShape(pres.ShapeType.roundRect, { x, y, w, h, fill: { color: LILAC }, rectRadius: 0.15 });
  return w;
};
const title = (slide, text, dark = false) =>
  slide.addText(text, { x: 0.5, y: 0.35, w: 9, h: 0.7, fontFace: FONT, fontSize: 26, bold: true, color: dark ? WHITE : P, isTextBox: true, margin: 0, fit: "shrink" });
const body = (slide, items, x, y, w, h, opts = {}) =>
  slide.addText(items.map((t, i) => ({ text: t, options: { bullet: opts.bullet !== false, breakLine: i < items.length - 1, paraSpaceAfter: 6 } })),
    { x, y, w, h, fontFace: FONT, fontSize: opts.size || 14, color: opts.color || INK, valign: "top", isTextBox: true, margin: 0 });
const card = (slide, x, y, w, h, head, text, color = LILAC) => {
  slide.addShape(pres.ShapeType.roundRect, { x, y, w, h, fill: { color }, line: { color, width: 0 }, rectRadius: 0.12 });
  slide.addText(head, { x: x + 0.15, y: y + 0.1, w: w - 0.3, h: 0.4, fontFace: FONT, fontSize: 14, bold: true, color: INK, isTextBox: true, margin: 0 });
  slide.addText(text, { x: x + 0.15, y: y + 0.5, w: w - 0.3, h: h - 0.6, fontFace: FONT, fontSize: 11.5, color: INK, valign: "top", isTextBox: true, margin: 0 });
};
const footer = (slide, n) =>
  slide.addText(`Питомец Финни · ${n}/10`, { x: 0.5, y: 5.2, w: 9, h: 0.3, fontFace: FONT, fontSize: 9, color: MUTED, align: "right", isTextBox: true, margin: 0 });

// 1. Title
{
  const s = pres.addSlide();
  s.background = { color: P };
  s.addImage({ path: path.join(ROOT, "assets/icon/icon-512.png"), x: 0.6, y: 0.7, w: 1.3, h: 1.3 });
  s.addText("Питомец Финни", { x: 0.6, y: 2.2, w: 5.6, h: 0.9, fontFace: FONT, fontSize: 40, bold: true, color: WHITE, isTextBox: true, margin: 0 });
  s.addText("Игровой сервис для формирования базовых финансовых навыков у детей 7–11 лет", { x: 0.6, y: 3.1, w: 5.4, h: 0.9, fontFace: FONT, fontSize: 16, color: "EADCF5", isTextBox: true, margin: 0 });
  s.addText("Функциональный прототип для Android · ТЗ Департамента финансов города Москвы · 2026", { x: 0.6, y: 4.5, w: 5.6, h: 0.5, fontFace: FONT, fontSize: 11, color: "CBB6DC", isTextBox: true, margin: 0 });
  phone(s, shot("02_home.png"), 7.0, 0.4, 4.8);
}

// 2. Problem and audience
{
  const s = pres.addSlide();
  title(s, "Проблема и целевая аудитория");
  card(s, 0.5, 1.3, 2.9, 1.7, "Кто", "Дети 7–11 лет, которые впервые получают карманные деньги: читают короткие фразы, считают в пределах 100–200.");
  card(s, 3.55, 1.3, 2.9, 1.7, "Что не так", "Про доходы и расходы объясняют абстрактно — ребёнок быстро устаёт. Ошибиться с реальными деньгами страшно.", "FFE3C8");
  card(s, 6.6, 1.3, 2.9, 1.7, "Чего не хватает", "Места, где решение принимаешь сам, сразу видишь последствие и можешь исправить ошибку без риска.");
  body(s, [
    "Виртуальный питомец делает последствия наглядными: от распределения монет зависят его настроение и рост.",
    "Родитель поддерживает, но не решает за ребёнка: у него свой раздел с целями и прогрессом.",
    "Никаких реальных денег, рекламы, аккаунтов и сбора данных.",
  ], 0.5, 3.3, 9, 1.8, { size: 14 });
  footer(s, 2);
}

// 3. Educational outcomes
{
  const s = pres.addSlide();
  title(s, "Образовательные результаты");
  s.addText("Единая рамка компетенций в области финансовой грамотности и финансовой культуры — базовый уровень, начальное общее образование", { x: 0.5, y: 1.1, w: 9, h: 0.5, fontFace: FONT, fontSize: 12, color: MUTED, isTextBox: true, margin: 0 });
  const rows = [
    ["Р1", "Понимать назначение бюджета; расходы не превышают доходов", "план на неделю; отрицательный баланс невозможен; журнал «откуда монеты»"],
    ["Р2", "Различать обязательные и необязательные расходы", "два раздела магазина; проверка «еда и уход куплены»"],
    ["Р3", "Планировать покупки при ограниченном бюджете", "100 монет в неделю, цены 10–60, сравнение план/факт"],
    ["Р4", "Ставить цель и регулярно откладывать", "цель с ценой, копилка, срок по среднему взносу"],
    ["Р5", "Оценивать свои решения и объяснять их", "итог недели «что случилось и почему», объяснение настроения питомца"],
  ];
  s.addTable(
    [[{ text: "Код", options: { bold: true, fill: { color: LILAC } } }, { text: "Навык", options: { bold: true, fill: { color: LILAC } } }, { text: "Механика в приложении", options: { bold: true, fill: { color: LILAC } } }],
      ...rows.map((r) => r.map((c) => ({ text: c })))],
    { x: 0.5, y: 1.7, w: 9, colW: [0.7, 4.0, 4.3], fontFace: FONT, fontSize: 12, color: INK, border: { type: "solid", color: "DDDDDD", pt: 0.5 }, rowH: 0.55, valign: "middle" },
  );
  footer(s, 3);
}

// 4. Product idea
{
  const s = pres.addSlide();
  title(s, "Идея: почему питомец учит считать");
  const w = phone(s, shot("11_pet_grown.png"), 0.5, 1.3, 3.7);
  const x = 0.5 + w + 0.4;
  body(s, [
    "Обучение действием: сначала выбор, потом финансовое и игровое последствие.",
    "Три решения каждую неделю — обязательное, желаемое, копилка — это и есть план бюджета.",
    "Состояние питомца (сытость, чистота, настроение) отвечает на решения сразу; стадия роста — на серию недель.",
    "Ошибка безопасна: питомец не болеет и не гибнет, показатели не падают ниже 10, рост не откатывается.",
    "Каждое изменение баланса и настроения объясняется одной фразой: «что изменилось и почему».",
    "Дизайн — Material 3: сгенерированная из цвета бренда схема, крупные элементы, нижняя навигация, светлая и тёмная тема, питомец нарисован кодом и оживает анимацией.",
  ], x, 1.3, 9.5 - x, 3.8, { size: 13 });
  footer(s, 4);
}

// 5. User path and economy
{
  const s = pres.addSlide();
  title(s, "Пользовательский путь и игровая экономика");
  const steps = ["Знакомство\n3 шага", "Питомец\n9 видов", "План\n50 / 20 / 30", "Покупки\nи задания", "Копилка\nи цель", "Итог недели\n+ рост"];
  steps.forEach((t, i) => {
    const x = 0.5 + i * 1.55;
    s.addShape(pres.ShapeType.roundRect, { x, y: 1.35, w: 1.35, h: 1.0, fill: { color: i % 2 ? "FFE3C8" : LILAC }, line: { color: WHITE, width: 0 }, rectRadius: 0.1 });
    s.addText(t, { x, y: 1.35, w: 1.35, h: 1.0, fontFace: FONT, fontSize: 11, bold: true, color: INK, align: "center", valign: "middle", isTextBox: true });
    if (i < steps.length - 1) s.addText("→", { x: x + 1.33, y: 1.55, w: 0.25, h: 0.6, fontFace: FONT, fontSize: 16, color: P, align: "center", isTextBox: true, margin: 0 });
  });
  card(s, 0.5, 2.65, 4.4, 2.3, "Доход и расходы", "Карманные деньги +100 в неделю; задания +20 (верно) / +5 (попытка).\nПокупка: цена ≤ баланс, показатель питомца +эффект.\nКопилка: взнос ≤ баланс; снятие с предпросмотром «цель отодвинется».\nСрок цели = остаток ÷ средний взнос.");
  card(s, 5.1, 2.65, 4.4, 2.3, "Итог недели → рост", "Три проверки: еда и уход куплены · траты по плану · копилка выросла → 0–3 очка роста.\nЕстественное убывание: сытость −30, чистота −25, настроение −10; бонусы +10 за план и копилку.\nСтадии: Малыш 0–3 → Подросток 4–8 → Взрослый 9+. Назад не откатывается.", "FFE3C8");
  footer(s, 5);
}

// 6. Mandatory features and boundaries
{
  const s = pres.addSlide();
  title(s, "Обязательные функции и границы прототипа");
  body(s, [
    "2.5.1–2.5.2 Знакомство, гостевой профиль, настройка и имя питомца",
    "2.5.3 Главный экран: питомец, показатели, монеты, копилка, цель, «Сейчас»",
    "2.5.4–2.5.5 Валюта с источником каждой монеты; план по 3 направлениям, контроль суммы",
    "2.5.6 10 товаров двух типов, подтверждение, отказ с объяснением при нехватке",
    "2.5.7 4 цели + своя, копилка, срок по среднему взносу, предпросмотр снятия",
    "2.5.8 10 заданий по 3 темам, выбор и ввод числа, объяснение всегда",
    "2.5.9–2.5.11 Обратная связь, 3 стадии и 3 выражения, история, справка",
    "2.5.12–2.5.14 Раздел взрослого с барьером, сохранение, демо-режим, контент в JSON",
  ], 0.5, 1.25, 5.6, 3.9, { size: 12.5 });
  card(s, 6.4, 1.25, 3.1, 3.9, "Осознанно за границей", "• Реальные деньги, платежи, банки\n• Реклама, подписки, покупки\n• Чаты, рейтинги, соцсети\n• Аккаунты и сбор данных\n• Сервер, ИИ, облако\n• Публикация в RuStore на этапе конкурса (карточка и подписанный APK готовы)\n• Начисление баллов родителем — в плане развития", "FFE3C8");
  footer(s, 6);
}

// 7. UX decisions
{
  const s = pres.addSlide();
  title(s, "Ключевые UX/UI-решения");
  const files = ["03_plan.png", "06_not_enough.png", "09_summary.png"];
  const caps = ["План: карточки направлений, ±10 тональными кнопками, доля бюджета", "Нехватка: не «нельзя», а «не хватает 25 — вот 3 варианта»", "Итог недели: три галочки, план/факт, объяснение простыми словами"];
  files.forEach((f, i) => {
    const x = 0.5 + i * 3.1;
    const w = phone(s, shot(f), x + 0.45, 1.2, 2.9);
    s.addText(caps[i], { x, y: 4.2, w: 2.9, h: 0.9, fontFace: FONT, fontSize: 10.5, color: INK, align: "center", isTextBox: true, margin: 0 });
    void w;
  });
  footer(s, 7);
}

// 8. Architecture
{
  const s = pres.addSlide();
  title(s, "Архитектура, стек, данные, контент");
  const boxes = [
    ["UI", "Jetpack Compose, Material 3 1.4\nNavigationSuite (панель / рейл), тёмная тема\nGameViewModel, 11 экранов, PetView на Canvas", LILAC],
    ["Data", "ContentRepository — assets/content.json\nStateStore — filesDir/state.json\nатомарная запись, без сети", "FFE3C8"],
    ["Domain", "Economy.kt — чистый Kotlin\nплан · покупки · копилка · задания · неделя · рост\n25 JVM-тестов", LILAC],
  ];
  boxes.forEach(([h, t, c], i) => {
    const x = 0.5 + i * 3.1;
    s.addShape(pres.ShapeType.roundRect, { x, y: 1.3, w: 2.9, h: 1.9, fill: { color: c }, line: { color: c, width: 0 }, rectRadius: 0.12 });
    s.addText(h, { x: x + 0.15, y: 1.4, w: 2.6, h: 0.4, fontFace: FONT, fontSize: 16, bold: true, color: P, isTextBox: true, margin: 0 });
    s.addText(t, { x: x + 0.15, y: 1.85, w: 2.6, h: 1.3, fontFace: FONT, fontSize: 11, color: INK, valign: "top", isTextBox: true, margin: 0 });
    if (i < 2) s.addText("→", { x: x + 2.85, y: 1.9, w: 0.3, h: 0.6, fontFace: FONT, fontSize: 18, color: P, align: "center", isTextBox: true, margin: 0 });
  });
  body(s, [
    "Стек: Kotlin 2.4, AGP 9.2, Compose BOM 2026.06 (Material 3, adaptive navigation suite, Material Symbols), kotlinx.serialization; minSdk 26 (Android 8.0), targetSdk 36; APK release 1,5 МБ (R8), без разрешений.",
    "Обновление контента: новое задание = запись в content.json; тест ContentTest проверяет структуру и минимумы ТЗ при каждой сборке; код не меняется.",
    "Сохранение: один JSON-файл профиля, никаких персональных данных; сброс и удаление — из раздела для взрослого.",
  ], 0.5, 3.45, 9, 1.7, { size: 12.5 });
  footer(s, 8);
}

// 9. Testing, limitations, roadmap
{
  const s = pres.addSlide();
  title(s, "Тестирование, ограничения, план доработки");
  card(s, 0.5, 1.3, 2.9, 3.7, "Проверено", "• 25 JVM-тестов экономики и контента — зелёные\n• Сквозной сценарий (12 шагов) на эмуляторе Android 16, arm64, release-сборка с R8\n• Тёмная тема, альбомная ориентация, рейл навигации\n• Запуск до главного экрана: ~1 с\n• Три раунда код-ревью, автопрогон tools/demo_run.sh");
  card(s, 3.55, 1.3, 2.9, 3.7, "Ограничения", "• Физическое устройство с 3 ГБ RAM — прогон предстоит команде перед сдачей\n• Неделя завершается кнопкой, календарной привязки нет (осознанно)\n• Графика питомца — примитивы Canvas\n• Один профиль на устройство\n• Без звука и планшетной раскладки", "FFE3C8");
  card(s, 6.6, 1.3, 2.9, 3.7, "Дальше", "1. Пилот с детьми при согласии родителей\n2. Родительские бонусы за реальные дела\n3. Больше заданий и событий через JSON\n4. Художественные спрайты и звук\n5. Несколько профилей\n6. Публикация в RuStore, 0+");
  footer(s, 9);
}

// 10. Links
{
  const s = pres.addSlide();
  s.background = { color: P };
  title(s, "Материалы для экспертов", true);
  body(s, [
    "Репозиторий: папка проекта finny-pet (ветка feat/finny-mvp) — README с быстрым запуском",
    "Сборка: release/finny-pet-1.2.0-release.apk — подписанный релиз, package ru.finny.pet, версия 1.2.0",
    "Документация: docs/Finny_Documentation.docx и docs/*.md — архитектура, формулы, матрица требований, тест-кейсы",
    "Демо: docs/BUILD_AND_DEMO.md — включение тестового профиля, сценарий на 4–5 минут; резервное видео docs/demo.mp4",
    "Карточка RuStore: docs/RUSTORE_CARD.md, иконка assets/icon/icon-512.png, скриншоты screenshots/store/",
  ], 0.5, 1.4, 6.2, 3.6, { size: 13, color: WHITE });
  phone(s, shot("13_parent.png"), 7.2, 0.4, 4.8);
  footer(s, 10);
}

pres.writeFile({ fileName: OUT }).then((f) => console.log("written", f));

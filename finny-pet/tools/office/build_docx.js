// Builds docs/Finny_Documentation.docx from the Markdown files in docs/.
// Supports the subset of Markdown used there: headings, paragraphs, bullet and numbered
// lists, pipe tables, fenced code, inline **bold** and `code`, and [text](link).
// Usage: node tools/office/build_docx.js
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, Table, TableRow, TableCell, WidthType,
  ShadingType, AlignmentType, LevelFormat, PageBreak, ImageRun,
} = require("docx");

const ROOT = path.resolve(__dirname, "..", "..");
const DOCS = path.join(ROOT, "docs");
const OUT = path.join(DOCS, "Finny_Documentation.docx");

// Order follows section 5 of the ТЗ.
const SECTIONS = [
  ["README.md", "Назначение продукта, состав репозитория, быстрый запуск", path.join(ROOT, "README.md")],
  ["BUILD_AND_DEMO.md", "Окружение, сборка APK, демонстрационный режим"],
  ["ARCHITECTURE.md", "Функциональная и компонентная архитектура"],
  ["DATA_MODEL.md", "Структура данных профиля, экономики, заданий и прогресса"],
  ["REQUIREMENTS_MATRIX.md", "Матрица соответствия обязательным требованиям"],
  ["ECONOMY.md", "Формулы и правила расчёта баланса, наград, состояния и роста питомца"],
  ["CONTENT_MAP.md", "Карта образовательного контента"],
  ["UX_ACCESSIBILITY.md", "Обоснование UX/UI-решений и настройки доступности"],
  ["PRIVACY_PERMISSIONS.md", "Разрешения Android, собираемые данные, удаление профиля"],
  ["TEST_CASES.md", "Тест-кейсы и отчёт о проверке"],
  ["LIMITATIONS_ROADMAP.md", "Известные ограничения и план развития"],
  ["LICENSES.md", "Сторонние библиотеки, шрифты, изображения и лицензии"],
  ["RUSTORE_CARD.md", "Черновик карточки приложения для RuStore"],
];

const FONT = "Arial";
const PURPLE = "520978";
const PAGE_WIDTH_DXA = 11906 - 2 * 1134; // A4 minus 2 cm margins

function inline(text) {
  // Split by **bold**, `code`, [text](url) — links keep the text only.
  const runs = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\([^)]+\))/g;
  let last = 0;
  let m;
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) runs.push(new TextRun({ text: text.slice(last, m.index), font: FONT }));
    const tok = m[0];
    if (tok.startsWith("**")) runs.push(new TextRun({ text: tok.slice(2, -2), bold: true, font: FONT }));
    else if (tok.startsWith("`")) runs.push(new TextRun({ text: tok.slice(1, -1), font: "Courier New", size: 20 }));
    else runs.push(new TextRun({ text: tok.slice(1, tok.indexOf("]")), font: FONT, color: PURPLE }));
    last = m.index + tok.length;
  }
  if (last < text.length) runs.push(new TextRun({ text: text.slice(last), font: FONT }));
  return runs;
}

function tableFromRows(rows) {
  const cols = rows[0].length;
  const width = Math.floor(PAGE_WIDTH_DXA / cols);
  const widths = Array(cols).fill(width);
  return new Table({
    width: { size: width * cols, type: WidthType.DXA },
    columnWidths: widths,
    rows: rows.map((cells, r) => new TableRow({
      tableHeader: r === 0,
      children: cells.map((c) => new TableCell({
        width: { size: width, type: WidthType.DXA },
        shading: r === 0 ? { type: ShadingType.CLEAR, fill: "EADCF5", color: "auto" } : undefined,
        margins: { top: 60, bottom: 60, left: 100, right: 100 },
        children: [new Paragraph({ children: inline(c).map((run) => run), spacing: { before: 0, after: 0 } })],
      })),
    })),
  });
}

let listInstance = 0;

function convert(md, level0) {
  const out = [];
  const lines = md.split("\n");
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (line.startsWith("```")) {
      const lang = line.slice(3).trim();
      const code = [];
      i++;
      while (i < lines.length && !lines[i].startsWith("```")) code.push(lines[i++]);
      i++;
      if (lang === "mermaid") {
        out.push(new Paragraph({ children: [new TextRun({ text: "Схема: UI → данные → домен; учебный контент (JSON в assets) → данные; файл профиля state.json ↔ данные. Домен не зависит ни от чего.", italics: true, font: FONT })], spacing: { after: 120 } }));
        continue;
      }
      code.forEach((c) => out.push(new Paragraph({
        children: [new TextRun({ text: c || " ", font: "Courier New", size: 18 })],
        shading: { type: ShadingType.CLEAR, fill: "F3EEF7", color: "auto" },
        spacing: { before: 0, after: 0 },
      })));
      out.push(new Paragraph({ text: "" }));
      continue;
    }
    const h = /^(#{1,4})\s+(.*)$/.exec(line);
    if (h) {
      const lvl = Math.min(h[1].length - 1 + level0, 3);
      const HL = [HeadingLevel.HEADING_1, HeadingLevel.HEADING_2, HeadingLevel.HEADING_3, HeadingLevel.HEADING_4][lvl];
      out.push(new Paragraph({ heading: HL, children: [new TextRun({ text: h[2], font: FONT })] }));
      i++;
      continue;
    }
    if (line.startsWith("|")) {
      const rows = [];
      while (i < lines.length && lines[i].startsWith("|")) {
        const cells = lines[i].slice(1, lines[i].endsWith("|") ? -1 : undefined).split("|").map((c) => c.trim());
        if (!cells.every((c) => /^:?-{2,}:?$/.test(c))) rows.push(cells);
        i++;
      }
      const cols = Math.max(...rows.map((r) => r.length));
      rows.forEach((r) => { while (r.length < cols) r.push(""); });
      out.push(tableFromRows(rows));
      out.push(new Paragraph({ text: "" }));
      continue;
    }
    const bullet = /^(\s*)[-•]\s+(.*)$/.exec(line);
    if (bullet) {
      out.push(new Paragraph({ children: inline(bullet[2]), numbering: { reference: "bullets", level: Math.min(Math.floor(bullet[1].length / 2), 2) } }));
      i++;
      continue;
    }
    const num = /^\s*(\d+)\.\s+(.*)$/.exec(line);
    if (num) {
      if (num[1] === "1") listInstance++; // a new list restarts numbering
      out.push(new Paragraph({ children: inline(num[2]), numbering: { reference: "numbers", level: 0, instance: listInstance } }));
      i++;
      continue;
    }
    if (line.trim() === "") { i++; continue; }
    // paragraph: join consecutive text lines
    const buf = [line.trim()];
    i++;
    while (i < lines.length && lines[i].trim() !== "" && !/^(#|\||```|\s*[-•]\s|\s*\d+\.\s)/.test(lines[i])) buf.push(lines[i++].trim());
    out.push(new Paragraph({ children: inline(buf.join(" ")), spacing: { after: 120 } }));
  }
  return out;
}

function image(file, widthPx) {
  const data = fs.readFileSync(file);
  const ratio = 2340 / 1080;
  return new ImageRun({ type: "png", data, transformation: { width: widthPx, height: Math.round(widthPx * ratio) } });
}

const children = [];
// Title page
children.push(new Paragraph({ text: "" }), new Paragraph({ text: "" }), new Paragraph({ text: "" }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new ImageRun({ type: "png", data: fs.readFileSync(path.join(ROOT, "assets/icon/icon-512.png")), transformation: { width: 160, height: 160 } })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 400 }, children: [new TextRun({ text: "Питомец Финни", bold: true, size: 64, font: FONT, color: PURPLE })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "Мобильное приложение — игровой сервис для формирования базовых финансовых навыков у детей 7–11 лет", size: 28, font: FONT })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 300 }, children: [new TextRun({ text: "Сопроводительная документация к функциональному прототипу", size: 24, font: FONT })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "Техническое задание: Департамент финансов города Москвы, 2026", size: 22, font: FONT, color: "666666" })] }));
children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "Версия 1.2.0 · пакет ru.finny.pet · Android 8.0+", size: 22, font: FONT, color: "666666" })] }));
children.push(new Paragraph({ children: [new PageBreak()] }));
children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: "Содержание", font: FONT })] }));
SECTIONS.forEach(([file, subtitle], idx) => {
  const head = fs.readFileSync(idx === 0 ? path.join(ROOT, "README.md") : path.join(DOCS, file), "utf8").match(/^# (.*)$/m)[1];
  children.push(new Paragraph({ spacing: { after: 60 }, children: [new TextRun({ text: `${idx + 1}. ${head}`, bold: true, font: FONT }), new TextRun({ text: ` — ${subtitle}`, font: FONT, color: "666666" })] }));
});
children.push(new Paragraph({ spacing: { after: 60 }, children: [new TextRun({ text: `${SECTIONS.length + 1}. Приложение. Экраны прототипа`, bold: true, font: FONT })] }));
children.push(new Paragraph({ children: [new PageBreak()] }));

SECTIONS.forEach(([file, subtitle, abs], idx) => {
  const md = fs.readFileSync(abs || path.join(DOCS, file), "utf8");
  // Promote: file's H1 becomes numbered H1; its H2 → H2.
  const body = md.replace(/^# (.*)$/m, `# ${idx + 1}. $1`);
  children.push(...convert(body, 0));
  children.push(new Paragraph({ children: [new PageBreak()] }));
});

// Appendix: screenshots
const CAPTIONS = {
  onboarding: "Знакомство", create_pet: "Создание питомца", home: "Главный экран, неделя 1", plan: "План на неделю",
  tasks: "Задания", shop: "Магазин", not_enough: "Нехватка монет: объяснение и варианты", savings: "Копилка и цель",
  withdraw_preview: "Предпросмотр снятия из копилки", summary: "Итог недели", home_week2: "Неделя 2 после итога",
  pet_grown: "Неделя 4: стадия «Подросток»", progress: "Прогресс и журнал монет", parent: "Раздел для взрослого",
  landscape_rail: "Альбомная ориентация: навигационный рейл", landscape_shop: "Альбомная ориентация: магазин", home_dark: "Тёмная тема",
};
children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: `${SECTIONS.length + 1}. Приложение. Экраны прототипа`, font: FONT })] }));
const shotsDir = path.join(ROOT, "screenshots", "store");
if (fs.existsSync(shotsDir)) {
  const files = fs.readdirSync(shotsDir).filter((f) => f.endsWith(".png")).sort();
  for (let k = 0; k < files.length; k += 2) {
    const pair = files.slice(k, k + 2);
    children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: pair.flatMap((f) => [image(path.join(shotsDir, f), 200), new TextRun({ text: "    " })]) }));
    children.push(new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: pair.map((f) => CAPTIONS[f.replace(/^\d+_/, "").replace(".png", "")] || f).join("        "), font: FONT, size: 18, color: "666666" })] }));
  }
}

const doc = new Document({
  creator: "Finny team",
  title: "Питомец Финни — документация",
  styles: {
    default: { document: { run: { font: FONT, size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 36, bold: true, color: PURPLE, font: FONT }, paragraph: { spacing: { before: 360, after: 200 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 28, bold: true, color: "2B2B2B", font: FONT }, paragraph: { spacing: { before: 280, after: 140 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 24, bold: true, font: FONT }, paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 2 } },
      { id: "Heading4", name: "Heading 4", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 22, bold: true, italics: true, font: FONT }, paragraph: { spacing: { before: 160, after: 80 }, outlineLevel: 3 } },
    ],
  },
  numbering: {
    config: [
      { reference: "bullets", levels: [0, 1, 2].map((l) => ({ level: l, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720 + 360 * l, hanging: 360 } } } })) },
      { reference: "numbers", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ],
  },
  sections: [{
    properties: { page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } },
    children,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(OUT, buf);
  console.log("written", OUT, buf.length, "bytes");
});

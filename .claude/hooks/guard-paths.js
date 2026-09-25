#!/usr/bin/env node
/**
 * PreToolUse guard для подагента `coder`.
 *
 * Contract: JSON приходит в stdin, exit 2 блокирует операцию, stderr уходит агенту.
 * Node выбран вместо bash+jq намеренно: jq может отсутствовать, а любой отказ
 * парсера в bash-версии приводил к fail-open, то есть к молчаливому пропуску всего.
 * Здесь любая непонятная ситуация = блокировка.
 *
 * Источник истины — .claude/task-scope.json, который оркестратор пишет ПЕРЕД
 * спавном кодера:
 *   {
 *     "allow":   ["src/rate_limiter.py", "src/config.py"],
 *     "protect": ["tests/", "ci/", ".github/", "pyproject.toml", "Makefile"]
 *   }
 */

const fs = require("fs");
const path = require("path");

// Корень репозитория берём от самого хука, а не от cwd: подагент может сделать
// `cd finny-pet`, и относительный путь к скоупу и сверка allow поехали бы
// (найдено на Windows 2026-09-25, сессия жила в finny-pet/).
const ROOT = path.resolve(__dirname, "..", "..");
const SCOPE_FILE = path.join(ROOT, ".claude", "task-scope.json");
// NTFS и APFS по умолчанию не различают регистр: `App/Src/Test` — тот же файл.
const FOLD = (s) => s.replace(/\\/g, "/").toLowerCase();

function deny(reason) {
  process.stderr.write(
    `BLOCKED by task scope: ${reason}\n` +
      `Обходить запрет нельзя. Если задача нерешаема в этих границах — верни STATUS: BLOCKED.\n`,
  );
  process.exit(2);
}

function readStdin() {
  try {
    return fs.readFileSync(0, "utf8");
  } catch {
    deny("не удалось прочитать вход хука");
  }
}

// --- нормализация пути к repo-relative ------------------------------------
function rel(p) {
  const abs = path.resolve(input.cwd || process.cwd(), p);
  const r = path.relative(ROOT, abs);
  return r.split(path.sep).join("/");
}

function matches(target, entry) {
  const t = FOLD(target);
  const e = FOLD(entry).replace(/\/+$/, "");
  return t === e || t.startsWith(e + "/");
}

// --- основной поток --------------------------------------------------------
const raw = readStdin();

let input;
try {
  input = JSON.parse(raw);
} catch {
  deny("вход хука не является валидным JSON");
}

// С 2026-08-21 хук объявлен в .claude/settings.json (PreToolUse из frontmatter
// агента в CC 2.1.235 молча не вызывается) и потому дёргается на КАЖДОМ вызове
// сессии. Ограничения — только для кодера: подагент несёт "agent_type" в stdin
// (проверено дампером), у главной сессии поля нет. Все, кроме кодера, — выход.
// Цена fail-open: если поле переименуют, кодер молча освободится — ловится
// обязательным негативным зондом после обновлений CC (WORKFLOW «Известные
// ограничения»).
if ((input.agent_type ?? "") !== "coder") {
  process.exit(0);
}

if (!fs.existsSync(SCOPE_FILE)) {
  deny(`.claude/task-scope.json отсутствует — оркестратор не зафиксировал скоуп задачи`);
}

let scope;
try {
  scope = JSON.parse(fs.readFileSync(SCOPE_FILE, "utf8"));
} catch {
  deny(`.claude/task-scope.json повреждён`);
}

const allow = Array.isArray(scope.allow) ? scope.allow : null;
const protect = Array.isArray(scope.protect) ? scope.protect : [];
if (!allow) deny(`.claude/task-scope.json: поле "allow" обязано быть массивом`);

const tool = input.tool_name || "";
const ti = input.tool_input || {};

if (["Edit", "Write", "NotebookEdit"].includes(tool)) {
  const fp = ti.file_path || ti.notebook_path;
  if (!fp) deny(`${tool} без file_path — проверить скоуп невозможно`);

  const target = rel(fp);
  if (target.startsWith("../")) deny(`${fp} находится вне репозитория`);
  if (!allow.some((a) => matches(target, a))) {
    deny(`${target} не входит в allow текущей задачи`);
  }
}

if (tool === "Bash" || tool === "PowerShell") {
  // Windows: `app\\src\\test`, `App/Src/Test` и `/usr/bin/git` — те же пути и тот же
  // git. Сверяем по свёрнутой строке: обратные слэши → прямые, регистр — нижний.
  const cmd = FOLD(String(ti.command || ""));

  // Мутации git-истории и прав — прерогатива оркестратора. `[^;&|]*` покрывает флаги
  // между `git` и субкомандой (`git -C . checkout` — реальный обход, GEM-T01 2026-08-21).
  // Fail-closed: слово-субкоманда в аргументах (`git log --grep checkout`) тоже блок.
  const gitMut =
    /(^|[;&|(\s\/"'])git(\.exe)?\b[^;&|]*\b(commit|reset|checkout|restore|stash|rebase|push|clean|update-index)\b/;
  if (gitMut.test(cmd)) deny("операции с git-историей выполняет оркестратор, не кодер");

  // Попытка отредактировать защищённый путь в обход Edit/Write.
  // PowerShell-командлеты и алиасы cmd — тоже запись (Windows, 2026-09-25).
  const writeVerb =
    /(>>?|(^|[;&|(\s])(sed\s+-i|perl\s+-i|rm|mv|cp|tee|truncate|touch|install|patch|dd|del|erase|ren|rename|move|copy|xcopy|robocopy|rmdir|rd|set-content|add-content|clear-content|out-file|remove-item|move-item|copy-item|new-item|rename-item|ri|ni|mi|cpi|sc|ac)(\s|$))/;

  // Inline-eval интерпретатора: python -c "open('tests/..','w')", node -e, и т.п.
  // Плюс Windows-обёртки: `py -c`, `powershell -Command`, `cmd /c` (в Git Bash — `cmd //c`).
  const inlineEval =
    /(^|[;&|(\s\/])((python3?|py|node|perl|ruby|php)(\.exe)?\s+(-c|-e|-p)|(powershell|pwsh)(\.exe)?\s[\s\S]*?-(c|command|encodedcommand|e|ec)|cmd(\.exe)?\s+\/\/?c)(\s|$)/;

  // Контейнер с bind-mount репозитория пишет в хостовую ФС мимо всех проверок.
  const containerMount =
    /(^|[;&|(\s])(docker|podman|nerdctl)\b[\s\S]*?(-v\s|--volume[= ]|--mount[= ])/;

  // Проект может лежать в подкаталоге (здесь — finny-pet/), и кодер пишет
  // `cd finny-pet && sed -i ... app/src/test/X.kt`: полного пути из protect в
  // команде нет. Поэтому сверяем ещё и хвосты пути длиной от двух сегментов
  // (`app/src/test/`, `src/test/`). Однсегментные хвосты (`test/`, `gradle/`) не
  // берём — слишком много ложных срабатываний. Найдено живым зондом 2026-09-22.
  function variants(entry) {
    const trail = entry.endsWith("/") ? "/" : "";
    const segs = entry.replace(/\/+$/, "").split("/").filter(Boolean);
    const out = [entry];
    for (let i = 1; i <= segs.length - 2; i++) out.push(segs.slice(i).join("/") + trail);
    return out;
  }
  const touchesProtected = protect.some((pr) => variants(FOLD(pr)).some((v) => cmd.includes(v)));

  if (touchesProtected) {
    if (writeVerb.test(cmd)) deny(`shell-запись в защищённый путь`);
    if (inlineEval.test(cmd))
      deny(`inline-eval интерпретатора с обращением к защищённому пути`);
    if (containerMount.test(cmd))
      deny(`контейнер с bind-mount и обращением к защищённому пути`);
  }

  // Отключение самого контура.
  if (/\.claude\//.test(cmd) && (writeVerb.test(cmd) || inlineEval.test(cmd))) {
    deny("модификация конфигурации контура запрещена");
  }
}

process.exit(0);

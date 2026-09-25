#!/usr/bin/env node
/**
 * SubagentStop-хук для `coder`. Последний рубеж: даже если PreToolUse-guard
 * был обойдён неучтённым способом, здесь мы механически сверяем, что в рабочем
 * дереве нет незакоммиченных изменений защищённых путей (диff от HEAD).
 *
 * Это не заменяет ревью — это делает один конкретный вид халтуры невозможным
 * незаметно, независимо от того, что напишет кодер в своём отчёте.
 *
 * Список protect берётся из .claude/task-scope.json, который оркестратор
 * пишет перед стартом задачи. Поле "base" хук больше не читает (2026-08-21) —
 * оно осталось источником $BASE для ACCEPTANCE-диффов спеки.
 */

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

// Корень — от расположения хука, не от cwd подагента (он мог сделать `cd finny-pet`).
const ROOT = path.resolve(__dirname, "..", "..");
const SCOPE_FILE = path.join(ROOT, ".claude", "task-scope.json");

function fail(msg) {
  process.stderr.write(`ORACLE INTEGRITY FAILED: ${msg}\n`);
  process.exit(2);
}

if (!fs.existsSync(SCOPE_FILE)) fail(`${SCOPE_FILE} отсутствует`);

let scope;
try {
  scope = JSON.parse(fs.readFileSync(SCOPE_FILE, "utf8"));
} catch {
  fail(`${SCOPE_FILE} повреждён`);
}

const protect = Array.isArray(scope.protect) ? scope.protect : [];
if (protect.length === 0) process.exit(0);

// Сверяем protect с HEAD, а не с base из task-scope (решение сеньора 2026-08-21).
// Кодер не коммитит — его правки всегда незакоммичены. `git status` вместо
// `git diff`: diff не видит новых (untracked) файлов в protect (найдено 2026-09-25).
// Сверка с base давала вечно-красный стоп осиротевшим сессиям кодера после
// коммитов оркестратора в protect-пути (base устаревал), а текст «откатите»
// провоцировал кодера сносить незакоммиченные файлы оркестратора.
// ЗАКОММИЧЕННУЮ подмену оракула ловит не этот хук, а ACCEPTANCE у ревьювера и
// оркестратора: git diff --name-only $BASE -- "*.test.ts" -> пусто.
let changed;
try {
  changed = execFileSync(
    "git",
    ["status", "--porcelain", "--untracked-files=all", "--", ...protect],
    { encoding: "utf8", cwd: ROOT },
  )
    .split("\n")
    .filter(Boolean);
} catch (e) {
  fail(`не удалось выполнить git status: ${e.message}`);
}

if (changed.length > 0) {
  fail(
    `в рабочем дереве изменены защищённые файлы:\n  ${changed.join("\n  ")}\n` +
      `НИЧЕГО НЕ ОТКАТЫВАЙ и не правь эти файлы. Верни STATUS: BLOCKED, ` +
      `перечисли эти файлы в отчёте и остановись — разбирается оркестратор.`,
  );
}

process.exit(0);

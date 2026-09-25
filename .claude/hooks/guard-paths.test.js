#!/usr/bin/env node
/**
 * Тест guard-paths.js. Запуск из корня репозитория:  node .claude/hooks/guard-paths.test.js
 *
 * Создаёт временную песочницу со своим task-scope.json, поэтому не зависит от
 * текущей задачи и ничего не портит. Прогоняйте после каждого обновления
 * Claude Code и после любой правки хука: молча сломавшийся guard хуже, чем
 * его отсутствие, потому что создаёт ложное чувство защиты.
 */

const { execFileSync } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");

const sandbox = fs.mkdtempSync(path.join(os.tmpdir(), "guard-test-"));
fs.mkdirSync(path.join(sandbox, ".claude", "hooks"), { recursive: true });
// Хук ищет скоуп от своего расположения (корень = ../..), поэтому гоняем копию из песочницы.
const HOOK = path.join(sandbox, ".claude", "hooks", "guard-paths.js");
fs.copyFileSync(path.resolve(__dirname, "guard-paths.js"), HOOK);
fs.writeFileSync(
  path.join(sandbox, ".claude/task-scope.json"),
  JSON.stringify({
    base: "HEAD",
    allow: ["src/limiter.py", "src/index.js"],
    protect: ["tests/", "pyproject.toml", "package.json", "docker-compose.yml", "Dockerfile",
              "finny-pet/app/src/test/", "finny-pet/app/build.gradle.kts"],
  }),
);

const cases = [
  // --- гейт по агенту: хук живёт в settings.json и дёргается у ВСЕХ; ограничения —
  // только для agent_type === "coder" (main-сессия поля не несёт, проверено дампером) ---
  ["PASS",  "main-сессия (без agent_type): git commit свободен", { tool_name: "Bash", tool_input: { command: "git commit -am wip" } }],
  ["PASS",  "main-сессия: Edit protect-файла свободен", { tool_name: "Edit", tool_input: { file_path: "tests/t.py" } }],
  ["PASS",  "другой агент (reviewer): чтение и команды свободны", { agent_type: "reviewer", tool_name: "Bash", tool_input: { command: "git checkout -- tests/" } }],

  // --- запись в защищённое (от кодера): должно блокироваться ---
  ["BLOCK", "Edit теста напрямую",            { agent_type: "coder", tool_name: "Edit", tool_input: { file_path: "tests/t.py" } }],
  ["BLOCK", "Write вне whitelist",            { agent_type: "coder", tool_name: "Write", tool_input: { file_path: "src/other.py" } }],
  ["BLOCK", "выход за пределы репозитория",   { agent_type: "coder", tool_name: "Edit", tool_input: { file_path: "../../etc/hosts" } }],
  ["BLOCK", "sed -i по тесту",                bash(`sed -i s/a/b/ tests/t.py`)],
  ["BLOCK", "перезапись через >",             bash(`echo pass > tests/t.py`)],
  ["BLOCK", "python -c запись в тест",        bash(`python3 -c "open('tests/t.py','w').write('')"`)],
  ["BLOCK", "node -e запись в package.json",  bash(`node -e "require('fs').writeFileSync('package.json','{}')"`)],
  ["BLOCK", "docker bind-mount + sed",        bash(`docker run --rm -v "$PWD":/w -w /w alpine sed -i s/a/b/ tests/t.py`)],
  ["BLOCK", "docker bind-mount + python -c",  bash(`docker run --rm -v $PWD:/w python:3.12 python -c "open('/w/tests/t.py','w')"`)],
  ["BLOCK", "перезапись Dockerfile",          bash(`echo FROM scratch > Dockerfile`)],
  ["BLOCK", "git commit",                     bash(`git commit -am wip`)],
  ["BLOCK", "git checkout",                   bash(`git checkout -- tests/`)],
  ["BLOCK", "git -C . checkout (обход через флаг, GEM-T01)", bash(`git -C . checkout abc123 -- .claude/launch.json`)],
  ["BLOCK", "git --no-pager restore",         bash(`git --no-pager restore tests/`)],
  ["BLOCK", "git -c key=val push",            bash(`git -c user.name=x push origin main`)],
  ["BLOCK", "fail-closed: субкоманда в аргументах", bash(`git log --grep checkout`)],
  ["BLOCK", "git checkout второй командой",   bash(`git status && git -C . checkout -- tests/`)],
  // --- проект в подкаталоге: кодер делает cd и пишет относительный путь (2026-09-22) ---
  ["BLOCK", "cd в подпроект + sed -i по тесту",   bash(`cd finny-pet && sed -i s/a/b/ app/src/test/java/X.kt`)],
  ["BLOCK", "cd в подпроект + > в build.gradle", bash(`cd finny-pet && echo x > app/build.gradle.kts`)],
  ["BLOCK", "cd глубже + rm теста",               bash(`cd finny-pet/app && rm src/test/java/X.kt`)],
  ["PASS",  "cd в подпроект + gradlew test",      bash(`cd finny-pet && ./gradlew testClassicDebugUnitTest --console=plain`)],
  ["PASS",  "gradlew с выводом в /tmp",            bash(`cd finny-pet && ./gradlew assembleClassicDebug > /tmp/b.log 2>&1`)],
  ["PASS",  "чтение теста из подпроекта",         bash(`cd finny-pet && cat app/src/test/java/X.kt`)],
  ["BLOCK", "снос самого хука",               bash(`node -e "require('fs').unlinkSync('.claude/hooks/guard-paths.js')"`)],

  // --- Windows (2026-09-25): обратные слэши, регистр, PowerShell, cmd, py ---
  ["BLOCK", "sed -i по пути с обратными слэшами",  bash(`cd finny-pet && sed -i s/a/b/ 'app\\src\\test\\X.kt'`)],
  ["BLOCK", "rm по пути в другом регистре",        bash(`cd finny-pet && rm App/Src/Test/Y.kt`)],
  ["BLOCK", "powershell -c Set-Content в тест",    bash(`powershell -c "Set-Content tests/t.py x"`)],
  ["BLOCK", "cmd //c del теста",                   bash(`cmd //c del tests\\t.py`)],
  ["BLOCK", "py -c запись в тест",                 bash(`py -c "open('tests/t.py','w')"`)],
  ["BLOCK", "git по абсолютному пути",             bash(`/usr/bin/git checkout -- tests/`)],
  ["BLOCK", "PowerShell: Remove-Item теста",       ps(`Remove-Item finny-pet\\app\\src\\test\\X.kt`)],
  ["BLOCK", "PowerShell: Out-File в build.gradle", ps(`"x" | Out-File finny-pet/app/build.gradle.kts`)],
  ["BLOCK", "PowerShell: git commit",              ps(`git commit -am wip`)],
  ["BLOCK", "Edit по абсолютному Windows-пути в тест", { agent_type: "coder", tool_name: "Edit", tool_input: { file_path: path.join(sandbox, "tests", "t.py") } }],
  ["BLOCK", "Edit относительно cwd подпроекта",    { agent_type: "coder", cwd: path.join(sandbox, "finny-pet"), tool_name: "Edit", tool_input: { file_path: "app/src/test/X.kt" } }],
  ["PASS",  "Edit разрешённого по абсолютному пути в другом регистре", { agent_type: "coder", tool_name: "Edit", tool_input: { file_path: path.join(sandbox, "SRC", "Limiter.py") } }],
  ["PASS",  "PowerShell: gradlew.bat test",        ps(`cd finny-pet; .\\gradlew.bat testGameDebugUnitTest --console=plain`)],
  ["PASS",  "PowerShell: чтение теста",            ps(`Get-Content finny-pet\\app\\src\\test\\X.kt`)],

  // --- легальная работа: не должно ломаться ---
  ["PASS",  "Edit разрешённого файла",        { agent_type: "coder", tool_name: "Edit", tool_input: { file_path: "src/limiter.py" } }],
  ["PASS",  "локальный pytest",               bash(`pytest tests/ -q`)],
  ["PASS",  "pytest в контейнере",            bash(`docker compose run --rm --no-deps app pytest -q`)],
  ["PASS",  "npm test в контейнере",          bash(`docker compose run --rm app npm test`)],
  ["PASS",  "docker build",                   bash(`docker build -t app:test .`)],
  ["PASS",  "docker logs",                    bash(`docker compose logs app --tail 50`)],
  ["PASS",  "mypy",                           bash(`mypy src --strict`)],
  ["PASS",  "ruff",                           bash(`ruff check src`)],
  ["PASS",  "чтение теста",                   bash(`cat tests/t.py`)],
  ["PASS",  "git diff (чтение)",              bash(`git diff --stat`)],
  ["PASS",  "git log (чтение)",               bash(`git log --oneline -5`)],
  ["PASS",  "git show (чтение)",              bash(`git show HEAD --stat`)],
];

function ps(command) {
  return { agent_type: "coder", tool_name: "PowerShell", tool_input: { command } };
}

function bash(command) {
  // Кейсы контура пишутся от лица кодера: у настоящего подагента stdin несёт agent_type.
  return { agent_type: "coder", tool_name: "Bash", tool_input: { command } };
}

let failed = 0;
for (const [want, name, payload] of cases) {
  let code = 0;
  try {
    execFileSync("node", [HOOK], {
      input: JSON.stringify(payload),
      cwd: sandbox,
      stdio: ["pipe", "pipe", "pipe"],
    });
  } catch (e) {
    code = e.status;
  }
  const got = code === 2 ? "BLOCK" : "PASS";
  if (got !== want) failed++;
  console.log(`${got === want ? "  ok  " : "FAIL  "}${want.padEnd(6)}${name}`);
}

// fail-closed: без файла скоупа guard обязан блокировать, а не пропускать
fs.unlinkSync(path.join(sandbox, ".claude/task-scope.json"));
let code = 0;
try {
  execFileSync("node", [HOOK], {
    input: JSON.stringify({ agent_type: "coder", tool_name: "Edit", tool_input: { file_path: "src/limiter.py" } }),
    cwd: sandbox,
    stdio: ["pipe", "pipe", "pipe"],
  });
} catch (e) {
  code = e.status;
}
if (code !== 2) failed++;
console.log(`${code === 2 ? "  ok  " : "FAIL  "}BLOCK fail-closed без task-scope.json`);

fs.rmSync(sandbox, { recursive: true, force: true });

if (failed > 0) {
  console.log(`\nПРОВАЛЕНО: ${failed}. Guard не защищает — не запускайте кодера.`);
  process.exit(1);
}
console.log("\nВсе кейсы пройдены. Guard рабочий.");

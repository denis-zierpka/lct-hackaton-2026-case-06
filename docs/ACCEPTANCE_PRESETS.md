# Пресеты ACCEPTANCE — Kotlin / Gradle / Android

Копируйте блок в секцию ACCEPTANCE шаблона задачи. Требование к любой команде одно:
она детерминирована и её может запустить ревьювер, не зная ничего о том, что делал кодер.

Все команды — из каталога `finny-pet/`. Окружение — `CLAUDE.md`, раздел «Окружение».
На Windows `./gradlew` → `.\gradlew.bat` (PowerShell) и `python3` → `python`;
`JAVA_HOME` не переопределять — он уже указывает на JDK 17.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=~/android-sdk   # только Linux
```

---

## Исходное состояние (замер 2026-09-22, коммит 83404be)

Перед тем как писать «→ exit 0» в приёмку, надо знать, что уже красное на чистом
коммите. Иначе кодер получает нерешаемую задачу.

| Проверка | Результат на 83404be |
| --- | --- |
| `testClassicDebugUnitTest` | 34 теста, 0 упало |
| `testGameDebugUnitTest` | 34 теста, 0 упало (тот же набор из `src/test/`) |
| `assembleClassicDebug`, `assembleGameDebug` | exit 0 |
| `lintClassicDebug` | 0 ошибок, 9 предупреждений |
| `lintGameDebug` | 0 ошибок, 10 предупреждений, 2 подсказки |
| предупреждения компилятора | 1: `GameApp.kt:105`, устаревший `LocalLifecycleOwner` |

Повторный замер на Windows 2026-09-25 (коммит 209ff3d): тесты 34 / 34, 0 упало;
сборка обоих вариантов — exit 0; lint classic — 0 ошибок, 8 предупреждений;
game — 0 ошибок, 9 предупреждений, 2 подсказки; предупреждение компилятора то же.
Предупреждений на одно меньше из-за `GradleDependency`: это правило сверяет версии
с сетью, и его число плавает со временем. Так же ведёт себя `AndroidGradlePluginVersion`
(«доступна новая версия Gradle»): 2026-09-25 он появился посреди прогона MVP-T01 без
единой правки gradle-файлов. Поэтому потолок считается **без сетевых проверок версий**
(`GradleDependency`, `AndroidGradlePluginVersion`, `NewerVersionAvailable`): на базе
**5 / 6** (classic / game), задача не может его увеличить.

Предупреждения линта — **потолок, а не цель**: задача не может их увеличить.
Починка исходных предупреждений — отдельная задача, не «заодно».

## Основной блок (логика в `domain/` и `data/`)

```
1. ./gradlew testClassicDebugUnitTest --console=plain         -> exit 0
2. число тестов в app/build/test-results/testClassicDebugUnitTest/*.xml
   = <было> + <добавлено оракулом>; skipped = 0
3. ./gradlew assembleClassicDebug assembleGameDebug          -> exit 0
4. ./gradlew lintClassicDebug lintGameDebug                  -> exit 0,
   ошибок 0, предупреждений без сетевых проверок версий не больше 5 / 6:
   grep ': Warning:' app/build/intermediates/lint_intermediate_text_report/<variant>/*/lint-results-<variant>.txt
     | grep -vcE '\[(GradleDependency|AndroidGradlePluginVersion|NewerVersionAvailable)\]'
5. git diff --name-only <BASE> -- app/src/test/              -> пусто
6. git diff --name-only <BASE> -- '*.gradle.kts' gradle/ gradle.properties -> пусто
7. git diff --shortstat <BASE>                               -> не более <N> строк
8. grep -rnE '^import (android|androidx)' app/src/main/java/ru/finny/pet/domain/ -> пусто
```

Пункт 2 обязателен: `exit 0` при нуле исполненных тестов — тоже `exit 0`.
Подсчёт — скриптом по XML, а не по выводу Gradle:

```bash
python3 - <<'EOF'        # на Windows: python
import glob, xml.etree.ElementTree as ET
t=f=s=0
for p in glob.glob("app/build/test-results/testClassicDebugUnitTest/*.xml"):
    r=ET.parse(p).getroot(); t+=int(r.get("tests")); f+=int(r.get("failures"))+int(r.get("errors")); s+=int(r.get("skipped"))
print(f"tests={t} failed={f} skipped={s}")
EOF
```

Пункт 6 отдельно от пункта 5: незаметно добавленная зависимость или ослабленная
настройка компилятора — самый частый способ «решить» задачу, не решая.

## Задача, затрагивающая UI варианта

Добавить к основному блоку:

```
9.  ./gradlew test<Вариант>DebugUnitTest                     -> exit 0
10. чек-лист живой проверки для человека: экран, действие, что должно быть видно
```

Пункт 10 оркестратор выполняет сам на эмуляторе (AVD `finni`, портрет 411 и 360 dp)
и прикладывает скриншоты «до/после»; плавность и физическое устройство — за человеком.

## Задача, затрагивающая контент (`content.json`)

```
1. ./gradlew testClassicDebugUnitTest                        -> exit 0 (ContentTest читает реальный файл)
2. python3 -c "import json;json.load(open('app/src/main/assets/content/content.json'))" -> exit 0
3. git diff --name-only <BASE> -- app/src/main/java/         -> пусто (контент без правки кода, ТЗ 2.5.14)
```

## Релизная сборка

Подпись требует `keystore.properties` и ключа вне репозитория — у кодера их нет и
не должно быть. Релиз собирает **оркестратор или сеньор**, не агент:

```
./gradlew assembleClassicRelease                             -> exit 0
apksigner verify --print-certs app/build/outputs/apk/classic/release/*.apk -> exit 0
aapt2 dump badging <apk> | grep uses-permission               -> только согласованные
```

## Чего в ACCEPTANCE быть не должно

| Команда | Почему плохо |
| --- | --- |
| `./gradlew test` без варианта | гоняет оба варианта, упавший не виден в сводке |
| `./gradlew build` как единственный пункт | непрозрачно, какая проверка упала |
| exit 0 без подсчёта тестов | ноль исполненных тестов тоже даёт 0 |
| `--continue` | Gradle продолжит после падения и может вернуть не тот код |
| «проверить, что экран работает» | не команда, а просьба; для этого пункт 10 и человек |
| любая команда с `|| true` | exit code перестаёт что-либо значить |

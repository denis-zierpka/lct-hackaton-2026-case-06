# Передача между сессиями

Правило (решение 2026-09-25): раз в 1–2 эпика сессия останавливается, обновляет этот файл,
готовит спеку следующей задачи в `docs/tasks/`, коммитит и пушит — работа продолжается в новой
сессии. Новая сессия читает этот файл первым.

## Где мы (2026-09-26, конец сессии 2)

- **Пакет промежуточной сдачи ТЗ 7.1 готов** (срок 2026-09-29): версия 1.3.0 (versionCode 4),
  вариант `game`, пакет `ru.finny.pet`, «Питомец Финни». Тег `v1.3.0`, ветка `feat/mvp-7-1`
  влита в `main`, всё запушено.
- Файл сдачи — [`finny-pet/release/finny-pet-1.3.0-release.apk`](../finny-pet/release/finny-pet-1.3.0-release.apk)
  (3,9 МБ, подпись v2 debug-ключом, SHA-256 `44fe9ea3…6ffef5`), установлен и запущен на
  Samsung S23 (холодный старт 0,21–0,39 с).
- Проверки на HEAD (чистая сборка): 117/117 JVM-тестов в обоих вариантах, сборки и lint exit 0,
  0 ошибок, предупреждений без сетевых проверок 5 / 6.
- Состав 7.1 по пунктам ТЗ — таблица в [`README.md`](../README.md); статусы требований —
  [`REQUIREMENTS_MATRIX.md`](../finny-pet/docs/REQUIREMENTS_MATRIX.md).

## Сделано (сессия 2)

| Задача | Что | Коммиты |
|---|---|---|
| MVP-T12 | 15 находок ревью UI + спорная: окна взрослого по центру, модальность и «Назад» у окон, пузырь в альбоме, «Сейчас» без тупика «В копилку», TalkBack, тумблер анимаций, барьер, `matchOver`, питомец из старого сохранения; три раунда независимого ревью, эмулятор и S23; доки сверены | b70afb3 |
| Релиз 7.1 | APK сдачи, apksigner/aapt2, тег `v1.3.0`, merge в `main` | 1d06250 |
| Инструменты | `tools/adbui.sh shot` на Windows (путь для Python) | 1fc438a |

Статусы находок и что осталось по UI — [`MVP-REVIEW-UI.md`](tasks/MVP-REVIEW-UI.md), раздел
«Статус после MVP-T12».

## Дальше

Эпика с готовой спекой нет: следующий шаг выбирает сеньор. Кандидаты к финалу (ТЗ 7.2) —
[`BACKLOG.md`](BACKLOG.md), «Осталось»:

1. Материалы сдачи для `game`: DOCX, PPTX, видео, скриншоты карточки RuStore, `tools/demo_run.sh`
   (п. 3–4) — генераторы пока под `classic`.
2. Постоянный ключ подписи (п. 2) — ключ `CN=Finny Pet` у Дениса; APK с другим ключом не встанет
   поверх 1.3.0 без удаления.
3. Комната в компактном альбоме (п. 10), полный прогон TalkBack (п. 11).
4. 3D-питомец (п. 5, `docs/ART_PIPELINE.md`), задания-действия (п. 6, вопрос заказчику № 1).

Решения, которые ждут людей: ключ подписи, доступ экспертов к репозиторию (владелец — Денис),
отправка [`QUESTIONS.md`](../finny-pet/docs/QUESTIONS.md) заказчику, проверка со взрослыми
([`USER_TESTING.md`](../finny-pet/docs/USER_TESTING.md)), звук и плавность на телефоне — на слух и
на глаз, загрузка пакета 7.1 на конкурсную платформу.

## Окружение (Windows 10, эта машина)

- JDK 17 Adoptium (`JAVA_HOME` задан), Android SDK — `%LOCALAPPDATA%\Android\Sdk`
  (platform 36, build-tools 36.0.0, emulator, образ android-36.1 google_apis x86_64),
  `finny-pet/local.properties` с `sdk.dir=C\:/Users/Singularity/AppData/Local/Android/Sdk`.
- Gradle: из `finny-pet/` — `./gradlew` в Git Bash или `.\gradlew.bat` в PowerShell.
  Свободной памяти мало (≈ 1–2 ГБ при запущенном эмуляторе): кодеров с Gradle — по одному,
  не параллельно.
- Эмулятор: AVD `finni` (WHPX), запуск
  `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd finni -no-snapshot-save -no-boot-anim -no-audio &`.
  Альбом: `settings put system accelerometer_rotation 0` + `user_rotation 1`; после — `user_rotation 0`,
  `accelerometer_rotation 1`. Состояние игры на debug-сборке правится через
  `run-as ru.finny.pet` (`files/state.json`) — сделать копию и вернуть после.
- Телефон команды: Samsung Galaxy S23 (SM-S911B), Android 14, adb serial `RZCX923ZP4L`, 360 dp.
  «Защищённая папка» → `pm`/`am`/`install` с `--user 0`. **Тесты — без звука**: `tools/adbui.sh mute`
  печатает текущую громкость — запомнить её и вернуть `unmute <N>` (она меняется: 9, потом 7).
  На телефоне release-сборка — `run-as` не работает. Ориентация телефона — `user_rotation`
  (по умолчанию 0, автоповорот выключен).
- Управление экраном: [`tools/adbui.sh`](../tools/adbui.sh) (`ANDROID_SERIAL=emulator-5554` или
  `RZCX923ZP4L`): `ui`, `tap "Текст"`, `shot`, `launch`, `wm360`, `font 1.3`, `mute`/`unmute`.
- Роли контура `test-author`, `coder`, `reviewer` зарегистрированы как типы агентов — спавнить
  через Agent/Workflow с `agentType`/`subagent_type`, хук guard их видит. Whitelist —
  `.claude/task-scope.json` (сейчас — MVP-T12), переписать перед спавном кодера.
- Workflow-скрипты: `${…}` внутри шаблонных строк промпта — подстановка JS; в тексте для
  агента экранировать `\${…}`.

## Стартовый промпт для новой сессии

> Продолжаем проект «Питомец Финни». Прочитай `docs/HANDOFF.md` и `CLAUDE.md`. Пакет 7.1 сдан в
> `main` (тег `v1.3.0`). Предложи план следующего эпика из раздела «Дальше» и жди отмашки.

# Сборка, установка и демонстрация

Инструкция по ТЗ 5 п. 2 (окружение и сборка релизного APK), 7.1 п. 3 (рабочая сборка) и 7.2
(установка, запуск, демо-режим, сброс профиля, тестовые данные). Сценарий показа — по
Приложению А и ТЗ 4.1.

## Что сдаём

| Вариант | Пакет | Название | Версия | В сдаче |
|---|---|---|---|---|
| `game` | `ru.finny.pet` | «Питомец Финни» | 1.3.0 (versionCode 4) | да: [`release/finny-pet-1.3.0-release.apk`](../release/finny-pet-1.3.0-release.apk) |
| `classic` | `ru.finny.pet.classic` | «Финни classic» | 1.3.0-classic (versionCode 4) | нет, остаётся в репозитории |

Оба варианта собираются из одного модуля ([app/build.gradle.kts](../app/build.gradle.kts), `productFlavors`),
у них разные пакеты, поэтому на одном устройстве они ставятся рядом.

## Окружение

| Инструмент | Версия | Откуда |
|---|---|---|
| JDK | 17 (на Windows проверено с Eclipse Adoptium 17.0.18) | установить отдельно |
| Android SDK | `platforms;android-36`, `build-tools;36.0.0`, `platform-tools` | `sdkmanager` из Android command-line tools |
| Gradle | 9.4.1 | скачивает `gradlew`; архив сверяется по `distributionSha256Sum` в [gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties) |
| Android Gradle Plugin | 9.2.1 | [build.gradle.kts](../build.gradle.kts) |
| Kotlin | 2.4.20 | там же |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 (Android 8.0) | [app/build.gradle.kts](../app/build.gradle.kts) |

Android Studio не нужна. Первой сборке нужен интернет (Gradle и зависимости); самому приложению
интернет не нужен.

Установка пакетов SDK (Windows — `sdkmanager.bat`):

```
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

Путь к SDK — в `finny-pet/local.properties` (файл в `.gitignore`) или в переменной `ANDROID_HOME`.

| | Windows (PowerShell) | Linux (bash) |
|---|---|---|
| JDK | `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17…"` | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` |
| SDK | `%LOCALAPPDATA%\Android\Sdk` | `~/android-sdk` |
| `local.properties` | `sdk.dir=C\:/Users/<user>/AppData/Local/Android/Sdk` | `sdk.dir=/home/<user>/android-sdk` |
| Gradle | `.\gradlew.bat` | `./gradlew` |
| Python (генераторы ассетов) | `python` | `python3` |

На Windows двоеточие после буквы диска в `local.properties` экранируется (`C\:`), слэши прямые;
без экранирования lint выдаёт ошибку `PropertyEscape`. `adb`, `apksigner`, `aapt2` лежат в
`<SDK>/platform-tools` и `<SDK>/build-tools/36.0.0` — удобно добавить их в `PATH`.

## Тесты и сборка

Все команды — из каталога `finny-pet/`. Ниже для Windows; на Linux то же с `./gradlew`.

| Что | Команда | Результат |
|---|---|---|
| Тесты `game` | `.\gradlew.bat :app:testGameDebugUnitTest` | 117 JVM-тестов, 0 падений; отчёт `app/build/reports/tests/testGameDebugUnitTest/index.html` |
| Тесты `classic` | `.\gradlew.bat :app:testClassicDebugUnitTest` | те же 117 тестов, 0 падений |
| Lint `game` | `.\gradlew.bat :app:lintGameDebug` | 0 ошибок, 10 предупреждений и 2 подсказки; у `classic` (`lintClassicDebug`) — 0 ошибок, 9 предупреждений. Из них 4 — сетевые проверки новых версий (`GradleDependency` ×3, `AndroidGradlePluginVersion`), остальных 6 и 5. Команда их не отключает (`lint.xml` и блока `lint` нет); отчёт — `app/build/reports/lint-results-gameDebug.txt` |
| Debug `game` | `.\gradlew.bat :app:assembleGameDebug` | `app/build/outputs/apk/game/debug/app-game-debug.apk` |
| Release `game` | `.\gradlew.bat :app:assembleGameRelease` | `app/build/outputs/apk/game/release/app-game-release.apk` |
| Debug `classic` | `.\gradlew.bat :app:assembleClassicDebug` | `app/build/outputs/apk/classic/debug/app-classic-debug.apk` |
| Release `classic` | `.\gradlew.bat :app:assembleClassicRelease` | `app/build/outputs/apk/classic/release/app-classic-release.apk` |
| AAB `game` (по желанию, ТЗ 3.3) | `.\gradlew.bat :app:bundleGameRelease` | `app/build/outputs/bundle/gameRelease/app-game-release.aab` |

Тесты проверяют только `domain/` (классы `EconomyTest`, `GameEditionTest`, `ContentTest`,
`Match3Test`, `MvpRulesTest` в [app/src/test/](../app/src/test/java/ru/finny/pet/domain/)) и
поэтому одинаковы для обоих вариантов. Состав — [ARCHITECTURE.md](ARCHITECTURE.md#тесты).

Размер APK 1.3.0 (факт): release `game` — 3 893 509 байт (≈ 3,9 МБ, R8 и сжатие ресурсов
включены), debug `game` — ≈ 21,9 МБ (без R8).

## Релиз и подпись

Подпись настраивается в [app/build.gradle.kts](../app/build.gradle.kts). Источник параметров —
по порядку:

1. `finny-pet/keystore.properties` (в `.gitignore`; шаблон — [keystore.properties.example](../keystore.properties.example)):
   `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Путь к ключу — абсолютный, на Windows
   с прямыми слэшами: `storeFile=C:/keys/finny-release.jks`.
2. Переменные окружения `FINNY_STOREFILE`, `FINNY_STOREPASSWORD`, `FINNY_KEYALIAS`, `FINNY_KEYPASSWORD`.
3. Ни того ни другого — release подписывается отладочным ключом (debug keystore этой машины),
   Gradle пишет предупреждение `keystore.properties not found: release APK will be signed with the debug key`.

**1.3.0 для промежуточной сдачи подписан debug-ключом** (`CN=Android Debug`): постоянного ключа
пока нет, он появится к финалу. Ключ и пароли в репозиторий не попадают (ТЗ 3.4): `*.jks`,
`*.keystore`, `keystore.properties` — в `.gitignore`.

Создать постоянный ключ (один раз, вне репозитория; `keytool` входит в JDK):

```
keytool -genkeypair -v -keystore C:/keys/finny-release.jks -alias finny -keyalg RSA -keysize 2048 -validity 10000
```

Проверка готового APK (пути для Windows; на Linux — `apksigner` без `.bat`, `aapt2` без `.exe`):

```
<SDK>\build-tools\36.0.0\apksigner.bat verify --print-certs app\build\outputs\apk\game\release\app-game-release.apk
<SDK>\build-tools\36.0.0\aapt2.exe dump badging app\build\outputs\apk\game\release\app-game-release.apk
```

| Проверка | Результат для 1.3.0 |
|---|---|
| `apksigner verify --print-certs` | `Signer #1 certificate DN: C=US, O=Android, CN=Android Debug` |
| `aapt2 dump badging`: пакет | `package: name='ru.finny.pet' versionCode='4' versionName='1.3.0'`, `targetSdkVersion:'36'`, `application-label:'Питомец Финни'` |
| `aapt2 dump badging`: разрешения | одно: `ru.finny.pet.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` |

Это разрешение с уровнем защиты `signature` добавляет библиотека `androidx.core` при слиянии
манифестов; пользователю оно не показывается и не запрашивается. В нашем
[AndroidManifest.xml](../app/src/main/AndroidManifest.xml) разрешений нет, интернета нет,
`allowBackup="false"`. Подробно — [PRIVACY_PERMISSIONS.md](PRIVACY_PERMISSIONS.md).

Файл сдачи — копия release-APK под именем `release/finny-pet-<версия>-release.apk`.

## Установка и запуск

```
adb install -r release/finny-pet-1.3.0-release.apk
adb shell am start -n ru.finny.pet/ru.finny.pet.MainActivity
```

Для `classic`: `adb shell am start -n ru.finny.pet.classic/ru.finny.pet.MainActivity`
(у каждого варианта свой файл `app/src/<вариант>/java/ru/finny/pet/MainActivity.kt`, полное имя
класса одинаковое, пакет приложения свой).

Без компьютера: скопировать APK на телефон и открыть, разрешив установку из неизвестных источников.

| Нужно | Команда |
|---|---|
| Чистый первый запуск | `adb shell pm clear ru.finny.pet` — стирает данные приложения, APK остаётся |
| Удалить приложение | `adb uninstall ru.finny.pet` |
| Время холодного старта | `adb shell am start -W -n ru.finny.pet/ru.finny.pet.MainActivity` — строка `TotalTime` |

APK, подписанные разными ключами, друг друга не обновляют (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
1.3.0 подписан debug-ключом, поэтому перед установкой финальной сборки с постоянным ключом или
сборки с другой машины старую версию надо удалить.

## Демо-режим и тестовый профиль

Игровая неделя — счётчик, а не календарь: неделя заканчивается кнопкой, поэтому все этапы цикла
проходятся подряд и без демо-режима (ТЗ 2.5.13). Демо-режим дополнительно открывает все
10 заданий сразу (без него на первой неделе открыто 6, остальные — со 2-й по 4-ю неделю).

Включить тестовый профиль:

1. Раздел для взрослого: на стартовом экране — кнопка «Для взрослого», в комнате — значок замка
   в верхней панели.
2. Решить пример: двузначное число × однозначное (12–19 × 3–9), «Войти». Неверный ответ — новый пример.
3. «Создать тестовый профиль (демо)» → «Да, продолжить». Прогресс стирается, демо-режим
   включается, приложение открывает знакомство — питомца создают заново.

Тот же раздел ([ParentScreen.kt](../app/src/game/java/ru/finny/pet/game/screens/ParentScreen.kt)):

| Кнопка / переключатель | Что делает |
|---|---|
| «Демо-режим» | включает и выключает демо без сброса прогресса |
| «Звуки», «Музыка», «Анимации» | настройки; музыка по умолчанию выключена |
| «Бонус ребёнку» (4 причины) | +10 монет, до 3 раз в игровую неделю, запись в журнале (ТЗ 2.5.12); доступно, когда питомец создан |
| «Сбросить профиль» | питомец, прогресс, покупки и копилка обнуляются; демо-режим и настройки сохраняются; переход на стартовый экран |
| «Удалить профиль и данные» | профиль удаляется целиком, в файле остаются только настройки звуков, музыки и анимаций; демо-режим выключается; переход на стартовый экран |

Внизу раздела — номер версии («Версия 1.3.0»).

### Тестовые данные

Тестовый профиль после создания питомца: неделя 1, 100 монет, копилка 0, сытость, чистота и
настроение по 70, стадия «Малыш», бомбочек 0, демо-режим включён (в комнате подпись «· демо»).
Весь контент и числа экономики — [content.json](../app/src/main/assets/content/content.json):
3 вида питомца (котик, зайка, щенок), 3 цвета, 10 товаров, 4 цели и своя цель, 10 заданий,
15 вопросов питомца, 10 статей справки. Профиль хранится в одном файле `state.json` в памяти
приложения ([StateStore.kt](../app/src/main/java/ru/finny/pet/data/StateStore.kt)).

## Сценарий демонстрации (Приложение А, 4–5 минут)

Перед показом: `adb uninstall ru.finny.pet`, затем `adb install` файла сдачи, чтобы шаг 1 был
настоящим первым запуском. Числа ниже — для этого порядка действий; выбор вида, цвета и имени на
них не влияет.

| Шаг ТЗ | Время | Действие | Что видно | Скриншот |
|---|---|---|---|---|
| 1 | 0:00 | Запуск → «Играть» → «Дальше» × 2 | знакомство: питомец, 100 монет в неделю, три части бюджета | |
| 2–3 | 0:20 | «Создать питомца»: вид (котик / зайка / щенок), цвет, имя-подсказка «Финни» или своё → «Начать!» | галочка на выбранном виде; реального имени, телефона и e-mail не спрашиваем | [01_create](../screenshots/game/01_create.png) |
| 4 | 0:45 | Диалог «Знакомься!» → «Понятно». Нажать «Игра» | комната: 100 монет, плашка «Сейчас: Составить план», цель «Выбрать», на «Заданиях» счётчик 6. Игра закрыта до плана и объясняет почему | |
| 5 | 1:05 | «Сейчас: Составить план»: «+» 5 раз в «Обязательное», по 2 — в «Желаемое» и «Копилку» → «Подтвердить план» | 50 / 20 / 20, «В кошельке: 10 из 100»; «Вне плана осталось 10 монет — это запас» | |
| 6 | 1:35 | «Задания» → «С чего начать?» → «Решить» → «Купить корм и шампунь, а потом думать об игрушке» | объяснение ответа, +10 монет (неверный ответ дал бы +5) → 110 | |
| 7 | 2:00 | «Магазин» → «Обязательное»: «Корм» → «Купить», «Шампунь» → «Купить»; «Желаемое»: «Бантик» → «Купить»; «Домик-палатка» | после каждой покупки — баланс и показатели питомца; на палатке «не хватает»: «Не хватает 15 монет: цена 60, у тебя 45» и варианты | |
| 8 | 2:45 | «Копилка» → цель «Самокат» → сумма 20 → «Отложить 20» | копилка 20 из 150, баланс 25 | |
| 9 | 3:05 | Нажать на питомца; кубок «Прогресс»; по желанию «Игра» → системная «Назад» → «Забрать монеты» | реплика или вопрос питомца; «Откуда монеты на этой неделе»; игра даёт монеты вне плана (1 за 20 очков, до 30 в неделю) | |
| 10 | 3:25 | Луна «Спать» (в альбомной — «Завершить неделю») → «Спать!» | итог недели: три отметки, «Рост: +3 (0 → 3)», план и факт, строка «Вне плана», если играли в шаге 9 → «Дальше, к неделе 2». Смена стадии: неделя 2 — план 50 / 0 / 0, «Корм» и «Шампунь», «Спать» → +2, всего 5 очков, питомец становится «Подростком» (порог 4) | |
| 11 | 4:05 | Закрыть приложение из недавних, открыть снова → «Продолжить» | неделя, монеты, копилка, цель и стадия на месте | |
| 12 | 4:20 | Замок → пример → «Войти» | прогресс ребёнка, «Бонус ребёнку», «Звуки» / «Музыка», версия 1.3.0; «Сбросить профиль» или «Создать тестовый профиль (демо)» → «Да, продолжить» | |

Мини-игру в шаге 9 не запускать до палатки в шаге 7: её монеты (до 30) делают палатку доступной,
и попытки покупки при нехватке не получится.

Скриншоты для 7.1 (ТЗ 7.1 п. 5) сняты на эмуляторе — `finny-pet/screenshots/game/`, по другому
порядку действий (план 50 / 30 / 20, в копилку 10), поэтому числа на них с таблицей не совпадают:

| Файл | Что на нём |
|---|---|
| [01_create](../screenshots/game/01_create.png) | создание питомца: котик / зайка / щенок, 3 цвета, галочка на выбранном (шаги 2–3) |
| [02_room](../screenshots/game/02_room.png) | комната после плана и взноса: 90 монет, копилка 10, «Сейчас: В магазин», «Цель: Самокат · 10 из 150», на «Заданиях» 6 |
| [03_plan](../screenshots/game/03_plan.png) | план до подтверждения: 50 / 30 / 20, «В кошельке: 0 из 100» |
| [04_savings](../screenshots/game/04_savings.png) | копилка: 10 в копилке, 90 на балансе, «Отложить 10», цель «Самокат», осталось 140 |

Снять свои
(в PowerShell не перенаправлять бинарный вывод `adb exec-out` в файл — он портится):

```
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png
```

Резервное видео до 3 минут (ТЗ 4.1, к финалу): `adb shell screenrecord --time-limit 180 /sdcard/demo.mp4`,
затем `adb pull /sdcard/demo.mp4`.

`tools/ui.py` и `tools/demo_run.sh` писались под `classic` (тексты кнопок, путь к APK) и путь
к `adb` на macOS; под `game` 1.3.0 не переделаны и в этой инструкции не используются.

## Проверка на эмуляторе

Эмулятор команды: Android 16, x86_64, на Windows с ускорением WHPX. Портрет 360 dp:

```
adb shell wm size 1080x1920
adb shell wm density 480
```

Вернуть как было: `adb shell wm size reset`, `adb shell wm density reset`.

| Проверено 2026-09-25 (эмулятор) | Результат |
|---|---|
| Размеры экрана | портрет 360 × 640 dp и 411 × 914 dp |
| Создание питомца | котик / зайка / щенок, 3 цвета, галочка на выбранном |
| Главный экран | плашка «Сейчас» помещается на 360 dp |
| План | 50 / 20 / 20 |
| Холодный старт release (`am start -W`, 3 замера) | ≈ 0,45–0,49 с (ТЗ 3.4 — не более 5 с на устройстве) |
| Мини-игра | закрыта до плана с объяснением; «Назад» → итог → начисление |
| Раздел для взрослого | пример-барьер, бонус (4 причины, до 3 раз по 10), «Звуки» / «Музыка», версия 1.3.0 |

Не проверено: сценарий демонстрации целиком с числами из таблицы (шаги 6–8, 10 со сменой стадии,
11 с «Продолжить») на эмуляторе не прогонялся — его прошли на физическом устройстве Samsung Galaxy S23 (Android 14, 8 ГБ, 360 dp)
2026-09-25 (отчёт — [TEST_CASES.md](TEST_CASES.md#отчёт-с-физического-устройства)); звук и плавность на слух и на глаз; проверка с пользователями (ТЗ 8.4) не
проводилась — протокол [USER_TESTING.md](USER_TESTING.md).

## Генерация ассетов

Все картинки и звуки уже лежат в репозитории; генераторы нужны только чтобы их изменить.
Скрипты — в [tools/art/](../tools/art/), лицензии — [LICENSES.md](LICENSES.md), таблица «скрипт →
файлы» — [ARCHITECTURE.md](ARCHITECTURE.md#генераторы-ассетов). Команды — из `finny-pet/`,
каталог вывода — абсолютный путь.

Blender 5.2:

| ОС | Запуск |
|---|---|
| Windows | `& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe"` (PowerShell) |
| Linux | `blender` из архива с blender.org или пакета, в `PATH` |

Питомец (108 кадров: 3 вида × 3 цвета × 3 стадии × 4 выражения):

```
<blender> -b -P tools/art/pet.py -- --all C:/tmp/pets --size 512
python tools/art/import_sprites.py C:/tmp/pets
```

`pet.py` пропускает уже отрендеренные PNG, `--only-species puppy` рендерит один вид; GPU берётся
первый доступный (OptiX, CUDA, HIP, oneAPI, Metal), иначе CPU. `import_sprites.py` (нужен Pillow:
`pip install pillow`) переводит PNG в WebP `app/src/main/res/drawable-nodpi/pet_*.webp` и
перегенерирует [PetSprites.kt](../app/src/main/java/ru/finny/pet/PetSprites.kt).

| Скрипт | Команда | Что получается |
|---|---|---|
| `room.py` | `<blender> -b -P tools/art/room.py -- --all <dir>` | комната: альбом и портрет, день и вечер (PNG) |
| `props.py` | `<blender> -b -P tools/art/props.py -- --all <dir>` | товары, цели, плитки мини-игры, монета (PNG) |
| `uiprops.py` | `<blender> -b -P tools/art/uiprops.py -- --all <dir>` | иконки интерфейса (PNG) |
| `smoke.py` | `<blender> -b -P tools/art/smoke.py -- <dir>/smoke.png` | проверка общих хелперов `lib.py` |
| `sounds.py` | `python tools/art/sounds.py` (Linux — `python3`) | 13 эффектов и музыкальная петля сразу в `app/src/game/res/raw/*.ogg`; нужен `ffmpeg` в `PATH` (Vorbis, если в сборке ffmpeg есть `libvorbis`, иначе Opus) |

Для `room.py`, `props.py`, `uiprops.py` конвертера в WebP в репозитории нет: PNG переводят в WebP
вручную и кладут в `app/src/game/res/drawable-nodpi/` под теми же именами. Эти три скрипта
через `lib.py` включают GPU только на Metal (macOS), на Windows и Linux рендер идёт на CPU.

3D-питомец с ручной анимацией — к финалу, план — [docs/ART_PIPELINE.md](../../docs/ART_PIPELINE.md).

Актуально на версию 1.3.0 (2026-09-25)

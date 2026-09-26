# Сторонние библиотеки, шрифты, изображения, звуки

Перечень по ТЗ 5.12 и 3.3: «Все использованные изображения, шрифты, звуки и библиотеки должны
иметь право на использование и распространение в составе прототипа». Описан сдаваемый вариант
`game` (пакет `ru.finny.pet`, версия 1.3.0); вариант `classic` (`ru.finny.pet.classic`) остаётся
в репозитории, в материалы сдачи не входит — его отличия помечены отдельно.

## Правообладатель собственных материалов

Изображения, звуки, музыка, тексты контента и исходный код созданы командой «Сингулярность Бытия»
(Ким Владимир, Цирпка Денис) для этого проекта; права принадлежат команде. Открытая лицензия
на них не назначается: материалы распространяются в составе прототипа для конкурса.

## Библиотеки

Сверено с `app/build.gradle.kts` (раздел `dependencies`).

| Библиотека | Версия | Лицензия | Назначение |
|---|---|---|---|
| Kotlin stdlib | 2.4.20 | Apache 2.0 | язык |
| kotlinx.serialization-json | 1.11.0 | Apache 2.0 | чтение контента, сохранение профиля |
| AndroidX Compose (BOM 2026.06.01: ui, material3, ui-tooling-preview) | 1.11.4 / Material3 1.4.0 | Apache 2.0 | интерфейс |
| AndroidX Compose Material3 Adaptive Navigation Suite | 1.4.0 | Apache 2.0 | нижняя панель / рейл по классу окна (`classic`) |
| AndroidX Compose Material Icons Extended | 1.7.8 | Apache 2.0 | иконки Material Symbols (`classic`; в APK попадают только используемые) |
| AndroidX Activity Compose | 1.12.4 | Apache 2.0 | точка входа, BackHandler |
| AndroidX Lifecycle ViewModel Compose | 2.10.0 | Apache 2.0 | ViewModel |
| JUnit | 4.13.2 | EPL 1.0 | тесты (только в сборке тестов) |

Инструменты сборки: Gradle 9.4.1, Android Gradle Plugin 9.2.1, R8 — Apache 2.0.

## Шрифты

| Шрифт | Лицензия | Где |
|---|---|---|
| Montserrat (variable, wght 100–900; Julieta Ulanovsky и соавторы) | SIL Open Font License 1.1 — разрешает встраивание и распространение в составе приложения | `app/src/main/res/font/montserrat.ttf`, единственный встроенный шрифт; шрифт шаблона презентации ЛЦТ-2026 |
| Montserrat ExtraBold — статический экземпляр того же шрифта (wght 800), производная по SIL OFL 1.1 | SIL Open Font License 1.1 | `tools/art/montserrat_extrabold.ttf` — только буквы вывесок в рендерах Blender (у Blender нет осей вариативного шрифта), в APK не входит. Пересборка: `python -c "from fontTools.ttLib import TTFont; from fontTools.varLib import instancer; instancer.instantiateVariableFont(TTFont('finny-pet/app/src/main/res/font/montserrat.ttf'), {'wght': 800}).save('finny-pet/tools/art/montserrat_extrabold.ttf')"` (fontTools 4.62, только у оркестратора) |

Эмодзи (в `classic` — иконки категорий и разделов) отображаются системным шрифтом
(Noto Color Emoji, SIL OFL 1.1) и в APK не входят.

## Изображения

Все изображения — собственная работа команды: 3D-сцены из примитивов, построенные и
отрендеренные скриптами в Blender. Blender — GPL-инструмент; результат рендера принадлежит
автору сцены. Сторонних моделей, текстур, стоков и сгенерированных нейросетью картинок нет.
Генераторы лежат в `tools/art/`, ассеты из них пересобираются.

| Что | Файлы | Генератор |
|---|---|---|
| Питомец: кот, зайка, щенок × 3 цвета × 3 стадии × 4 выражения | 108 WebP `pet_*` в `app/src/main/res/drawable-nodpi/` (общие для обоих вариантов) | `tools/art/pet.py` (Blender 5.2) → `tools/art/import_sprites.py` (WebP и `PetSprites.kt`) |
| Комната: альбом и портрет, день и вечер | 4 WebP `room_*` в `app/src/game/res/drawable-nodpi/` | `tools/art/room.py` |
| Товары, цели, плитки мини-игры, монета | 22 WebP там же: `item_*` (10), `goal_*` (5), `tile_*` (6), `ui_coin` | `tools/art/props.py` |
| Иконки интерфейса: банка и крышки направлений бюджета, кошелёк, копилка, кубок, книга, замок, солнце, луна, геймпад, сумка, пузырь с вопросом | 14 WebP `ui_*` там же | `tools/art/uiprops.py` |
| Иконка приложения — рендер 3D-котика на фирменном градиенте | `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png` (адаптивная иконка, фон `drawable/ic_launcher_background.xml`), `assets/icon/icon-512.png` для магазина | котик — `tools/art/pet.py` |

Итого в `game/res/drawable-nodpi/` 40 файлов. Анимация питомца в `game` — код
`game/ui/PetSprite.kt` (в `classic` — `ui/PetView.kt`).

## Звуки

14 файлов в `app/src/game/res/raw/`, контейнер Ogg, кодек Opus. Только в `game`; в `classic`
звуков нет.

| Файл | Назначение |
|---|---|
| `sfx_coin`, `sfx_pop`, `sfx_tap`, `sfx_success`, `sfx_fail`, `sfx_whoosh`, `sfx_munch`, `sfx_splash`, `sfx_fanfare`, `sfx_match`, `sfx_bomb`, `sfx_bubble`, `sfx_sleep` | 13 звуковых эффектов |
| `music_loop` | фоновая мелодия (петля) |

Все звуки синтезированы с нуля скриптом `tools/art/sounds.py`: синусоиды, шум и фильтры на
стандартной библиотеке Python, без сэмплов и чужих записей; ffmpeg только кодирует результат.
Собственная работа команды.

## Материалы организатора

Тексты заданий и глоссария написаны командой для проекта. Образовательные результаты
обоснованы формулировками Единой рамки компетенций в области финансовой грамотности и
финансовой культуры (ТЗ, раздел 6, п. 1); рамка используется как каталог формулировок,
см. `docs/competencies.md`. Палитра интерфейса повторяет цвета шаблона презентации ЛЦТ-2026.
Логотипы организаторов в приложении не используются.

## Инструменты, которые в приложение не входят

Blender 5.2 (GPL), Python с Pillow (HPND) и ffmpeg — генерация ассетов; `tools/office` на Node.js
с npm-пакетами docx и pptxgenjs (MIT) и @material/material-color-utilities 0.2.7 (Apache 2.0) —
сборка DOCX/PPTX и цветовой схемы темы.

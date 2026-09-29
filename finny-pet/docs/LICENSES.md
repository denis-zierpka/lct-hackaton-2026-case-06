# Сторонние библиотеки, шрифты, изображения, звуки

Перечень по ТЗ 5.12 и 3.3: «Все использованные изображения, шрифты, звуки и библиотеки должны
иметь право на использование и распространение в составе прототипа». Описан сдаваемый вариант
`game` (пакет `ru.finny.pet`, версия 1.4.0); вариант `classic` (`ru.finny.pet.classic`) остаётся
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
| Montserrat ExtraBold — статический экземпляр того же шрифта (wght 800), производная по SIL OFL 1.1 | SIL Open Font License 1.1 | `tools/art/montserrat_extrabold.ttf` — только буквы вывесок в рендерах Blender (у Blender нет осей вариативного шрифта); сам файл в APK не входит, буквы вывесок запечены в фоны мест `bg_*` (картинки, созданные шрифтом, OFL 1.1 не ограничивает). Пересборка: `python -c "from fontTools.ttLib import TTFont; from fontTools.varLib import instancer; instancer.instantiateVariableFont(TTFont('finny-pet/app/src/main/res/font/montserrat.ttf'), {'wght': 800}).save('finny-pet/tools/art/montserrat_extrabold.ttf')"` (fontTools 4.62, только у оркестратора) |

Эмодзи (в `game` — товары, вещи и мечты без своей картинки; в `classic` — иконки категорий и разделов)
отображаются системным шрифтом
(Noto Color Emoji, SIL OFL 1.1) и в APK не входят.

## Изображения

Все изображения — собственная работа команды: 3D-сцены из примитивов, построенные и
отрендеренные скриптами в Blender. Blender — GPL-инструмент; результат рендера принадлежит
автору сцены. Сторонних моделей, текстур, стоков и сгенерированных нейросетью картинок нет.
Генераторы лежат в `tools/art/`, ассеты из них пересобираются.

| Что | Файлы | Генератор |
|---|---|---|
| Питомец: кот, зайка, щенок × 3 цвета × 3 стадии × 4 выражения | 108 WebP `pet_*` в `app/src/main/res/drawable-nodpi/` (общие для обоих вариантов) | `tools/art/pet.py` (Blender 5.2) → `tools/art/import_sprites.py` (WebP и `PetSprites.kt`) |
| Комната: портрет день/вечер — пустая оболочка (TOWN-A1d1–A1d2), альбом — прежний рендер (A1h); мебель — 7 спрайтов | 4 WebP `room_*`, 7 WebP `furn_*` в `app/src/game/res/drawable-nodpi/` | `tools/art/room.py` (`--only room_port_*`, `--sprites`) → `tools/art/to_webp.py` (оболочки `--rgb`) |
| Фоны мест «Городка»: рынок у реки, лавка «У Фомы», пекарня (портрет) | 3 WebP `bg_market_port`, `bg_foma_port`, `bg_bakery_port` там же | `tools/art/place.py` → `tools/art/to_webp.py --rgb` (TOWN-A1c) |
| Жители «Городка»: Марта, Фома, Боря, Ося, Тоша, Степан, Кеша, Лиза, Ася (силуэт взрослого, своя палитра, аксессуары и предметы ролей — колпак, сумка, шапочка врача, берет и палитра, мяч, гаечный ключ, значок-монета — примитивы) | 9 WebP `res_*` там же (512 × 512; `res_borya` — 768 × 768 для крупного плана пекарни, решение № 45) | `tools/art/pet.py --residents` → `tools/art/to_webp.py` (TOWN-A1f) |
| Товары, цели, плитки мини-игры, монета; вещи комнаты и плакат события (TOWN-A1e1, круг A1e1-2) | 39 WebP там же: `item_*` (26: товары и стартовые `item_home_*`; ванна с пеной, мячик, книжка, домик — перерендер 216 px), `goal_*` (6), `poster_food_super`, `tile_*` (6), `ui_coin` | `tools/art/props.py` (вещи комнаты — `render_thing` камерой и светом `room.py`) → `tools/art/to_webp.py` |
| Выпечка пекарни: багет, хлеб, круассан, кекс, пончик, крендель (TOWN-J1-1b3, круг 1b3-2) | 6 WebP `pastry_*` 256 × 256 там же | `tools/art/props.py` → `tools/art/to_webp.py --size 256` |
| Улица «Городка»: фон и фасады — дом, рынок, «У Фомы», пекарня, калитки парка, леса, зоопарка (TOWN-A1g1, круги A1g1-2…4) | фон `bg_street_port` 1080 × 1920 RGB и 7 WebP `fac_<placeId>` 384 × 384 там же | `tools/art/facade.py --street` / `--all` → `tools/art/to_webp.py` (фон `--rgb`) |
| Иконки интерфейса: банка и крышки направлений бюджета, кошелёк, копилка, кубок, книга, замок, солнце, луна, геймпад, сумка, пузырь с вопросом | 14 WebP `ui_*` там же | `tools/art/uiprops.py` |
| Иконка приложения — рендер 3D-котика на фирменном градиенте | `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png` (адаптивная иконка, фон `drawable/ic_launcher_background.xml`), `assets/icon/icon-512.png` для магазина | котик — `tools/art/pet.py` |

Итого в `game/res/drawable-nodpi/` 91 файл. Анимация питомца в `game` — код
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

Тексты событий, загадок и глоссария написаны командой для проекта. Образовательные результаты
обоснованы формулировками Единой рамки компетенций в области финансовой грамотности и
финансовой культуры (ТЗ, раздел 6, п. 1); рамка используется как каталог формулировок,
см. `docs/competencies.md`. Палитра интерфейса повторяет цвета шаблона презентации ЛЦТ-2026.
Логотипы организаторов в приложении не используются.

## Инструменты, которые в приложение не входят

Blender 5.2 (GPL), Python с Pillow (HPND) и ffmpeg — генерация ассетов; `tools/office` на Node.js
с npm-пакетами docx и pptxgenjs (MIT) и @material/material-color-utilities 0.2.7 (Apache 2.0) —
сборка DOCX/PPTX и цветовой схемы темы.

# Сторонние библиотеки, шрифты, изображения, звуки

## Библиотеки

| Библиотека | Версия | Лицензия | Назначение |
|---|---|---|---|
| Kotlin stdlib | 2.4.20 | Apache 2.0 | язык |
| kotlinx.serialization-json | 1.11.0 | Apache 2.0 | чтение контента, сохранение профиля |
| AndroidX Compose (BOM 2026.06.01: ui, material3, ui-tooling-preview) | 1.11.4 / Material3 1.4.0 | Apache 2.0 | интерфейс |
| AndroidX Compose Material3 Adaptive Navigation Suite | 1.4.0 | Apache 2.0 | нижняя панель / рейл по классу окна |
| AndroidX Compose Material Icons Extended | 1.7.8 | Apache 2.0 | иконки Material Symbols (в APK попадают только используемые) |
| material-color-utilities (npm, только при генерации темы) | 0.2.7 | Apache 2.0 | генерация цветовой схемы M3 из seed-цветов; в приложение не входит |
| AndroidX Activity Compose | 1.12.4 | Apache 2.0 | точка входа, BackHandler |
| AndroidX Lifecycle ViewModel Compose | 2.10.0 | Apache 2.0 | ViewModel |
| JUnit | 4.13.2 | EPL 1.0 | тесты (только в сборке тестов) |

Инструменты сборки: Gradle 9.4.1, Android Gradle Plugin 9.2.1, R8 — Apache 2.0.

## Шрифты

| Шрифт | Лицензия | Где |
|---|---|---|
| Montserrat (variable, wght 100–900; Julieta Ulanovsky и соавторы) | SIL Open Font License 1.1 — разрешает встраивание и распространение в составе приложения | `app/src/main/res/font/montserrat.ttf`, единственный встроенный шрифт; шрифт шаблона презентации ЛЦТ-2026 |

Эмодзи отображаются системным шрифтом (Noto Color Emoji, SIL OFL 1.1) и в APK не входят.

## Изображения

Все изображения созданы командой для этого проекта и распространяются вместе с прототипом:

- питомец (3 вида × 3 цвета × 3 стадии × 4 выражения = 108 спрайтов WebP в `app/src/main/res/drawable-nodpi/`) — собственные 3D-модели из примитивов, построены и отрендерены скриптом `tools/art/pet.py` в Blender 5.2 (GPL-инструмент; результат рендера принадлежит команде, как и любой файл, созданный в Blender); анимация — код `ui/PetView.kt`;
- иконка приложения — рендер того же 3D-котика на фирменном градиенте: `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png` (адаптивная иконка, фон `drawable/ic_launcher_background.xml`) и `assets/icon/icon-512.png` для магазина;
- иконки категорий и разделов — стандартные эмодзи Unicode.

## Звуки

Не используются.

## Материалы организатора

Тексты заданий и глоссария написаны для проекта на основе Единой рамки компетенций в области финансовой грамотности (базовый уровень). Логотипы организаторов в приложении не используются.

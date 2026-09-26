```
TASK: TOWN-A1a — инструменты арта: GPU в lib.py, to_webp.py, импортируемые генераторы, листы без искажений
EPIC: TOWN-A1 (docs/tasks/TOWN-A1.md)
BASE: последний коммит этой спеки — `git log -1 --format=%h -- docs/tasks/TOWN-A1a.md`
BRANCH: feat/town

## КОНТЕКСТ
Первая задача эпика арта среза 1. Генераторы room.py, props.py, uiprops.py сидят на lib.py, а
lib.reset_scene включает GPU только через METAL (lib.py:36–41): на Windows это TypeError и рендер
на CPU. На этой машине Cycles видит HIP (AMD Radeon RX 9060 XT); pet.py уже перебирает бэкенды
(pet.py:44–55). Конвертера PNG → WebP для room/props/uiprops нет (import_sprites.py берёт только
pet_*). room.py и uiprops.py разбирают argv и рендерят на уровне модуля — следующие задачи не могут
импортировать их функции. props.py с «--only a,b --out F» пишет все кадры в один файл.
tools/sheets.py делает convert("RGB") (прозрачное → чёрное) и растягивает каждый кадр в ячейку по
пропорции первого (квадрат рядом с портретом вытягивается в 1,78 раза). Спека прошла 3 критиков
(25 находок по A1a учтены). Ассеты не меняются.

## ТРЕБОВАНИЕ ТЗ
3.3 (ассеты пересобираются генераторами из репозитория, LICENSES.md); подготовка эпика TOWN-A1.

## CONTRACT
1. lib.reset_scene: выбор устройства — как pet.py:44–55: для kind в (OPTIX, CUDA, HIP, ONEAPI, METAL)
   prefs.compute_device_type = kind и prefs.get_devices_for_type(kind); TypeError/ValueError —
   пропустить; первый kind, у которого есть устройство этого типа: d.use = (d.type == kind) для всех
   prefs.devices, scene.cycles.device = "GPU"; иначе CPU. Печать «cycles device: <KIND или CPU>» с
   flush — ПОСЛЕ выбора, из фактического состояния. Шапка lib.py и докстринг reset_scene — без
   «Metal». Остальное reset_scene не меняется. pet.py не трогать.
2. room.py и uiprops.py: разбор argv и запуск — под `if __name__ == "__main__":` (не по наличию «--»);
   функции получают значения параметрами (uiprops.render_prop(name, out, samples, size)). Импорт модуля
   внутри Blender не создаёт объектов и материалов, не рендерит и не читает sys.argv. CLI, имена,
   умолчания и кадры — те же.
3. props.py: «--out X» при «--only»: X — каталог, если X уже существующий каталог или имён больше
   одного (каталог создаётся, файлы X/<имя>.png); иначе X — файл. Докстринг (строки 3–4) — с формой
   «--only a,b --out DIR». Остальное CLI не меняется.
4. Новый finny-pet/tools/art/to_webp.py (Python + Pillow, не Blender):
   `python tools/art/to_webp.py SRC [SRC ...] --dst DIR [--size N | --size WxH] [--rgb]`.
   SRC — PNG-файлы или каталоги (*.png); несуществующий SRC или ни одного PNG — exit ≠ 0 без записи.
   DIR создаётся. Для каждого: Image.open → convert("RGB" с --rgb, иначе "RGBA") → resize((W, H),
   LANCZOS), если --size (N → N×N) → save(DIR/<stem>.webp, "WEBP", quality=88, alpha_quality=90,
   method=6) — порядок и параметры как import_sprites.py:33–34. Строка на файл «<имя> <W>x<H> <mode>
   <байт> B» — размер и mode из ЗАПИСАННОГО файла (Image.open(out)); итог — число файлов и байт.
5. tools/sheets.py: (а) кадр с прозрачностью (RGBA, LA, P с info["transparency"]) кладётся на белый,
   RGB — как есть; (б) кадр вписывается в ячейку W×H С СОХРАНЕНИЕМ ПРОПОРЦИЙ (ImageOps.contain) и
   центрируется на белом; H ячейки — по первому кадру, как сейчас. CLI и подписи — без изменений.

## SCOPE (allow)
finny-pet/tools/art/lib.py, finny-pet/tools/art/room.py, finny-pet/tools/art/uiprops.py,
finny-pet/tools/art/props.py, finny-pet/tools/art/to_webp.py (новый), tools/sheets.py

## ANTI-SCOPE
Новые билдеры и ассеты; pet.py, import_sprites.py, sounds.py, smoke.py; app/, content.json, docs/.
Кодер может запускать Blender и to_webp для самопроверки ТОЛЬКО с выводом в каталог вне репозитория
(системный temp); в дереве репозитория после него — только файлы allow. Зависимости 0.
Доки (BUILD_AND_DEMO «Генерация ассетов», ARCHITECTURE «Генераторы ассетов», GAME_CONCEPT §17.6,
ART_PIPELINE) правит оркестратор после приёмки.

## ACCEPTANCE (оркестратор, Git Bash из корня; B="/c/Program Files/Blender Foundation/Blender 5.2/blender.exe";
## T — каталог в scratchpad, TW=$(cygpath -m $T), RW=$(pwd -W); у КАЖДОГО вызова Blender —
## --python-exit-code 1 до -P/--python-expr; время рендеров записать)
  1. GPU по состоянию, а не по печати:
     "$B" -b --factory-startup --python-exit-code 1 --python-expr "import sys,bpy; sys.path.insert(0, r'$RW/finny-pet/tools/art'); import lib; s=lib.reset_scene(8,64); p=bpy.context.preferences.addons['cycles'].preferences; on=[d.type for d in p.devices if d.use]; print('PROBE', s.cycles.device, p.compute_device_type, on); assert s.cycles.device=='GPU' and p.compute_device_type=='HIP' and on and set(on)=={'HIP'}"
     -> exit 0, «PROBE GPU HIP ['HIP', …]»; grep -c "Metal GPU\|on Metal" finny-pet/tools/art/lib.py -> 0
     (голое «Metal» совпадает с входом «Metallic» у Principled BSDF — не критерий; поправка по приёмке)
  2. smoke: "$B" -b --python-exit-code 1 -P finny-pet/tools/art/smoke.py -- $TW/s_gpu.png -> 512×512 RGBA. Время
     GPU против CPU — на тяжёлом кадре (smoke 512² × 64 упирается в запуск Blender: 11,8 против 11,0 с):
     room.render_variant('room_port_day', …, 64) через --python-expr, CPU — подменой scene.cycles.device в
     room.reset_scene; время GPU < время CPU (2026-09-26: 28,8 с против 103 с)
  3. импорт без побочных эффектов (cwd = пустой $T/imp):
     cd $T/imp && "$B" -b --factory-startup --python-exit-code 1 --python-expr "import sys,bpy; sys.path.insert(0, r'$RW/finny-pet/tools/art'); n=(len(bpy.data.objects),len(bpy.data.materials)); import lib, room, uiprops, props; m=(len(bpy.data.objects),len(bpy.data.materials)); assert m==n, m; print('IMPORT OK', n)" -- --only room_port_day --out $TW/leak.png > $T/imp.log 2>&1
     -> exit 0; «IMPORT OK» в логе; «cycles device» и «Saved» в логе нет; $T/imp пуст; $TW/leak.png нет
  4. комната: room.py -- --only room_port_day --out $TW/room_prev.png --preview -> 540×960;
     room.py -- --only room_port_day --out $TW/room_port_day.png -> 1080×1920;
     to_webp.py $TW/room_port_day.png --dst $TW/w_bg --rgb -> один файл, PIL mode RGB, os.path.getsize ≤ 61 440;
     лист «room_port_day из res / новый» — та же комната
  5. props: props.py -- --only item_fun_ball,item_food_basic --out $TW/p2 -> ровно 2 файла в $TW/p2;
     props.py -- --only item_fun_ball --out $TW/p2 (существующий каталог) -> $TW/p2/item_fun_ball.png;
     props.py -- --only item_fun_ball --out $TW/one.png -> файл; альфа-bbox новых кадров совпадает с res
     в пределах ±3 px: item_fun_ball (85,69,414,377), item_food_basic (72,118,412,341)
  6. uiprops: uiprops.py -- --only ui_lock --out $TW/ui_lock.png -> 512×512, bbox ±3 px от (151,127,364,405);
     uiprops.py -- --only ui_lock --out $TW/ui_lock256.png --size 256 --samples 8 -> 256×256
  7. to_webp: to_webp.py $TW/p2 --dst $TW/w_sp -> ровно 2 файла, PIL mode RGBA, 512×512, getsize ≤ 37 888;
     побайтное равенство с эталоном: python -c "import io,sys;from PIL import Image;s,d,m=sys.argv[1:4];b=io.BytesIO();Image.open(s).convert(m).save(b,'WEBP',quality=88,alpha_quality=90,method=6);assert b.getvalue()==open(d,'rb').read()" $TW/p2/item_fun_ball.png $TW/w_sp/item_fun_ball.webp RGBA
     (и для фона из п. 4 с RGB); --size: `--size 540x960` на фоне и `--size 256` на спрайте — равенство с
     эталоном convert → resize(LANCZOS) → save; пустой каталог и несуществующий SRC -> exit ≠ 0, --dst пуст
  8. sheets: кадр 1080×1920 первым (i = 0), затем пять кадров 512² в $TW (i = 1…5: RGBA-рендер из п. 5,
     res item_fun_ball.webp, LA прозрачный, P с transparency, RGB чёрный) -> python tools/sheets.py
     $TW/a1a_alpha.jpg "альфа" …; ячейка i: x = 16 + 376·(i mod 5), y = 60 + 720·(i div 5); квадрат
     360×360 занимает в ней y+140…y+500; проба PIL im.getpixel((x+8, y+148)) — для i = 1…4 каналы ≥ 240,
     для i = 5 (RGB-контроль) ≤ 15; на том же листе, собранном ДО правки (git stash/копия BASE), i = 1 даёт ≤ 15;
     квадрат не растянут: в ячейке i = 1 bbox небелых пикселей (любой канал < 200) имеет пропорцию w/h как
     альфа-bbox исходного PNG ±3 %
  9. регрессия глазами: лист «res / новый» для room_port_day, item_fun_ball, item_food_basic, ui_lock
  10. git diff --name-only BASE и git status --porcelain --untracked-files=all -> только 6 путей allow
      (to_webp.py как ??); git status --porcelain -uall -- finny-pet/app -> пусто
  11. ревьювер: PASS
```

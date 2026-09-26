"""Мутационные зонды оркестратора (WORKFLOW №14; TOWN-S1a…S1c): каждый мутант обязан СКОМПИЛИРОВАТЬСЯ и
сделать оракул красным. Самопроверка первой: исходный код зелёный, иначе вердикта нет.
Запуск из корня: python tools/mutation_probe.py <mutations.json>
mutations.json — список {"name", "file" (от finny-pet/), "old" (ровно одно вхождение), "new"}.
Файл восстанавливается из копии после каждого мутанта; в конце — повторный прогон исходного."""
import json, subprocess, sys, shutil, glob, os
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "finny-pet")
GRADLE = os.path.join(ROOT, "gradlew.bat" if os.name == "nt" else "gradlew")
RES = os.path.join(ROOT, "app", "build", "test-results", "testGameDebugUnitTest")


def run():
    shutil.rmtree(RES, ignore_errors=True)
    p = subprocess.run([GRADLE, "testGameDebugUnitTest", "--console=plain", "-q"], cwd=ROOT,
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    t = f = 0
    failed = []
    for x in glob.glob(os.path.join(RES, "*.xml")):
        r = ET.parse(x).getroot()
        t += int(r.get("tests")); f += int(r.get("failures")) + int(r.get("errors"))
        for tc in r.iter("testcase"):
            if tc.find("failure") is not None or tc.find("error") is not None:
                failed.append(r.get("name").split(".")[-1] + "." + tc.get("name"))
    compile_err = "Compilation error" in p.stdout + p.stderr or "e: file:" in p.stdout + p.stderr
    return p.returncode, t, f, failed, compile_err


muts = json.load(open(sys.argv[1], encoding="utf-8"))
code, t, f, _, ce = run()
print(f"SELF-CHECK original: exit={code} tests={t} failed={f} compile_err={ce}", flush=True)
if code != 0 or f != 0 or t == 0:
    print("ABORT: original is not green"); sys.exit(2)
ok = True
for m in muts:
    path = os.path.join(ROOT, m["file"])
    src = open(path, encoding="utf-8").read()
    n = src.count(m["old"])
    if n != 1:
        print(f"[{m['name']}] SKIP: pattern found {n} times"); ok = False; continue
    shutil.copy(path, path + ".bak")
    try:
        open(path, "w", encoding="utf-8").write(src.replace(m["old"], m["new"]))
        code, t, f, failed, ce = run()
        if t == 0 or ce:
            verdict = "NO-COMPILE (probe invalid)"; ok = False
        elif f > 0:
            verdict = f"CAUGHT by {f}: " + ", ".join(failed[:3])
        else:
            verdict = "SURVIVED"; ok = False
        print(f"[{m['name']}] {verdict}", flush=True)
    finally:
        shutil.move(path + ".bak", path)
code, t, f, _, ce = run()
print(f"RESTORE-CHECK: exit={code} tests={t} failed={f}", flush=True)
print("ALL CAUGHT" if ok and f == 0 else "PROBLEMS")

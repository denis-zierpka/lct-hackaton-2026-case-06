"""Мутационные зонды оркестратора (WORKFLOW №14; TOWN-S1a…S1c): каждый мутант обязан СКОМПИЛИРОВАТЬСЯ и
сделать оракул красным. Самопроверка первой: исходный код зелёный, иначе вердикта нет.
Запуск из корня: python tools/mutation_probe.py <mutations.json>
mutations.json — список {"name", "file" (от finny-pet/), "old" (ровно одно вхождение), "new", "expect"?}.
expect (строка или список, TOWN-J1-0) — подстроки имён тестов, которые обязаны упасть: CAUGHT только если
каждая нашлась среди упавших, иначе WRONG-TEST. Прогон дольше TIMEOUT с — HUNG (демон Gradle гасится).
Файл восстанавливается из копии после каждого мутанта; в конце — повторный прогон исходного."""
import json, subprocess, sys, shutil, glob, os
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "finny-pet")
GRADLE = os.path.join(ROOT, "gradlew.bat" if os.name == "nt" else "gradlew")
RES = os.path.join(ROOT, "app", "build", "test-results", "testGameDebugUnitTest")
TIMEOUT = 900


def run():
    shutil.rmtree(RES, ignore_errors=True)
    try:
        p = subprocess.run([GRADLE, "testGameDebugUnitTest", "--console=plain", "-q"], cwd=ROOT,
                           capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=TIMEOUT)
    except subprocess.TimeoutExpired:
        subprocess.run([GRADLE, "--stop"], cwd=ROOT, capture_output=True)
        return None
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
r0 = run()
if r0 is None:
    print("ABORT: original HUNG"); sys.exit(2)
code, t, f, _, ce = r0
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
        res = run()
        expect = m.get("expect", [])
        expect = [expect] if isinstance(expect, str) else expect
        if res is None:
            verdict = "HUNG"; ok = False
        else:
            code, t, f, failed, ce = res
            miss = [e for e in expect if not any(e in x for x in failed)]
            if t == 0 or ce:
                verdict = "NO-COMPILE (probe invalid)"; ok = False
            elif f > 0 and not miss:
                verdict = f"CAUGHT by {f}: " + ", ".join(failed)
            elif f > 0:
                verdict = f"WRONG-TEST (нет {miss}) by {f}: " + ", ".join(failed); ok = False
            else:
                verdict = "SURVIVED"; ok = False
        print(f"[{m['name']}] {verdict}", flush=True)
    finally:
        shutil.move(path + ".bak", path)
rr = run()
code, t, f, _, ce = rr if rr is not None else (1, 0, 1, [], False)
print(f"RESTORE-CHECK: exit={code} tests={t} failed={f}", flush=True)
print("ALL CAUGHT" if ok and f == 0 else "PROBLEMS")

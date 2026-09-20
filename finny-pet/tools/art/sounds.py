#!/usr/bin/env python3
"""Synthesize Finny's sound effects and music loop from scratch (stdlib only).

Writes WAVs to a temp dir, converts with ffmpeg to OGG Vorbis into app/src/game/res/raw/.
Run: python3 tools/art/sounds.py
"""
import math
import os
import random
import struct
import subprocess
import tempfile
import wave

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "app", "src", "game", "res", "raw")
random.seed(7)  # deterministic output


# ---------- primitives ----------

def silence(sec):
    return [0.0] * int(SR * sec)


def env(n, attack, decay, curve=4.0):
    """Attack (linear, seconds) then exponential decay to ~0 at the end."""
    a = max(1, int(SR * attack))
    out = []
    for i in range(n):
        if i < a:
            out.append(i / a)
        else:
            out.append(math.exp(-curve * (i - a) / max(1, n - a)))
    return out


def tone(freq, sec, harmonics=((1, 1.0),), attack=0.005, curve=5.0, glide=0.0):
    """Sum of sine partials (ratio, gain); glide = freq multiplier at the end (1.0 = none)."""
    n = int(SR * sec)
    e = env(n, attack, sec, curve)
    out = []
    ph = 0.0
    for i in range(n):
        f = freq * (1 + (glide - 1) * i / n) if glide else freq
        ph += 2 * math.pi * f / SR
        s = sum(g * math.sin(ph * r) for r, g in harmonics)
        out.append(s * e[i])
    return out


def noise(sec):
    return [random.uniform(-1, 1) for _ in range(int(SR * sec))]


def lowpass(x, cutoff):
    """One-pole low-pass; cutoff in Hz (scalar or list per sample)."""
    y, out = 0.0, []
    scalar = not isinstance(cutoff, list)
    for i, v in enumerate(x):
        c = cutoff if scalar else cutoff[i]
        a = 1 - math.exp(-2 * math.pi * c / SR)
        y += a * (v - y)
        out.append(y)
    return out


def highpass(x, cutoff):
    return [v - l for v, l in zip(x, lowpass(x, cutoff))]


def shape(x, e):
    return [v * g for v, g in zip(x, e)]


def mix(*parts):
    """Sum lists of (offset_seconds, samples)."""
    n = max(int(SR * off) + len(s) for off, s in parts)
    out = [0.0] * n
    for off, s in parts:
        o = int(SR * off)
        for i, v in enumerate(s):
            out[o + i] += v
    return out


def reverb(x, echoes=((0.031, 0.3), (0.067, 0.15)), tail=0.1):
    out = x + silence(tail)
    for delay, gain in echoes:
        d = int(SR * delay)
        for i in range(len(x)):
            if i + d < len(out):
                out[i + d] += x[i] * gain
    return out


def master(x, peak_db):
    x = [v * (1 - math.exp(-8 * (len(x) - i) / SR)) for i, v in enumerate(x)]  # 125 ms fade-out, no click
    dc = sum(x) / len(x)
    x = [v - dc for v in x]
    peak = max(abs(v) for v in x) or 1.0
    g = 10 ** (peak_db / 20) / peak
    return [v * g for v in x]


def write_wav(path, x):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v)) * 32767)) for v in x))


def note(n):
    """MIDI note number -> Hz."""
    return 440 * 2 ** ((n - 69) / 12)


MARIMBA = ((1, 1.0), (4, 0.25), (10, 0.05))  # marimba-like partials
BELL = ((1, 1.0), (2.76, 0.3))


# ---------- effects ----------

def sfx_coin():
    a = tone(note(88), 0.25, ((1, 1.0), (2.5, 0.4)), curve=7)
    b = tone(note(95), 0.22, ((1, 0.6),), curve=8)
    return reverb(mix((0.0, a), (0.04, b)))


def sfx_pop():
    body = tone(420, 0.12, ((1, 1.0), (2, 0.2)), attack=0.002, curve=9, glide=0.55)
    return body


def sfx_tap():
    t = tone(900, 0.06, ((1, 1.0), (2, 0.3)), attack=0.001, curve=10, glide=0.7)
    return lowpass(t, 3000)


def sfx_success():
    notes = [(0.0, 72), (0.16, 76), (0.32, 79)]
    return reverb(mix(*[(t, tone(note(n), 0.35, MARIMBA, curve=5)) for t, n in notes]))


def sfx_fail():
    a = tone(note(67), 0.28, ((1, 1.0), (2, 0.15)), attack=0.02, curve=4)
    b = tone(note(64), 0.32, ((1, 1.0), (2, 0.15)), attack=0.02, curve=4)
    return reverb(mix((0.0, a), (0.2, b)))


def sfx_whoosh():
    n = int(SR * 0.35)
    cut = [400 + 3600 * math.sin(math.pi * i / n) for i in range(n)]
    x = lowpass(noise(0.35), cut)
    return shape(highpass(x, 150), env(n, 0.12, 0.35, 3))


def sfx_munch():
    parts = []
    for k, t in enumerate((0.0, 0.17, 0.34)):
        n = int(SR * 0.12)
        crunch = shape(lowpass(noise(0.12), 1800 - 300 * k), env(n, 0.004, 0.12, 8))
        thump = tone(140, 0.08, ((1, 1.0),), attack=0.003, curve=8, glide=0.6)
        parts += [(t, crunch), (t, [v * 0.8 for v in thump])]
    return mix(*parts)


def sfx_splash():
    n = int(SR * 0.5)
    water = shape(lowpass(highpass(noise(0.5), 600), 2500), env(n, 0.02, 0.5, 5))
    parts = [(0.0, water)]
    for _ in range(9):  # little bubbles rising through the splash
        t = random.uniform(0.03, 0.35)
        f = random.uniform(500, 1400)
        parts.append((t, [v * 0.35 for v in tone(f, 0.08, ((1, 1.0),), attack=0.005, curve=7, glide=1.6)]))
    return reverb(mix(*parts))


def sfx_fanfare():
    seq = [(0.0, 72, 0.18), (0.18, 72, 0.18), (0.36, 76, 0.18), (0.54, 79, 0.3), (0.84, 84, 0.5)]
    parts = []
    for t, n, d in seq:
        lead = tone(note(n), d + 0.15, ((1, 1.0), (2, 0.5), (3, 0.25), (4, 0.1)), attack=0.01, curve=4)
        parts.append((t, lead))
    parts.append((0.54, [v * 0.35 for v in tone(note(60), 0.8, ((1, 1.0), (2, 0.3)), attack=0.02, curve=3)]))
    return reverb(mix(*parts), tail=0.2)


def sfx_match():
    parts = [(i * 0.03, [v * 0.6 for v in tone(note(84 + 2 * i), 0.18, BELL, curve=6)]) for i in range(7)]
    return reverb(mix(*parts))


def sfx_bomb():
    n = int(SR * 0.5)
    thump = tone(70, 0.45, ((1, 1.0), (2, 0.3)), attack=0.005, curve=6, glide=0.4)
    boom = shape(lowpass(noise(0.5), [1500 * math.exp(-6 * i / n) + 80 for i in range(n)]), env(n, 0.01, 0.5, 6))
    return reverb(mix((0.0, thump), (0.0, [v * 0.7 for v in boom])), tail=0.15)


def sfx_bubble():
    return tone(380, 0.2, ((1, 1.0), (2, 0.2)), attack=0.01, curve=5, glide=2.2)


def sfx_sleep():
    parts = []
    for i, n in enumerate((79, 76, 72)):
        parts.append((i * 0.25, tone(note(n), 0.55, ((1, 1.0), (2, 0.15), (3, 0.05)), attack=0.06, curve=3)))
    hum = tone(note(48), 0.8, ((1, 1.0), (2, 0.4)), attack=0.15, curve=2.5)
    parts.append((0.0, [v * 0.35 for v in hum]))
    return reverb(mix(*parts), tail=0.2)


# ---------- music ----------

def music_loop():
    bpm, bars = 90, 8
    beat = 60 / bpm
    total = int(SR * beat * 4 * bars)  # 21.33 s
    out = [0.0] * total

    def add(t, samples, gain):
        o = int(SR * t)
        for i, v in enumerate(samples):
            out[(o + i) % total] += v * gain  # wrap tails around -> seamless loop

    chords = [(60, 64, 67), (57, 60, 64), (53, 57, 60), (55, 59, 62)] * 2  # C Am F G
    pad_h = ((1, 1.0), (2, 0.35), (3, 0.12))
    for b, ch in enumerate(chords):
        t = b * 4 * beat
        for n in ch:
            add(t, tone(note(n - 12), 4 * beat + 0.4, pad_h, attack=0.5, curve=1.5), 0.18)
        add(t, tone(note(ch[0] - 24), 4 * beat, ((1, 1.0), (2, 0.2)), attack=0.02, curve=2), 0.25)

    # plucked melody: two 4-bar phrases, pentatonic over the chords, ends on the root
    melody = [
        (0, 72, 1), (1, 76, 1), (2, 79, 2), (4, 76, 1), (5, 72, 1), (6, 74, 2),
        (8, 69, 1), (9, 72, 1), (10, 74, 2), (12, 71, 1), (13, 74, 1), (14, 72, 2),
        (16, 72, 1), (17, 76, 1), (18, 79, 1), (19, 81, 1), (20, 79, 2), (22, 76, 2),
        (24, 74, 1), (25, 72, 1), (26, 69, 2), (28, 71, 1), (29, 74, 1), (30, 72, 2),
    ]
    pluck_h = ((1, 1.0), (2, 0.5), (3, 0.2), (4, 0.08))
    for beat_i, n, dur in melody:
        add(beat_i * beat, tone(note(n), dur * beat * 1.2, pluck_h, attack=0.004, curve=5), 0.32)

    # arpeggio sparkle on the "and" of each beat, very quiet
    for b, ch in enumerate(chords):
        for k in range(8):
            n = ch[k % 3] + 12
            add((b * 8 + k) * beat / 2 + beat / 4, tone(note(n), 0.25, ((1, 1.0), (2, 0.2)), attack=0.003, curve=6), 0.07)

    return lowpass(out, 6000)


SOUNDS = {
    "sfx_coin": sfx_coin, "sfx_pop": sfx_pop, "sfx_tap": sfx_tap, "sfx_success": sfx_success,
    "sfx_fail": sfx_fail, "sfx_whoosh": sfx_whoosh, "sfx_munch": sfx_munch, "sfx_splash": sfx_splash,
    "sfx_fanfare": sfx_fanfare, "sfx_match": sfx_match, "sfx_bomb": sfx_bomb, "sfx_bubble": sfx_bubble,
    "sfx_sleep": sfx_sleep, "music_loop": music_loop,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    encoders = subprocess.run(["ffmpeg", "-hide_banner", "-encoders"], capture_output=True, text=True).stdout
    global CODEC  # ponytail: homebrew ffmpeg ships without libvorbis; Opus-in-Ogg plays natively on Android 5+ (minSdk 26)
    CODEC = ["-c:a", "libvorbis", "-q:a", "4"] if " libvorbis " in encoders else ["-c:a", "libopus", "-b:a", "64k"]
    total = 0
    with tempfile.TemporaryDirectory() as tmp:
        for name, fn in SOUNDS.items():
            x = fn()
            if name == "music_loop":
                dc = sum(x) / len(x)
                x = [v - dc for v in x]
                peak = max(abs(v) for v in x)
                x = [v * 10 ** (-14 / 20) / peak for v in x]  # no fade: seamless loop
            else:
                x = master(x, -6)
            peak, dc = max(abs(v) for v in x), sum(x) / len(x)
            assert peak < 0.9 and abs(dc) < 0.01, (name, peak, dc)
            wav = os.path.join(tmp, name + ".wav")
            ogg = os.path.join(OUT, name + ".ogg")
            write_wav(wav, x)
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav] + CODEC + [ogg], check=True)
            dur = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", ogg],
                                 capture_output=True, text=True, check=True).stdout.strip()
            size = os.path.getsize(ogg)
            total += size
            print(f"{name:14s} {float(dur):6.2f} s  {size / 1024:6.1f} KB  peak={peak:.2f} dc={dc:+.4f}")
    print(f"total {total / 1024:.0f} KB -> {OUT}")


if __name__ == "__main__":
    main()

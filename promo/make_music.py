"""Vaqtincha (placeholder) fon musiqasi: Hijaz maqomi (re-mi bemol-fa diez-sol-lya-si bemol-do), oud, doira, drone.
Magnific "Arabic Roots" treki bilan almashtirish uchun add_music.sh dan foydalaning.
Ishlatish: python3 make_music.py out/music_temp.wav [ting.mp3]"""
import subprocess, sys, wave
import numpy as np

SR = 44100
DUR = 38.0
N = int(SR * DUR)
rng = np.random.default_rng(7)
out_path = sys.argv[1]
ting_path = sys.argv[2] if len(sys.argv) > 2 else None

L = np.zeros(N); R = np.zeros(N)
def add(sig, t0, pan=0.0, gain=1.0):
    i = int(t0 * SR)
    if i >= N: return
    s = sig[: N - i] * gain
    L[i:i + len(s)] += s * (1 - max(pan, 0)); R[i:i + len(s)] += s * (1 + min(pan, 0))

def env_ar(n, a, r):
    e = np.ones(n); na = max(1, int(a * SR)); nr = max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na) ** 1.5
    e[-nr:] *= np.linspace(1, 0, nr) ** 1.5
    return e

def pluck(freq, dur, decay=0.996, bright=0.5):
    n = int(dur * SR); p = max(2, int(round(SR / freq)))
    y = np.zeros(n)
    noise = rng.uniform(-1, 1, p) * np.hanning(p) ** 0.3
    # sodda past chastotali filtr: boshlang'ich shovqinni yumshatish
    for _ in range(int(3 + (1 - bright) * 6)):
        noise = 0.5 * (noise + np.roll(noise, 1))
    y[:p] = noise
    prev2 = 0.0
    k = p
    while k < n:
        m = min(p, n - k)
        prev = y[k - p:k - p + m]
        shifted = np.concatenate(([y[k - p - 1] if k - p - 1 >= 0 else 0.0], y[k - p:k - p + m - 1]))
        y[k:k + m] = decay * 0.5 * (prev + shifted)
        k += p
    y /= (np.max(np.abs(y)) + 1e-9)
    return y * np.exp(-np.arange(n) / (SR * dur * 0.55))

# ---- chastotalar (Hijaz, re tonika)
F = {"D2": 73.42, "A2": 110.0, "D3": 146.83, "Eb3": 155.56, "F#3": 185.0, "G3": 196.0, "A3": 220.0, "Bb3": 233.08,
     "C4": 261.63, "D4": 293.66, "Eb4": 311.13, "F#4": 369.99, "G4": 392.0, "A4": 440.0, "Bb4": 466.16, "C5": 523.25, "D5": 587.33}
BPM = 80; BEAT = 60 / BPM

# ---- drone + pad
t = np.arange(N) / SR
drone = (np.sin(2 * np.pi * F["D2"] * t) + 0.55 * np.sin(2 * np.pi * F["A2"] * t) + 0.25 * np.sin(2 * np.pi * 2 * F["D2"] * t))
drone *= (0.8 + 0.2 * np.sin(2 * np.pi * 0.17 * t)) * np.minimum(1, t / 3.0)
L += drone * 0.075; R += drone * 0.075
chords = [("D3", "F#3", "A3"), ("Eb3", "G3", "Bb3"), ("D3", "F#3", "A3"), ("G3", "Bb3", "D4")]
for k in range(7):
    t0 = 1.0 + k * 5.4; n = int(6.4 * SR)
    ch = chords[k % len(chords)]
    pad = sum(np.sin(2 * np.pi * F[x] * np.arange(n) / SR + i) + 0.4 * np.sin(2 * np.pi * F[x] * 2 * np.arange(n) / SR) for i, x in enumerate(ch))
    pad *= env_ar(n, 2.0, 2.6) * 0.035
    add(pad, t0, 0.0)

# ---- oud ohangi
A_ = [("D4", 1), ("Eb4", .5), ("F#4", .5), ("G4", 1), ("F#4", .5), ("Eb4", .5), ("D4", 2)]
B_ = [("A4", 1), ("G4", .5), ("F#4", .5), ("Eb4", 1), ("D4", .5), ("Eb4", .5), ("F#4", 2)]
C_ = [("D5", 1), ("C5", .5), ("Bb4", .5), ("A4", 1), ("G4", .5), ("F#4", .5), ("G4", 1), ("A4", 1)]
order = [A_, B_, A_, C_, B_, C_, A_]
tt = 2.5
for ph in order:
    for note, beats in ph:
        d = beats * BEAT
        if tt > DUR - 3.5: break
        sig = pluck(F[note], min(2.4, d + 1.4), decay=0.9965 if F[note] < 400 else 0.995)
        add(sig, tt, pan=0.18 if note in ("A4", "D5", "C5") else -0.12, gain=0.55)
        tt += d
# yakuniy cho'zilgan nota
add(pluck(F["D4"], 3.2, 0.9975), DUR - 4.0, 0.0, 0.7)
add(pluck(F["D3"], 3.6, 0.9978), DUR - 4.0, 0.0, 0.55)

# ---- doira (maqsum): D T . T D . T .
def dum(): 
    n = int(.35 * SR); x = np.arange(n) / SR
    return np.sin(2 * np.pi * (60 + 90 * np.exp(-x * 22)) * x) * np.exp(-x * 11)
def tek():
    n = int(.12 * SR); x = np.arange(n) / SR
    s = rng.uniform(-1, 1, n); s = s - np.convolve(s, np.ones(8) / 8, mode="same")  # yuqori chastotali shovqin
    return s * np.exp(-x * 38) * 0.6
EIGHTH = BEAT / 2; pattern = {0: "D", 1: "T", 3: "T", 4: "D", 6: "T"}
bar = 8 * EIGHTH; k = 0
start = 5.0
while start + k * bar < DUR - 2.5:
    base = start + k * bar
    swell = 0.5 + 0.5 * np.clip((base - 5) / 14, 0, 1)
    for pos, kind in pattern.items():
        add(dum() * 0.30 * swell if kind == "D" else tek() * 0.22 * swell, base + pos * EIGHTH, 0.05)
    k += 1

# ---- sahna almashinuvida yengil "zang"
for t0 in (5.0, 11.0, 17.0, 23.0, 29.0, 33.4):
    n = int(2.2 * SR); x = np.arange(n) / SR
    bell = (np.sin(2 * np.pi * 1760 * x) + .6 * np.sin(2 * np.pi * 2349 * x) + .35 * np.sin(2 * np.pi * 3136 * x)) * np.exp(-x * 3.2)
    add(bell * 0.045, t0, 0.2)

# ---- ilovaning o'z "ting"i: bildirishnoma paydo bo'lganda (12.5 s)
if ting_path:
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", ting_path, "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"], capture_output=True).stdout
    tg = np.frombuffer(raw, dtype=np.float32).astype(np.float64)[: int(5.5 * SR)]
    add(tg * 0.9, 12.45, 0.0)

# ---- reverb (sintetik IR)
def ir(sec, seed):
    r = np.random.default_rng(seed); n = int(sec * SR); x = np.arange(n) / SR
    h = r.normal(size=n) * np.exp(-x * 2.6); h[0] = 0
    return h / np.sqrt(np.sum(h ** 2))
def conv(sig, h):
    n = len(sig) + len(h); m = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(sig, m) * np.fft.rfft(h, m), m)[: len(sig)]
wL = conv(L, ir(1.7, 1)); wR = conv(R, ir(1.7, 2))
L = L * 0.78 + wL * 0.30; R = R * 0.78 + wR * 0.30

# ---- master
fi = np.minimum(1, t / 1.2); fo = np.clip((DUR - t) / 2.8, 0, 1)
L *= fi * fo; R *= fi * fo
peak = max(np.abs(L).max(), np.abs(R).max()); g = 0.89 / peak
st = np.stack([L * g, R * g], axis=1)
pcm = (st * 32767).astype("<i2")
with wave.open(out_path, "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print("musiqa tayyor:", out_path, f"{DUR}s")

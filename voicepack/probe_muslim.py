"""muslim.uz namoz vaqtlari manbasini aniqlash (diagnostika)."""
import re, json, pathlib, urllib.request, urllib.parse

OUT = pathlib.Path("out"); OUT.mkdir(exist_ok=True)
UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
log = []

def get(url, n=3000000):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.headers.get("content-type", ""), r.read(n).decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001
        return getattr(e, "code", 0), "", str(e)

st, ct, js = get("https://www.muslim.uz/build/js/scripts.min.js")
log.append(f"scripts.min.js -> {st} len={len(js)}")
(OUT / "scripts.min.js").write_text(js[:900000])
for kw in ["prayer", "namaz", "namoz", "ajax", "getJSON", "url:", "region", "city", "geolocation", "bigdatacloud", "/site/", "/api"]:
    for m in list(re.finditer(kw, js, re.I))[:4]:
        s = max(0, m.start() - 160)
        log.append(f"[{kw}] ..." + js[s:m.start() + 260].replace("\n", " "))
(OUT / "log.txt").write_text("\n".join(log))
print("\n".join(log)[:6000])

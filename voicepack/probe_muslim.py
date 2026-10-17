"""old.muslim.uz va muslim.uz namoz jadvallarini o'rganish (diagnostika)."""
import re, json, pathlib, urllib.request, urllib.parse

OUT = pathlib.Path("out"); OUT.mkdir(exist_ok=True)
UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
log = []

def get(url, n=2000000):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, r.headers.get("content-type", ""), r.read(n).decode("utf-8", "replace"), r.geturl()
    except Exception as e:  # noqa: BLE001
        return getattr(e, "code", 0), "", str(e), url

def links_of(body, base):
    return sorted({urllib.parse.urljoin(base, l) for l in re.findall(r'(?:href|src|action)=["\']([^"\']+)["\']', body)})

seen = {}
for url in ["http://old.muslim.uz/", "https://old.muslim.uz/", "https://www.muslim.uz/c/prayer", "https://www.muslim.uz/oz/c/prayer", "https://www.muslim.uz/c/category/namoz"]:
    st, ct, body, final = get(url)
    log.append(f"GET {url} -> {st} {ct} final={final} len={len(body)}")
    if st == 200:
        name = re.sub(r'[^A-Za-z0-9]+', '_', url)[:60]
        (OUT / f"{name}.html").write_text(body[:400000])
        ls = links_of(body, final)
        rel = [l for l in ls if re.search(r'namaz|namoz|vaqt|taqvim|prayer|time|region|city|month|calendar', l, re.I)]
        log.append("  relevant: " + json.dumps(rel[:40], ensure_ascii=False))
        # jadval izlari
        for kw in ["Бомдод", "Bomdod", "Тонг", "Tong", "<table", "select", "option"]:
            m = re.search(kw, body)
            if m:
                s = max(0, m.start() - 150)
                log.append(f"  [{kw}] " + re.sub(r'\s+', ' ', body[s:m.start() + 400]))
(OUT / "log.txt").write_text("\n".join(log))
print("\n".join(log)[:7000])

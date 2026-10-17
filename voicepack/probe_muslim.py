"""muslim.uz namoz vaqtlari manbasini aniqlash (diagnostika)."""
import re, json, pathlib, urllib.request, urllib.parse

OUT = pathlib.Path("out"); OUT.mkdir(exist_ok=True)
UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
log = []

def get(url, n=400000):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            data = r.read(n)
            return r.status, r.headers.get("content-type", ""), data.decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001
        return getattr(e, "code", 0), "", str(e)

for base in ["https://www.muslim.uz/oz", "https://muslim.uz/oz", "https://muslim.uz/", "https://www.muslim.uz/oz/namoz-vaqtlari"]:
    st, ct, body = get(base)
    log.append(f"GET {base} -> {st} {ct} len={len(body)}")
    if st == 200 and len(body) > 2000:
        (OUT / "home.html").write_text(body[:200000])
        links = sorted(set(re.findall(r'(?:href|src)=["\']([^"\']+)["\']', body)))
        log.append("links: " + str(len(links)))
        rel = [l for l in links if re.search(r'namoz|vaqt|taqvim|prayer|time|api', l, re.I)]
        log.append("relevant links: " + json.dumps(rel[:60], ensure_ascii=False))
        scripts = [urllib.parse.urljoin(base, l) for l in links if l.endswith(".js") or ".js?" in l]
        api_hits = set()
        for s in scripts[:15]:
            st2, _, js = get(s, 1500000)
            if st2 == 200:
                for m in re.findall(r'["\'`](/?(?:api|v1|v2)/[A-Za-z0-9_/\-\.\?=&{}$]+)["\'`]', js):
                    api_hits.add(m)
                for m in re.findall(r'https?://[A-Za-z0-9\.\-/_]*(?:api|namoz|vaqt|prayer)[A-Za-z0-9\.\-/_\?=&]*', js):
                    api_hits.add(m)
        log.append("api hints: " + json.dumps(sorted(api_hits)[:80], ensure_ascii=False))
        break

for path in ["/oz/namoz-vaqtlari", "/oz/namoz", "/oz/prayer-times", "/api/prayer-times", "/api/namoz-vaqtlari"]:
    st, ct, body = get("https://muslim.uz" + path)
    log.append(f"PROBE {path} -> {st} {ct} len={len(body)} :: {body[:160].replace(chr(10),' ')}")

(OUT / "log.txt").write_text("\n".join(log))
print("\n".join(log))

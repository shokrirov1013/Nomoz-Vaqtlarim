"""Promo videoni kadrma-kadr (deterministik) chizadi.
Ishlatish:
  python3 render.py preview 3 6.5 13.5      # tanlangan soniyalardan PNG (frames_preview/)
  python3 render.py video out.mp4           # to'liq video (ovozsiz), 30 fps, 38 soniya
"""
import asyncio, pathlib, subprocess, sys
from playwright.async_api import async_playwright

HERE = pathlib.Path(__file__).resolve().parent
CHROME = "/opt/pw-browsers/chromium-1194/chrome-linux/chrome"
FPS, DURATION = 30, 38.0


async def open_page(p):
    b = await p.chromium.launch(executable_path=CHROME, args=["--no-sandbox", "--font-render-hinting=none", "--disable-lcd-text"])
    page = await b.new_page(viewport={"width": 1080, "height": 1920}, device_scale_factor=1)
    await page.goto(f"file://{HERE}/index.html")
    await page.evaluate("document.fonts.ready")
    await page.wait_for_timeout(600)
    return b, page


async def preview(times):
    out = HERE / "frames_preview"; out.mkdir(exist_ok=True)
    async with async_playwright() as p:
        b, page = await open_page(p)
        for t in times:
            await page.evaluate(f"render({t})")
            await page.screenshot(path=str(out / f"t{t:05.2f}.png"))
        await b.close()
    print("tayyor:", out)


async def video(dst):
    n = int(FPS * DURATION)
    async with async_playwright() as p:
        b, page = await open_page(p)
        ff = subprocess.Popen(
            ["ffmpeg", "-y", "-loglevel", "error", "-f", "image2pipe", "-framerate", str(FPS), "-c:v", "mjpeg", "-i", "-",
             "-c:v", "libx264", "-preset", "medium", "-crf", "17", "-pix_fmt", "yuv420p", "-movflags", "+faststart", dst],
            stdin=subprocess.PIPE)
        for i in range(n):
            await page.evaluate(f"render({i / FPS})")
            ff.stdin.write(await page.screenshot(type="jpeg", quality=93))
            if i % 120 == 0:
                print(f"{i}/{n}", flush=True)
        ff.stdin.close(); ff.wait()
        await b.close()
    print("video:", dst)


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "preview":
        asyncio.run(preview([float(x) for x in sys.argv[2:]]))
    else:
        asyncio.run(video(sys.argv[2]))

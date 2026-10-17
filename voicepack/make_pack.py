"""Tayyor ovoz paketini yaratadi: voicepack/phrases.json -> app/src/main/res/raw/v_<til>_<kalit>.mp3"""
import asyncio
import json
import pathlib
import sys

import edge_tts

ROOT = pathlib.Path(__file__).resolve().parent.parent
RAW = ROOT / "app/src/main/res/raw"
RAW.mkdir(parents=True, exist_ok=True)
data = json.loads((ROOT / "voicepack/phrases.json").read_text(encoding="utf-8"))


async def one(lang: str, key: str, text: str, voice: str) -> bool:
    path = RAW / f"v_{lang}_{key}.mp3"
    for attempt in range(3):
        try:
            await edge_tts.Communicate(text, voice, rate=data["rate"], pitch=data["pitch"], volume=data["volume"]).save(str(path))
            if path.exists() and path.stat().st_size > 2000:
                return True
        except Exception as e:  # noqa: BLE001
            print("qayta urinish", lang, key, attempt, e)
        await asyncio.sleep(2)
    return False


async def main() -> int:
    bad = []
    for lang in ("uz", "ru"):
        voice = data["voices"][lang]
        for key, text in data[lang].items():
            if not await one(lang, key, text, voice):
                bad.append(f"{lang}/{key}")
    total = len(list(RAW.glob("v_*.mp3")))
    print("yaratildi:", total, "xato:", bad)
    return 1 if bad else 0


sys.exit(asyncio.run(main()))

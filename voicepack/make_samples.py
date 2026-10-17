"""Ovoz tanlash uchun namunalar: bir xil ma'noli ibora turli til va ovozlarda (yumshoq ohangda)."""
import asyncio
import json
import pathlib

import edge_tts

OUT = pathlib.Path("out")
OUT.mkdir(exist_ok=True)

UZ = "Peshin vaqti kirdi. Marhamat, namozingizni o'qib oling."
TR = "Öğle namazının vakti girdi. Buyurun, namazınızı kılın."
KK = "Бесін намазының уақыты кірді. Намазыңызды оқып алыңыз."
AZ = "Zöhr namazının vaxtı daxil oldu. Buyurun, namazınızı qılın."
AR = "حَانَ الْآنَ وَقْتُ صَلَاةِ الظُّهْرِ. تَفَضَّلْ، أَدِّ صَلَاتَكَ."
RU = "Наступило время полуденной молитвы. Пожалуйста, совершите намаз."

# (fayl nomi, ovoz, matn, tezlik)
SAMPLES = [
    ("01-uzbek-Madina-ayol", "uz-UZ-MadinaNeural", UZ, "-10%"),
    ("02-uzbek-Madina-ayol-sekin", "uz-UZ-MadinaNeural", UZ, "-22%"),
    ("03-uzbek-Sardor-erkak", "uz-UZ-SardorNeural", UZ, "-10%"),
    ("04-turk-Emel-ayol", "tr-TR-EmelNeural", TR, "-10%"),
    ("05-turk-Ahmet-erkak", "tr-TR-AhmetNeural", TR, "-10%"),
    ("06-qozoq-Aigul-ayol", "kk-KZ-AigulNeural", KK, "-10%"),
    ("07-qozoq-Daulet-erkak", "kk-KZ-DauletNeural", KK, "-10%"),
    ("08-ozarbayjon-Banu-ayol", "az-AZ-BanuNeural", AZ, "-10%"),
    ("09-ozarbayjon-Babek-erkak", "az-AZ-BabekNeural", AZ, "-10%"),
    ("10-arab-Zariyah-ayol", "ar-SA-ZariyahNeural", AR, "-10%"),
    ("11-arab-Hamed-erkak", "ar-SA-HamedNeural", AR, "-10%"),
    ("12-rus-Svetlana-ayol", "ru-RU-SvetlanaNeural", RU, "-10%"),
]


async def main() -> None:
    manifest = []
    for name, voice, text, rate in SAMPLES:
        path = OUT / f"{name}.mp3"
        try:
            await edge_tts.Communicate(text, voice, rate=rate, pitch="-3Hz", volume="-5%").save(str(path))
            ok = path.exists() and path.stat().st_size > 2000
        except Exception as e:  # noqa: BLE001
            print("XATO", name, voice, e)
            ok = False
        manifest.append({"file": path.name, "voice": voice, "rate": rate, "ok": ok})
        print("OK " if ok else "XATO", name)
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=1))


asyncio.run(main())

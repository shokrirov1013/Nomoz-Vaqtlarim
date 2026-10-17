"""PowerPoint taqdimotiga HARAKATLANUVCHI animatsiya va slayd o'tishlarini qo'shadi (OOXML: <p:transition>, <p:timing>).

pptxgenjs animatsiyani qo'llab-quvvatlamaydi, shuning uchun tayyor .pptx ni ochib, har bir slaydga
o'tish effekti va elementlar uchun kirish effektlari (Fade, Fly In, Zoom, Wipe) qo'shamiz.
Hammasi slayd boshlanishi bilan O'ZI ishga tushadi (bosish shart emas) va ketma-ket kechikishlar bilan o'ynaydi.

Ishlatish: python3 animate_deck.py kirish.pptx natija.pptx
"""
import re
import sys
import zipfile
from lxml import etree

SRC, DST = sys.argv[1], sys.argv[2]

# ---- slayd o'tishlari (PowerPoint'ning standart effektlari)
TRANSITIONS = {
    1: '<p:fade/>',
    2: '<p:push dir="l"/>',
    3: '<p:fade/>',
    4: '<p:push dir="l"/>',
    5: '<p:wipe dir="r"/>',
    6: '<p:cover dir="r"/>',
    7: '<p:push dir="l"/>',
    8: '<p:zoom dir="in"/>',
}

# ---- animatsiya rejasi: (nom yoki #id, effekt, kechikish ms, davomiylik ms)
def stagger(names, effect, start, step, dur=600):
    return [(n, effect, start + i * step, dur) for i, n in enumerate(names)]

PLAN = {
    1: [("#3", "fly_l", 0, 800), ("#2", "fade", 100, 1400), ("ilova-ikonkasi", "zoom", 200, 1000),
        ("#4", "fade", 800, 800), ("muallif", "fade", 1500, 700)],
    2: [("#2", "fly_l", 0, 700)]
       + sum([[(f"karta-{i}", "zoom", 350 + (i - 1) * 450, 600), (f"nega-{i}-doira", "zoom", 600 + (i - 1) * 450, 500),
               (f"nega-{i}-ikonka", "zoom", 700 + (i - 1) * 450, 500), (f"sarlavha-{i}", "fade", 850 + (i - 1) * 450, 500),
               (f"matn-{i}", "fade", 1050 + (i - 1) * 450, 600)] for i in (1, 2, 3)], []),
    3: [("#2", "fly_l", 0, 700)]
       + sum([[(f"imkoniyat-{i}", "fly_b", 300 + (i - 1) * 260, 600), (f"imkon-{i}-doira", "zoom", 550 + (i - 1) * 260, 450),
               (f"imkon-{i}-ikonka", "zoom", 650 + (i - 1) * 260, 450), (f"imkon-sarlavha-{i}", "fade", 750 + (i - 1) * 260, 450),
               (f"imkon-matn-{i}", "fade", 900 + (i - 1) * 260, 450)] for i in range(1, 7)], []),
    4: [("#2", "fly_l", 0, 700), ("vaqt-chizigi", "wipe_l", 300, 1800)]
       + sum([[(f"vaqt-{i}", "fade", 500 + (i - 1) * 520, 450), (f"tugun-{i}", "zoom", 600 + (i - 1) * 520, 400),
               (f"tavsif-{i}", "fade", 800 + (i - 1) * 520, 500)] for i in range(1, 6)], [])
       + [("bekor-karta", "fly_b", 3500, 700), ("bajarildi-doira", "zoom", 3900, 500), ("bajarildi-ikonka", "zoom", 4000, 500),
          ("bekor-matn", "fade", 4200, 600)],
    5: [("#2", "fly_l", 0, 700)]
       + sum([[(f"zanjir-{i}", "fly_l", 300 + (i - 1) * 500, 600), (f"raqam-doira-{i}", "zoom", 600 + (i - 1) * 500, 450),
               (f"raqam-{i}", "zoom", 700 + (i - 1) * 500, 450), (f"zanjir-sarlavha-{i}", "fade", 800 + (i - 1) * 500, 450),
               (f"zanjir-matn-{i}", "fade", 950 + (i - 1) * 500, 450)] for i in (1, 2, 3)], [])
       + [("korsatkich-1", "fly_r", 2000, 700), ("korsatkich-1-son", "zoom", 2500, 600), ("korsatkich-1-matn", "fade", 2800, 500),
          ("korsatkich-2", "fly_r", 3100, 700), ("korsatkich-2-son", "zoom", 3600, 600), ("korsatkich-2-matn", "fade", 3900, 500),
          ("izoh", "fade", 4300, 600)],
    6: [("#2", "fly_l", 0, 700)]
       + sum([[(f"nuqta-{i}", "zoom", 300 + (i - 1) * 450, 400), (f"dizayn-sarlavha-{i}", "fly_l", 400 + (i - 1) * 450, 550),
               (f"dizayn-matn-{i}", "fade", 650 + (i - 1) * 450, 500)] for i in range(1, 5)], [])
       + [("kadr-vaqtlar", "fly_b", 1000, 900), ("kadr-eslatma", "fly_b", 1500, 900)],
    7: [("#2", "fly_l", 0, 700)]
       + sum([[(f"ishonch-karta-{i}", "zoom", 350 + (i - 1) * 500, 600), (f"ishonch-{i}-doira", "zoom", 650 + (i - 1) * 500, 500),
               (f"ishonch-{i}-ikonka", "zoom", 750 + (i - 1) * 500, 500), (f"ishonch-sarlavha-{i}", "fade", 900 + (i - 1) * 500, 500),
               (f"ishonch-matn-{i}", "fade", 1100 + (i - 1) * 500, 600)] for i in (1, 2, 3)], []),
    8: [("#2", "fly_l", 0, 700), ("bepul", "fade", 300, 600),
        ("havola-karta", "fly_l", 600, 700), ("github-ikonka", "zoom", 1000, 450), ("havola", "fade", 1100, 500), ("havola-izoh", "fade", 1300, 500),
        ("muallif-karta", "fly_l", 1500, 700), ("muallif-doira", "zoom", 1900, 450), ("muallif-ikonka", "zoom", 2000, 450), ("muallif-matn", "fade", 2100, 600),
        ("qr-karta", "zoom", 1200, 800), ("qr-kod", "fade", 1800, 700), ("qr-izoh", "fade", 2300, 500)],
}

NS = ('xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
      'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
      'xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"')


class Ids:
    def __init__(self, start=5):
        self.n = start

    def next(self):
        self.n += 1
        return self.n


def set_visible(ids, spid):
    return (f'<p:set><p:cBhvr><p:cTn id="{ids.next()}" dur="1" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst></p:cTn>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl><p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst></p:cBhvr>'
            f'<p:to><p:strVal val="visible"/></p:to></p:set>')


def anim_num(ids, spid, attr, v0, v1, dur):
    def val(v):
        return f'<p:val><p:fltVal val="{v}"/></p:val>' if isinstance(v, (int, float)) else f'<p:val><p:strVal val="{v}"/></p:val>'
    return (f'<p:anim calcmode="lin" valueType="num"><p:cBhvr><p:cTn id="{ids.next()}" dur="{dur}" fill="hold"/>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl><p:attrNameLst><p:attrName>{attr}</p:attrName></p:attrNameLst></p:cBhvr>'
            f'<p:tavLst><p:tav tm="0">{val(v0)}</p:tav><p:tav tm="100000">{val(v1)}</p:tav></p:tavLst></p:anim>')


def anim_effect(ids, spid, flt, dur):
    return (f'<p:animEffect transition="in" filter="{flt}"><p:cBhvr><p:cTn id="{ids.next()}" dur="{dur}"/>'
            f'<p:tgtEl><p:spTgt spid="{spid}"/></p:tgtEl></p:cBhvr></p:animEffect>')


def effect_xml(ids, spid, kind, delay, dur, node, grp):
    preset = {"fade": (10, 0), "fly_b": (2, 4), "fly_l": (2, 8), "fly_r": (2, 2), "zoom": (53, 16), "wipe_l": (22, 8)}[kind]
    gattr = f' grpId="{grp}"' if grp is not None else ""
    head = (f'<p:par><p:cTn id="{ids.next()}" presetID="{preset[0]}" presetClass="entr" presetSubtype="{preset[1]}" '
            f'fill="hold"{gattr} nodeType="{node}"><p:stCondLst><p:cond delay="{delay}"/></p:stCondLst><p:childTnLst>')
    body = set_visible(ids, spid)
    if kind == "fade":
        body += anim_effect(ids, spid, "fade", dur)
    elif kind == "fly_b":
        body += anim_num(ids, spid, "ppt_x", "#ppt_x", "#ppt_x", dur) + anim_num(ids, spid, "ppt_y", "1+#ppt_h/2", "#ppt_y", dur)
    elif kind == "fly_l":
        body += anim_num(ids, spid, "ppt_x", "0-#ppt_w/2", "#ppt_x", dur) + anim_num(ids, spid, "ppt_y", "#ppt_y", "#ppt_y", dur)
    elif kind == "fly_r":
        body += anim_num(ids, spid, "ppt_x", "1+#ppt_w/2", "#ppt_x", dur) + anim_num(ids, spid, "ppt_y", "#ppt_y", "#ppt_y", dur)
    elif kind == "zoom":
        body += anim_num(ids, spid, "ppt_w", 0, "#ppt_w", dur) + anim_num(ids, spid, "ppt_h", 0, "#ppt_h", dur) + anim_effect(ids, spid, "fade", dur)
    elif kind == "wipe_l":
        body += anim_effect(ids, spid, "wipe(left)", dur)
    return head + body + "</p:childTnLst></p:cTn></p:par>"


def timing_xml(items, shapes):
    """items: [(spid, kind, delay, dur)]; shapes: {spid: 'sp'|'pic'}"""
    ids = Ids(4)
    effects = []
    bld = []
    for k, (spid, kind, delay, dur) in enumerate(items):
        is_sp = shapes.get(spid) == "sp"
        grp = 0 if is_sp else None
        effects.append(effect_xml(ids, spid, kind, delay, dur, "afterEffect" if k == 0 else "withEffect", grp))
        if is_sp:
            bld.append(f'<p:bldP spid="{spid}" grpId="0" animBg="1"/>')
    return (
        f'<p:timing {NS}><p:tnLst><p:par><p:cTn id="1" dur="indefinite" restart="never" nodeType="tmRoot"><p:childTnLst>'
        '<p:seq concurrent="1" nextAc="seek"><p:cTn id="2" dur="indefinite" nodeType="mainSeq"><p:childTnLst>'
        '<p:par><p:cTn id="3" fill="hold"><p:stCondLst><p:cond delay="indefinite"/>'
        '<p:cond evt="onBegin" delay="0"><p:tn val="2"/></p:cond></p:stCondLst><p:childTnLst>'
        '<p:par><p:cTn id="4" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst>'
        + "".join(effects) +
        '</p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:par>'
        '</p:childTnLst></p:cTn>'
        '<p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst>'
        '<p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:nextCondLst></p:seq>'
        '</p:childTnLst></p:cTn></p:par></p:tnLst>'
        + (f'<p:bldLst>{"".join(bld)}</p:bldLst>' if bld else "") + '</p:timing>')


def process(xml, idx):
    # slayd raqami elementi ID'si boshqa elementnikiga to'qnashmasligi uchun
    xml = xml.replace('<p:cNvPr id="25" name="Slide Number Placeholder 0"', '<p:cNvPr id="9999" name="Slide Number Placeholder 0"')
    # nom -> id, tur
    names = {}
    shapes = {}
    root = etree.fromstring(xml.encode("utf-8"))
    pns = {"p": "http://schemas.openxmlformats.org/presentationml/2006/main"}
    for el in root.iterfind(".//p:spTree/*", pns):
        tag = etree.QName(el).localname
        if tag not in ("sp", "pic"):
            continue
        c = el.find(".//p:cNvPr", pns)
        sid, nm = c.get("id"), c.get("name")
        shapes[sid] = "pic" if tag == "pic" else "sp"
        if nm and nm not in names:
            names[nm] = sid
    items = []
    for key, kind, delay, dur in PLAN[idx]:
        sid = key[1:] if key.startswith("#") else names.get(key)
        if sid is None:
            raise SystemExit(f"slayd {idx}: '{key}' topilmadi")
        items.append((sid, kind, delay, dur))
    tr = f'<p:transition {NS} spd="med">{TRANSITIONS[idx]}</p:transition>'
    tm = timing_xml(items, shapes)
    for frag in (tr, tm):
        etree.fromstring(frag)  # to'g'ri XML ekanini tekshirish
    # clrMapOvr dan keyin: avval transition, keyin timing
    insert = tr.replace(f" {NS}", "") + tm.replace(f" {NS}", "")
    assert "</p:clrMapOvr></p:sld>" in xml, f"slayd {idx}: kutilgan tuzilma emas"
    return xml.replace("</p:clrMapOvr></p:sld>", "</p:clrMapOvr>" + insert + "</p:sld>")


zin = zipfile.ZipFile(SRC)
zout = zipfile.ZipFile(DST, "w", zipfile.ZIP_DEFLATED)
count = 0
for item in zin.infolist():
    data = zin.read(item.filename)
    m = re.fullmatch(r"ppt/slides/slide(\d+)\.xml", item.filename)
    if m and int(m.group(1)) in PLAN:
        data = process(data.decode("utf-8"), int(m.group(1))).encode("utf-8")
        count += 1
    zout.writestr(item, data)
zout.close()
print(f"animatsiya qo'shildi: {count} slayd -> {DST}")

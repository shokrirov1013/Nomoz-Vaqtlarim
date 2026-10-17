// Namoz Vaqtlarim — qisqa taqdimot (7 slayd). Ishlatish: node build_deck.js <assets_papka> <natija.pptx>
const pptxgen = require("pptxgenjs");
const React = require("react");
const ReactDOMServer = require("react-dom/server");
const sharp = require("sharp");
const fa = require("react-icons/fa");
const SKILL = "/root/.claude/skills/synced/5818fedd-d64c-4ec5-aa90-20a31542b365_25d81cc7-74c9-4be4-8308-ac3c13c040bf/pptx";
const { applyTheme } = require(`${SKILL}/scripts/apply_theme.js`);

const ASSETS = process.argv[2];
const OUT = process.argv[3];

const THEME = {
  name: "Namoz Zumrad",
  headFontFace: "Cambria",
  bodyFontFace: "Calibri",
  colors: {
    dk1: "06231F", lt1: "FFFFFF", dk2: "0B3B34", lt2: "F3EFE3",
    accent1: "E3BC6A", accent2: "2FB38F", accent3: "8FD6C0", accent4: "C9973F", accent5: "1E6F5E", accent6: "F1D58C",
    hlink: "F1D58C", folHlink: "8FD6C0",
  },
};

async function icon(name, color, size = 256) {
  const svg = ReactDOMServer.renderToStaticMarkup(React.createElement(fa[name], { color: `#${color}`, size: String(size) }));
  const buf = await sharp(Buffer.from(svg)).png().toBuffer();
  return "image/png;base64," + buf.toString("base64");
}

(async () => {
  const pres = new pptxgen();
  pres.layout = "LAYOUT_WIDE"; // 13.33 x 7.5
  pres.title = "Namoz Vaqtlarim";
  pres.author = "Shokirov";
  pres.theme = { headFontFace: THEME.headFontFace, bodyFontFace: THEME.bodyFontFace };
  const C = pres.SchemeColor;

  // ---- layoutlar
  pres.defineSlideMaster({
    title: "TITLE_DARK",
    background: { color: C.text1 },
    objects: [
      { placeholder: { options: { name: "title", type: "title", x: 0.8, y: 2.2, w: 7.4, h: 1.4, fontSize: 60, bold: true, color: C.background2, align: "left", valign: "bottom", margin: 0 }, text: "" } },
      { placeholder: { options: { name: "body", type: "body", x: 0.8, y: 3.85, w: 6.9, h: 1.1, fontSize: 24, color: C.accent3, align: "left", valign: "top", margin: 0 }, text: "" } },
    ],
  });
  pres.defineSlideMaster({
    title: "CONTENT_DARK",
    background: { color: C.text1 },
    objects: [
      { placeholder: { options: { name: "title", type: "title", x: 0.7, y: 0.45, w: 11.9, h: 0.95, fontSize: 38, bold: true, color: C.background2, align: "left", valign: "middle", margin: 0 }, text: "" } },
      { text: { text: "Namoz Vaqtlarim", options: { x: 0.7, y: 7.0, w: 5, h: 0.3, fontSize: 11, color: C.accent3, margin: 0, isTextBox: true } } },
    ],
    slideNumber: { x: 12.1, y: 7.0, w: 0.5, h: 0.3, color: C.accent3, fontSize: 11, align: "right" },
  });

  const gold = "E3BC6A", dark = "06231F";
  const ic = {
    pin: await icon("FaMapMarkerAlt", dark), bell: await icon("FaBell", dark), moon: await icon("FaMoon", dark),
    cal: await icon("FaCalendarAlt", dark), book: await icon("FaBookOpen", dark), lang: await icon("FaLanguage", dark),
    clock: await icon("FaClock", dark), check: await icon("FaCheckCircle", dark), chart: await icon("FaChartBar", dark),
    heart: await icon("FaHeart", dark), github: await icon("FaGithub", "E3BC6A"),
  };
  const sh = () => ({ type: "outer", color: "000000", blur: 12, offset: 3, angle: 90, opacity: 0.3 });

  function iconBadge(s, img, x, y, d = 0.85, name = "belgi") {
    s.addShape(pres.ShapeType.ellipse, { x, y, w: d, h: d, fill: { color: C.accent1 }, line: { type: "none" }, objectName: `${name}-doira` });
    const p = d * 0.5;
    s.addImage({ data: img, x: x + (d - p) / 2, y: y + (d - p) / 2, w: p, h: p, objectName: `${name}-ikonka`, altText: name });
  }
  function card(s, x, y, w, h, name) {
    s.addShape(pres.ShapeType.roundRect, { x, y, w, h, rectRadius: 0.18, fill: { color: C.text2 }, line: { color: C.accent5, width: 1 }, shadow: sh(), objectName: name });
  }

  // ===== 1. Sarlavha
  pres.addSection({ title: "Kirish" });
  let s = pres.addSlide({ masterName: "TITLE_DARK", sectionTitle: "Kirish" });
  s.addShape(pres.ShapeType.ellipse, { x: 8.0, y: 0.9, w: 5.0, h: 5.0, fill: { color: C.accent1, transparency: 88 }, line: { type: "none" }, objectName: "yorug-dog" });
  s.addText("Namoz Vaqtlarim", { placeholder: "title" });
  s.addText("Namozni vaqtida o'qing — eslatma o'zi aytadi", { placeholder: "body" });
  s.addImage({ path: `${ASSETS}/icon.png`, x: 8.45, y: 1.35, w: 4.1, h: 4.1, shadow: sh(), objectName: "ilova-ikonkasi", altText: "Namoz Vaqtlarim ilova ikonkasi" });
  s.addText("Shokirov tomonidan yaratilgan · 18 yoshli talaba", { x: 0.8, y: 6.35, w: 7.5, h: 0.4, fontSize: 16, color: C.background2, margin: 0, isTextBox: true, objectName: "muallif" });
  s.addNotes("Salom! Bu — Namoz Vaqtlarim: namoz vaqtlarini aniq ko'rsatadigan, yumshoq ovozli eslatma beradigan Android ilovasi. Uni 18 yoshli talaba Shokirov o'z istaklari yo'lida yaratgan.");

  // ===== 2. Nega kerak
  pres.addSection({ title: "Asosiy g'oya" });
  s = pres.addSlide({ masterName: "CONTENT_DARK", sectionTitle: "Asosiy g'oya" });
  s.addText("Nega bu ilova kerak", { placeholder: "title" });
  const why = [
    ["pin", "Aniq vaqt", "Joylashuvingiz bo'yicha hisoblangan va idora (muslim.uz) vaqtlariga moslangan namoz vaqtlari."],
    ["bell", "Yumshoq eslatma", "Namozdan 5 daqiqa oldin va vaqtida: avval «ting», keyin muloyim ovozli xabar."],
    ["chart", "Tartib va kuzatuv", "«Bajarildi» belgisi, tarix va statistika. Faqat siz o'zingiz belgilagan ma'lumotlar."],
  ];
  why.forEach(([k, h, b], i) => {
    const x = 0.7 + i * 4.1;
    card(s, x, 1.9, 3.7, 4.4, `karta-${i + 1}`);
    iconBadge(s, ic[k], x + 0.4, 2.3, 1.0, `nega-${i + 1}`);
    s.addText(h, { x: x + 0.4, y: 3.6, w: 2.9, h: 0.6, fontSize: 24, bold: true, color: C.accent6, margin: 0, isTextBox: true, objectName: `sarlavha-${i + 1}` });
    s.addText(b, { x: x + 0.4, y: 4.3, w: 2.9, h: 1.7, fontSize: 16, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: `matn-${i + 1}` });
  });
  s.addNotes("Uch asosiy fikr: aniq vaqt, muloyim eslatma va tartib. Ilova namozni o'qiganingizni o'zi bilmaydi — faqat siz bosgan «Bajarildi» saqlanadi.");

  // ===== 3. Imkoniyatlar
  pres.addSection({ title: "Imkoniyatlar" });
  s = pres.addSlide({ masterName: "CONTENT_DARK", sectionTitle: "Imkoniyatlar" });
  s.addText("Asosiy imkoniyatlar", { placeholder: "title" });
  const feats = [
    ["clock", "Namoz vaqtlari", "Besh vaqt va Quyosh chiqishi, har kuni yangilanadi"],
    ["bell", "Qayta eslatma", "Belgilanmasa 10 daqiqada bir, jami 3 marta"],
    ["moon", "Bomdod va Tahajjud", "Budilnik; kunlarni o'zingiz tanlaysiz"],
    ["cal", "Taqvim", "Milodiy va Hijriy, shaxsiy eslatmalar"],
    ["book", "Suralar", "Oyatal Kursi, Fotiha, Ixlos, Falaq, Nos"],
    ["lang", "Lotin va Kirill", "Kattalar uchun katta matn"],
  ];
  feats.forEach(([k, h, b], i) => {
    const x = 0.7 + (i % 3) * 4.1, y = 1.75 + Math.floor(i / 3) * 2.6;
    card(s, x, y, 3.7, 2.3, `imkoniyat-${i + 1}`);
    iconBadge(s, ic[k], x + 0.35, y + 0.35, 0.8, `imkon-${i + 1}`);
    s.addText(h, { x: x + 1.3, y: y + 0.35, w: 2.2, h: 0.8, fontSize: 20, bold: true, color: C.accent6, valign: "middle", margin: 0, isTextBox: true, objectName: `imkon-sarlavha-${i + 1}` });
    s.addText(b, { x: x + 0.35, y: y + 1.3, w: 3.0, h: 0.8, fontSize: 15, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: `imkon-matn-${i + 1}` });
  });
  s.addNotes("Ilovaning oltita asosiy imkoniyati. Hammasi bitta ilovada, internet bo'lmaganda ham asosiy qismi ishlaydi.");

  // ===== 4. Eslatma zanjiri
  pres.addSection({ title: "Eslatma" });
  s = pres.addSlide({ masterName: "CONTENT_DARK", sectionTitle: "Eslatma" });
  s.addText("Eslatma qanday ishlaydi", { placeholder: "title" });
  s.addShape(pres.ShapeType.line, { x: 1.4, y: 3.45, w: 10.5, h: 0, line: { color: C.accent5, width: 3 }, objectName: "vaqt-chizigi" });
  const steps = [["−5 daq.", "Ting va «Iltimos, o'qib oling»"], ["0", "Namoz vaqti: ting va ovozli xabar"], ["+10", "1-qayta eslatma"], ["+20", "2-qayta eslatma"], ["+30", "Oxirgi eslatma"]];
  steps.forEach(([t, d], i) => {
    const cx = 1.4 + i * 2.625;
    s.addText(t, { x: cx - 1.1, y: 2.2, w: 2.2, h: 0.8, fontSize: 32, bold: true, color: C.accent1, align: "center", valign: "middle", margin: 0, isTextBox: true, objectName: `vaqt-${i + 1}` });
    s.addShape(pres.ShapeType.ellipse, { x: cx - 0.22, y: 3.23, w: 0.44, h: 0.44, fill: { color: i === 1 ? C.accent1 : C.text1 }, line: { color: C.accent1, width: 3 }, objectName: `tugun-${i + 1}` });
    s.addText(d, { x: cx - 1.25, y: 3.95, w: 2.5, h: 1.0, fontSize: 16, color: C.background2, align: "center", valign: "top", margin: 0, isTextBox: true, objectName: `tavsif-${i + 1}` });
  });
  card(s, 0.7, 5.35, 11.9, 1.3, "bekor-karta");
  iconBadge(s, ic.check, 1.05, 5.55, 0.9, "bajarildi");
  s.addText("«Bajarildi» bosilsa, shu namozning qolgan eslatmalari darhol bekor bo'ladi. Boshqa namozlarga tegilmaydi.", { x: 2.2, y: 5.45, w: 10.1, h: 1.1, fontSize: 18, color: C.background2, valign: "middle", margin: 0, isTextBox: true, objectName: "bekor-matn" });
  s.addNotes("Har bir namoz uchun alohida hisoblagich. Telefon qayta yoqilsa, eslatmalar o'zi tiklanadi.");

  // ===== 5. Manba
  pres.addSection({ title: "Aniqlik" });
  s = pres.addSlide({ masterName: "CONTENT_DARK", sectionTitle: "Aniqlik" });
  s.addText("Vaqtlar qayerdan olinadi", { placeholder: "title" });
  const chain = [["1", "islom.uz", "Xizmat ishlasa, shahar jadvali"], ["2", "muslim.uz bilan sinxron hisob", "O'zbekiston Musulmonlari idorasi vaqtlariga moslangan"], ["3", "Telefondagi hisob", "Internet bo'lmaganda ham ishlaydi"]];
  chain.forEach(([n, h, b], i) => {
    const y = 1.8 + i * 1.6;
    card(s, 0.7, y, 6.4, 1.4, `zanjir-${i + 1}`);
    s.addShape(pres.ShapeType.ellipse, { x: 1.0, y: y + 0.3, w: 0.8, h: 0.8, fill: { color: C.accent1 }, line: { type: "none" }, objectName: `raqam-doira-${i + 1}` });
    s.addText(n, { x: 1.0, y: y + 0.3, w: 0.8, h: 0.8, fontSize: 26, bold: true, color: C.text1, align: "center", valign: "middle", margin: 0, isTextBox: true, objectName: `raqam-${i + 1}` });
    s.addText(h, { x: 2.1, y: y + 0.18, w: 4.8, h: 0.55, fontSize: 19, bold: true, color: C.accent6, valign: "middle", margin: 0, isTextBox: true, objectName: `zanjir-sarlavha-${i + 1}` });
    s.addText(b, { x: 2.1, y: y + 0.75, w: 4.8, h: 0.5, fontSize: 15, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: `zanjir-matn-${i + 1}` });
  });
  card(s, 7.5, 1.8, 5.1, 2.3, "korsatkich-1");
  s.addText("0 daq.", { x: 7.8, y: 1.95, w: 4.5, h: 1.1, fontSize: 60, bold: true, color: C.accent1, valign: "middle", margin: 0, isTextBox: true, objectName: "korsatkich-1-son" });
  s.addText("Quyosh, Peshin, Asr: idora vaqti bilan farq (Toshkent, sinov kuni)", { x: 7.8, y: 3.1, w: 4.5, h: 0.85, fontSize: 15, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: "korsatkich-1-matn" });
  card(s, 7.5, 4.4, 5.1, 2.3, "korsatkich-2");
  s.addText("−3 · +4 · +3", { x: 7.8, y: 4.55, w: 4.5, h: 1.1, fontSize: 44, bold: true, color: C.accent1, valign: "middle", margin: 0, isTextBox: true, objectName: "korsatkich-2-son" });
  s.addText("Bomdod · Shom · Xufton: idora «ehtiyot» daqiqalariga moslama", { x: 7.8, y: 5.7, w: 4.5, h: 0.85, fontSize: 15, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: "korsatkich-2-matn" });
  s.addText("Boshqa shaharlar uchun idora jadvali berilmaydi: joylashuvga aniq hisob va Toshkentdan olingan moslama ishlatiladi.", { x: 0.7, y: 6.5, w: 6.6, h: 0.45, fontSize: 12, italic: true, color: C.accent3, margin: 0, isTextBox: true, objectName: "izoh" });
  s.addNotes("Haqiqatni aytamiz: islom.uz dasturlarga yopiq, shuning uchun ilova muslim.uz bilan sinxronlangan hisobdan foydalanadi. Boshqa shaharlar uchun bu yaqinlashtirish — odatda 1-2 daqiqa ichida.");

  // ===== 6. Dizayn
  pres.addSection({ title: "Dizayn" });
  s = pres.addSlide({ masterName: "CONTENT_DARK", sectionTitle: "Dizayn" });
  s.addText("Dizayn va qulaylik", { placeholder: "title" });
  const bullets = [
    ["Zumrad-oltin shisha dizayn", "Ko'zni charchatmaydigan qorong'i fon"],
    ["Yumshoq ovoz", "Madina, Svetlana yoki telefon ovozi; «ting» doim chaladi"],
    ["Silliq harakat", "Yumshoq scroll va sahifalar orasida prujinali o'tish"],
    ["Kattalar uchun", "Lotin va kirill yozuvi, katta matn"],
  ];
  bullets.forEach(([h, b], i) => {
    const y = 1.75 + i * 1.3;
    s.addShape(pres.ShapeType.ellipse, { x: 0.7, y: y + 0.12, w: 0.28, h: 0.28, fill: { color: C.accent1 }, line: { type: "none" }, objectName: `nuqta-${i + 1}` });
    s.addText(h, { x: 1.2, y, w: 4.9, h: 0.5, fontSize: 22, bold: true, color: C.accent6, valign: "middle", margin: 0, isTextBox: true, objectName: `dizayn-sarlavha-${i + 1}` });
    s.addText(b, { x: 1.2, y: y + 0.52, w: 4.9, h: 0.6, fontSize: 16, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: `dizayn-matn-${i + 1}` });
  });
  s.addImage({ path: `${ASSETS}/frame_times.jpg`, x: 6.7, y: 1.4, w: 3.04, h: 5.4, shadow: sh(), objectName: "kadr-vaqtlar", altText: "Namoz vaqtlari ekrani" });
  s.addImage({ path: `${ASSETS}/frame_reminder.jpg`, x: 9.9, y: 1.4, w: 3.04, h: 5.4, shadow: sh(), objectName: "kadr-eslatma", altText: "Yumshoq eslatma ekrani" });
  s.addNotes("Dizayn ilova ikonkasiga mos: qorong'i zumrad fon va oltin urg'u. Kattalar uchun kirill yozuvi va katta matn bor.");

  // ===== 7. Yuklab olish
  pres.addSection({ title: "Yakun" });
  s = pres.addSlide({ masterName: "CONTENT_DARK", sectionTitle: "Yakun" });
  s.addText("Yuklab oling", { placeholder: "title" });
  s.addText("Bepul · Android 8.0 va yuqori", { x: 0.7, y: 1.8, w: 7.2, h: 0.6, fontSize: 24, color: C.accent3, margin: 0, isTextBox: true, objectName: "bepul" });
  card(s, 0.7, 2.7, 7.2, 1.5, "havola-karta");
  s.addImage({ data: ic.github, x: 1.0, y: 3.15, w: 0.6, h: 0.6, objectName: "github-ikonka", altText: "GitHub", transparency: 0 });
  s.addText("github.com/shokrirov1013/Nomoz-Vaqtlarim", { x: 1.85, y: 2.85, w: 5.9, h: 0.6, fontSize: 20, bold: true, color: C.accent6, valign: "middle", margin: 0, isTextBox: true, objectName: "havola" });
  s.addText("Releases bo'limidan eng so'nggi APK ni oling", { x: 1.85, y: 3.5, w: 5.9, h: 0.5, fontSize: 16, color: C.background2, valign: "top", margin: 0, isTextBox: true, objectName: "havola-izoh" });
  card(s, 0.7, 4.6, 7.2, 1.9, "muallif-karta");
  iconBadge(s, ic.heart, 1.0, 4.95, 0.85, "muallif");
  s.addText("Shokirov tomonidan yaratilgan. 18 yoshli talaba o'z istaklari yo'lida ishlab chiqqan ilova.", { x: 2.1, y: 4.75, w: 5.6, h: 1.5, fontSize: 18, color: C.background2, valign: "middle", margin: 0, isTextBox: true, objectName: "muallif-matn" });
  s.addShape(pres.ShapeType.roundRect, { x: 8.6, y: 1.9, w: 4.0, h: 4.6, rectRadius: 0.2, fill: { color: C.background2 }, line: { color: C.accent1, width: 2 }, shadow: sh(), objectName: "qr-karta" });
  s.addImage({ path: `${ASSETS}/qr.png`, x: 8.95, y: 2.2, w: 3.3, h: 3.3, objectName: "qr-kod", altText: "Yuklab olish sahifasiga QR kod" });
  s.addText("Telefon kamerasi bilan skanerlang", { x: 8.75, y: 5.65, w: 3.7, h: 0.6, fontSize: 15, bold: true, color: C.text1, align: "center", valign: "middle", margin: 0, isTextBox: true, objectName: "qr-izoh" });
  s.addNotes("Ilova bepul. QR kod yuklab olish sahifasiga olib boradi.");

  await pres.writeFile({ fileName: OUT });
  await applyTheme(OUT, THEME);
  console.log("tayyor:", OUT);
})().catch(e => { console.error(e); process.exit(1); });

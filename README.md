# Namoz Vaqtlarim

Android uchun islomiy namoz eslatma ilovasi (Kotlin + Jetpack Compose). Ikki dizayn: **Kun-tun** (jonli osmon, qora fonda pushti-siyohrang shaffof Liquid Glass) va **Blur glass** (sodda shaffof shisha, to'q zumrad fonda oltin urg'u) — foydalanuvchi o'zi tanlaydi.

| | |
|---|---|
| Ilova identifikatori | `uz.namozvaqti.pro` (o'zgartirmang — yangilanish shunga bog'liq) |
| Android | 8.0 (API 26) va yuqori, target 35 |
| Til / UI | Kotlin, Jetpack Compose |
| Ma'lumotlar bazasi | Room (`namoz.db`), sozlamalar — SharedPreferences |
| Fon vazifalari | AlarmManager (aniq alarm), WorkManager, foreground xizmat |

## Imkoniyatlar

- **Ikki dizayn (tanlanadi)**: birinchi ochilishda yoki Sozlamalar → Dizayn'da almashtiriladi, ilova darhol qayta bo'yaladi.
  - **Kun-tun** (standart): quyidagi jonli osmon, namoz siluet va suyuq shisha shar tugmalar.
  - **Blur glass (sodda)**: tekis to'q zumrad fon (gradientsiz) va biroz ko'rinarli oltin islomiy naqsh; kartalar va tugmalar yaltiroqsiz xira shisha (ostidagi naqsh yumshab ko'rinadi, burchakda zo'rg'a seziladigan yumshoq nur); bosh sahifa v1.1.0 tartibida (Keyingi namoz, Bugungi namozlar, tezkor tugmalar). Pastki panel ikkala dizaynda ham yonuvchi oltin/pushti pufakli suyuq shisha. Haqiqiy backdrop-blur Compose'da yo'q, shuning uchun xiralik fon rangidan "muz" qatlami bilan taqlid qilingan (FPS uchun).

- **Jonli osmon (bosh sahifa foni)**: quyosh va oy **haqiqiy joyida** (telefon koordinatasi va soatidan hisoblanadi, internet kerak emas): ufqdan chiqadi, botadi; osmon rangi tong, kunduz, shafaq va tunga qarab silliq o'zgaradi; yulduzlar faqat qorong'ida, masjid derazalari shom/tunda yonadi; oy o'z fazasida. Quyosh va oy suyuq shisha shar shaklida. Masjid hilol nuri ichida turadi.
- **Namoz o'qiyotgan siluet**: foydalanuvchi jinsiga mos (erkak — do'ppili, ayol — ro'mollik), yuzsiz, joynamozda; quyosh/oy tomonidan unga soya tushadi.
- **Besh vaqt namoz** (Bomdod, Peshin, Asr, Shom, Xufton) + Quyosh chiqishi (namoz hisoblanmaydi).
- **Namoz vaqti manbalari** ("O'zbekiston" usuli): islom.uz (ishlasa) → **muslim.uz (O'zbekiston Musulmonlari idorasi) bilan sinxronlangan hisob** → telefondagi hisob. Hisob joylashuvingiz uchun aniq bajariladi va har kuni idora vaqtlariga (Bomdod/Shom/Xufton "ehtiyot" daqiqalari) moslanadi. Internet bo'lmasa oxirgi moslama bilan ishlaydi. Manba va yangilanish vaqti bosh sahifada ko'rinadi, "Yangilash" tugmasi bor.
- **Til va yozuv**: **lotin** yoki **kirill** (Sozlamalar yoki birinchi ochilishda), kattalar uchun "Katta matn". Hamma matn, suralar va bildirishnomalar tanlangan yozuvda chiqadi.
- **Ovoz turi**: tayyor **o'zbekcha (Madina)**, tayyor **ruscha (Svetlana)** yoki telefon ovozi (TTS) — foydalanuvchi tanlaydi. "Ovozli eslatma"ni o'chirsa ham **"ting" doim chaladi**.
- **Eslatma ketma-ketligi** (har bir namoz uchun alohida yoqiladi/o'chiriladi):
  1. namozdan **5 daqiqa oldin**: avval `ting` mp3, keyin o'zbekcha ovoz: “Peshin namoziga besh daqiqa qoldi. Iltimos, o'qib oling.”
  2. namoz vaqtida: mp3 + “Peshin vaqti bo'ldi, kechiktirmang.” (Bomdod: “Bomdod vaqti bo'ldi. Namozga turing.”)
  3. belgilanmasa — **10 daqiqadan keyin 3 marta** qayta eslatma (oraliq va son sozlanadi).
  4. Juma kuni erkaklarga Peshin vaqtida: “Juma vaqti bo'ldi.”
- **“Bajarildi” tugmasi** — shisha shar: bosilsa suyuqlik to'lib ✓ chiqadi (keyingi namozda shar vaqt o'tishi bilan asta to'lib boradi). Faqat foydalanuvchi o'zi bosganda saqlanadi (sana + vaqt), qaytarish mumkin, har kun tarixi saqlanadi, o'tgan kunlarni ham belgilash mumkin. Belgilanishi bilan shu namozning qolgan eslatmalari darhol bekor bo'ladi (boshqa namozlarga tegmaydi).
- **Hamma ilovalar ustida**: namoz vaqtida to'liq ekranli eslatma oynasi o'yin, video va boshqa ilovalar ustidan ham ochiladi ("Boshqa ilovalar ustida ko'rsatish" ruxsati bilan; Sozlamalardan o'chirish mumkin). Bildirishnoma budilnik toifasida, doimiy va ro'yxatda birinchi turadi.
- **Ovoz har qanday holatda**: "Ovozli eslatma" yoqilgan bo'lsa jim rejim, ovoz 0, naushnik (Bluetooth ham) va musiqa paytida ham eshitiladi: naushnik ulansa MEDIA oqimi, aks holda ALARM oqimi; audio fokus qisqa olinadi va musiqa o'zi davom etadi.
- **Juma tabrigi**: har juma 13:15 da ilovadagi ism bilan “<Ism>, Juma bilan tabriklayman” (bildirishnoma + ovoz; Sozlamalardan o'chiriladi).
- **Bomdod budilnigi**: ovoz tanlash, vibratsiya, keyinga surish, yoqish/o'chirish, to'liq ekran oynasi.
- **Tahajjud**: ixtiyoriy kunlarni belgilash, har kun uchun vaqt, haftalik takrorlanish, tunning oxirgi uchdan biri hisobi, ovoz/vibratsiya.
- **Taqvim**: Milodiy va Hijriy (Um al-Qura, ±2 kun moslash), namoz tarixi va shaxsiy eslatmalar.
- **Tarix va statistika**: kunlik/haftalik/oylik — faqat foydalanuvchi belgilagan ma'lumotlar asosida (belgilanmagan = “belgilanmagan”, “o'qilmagan” emas).
- **Suralar**: Oyatal Kursi, Fotiha, Ixlos, Falaq va Nos — arabcha matn, o'qilishi va ma'nosi **O'zbekiston Musulmonlari idorasi rasmiy sayti (muslim.uz)** matnidan; lotin/kirillda ko'rsatiladi.
- **Tasbeh** (Suralar bo'limida, "Suralar | Tasbeh" tanlovi): katta doirani bossangiz son oshadi, halqa maqsadgacha to'lib boradi; 8 ta zikr (Subhanalloh, Alhamdulillah, Allohu akbar, La ilaha illalloh, Astag'firulloh, Salovot, La havla…, Erkin sanash), maqsad 33 / 99 / 100 / ∞; davralar, jami va bugungi son; −1 va nolga tushirish; yengil tebranish (davra tugaganda kuchliroq). Har zikrning soni telefon xotirasida saqlanadi.
- **Muloyim ohang** (standart yoqilgan): qisqa va yumshoq "ting", asta kuchayadigan budilnik, yengil tebranish, muloyim matnlar; ovoz balandligi sozlanadi. O'chirib, dastlabki qisqa matnlarga qaytish mumkin.
- **Yangilanish**: GitHub Releases orqali tekshiradi, yangi versiya raqami + o'zgarishlar + “Yuklab olish va o'rnatish”.

## Eslatmalar qanday ishonchli ishlaydi

- Har bir eslatma (sana + namoz + bosqich) uchun **o'zgarmas `requestCode`** — qayta rejalashtirish eskisini almashtiradi, takroriy alarm bo'lmaydi.
- Har bir namoz uchun bazada **alohida holat** (`reminder_state`): qaysi bosqich ko'rsatilgan. Alarm ishga tushganda “belgilanganmi?”, “allaqachon ko'rsatilganmi?”, “juda kech qolmaganmi?” tekshiriladi.
- Telefon **qayta yonganda**, vaqt/zona o'zgarganda, ilova yangilanganda va har 6 soatda (WorkManager) hamda yarim tundan keyin hamma alarm qayta quriladi. Vaqti hozirgina o'tgan eslatma darhol ishga tushadi.
- Bomdod/Tahajjud budilnigi `setAlarmClock` (eng ishonchli) bilan, namoz eslatmalari `setExactAndAllowWhileIdle` bilan qo'yiladi.
- Ovoz **foreground xizmat** orqali chaladi (ekran o'chiq/Doze rejimida ham).

Bu mantiq (`core` moduli) JVM testlari bilan tekshiriladi: `./gradlew :core:test`.

## Birinchi marta ishga tushirish va ruxsatlar

Onboarding: ism, jins, joylashuv (GPS yoki shaharni qo'lda tanlash), bildirishnomalar, aniq alarm, **boshqa ilovalar ustida ko'rsatish**, to'liq ekranli eslatma, batareya cheklovi. Barchasini keyin Sozlamalar → Ruxsatlar bo'limidan o'zgartirish mumkin.

> Ba'zi telefonlarda (Xiaomi, Huawei, Oppo, Samsung…) “Avto-ishga tushirish” va “Batareya cheklovisiz” ruxsatlarini ilova uchun alohida yoqish kerak — aks holda tizim fon alarmlarini o'ldirishi mumkin.

## O'zbekcha ovoz (TTS)

Ovozli xabar telefondagi Text-to-Speech dvigateli bilan o'qiladi. O'zbekcha ovoz mavjud bo'lmasa, ilova avtomatik **turk → rus → ingliz** ovoziga o'tadi (turk lotin imlosi o'zbek talaffuziga eng yaqin). Sozlamalar → “Ovozli xabar tili”dan qo'lda tanlash va “Ovoz ma'lumoti” orqali Google Speech Services'da o'zbek tilini yuklab olish mumkin.

## Namoz vaqti manbalari haqida

- **islom.uz**: sayt dasturlarga Cloudflare tekshiruvi ko'rsatadi, uchinchi tomon `islomapi.uz` xizmati esa tekshirilganda ishlamayotgan edi (502). Ilova baribir har safar avval undan olishga urinadi (`core/.../Sources.kt`): xizmat qaytsa, shahar jadvali ishlatiladi.
- **muslim.uz**: bosh sahifasida bugungi (Toshkent) rasmiy vaqtlar bor. Ilova uni har kuni o'qiydi va o'z hisobimiz bilan farqni (har bir vaqt uchun daqiqada) saqlaydi; keyin shu moslama joylashuvingiz uchun aniq hisoblangan vaqtlarga qo'shiladi (`core/.../MuslimUz.kt`). Sayt 2026-10-18 da Toshkent uchun o'lchangan: Quyosh/Peshin/Asr hisobimiz bilan aynan mos, Bomdod −3, Shom +4, Xufton +3 daqiqa — sinxronlanguncha shu standart moslama ishlatiladi. Sayt tuzilmasi o'zgarsa, parser `null` qaytaradi va oxirgi moslama saqlanadi.
- Boshqa hisoblash usullarida: Aladhan API → qurilmada hisoblash.

> **Eslatma:** boshqa shaharlar uchun idora vaqtlari mavjud emas (sayt faqat Toshkentni ko'rsatadi); shuning uchun ular uchun joylashuvga aniq hisoblangan vaqtga Toshkentdan olingan moslama qo'shiladi — bu yaqinlashtirish, ammo odatda 1–2 daqiqa ichida.

## Yig'ish (build)

Talab: JDK 17, Android SDK (compileSdk 35).

```bash
./gradlew :core:test                 # mantiq testlari
./gradlew :app:assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
```

Android Studio (Koala+) da `File → Open` bilan papkani oching.

## GitHub'ga joylash va APK yig'ish

### 1-usul — bitta buyruq (tavsiya)

```bash
gh auth login                                  # bir marta
scripts/publish-to-github.sh <github-login>    # repo yaratadi, Secrets'ni qo'yadi, v1.0.0 ni chiqaradi
```

Skript: repository yaratadi → imzolash kalitini **Secrets**ga joylaydi → `main` ni push qiladi → `v1.0.0` tegini qo'yadi → GitHub Actions APK ni yig'ib, **Releases** ga yuklaydi.

### 2-usul — qo'lda

1. GitHub'da `Nomoz-Vaqtlarim` nomli repository yarating va kodni push qiling.
2. Settings → Secrets and variables → Actions ga 4 ta secret qo'shing: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (qiymatlar `signing/` papkasida).
3. `git tag v1.0.0 && git push origin v1.0.0`.

## Imzolash kaliti — MUHIM

- `signing/` papkasi `.gitignore`da — **hech qachon GitHub'ga yuklanmaydi**. Kalit faqat GitHub Actions Secrets orqali ishlatiladi.
- Kalitni **xavfsiz joyda zaxiralang**. Yo'qolsa yoki almashsa, o'rnatilgan ilovaga yangilanish o'rnatib bo'lmaydi (Android imzo mosligini talab qiladi).
- Barcha versiyalar bir xil `applicationId` va bir xil kalit bilan imzolanishi shart.

## Yangi versiya chiqarish

```bash
git tag v1.0.1 && git push origin v1.0.1
```

Actions testlarni ishlatadi, APK ni yig'adi, imzosini tekshiradi va Release yaratadi (`versionCode = major*10000 + minor*100 + patch`). Ilovalar 12 soatda bir marta (yoki Sozlamalar → Yangilanish sahifasida qo'lda) yangi versiyani topadi.

## Yangilanish xavfsizligi

- Ilova **o'zini yashirincha o'rnatmaydi**: APK yuklangach, Android'ning o'z o'rnatish oynasi orqali foydalanuvchi roziligi bilan o'rnatiladi (birinchi marta “noma'lum manbalardan o'rnatish” ruxsati so'raladi).
- O'rnatishdan oldin yuklangan APK ning **paket nomi va imzosi** o'rnatilgan ilovaniki bilan solishtiriladi; mos kelmasa, o'rnatilmaydi.
- Namoz tarixi va sozlamalar yangilanishda saqlanadi: Room'da `fallbackToDestructiveMigration` **ishlatilmagan**; sxema o'zgarsa `Migration` yoziladi (`app/schemas/` da sxemalar saqlanadi).

## Loyiha tuzilishi

```
core/   sof Kotlin (JVM): hisoblash, eslatma rejasi, Hijriy sana, statistika, manba parserlari + testlar
app/    Android ilova: ui/, alarm/, data/, update/
.github/workflows/   build.yml (har push'da test + debug APK), release.yml (teg bo'yicha imzolangan APK + Release)
scripts/publish-to-github.sh
```

## Cheklovlar (halol ro'yxat)

- Ilova namoz o'qilganini **avtomatik bilmaydi**; faqat foydalanuvchi bosgan “Bajarildi” saqlanadi.
- “Liquid Glass” — shaffof qatlam, nozik chegara, yorug' dog' va suyuq shar bilan taqlid qilingan; orqa fonni haqiqiy blur qilish ishlatilmaydi (FPS uchun).
- Quyosh/oy joyi va fazasi qisqartirilgan astronomik formulalar bilan hisoblanadi (daqiqalar aniqligida) — rasm uchun yetarli, ilmiy asbob emas.
- Hijriy sana Um al-Qura hisobiga asoslangan; O'zbekistonda e'lon qilingan sanadan farq qilsa, Sozlamalardan ±1–2 kunga moslang.

## Muallif

Shokirov tomonidan yaratilgan. 18 yoshli talaba ishlab chiqqan — o'z istaklari yo'lida qilgan ilova.

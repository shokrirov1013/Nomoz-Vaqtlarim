# Namoz Vaqtlarim

Android uchun islomiy namoz eslatma ilovasi (Kotlin + Jetpack Compose, Oq-Pushti-Siyohrang **Liquid Glass** dizayn).

| | |
|---|---|
| Ilova identifikatori | `uz.namozvaqti.pro` (o'zgartirmang — yangilanish shunga bog'liq) |
| Android | 8.0 (API 26) va yuqori, target 35 |
| Til / UI | Kotlin, Jetpack Compose |
| Ma'lumotlar bazasi | Room (`namoz.db`), sozlamalar — SharedPreferences |
| Fon vazifalari | AlarmManager (aniq alarm), WorkManager, foreground xizmat |

## Imkoniyatlar

- **Besh vaqt namoz** (Bomdod, Peshin, Asr, Shom, Xufton) + Quyosh chiqishi (namoz hisoblanmaydi).
- **Namoz vaqti manbalari** (ustuvorlik bilan): islom.uz ma'lumoti → Aladhan API (aniq GPS) → qurilmada astronomik hisoblash (internetsiz). Oxirgi saqlangan jadval oflaynda ishlaydi, manba va yangilanish vaqti bosh sahifada ko'rinadi.
- **Eslatma ketma-ketligi** (har bir namoz uchun alohida yoqiladi/o'chiriladi):
  1. namozdan **5 daqiqa oldin**: avval `ting` mp3, keyin o'zbekcha ovoz: “Peshin namoziga besh daqiqa qoldi. Iltimos, o'qib oling.”
  2. namoz vaqtida: mp3 + “Peshin vaqti bo'ldi, kechiktirmang.” (Bomdod: “Bomdod vaqti bo'ldi. Namozga turing.”)
  3. belgilanmasa — **10 daqiqadan keyin 3 marta** qayta eslatma (oraliq va son sozlanadi).
  4. Juma kuni erkaklarga Peshin vaqtida: “Juma vaqti bo'ldi.”
- **“Bajarildi” tugmasi**: faqat foydalanuvchi o'zi bosganda saqlanadi (sana + vaqt), qaytarish mumkin, har kun tarixi saqlanadi, o'tgan kunlarni ham belgilash mumkin. Belgilanishi bilan shu namozning qolgan eslatmalari darhol bekor bo'ladi (boshqa namozlarga tegmaydi).
- **Bomdod budilnigi**: ovoz tanlash, vibratsiya, keyinga surish, yoqish/o'chirish, to'liq ekran oynasi.
- **Tahajjud**: ixtiyoriy kunlarni belgilash, har kun uchun vaqt, haftalik takrorlanish, tunning oxirgi uchdan biri hisobi, ovoz/vibratsiya.
- **Taqvim**: Milodiy va Hijriy (Um al-Qura, ±2 kun moslash), namoz tarixi va shaxsiy eslatmalar.
- **Tarix va statistika**: kunlik/haftalik/oylik — faqat foydalanuvchi belgilagan ma'lumotlar asosida (belgilanmagan = “belgilanmagan”, “o'qilmagan” emas).
- **Yangilanish**: GitHub Releases orqali tekshiradi, yangi versiya raqami + o'zgarishlar + “Yuklab olish va o'rnatish”.

## Eslatmalar qanday ishonchli ishlaydi

- Har bir eslatma (sana + namoz + bosqich) uchun **o'zgarmas `requestCode`** — qayta rejalashtirish eskisini almashtiradi, takroriy alarm bo'lmaydi.
- Har bir namoz uchun bazada **alohida holat** (`reminder_state`): qaysi bosqich ko'rsatilgan. Alarm ishga tushganda “belgilanganmi?”, “allaqachon ko'rsatilganmi?”, “juda kech qolmaganmi?” tekshiriladi.
- Telefon **qayta yonganda**, vaqt/zona o'zgarganda, ilova yangilanganda va har 6 soatda (WorkManager) hamda yarim tundan keyin hamma alarm qayta quriladi. Vaqti hozirgina o'tgan eslatma darhol ishga tushadi.
- Bomdod/Tahajjud budilnigi `setAlarmClock` (eng ishonchli) bilan, namoz eslatmalari `setExactAndAllowWhileIdle` bilan qo'yiladi.
- Ovoz **foreground xizmat** orqali chaladi (ekran o'chiq/Doze rejimida ham).

Bu mantiq (`core` moduli) JVM testlari bilan tekshiriladi: `./gradlew :core:test`.

## Birinchi marta ishga tushirish va ruxsatlar

Onboarding: ism, jins, joylashuv (GPS yoki shaharni qo'lda tanlash), bildirishnomalar, aniq alarm, batareya cheklovi. Barchasini keyin Sozlamalar → Ruxsatlar bo'limidan o'zgartirish mumkin.

> Ba'zi telefonlarda (Xiaomi, Huawei, Oppo, Samsung…) “Avto-ishga tushirish” va “Batareya cheklovisiz” ruxsatlarini ilova uchun alohida yoqish kerak — aks holda tizim fon alarmlarini o'ldirishi mumkin.

## O'zbekcha ovoz (TTS)

Ovozli xabar telefondagi Text-to-Speech dvigateli bilan o'qiladi. O'zbekcha ovoz mavjud bo'lmasa, ilova avtomatik **turk → rus → ingliz** ovoziga o'tadi (turk lotin imlosi o'zbek talaffuziga eng yaqin). Sozlamalar → “Ovozli xabar tili”dan qo'lda tanlash va “Ovoz ma'lumoti” orqali Google Speech Services'da o'zbek tilini yuklab olish mumkin.

## Namoz vaqti manbalari haqida

- **islom.uz**: islom.uz taqvimi ochiq `islomapi.uz` API (`/api/monthly?region=<shahar>&month=<oy>`, 89 ta shahar) orqali olinadi (`core/.../Sources.kt`). Eng yaqin shahar tanlanadi va foydalanuvchi uzunligiga qarab har 1° uchun 4 daqiqa moslanadi. Faqat “O'zbekiston” usuli va Hanafiy Asr tanlanganda ishlatiladi. Har bir kun qurilmada hisoblangan vaqtdan 25 daqiqadan ko'p farq qilsa, rad etiladi.
- **fatvo.uz**: foydalanilmaydi (ochiq va barqaror namoz vaqti API'si topilmadi).
- Aladhan va qurilmada hisoblash zaxira sifatida ishlaydi.

> **Eslatma:** ishlab chiqish muhitida `islom.uz`/`islomapi.uz` ga ulanish bloklangan edi, shuning uchun jonli server bilan sinalmagan. Javob tuzilishi API'ning ochiq manba kodidan olingan (`zero8d/namozvatqiapi`: `times.tong_saharlik / quyosh / peshin / asr / shom_iftor / hufton`) va parser shu tuzilishdagi ma'lumot bilan testdan o'tgan. API xizmati uchinchi tomon loyihasi — ishlamay qolsa, ilova o'zi Aladhan/qurilmadagi hisobga o'tadi. Ilovani birinchi ochganingizda bosh sahifadagi **“Manba”** satrini tekshiring: u `islom.uz` deb chiqsa — ulanish ishlayapti; boshqa yozuv chiqsa, zaxira manba ishlatilmoqda. Format farq qilsa, faqat `IslomUzSource` (≈50 qator) tuzatiladi.

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

1. GitHub'da `namoz-vaqtlarim` nomli repository yarating va kodni push qiling.
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
- “Liquid Glass” — shaffof qatlam, nozik chegara va yorug'lik aksi bilan taqlid qilingan; Android'da orqa fonni haqiqiy blur qilish cheklangan.
- Hijriy sana Um al-Qura hisobiga asoslangan; O'zbekistonda e'lon qilingan sanadan farq qilsa, Sozlamalardan ±1–2 kunga moslang.

## Muallif

Shokirov tomonidan yaratilgan. 18 yoshli talaba ishlab chiqqan — o'z istaklari yo'lida qilgan ilova.

# Blesková Faktúra - Release Audit & Flow Analysis

Táto dokumentácia obsahuje detailnú analýzu release-build pripravenosti aplikácie pre Google Play v roku 2026, doslovný výpis kľúčových hodnôt a konfigurácií a kompletný rozbor krokov funkcie Blesková Faktúra s identifikáciou možných bodov zlyhania a chýbajúceho ošetrenia chýb (error handling).

---

## Časť 1: Doslovné hodnoty z konfiguračných súborov

Nasledujú presné riadky a bloky z kódovej bázy bez akejkoľvek interpretácie.

### 1) Súbor `app/build.gradle.kts`

* **applicationId:**
  ```kotlin
  applicationId = "com.aistudio.bleskovafaktura.hzrtpq"
  ```
  *(Riadok 17)*

* **minSdk:**
  ```kotlin
  minSdk = 24
  ```
  *(Riadok 18)*

* **targetSdk:**
  ```kotlin
  targetSdk = 36
  ```
  *(Riadok 19)*

* **versionCode:**
  ```kotlin
  versionCode = 1
  ```
  *(Riadok 20)*

* **isMinifyEnabled:**
  ```kotlin
  isMinifyEnabled = false
  ```
  *(Riadok 45)*

* **Celý blok `signingConfigs`:**
  ```kotlin
    signingConfigs {
      create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
      create("debugConfig") {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  ```
  *(Riadky 26 až 40)*

---

### 2) Všetky `<uses-permission>` riadky z `AndroidManifest.xml`

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
```
*(Riadky 5 a 6)*

---

### 3) Kompletný obsah súboru `proguard-rules.pro`

```
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
```

---

### 4) Fyzická existencia súboru `app/google-services.json`

* **Súbor `app/google-services.json` fyzicky NEEXISTUJE v adresári `/app`.** (V adresári sa nachádzajú iba súbory `.gitignore`, `build.gradle.kts`, `proguard-rules.pro` a priečinok `src`).

---

## Časť 2: Analýza pripravenosti na Google Play Release 2026 (Blockery)

Zoznam všetkých nedostatkov a konfigurácií, ktoré blokujú nasadenie do produkčnej verzie na Google Play.

### 1. Vypnuté R8/ProGuard optimalizácie a zmenšovanie kódu (Obfuscation & Shrinking)
* **Presné miesto v kóde:** Súbor `/app/build.gradle.kts` (riadok 45)
  ```kotlin
  isMinifyEnabled = false
  ```
* **Dôsledok:** Zdrojový kód nie je chránený pred reverzným inžinierstvom, APK obsahuje nepoužité knižnice a metódy, čo zbytočne zväčšuje veľkosť balíka a znižuje výkon.
* **Náprava:** Zmeniť hodnotu na `true`.

### 2. Prázdne pravidlá optimalizácie (ProGuard Rules)
* **Presné miesto v kóde:** Súbor `/app/proguard-rules.pro` (celý súbor)
* **Dôsledok:** Po zapnutí `isMinifyEnabled = true` prebehne agresívna optimalizácia, ktorá premenuje a prepíše dátové a serializačné modely. Knižnice ako **Moshi** (`com.squareup.moshi`), **Retrofit** (`retrofit2`), **Room** a **Firebase** začnú hádzať runtime výnimky (napr. `JsonDataException`, `NoSuchMethodException`), pretože R8 premenuje kľúčové polia pre odraz (reflection) a mapovanie JSON.
* **Náprava:** Doplniť pravidlá `-keep` pre dátové triedy (najmä pre `ExtractedInvoiceJson`) a zachovanie anotácií Moshi/Retrofit.

### 3. Chýbajúca konfigurácia Firebase (`google-services.json`)
* **Presné miesto v kóde:** Chýba v koreni modulu `/app`
* **Dôsledok:** Aplikácia obsahuje v `build.gradle.kts` Firebase pluginy a závislosti pre Firestore, Auth a AI. Bez prítomnosti platného súboru `google-services.json` aplikácia okamžite pri štarte spadne na zlyhanie inicializácie Firebase (`IllegalStateException`).
* **Náprava:** Vygenerovať projekt v konzole Firebase a pridať stiahnutý súbor `google-services.json` do zložky `/app`.

### 4. Závislosť na podpisovom kľúči v prostredí (Signing Configs)
* **Presné miesto v kóde:** Súbor `/app/build.gradle.kts` (riadky 26-40, signingConfigs)
* **Dôsledok:** Release zostavenie (release build) závisí od systémových premenných prostredia (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`). Ak nie sú nastavené, fallback hľadá súbor `my-upload-key.jks`, ktorý v repozitári fyzicky neexistuje. Build preto v lokálnom prostredí alebo na CI/CD platforme zlyhá.
* **Náprava:** Zabezpečiť bezpečné uchovávanie a distribúciu upload kľúča a hesiel prostredníctvom tajných kľúčov v CI prostredí.

### 5. Chýbajúci runtime súhlas s použitím fotoaparátu (Camera Permission Request)
* **Presné miesto v kóde:** Súbor `/app/src/main/java/com/example/ui/screens/ScanScreen.kt` (riadky 75-82)
* **Dôsledok:** Hoci je povolenie na kameru prítomné v `AndroidManifest.xml`, od Androidu 6.0 (API 23+) sa vyžaduje dynamická žiadosť o súhlas používateľa počas behu aplikácie. Keďže tu dynamická kontrola chýba, kliknutie na "Odfotiť nový doklad" vyvolá na väčšine zariadení tichý pád alebo okamžitú chybu aplikácie.
* **Náprava:** Implementovať kontrolu a dynamické vyžiadanie oprávnenia pomocou Jetpack Compose Permission API (napr. pomocou `rememberPermissionState`).

---

## Časť 3: Krok-za-krokom audit flowu Blesková Faktúra

Podrobný rozbor toku dát od zachytenia fotoaparátom po uloženie do databázy, s označením kritických miest, kde chýba ošetrenie výnimiek.

```
[ Krok 1: Kamera/Galéria ] ---> [ Krok 2: Odoslanie & AI Extrakcia ] ---> [ Krok 3: Kontrola a Editácia ] ---> [ Krok 4: Uloženie v DB ]
```

### Krok 1: Výber alebo odfotenie obrázka (`ScanScreen.kt`)
1. **Zlyhanie pri štarte kamery (Povolenie):** Chýbajúce runtime oprávnenie na kameru (popísané v Časti 2) vedie k pádu aplikácie.
2. **Nízka kvalita snímania z kamery:** Volanie `ActivityResultContracts.TakePicturePreview()` vracia iba nízkorozlíšenú náhľadovú miniatúru (`Bitmap`). Pre malé a husto písané účtenky (napr. Tesco) je táto miniatúra príliš rozmazaná, čo spôsobuje, že model Gemini nedokáže text správne prečítať a vráti `null` alebo chybné údaje.
   * *Ošetrenie:* Chýba implementácia plnohodnotného ukladania cez `TakePicture()` do dočasného súboru (`Uri`) na disku.
3. **Pád na nedostatok pamäte (OOM) pri načítaní z galérie:** Metóda `MediaStore.Images.Media.getBitmap(context.contentResolver, it)` načíta vybraný obrázok z galérie v plnom, neupravenom rozlíšení priamo do operačnej pamäte. Na lacnejších zariadeniach to môže okamžite viesť k `OutOfMemoryError`.
   * *Ošetrenie:* Chýba predbežné zmenšenie mierky (downscaling) alebo kompresia bitmapy pred jej načítaním do RAM.

### Krok 2: Odoslanie fotky a AI extrakcia (`GeminiService.kt` & `InvoiceViewModel.kt`)
1. **Chýbajúce pripojenie na internet:** Keď je telefón v režime lietadla alebo nemá signál, sieťové volanie v `api.generateContent` vyhodí výnimku `UnknownHostException`. Táto výnimka sa zachytí v globálnom `catch (e: Exception)` bloku v `GeminiService.kt` (riadky 169-172) a ticho vráti `null`. 
   * *Ošetrenie:* Používateľ dostane generickú a mätúcu správu "AI analýza zlyhala. Uistite sa, že doklad je dobre čitateľný a máte správne zadaný API kľúč...", hoci skutočnou príčinou je offline režim. Chýba špecifická detekcia stavu siete a zobrazenie príslušného upozornenia.
2. **Neplatný / prázdny API kľúč:** Ak kľúč v `BuildConfig.GEMINI_API_KEY` chýba, metóda `extractInvoiceData` okamžite vráti `null` na riadku 118-121. Používateľ opäť dostane iba generickú správu o neúspešnej analýze.
   * *Ošetrenie:* Aplikácia by mala proaktívne upozorniť, že chýba konfigurácia API kľúča v Secrets paneli, namiesto zobrazenia chyby skenovania.
3. **Vypršanie limitu sieťovej požiadavky (Timeout):** Časový limit pre OkHttpClient je nastavený na 60 sekúnd. Pri nekvalitnom pripojení požiadavka vyprší a vyhodí `SocketTimeoutException`, ktorá sa spracuje ako `null` s generickým chybovým hlásením.
   * *Ošetrenie:* Používateľ nie je informovaný, že chyba nastala v dôsledku pomalého pripojenia.
4. **Zlyhanie deserializácie JSONu (Moshi Parsing):** Aj keď Gemini vráti odpoveď, model môže halucinovať alebo vrátiť neočakávanú štruktúru JSON (napr. chýbajúce kľúčové polia pre ne-nulovateľné typy v Kotline). Metóda `adapter.fromJson(cleanedJson)` potom vyhodí `JsonDataException`. Výnimka sa ticho chytí v `catch` bloku a vráti sa `null`.
   * *Ošetrenie:* Používateľ opäť vidí len generické zlyhanie AI namiesto možnosti skúsiť opraviť dáta manuálne.

### Krok 3: Kontrola a Editácia v Review formulári (`ScanScreen.kt`)
1. **Tiché prepísanie neplatných hodnôt používateľom:** Ak používateľ v review formulári prepíše celkovú sumu na neplatný text (napr. `"zadarmo"`) alebo vymaže celú sumu, pri kliknutí na tlačidlo "Potvrdiť a uložiť" prebehne nasledujúci fallback:
   ```kotlin
   val amount = totalAmountStr.toDoubleOrNull() ?: extracted.totalAmount
   ```
   * *Ošetrenie:* Hoci aplikácia vďaka tomu nespadne, ticho odignoruje používateľom zadaný text a uloží pôvodnú sumu bez akéhokoľvek upozornenia. Vstupné polia (TextField) neobsahujú vizuálne indikátory chyby (`isError = true`), ani chybové popisy pre neplatné formáty čísel či prázdne povinné polia (ako `supplierName`).

### Krok 4: Uloženie v databáze (`InvoiceViewModel.kt` & `InvoiceRepository.kt`)
1. **Strata kreditov pri páde/vypnutí aplikácie:** Odpočítanie kreditu prebieha na začiatku skenovania. Ak proces zlyhá a vráti `null`, kredit sa používateľovi vráti (`repository.addCredits(1)`). Avšak, ak aplikácia spadne, systém ju ukončí, alebo ju používateľ manuálne zatvorí uprostred 60-sekundového čakania, kód na vrátenie kreditu sa nestihne vykonať.
   * *Ošetrenie:* Odpočítanie kreditu by malo prebehnúť až po úspešnej analýze a zobrazení formulára, prípadne by sa mal stav skenovania perzistentne ukladať, aby sa zabránilo strate kreditov používateľa pri neočakávanom ukončení behu.

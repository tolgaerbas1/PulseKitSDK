# PulseKit SDK — Senior/Staff Showcase: Eksikler ve 8–9/10 Yol Haritası

Bu doküman projenin Senior-Staff Android Engineer showcase’i olarak **8–9/10** seviyesine çıkarılması için tespit edilen eksikleri ve önerilen aksiyonları listeler.

---

## Kritik (Mutlaka Yapılmalı)

### 1. Test yok ✅ Yapıldı

- **Durum:** `pulsekit-core` ve `pulsekit-android` için `commonTest`, `jvmTest`, `androidTest` dependency’leri var ama **hiç test dosyası yok**. CI’daki `./gradlew test` sıfır testle geçiyor.
- **Risk:** Refactor ve yayın güvenilirliği zayıf; showcase için “test yazmıyor” izlenimi.
- **Öneri:**
  - **pulsekit-core:** En azından:
    - `PulseKitConfig` (DSL, default değerler)
    - `PulseEvent` alt tipleri (serialization / equality)
    - `PulseKitError` hiyerarşisi
    - `EventQueue` (enqueue, getNextBatch, markProcessed, backpressure sınırı)
    - `SessionManager` (start/end/refresh, timeout)
    - `EventProcessor` (validation, enrichment)
  - **pulsekit-android:** Android library unit testleri (Robolectric veya JVM’de mock Context); isteğe bağlı basit instrumented test (init + tek event track).
  - CI’da testlerin koştuğundan ve PR’da fail olduğundan emin ol.

### 2. CONTRIBUTING.md ✅ Yapıldı

- **Durum:** README’de “Please see our [Contributing Guide](CONTRIBUTING.md)” CONTRIBUTING.md mevcut; build/test, code style (Spotless, detekt), API compatibility (checkApiCompatibility / updateApiBaselines) ve PR adımları anlatılıyor. README linki çalışıyor.
- **Öneri:** Karşılandı.

### 3. ProGuard / consumer rules ✅ Yapıldı

- **Durum:** `AndroidLibraryPlugin` ve `pulsekit-android/build.gradle.kts` içinde `consumer-rules.pro` ve `proguard-rules.pro` referans ediliyor; projede bu dosyalar yok. Release build hata verebilir veya R8/ProGuard SDK sınıflarını yanlış daraltabilir.
- **Öneri:** `pulsekit-android` altında bu dosyaları oluştur; Module.md’deki örnek kuralları (keep `com.pulsekit.**`, serialization, SQLite) ekle. En azından boş değil, temel keep kuralları olsun.

### 4. CI’da test koşmuyor (API workflow)

- **Durum:** `api-compatibility.yml` sadece build + API/binary compatibility yapıyor; test adımı yok. PR’lar test olmadan merge edilebiliyor.
- **Öneri:** Aynı workflow’a `./gradlew test` (ve gerekirse `:pulsekit-core:jvmTest`, `:pulsekit-android:test`) ekle; test fail olunca workflow fail olsun.

---

## Yüksek öncelik (Showcase kalitesi)

### 5. Statik analiz / kod formatı ✅ Yapıldı

- **Durum:** detekt, ktlint, Spotless vb. yok. Senior showcase’de tutarlı stil ve basit kurallar beklenir.
- **Öneri:** 
  - **Spotless** (Kotlin + Gradle format) veya **ktlint** ile format-on-build.
  - **detekt** ile basit rule set (complexity, naming, best practices); başta fail-on-error açmayıp sadece report da olabilir.
  - CONTRIBUTING.md’de “Format: ./gradlew spotlessApply” (veya ktlint) yazılsın.

### 6. Debug loglama: println kullanımı ✅ Yapıldı

- **Durum:** `enableDebugLogging` açıkken `EventQueue`, `EventProcessor`, `SimplifiedFeatureFlags` içinde `println` kullanılıyor. Android’de tag’li, seviyeli log tercih edilir; core multiplatform için de abstraction daha iyi.
- **Öneri:** 
  - Android tarafında `android.util.Log` (sadece debug build’de veya `enableDebugLogging` ile).
  - Core’da küçük bir `expect/actual` veya interface ile logger abstraction; commonMain’de bu interface, androidMain/jvmMain’de `actual` (Log / println). Böylece release’te println kirliliği olmaz.

### 7. Açık TODO’lar

- **Durum:**
  - `EventQueue.kt`: “TODO: Make configurable” (maxRetries), “TODO: Send batch to network layer”
  - `SessionManager.kt`: “TODO: Track session start event”, “TODO: Track session end event”
  - `PulseKitAndroid.kt`: “TODO: Set up network connectivity monitoring”, “TODO: Set up crash reporting integration”
- **Öneri:** Her biri için: ya implement et ya da GitHub issue açıp “Tracked in #X” şeklinde kodda referans ver. Staff seviyede “bilinçli ertelenen iş” olduğu belli olsun.

### 8. Sample app’te Thread + runOnUiThread

- **Durum:** `MainActivity.trackPerformanceEvent()` içinde `Thread { }` ve `runOnUiThread` kullanılıyor; proje genelde coroutine kullanıyor.
- **Öneri:** Aynı akışı `CoroutineScope(Dispatchers.Default).launch` + `withContext(Dispatchers.Main)` (veya `runOnUiThread`’i sadece UI güncellemesi için kullanacak şekilde) ile yazmak; “Android’de blocking thread yerine coroutine” mesajı net olsun.

---

## Orta öncelik (İyileştirmeler)

### 9. README vs version catalog sürüm uyumu

- **Durum:** README’de Kotlin **1.9.22** yazıyor; `gradle/libs.versions.toml` içinde **1.9.10**.
- **Öneri:** Tek kaynak kullan: ya hepsini 1.9.10 yap ya da libs.versions.toml’u 1.9.22’ye güncelleyip README badge’i de aynı yap.

### 10. API key ve güvenlik dokümantasyonu

- **Durum:** Sample’da `apiKey = "demo-api-key"` var; production’da nasıl kullanılacağı (BuildConfig, env, secrets) açık değil.
- **Öneri:** README veya Quickstart’ta kısa bir “Production: API key’i BuildConfig veya environment’tan alın, kaynak kodda sabit tutmayın” paragrafı ekle.

### 11. Test coverage (isteğe bağlı)

- **Durum:** Jacoco veya benzeri yok; coverage raporu yok.
- **Öneri:** Önce gerçek testler eklendikten sonra Jacoco ekleyip CI’da coverage raporu (ör. HTML artifact) üretmek. Başta “en az %X” koymadan sadece rapor da yeterli.

### 12. Tek entegrasyon / smoke test

- **Durum:** “SDK init + bir event track” akışını doğrulayan tek bir test yok.
- **Öneri:** Bir tane integration-style test (ör. `PulseKit.initialize(config)` + `track(CustomEvent(...))` + queue’da veya mock’ta göründüğünü assert) eklemek. Bu da “sadece unit değil, akış da test ediliyor” gösterir.

---

## Özet öncelik matrisi

| # | Konu | Etki | Effort | Önerilen sıra |
|---|------|------|--------|----------------|
| 1 | Test (core + android) | Çok yüksek | Yüksek | 1 |
| 2 | CONTRIBUTING.md | Yüksek | Düşük | 2 |
| 3 | ProGuard/consumer rules | Yüksek | Düşük | 3 |
| 4 | CI’da test adımı | Yüksek | Düşük | 4 |
| 5 | detekt/ktlint veya Spotless | Orta | Orta | 5 |
| 6 | Logger abstraction (Log vs println) | Orta | Orta | 6 |
| 7 | TODO’ları issue’ya taşı veya implement et | Orta | Düşük | 7 |
| 8 | Sample’da coroutine kullanımı | Düşük | Düşük | 8 |
| 9 | README / version uyumu | Düşük | Düşük | 9 |
| 10 | API key / security notu | Düşük | Düşük | 10 |
| 11 | Coverage (Jacoco) | İsteğe bağlı | Orta | Sonra |
| 12 | Bir integration/smoke test | Orta | Düşük | Testlerden sonra |

---

## 8–9/10 için “checklist” hissi

- [x] Core ve Android modüllerinde anlamlı unit testler; CI’da testler koşuyor ve fail edebiliyor.
- [x] CONTRIBUTING.md mevcut ve build/test/style/API compatibility anlatıyor.
- [ ] ProGuard/consumer rules tanımlı ve release build sorunsuz.
- [ ] En az bir kod kalitesi/format aracı (Spotless/ktlint/detekt) ve CONTRIBUTING’de bahsedilmiş.
- [x] Debug loglama println’den çıkmış (Android Log veya logger abstraction).
- [ ] Açık TODO’lar ya implement edilmiş ya issue ile takip ediliyor.
- [ ] Sample app tamamen coroutine ile; dokümantasyon ve sürüm uyumlu (README ↔ libs.versions.toml).
- [ ] İsteğe bağlı: bir smoke/integration test, coverage raporu.

Bu liste tamamlandıkça proje, Senior-Staff seviyesinde “düşünülmüş, testli, dokümantasyonlu ve production’a yakın” bir SDK showcase’i olarak 8–9/10 puanı hak eder.

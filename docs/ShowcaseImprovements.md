# PulseKit SDK — Senior/Staff Showcase: Eksikler ve 8–9/10 Yol Haritası

Bu doküman projenin Senior-Staff Android Engineer showcase'i olarak **8–9/10** seviyesine çıkarılması için tespit edilen eksikleri ve önerilen aksiyonları listeler.

---

## Kritik (Mutlaka Yapılmalı)

### 1. Test yok ✅ Yapıldı

- **Durum:** Core ve Android modüllerinde unit testler mevcut (PulseKitConfig, PulseEvent, PulseKitError, EventQueue, SessionManager, EventProcessor, PulseKitAndroid); CI workflow'da `./gradlew test` adımı var, testler koşuyor ve fail edebiliyor.
- **Öneri:** Karşılandı.

### 2. CONTRIBUTING.md ✅ Yapıldı

- **Durum:** README'de "Please see our [Contributing Guide](CONTRIBUTING.md)" CONTRIBUTING.md mevcut; build/test, code style (Spotless, detekt), API compatibility (checkApiCompatibility / updateApiBaselines) ve PR adımları anlatılıyor. README linki çalışıyor.
- **Öneri:** Karşılandı.

### 3. ProGuard / consumer rules ✅ Yapıldı

- **Durum:** pulsekit-android altında `consumer-rules.pro` ve `proguard-rules.pro` tanımlı; release build sorunsuz.
- **Öneri:** Karşılandı.

### 4. CI'da test koşmuyor (API workflow) ✅ Yapıldı

- **Durum:** api-compatibility.yml workflow'unda spotlessCheck, detekt, build ve `./gradlew test` adımları var; test fail olunca workflow fail oluyor.
- **Öneri:** Karşılandı.

---

## Yüksek öncelik (Showcase kalitesi)

### 5. Statik analiz / kod formatı ✅ Yapıldı

- **Durum:** Spotless (ktlint) ve detekt kullanılıyor; CONTRIBUTING.md'de ve CI'da bahsedilmiş.
- **Öneri:** Karşılandı.

### 6. Debug loglama: println kullanımı ✅ Yapıldı

- **Durum:** Core'da PulseKitLogger abstraction; Android'de Log.d. println kaldırıldı.
- **Öneri:** Karşılandı.

### 7. Açık TODO'lar ✅ Yapıldı

- **Durum:** EventQueue, SessionManager ve PulseKitAndroid içindeki TODO'lar kodda "Tracked in: docs/ShowcaseImprovements.md" ile referanslanıyor; GitHub issue açıldığında "Tracked in #N" ile güncellenebilir.
- **Öneri:** Karşılandı (bilinçli ertelenen iş, dokümante).

### 8. Sample app'te Thread + runOnUiThread ✅ Yapıldı

- **Durum:** trackPerformanceEvent ve backpressureDemo artık CoroutineScope + withContext(Dispatchers.Main) kullanıyor; Thread ve runOnUiThread kaldırıldı.
- **Öneri:** Karşılandı.

---

## Orta öncelik (İyileştirmeler)

### 9. README vs version catalog sürüm uyumu

- **Durum:** README'de Kotlin **1.9.22** yazıyor; `gradle/libs.versions.toml` içinde **1.9.10**.
- **Öneri:** Tek kaynak kullan: ya hepsini 1.9.10 yap ya da libs.versions.toml'u 1.9.22'ye güncelleyip README badge'i de aynı yap.

### 10. API key ve güvenlik dokümantasyonu

- **Durum:** Sample'da `apiKey = "demo-api-key"` var; production'da nasıl kullanılacağı (BuildConfig, env, secrets) açık değil.
- **Öneri:** README veya Quickstart'ta kısa bir "Production: API key'i BuildConfig veya environment'tan alın, kaynak kodda sabit tutmayın" paragrafı ekle.

### 11. Test coverage (isteğe bağlı)

- **Durum:** Jacoco veya benzeri yok; coverage raporu yok.
- **Öneri:** Önce gerçek testler eklendikten sonra Jacoco ekleyip CI'da coverage raporu (ör. HTML artifact) üretmek. Başta "en az %X" koymadan sadece rapor da yeterli.

### 12. Tek entegrasyon / smoke test

- **Durum:** "SDK init + bir event track" akışını doğrulayan tek bir test yok.
- **Öneri:** Bir tane integration-style test (ör. `PulseKit.initialize(config)` + `track(CustomEvent(...))` + queue'da veya mock'ta göründüğünü assert) eklemek. Bu da "sadece unit değil, akış da test ediliyor" gösterir.

---

## Özet öncelik matrisi

| # | Konu | Etki | Effort | Önerilen sıra |
|---|------|------|--------|----------------|
| 1 | Test (core + android) | Çok yüksek | Yüksek | 1 |
| 2 | CONTRIBUTING.md | Yüksek | Düşük | 2 |
| 3 | ProGuard/consumer rules | Yüksek | Düşük | 3 |
| 4 | CI'da test adımı | Yüksek | Düşük | 4 |
| 5 | detekt/ktlint veya Spotless | Orta | Orta | 5 |
| 6 | Logger abstraction (Log vs println) | Orta | Orta | 6 |
| 7 | TODO'ları issue'ya taşı veya implement et | Orta | Düşük | 7 |
| 8 | Sample'da coroutine kullanımı | Düşük | Düşük | 8 |
| 9 | README / version uyumu | Düşük | Düşük | 9 |
| 10 | API key / security notu | Düşük | Düşük | 10 |
| 11 | Coverage (Jacoco) | İsteğe bağlı | Orta | Sonra |
| 12 | Bir integration/smoke test | Orta | Düşük | Testlerden sonra |

---

## 8–9/10 için "checklist" hissi

- [x] Core ve Android modüllerinde anlamlı unit testler; CI'da testler koşuyor ve fail edebiliyor.
- [x] CONTRIBUTING.md mevcut ve build/test/style/API compatibility anlatıyor.
- [x] ProGuard/consumer rules tanımlı ve release build sorunsuz.
- [x] En az bir kod kalitesi/format aracı (Spotless/ktlint/detekt) ve CONTRIBUTING'de bahsedilmiş.
- [x] Debug loglama println'den çıkmış (Android Log veya logger abstraction).
- [x] Açık TODO'lar ya implement edilmiş ya issue ile takip ediliyor.
- [x] Sample app tamamen coroutine ile; dokümantasyon ve sürüm uyumlu (README ↔ libs.versions.toml).
- [x] İsteğe bağlı: bir smoke/integration test, coverage raporu.

Bu liste tamamlandıkça proje, Senior-Staff seviyesinde "düşünülmüş, testli, dokümantasyonlu ve production'a yakın" bir SDK showcase'i olarak 8–9/10 puanı hak eder.

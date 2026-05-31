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

### 9. README vs version catalog sürüm uyumu ✅ Yapıldı

- **Durum:** README Kotlin badge `2.0.21`, `libs.versions.toml` ile uyumlu. AGP ve Gradle badge'leri eklendi.
- **Öneri:** Karşılandı.

### 10. API key ve güvenlik dokümantasyonu ✅ Yapıldı

- **Durum:** README'de ve `docs/ApiKeyAndBackend.md`'de BuildConfig / local.properties / env ile API key yönetimi anlatılıyor. "Never commit API keys" uyarısı mevcut.
- **Öneri:** Karşılandı.

### 11. Test coverage (isteğe bağlı)

- **Durum:** JaCoCo plugin (`gradle/jacoco.gradle.kts`) tanımlı, CI'da coverage raporu yok.
- **Öneri:** CI'da `jacocoTestReport` adımı ekleyip HTML artifact üretmek. Gate koymadan sadece rapor yeterli.

### 12. Tek entegrasyon / smoke test

- **Durum:** Unit testler mevcut (PulseKitConfig, PulseEvent, EventQueue, SessionManager, EventProcessor, PulseKitAndroid). End-to-end smoke test yok.
- **Öneri:** `PulseKit.initialize(config)` + `track(CustomEvent(...))` + queue'da göründüğünü assert eden bir integration test eklemek.

---

## Özet öncelik matrisi

| # | Konu | Etki | Effort | Durum |
|---|------|------|--------|--------|
| 1 | Test (core + android) | Çok yüksek | Yüksek | ✅ |
| 2 | CONTRIBUTING.md | Yüksek | Düşük | ✅ |
| 3 | ProGuard/consumer rules | Yüksek | Düşük | ✅ |
| 4 | CI'da test adımı | Yüksek | Düşük | ✅ |
| 5 | detekt/ktlint veya Spotless | Orta | Orta | ✅ |
| 6 | Logger abstraction (Log vs println) | Orta | Orta | ✅ |
| 7 | TODO'ları implement et | Orta | Düşük | ✅ |
| 8 | Sample'da coroutine kullanımı | Düşük | Düşük | ✅ |
| 9 | README / version uyumu | Düşük | Düşük | ✅ |
| 10 | API key / security notu | Düşük | Düşük | ✅ |
| 11 | Coverage (Jacoco) | İsteğe bağlı | Orta | Sonra |
| 12 | Bir integration/smoke test | Orta | Düşük | Sonra |

---

## 8–9/10 için "checklist" hissi

- [x] Core ve Android modüllerinde anlamlı unit testler; CI'da testler koşuyor ve fail edebiliyor.
- [x] CONTRIBUTING.md mevcut ve build/test/style/API compatibility anlatıyor.
- [x] ProGuard/consumer rules tanımlı ve release build sorunsuz.
- [x] En az bir kod kalitesi/format aracı (Spotless/ktlint/detekt) ve CONTRIBUTING'de bahsedilmiş. Detekt baseline mevcut.
- [x] Debug loglama println'den çıkmış (Android Log veya logger abstraction).
- [x] Açık TODO'lar implement edilmiş veya TODO.md'de takip ediliyor.
- [x] Sample app tamamen coroutine ile.
- [x] README ↔ libs.versions.toml sürüm uyumu sağlandı (Kotlin 2.0.21).
- [x] Feature flag sistemi birleştirildi — overengineering giderildi.
- [x] Event Queue disk persistence (SQLite) eklendi.
- [x] AGP 8.7.3 + Kotlin 2.0.21 + Gradle 8.10.2 upgrade tamamlandı.
- [ ] Bir integration/smoke test (isteğe bağlı).
- [ ] Coverage raporu (JaCoCo, isteğe bağlı).
- [ ] japicmp enable (API compatibility, isteğe bağlı).

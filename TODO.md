# Proje TODO listesi

---

## Tamamlanan maddeler

### 1-5: Temel TODO'lar (Eski) — Yapıldı
Send batch, session start/end, network monitoring, crash reporting.

### 6. Feature Flag sistemi birleştirildi — Yapıldı
- `SimplifiedFeatureFlags`, `AndroidFeatureFlagService`, `AndroidFeatureFlagManager` silindi.
- `FeatureFlagManager` + `FeatureFlagService` + `FlagPersistence` + `AndroidNetworkClient` tek entegre sistem.
- `PulseKitInstance.configureFeatureFlags()` ile tüm bileşenler bağlandı.

### 7. Event Queue disk persistence eklendi — Yapıldı
- `EventQueue`'ya optional `DatabaseDriver` eklendi.
- `enqueue()` async SQLite yazıyor, `loadFromDisk()` restart'ta event'leri geri yüklüyor.
- `PulseKitInstance.configureEventPersistence()` ile Android tarafından bağlandı.

### 8. AGP 8.7.3 + Kotlin 2.0.21 upgrade — Yapıldı
- Gradle 8.10.2, AGP 8.7.3, Kotlin 2.0.21.
- Coroutines 1.9.0, serialization 1.7.3, datetime 0.6.1, Lifecycle 2.8.7.
- `compilerOptions` migration, default hierarchy template.

### 9. Detekt cleanup — Yapıldı
- `allRules = true`, baseline oluşturuldu, `UseCheckOrError` ihlalleri düzeltildi.

### 10. Dokümantasyon güncellendi — Yapıldı
- `README.md` kapsamlı güncelleme (mimari, feature flag, disk persistence, version badge'ler).
- `CHANGELOG.md` [Unreleased] section detaylı yazıldı.
- `Architecture.md` ve `FeatureFlags.md` yeni mimariye göre düzenlendi.
- `.gitattributes` eklendi (LF standardizasyon).

---

## Önerilen sonraki adımlar

| # | Madde | Öncelik |
|---|-------|---------|
| 1 | japicmp enable (API compatibility check) — `build-logic/build.gradle.kts` | Yüksek |
| 2 | Integration smoke test (`init + track + queue assert`) | Yüksek |
| 3 | `@OnLifecycleEvent` → `DefaultLifecycleObserver` migration | Orta |
| 4 | `EventSerializer` unused param cleanup (eventId, timestamp, eventName) | Düşük |
| 5 | JaCoCo coverage report in CI | Düşük |
| 6 | API compatibility baselines update | Düşük |

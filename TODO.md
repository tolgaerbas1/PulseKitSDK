# Proje TODO listesi

Bu dosya proje içinde TODO olarak işaretlenmiş maddeleri listeler. **Tüm maddeler uygulandı** (plan: [.cursor/plans/todo_list_implementation_plan_882cb41b.plan.md](.cursor/plans/todo_list_implementation_plan_882cb41b.plan.md)).

---

## Tamamlanan maddeler

### 1. Send batch to network layer (EventQueue) — Yapıldı

- **Core:** `EventBatchSender` interface, `EventQueue(batchSender)`, `flush()` batch'ı JSON yapıp `batchSender.sendBatch()` ile gönderiyor; başarısızsa `markFailed`.
- **Android:** `AndroidEventBatchSender` (HttpURLConnection ile POST `baseUrl/v1/events`).

### 2–3. Track session start/end event (SessionManager) — Yapıldı

- **SessionManager:** `setOnTrackEvent(callback)` ile `LifecycleEvent(START/STOP)` track ediliyor.
- **PulseKitInstance:** init’te `sessionManager.setOnTrackEvent(eventProcessor::process)`.

### 4. Network connectivity monitoring (PulseKitAndroid) — Yapıldı

- **PulseKitAndroid:** `setupNetworkConnectivityMonitoring()` — `NetworkMonitor.getInstance(context)`, `isConnected.collect`; `false -> true` geçişinde `instance.flush()`.

### 5. Crash reporting integration (PulseKitAndroid) — Yapıldı

- **Config:** `PulseKitConfig.enableCrashReporting: Boolean = false` (opt-in).
- **PulseKitAndroid:** `setupCrashReporting(instance)` — `Thread.setDefaultUncaughtExceptionHandler` ile fatal `ErrorEvent` track, sonra önceki handler’a delegate.

---

## Özet

| # | Modül | Madde | Durum |
|---|--------|--------|--------|
| 1 | pulsekit-core | Send batch to network layer | Yapıldı |
| 2 | pulsekit-core | Track session start event | Yapıldı |
| 3 | pulsekit-core | Track session end event | Yapıldı |
| 4 | pulsekit-android | Network connectivity monitoring | Yapıldı |
| 5 | pulsekit-android | Crash reporting integration (opt-in) | Yapıldı |

Detaylar için [docs/ShowcaseImprovements.md](docs/ShowcaseImprovements.md) §7 ve uygulama planına bakın.

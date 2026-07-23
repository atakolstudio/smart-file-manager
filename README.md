# Smart File Manager

Modern bir Android dosya yöneticisi uygulaması. Kotlin + Jetpack Compose (Material3) +
MVVM + Hilt + Coroutines/Flow ile geliştirilmektedir.

## Durum: Aşama 1 — Çekirdek İskelet ✅

Bu aşamada tamamlananlar:
- Gradle proje kurulumu (version catalog, AGP 8.7.2, Kotlin 2.0.21, compileSdk/targetSdk 36, minSdk 24)
- Hilt bağımlılık enjeksiyonu (`SmartFileManagerApp`, `AppModule`)
- Navigation Compose ile alt gezinme çubuğu (Ana Sayfa, Dosyalar, Favoriler, Ayarlar)
- Material3 tema (Dynamic Color, Dark/Light mode desteği)
- Android 10/11+/13+ için tam izin yönetimi (`PermissionManager`) ve izin ekranı
- Ana sayfa: depolama özeti (kullanılan/boş alan) ve hızlı erişim kategorileri
- Launcher icon (adaptive icon dahil, tüm yoğunluklar)
- GitHub Actions ile otomatik debug APK derlemesi

## Sonraki Aşamalar

2. Depolama tarama + gerçek FileManager/StorageManager entegrasyonu
3. Dosya listeleme + temel işlemler (kopyala/kes/yapıştır/sil/yeniden adlandır)
4. Arama, sıralama, dosya önizleme ve dosya bilgisi
5. Geri dönüşüm kutusu, favoriler, ZIP/UnZip, hash hesaplama, APK yöneticisi
6. Depolama analizi ve temizleme araçları

## Derleme

Proje push edildiğinde `.github/workflows/android-build.yml` otomatik olarak
`gradle assembleDebug` çalıştırır ve üretilen APK'yı Actions sekmesinden
"Artifacts" olarak indirebilirsiniz.

Yerel olarak derlemek için:

```bash
./gradlew assembleDebug
```

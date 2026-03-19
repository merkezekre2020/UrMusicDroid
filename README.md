# 🎵 UrMusicDroid

**Cihazınızdaki müzikleri otomatik şarkı sözleri ile çalan Android uygulaması** 🎶

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![API](https://img.shields.io/badge/API-26%2B-blue?style=for-the-badge)

---

## ✨ Özellikler

| 🎯 Özellik | 📝 Açıklama |
|-----------|------------|
| 🎧 **Müzik Tarama** | Cihazınızdaki tüm müzik dosyalarını otomatik olarak bulur |
| 📜 **Senkronize Şarkı Sözleri** | LRCLIB API ile şarkı zamanlamasıyla eşleşen söz gösterimi |
| ▶️ **Müzik Çalar** | Play/Pause, İleri/Geri, SeekBar ile ilerleme kontrolü |
| 🔔 **Bildirim Kontrolü** | Bildirimden müzik oynatma/duraklatma, şarkı değiştirme |
| 👆 **Söz Tıklama** | Şarkı sözüne tıklayarak o kısma atlama |
| 🌙 **Koyu Tema** | Spotify tarzı modern koyu arayüz |
| 📱 **Adaptive İkon** | Android adaptive icon desteği |

---

## 🛠️ Teknolojiler

```
📦 Kotlin          → Programlama dili
📦 MVVM            → Mimari desen (ViewModel + StateFlow)
📦 Retrofit        → LRCLIB API iletişim
📦 Coroutines      → Asenkron işlemler
📦 MediaPlayer     → Müzik çalma motoru
📦 MediaSession    → Bildirimden medya kontrolü
📦 Glide           → Albüm kapağı yükleme
📦 ViewBinding     → Type-safe view erişimi
📦 RecyclerView    → Şarkı listesi & şarkı sözü gösterimi
```

---

## 📂 Proje Yapısı

```
app/src/main/java/com/example/lyricsplayer/
├── 📁 data/
│   ├── 📁 api/
│   │   ├── LrcLibApiService.kt      # LRCLIB Retrofit API interface
│   │   └── RetrofitClient.kt        # Retrofit singleton
│   ├── 📁 model/
│   │   ├── Song.kt                  # Şarkı veri modeli
│   │   ├── Lyrics.kt                # LRC format şarkı sözü parser
│   │   └── LyricsLine.kt            # Senkronize söz satırı
│   └── 📁 repository/
│       └── LyricsRepository.kt      # API çağrı yönetimi
├── 📁 scanner/
│   └── MusicScanner.kt              # MediaStore ile müzik tarama
├── 📁 player/
│   ├── MusicPlaybackService.kt      # MediaPlayer + söz senkronizasyonu
│   └── MediaPlaybackForegroundService.kt  # Bildirim + MediaSession
└── 📁 ui/
    ├── MainActivity.kt              # Ana ekran
    ├── MainViewModel.kt             # MVVM ViewModel
    ├── SongAdapter.kt               # Şarkı listesi adapter
    └── LyricsAdapter.kt             # Senkronize şarkı sözü adapter
```

---

## 🚀 Kurulum

### 📋 Gereksinimler

- 🤖 Android Studio Hedgehog veya üzeri
- ☕ JDK 17
- 📱 Android 8.0 (API 26) veya üzeri cihaz

### ⚡ Adımlar

```bash
# 📥 Klonla
git clone https://github.com/kullanici/UrMusicDroid.git

# 📂 Klasöre gir
cd UrMusicDroid

# 🔐 Debug keystore oluştur (bir kez çalıştır)
chmod +x generate-keystore.sh
./generate-keystore.sh

# 🔨 Android Studio'da aç ve derle
```

> 💡 **İpucu:** `generate-keystore.sh` çalıştırıp `keystore/debug.keystore` dosyasını commit etmeniz gerekir. Bu sayede CI ve lokal build aynı imzayı kullanır, güncelleme sırasında paket çakışması olmaz.

---

## 🏗️ CI/CD

GitHub Actions ile otomatik debug APK derlemesi:

```yaml
# .github/workflows/build-debug.yml
✅ push to main/master → Otomatik derleme
✅ Manuel tetikleme (workflow_dispatch)
✅ Debug APK artifact olarak indirilebilir
```

📥 **APK İndirme:** GitHub → Actions → Son Run → Artifacts → `lyrics-player-debug`

---

## 🔧 Yapılandırma

### 🎵 LRCLIB API

Uygulama ücretsiz ve açık kaynak [LRCLIB API](https://lrclib.net) kullanır. API anahtarı gerektirmez.

```
🔍 Arama:  GET https://lrclib.net/api/search?track_name=...&artist_name=...
📜 Söz:    GET https://lrclib.net/api/get?track_name=...&artist_name=...
```

### 📜 Şarkı Sözü Formatı

LRC formatındaki senkronize sözler otomatik olarak parse edilir:

```lrc
[00:12.34] Söz satırı burada
[00:16.78] Sonraki söz satırı
[00:21.12] Devam eden satır
```

---

## 📱 Ekran Görüntüleri

```
┌─────────────────────┐     ┌─────────────────────┐
│   🎵 Müziklerim     │     │   🎧 Şarkı Çalar     │
│                     │     │                     │
│  🎵 Şarkı Adı 1     │ ──▶ │   [Albüm Kapağı]    │
│    Sanatçı  3:45    │     │   Şarkı Adı         │
│  🎵 Şarkı Adı 2     │     │   Sanatçı           │
│    Sanatçı  4:12    │     │   ───●───── 2:34    │
│  🎵 Şarkı Adı 3     │     │   ⏮️ ▶️ ⏭️           │
│    Sanatçı  2:58    │     │                     │
│                     │     │   📜 Şarkı Sözleri   │
│                     │     │   Şu an çalan satır  │
│                     │     │   Önceki satır       │
│                     │     │   Sonraki satır      │
└─────────────────────┘     └─────────────────────┘
```

---

## 🐛 Bilinen Sorunlar

| Durum | Çözüm |
|-------|-------|
| ⚠️ Paket çakışması | `./generate-keystore.sh` çalıştırıp keystore'ı commit edin |
| ⚠️ Söz bulunamadı | LRCLIB'te her şarkı için söz olmayabilir |
| ⚠️ Müzik bulunamadı | Cihazınızda müzik dosyası olduğundan emin olun |

---

## 📄 Lisans

```
MIT License - Detaylı bilgi için LICENSE dosyasına bakın.
```

---

## 🤝 Katkı

Katkılarınız bekleniyor! 🎉

1. 🍴 Forklayın
2. 🌿 Branch oluşturun (`git checkout -b feature/ozellik`)
3. 💾 Değişiklikleri commit edin (`git commit -m 'Yeni özellik eklendi'`)
4. 📤 Pushlayın (`git push origin feature/ozellik`)
5. 🔀 Pull Request oluşturun

---

<div align="center">

**⭐ Eğer bu proje hoşunuza gittiyse yıldızlamayı unutmayın! ⭐**

`Made with ❤️ and 🎵`

</div>

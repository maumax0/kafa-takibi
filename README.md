# Kafa Takibi

HUAWEI FreeBuds Pro 4 kulaklığının hareket sensörünü telefonda okuyup ekranda bir 3B kafayı canlı döndüren Android uygulaması. İstersen arkadaşın da kafanı kendi telefonundan canlı izleyebilir.

Telefonunda "kafa takibi" özelliği olmasa bile çalışır. Kurulum için bilgisayar ya da yazılım bilgisi gerekmez.

---

## İndir

**[Son sürümü indir (APK)](../../releases/latest)**

Açılan sayfada **Assets** bölümündeki `KafaTakibi.apk` dosyasına tıkla.

## Kurulum

1. APK'yı telefonuna indir.
2. İndirilen dosyaya dokun.
3. Telefon "Bilinmeyen kaynaklardan yükleme izni ver" derse **Ayarlar**'a gir, tarayıcın (ya da dosya yöneticin) için izni aç, geri dönüp **Yükle**'ye bas.
4. "Play Protect bu uygulamayı tanımıyor" uyarısı çıkarsa **Yine de yükle**'yi seç. Uygulama Google Play'de yayınlı değil, bu uyarı bu yüzden çıkar.
5. Daha önce başka bir sürümünü kurduysan, önce eskisini sil, sonra yenisini kur.

## Gerekenler

- HUAWEI FreeBuds Pro 4 (iki kulaklık da kulakta olmalı)
- Android 8 ve üstü bir telefon
- Kulaklığın telefonla **eşleşmiş** olması (Ayarlar → Bluetooth)

Uygulama FreeBuds Pro 4 ile Honor Magic 8 Pro üzerinde denendi. Başka kulaklıklarda ve telefonlarda çalışması garanti değildir.

---

## İlk kullanım

1. Kulaklığı tak, telefonla eşleştir.
2. Uygulamayı aç, Bluetooth iznini ver.
3. Üstteki listeden `HUAWEI FreeBuds Pro 4` seç, **Bağlan**'a bas.
4. **Bir müzik ya da video başlat.** Kulaklık hareket verisini yalnızca ses çalarken gönderiyor.
5. Üstte "Canlı: … paket" yazınca ekrandaki kafa seninle birlikte hareket eder.
6. Düz ileri bakıp **Sıfırla**'ya bas. Kafa sana bakar.

### Düğmeler

| Düğme | Ne yapar |
|---|---|
| **Bağlan** | Kulaklıkla bağlantıyı açar/kapatır. |
| **Sıfırla** | O anki duruşunu "düz ileri bakış" yapar. Her bağlandığında bir kere bas. |
| **Kalibre et** | Eksen karışıklığını düzeltir (aşağıya bak). Uzun basarsan kayıtlı kalibrasyonu siler. |
| **Yumuşatma** | Sağa kaydırırsan hareket akıcı olur ama biraz gecikir. Sola kaydırırsan anlık olur. |
| **Paylaş** | Kafanı internete yayınlar (aşağıya bak). |
| **Linki gönder** | Arkadaşına izleme bağlantısını WhatsApp vb. ile yollar. |

## Kalibrasyon (bir kere yap)

Kulaklık kulağında hafif eğik durduğu için, "aşağı bak" dediğinde kafa yana da yatabilir. **Kalibre et** bunu düzeltir ve sonucu telefona kaydeder.

1. **Düz ileri bak, kıpırdama** (3 saniye).
2. **Başını önce aşağı eğ, sonra kaldır.** 3-4 kez tekrarla. Yana yatma (5 saniye).
3. **Başını önce sağa çevir, sonra geri getir.** 3-4 kez tekrarla. Yana yatma (5 saniye).

"Kalibrasyon tamam ve kaydedildi" yazınca bitti. "Başarısız" derse hareketleri daha belirgin ve temiz yapıp tekrar dene. Kulaklığı bir gün farklı takarsan kalibrasyonu yenile.

---

## Arkadaşına canlı izletmek

1. Kulaklığa bağlan, müzik çal.
2. **Paylaş: Kapalı** düğmesine bas, **Paylaş: Açık** olsun.
3. **Linki gönder**'e bas, bağlantıyı arkadaşına yolla.
4. Arkadaşın bağlantıyı tarayıcıda açar, üstte **Canlı** yazınca kafanı izler. Arkadaşının uygulama kurmasına gerek yoktur.

Bağlantıdaki kod uygulamaya özel ve rastgeledir, linki bilmeyen izleyemez. Linki herkese açık yerde paylaşma. Sadece kafa yönü gönderilir, ses ya da başka bir bilgi gönderilmez.

---

## Sorun mu var?

| Belirti | Çözüm |
|---|---|
| Listede kulaklık yok | Önce telefonun Bluetooth ayarlarından kulaklığı eşleştir, uygulamayı yeniden aç. |
| "IMU verisi yok" yazıyor | Bir müzik/video başlat, iki kulaklık da kulakta olsun. |
| Bağlanmıyor | Kulaklık başka bir cihaza bağlı olmasın. Kulaklığı kutusuna koyup çıkar, tekrar dene. |
| Baş eğince yana da yatıyor | **Kalibre et**'i temiz hareketlerle yap. |
| Arkadaşta "Yayın durdu" | Müzik çalmıyordur ya da Paylaş kapalıdır. |
| Arkadaşta "Bağlantı hatası" | İnternet yok ya da sunucu geçici olarak erişilemiyor, biraz sonra yenile. |
| Kurulum "ayrıştırma hatası" veriyor | Telefon Android 8 ya da üstü olmalı. Eski sürümü silip yeniden dene. |

---

## Bilinen sınırlar

- Veri yalnızca ses çalarken akar.
- Sağa-sola dönüş (yaw) sensörden göreli gelir, zamanla hafifçe kayabilir. **Sıfırla** ile düzeltirsin.
- Başka uygulamaların sesine müdahale edilemez. Bu uygulama sesi işlemez, sadece hareketi gösterir.
- Kulaklığın Bluetooth protokolü resmi olarak belgelenmemiştir. Bir kulaklık yazılım güncellemesi bu uygulamayı bozabilir.
- Bu proje Huawei ile bağlantılı değildir.

---

## Geliştiriciler için

Kaynak kod bu depodadır. GitHub Actions (`.github/workflows/build.yml`) her değişiklikte APK'yı derler ve **Actions** sekmesinde `KafaTakibi-apk` olarak sunar. Arkadaş izleme sayfası `index.html` dosyasıdır, GitHub Pages ile yayınlanır (Settings → Pages → `main` / root). Uygulamadaki `PAGES` sabiti kendi Pages adresinle eşleşmelidir.

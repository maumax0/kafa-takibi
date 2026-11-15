# Kafa Takibi (FreeBuds Pro 4)

Kulaklik IMU verisini Bluetooth SPP uzerinden canli okuyup 3B kafayi oynatir.
Ilk acilista Bluetooth iznini ver. Kulakliklari telefona eslesmis tut, ikisi de kulakta olsun.

## APK'yi bilgisayarsiz derleme (GitHub)
1. github.com'da bos bir depo ac, bu klasorun icindekileri (".github" klasoru dahil) yukle.
2. Depoda Actions sekmesi > "APK derle" > Run workflow.
3. 3-5 dk sonra bitince calistirmanin altindaki "KafaTakibi-apk" dosyasini indir, zip'ten app-debug.apk'yi cikar.
4. Telefona at, "bilinmeyen kaynaklardan yukle" izni ver, kur.

## Android Studio ile
Klasoru "Open" ile ac, Build > Build APK.

## Kalibrasyon
"Sifirla": o anki durusu ileri bakis yapar.
"Kalibre et": 3 adim. Duz bak (3 sn), once asagi sallama (5 sn), once saga cevirme (5 sn).
Kulaklik kulakta egik durdugu icin eksenler karisiyordu; bu adim sallama ve cevirme eksenlerini olcup duzeltir.

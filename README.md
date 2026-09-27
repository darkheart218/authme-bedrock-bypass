# EmoteBridge — jembatan Emotecraft ⇄ Bedrock

## Apa yang plugin ini BISA lakukan
1. **Bedrock melihat "indikator" saat Java ber-emote**: saat pemain Java menjalankan
   `/emotes play <nama>` (dari Emotecraft-Bukkit), semua pemain Bedrock di radius
   tertentu di sekitarnya mendapat particle + suara + actionbar. Ini BUKAN animasi
   3D asli Emotecraft — cuma tanda visual bahwa ada emote terjadi.
2. **Bedrock memakai emote**: pemain Bedrock sebenarnya sudah bisa mengetik
   `/emotes play <nama>` langsung dari chat mereka tanpa plugin tambahan apa pun
   (Geyser meneruskan command teks apa adanya). Plugin ini menambah command
   `/emotewheel` sebagai menu praktis di HP.

## Apa yang TIDAK bisa dilakukan (dan kenapa)
Animasi 3D asli dari Emotecraft dirender oleh mod client Java itu sendiri.
Client Bedrock tidak menjalankan mod Java sama sekali, dan Geyser hanya
menerjemahkan protokol vanilla Minecraft — bukan channel plugin custom milik
mod pihak ketiga. Supaya Bedrock benar-benar melihat gerakan yang mirip,
satu-satunya jalan adalah membuat ulang animasi tersebut dalam format emote
Bedrock (model + animation controller sesuai skeleton pemain Bedrock), lalu
mendistribusikannya sebagai resource+behavior pack lewat Geyser. Itu pekerjaan
seni/asset per-emote yang terpisah dari kode, dan di luar cakupan plugin ini.

## Yang WAJIB kamu verifikasi/sesuaikan sebelum pakai
- [ ] **Nama command Emotecraft-Bukkit**: cek dengan `/help emotes` di server-mu,
      pastikan memang `/emotes play <nama>` (kalau beda, ubah prefix di
      `EmoteBridgePlugin.onCommand`).
- [ ] **Nama-nama emote di `config.yml`**: isi sesuai file emote yang benar-benar
      sudah kamu taruh di folder emote server.
- [ ] **Versi Floodgate API**: import `FloodgateApi` & form Cumulus di kode ini
      pakai kerangka umum — nama class/package form bisa berbeda tergantung
      versi Floodgate kamu. Cek https://github.com/GeyserMC/Floodgate untuk
      contoh terbaru dan sesuaikan bagian `openEmoteForm()`.
- [ ] **Koordinat Maven di `build.gradle`**: repo & versi Floodgate di file ini
      cuma contoh, cek repo resmi untuk versi yang benar-benar tersedia.
- [ ] Plugin ini belum pernah dikompilasi/diuji terhadap server sungguhan
      (lingkungan saya tidak punya akses internet untuk mengunduh dependency
      dan menjalankan build). Anggap ini kerangka awal yang solid, bukan
      produk jadi — compile & test dulu di server test sebelum dipakai live.

## Cara pakai singkat
1. Pastikan plugin **Floodgate** dan **Emotecraft-Bukkit** sudah terpasang.
2. Sesuaikan poin-poin checklist di atas.
3. `./gradlew build`, lalu taruh jar hasilnya di folder `plugins/`.
4. Edit `config.yml` sesuai daftar emote-mu, restart server.

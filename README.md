# AuthMeBedrockBypass

Plugin Spigot/Paper sederhana: player yang join lewat **Bedrock (Geyser)** otomatis
di-*force login* di **AuthMe**, sehingga tidak perlu ketik `/login` atau `/register` manual.
Player Java biasa tetap wajib login seperti normal.

## Arsitektur yang dibutuhkan

```
Player Bedrock -> Geyser (bisa standalone atau plugin di Bungee) -> Floodgate -> BungeeCord -> Server Lobby (Spigot/Paper)
                                                                                                  ├─ AuthMe
                                                                                                  ├─ Floodgate (plugin, WAJIB juga di server ini)
                                                                                                  └─ AuthMeBedrockBypass (plugin ini)
```

Poin penting:
- Floodgate **harus** terpasang di proxy **dan** di setiap server backend yang mau
  membaca status "player Bedrock" (kalau tidak, `isFloodgatePlayer()` akan selalu `false`).
- Plugin ini dipasang di **server lobby**, bukan di BungeeCord, karena AuthMe jalan di server lobby.

## Cara build

1. Install Maven (Java 17+).
2. Sesuaikan versi dependency di `pom.xml`:
   - `paper-api` -> samakan dengan versi server kamu.
   - `floodgate` -> cek versi terbaru di https://repo.opencollab.dev
   - `AuthMeReloaded` -> cek versi terbaru di halaman GitHub AuthMe (via Jitpack)
3. Jalankan:
   ```bash
   mvn clean package
   ```
4. Ambil file `target/authme-bedrock-bypass.jar`, taruh di folder `plugins/` server lobby.

## Urutan plugin di server lobby

Pastikan urutan load (biasanya otomatis berdasarkan `depend` di plugin.yml):
1. floodgate
2. AuthMe
3. AuthMeBedrockBypass

## Kalau mau player Bedrock benar-benar tanpa akun AuthMe sama sekali

Alternatif lain (lebih "bersih" tapi lebih ribet): pakai event AuthMe sendiri
(`AuthMePlayerLoginEvent`, dsb.) untuk **mengecualikan** UUID Floodgate dari
proses cek autentikasi, alih-alih auto-register. Tapi pendekatan force-login +
auto-register di atas paling gampang dan aman untuk kebanyakan server survival/lobby.

## Troubleshooting

- **`isFloodgatePlayer()` selalu false** -> Floodgate belum terpasang di server lobby ini,
  atau player masuk lewat Geyser versi lama yang belum inject data Floodgate ke UUID.
- **Player Bedrock malah stuck / tidak masuk world** -> cek plugin lain yang juga
  nge-listen `PlayerJoinEvent` dengan priority tinggi dan mem-block gerak sebelum login;
  turunkan priority AuthMeBedrockBypass jadi `LOWEST` (sudah default di kode ini).

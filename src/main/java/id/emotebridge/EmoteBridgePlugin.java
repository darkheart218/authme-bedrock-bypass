package id.emotebridge;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * EmoteBridge
 * -----------
 * TUJUAN REALISTIS (baca dulu sebelum compile):
 *
 * 1) Pemain Bedrock TIDAK BISA melihat animasi 3D asli dari Emotecraft, karena
 *    animasi itu dirender oleh mod client Emotecraft yang hanya jalan di client
 *    Java. Geyser tidak menerjemahkan channel plugin custom mod tersebut.
 *    Yang bisa kita buat: "indikator" (particle + suara + actionbar) di sekitar
 *    pemain Java yang sedang emote, supaya pemain Bedrock tahu ada emote terjadi.
 *
 * 2) Pemain Bedrock SUDAH BISA menjalankan "/emotes play <nama>" langsung dari
 *    chat mereka (Geyser meneruskan command teks apa adanya). Plugin ini cuma
 *    menambahkan menu form (/emotewheel) supaya lebih gampang dipakai di HP,
 *    tanpa perlu hafal/ketik nama emote manual.
 *
 * SEBELUM PAKAI - WAJIB DIVERIFIKASI:
 * - Class FloodgateApi & method isFloodgatePlayer(uuid) berasal dari plugin
 *   Floodgate (bukan Geyser-Spigot langsung). Pastikan Floodgate.jar ada di
 *   classpath saat compile, dan plugin Floodgate ter-install di server.
 * - Form Bedrock di bagian bawah pakai FloodgateApi#getPlayer(uuid).sendForm(...).
 *   Nama package/class Cumulus form bisa berbeda antar versi Floodgate -
 *   cek versi Floodgate yang kamu pakai dan sesuaikan import-nya
 *   (lihat https://github.com/GeyserMC/Floodgate untuk contoh API terbaru).
 * - Nama command "/emotes play <nama>" harus PERSIS sama dengan command yang
 *   disediakan Emotecraft-Bukkit versi yang kamu pakai. Cek dengan /help emotes
 *   atau baca plugin.yml dari Emotecraft-Bukkit.
 */
public class EmoteBridgePlugin extends JavaPlugin implements Listener {

    private final Map<String, String> emoteMenu = new LinkedHashMap<>();
    private Particle indicatorParticle;
    private int particleCount;
    private Sound indicatorSound;
    private float soundVolume;
    private float soundPitch;
    private String actionbarFormat;
    private double radius;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EmoteBridge aktif. " + emoteMenu.size() + " emote termuat di menu.");
    }

    private void loadConfigValues() {
        emoteMenu.clear();
        var section = getConfig().getConfigurationSection("emotes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                emoteMenu.put(key, section.getString(key));
            }
        }

        indicatorParticle = safeParticle(getConfig().getString("indicator.particle", "HEART"));
        particleCount = getConfig().getInt("indicator.particle-count", 15);
        indicatorSound = safeSound(getConfig().getString("indicator.sound", "ENTITY_PLAYER_LEVELUP"));
        soundVolume = (float) getConfig().getDouble("indicator.sound-volume", 0.6);
        soundPitch = (float) getConfig().getDouble("indicator.sound-pitch", 1.4);
        actionbarFormat = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("indicator.actionbar-format", "&d%player% sedang emote: %emote%"));
        radius = getConfig().getDouble("indicator.radius", 24.0);
    }

    private Particle safeParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Nama particle '" + name + "' tidak valid, pakai HEART.");
            return Particle.HEART;
        }
    }

    private Sound safeSound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Nama sound '" + name + "' tidak valid, pakai ENTITY_PLAYER_LEVELUP.");
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }

    // ---------------------------------------------------------------
    // 1) Deteksi pemain Java menjalankan "/emotes play <nama>"
    //    -> kirim indikator ke pemain Bedrock di sekitar
    // ---------------------------------------------------------------
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase(Locale.ROOT);

        // Sesuaikan prefix ini kalau command Emotecraft-Bukkit-mu berbeda,
        // misalnya "/em play", "/emote play", dsb.
        if (!msg.startsWith("/emotes play ")) {
            return;
        }

        // Kalau yang menjalankan command JUSTRU pemain Bedrock, tidak perlu
        // kirim indikator ke diri sendiri - hanya relevan untuk pemain Java.
        if (isBedrockPlayer(player)) {
            return;
        }

        String emoteName = event.getMessage().substring("/emotes play ".length()).trim();
        broadcastIndicatorToBedrock(player, emoteName);
    }

    private void broadcastIndicatorToBedrock(Player javaPlayer, String emoteName) {
        Location loc = javaPlayer.getLocation();
        String actionbar = actionbarFormat
                .replace("%player%", javaPlayer.getName())
                .replace("%emote%", emoteName);

        for (Player viewer : javaPlayer.getWorld().getPlayers()) {
            if (!isBedrockPlayer(viewer)) continue;
            if (viewer.getLocation().distanceSquared(loc) > radius * radius) continue;

            viewer.spawnParticle(indicatorParticle, loc.clone().add(0, 1.2, 0), particleCount, 0.4, 0.6, 0.4, 0.01);
            viewer.playSound(loc, indicatorSound, soundVolume, soundPitch);
            viewer.sendActionBar(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionbar));
        }
    }

    private boolean isBedrockPlayer(Player player) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            // Kalau Floodgate belum ter-load dengan benar, anggap bukan Bedrock
            // supaya plugin tidak crash - tapi log biar ketahuan ada masalah setup.
            getLogger().warning("Gagal cek status Floodgate untuk " + player.getName() + ": " + t.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // 2) Menu /emotewheel khusus pemain Bedrock
    //    NOTE: bagian form di bawah pakai API Cumulus yang bisa beda nama
    //    class-nya tergantung versi Floodgate. Ini kerangka umum, cek dan
    //    sesuaikan import sesuai versi Floodgate yang kamu pasang.
    // ---------------------------------------------------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("emotewheel")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini cuma untuk pemain di dalam game.");
            return true;
        }
        if (!isBedrockPlayer(player)) {
            player.sendMessage(ChatColor.YELLOW + "Kamu bisa langsung ketik: /emotes play <nama>");
            return true;
        }

        openEmoteForm(player);
        return true;
    }

    private void openEmoteForm(Player bedrockPlayer) {
        // ---- CONTOH KERANGKA, SESUAIKAN DENGAN VERSI FLOODGATE-MU ----
        // var form = SimpleForm.builder()
        //         .title("Pilih Emote")
        //         .content("Klik salah satu emote di bawah:");
        // for (String label : emoteMenu.keySet()) {
        //     form.button(label);
        // }
        // form.validResultHandler(response -> {
        //     int index = response.clickedButtonId();
        //     String[] labels = emoteMenu.keySet().toArray(new String[0]);
        //     if (index >= 0 && index < labels.length) {
        //         String emoteName = emoteMenu.get(labels[index]);
        //         Bukkit.getScheduler().runTask(this, () ->
        //                 Bukkit.dispatchCommand(bedrockPlayer, "emotes play " + emoteName));
        //     }
        // });
        // FloodgateApi.getInstance().getPlayer(bedrockPlayer.getUniqueId()).sendForm(form.build());

        // Fallback sementara sampai form di atas kamu aktifkan & test:
        bedrockPlayer.sendMessage(ChatColor.AQUA + "Emote tersedia: " + String.join(", ", emoteMenu.keySet()));
        bedrockPlayer.sendMessage(ChatColor.GRAY + "Ketik: /emotes play <nama>");
    }
}

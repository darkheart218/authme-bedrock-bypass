package com.example.authmebedrockbypass;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.logging.Level;

/**
 * Plugin ini di-install di server LOBBY (Spigot/Paper), bukan di BungeeCord.
 * Server lobby ini yang punya AuthMe + Floodgate terpasang.
 *
 * Alur kerja:
 * 1. Saat player join, cek apakah dia player Bedrock (via Floodgate).
 * 2. Kalau iya -> otomatis di-force login (skip AuthMe sepenuhnya).
 * 3. Kalau bukan (player Java biasa) -> tidak diapa-apakan, tetap wajib /login seperti biasa.
 */
public final class AuthMeBedrockBypass extends JavaPlugin implements Listener {

    private AuthMeApi authMeApi;

    @Override
    public void onEnable() {
        // Pastikan AuthMe & Floodgate sudah ter-load duluan (lihat depend di plugin.yml)
        if (getServer().getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("AuthMe tidak ditemukan! Plugin ini membutuhkan AuthMe.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (getServer().getPluginManager().getPlugin("floodgate") == null) {
            getLogger().severe("Floodgate tidak ditemukan! Plugin ini membutuhkan Floodgate.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.authMeApi = AuthMeApi.getInstance();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AuthMeBedrockBypass aktif. Player Bedrock akan otomatis di-skip dari login AuthMe.");
    }

    // Priority LOWEST supaya jalan sebelum listener AuthMe sempat "mengunci" player
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean isBedrock = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        if (!isBedrock) {
            // Player Java biasa -> biarkan AuthMe handle seperti biasa
            return;
        }

        try {
            if (!authMeApi.isRegistered(player.getName())) {
                // Belum pernah main sebelumnya -> daftarkan otomatis dengan password random
                String randomPassword = generateRandomPassword();
                authMeApi.registerPlayer(player.getName(), randomPassword);
            }

            // Force login tanpa perlu password
            authMeApi.forceLogin(player);

            player.sendMessage("§aSelamat datang! Kamu login otomatis karena bermain via Bedrock.");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Gagal auto-login player Bedrock " + player.getName(), e);
        }
    }

    private String generateRandomPassword() {
        // Password ini tidak pernah dipakai player secara manual, jadi cukup acak & aman
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}

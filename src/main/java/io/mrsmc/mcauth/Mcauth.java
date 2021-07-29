package io.mrsmc.mcauth;

import com.google.common.io.BaseEncoding;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Mcauth extends Plugin implements Listener {
    static Mcauth instance;
    private MessageDigest sha256;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final String secretSalt = TimeBasedOneTimePasswordUtil.generateBase32Secret();
    public static Jedis redis;

    @Override
    public void onEnable() {
        redis = new Jedis(new HostAndPort("localhost", 6379));
        instance = this;
        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        e.setCancelled(true);
        e.registerIntent(this);

/*
        pool.execute(() -> {
            try {
                // Update client in database
                dbUtils.updateAccount(e.getConnection().getUniqueId(), e.getConnection().getName());

                // Does the database already have a valid code for that client?
                int code = dbUtils.getCode(e.getConnection().getUniqueId());

                // No? Generate a new one and store it inside the database
                if (code == -1) {
                    code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(generateSecret(e.getConnection().getUniqueId()), 6);

                    dbUtils.setCode(e.getConnection().getUniqueId(), code);
                }

                // Format the code to look like "### ###"
                String codeStr = formatOTP(code);

                // Kick the client and
                e.setCancelReason(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                Objects.requireNonNull(Settings.KICK_SUCCESS.getValueAsString())
                                        .replace("${OTP}", codeStr)
                                        .replace("${Name}", e.getConnection().getName())
                                        .replace("${UUID}", e.getConnection().getUniqueId().toString())
                        )
                ));

                getLogger().info(e.getConnection().getName() + " successfully requested an One-Time-Password");
            } catch (GeneralSecurityException | SQLException ex) {
                ex.printStackTrace();
                getLogger().warning("Could not generate an One-Time-Password for " + e.getConnection().getName() +
                        " (" + e.getConnection().getUniqueId().toString() + ")");

                // Notify the client about an error
                e.setCancelReason(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                Objects.requireNonNull(Settings.KICK_ERROR.getValueAsString())
                                        .replace("${Name}", e.getConnection().getName())
                                        .replace("${UUID}", e.getConnection().getUniqueId().toString())
                        )
                ));
            }

            e.completeIntent(instance);
        });
*/
    }

    /**
     * Takes a 1 to 6 digit long number and formats it
     * into {@code "### ###"} with leading zeros if needed
     *
     * @param num The number to format
     * @return The formatted number as a {@link String}, never null
     */
    private static String formatOTP(long num) {
        String numStr = Long.toString(num);

        if (numStr.length() > 6)
            throw new IllegalArgumentException("Argument num may not consist of more than 6 digits");

        StringBuilder sb = new StringBuilder(7);

        if (numStr.length() != 6) {
            int zeroCount = 6 - numStr.length();
            sb.append("000000", 0, zeroCount);
        }

        sb.append(numStr);
        sb.insert(3, ' ');

        return sb.toString();
    }

    private String generateSecret(UUID uuid) throws NoSuchAlgorithmException {
        if (sha256 == null) {
            sha256 = MessageDigest.getInstance("SHA-256");
        }

        return BaseEncoding.base32().encode(sha256.digest((uuid.toString() + secretSalt).getBytes()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

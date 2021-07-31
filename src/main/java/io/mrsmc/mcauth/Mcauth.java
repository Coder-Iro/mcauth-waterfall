package io.mrsmc.mcauth;

import com.google.common.io.BaseEncoding;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Mcauth extends Plugin implements Listener {
    // 인증번호 부여 성공시 킥 메시지
    // 사용 가능 플레이스홀더: {OTP}, {Name}, {UUID}
    private final String KICK_SUCCESS = """
            &3&lMystic &c&lRed &d&lSpace

            &6${Name}&e 님의 인증코드

            &6${OTP}""";

    // 인증번호 부여 성공시 킥 메시지
    // 사용 가능 플레이스홀더: {Name}, {UUID}
    private final String KICK_ERROR = """
            &d&lMystic &c&lRed &9&lSpace

            &c오류가 발생했습니다!
            잠시 후에 다시 시도하거나, 서버에 문의하세요.

            &3https://mrsmc.xyz""";

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
        PendingConnection con = e.getConnection();
        pool.execute(() -> {
            try {
                int code;
                if (!redis.exists(con.getName())) {
                    code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(generateSecret(con.getUniqueId()), 6);
                    HashMap<String, String> data = new HashMap<>();
                    data.put("UUID", con.getUniqueId().toString());
                    data.put("code", Integer.toString(code));
                    redis.hset(con.getName(), data);
                    redis.expire(con.getName(), 300L);
                } else {
                    code = Integer.parseInt(redis.hget(con.getName(), "code"));
                }
                String codeStr = formatOTP(code);

                // Kick the client and
                e.setCancelReason(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                KICK_SUCCESS
                                        .replace("${OTP}", codeStr)
                                        .replace("${Name}", e.getConnection().getName())
                                        .replace("${UUID}", e.getConnection().getUniqueId().toString())
                        )
                ));
            } catch (GeneralSecurityException ex) {
                ex.printStackTrace();
                getLogger().warning("Could not generate an One-Time-Password for " + e.getConnection().getName() +
                        " (" + e.getConnection().getUniqueId().toString() + ")");

                // Notify the client about an error
                e.setCancelReason(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                KICK_ERROR.replace("${Name}", e.getConnection().getName())
                                        .replace("${UUID}", e.getConnection().getUniqueId().toString())
                        )
                ));
            }
            e.completeIntent(instance);
        });
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

package io.mrsmc.mcauth

import com.google.common.io.BaseEncoding
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.Jedis
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

class Mcauth : Plugin() {
    private lateinit var redis: Jedis

    override fun onEnable() {
        redis = Jedis(HostAndPort("localhost", 6379))
        proxy.pluginManager.registerListener(this, McauthListener(this))
    }

    private class McauthListener(private val plugin: Mcauth) : Listener {
        private val pool = Executors.newCachedThreadPool()
        private val redis = plugin.redis

        @EventHandler
        fun onLogin(event: LoginEvent) {
            event.isCancelled = true
            event.registerIntent(plugin)
            val conn = event.connection

            pool.execute {
                try {
                    val code = if (!redis.exists(conn.name)) {
                        val code = TimeBasedOneTimePasswordUtil.generateCurrentNumber(generateSecret(conn.uniqueId))
                        redis.hset(
                            conn.name, hashMapOf(
                                "UUID" to conn.uniqueId.toString(),
                                "code" to code.toString()
                            )
                        )
                        redis.expire(conn.name, 300L)
                        code
                    } else {
                        redis.hget(conn.name, "code").toInt()
                    }

                    val codeStr = formatOTP(code)

                    event.setCancelReason(
                        *TextComponent.fromLegacyText(
                            ChatColor.translateAlternateColorCodes('&', KICK_SUCCESS.format(conn.name, codeStr))
                        )
                    )
                } catch (exception: GeneralSecurityException) {
                    exception.printStackTrace()
                    plugin.logger.warning("Could not generate an One-Time-Password for ${conn.name} (${conn.uniqueId})")

                    event.setCancelReason(*TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', KICK_ERROR)
                    ))
                }
                event.completeIntent(plugin)
            }
        }

        companion object {
            // 인증번호 부여 성공시 킥 메시지
            private const val KICK_SUCCESS = """
            &3&lMystic &c&lRed &d&lSpace

            &6{}&e 님의 인증코드

            &6{}"""

            // 인증번호 부여 성공시 킥 메시지
            private const val KICK_ERROR = """
            &3&lMystic &c&lRed &d&lSpace

            &c&l오류가 발생했습니다!
            &r잠시 후에 다시 시도하거나, 서버에 문의하세요.

            &3https://mrsmc.xyz"""

            private val secretSalt = TimeBasedOneTimePasswordUtil.generateBase32Secret()

            private var SHA_256: MessageDigest? = null

            fun generateSecret(uuid: UUID): String {
                val messageDigest = SHA_256 ?: run {
                    val instance = MessageDigest.getInstance("SHA-256")
                    SHA_256 = instance
                    instance
                }

                return BaseEncoding.base32().encode(messageDigest.digest((uuid.toString() + secretSalt).toByteArray()))
            }

            fun formatOTP(num: Int): String {
                require(num in 0 until 1_000_000) {
                    "Argument num may not consist of more than 6 digits"
                }

                val otp = num.toString().padEnd(6, '0')
                return "${otp.take(3)} ${otp.takeLast(3)}"
            }
        }
    }
}
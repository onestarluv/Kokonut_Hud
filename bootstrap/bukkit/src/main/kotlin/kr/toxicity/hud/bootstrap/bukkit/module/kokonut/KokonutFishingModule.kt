package kr.toxicity.hud.bootstrap.bukkit.module.kokonut

import kr.toxicity.hud.api.bukkit.trigger.HudBukkitEventTrigger
import kr.toxicity.hud.api.listener.HudListener
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.api.yaml.YamlObject
import kr.toxicity.hud.bootstrap.bukkit.module.BukkitModule
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * Kokonut-Fishing HUD 값(컴파일 의존 없음, 플레이어 메타데이터로 받는다).
 *
 * 영기 축적 게이지(`kokonut_fishing_aura`): [fishing_aura](0~100 %), [fishing_aura_visible](1 = 영기 축적 낚싯대를 듦).
 *
 * 다중 투척(`kokonut_fishing_multicast` = double[]{남은 초, 전체 초, 예약 1/0}, 다중 투척 낚싯대를 주 손에 들었을 때만):
 * [fishing_multicast_visible](0/1), [fishing_multicast_state](0 사용 가능, 1 쿨타임, 2 예약됨),
 * [fishing_multicast_cooldown](남은 초), [fishing_multicast_ratio](남은 비율 0~100), [fishing_multicast_time](1분 이상 "13m", 미만 "23s", 올림. 쿨타임 아니면 빈 문자열).
 *
 * 메타데이터는 메인 스레드에서 5틱마다 캐시로 옮기고, 렌더링은 캐시만 읽는다.
 */
class KokonutFishingModule : BukkitModule {
    override val triggers: Map<String, (YamlObject) -> HudBukkitEventTrigger<*>> = emptyMap()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener> = emptyMap()
    override val strings: Map<String, HudPlaceholder<String>> = mapOf(
        "multicast_time" to HudPlaceholder.of { _, _ ->
            Function { player ->
                val seconds = FishingMultiCastCache.get(player.uuid())?.remaining?.toLong() ?: 0L
                when {
                    seconds <= 0 -> ""
                    seconds >= 60 -> "${(seconds + 59) / 60}m"
                    else -> "${seconds}s"
                }
            }
        },
    )
    override val booleans: Map<String, HudPlaceholder<Boolean>> = emptyMap()

    override val numbers: Map<String, HudPlaceholder<Number>> = mapOf(
        "aura" to placeholder { it ?: 0.0 },
        "aura_visible" to placeholder { if (it == null) 0 else 1 },
        "multicast_visible" to multiCast { if (it == null) 0 else 1 },
        "multicast_state" to multiCast { it?.state ?: 0 },
        "multicast_cooldown" to multiCast { it?.remaining ?: 0.0 },
        "multicast_ratio" to multiCast { if (it == null || it.total <= 0) 0.0 else (it.remaining * 100.0 / it.total).coerceIn(0.0, 100.0) },
    )

    override fun start() {
        val plugin = JavaPlugin.getProvidingPlugin(KokonutFishingModule::class.java)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { FishingAuraCache.tick(); FishingMultiCastCache.tick() }, 20L, 5L)
    }

    private fun placeholder(select: (Double?) -> Number): HudPlaceholder<Number> =
        HudPlaceholder.of { _, _ ->
            Function { player -> select(FishingAuraCache.get(player.uuid())) }
        }

    private fun multiCast(select: (FishingMultiCastCache.Value?) -> Number): HudPlaceholder<Number> =
        HudPlaceholder.of { _, _ ->
            Function { player -> select(FishingMultiCastCache.get(player.uuid())) }
        }
}

object FishingAuraCache {
    const val KEY = "kokonut_fishing_aura"
    private val values = ConcurrentHashMap<UUID, Double>()

    fun get(uuid: UUID): Double? = values[uuid]

    fun tick() {
        val online = HashSet<UUID>()
        for (player in Bukkit.getOnlinePlayers()) {
            online += player.uniqueId
            val value = player.getMetadata(KEY).firstOrNull()?.asDouble()
            if (value == null) values.remove(player.uniqueId) else values[player.uniqueId] = value.coerceIn(0.0, 100.0)
        }
        values.keys.retainAll(online)
    }
}

object FishingMultiCastCache {
    const val KEY = "kokonut_fishing_multicast"

    data class Value(val remaining: Double, val total: Double, val reserved: Boolean) {
        val state: Int get() = when { reserved -> 2; remaining > 0 -> 1; else -> 0 }
    }

    private val values = ConcurrentHashMap<UUID, Value>()

    fun get(uuid: UUID): Value? = values[uuid]

    fun tick() {
        val online = HashSet<UUID>()
        for (player in Bukkit.getOnlinePlayers()) {
            online += player.uniqueId
            val raw = player.getMetadata(KEY).firstOrNull()?.value() as? DoubleArray
            if (raw == null || raw.size < 3) values.remove(player.uniqueId)
            else values[player.uniqueId] = Value(raw[0].coerceAtLeast(0.0), raw[1].coerceAtLeast(0.0), raw[2] > 0)
        }
        values.keys.retainAll(online)
    }
}

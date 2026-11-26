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
 * Kokonut-Enchants 쿨타임 각인 HUD 값(컴파일 의존 없음, 플레이어 메타데이터로 받는다). 다중 투척 칸과 같은 모양.
 *
 * 도약(`kokonut_enchant_leap`, 도약 신발을 신었을 때만)·자르반(`kokonut_enchant_rush`, 자르반 창을 주 손에 들었을 때만)
 * = double[]{남은 초, 전체 초}:
 * [enchant_leap_visible](0/1), [enchant_leap_state](0 사용 가능, 1 쿨타임), [enchant_leap_ratio](남은 비율 0~100),
 * [enchant_leap_time]("3s", 쿨타임 아니면 빈 문자열). 자르반은 leap 대신 rush.
 *
 * [enchant_leap_slot]: 도약 칸 자리. 0 = 핫바 오른쪽 첫 칸, 1 = 주 손 스킬 칸(자르반 또는 다중 투척) 옆,
 * 2 = 영기 게이지 + 다중 투척 칸 옆.
 *
 * 메타데이터는 메인 스레드에서 5틱마다 캐시로 옮기고, 렌더링은 캐시만 읽는다.
 */
class KokonutEnchantModule : BukkitModule {
    override val triggers: Map<String, (YamlObject) -> HudBukkitEventTrigger<*>> = emptyMap()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener> = emptyMap()
    override val booleans: Map<String, HudPlaceholder<Boolean>> = emptyMap()

    override val strings: Map<String, HudPlaceholder<String>> = mapOf(
        "leap_time" to time(EnchantSkillCache.leap),
        "rush_time" to time(EnchantSkillCache.rush),
    )

    override val numbers: Map<String, HudPlaceholder<Number>> = mapOf(
        "leap_visible" to number(EnchantSkillCache.leap) { if (it == null) 0 else 1 },
        "leap_state" to number(EnchantSkillCache.leap) { it?.state ?: 0 },
        "leap_ratio" to number(EnchantSkillCache.leap) { it?.ratio ?: 0.0 },
        "rush_visible" to number(EnchantSkillCache.rush) { if (it == null) 0 else 1 },
        "rush_state" to number(EnchantSkillCache.rush) { it?.state ?: 0 },
        "rush_ratio" to number(EnchantSkillCache.rush) { it?.ratio ?: 0.0 },
        "leap_slot" to HudPlaceholder.of { _, _ ->
            Function { player -> EnchantSkillCache.leapSlot(player.uuid()) }
        },
    )

    override fun start() {
        val plugin = JavaPlugin.getProvidingPlugin(KokonutEnchantModule::class.java)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { EnchantSkillCache.tick() }, 20L, 5L)
    }

    private fun number(values: Map<UUID, EnchantSkillCache.Value>, select: (EnchantSkillCache.Value?) -> Number) =
        HudPlaceholder.of { _, _ -> Function { player -> select(values[player.uuid()]) } }

    private fun time(values: Map<UUID, EnchantSkillCache.Value>): HudPlaceholder<String> =
        HudPlaceholder.of { _, _ ->
            Function { player ->
                val remaining = values[player.uuid()]?.remaining ?: 0.0
                if (remaining <= 0) "" else "${kotlin.math.ceil(remaining).toLong()}s"
            }
        }
}

object EnchantSkillCache {
    const val LEAP_KEY = "kokonut_enchant_leap"
    const val RUSH_KEY = "kokonut_enchant_rush"

    data class Value(val remaining: Double, val total: Double) {
        val state: Int get() = if (remaining > 0) 1 else 0
        val ratio: Double get() = if (total <= 0) 0.0 else (remaining * 100.0 / total).coerceIn(0.0, 100.0)
    }

    val leap = ConcurrentHashMap<UUID, Value>()
    val rush = ConcurrentHashMap<UUID, Value>()

    /** 0 = 첫 칸, 1 = 주 손 스킬 칸 옆, 2 = 영기 게이지 + 다중 투척 칸 옆. */
    fun leapSlot(uuid: UUID): Int {
        val multiCast = FishingMultiCastCache.get(uuid) != null
        return when {
            multiCast && FishingAuraCache.get(uuid) != null -> 2
            multiCast || rush.containsKey(uuid) -> 1
            else -> 0
        }
    }

    fun tick() {
        val online = HashSet<UUID>()
        for (player in Bukkit.getOnlinePlayers()) {
            online += player.uniqueId
            read(player, LEAP_KEY, leap)
            read(player, RUSH_KEY, rush)
        }
        leap.keys.retainAll(online)
        rush.keys.retainAll(online)
    }

    private fun read(player: org.bukkit.entity.Player, key: String, values: MutableMap<UUID, Value>) {
        val raw = player.getMetadata(key).firstOrNull()?.value() as? DoubleArray
        if (raw == null || raw.size < 2) values.remove(player.uniqueId)
        else values[player.uniqueId] = Value(raw[0].coerceAtLeast(0.0), raw[1].coerceAtLeast(0.0))
    }
}

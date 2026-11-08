package kr.toxicity.hud.bootstrap.bukkit.module.kokonut

import kr.gomsv.kokonutCore.api.currency.CurrencyAccount
import kr.gomsv.kokonutCore.api.util.CurrencyUtil
import kr.toxicity.hud.api.bukkit.trigger.HudBukkitEventTrigger
import kr.toxicity.hud.api.listener.HudListener
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.api.yaml.YamlObject
import kr.toxicity.hud.bootstrap.bukkit.module.BukkitModule
import java.util.UUID
import java.util.function.Function

/**
 * Kokonut-Core 재화 플레이스홀더: [currency_gold], [currency_silver], [currency_kokonut].
 *
 * HUD는 2틱마다 메인 스레드에서 렌더링하므로 DB를 직접 조회하지 않는다.
 * Core가 모든 플러그인에 공유하는 비동기 재화 스냅샷([CurrencyUtil.snapshot])만 읽는다.
 */
class KokonutCurrencyModule : BukkitModule {
    override val triggers: Map<String, (YamlObject) -> HudBukkitEventTrigger<*>> = emptyMap()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener> = emptyMap()
    override val strings: Map<String, HudPlaceholder<String>> = emptyMap()
    override val booleans: Map<String, HudPlaceholder<Boolean>> = emptyMap()

    override val numbers: Map<String, HudPlaceholder<Number>> = mapOf(
        "gold" to placeholder { it.gold },
        "silver" to placeholder { it.silver },
        "kokonut" to placeholder { it.kokonut },
    )

    private fun placeholder(select: (CurrencyBalanceCache.Balance) -> Number): HudPlaceholder<Number> =
        HudPlaceholder.of { _, _ ->
            Function { player -> select(CurrencyBalanceCache.get(player.uuid())) }
        }
}

/** Core 스냅샷을 HUD 값으로 바꾸는 얇은 어댑터. 첫 조회가 끝나기 전에는 0을 반환한다. */
object CurrencyBalanceCache {
    data class Balance(val gold: Long, val silver: Double, val kokonut: Long)

    private val EMPTY = Balance(0L, 0.0, 0L)

    /** 렌더링 스레드에서 호출. 절대 블로킹하지 않는다. */
    fun get(uuid: UUID): Balance = try {
        CurrencyUtil.snapshot(uuid).map(::toBalance).orElse(EMPTY)
    } catch (_: LinkageError) {
        EMPTY // Core가 없거나 구버전
    }

    private fun toBalance(account: CurrencyAccount) = Balance(account.gold(), account.silver(), account.kokonut())

    /** 캐시는 Core가 소유한다. 기존 종료 호출부 호환용. */
    fun close() = Unit
}

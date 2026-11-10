package kr.toxicity.hud.bootstrap.bukkit.module.kokonut

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kr.toxicity.hud.api.bukkit.trigger.HudBukkitEventTrigger
import kr.toxicity.hud.api.listener.HudListener
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.api.yaml.YamlObject
import kr.toxicity.hud.bootstrap.bukkit.module.BukkitModule
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Method
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.logging.Level

/**
 * 위치 배너 플레이스홀더: [location_title], [location_subtitle], [location_lines](0·1·2).
 *
 * - Kokonut_Guard 영역에 들어서면 영역 이름(`kokonut-hud` 플래그 값, 없으면 영역 ID)을 알린다(어느 월드든, 야생보다 우선).
 * - 이름이 `wild`로 시작하는 월드에서는 바이옴(제목)과 플레이어가 들어가 있는 구조물(부제)을 알린다.
 *
 * 10틱마다 메인 스레드에서 위치를 확인하고, 렌더링 스레드는 [LocationBanner]가 공개한 값만 읽는다.
 */
class KokonutLocationModule : BukkitModule {
    override val triggers: Map<String, (YamlObject) -> HudBukkitEventTrigger<*>> = emptyMap()
    override val listeners: Map<String, (YamlObject) -> (UpdateEvent) -> HudListener> = emptyMap()
    override val booleans: Map<String, HudPlaceholder<Boolean>> = emptyMap()

    override val strings: Map<String, HudPlaceholder<String>> = mapOf(
        "title" to placeholder { it?.title ?: "" },
        "subtitle" to placeholder { it?.subtitle ?: "" },
    )
    override val numbers: Map<String, HudPlaceholder<Number>> = mapOf(
        "lines" to placeholder { it?.lines ?: 0 },
    )

    override fun start() {
        val plugin = JavaPlugin.getProvidingPlugin(KokonutLocationModule::class.java)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { LocationTracker.tick() }, 20L, 10L)
    }

    private fun <T : Any> placeholder(select: (Place?) -> T): HudPlaceholder<T> =
        HudPlaceholder.of { _, _ ->
            Function { player -> select(LocationTracker.visible(player.uuid())) }
        }
}

object LocationTracker {
    private val banners = ConcurrentHashMap<UUID, LocationBanner>()

    fun visible(uuid: UUID): Place? = banners[uuid]?.visible(System.currentTimeMillis())

    fun tick() {
        val now = System.currentTimeMillis()
        val online = HashSet<UUID>()
        for (player in Bukkit.getOnlinePlayers()) {
            online += player.uniqueId
            val place = try {
                placeOf(player)
            } catch (e: RuntimeException) {
                null
            }
            val announced = banners.computeIfAbsent(player.uniqueId) { LocationBanner() }.sample(place, now) ?: continue
            // 배너가 뜨는 순간 본인에게만 들리는 소리: 기본은 책장 넘김, 야생 구조물 발견은 지도 기록 소리.
            val sound = if (announced.subtitle != null) "minecraft:ui.cartography_table.take_result" else "minecraft:item.book.page_turn"
            player.playSound(player, sound, SoundCategory.MASTER, 0.6f, 1.0f)
        }
        banners.keys.retainAll(online)
    }

    private fun placeOf(player: Player): Place? {
        val location = player.location
        GuardRegions.title(location)?.let { return Place(it, region = true) }
        val world = location.world ?: return null
        if (!world.name.startsWith("wild", ignoreCase = true)) return null
        val biome = world.getBiome(location.blockX, location.blockY, location.blockZ)
        val biomeKey = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).getKey(biome)?.asString() ?: return null
        return Place(LocationNames.biome(biomeKey), structureAt(location))
    }

    /** 플레이어가 실제로 들어가 있는(경계 상자 안) 구조물 이름. 청크에 걸친 구조물만 확인한다. */
    private fun structureAt(location: Location): String? {
        val world = location.world ?: return null
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE)
        return world.getStructures(location.blockX shr 4, location.blockZ shr 4)
            .filter { it.boundingBox.contains(location.x, location.y, location.z) }
            .minByOrNull { it.boundingBox.volume } // 겹치면 가장 작은(구체적인) 구조물
            ?.let { registry.getKey(it.structure) }
            ?.let { LocationNames.structure(it.asString()) }
    }
}

/**
 * Kokonut_Guard(WorldGuard) 공개 API를 리플렉션으로 사용해 플레이어가 서 있는 영역의 표시 이름을 구한다.
 *
 * 모든 영역이 대상이다(`__global__` 제외). 겹쳐 있으면 우선순위가 가장 높은 영역을 쓴다.
 * 이름은 그 영역의 `kokonut-hud` 플래그 값, 없으면 영역 ID. 플래그 값이 `none` 또는 `숨김`이면 그 영역은 표시하지 않는다.
 * Guard가 없으면 항상 null.
 */
private object GuardRegions {
    private val hidden = setOf("none", "숨김")

    private class Api(
        val adapt: Method,
        val query: Any,
        val applicableRegions: Method,
        val id: Method,
        val priority: Method,
        val getFlag: Method,
        val flag: Any?,
    )

    private var api: Api? = null
    private var resolvedFor: Any? = null
    private var warned = false

    fun title(location: Location): String? {
        val api = resolve() ?: return null
        return try {
            val regions = api.applicableRegions.invoke(api.query, api.adapt.invoke(null, location)) as Iterable<*>
            val region = regions.filterNotNull().maxByOrNull { api.priority.invoke(it) as Int } ?: return null
            val name = api.flag?.let { api.getFlag.invoke(region, it) as? String }?.trim()?.takeIf { it.isNotEmpty() }
                ?: return api.id.invoke(region) as String
            name.takeUnless { it.lowercase() in hidden }
        } catch (e: ReflectiveOperationException) {
            warn(e)
            null
        }
    }

    private fun resolve(): Api? {
        val plugin = Bukkit.getPluginManager().getPlugin("WorldGuard")?.takeIf { it.isEnabled } ?: return null
        if (resolvedFor === plugin) return api
        resolvedFor = plugin
        api = try {
            val cl = plugin.javaClass.classLoader
            val worldGuard = cl.loadClass("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null)
            val flag = worldGuard.javaClass.getMethod("getFlagRegistry").invoke(worldGuard).let {
                it.javaClass.getMethod("get", String::class.java).invoke(it, "kokonut-hud")
            }
            val platform = worldGuard.javaClass.getMethod("getPlatform").invoke(worldGuard)
            val container = platform.javaClass.getMethod("getRegionContainer").invoke(platform)
            val query = container.javaClass.getMethod("createQuery").invoke(container)
            val region = cl.loadClass("com.sk89q.worldguard.protection.regions.ProtectedRegion")
            Api(
                cl.loadClass("com.sk89q.worldedit.bukkit.BukkitAdapter").getMethod("adapt", Location::class.java),
                query,
                query.javaClass.getMethod("getApplicableRegions", cl.loadClass("com.sk89q.worldedit.util.Location")),
                region.getMethod("getId"),
                region.getMethod("getPriority"),
                region.getMethod("getFlag", cl.loadClass("com.sk89q.worldguard.protection.flags.Flag")),
                flag,
            )
        } catch (e: ReflectiveOperationException) {
            warn(e)
            null
        }
        return api
    }

    private fun warn(e: Exception) {
        if (warned) return
        warned = true
        Bukkit.getLogger().log(Level.WARNING, "[Kokonut_HUD] Kokonut_Guard 영역 연동 실패. 영역 배너를 표시하지 않습니다.", e)
    }
}

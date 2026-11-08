package kr.toxicity.hud.bootstrap.bukkit

import kr.toxicity.hud.BetterHudImpl
import kr.toxicity.hud.api.BetterHud
import kr.toxicity.hud.api.BetterHudAPI
import kr.toxicity.hud.api.BetterHudLogger
import kr.toxicity.hud.api.adapter.WorldWrapper
import kr.toxicity.hud.api.bukkit.BukkitBootstrap
import kr.toxicity.hud.api.bukkit.bedrock.BedrockAdapter
import kr.toxicity.hud.api.bukkit.event.HudPlayerJoinEvent
import kr.toxicity.hud.api.bukkit.event.HudPlayerQuitEvent
import kr.toxicity.hud.api.bukkit.event.PluginReloadStartEvent
import kr.toxicity.hud.api.bukkit.event.PluginReloadedEvent
import kr.toxicity.hud.api.bukkit.nms.NMS
import kr.toxicity.hud.api.manager.ConfigManager
import kr.toxicity.hud.api.placeholder.HudPlaceholder
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.scheduler.HudScheduler
import kr.toxicity.hud.api.version.MinecraftVersion
import kr.toxicity.hud.bootstrap.bukkit.manager.ModuleManager
import kr.toxicity.hud.bootstrap.bukkit.player.HudPlayerBukkit
import kr.toxicity.hud.bootstrap.bukkit.util.bukkitPlayer
import kr.toxicity.hud.bootstrap.bukkit.util.call
import kr.toxicity.hud.bootstrap.bukkit.util.registerListener
import kr.toxicity.hud.manager.*
import kr.toxicity.hud.pack.PackUploader
import kr.toxicity.hud.scheduler.BukkitScheduler
import kr.toxicity.hud.util.*
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStream
import java.net.URLClassLoader

@Suppress("UNUSED")
class BukkitBootstrapImpl : BukkitBootstrap, JavaPlugin() {

    private val listener = object : Listener {}

    private val isFolia = false
    private val isPaper = true
    private val scheduler = BukkitScheduler(this)
    private val minecraftVersion by lazy {
        Bukkit.getBukkitVersion()
            .substringBefore('-')
            .toMinecraftVersion()
    }

    private val log = object : BetterHudLogger {
        override fun info(vararg message: String) {
            val l = logger
            synchronized(l) {
                message.forEach {
                    l.info(it)
                }
            }
        }
        override fun warn(vararg message: String) {
            val l = logger
            synchronized(l) {
                message.forEach {
                    l.warning(it)
                }
            }
        }
    }

    private val core = BetterHudImpl(this).apply {
        BetterHudAPI.inst(this)
        addReloadStartTask {
            scheduler.asyncTask {
                PluginReloadStartEvent().call()
            }
        }
        addReloadEndTask {
            scheduler.asyncTask {
                PluginReloadedEvent(it).call()
            }
        }
        addReloadStartTask {
            HandlerList.unregisterAll(listener)
        }
    }

    private lateinit var nms: NMS

    private val bedrockAdapter = BedrockAdapter { false }

    override fun isFolia(): Boolean = isFolia
    override fun volatileCode(): NMS = nms
    override fun bedrockAdapter(): BedrockAdapter = bedrockAdapter

    override fun isPaper(): Boolean = isPaper
    override fun scheduler(): HudScheduler = scheduler
    override fun jarFile(): File = file
    override fun core(): BetterHud = core
    override fun version(): String = description.version

    var skipInitialReload = false

    override fun onLoad() {
        val pluginManager = Bukkit.getPluginManager()
        check(Bukkit.getMinecraftVersion() == "26.3") { "Paper 26.3 required" }
        nms = kr.toxicity.hud.nms.v26_R2.NMSImpl()
        nms.registerCommand(CommandManager.module)
    }


    override fun onEnable() {
        nms.handleReloadCommand(CommandManager.module)
        val pluginManager = Bukkit.getPluginManager()
        pluginManager.registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.HIGHEST)
            fun PlayerJoinEvent.join() {
                register(player)
            }
            @EventHandler
            fun PlayerQuitEvent.quit() {
                val player = player
                PlayerManagerImpl.removeHudPlayer(player.uniqueId)?.let {
                    it.cancel()
                    HudPlayerQuitEvent(it).call()
                }
            }
        }, this)
        ModuleManager.start()
        Bukkit.getOnlinePlayers().forEach {
            register(it)
        }
        core.start()
        registerListener(object : Listener {
            @EventHandler
            fun ServerLoadEvent.load() {
                debug(ConfigManager.DebugLevel.MANAGER,"Initialized: $type")
                if (!skipInitialReload) {
                    core.reload()
                }
                log.info(
                    "Minecraft version: $minecraftVersion, NMS version: ${nms.version}",
                    "Platform: Paper",
                    "Plugin enabled."
                )
            }
        })
    }

    override fun onDisable() {
        core.end()
        kr.toxicity.hud.bootstrap.bukkit.module.kokonut.CurrencyBalanceCache.close()
        Bukkit.getScheduler().cancelTasks(this)
        log.info("Plugin disabled.")
    }

    fun register(player: Player) {
        if (!player.isOnline) return
        if (ConfigManagerImpl.disableToBedrockPlayer && bedrockAdapter.isBedrockPlayer(player.uniqueId)) return
        val adaptedPlayer = player
        val audience = PlayerManagerImpl.addHudPlayer(adaptedPlayer.uniqueId) {
            val impl = HudPlayerBukkit(adaptedPlayer, player)
            task {
                if (player.isOnline && PlayerManagerImpl.getHudPlayer(player.uniqueId) === impl) {
                    sendResourcePack(impl)
                    player.updateCommands()
                    HudPlayerJoinEvent(impl).call()
                }
            }
            impl
        }
    }

    override fun resource(path: String): InputStream? = getResource(path)
    override fun logger(): BetterHudLogger = log
    override fun dataFolder(): File = dataFolder
    override fun console(): Audience = Bukkit.getConsoleSender().let {
        it
    }

    override fun startMetrics() {}
    override fun endMetrics() {}

    override fun sendResourcePack(player: HudPlayer) {
        PackUploader.server?.let {
            (player.handle() as Player).setResourcePack(it.uuid, it.url, it.digest, null as Component?, false)
        }
    }
    override fun sendResourcePack() {
        PackUploader.server?.let {
            Bukkit.getOnlinePlayers().forEach { player ->
                player.setResourcePack(it.uuid, it.url, it.digest, null as Component?, false)
            }
        }
    }

    override fun minecraftVersion(): MinecraftVersion = minecraftVersion
    override fun mcmetaVersion(): Int = nms.version.metaVersion
    override fun triggerListener(): Listener = listener

    override fun world(name: String): WorldWrapper? {
        return Bukkit.getWorld(name)?.let {
            WorldWrapper(it.name)
        }
    }

    override fun worlds(): List<WorldWrapper> = Bukkit.getWorlds().map {
        WorldWrapper(it.name)
    }

    override fun classloader(): URLClassLoader {
        return javaClass.classLoader as URLClassLoader
    }
}

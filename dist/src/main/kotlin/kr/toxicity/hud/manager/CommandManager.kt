package kr.toxicity.hud.manager

import kr.toxicity.command.BetterCommandSource
import kr.toxicity.command.CommandListener
import kr.toxicity.command.SenderType
import kr.toxicity.command.impl.BetterCommand
import kr.toxicity.command.impl.ClassSerializer
import kr.toxicity.command.impl.CommandMessage
import kr.toxicity.command.impl.annotation.*
import kr.toxicity.hud.api.configuration.HudObjectType
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.plugin.ReloadFlagType
import kr.toxicity.hud.api.plugin.ReloadInfo
import kr.toxicity.hud.api.plugin.ReloadState.*
import kr.toxicity.hud.api.update.UpdateEvent
import kr.toxicity.hud.command.*
import kr.toxicity.hud.placeholder.PlaceholderSource
import kr.toxicity.hud.resource.GlobalResource
import kr.toxicity.hud.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.io.File

object CommandManager : BetterHudManager {

    override val managerName: String = "Command"
    override val supportExternalPacks: Boolean = false

    val library = BetterCommand(DATA_FOLDER.subFolder("lang"), MiniMessage.miniMessage(), BOOTSTRAP.logger())
        .exceptionHandler {
            if (ConfigManagerImpl.debug()) {
                warn(
                    "Stack trace:",
                    it.stackTraceToString()
                )
            }
        }
        .addSerializer(HudPlayerStack::class.java, ClassSerializer.builder { _, s ->
            if (s == "all") HudPlayerStack(PlayerManagerImpl.allHudPlayer)
            else PlayerManagerImpl.getHudPlayer(s)?.let {
                HudPlayerStack(listOf(it))
            }
        }.name("player")
            .suggests { PlayerManagerImpl.allHudPlayer.map { it.name() } + "all" }
            .nullMessage(CommandMessage("betterhud.null.player", Component.text("플레이어를 찾을 수 없습니다: [value]")))
            .build()
        )
        .addSerializer(HudStack::class.java, ClassSerializer.builder { _, s ->
            when (s) {
                "all" -> HudStack(HudObjectType.HUD.all())
                "default" -> HudStack(HudObjectType.HUD.defaultObjects().toList())
                else -> HudManagerImpl.getHud(s)?.let {
                    HudStack(listOf(it))
                }
            }
        }.name("hud")
            .suggests { HudManagerImpl.allNames.toList() + "all" }
            .nullMessage(CommandMessage("betterhud.null.hud", Component.text("HUD를 찾을 수 없습니다: [value]")))
            .build()
        )
        .silentLog {
            !ConfigManagerImpl.debug()
        }

    @Suppress("UNUSED")
    val module = library.module<BetterCommandSource>("hud")
        .aliases(arrayOf("betterhud", "bh"))
        .permission("hud")
        .executes(object : CommandListener {

            //Reload
            private val reload_tryReload = library.registerKey(CommandMessage("betterhud.reload.message.try_reload", Component.text("다시 불러오는 중...")))
            private val reload_onReload = library.registerKey(CommandMessage("betterhud.reload.message.on_reload", Component.text("이미 다시 불러오는 중입니다.")))
            private val reload_success = library.registerKey(CommandMessage("betterhud.reload.message.success", Component.text("다시 불러왔습니다. ([time] ms)")))
            private val reload_failure1 = library.registerKey(CommandMessage("betterhud.reload.message.failure.1", Component.text("다시 불러오지 못했습니다.")))
            private val reload_failure2 = library.registerKey(CommandMessage("betterhud.reload.message.failure.2", Component.text("원인: [cause]")))
            @Command
            @Description(key = "betterhud.reload.description", defaultValue = "HUD 설정과 리소스팩을 다시 불러옵니다.")
            @Aliases(aliases = ["re", "rl"])
            @Permission("betterhud.reload")
            fun reload(@Source me: BetterCommandSource, @Option @CanBeNull args: String?) {
                reload_tryReload.send(me)
                asyncTask {
                    when (val reload = PLUGIN.reload(me, *args?.split(' ')?.let { ReloadFlagType.from(it).toTypedArray() } ?: emptyArray())) {
                        is OnReload -> reload_onReload.send(me)
                        is Success -> reload_success.send(me, mapOf("time" to reload.time.withDecimal()))
                        is Failure -> {
                            reload_failure1.send(me)
                            reload_failure2.send(me, mapOf("cause" to Component.text("${reload.throwable.javaClass.simpleName}: ${reload.throwable.message}")))
                        }
                    }
                }
            }
            //Reload

            //Parse
            private val parse_failure1 = library.registerKey(CommandMessage("betterhud.parse.message.failure.1", Component.text("변환하지 못했습니다.")))
            private val parse_failure2 = library.registerKey(CommandMessage("betterhud.parse.message.failure.2", Component.text("원인: [cause]")))
            @Command
            @Description(key = "betterhud.parse.description", defaultValue = "플레이스홀더 출력을 확인합니다.")
            @Aliases(aliases = ["p"])
            @Permission("betterhud.parse")
            @Sender(type = [SenderType.PLAYER])
            fun parse(@Source me: HudPlayer, @Vararg argument: String) {
                runCatching {
                    me.audience().sendMessage(
                        MiniMessage
                            .miniMessage()
                            .deserialize(PlaceholderManagerImpl.parse(argument, PlaceholderSource.empty)(UpdateEvent.EMPTY)(me))
                    )
                }.onFailure { e ->
                    parse_failure1.send(me)
                    parse_failure2.send(me, mapOf("cause" to Component.text("${e.javaClass.simpleName}: ${e.message}")))
                }
            }
            //Parse

        }).children("hud") {
            it.permission("betterhud.hud")
                .description(CommandMessage("betterhud.hud.description", Component.text("플레이어 HUD 관리")))
                .executes(object : CommandListener {
                    //Hud add
                    private val add_success = library.registerKey(CommandMessage("betterhud.hud.add.message.success", Component.text("추가했습니다.")))
                    private val add_failure = library.registerKey(CommandMessage("betterhud.hud.add.message.failure", Component.text("[player]님은 이미 '[hud]' HUD를 사용 중입니다.")))
                    @Command
                    @Description(key = "betterhud.hud.add.description", defaultValue = "플레이어에게 HUD를 추가합니다.")
                    @Aliases(aliases = ["a"])
                    @Permission("betterhud.hud.add")
                    fun add(@Source me: BetterCommandSource, players: HudPlayerStack, huds: HudStack) {
                        var stack = 0
                        players.forEach { player ->
                            huds.forEach { hud ->
                                val success = hud.add(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "hud" to Component.text(hud.name)
                                    )
                                    if (success) add_success.send(me, map)
                                    else add_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Hud add

                    //Hud remove
                    private val remove_success = library.registerKey(CommandMessage("betterhud.hud.remove.message.success", Component.text("제거했습니다.")))
                    private val remove_failure = library.registerKey(CommandMessage("betterhud.hud.remove.message.failure", Component.text("[player]님은 '[hud]' HUD를 사용하고 있지 않습니다.")))
                    @Command
                    @Description(key = "betterhud.hud.remove.description", defaultValue = "플레이어의 HUD를 제거합니다.")
                    @Aliases(aliases = ["r"])
                    @Permission("betterhud.hud.remove")
                    fun remove(@Source me: BetterCommandSource, players: HudPlayerStack, huds: HudStack) {
                        var stack = 0
                        players.forEach { player ->
                            huds.forEach { hud ->
                                val success = hud.remove(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "hud" to Component.text(hud.name)
                                    )
                                    if (success) remove_success.send(me, map)
                                    else remove_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Hud remove
                })
        }.children("turn") {
            it.aliases(arrayOf("t"))
                .description(CommandMessage("betterhud.turn.description", Component.text("HUD 켜기/끄기")))
                .permission("betterhud.turn")
                .executes(object : CommandListener {
                    //Turn on
                    private val on_no_target = library.registerKey(CommandMessage("betterhud.turn.on.message.no_target", Component.text("대상 플레이어를 지정해 주세요.")))
                    private val on_success = library.registerKey(CommandMessage("betterhud.turn.on.message.success", Component.text("HUD를 켰습니다.")))
                    @Command
                    @Description(key = "betterhud.turn.on.description", defaultValue = "HUD를 켭니다.")
                    @Permission("betterhud.turn.on")
                    fun on(@Source me: BetterCommandSource, @Option target: HudPlayerStack?) {
                        when {
                            target != null && me.hasPermission("betterhud.turn.on.admin") -> target.forEach { p ->
                                p.isHudEnabled = true
                            }
                            me is HudPlayer -> me.isHudEnabled = true
                            else -> return on_no_target.send(me)
                        }
                        on_success.send(me)
                    }
                    //Turn on

                    //Turn off
                    private val off_no_target = library.registerKey(CommandMessage("betterhud.turn.off.message.no_target", Component.text("대상 플레이어를 지정해 주세요.")))
                    private val off_success = library.registerKey(CommandMessage("betterhud.turn.off.message.success", Component.text("HUD를 껐습니다.")))
                    @Command
                    @Description(key = "betterhud.turn.off.description", defaultValue = "HUD를 끕니다.")
                    @Permission("betterhud.turn.off")
                    fun off(@Source me: BetterCommandSource, @Option target: HudPlayerStack?) {
                        when {
                            target != null && me.hasPermission("betterhud.turn.off.admin") -> target.forEach { p ->
                                p.isHudEnabled = false
                            }
                            me is HudPlayer -> me.isHudEnabled = false
                            else -> return off_no_target.send(me)
                        }
                        off_success.send(me)
                    }
                    //Turn off
                })
        }


    override fun start() {
        // 명령어 문구는 코드 기본값(한국어)을 쓰고, 도움말 형식 등 라이브러리 내부 문구만 ko_KR로 덮어쓴다.
        BOOTSTRAP.resource("lang/ko_KR.json")?.use { input ->
            File(DATA_FOLDER.subFolder("lang"), "ko_KR.json").outputStream().use { input.copyTo(it) }
        }
        library.reload()
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
        // Messages use the registered built-in defaults.
    }

    override fun end() {
    }
}

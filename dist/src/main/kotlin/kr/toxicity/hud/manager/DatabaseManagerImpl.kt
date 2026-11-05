package kr.toxicity.hud.manager
import kr.toxicity.hud.api.database.HudDatabase
import kr.toxicity.hud.api.database.HudDatabaseConnector
import kr.toxicity.hud.api.manager.DatabaseManager
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.plugin.ReloadInfo
import kr.toxicity.hud.resource.GlobalResource
import java.io.File
object DatabaseManagerImpl : BetterHudManager, DatabaseManager {
 override val managerName = "Session"
 override val supportExternalPacks = false
 private val session = object : HudDatabase {
 override fun isClosed() = false
 override fun close() {}
 override fun load(player: HudPlayer) {}
 override fun save(player: HudPlayer) = true
 }
 override fun getCurrentDatabase(): HudDatabase = session
 override fun addDatabase(name: String, connector: HudDatabaseConnector) = false
 override fun start() {}
 override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {}
 override fun end() {}
}

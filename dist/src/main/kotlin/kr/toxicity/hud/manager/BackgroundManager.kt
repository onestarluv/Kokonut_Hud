package kr.toxicity.hud.manager

import kr.toxicity.hud.api.plugin.ReloadInfo
import kr.toxicity.hud.background.HudBackground
import kr.toxicity.hud.location.PixelLocation
import kr.toxicity.hud.pack.PackGenerator
import kr.toxicity.hud.resource.GlobalResource
import kr.toxicity.hud.util.*
import java.io.File

//TODO replace it to proper background in the future.
object BackgroundManager : BetterHudManager {

    override val managerName: String = "Background"
    override val supportExternalPacks: Boolean = true

    private val backgroundMap = HashMap<String, HudBackground>()

    override fun start() {

    }

    fun getBackground(name: String) = backgroundMap[name]

    override fun preReload() {
        backgroundMap.clear()
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
    }

    override fun end() {
    }
}
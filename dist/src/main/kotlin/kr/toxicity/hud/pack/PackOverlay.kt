package kr.toxicity.hud.pack

import kr.toxicity.hud.util.PLUGIN

enum class PackOverlay(
    val overlayName: String,
    val shaderCoreName: String,
    val minVersion: Int,
    val maxVersion: Int
) {
    V26_3("betterhud_26_3", "text",97, 99)
    ;
    fun loadAssets() {
        PLUGIN.loadAssets(overlayName) { n, i ->
            val read = i.readAllBytes()
            PackGenerator.addTask(buildList {
                add(overlayName)
                addAll(n.split('/'))
            }) {
                read
            }
        }
    }
}
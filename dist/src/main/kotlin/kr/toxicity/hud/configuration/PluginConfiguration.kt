package kr.toxicity.hud.configuration

import kr.toxicity.hud.api.yaml.YamlObject
import kr.toxicity.hud.util.BOOTSTRAP
import kr.toxicity.hud.util.DATA_FOLDER
import kr.toxicity.hud.util.ifNull
import kr.toxicity.hud.util.toYaml
import java.io.File

enum class PluginConfiguration(
    private val dir: String
) {
    CONFIG("config.yml"),
    FONT("font.yml"),
    SHADER("shader.yml")
    ;

    fun create(): YamlObject {
        return BOOTSTRAP.resource(dir).ifNull { "Missing embedded defaults: $dir" }.use { it.toYaml() }
    }
}

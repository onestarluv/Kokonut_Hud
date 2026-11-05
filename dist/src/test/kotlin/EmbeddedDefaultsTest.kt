import org.yaml.snakeyaml.Yaml
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class EmbeddedDefaultsTest {
    private fun yaml(path: String): Map<String, Any> =
        javaClass.getResourceAsStream("/$path")!!.use { Yaml().load(it) }

    @Test fun defaultHudExistsAndLayoutIsBundled() {
        val config = yaml("config.yml")
        val huds = yaml("default/huds/default-hud.yml")
        for (name in config["default-hud"] as List<*>) assertTrue(name in huds)
        assertTrue(yaml("default/layouts/default-layout.yml").isNotEmpty())
        assertTrue(yaml("default/images/default-image.yml").isNotEmpty())
        assertEquals("Kokonut_HUD/build", config["build-folder-location"])
    }
}

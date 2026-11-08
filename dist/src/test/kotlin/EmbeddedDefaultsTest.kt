import org.yaml.snakeyaml.Yaml
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import javax.imageio.ImageIO

class EmbeddedDefaultsTest {
    private fun yaml(path: String): Map<String, Any> =
        javaClass.getResourceAsStream("/$path")!!.use { Yaml().load(it) }

    @Test fun defaultHudExistsAndLayoutIsBundled() {
        val config = yaml("config.yml")
        val huds = yaml("default/huds/default-hud.yml")
        for (name in config["default-hud"] as List<*>) assertTrue(name in huds)
        assertTrue(yaml("default/layouts/default-layout.yml").isNotEmpty())
        assertTrue(yaml("default/images/default-image.yml").isNotEmpty())
        assertEquals("Nexo/pack/external_packs/Kokonut_HUD", config["build-folder-location"])
    }

    @Test fun currencyHudReferencesBundledImagesAndFont() {
        val images = yaml("default/images/default-image.yml")
        val layout = yaml("default/layouts/default-layout.yml")["currency_bar"] as Map<*, *>
        val fonts = yaml("default/texts/currency-font.yml")
        for (entry in (layout["images"] as Map<*, *>).values) {
            val image = images[(entry as Map<*, *>)["name"]] as Map<*, *>
            assertTrue(javaClass.getResource("/default/assets/${image["file"]}") != null, "missing ${image["file"]}")
        }
        val patterns = (layout["texts"] as Map<*, *>).values.map { (it as Map<*, *>)["pattern"] }
        assertEquals(listOf("[currency_silver]", "[currency_gold]", "[currency_kokonut]"), patterns)
        for (entry in (layout["texts"] as Map<*, *>).values) assertTrue((entry as Map<*, *>)["name"] in fonts)
    }

    @Test fun currencyImagesDoNotOverlapAndBalancesMatchTheirIcons() {
        val images = yaml("default/images/default-image.yml")
        val layout = yaml("default/layouts/default-layout.yml")["currency_bar"] as Map<*, *>
        val imageEntries = (layout["images"] as Map<*, *>).values.map { it as Map<*, *> }
        val textEntries = (layout["texts"] as Map<*, *>).values.map { it as Map<*, *> }
        assertEquals(imageEntries.size, textEntries.size)
        var previousRight = Double.NEGATIVE_INFINITY
        for ((image, text) in imageEntries.zip(textEntries)) {
            val definition = images[image["name"]] as Map<*, *>
            val bitmap = javaClass.getResourceAsStream("/default/assets/${definition["file"]}")!!.use { ImageIO.read(it) }
            val scale = (image["scale"] as? Number)?.toDouble() ?: 1.0
            val left = (image["x"] as Number).toDouble()
            val top = (image["y"] as Number).toDouble()
            val right = left + bitmap.width * scale
            val bottom = top + bitmap.height * scale
            assertTrue(left > previousRight, "currency backgrounds must have a gap")
            assertEquals("[${image["name"]}]", text["pattern"], "balance must match the currency icon")
            assertTrue((text["x"] as Number).toDouble() in (left + 20 * scale)..<right, "balance must start after the icon, inside its background")
            assertTrue((text["y"] as Number).toDouble() in top..<bottom, "balance must be vertically inside its background")
            previousRight = right
        }
    }
}

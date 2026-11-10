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

    @Test fun currencyChipsKeepPixelArtSharp() {
        // Pixel art only stays even at whole-number scales; the chips are drawn at their final size (13 px tall, around the 9 px mini icon).
        // Silver balances run large, gold and kokonut stay within thousands, so silver gets the widest chip.
        val images = yaml("default/images/default-image.yml")
        val layout = yaml("default/layouts/default-layout.yml")["currency_bar"] as Map<*, *>
        val widths = mutableMapOf<String, Int>()
        for (entry in (layout["images"] as Map<*, *>).values.map { it as Map<*, *> }) {
            assertEquals(1.0, (entry["scale"] as? Number)?.toDouble() ?: 1.0, "chip scale")
            val file = (images[entry["name"]] as Map<*, *>)["file"]
            val bitmap = javaClass.getResourceAsStream("/default/assets/$file")!!.use { ImageIO.read(it) }
            assertEquals(13, bitmap.height)
            widths[entry["name"].toString()] = bitmap.width
        }
        assertEquals(77, widths["currency_silver"]); assertEquals(51, widths["currency_gold"]); assertEquals(51, widths["currency_kokonut"])
    }

    @Test fun locationBannerIsCentredAndSwitchesByLineCount() {
        val images = yaml("default/images/default-image.yml")
        val layouts = yaml("default/layouts/default-layout.yml")
        val hud = yaml("default/huds/default-hud.yml")["location_hud"] as Map<*, *>
        for (entry in (hud["layouts"] as Map<*, *>).values.map { it as Map<*, *> }) {
            assertEquals(50, ((entry["gui"] as Map<*, *>)["x"] as Number).toInt(), "banner sits at the top centre")
        }
        for ((name, lines, height) in listOf(Triple("location_banner", "1", 20), Triple("location_banner_tall", "2", 34))) {
            val layout = layouts[name] as Map<*, *>
            assertEquals("center", layout["offset"]); assertEquals("center", layout["align"])
            val condition = (layout["conditions"] as Map<*, *>).values.single() as Map<*, *>
            assertEquals("location_lines", condition["first"]); assertEquals(lines, condition["second"])
            val image = (layout["images"] as Map<*, *>).values.single() as Map<*, *>
            val file = (images[image["name"]] as Map<*, *>)["file"]
            val bitmap = javaClass.getResourceAsStream("/default/assets/$file")!!.use { ImageIO.read(it) }
            assertEquals(176, bitmap.width); assertEquals(height, bitmap.height)
            val texts = (layout["texts"] as Map<*, *>).values.map { it as Map<*, *> }
            assertEquals(lines.toInt(), texts.size)
            for (text in texts) {
                assertTrue((text["y"] as Number).toInt() + 8 < height - 3, "text stays on the plate")
                assertEquals("location_font", text["name"], "the currency font has no Hangul glyphs")
            }
        }
        val font = yaml("default/texts/location-font.yml")["location_font"] as Map<*, *>
        assertTrue("korean" in font["include"] as List<*>, "banner names are Korean")
        assertTrue(javaClass.getResource("/default/fonts/${font["file"]}") != null, "Galmuri is bundled")
        assertEquals(10, font["scale"], "Galmuri11 is only pixel-exact on its 10 px grid")
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
            assertTrue((text["x"] as Number).toDouble() in (left + 13 * scale)..<right, "balance must start after the icon, inside its background")
            assertTrue((text["y"] as Number).toDouble() in top..<bottom, "balance must be vertically inside its background")
            previousRight = right
        }
    }
}

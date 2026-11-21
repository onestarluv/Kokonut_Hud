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

        val font = fonts["currency_font"] as Map<*, *>
        assertEquals("bitmap", font["type"])
        val atlas = (font["chars"] as Map<*, *>)["balance"] as Map<*, *>
        val glyphs = (atlas["codepoints"] as List<*>).single() as String
        val bitmap = javaClass.getResourceAsStream("/default/assets/${atlas["file"]}")!!.use { ImageIO.read(it) }
        assertEquals("0123456789,.-", glyphs)
        assertEquals(8, bitmap.height)
        val cellWidth = bitmap.width / glyphs.length
        assertEquals(bitmap.width, cellWidth * glyphs.length)
        for (index in glyphs.indices) {
            assertTrue((0 until bitmap.height).any { row ->
                (0 until cellWidth).any { column -> bitmap.getRGB(index * cellWidth + column, row) ushr 24 != 0 }
            }, "missing glyph ${glyphs[index]}")
        }
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
            val textTop = (text["y"] as Number).toDouble()
            assertEquals(1.0, (text["scale"] as Number).toDouble(), "balance bitmap scale")
            assertTrue(textTop >= top + 2 && textTop + 8 <= bottom - 2, "balance fits inside the chip face")
            previousRight = right
        }
    }

    @Test fun fishingAuraGaugeFitsItsFrame() {
        val images = yaml("default/images/default-image.yml")
        val layout = yaml("default/layouts/default-layout.yml")["fishing_aura_gauge"] as Map<*, *>
        val frame = (images["fishing_aura_frame"] as Map<*, *>)["file"]
        val fill = images["fishing_aura_fill"] as Map<*, *>
        assertEquals("listener", fill["type"])
        val frameImage = javaClass.getResourceAsStream("/default/assets/$frame")!!.use { ImageIO.read(it) }
        val fillImage = javaClass.getResourceAsStream("/default/assets/${fill["file"]}")!!.use { ImageIO.read(it) }
        assertEquals(13, frameImage.width) // 세로 게이지: 재화 칩과 같은 13px 두께
        assertEquals("up", fill["split-type"])
        assertEquals(fillImage.height, fill["split"])
        val fillEntry = (layout["images"] as Map<*, *>).values.map { it as Map<*, *> }.single { it["name"] == "fishing_aura_fill" }
        val x = fillEntry["x"] as Int; val y = fillEntry["y"] as Int
        assertTrue(x + fillImage.width <= frameImage.width - 2 && y + fillImage.height <= frameImage.height - 2, "fill inside the frame")
        assertTrue("fishing_aura_hud" in yaml("default/huds/default-hud.yml"))
    }

    @Test fun fishingMultiCastSkillSlotFitsItsFrame() {
        val images = yaml("default/images/default-image.yml")
        fun image(name: String) = javaClass.getResourceAsStream("/default/assets/${(images[name] as Map<*, *>)["file"]}")!!.use { ImageIO.read(it) }
        val frame = image("fishing_multicast_frame")
        val icon = image("fishing_multicast_icon")
        val cooldown = image("fishing_multicast_cooldown")
        assertEquals(16, icon.width); assertEquals(16, icon.height) // 스킬 아이콘은 16x16
        assertEquals(icon.width, cooldown.width); assertEquals(icon.height, cooldown.height)
        assertEquals(frame.width, image("fishing_multicast_reserved").width)
        val fill = images["fishing_multicast_cooldown"] as Map<*, *>
        assertEquals("listener", fill["type"]); assertEquals(cooldown.height, fill["split"])
        val layouts = yaml("default/layouts/default-layout.yml")
        for (name in listOf("fishing_multicast_skill", "fishing_multicast_skill_shifted")) {
            val entries = ((layouts[name] as Map<*, *>)["images"] as Map<*, *>).values.map { it as Map<*, *> }
            val iconEntry = entries.single { it["name"] == "fishing_multicast_icon" }
            val x = iconEntry["x"] as Int; val y = iconEntry["y"] as Int
            assertTrue(x >= 2 && y >= 2 && x + icon.width <= frame.width - 2 && y + icon.height <= frame.height - 2, "icon inside the frame")
        }
        assertTrue("fishing_multicast_hud" in yaml("default/huds/default-hud.yml"))
        assertTrue("multicast_font" in yaml("default/texts/multicast-font.yml"))
    }
}

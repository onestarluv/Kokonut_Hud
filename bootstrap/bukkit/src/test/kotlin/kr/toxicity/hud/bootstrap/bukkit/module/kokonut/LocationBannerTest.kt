package kr.toxicity.hud.bootstrap.bukkit.module.kokonut

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocationBannerTest {
    private val market = Place("코코넛 항구 시장", region = true)
    private val plains = Place("평원")
    private val river = Place("강")

    @Test fun firstSampleIsAnnouncedAtOnce() {
        val banner = LocationBanner()
        banner.sample(plains, 0)
        assertEquals(plains, banner.visible(0))
        assertEquals(plains, banner.visible(3_999))
        assertNull(banner.visible(4_000))
    }

    @Test fun regionIsAnnouncedOnEntryWithoutSettling() {
        val banner = LocationBanner()
        banner.sample(null, 0)
        banner.sample(market, 500)
        assertEquals(market, banner.visible(500))
    }

    @Test fun wildPlaceWaitsUntilItSettles() {
        val banner = LocationBanner()
        banner.sample(null, 0)
        banner.sample(river, 10_000)
        banner.sample(plains, 10_500) // crossed the river quickly
        assertNull(banner.visible(10_500))
        banner.sample(plains, 11_000)
        assertNull(banner.visible(11_000))
        banner.sample(plains, 11_500)
        assertEquals(plains, banner.visible(11_500))
    }

    @Test fun reenteringSoonIsNotAnnouncedAgain() {
        val banner = LocationBanner()
        banner.sample(market, 0)
        banner.sample(null, 5_000)
        banner.sample(market, 10_000)
        assertNull(banner.visible(10_000))
        banner.sample(null, 20_000)
        banner.sample(market, 31_000)
        assertEquals(market, banner.visible(31_000))
    }

    @Test fun sampleReportsTheMomentOfAnnouncementOnce() {
        val banner = LocationBanner()
        assertEquals(market, banner.sample(market, 0))
        assertNull(banner.sample(market, 500))
        assertNull(banner.sample(null, 1_000))
    }

    @Test fun twoLinesOnlyWithStructure() {
        assertEquals(1, plains.lines)
        assertEquals(2, Place("평원", "마을").lines)
    }

    @Test fun namesAreKoreanWithReadableFallback() {
        assertEquals("평원", LocationNames.biome("minecraft:plains"))
        assertEquals("cherry valley", LocationNames.biome("terralith:cherry_valley"))
        assertEquals("마을", LocationNames.structure("minecraft:village_snowy"))
        assertEquals("폐허가 된 차원문", LocationNames.structure("minecraft:ruined_portal_mountain"))
        assertNull(LocationNames.structure("minecraft:buried_treasure"))
    }
}

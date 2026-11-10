package kr.toxicity.hud.bootstrap.bukkit.module.kokonut

/**
 * 바이옴·구조물 키(`minecraft:plains`)를 위치 배너에 표시할 한국어 이름으로 바꾼다.
 * HUD 글꼴은 번역 키를 그릴 수 없으므로 클라이언트 ko_kr 대신 여기서 직접 이름을 정한다.
 * 목록에 없는 키(데이터팩 등)는 경로의 밑줄을 띄어쓰기로 바꿔 표시한다.
 */
object LocationNames {
    private val biomes = mapOf(
        "badlands" to "악지형",
        "bamboo_jungle" to "대나무 정글",
        "basalt_deltas" to "현무암 삼각주",
        "beach" to "해변",
        "birch_forest" to "자작나무 숲",
        "cherry_grove" to "벚나무 숲",
        "cold_ocean" to "차가운 바다",
        "crimson_forest" to "진홍빛 숲",
        "dark_forest" to "어두운 숲",
        "deep_cold_ocean" to "깊고 차가운 바다",
        "deep_dark" to "깊은 어둠",
        "deep_frozen_ocean" to "깊고 얼어붙은 바다",
        "deep_lukewarm_ocean" to "깊고 미지근한 바다",
        "deep_ocean" to "깊은 바다",
        "desert" to "사막",
        "dripstone_caves" to "점적석 동굴",
        "end_barrens" to "엔드 불모지",
        "end_highlands" to "엔드 고지",
        "end_midlands" to "엔드 중지",
        "eroded_badlands" to "침식된 악지형",
        "flower_forest" to "꽃 숲",
        "forest" to "숲",
        "frozen_ocean" to "얼어붙은 바다",
        "frozen_peaks" to "얼어붙은 봉우리",
        "frozen_river" to "얼어붙은 강",
        "grove" to "산림",
        "ice_spikes" to "역고드름",
        "jagged_peaks" to "뾰족한 봉우리",
        "jungle" to "정글",
        "lukewarm_ocean" to "미지근한 바다",
        "lush_caves" to "무성한 동굴",
        "mangrove_swamp" to "맹그로브 늪",
        "meadow" to "목초지",
        "mushroom_fields" to "버섯 들판",
        "nether_wastes" to "네더 황무지",
        "ocean" to "바다",
        "old_growth_birch_forest" to "자작나무 원시림",
        "old_growth_pine_taiga" to "소나무 원시 타이가",
        "old_growth_spruce_taiga" to "가문비나무 원시 타이가",
        "pale_garden" to "창백한 정원",
        "plains" to "평원",
        "river" to "강",
        "savanna" to "사바나",
        "savanna_plateau" to "사바나 고원",
        "small_end_islands" to "작은 엔드 섬",
        "snowy_beach" to "눈 덮인 해변",
        "snowy_plains" to "눈 덮인 평원",
        "snowy_slopes" to "눈 덮인 비탈",
        "snowy_taiga" to "눈 덮인 타이가",
        "soul_sand_valley" to "영혼 모래 골짜기",
        "sparse_jungle" to "드문드문한 정글",
        "stony_peaks" to "돌 봉우리",
        "stony_shore" to "돌 해안",
        "sunflower_plains" to "해바라기 평원",
        "swamp" to "늪",
        "taiga" to "타이가",
        "the_end" to "디 엔드",
        "the_void" to "공허",
        "warm_ocean" to "따뜻한 바다",
        "warped_forest" to "뒤틀린 숲",
        "windswept_forest" to "바람이 세찬 숲",
        "windswept_gravelly_hills" to "바람이 세찬 자갈 언덕",
        "windswept_hills" to "바람이 세찬 언덕",
        "windswept_savanna" to "바람이 세찬 사바나",
        "wooded_badlands" to "나무가 우거진 악지형",
    )

    private val structures = mapOf(
        "ancient_city" to "고대 도시",
        "bastion_remnant" to "보루 잔해",
        "desert_pyramid" to "사막 피라미드",
        "end_city" to "엔드 시티",
        "fortress" to "네더 요새",
        "igloo" to "이글루",
        "jungle_pyramid" to "정글 사원",
        "mansion" to "삼림 대저택",
        "mineshaft" to "폐광",
        "mineshaft_mesa" to "폐광",
        "monument" to "바다 유적",
        "ocean_ruin_cold" to "바다 폐허",
        "ocean_ruin_warm" to "바다 폐허",
        "pillager_outpost" to "약탈자 전초기지",
        "shipwreck" to "난파선",
        "shipwreck_beached" to "난파선",
        "stronghold" to "요새",
        "swamp_hut" to "마녀의 오두막",
        "trail_ruins" to "오솔길 폐허",
        "trial_chambers" to "시련의 회당",
    )

    /** 너무 작거나 땅속에 묻혀 있어 배너로 알릴 가치가 없는 구조물. */
    private val hiddenStructures = setOf("buried_treasure", "nether_fossil")

    fun biome(key: String): String = biomes[path(key)] ?: fallback(key)

    /** 표시하지 않는 구조물이면 null. */
    fun structure(key: String): String? {
        val path = path(key)
        if (path in hiddenStructures) return null
        structures[path]?.let { return it }
        return when {
            path.startsWith("village_") -> "마을"
            path.startsWith("ruined_portal") -> "폐허가 된 차원문"
            else -> fallback(key)
        }
    }

    private fun path(key: String) = key.substringAfter(':')
    private fun fallback(key: String) = path(key).substringAfterLast('/').replace('_', ' ')
}

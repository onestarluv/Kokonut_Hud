package kr.toxicity.hud.bootstrap.bukkit.module.kokonut

/** 위치 배너에 표시할 장소. [region]이면 영역 이름(한 줄), 아니면 바이옴 + 선택적 구조물(두 줄). */
data class Place(val title: String, val subtitle: String? = null, val region: Boolean = false) {
    val lines: Int get() = if (subtitle == null) 1 else 2
}

/**
 * 한 플레이어의 위치 배너 상태. 메인 스레드에서 [sample]로 갱신하고, HUD 렌더링 스레드는 [visible]만 읽는다.
 *
 * - 영역은 들어서는 즉시, 야생 바이옴·구조물은 [settleMillis] 동안 그대로일 때 알린다(강을 건널 때마다 깜빡이지 않도록).
 * - 알린 장소는 [repeatMillis] 안에 다시 들어와도 다시 알리지 않는다(경계를 오갈 때 반복 방지).
 * - 배너는 [showMillis] 동안 보인다.
 */
class LocationBanner(
    private val showMillis: Long = 4_000,
    private val settleMillis: Long = 1_000,
    private val repeatMillis: Long = 30_000,
) {
    private class Display(val place: Place, val until: Long)

    private var sampled = false
    private var candidate: Place? = null
    private var candidateSince = 0L
    private var current: Place? = null
    private val announced = LinkedHashMap<Place, Long>()

    @Volatile
    private var display: Display? = null

    /** @return 이번 호출로 새로 알린 장소(배너가 뜨는 순간), 없으면 null. */
    fun sample(place: Place?, now: Long): Place? {
        if (!sampled || place != candidate) {
            candidate = place
            candidateSince = now
        }
        val immediate = !sampled || place == null || place.region
        sampled = true
        if (place == current || (!immediate && now - candidateSince < settleMillis)) return null
        current = place
        if (place == null) return null
        val last = announced[place]
        if (last != null && now - last < repeatMillis) return null
        announced.remove(place)
        announced[place] = now
        if (announced.size > 16) announced.entries.removeIf { now - it.value >= repeatMillis }
        display = Display(place, now + showMillis)
        return place
    }

    fun visible(now: Long): Place? = display?.takeIf { now < it.until }?.place
}

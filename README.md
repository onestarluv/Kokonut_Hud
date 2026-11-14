# Kokonut_HUD

에고버스 전용 HUD (Paper 26.3 / Java 25). Kokonut-Core가 필요합니다.
원본: [BetterHud](https://github.com/toxicity188/BetterHud), MIT 라이선스(LICENSE 참고).

## 표시 내용

기본 HUD `currency_hud`가 화면 왼쪽 위에 **실버·골드·코코넛** 칩을 한 줄로 표시합니다. 칩은 CoreUi 메뉴(도감·메인 메뉴·우편함)와 같은 가죽 테두리·양피지 바탕이며 원본 크기(scale 1.0)로 그려 픽셀이 고르게 보입니다. 칩은 9px 미니 아이콘(`art/icons/*_mini.png`)에 맞춰 높이 13px이며, 폭은 실버 77×13(999,999.99까지), 골드·코코넛 51×13(9,999까지)입니다. 칩 사이 여백은 4px이며, 숫자는 아이콘 칸 뒤 x+15부터 갈무리11에서 만든 7px 숫자로 `#3B2A1A` 색상으로 그립니다. 실버는 소수점 최대 두 자리 표시를 유지합니다.

- 칩 아이콘 원본: `art/icons/{silver,gold,kokonut}_mini.png`(9×9, `D:\kokonut\resource`의 미니 아이콘). 16×16 원본 `art/icons/{silver,gold,kokonut}.png`는 Nexo 아이템 `icon_*`와 같은 그림
- 칩 생성: `java tools/HudChips.java art/icons common-resources/default/assets/currency common-resources/default/fonts/galmuri11.ttf` → `chip_*.png`, `balance.png`

- 설정: `common-resources/config.yml`, HUD·레이아웃·이미지: `common-resources/default`
- 잔액 글씨는 `outline: false`로 그림자를 끕니다.
- 내장 리소스만 읽습니다. 바꾸려면 소스를 수정하고 다시 빌드합니다.
- 플레이스홀더: `[currency_silver]`, `[currency_gold]`, `[currency_kokonut]`
  - 렌더링은 캐시만 읽고, 2초마다 별도 스레드에서 Core `CurrencyUtil`로 다시 조회합니다.
  - 잔액 변경은 최대 약 2초 뒤 반영되며, 첫 조회 전에는 0으로 표시합니다.

### 위치 배너 (`location_hud`)

화면 위쪽 가운데(gui x 50, y 12)에 리본 모양 배너를 4초 동안 표시합니다. 재화 칩과 같은 가죽 테두리·양피지 바탕에 금색 장식을 붙였습니다.

- **영역**: Kokonut_Guard의 모든 영역은 들어설 때 이름을 한 줄 배너로 표시합니다(`__global__` 제외, 겹치면 우선순위가 가장 높은 영역). 어느 월드든 동작하며 야생 표시보다 우선합니다.
  이름은 영역의 `kokonut-hud` 플래그 값이며, 플래그가 없으면 영역 ID를 그대로 씁니다. 값이 `none` 또는 `숨김`이면 그 영역은 표시하지 않습니다.
  예: `/영역 플래그 market kokonut-hud 코코넛 항구 시장` (GUI 이름: `HUD 지역 이름`)
- **야생**: 이름이 `wild`로 시작하는 월드에서는 바이옴을 제목으로, 플레이어가 들어가 있는(경계 상자 안) 구조물이 있으면 점선 아래 부제로 표시합니다.
  바이옴·구조물이 1초 이상 유지될 때만 알리고(강을 건널 때 깜빡임 방지), 30초 안에 같은 장소로 돌아오면 다시 알리지 않습니다.
- 소리: 배너가 뜰 때 본인에게만 `item.book.page_turn`(볼륨 0.6), 야생 구조물(두 줄 배너)은 `ui.cartography_table.take_result`를 재생합니다.
- 글꼴: CoreUi 메뉴와 같은 갈무리11(`default/fonts/galmuri11.ttf`, `location_font`). 10px 격자 그대로(글꼴 1픽셀 = GUI 1픽셀) 그립니다.
- 한국어 이름: `LocationNames.kt`. 목록에 없는 데이터팩 키는 경로를 띄어쓰기로 바꿔 표시합니다. 묻힌 보물·네더 화석은 표시하지 않습니다.
- 플레이스홀더: `[location_title]`, `[location_subtitle]`, `[location_lines]`(0 숨김 · 1 한 줄 · 2 두 줄)
- 배너 생성: `java tools/HudLocationBanner.java common-resources/default/assets/location` → `banner.png`(176×20), `banner_tall.png`(176×34)

## 명령어 (OP)

| 명령어 | 설명 |
| --- | --- |
| `/hud reload` | 설정과 리소스팩을 다시 불러옵니다. |
| `/hud hud add <플레이어\|all> <HUD>` | HUD를 추가합니다. |
| `/hud hud remove <플레이어\|all> <HUD>` | HUD를 제거합니다. |
| `/hud turn <on\|off> [플레이어]` | HUD를 켜거나 끕니다. |
| `/hud parse <텍스트>` | 플레이스홀더 출력을 확인합니다. |

## 빌드·배포

```powershell
.\gradlew.bat pluginJar test
```

- 산출물: `build/libs/Kokonut_HUD-bukkit-cbt-1.0.jar` (`core`의 `buildPlugins`로도 함께 빌드됩니다.)
- `..`(상위 `core`)를 composite build로 포함해 Core API를 컴파일합니다.
- 리소스팩은 `plugins/Nexo/pack/external_packs/Kokonut_HUD.zip`에 생성되고, Nexo가 합쳐서 배포합니다.
  HUD는 서버 로드 시 팩을 동기로 만들고 Nexo는 그 뒤 비동기로 생성하므로 로드 순서 지정은 필요 없습니다(Core가 Nexo를 softdepend하므로 지정하면 순환).
  `/hud reload` 후에는 Nexo 팩도 다시 생성해야 반영됩니다.

### 예전 HUD 이미지·좌표가 남는 경우

현재 HUD 생성 팩은 `plugins/Nexo/pack/external_packs/Kokonut_HUD.zip`으로 병합합니다. 예전에 직접 복사한 `plugins/Nexo/pack/assets/betterhud`가 함께 있으면 Nexo가 구버전 글꼴 provider와 텍스처를 먼저 병합해 최신 JAR를 설치해도 옛 이미지·좌표가 표시될 수 있습니다. 직접 복사본은 팩 입력 밖에 백업하고 `/nexo reload all`로 최종 팩을 다시 생성합니다. 새 팩 다운로드를 받은 뒤 확인하세요.

2026-11-08 확인: 최종 `pack.zip`에 같은 재화 글자 코드의 9px 구 provider와 20px 신 provider가 중복되어 있었고 구 텍스처가 남아 있었습니다. 기존 `betterhud` 직접 복사본 538개와 겹치는 bossbar 이미지 3개를 `home1/plugin-backups/Kokonut_HUD/nexo-legacy-assets-20261108-160520-948`로 옮겨 보존했습니다. `common-resources`와 최신 외부 ZIP은 수정하지 않았습니다.

## 참고

- 제거한 기능: 체력·허기 HUD, 엔티티 팝업, 나침반 기본 리소스와 명령어, Fabric·Velocity·Folia, 구버전 NMS, 외부 확장, 통계, 업데이트 조회.
- 내부 클래스·권한 이름의 `betterhud` 식별자는 엔진 호환성을 위해 유지합니다.
- DB가 필요한 기능은 Core의 `CoreDatabase`를 사용합니다. 별도 DB 풀은 두지 않습니다.

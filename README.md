# Kokonut_HUD

에고버스 전용 HUD (Paper 26.3 / Java 25). Kokonut-Core가 필요합니다.
원본: [BetterHud](https://github.com/toxicity188/BetterHud), MIT 라이선스(LICENSE 참고).

## 표시 내용

기본 HUD `currency_hud`가 화면 왼쪽 위에 **실버·골드·코코넛** 잔액을 한 줄로 표시합니다. 현재 122×20 이미지에 맞춰 항목 간격은 130px(이미지 사이 8px), 숫자는 아이콘 오른쪽 24px부터 12px 크기의 어두운 글자로 표시합니다. 실버는 소수점 최대 두 자리 표시를 유지합니다.

- 설정: `common-resources/config.yml`, HUD·레이아웃·이미지: `common-resources/default`
- 잔액 글씨에는 `outline: "80000000"`으로 반투명 검정 그림자를 적용합니다. 이 엔진의 `outline`은 글자 그림자 색상(ARGB) 설정입니다.
- 내장 리소스만 읽습니다. 바꾸려면 소스를 수정하고 다시 빌드합니다.
- 플레이스홀더: `[currency_silver]`, `[currency_gold]`, `[currency_kokonut]`
  - 렌더링은 캐시만 읽고, 2초마다 별도 스레드에서 Core `CurrencyUtil`로 다시 조회합니다.
  - 잔액 변경은 최대 약 2초 뒤 반영되며, 첫 조회 전에는 0으로 표시합니다.

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

## 참고

- 제거한 기능: 체력·허기 HUD, 엔티티 팝업, 나침반 기본 리소스와 명령어, Fabric·Velocity·Folia, 구버전 NMS, 외부 확장, 통계, 업데이트 조회.
- 내부 클래스·권한 이름의 `betterhud` 식별자는 엔진 호환성을 위해 유지합니다.
- DB가 필요한 기능은 Core의 `CoreDatabase`를 사용합니다. 별도 DB 풀은 두지 않습니다.

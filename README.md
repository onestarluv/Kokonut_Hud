# Kokonut_HUD

에고버스 전용 HUD. Kokonut-Core와 함께 Paper 26.3 / Java 25에서 사용합니다.
Core 메타데이터 기준: `cbt-1.0`, `pingpong`, https://wiki.egoverse.kr, 에고버스 플러그인.

기존 BetterHud의 체력·허기·방어력 이미지, 텍스트, 엔티티 팝업, 애니메이션과 셰이더 렌더링을 유지합니다.
레이아웃과 표시 항목은 `common-resources/default`, 운영 기본값은 `common-resources/config.yml`,
폰트·셰이더 설정은 같은 디렉터리의 내장 리소스에서만 읽습니다. 변경하려면 소스를 수정하고 다시 빌드합니다.
기존 로컬 YAML이나 외부 팩 설정은 사용하지 않습니다. 바이너리 이미지·폰트 캐시와 생성된 리소스팩은 데이터 폴더에 기록합니다.

HUD는 2틱마다 렌더링하며 논리 시간은 서버 틱 단위로 진행합니다. 동일한 표시 결과의 패킷 전송은 기존 비교 로직으로 생략합니다.
로그아웃과 종료 시 작업과 플레이어 캐시를 정리합니다. 표시 상태는 세션에만 유지하며 파일이나 DB에 저장하지 않습니다.
기본 HUD는 Bukkit의 체력·허기·장비 데이터를 사용하므로 Core DB 조회가 필요하지 않습니다.
향후 DB 기능은 Core의 `CoreDatabase` 서비스로 구현해야 합니다. 독립 풀이나 DB 접속 설정은 포함하지 않습니다.
서버 식별이 필요한 기능을 추가할 경우 플레이어의 현재 월드가 아닌 서버 메인 월드 이름을 사용합니다.

Fabric, Velocity, Folia, 구버전 NMS 및 셰이더, 외부 확장, 사용 통계, 업데이트 조회, 런타임 라이브러리 다운로드를 제거했습니다.
명령어/API의 기존 BetterHud 식별자는 렌더링 엔진 내부 호환성을 위해 유지합니다.

```powershell
.\gradlew.bat pluginJar test
```

산출물: `build/libs/Kokonut_HUD-bukkit-cbt-1.0.jar`.
Kokonut-Core와 함께 설치하고 생성된 `plugins/Kokonut_HUD/build` 리소스팩을 서버의 기존 리소스팩 배포 경로에 반영합니다.
내장 HTTP 서버는 기본적으로 사용하지 않습니다. 클라이언트에는 생성한 리소스팩 적용이 필요합니다.

검증: Paper 26.3 NMS/API 컴파일, 내장 기본 HUD·레이아웃·이미지 참조 테스트 및 배포 JAR 구성을 확인합니다.
실제 Core 운영 서버 접속과 클라이언트 화면 검수는 별도로 필요합니다.

원본: [BetterHud](https://github.com/toxicity188/BetterHud), MIT 라이선스. 원 저작권 고지는 LICENSE에 보존합니다.

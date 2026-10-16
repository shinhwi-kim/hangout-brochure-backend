# 서비스 아키텍처

## 목표

Hangout Brochure는 한국에서 열리는 행사 정보를 여러 출처에서 모아 정리하고, 웹 클라이언트가 행아웃과 카테고리를 조회할 수 있게 제공한다.

핵심 흐름은 두 가지다.

1. **조회 흐름**: 웹 클라이언트가 API를 통해 검증·게시된 행아웃을 조회한다.
2. **수집 흐름**: 주기적으로 외부 출처를 확인하고, 행사 정보를 후보 데이터로 추출·정규화한 뒤 DB에 반영한다.

조회 API는 안정적인 서비스 경로로 유지하고, 크롤링과 AI 처리 작업은 별도의 수집 흐름으로 둔다. 수집 실패나 외부 사이트 지연이 API 응답을 막지 않도록 분리한다.

## 전체 흐름

```mermaid
flowchart LR
    Web[웹 지도 / 웹 클라이언트]
    API[Spring Boot API<br/>Controller → Service]
    Repo[HangoutRepository<br/>Repository Adapter]
    JPA[Spring Data JPA]
    Hibernate[Hibernate]
    DB[(PostgreSQL<br/>게시된 행아웃 / 카테고리)]

    Cron[주기 실행기 / Cron]
    Worker[행사 수집 Worker]
    Registry[출처 설정<br/>공식 API / RSS / 웹페이지]
    Adapters[출처별 수집기<br/>API · Feed · Crawler]
    Raw[(원문과 수집 기록)]
    AI[AI 정보 추출<br/>구조화된 후보 생성]
    Normalize[정규화 · 검증 · 중복 판별]
    Review{검토 필요?}
    Candidates[(행사 후보 / 검토 상태)]

    Web --> API
    API --> Repo --> JPA --> Hibernate --> DB

    Cron --> Worker
    Registry --> Adapters
    Worker --> Adapters
    Adapters --> Raw --> AI --> Normalize --> Review
    Review -->|자동 게시 가능| DB
    Review -->|정보 불확실 / 충돌| Candidates
    Candidates -->|검토 후 승인| DB
```

이 그림에서 **API와 DB의 행아웃·카테고리 조회 경로는 현재 구현되어 있다.** 수집 Worker부터 후보 검토와 게시까지는 목표 구조이며, 아직 구현되지 않았다.

## 현재 구현 구조

```text
controller
├── HangoutBrochureController
├── advice
└── dto
    ├── request
    └── response
        └── error

service
├── HangoutService
└── HangoutCategoryService

domain
├── Hangout / HangoutCategory
└── 조회 Criteria 및 결과

repository
├── HangoutRepository                 # 서비스가 의존하는 조회 인터페이스
├── HangoutRepositoryAdapter          # JPA Entity를 domain 모델로 변환
└── jpa
    ├── HangoutJpaRepository
    ├── HangoutCategoryJpaRepository
    └── entity
        ├── HangoutEntity
        └── HangoutCategoryEntity
```

ㅋ## 수집기 코드 배치와 실행 방식

수집 기능은 공개 조회용 `controller / service / repository`와 나란히 `command`와 `ingestion`으로 둔다. `command`는 실행 진입점과 옵션 선택을, `ingestion`은 출처별 수집 동작을 담당한다.

```text
command
└── ingestion
    ├── IngestCommand       # --app.command=ingest 진입점
    ├── persistence
    │   └── IngestionPersistence # 실행 이력과 원문 upsert
    └── source
        ├── IngestionSource # 출처 수집기의 공통 계약
        └── seoulCulturalEvent
            ├── SeoulCulturalEventClient   # 서울 문화행사 API 호출·페이지 처리
            ├── SeoulCulturalEventRecord   # 출처 응답을 정규화한 레코드
            └── SeoulCulturalEventIngestionSource # 수집 실행 구현

ingestion.application      # 수집 조정, 중복 판별, 저장 (추가 시)
ingestion.persistence      # 원문/후보/체크포인트 저장 (추가 시)
```

cron이 Spring 애플리케이션 안에서 계속 돌도록 하기보다, 초기에 **한 번 실행 후 종료하는 명령**을 만들고 OS cron이나 컨테이너 스케줄러가 정해진 주기에 호출하는 편이 운영과 재시도에 편하다. API 서버와 수집 작업도 각각 따로 실행할 수 있다. 출처 어댑터는 수집 로직만 맡고, command는 설정을 읽어 수집 흐름을 시작하는 역할을 한다.

서울시 문화행사 데이터는 [서울시 문화행사 정보](https://data.seoul.go.kr/dataList/OA-23041/S/1/datasetView.do)의 항목을 `ingestion_seoul_cultural_hangouts_raw`에 저장한다. API 응답 필드와 원본 JSON을 함께 보존하며, `ingestion_sources`에는 `seoul-cultural-events` 출처를 등록한다. `command.ingestion.IngestCommand`가 `--source` 이름에 맞는 `IngestionSource`를 선택하고, 서울 문화행사 수집 구현은 `command.ingestion.source.seoulCulturalEvent` 패키지에 둔다. `(source_name, external_id)` 고유 키로 재수집을 멱등하게 처리하며, 실행 결과는 `ingestion_runs`에 기록한다. 수집 원문은 아직 후보 검증 후 공개 행아웃으로 승격하지 않는다.

현재 명령은 `SEOUL_OPEN_API_KEY` 환경 변수와 함께 다음처럼 실행한다.

```bash
export SEOUL_OPEN_API_KEY=발급받은_키
./gradlew bootRun --args='--spring.main.web-application-type=none --app.command=ingest --source=seoul-cultural-events'
```

여러 출처를 한 번에 실행하려면 `--source=...` 옵션을 반복한다. 예를 들어 `--source=seoul-cultural-events --source=another-source`처럼 지정할 수 있다.

개발 단계에서는 이 명령을 직접 실행해 결과를 확인하고, 운영에서는 동일한 명령을 컨테이너 작업으로 패키징해 cron이 호출하게 한다. 실행 주기는 열린데이터광장에 명시된 일 1회 갱신에 맞춰 하루 한 번부터 시작할 수 있다.

조회 요청은 `Controller → Service → HangoutRepository → JPA Repository → Hibernate → PostgreSQL` 순서로 처리한다. Controller는 HTTP DTO를 받고, Service는 조회 조건과 페이지 결과를 다루며, Adapter는 JPA Entity를 API에 독립적인 domain 모델로 변환한다.

현재 API:

- `GET /api/hangouts`: 카테고리와 겹치는 일정 범위로 행아웃을 필터링하고 UUID 커서로 조회한다.
- `GET /api/hangout-categories`: 카테고리 코드나 이름의 부분 문자열을 검색하고 정수 ID 커서로 조회한다.

## 목표 수집 흐름

### 1. 출처별 수집기와 증분 처리

출처에 따라 전용 Adapter를 둔다.

- **공식 API**: 공공기관·행사 플랫폼이 API를 제공하면 우선 사용한다. 인증 키는 코드나 DB에 저장하지 않고 환경 변수 또는 Secret으로 관리한다.
- **RSS / 뉴스 피드**: 새 기사와 행사 공지를 찾는 용도로 사용하고, 원문 URL과 발행 정보를 보존한다.
- **웹페이지 크롤러**: 공식 행사 페이지 등 출처별 페이지 구조를 파싱한다. 출처의 이용 조건과 요청 제한을 지키고, 페이지 구조 변경이나 실패를 출처별로 격리한다.

출처별 수집기는 원문을 `Hangout`으로 바로 만들지 않는다. 원문 위치, 출처 식별자, 수집 시각, 외부 ID와 함께 보관하거나 재처리할 수 있게 남긴다.

수집기와 AI 추출기는 **책임을 나눈 논리 단계**이지, 처음부터 별도 프로세스나 서비스로 배포해야 한다는 뜻은 아니다. 초기에는 RSS 수집기가 원문을 가져온 뒤 공통 추출기를 호출해 한 Worker 작업 안에서 처리해도 된다. 공식 API처럼 이미 구조화된 데이터는 AI 추출을 건너뛰고 필드 변환을 할 수 있다.

각 API, RSS 피드, 크롤링 대상은 **별도의 수집 체크포인트**를 가진다. 한 출처의 진행 상태를 다른 출처와 공유하지 않는다.

- **공식 API**: `updated_at` 같은 변경 시각이나 페이지 토큰을 우선 사용한다. 처음에는 전체/기간 조회를 하고, 이후 성공한 체크포인트부터 이어 읽는다.
- **RSS**: 항목의 `guid`를 우선 식별자로 사용하고 없으면 정규화한 링크를 사용한다. `ETag` / `Last-Modified`가 지원되면 다음 요청에 활용한다. 발행 시각은 늦게 추가되거나 수정될 수 있으므로 그것만 믿지 않고, 매 실행마다 최근 구간을 겹쳐 읽은 뒤 중복 제거한다.
- **웹페이지 크롤링**: sitemap이나 목록의 갱신 시각이 있으면 활용한다. 없으면 정해진 범위를 주기적으로 다시 확인하고, URL과 내용 해시로 새 글·수정 글을 판별한다.

수집과 AI 처리는 진행 상태를 분리한다.

1. 출처에서 발견한 원문을 `ingestion_seoul_cultural_hangouts_raw` 등 출처별 원문 테이블에 먼저 저장한다. `(source_name, external_id)`에 고유 제약을 두어 같은 항목이 반복 발견돼도 중복 저장되지 않게 한다.
2. 원문 저장이 성공한 뒤 출처 체크포인트를 갱신한다. 이렇게 하면 AI 처리에 실패해도 다음 실행에서 원문을 잃지 않는다.
3. Worker는 아직 처리되지 않았거나 원문 해시가 바뀐 문서를 AI 추출 대상으로 가져온다. 실패한 문서는 재시도하고 성공·검토 대기·실패 상태를 기록한다.
4. 같은 원문과 같은 추출기 버전으로 이미 처리한 문서는 건너뛴다. 프롬프트나 추출 스키마를 바꾸어 재추출할 때는 추출기 버전을 바꾸어 대상 문서를 다시 처리할 수 있게 한다.

수집 체크포인트는 “어디까지 원문을 저장했는가”를 나타내고, 문서 처리 상태는 “AI 추출과 검증이 어디까지 됐는가”를 나타낸다. 행사의 `starts_at`은 출처 처리 진행 위치가 아니므로 증분 수집 기준으로 사용하지 않는다.

### 2. AI 정보 추출

AI는 원문에서 행사 후보 필드를 추출하는 보조 도구로 사용한다. 예를 들면 다음과 같다.

- 행사명과 설명
- 시작·종료 일시
- 장소명, 주소, 좌표 후보
- 카테고리 후보
- 공식 행사 URL과 근거 문장
- 필드별 신뢰도 또는 검토 사유

AI 결과는 구조화된 후보로 저장하고 곧바로 공개 API에 노출하지 않는다. 필수 정보 누락, 날짜 모순, 낮은 신뢰도, 서로 다른 출처의 충돌이 있으면 검토 대상으로 둔다. 충분히 검증된 항목만 게시된 행아웃으로 반영한다.

### 3. 정규화와 중복 제거

- 내부 카테고리 코드(`music`, `sport` 등)로 매핑하고, 출처의 분류명은 그대로 내부 카테고리로 사용하지 않는다.
- 한국 출처에서 시간대가 생략된 일시는 `Asia/Seoul` 기준으로 해석한다. 시간대가 명시된 값은 해당 offset을 존중하고, DB에는 시점을 보존한다.
- 출처의 외부 ID, 정규화한 URL, 행사명·일시·장소 조합을 이용해 중복 후보를 찾는다.
- 같은 행사가 여러 출처에서 발견되면 출처 링크를 모두 보존하고, 최신 정보와 신뢰도에 따라 정본을 갱신한다.
- 수집 재시도에도 중복 행이 만들어지지 않도록 수집 반영은 멱등하게 설계한다.

## 목표 데이터 책임

현재 PostgreSQL에는 공개 조회에 사용하는 `hangouts`, `hangout_categories`, `hangout_category_assignments`가 있다. 수집 기능을 추가할 때는 공개용 데이터와 수집 중간 데이터를 구분한다.

현재 추가된 수집 테이블:

| 테이블 | 책임 |
| --- | --- |
| `ingestion_sources` | 수집 출처 이름, 종류, 표시명, 기본 URL, 활성화 여부와 마지막 성공 시각 관리 |
| `ingestion_runs` | 실행 시작·종료, 성공/실패, 조회·저장 건수와 오류 기록 |
| `ingestion_seoul_cultural_hangouts_raw` | 서울 문화행사 데이터셋의 모든 행사 필드와 원본 JSON, 출처 URL·외부 ID, 내용 해시, 확인 시각 및 처리 상태 보관 |

`ingestion_seoul_cultural_hangouts_raw`는 데이터셋의 행사 필드를 다음처럼 저장한다. `raw_payload`에도 API의 원본 행 전체를 보존한다.

| 서울 API 필드 | 저장 컬럼 | 서울 API 필드 | 저장 컬럼 |
| --- | --- | --- | --- |
| `CODENAME` | `category` | `GUNAME` | `district` |
| `TITLE` | `title` | `DATE` | `event_date` |
| `PLACE` | `place` | `ORG_NAME` | `organization` |
| `USE_TRGT` | `target_audience` | `USE_FEE` | `fee` |
| `INQUIRY` | `inquiry` | `PLAYER` | `performers` |
| `PROGRAM` | `program` | `ETC_DESC` | `additional_info` |
| `ORG_LINK` | `homepage_url` | `MAIN_IMG` | `image_url` |
| `RGSTDATE` | `application_date` | `TICKET` | `ticket_type` |
| `STRTDATE` | `starts_on` | `END_DATE` | `ends_on` |
| `THEMECODE` | `theme` | `LOT` | `longitude` |
| `LAT` | `latitude` | `IS_FREE` | `free_or_paid` |
| `HMPG_ADDR` | `detail_url` | `PRO_TIME` | `event_time` |

다음 단계에서는 `hangout_candidates`로 추출·정규화한 후보와 검토 상태를 관리하고, `hangout_source_references`로 게시된 행아웃의 출처를 연결할 수 있다. 현재 API는 수집 원문이나 후보를 공개하지 않는다.

## Worker 배치와 실행

초기에는 기존 Spring Boot 코드베이스 안에 수집 모듈을 둘 수 있지만, 주기 작업은 API 요청 처리와 분리해 실행한다. 배포 시에는 같은 코드와 DB를 사용하는 별도 Worker 프로세스로 분리하는 구성을 목표로 한다.

```text
hangout-api       → 조회 API 제공
hangout-worker    → Cron 실행, 외부 수집, AI 추출, 정규화·저장
postgres          → 공개 데이터와 수집 상태 저장
```

이렇게 하면 Worker의 장시간 작업, 재시도, 외부 API 제한이 웹 API 지연으로 이어지는 것을 줄일 수 있다. 여러 Worker를 운영할 때는 동시에 같은 작업을 중복 실행하지 않도록 작업 임대(lease)나 DB 기반 실행 잠금, 출처별 요청 제한을 추가한다.

## 설계 원칙

1. **공식 API와 원 출처를 우선한다.** 뉴스는 행사 발견과 정보 보강에 활용하되, 가능한 한 공식 행사 페이지를 근거 출처로 연결한다.
2. **출처와 근거를 보존한다.** 공개된 일정의 출처 URL과 마지막 확인 시각을 추적할 수 있어야 한다.
3. **AI는 후보를 만들고 검증 규칙이 게시를 결정한다.** AI 응답만으로 공개 데이터를 확정하지 않는다.
4. **반복 실행에 안전해야 한다.** 같은 기간을 여러 번 수집해도 동일 행사가 중복 생성되지 않도록 한다.
5. **API 모델과 저장 모델을 분리한다.** 외부 DTO, domain 모델, JPA Entity는 서로 다른 변경 이유를 가진다.
6. **수집 실패를 격리하고 관측 가능하게 한다.** 출처별 오류·재시도·실행 결과를 기록해 한 출처의 장애가 전체 수집을 막지 않도록 한다.

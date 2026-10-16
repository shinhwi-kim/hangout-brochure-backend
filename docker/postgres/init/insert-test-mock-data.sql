-- Local development sample hangouts.
-- Generate a fresh mix of past, ongoing, soon-to-start, and future hangouts
-- whenever a new local database volume is initialized.
CREATE TEMP TABLE _seed_hangout_ids (
    sample_number INT PRIMARY KEY,
    hangout_id UUID NOT NULL
);

INSERT INTO _seed_hangout_ids (sample_number, hangout_id)
SELECT series.i, uuidv7()
FROM generate_series(1, 100) AS series(i);

WITH sample_locations(location_number, location_name, address, latitude, longitude) AS (
    VALUES
        (1, '여의도 한강공원', '서울특별시 영등포구 여의동로 330', 37.5284, 126.9326),
        (2, '서울숲', '서울특별시 성동구 뚝섬로 273', 37.5445, 127.0374),
        (3, '성수 연무장길', '서울특별시 성동구 연무장길 일대', 37.5446, 127.0561),
        (4, '문화비축기지', '서울특별시 마포구 증산로 87', 37.5717, 126.8933),
        (5, '노들섬', '서울특별시 용산구 양녕로 445', 37.5170, 126.9599),
        (6, '망원시장', '서울특별시 마포구 포은로 6길 27', 37.5562, 126.9056),
        (7, '올림픽공원', '서울특별시 송파구 올림픽로 424', 37.5208, 127.1215),
        (8, '북서울꿈의숲', '서울특별시 강북구 월계로 173', 37.6214, 127.0414),
        (9, '남산공원', '서울특별시 중구 삼일대로 231', 37.5509, 126.9909),
        (10, '세종문화회관', '서울특별시 종로구 세종대로 175', 37.5729, 126.9769),
        (11, '잠실종합운동장', '서울특별시 송파구 올림픽로 25', 37.5145, 127.0736),
        (12, '경의선숲길', '서울특별시 마포구 연남동 일대', 37.5637, 126.9234)
),
sample_hangouts AS (
    SELECT
        series.i,
        seed_ids.hangout_id,
        category.name AS category_name,
        location.location_name,
        location.address,
        location.latitude,
        location.longitude
    FROM generate_series(1, 100) AS series(i)
    JOIN _seed_hangout_ids AS seed_ids ON seed_ids.sample_number = series.i
    JOIN hangout_categories AS category ON category.id = ((series.i - 1) % 8) + 1
    JOIN sample_locations AS location ON location.location_number = ((series.i * 7 - 1) % 12) + 1
)
INSERT INTO hangouts (id, title, description, location_name, address, latitude, longitude, starts_at, ends_at)
SELECT
    sample_hangouts.hangout_id,
    CASE series.i
        WHEN 1 THEN '한강 재즈 피크닉'
        WHEN 2 THEN '성수 주말 브런치 모임'
        WHEN 3 THEN '망원 플리마켓'
        WHEN 4 THEN '서울숲 야외 드로잉'
        WHEN 5 THEN '초보 러닝 크루'
        WHEN 6 THEN '가죽 공예 원데이 클래스'
        WHEN 7 THEN '동네 책 나눔 모임'
        WHEN 8 THEN '아이와 함께하는 공원 놀이'
        WHEN 9 THEN '노들섬 버스킹 데이'
        WHEN 10 THEN '시장 먹거리 산책'
        ELSE sample_hangouts.category_name || ' 행아웃 #' || lpad(series.i::text, 3, '0')
    END,
    '로컬 개발 확인을 위한 샘플 행아웃입니다. 일정과 장소는 예시 데이터입니다.',
    sample_hangouts.location_name,
    sample_hangouts.address,
    sample_hangouts.latitude,
    sample_hangouts.longitude,
    schedule.starts_at,
    schedule.ends_at
FROM generate_series(1, 100) AS series(i)
JOIN sample_hangouts ON sample_hangouts.i = series.i
CROSS JOIN LATERAL (
    SELECT
        CASE series.i % 10
            WHEN 0 THEN CURRENT_TIMESTAMP - INTERVAL '5 hours' - (series.i % 3) * INTERVAL '1 hour'
            WHEN 1 THEN CURRENT_TIMESTAMP - INTERVAL '2 hours'
            WHEN 2 THEN CURRENT_TIMESTAMP - INTERVAL '15 minutes'
            WHEN 3 THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes'
            WHEN 4 THEN CURRENT_TIMESTAMP + INTERVAL '2 hours'
            WHEN 5 THEN CURRENT_TIMESTAMP + INTERVAL '1 day' + (series.i % 12) * INTERVAL '2 hours'
            WHEN 6 THEN CURRENT_TIMESTAMP + INTERVAL '3 days' + (series.i % 20) * INTERVAL '1 day'
            WHEN 7 THEN CURRENT_TIMESTAMP + INTERVAL '14 days' + (series.i % 30) * INTERVAL '1 day'
            WHEN 8 THEN CURRENT_TIMESTAMP + INTERVAL '2 months' + (series.i % 4) * INTERVAL '1 month'
            ELSE CURRENT_TIMESTAMP - INTERVAL '30 minutes'
        END + ((series.i * 7 % 19) - 9) * INTERVAL '1 minute' AS starts_at,
        CASE series.i % 10
            WHEN 0 THEN CURRENT_TIMESTAMP - INTERVAL '3 hours' - (series.i % 3) * INTERVAL '1 hour'
            WHEN 1 THEN CURRENT_TIMESTAMP + INTERVAL '2 hours'
            WHEN 2 THEN CURRENT_TIMESTAMP + INTERVAL '105 minutes'
            WHEN 3 THEN CURRENT_TIMESTAMP + INTERVAL '3 hours'
            WHEN 4 THEN CURRENT_TIMESTAMP + INTERVAL '7 hours'
            WHEN 5 THEN CURRENT_TIMESTAMP + INTERVAL '1 day' + (series.i % 12) * INTERVAL '2 hours' + INTERVAL '4 hours'
            WHEN 6 THEN CURRENT_TIMESTAMP + INTERVAL '3 days' + (series.i % 20) * INTERVAL '1 day' + INTERVAL '6 hours'
            WHEN 7 THEN CURRENT_TIMESTAMP + INTERVAL '15 days' + (series.i % 30) * INTERVAL '1 day'
            WHEN 8 THEN CURRENT_TIMESTAMP + INTERVAL '2 months' + (series.i % 4) * INTERVAL '1 month' + INTERVAL '4 hours'
            ELSE CURRENT_TIMESTAMP + INTERVAL '90 minutes'
        END + ((series.i * 7 % 19) - 9) * INTERVAL '1 minute' AS ends_at
) AS schedule;

-- Every sample has one category; every fourth sample gets a second one.
INSERT INTO hangout_category_assignments (hangout_id, category_id)
SELECT seed_ids.hangout_id, ((series.i - 1) % 8) + 1
FROM generate_series(1, 100) AS series(i)
JOIN _seed_hangout_ids AS seed_ids
    ON seed_ids.sample_number = series.i;

INSERT INTO hangout_category_assignments (hangout_id, category_id)
SELECT seed_ids.hangout_id, (series.i % 8) + 1
FROM generate_series(1, 100) AS series(i)
JOIN _seed_hangout_ids AS seed_ids
    ON seed_ids.sample_number = series.i
WHERE series.i % 4 = 0;

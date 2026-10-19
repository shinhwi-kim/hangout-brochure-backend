-- Reference data the application cannot run without: the categories it exposes, the
-- sources it ingests from, and the translation between a source's labels and ours.
-- Sample hangouts are not here; those are opt-in via 03-additional-seed.sh.

INSERT INTO hangout_categories (id, code, name) VALUES
    (1, 'music', '음악'),
    (2, 'food', '음식'),
    (3, 'market', '마켓'),
    (4, 'art', '예술'),
    (5, 'sport', '스포츠'),
    (6, 'workshop', '워크숍'),
    (7, 'community', '커뮤니티'),
    (8, 'family', '가족'),
    (9, 'theater', '연극'),
    (10, 'musical', '뮤지컬·오페라'),
    (11, 'dance', '무용'),
    (12, 'movie', '영화'),
    (13, 'festival', '축제');

SELECT setval(pg_get_serial_sequence('hangout_categories', 'id'), (SELECT MAX(id) FROM hangout_categories));

INSERT INTO ingestion_sources (source_name, source_type, display_name, base_url)
VALUES (
    'seoul-cultural-events',
    'open_api',
    '서울시 문화행사',
    'http://openapi.seoul.go.kr:8088'
);

-- Seoul's 17 labels folded into our categories. '기타' is deliberately absent:
-- it carries no meaning, so those events simply get no category.
INSERT INTO ingestion_category_mappings (source_name, source_category, category_id) VALUES
    ('seoul-cultural-events', '클래식',         1),
    ('seoul-cultural-events', '콘서트',         1),
    ('seoul-cultural-events', '국악',           1),
    ('seoul-cultural-events', '독주/독창회',    1),
    ('seoul-cultural-events', '전시/미술',      4),
    ('seoul-cultural-events', '교육/체험',      6),
    ('seoul-cultural-events', '연극',           9),
    ('seoul-cultural-events', '뮤지컬/오페라',  10),
    ('seoul-cultural-events', '무용',           11),
    ('seoul-cultural-events', '영화',           12),
    ('seoul-cultural-events', '축제-문화/예술', 13),
    ('seoul-cultural-events', '축제-기타',      13),
    ('seoul-cultural-events', '축제-전통/역사', 13),
    ('seoul-cultural-events', '축제-자연/경관', 13),
    ('seoul-cultural-events', '축제-시민화합',  13),
    ('seoul-cultural-events', '축제-관광/체육', 13),
    ('seoul-cultural-events', '축제-관광/체육', 5);

-- 초기 기본 데이터 정의

-- Term 테이블 약관 추가
INSERT IGNORE INTO term(type)
VALUES
    ('SERVICE'),
    ('PRIVACY'),
    ('AGE_14'),
    ('MARKETING');
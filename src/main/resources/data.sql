-- 초기 기본 데이터 정의

-- Term 테이블 약관 추가
INSERT IGNORE INTO term(type)
VALUES
    ('SERVICE'),
    ('PRIVACY'),
    ('AGE_14'),
    ('MARKETING');


INSERT INTO challenge (
    name,
    explain_content,
    caption_img_url,
    participant_count,
    category,
    time_start,
    time_end,
    duration_option_list,
    deposit_option_list,
    success_option_list,
    failure_option_list,
    verification_label_list,
    status
)
SELECT
    '매일 6시 기상',
    '매일 6시에 기상하는 챌린지',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    1443,
    'LIFESTYLE_ROUTINE',
    '05:30:00',
    '06:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["이불 개어진 사진"]',
    '["이불이 보이지 않아요"]',
    '["PILLOW","WAKEUP"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '매일 6시 기상'
);

INSERT INTO challenge (
    name,
    explain_content,
    caption_img_url,
    participant_count,
    category,
    time_start,
    time_end,
    duration_option_list,
    deposit_option_list,
    success_option_list,
    failure_option_list,
    verification_label_list,
    status
)
SELECT
    '아침 운동',
    '매일 아침 운동을 인증하는 챌린지',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    3402,
    'FITNESS',
    '06:30:00',
    '08:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["운동 중인 사진"]',
    '["운동 장면이 보이지 않아요"]',
    '["SPORT","FITNESS"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '아침 운동'
);

INSERT INTO challenge (
    name,
    explain_content,
    caption_img_url,
    participant_count,
    category,
    time_start,
    time_end,
    duration_option_list,
    deposit_option_list,
    success_option_list,
    failure_option_list,
    verification_label_list,
    status
)
SELECT
    '독서 습관',
    '매일 독서하는 습관을 만드는 챌린지',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    63,
    'PERSONAL_DEVELOPMENT',
    '06:30:00',
    '08:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["책 읽는 사진"]',
    '["책이 보이지 않아요"]',
    '["BOOK"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '독서 습관'
);

INSERT INTO challenge (
    name,
    explain_content,
    caption_img_url,
    participant_count,
    category,
    time_start,
    time_end,
    duration_option_list,
    deposit_option_list,
    success_option_list,
    failure_option_list,
    verification_label_list,
    status
)
SELECT
    '건강한 아침 식사',
    '매일 건강한 식단을 인증하는 챌린지',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    5,
    'EATING_HABITS',
    '06:30:00',
    '08:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["아침 식사 사진"]',
    '["음식이 보이지 않아요"]',
    '["FOOD","MEAL"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '건강한 아침 식사'
);

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
    description,
    caption_img_url,
    verify_method_content,
    verification_example_photo_url,
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
    '[{"type":"h2","content":"이 챌린지는?"},{"type":"paragraph","content":"세상보다 먼저 눈뜨는 21일, 나만의 새벽 30분."},{"type":"linebreak","content":""},{"type":"h2","content":"하면 좋은 점"},{"type":"blockquote","content":"아침의 나만의 30분이 생겨요"},{"type":"paragraph","content":"세상이 조용한 시간, 방해 없이 나에게 집중할 수 있어요."},{"type":"linebreak","content":""},{"type":"blockquote","content":"마음의 여유가 생겨요"},{"type":"paragraph","content":"허둥지둥 뛰는 아침 대신, 커피 한 잔의 여유를 챙겨요."},{"type":"linebreak","content":""},{"type":"blockquote","content":"생체 리듬이 정렬돼요"},{"type":"paragraph","content":"일찍 일어나면 밤에 잠도 잘 오는 선순환이 만들어져요."},{"type":"linebreak","content":""},{"type":"blockquote","content":"하루의 주도권을 되찾아요"},{"type":"paragraph","content":"\"시작 당한\" 게 아니라 \"시작한\" 감각으로 살게 돼요."},{"type":"linebreak","content":""},{"type":"h2","content":"이런 분께 추천해요"},{"type":"blockquote","content":"미라클 모닝을 여러 번 시도했지만 매번 3일을 못 넘긴 분"},{"type":"blockquote","content":"출근·등교 전 늘 시간에 쫓기는 게 지친 분"},{"type":"blockquote","content":"아침형 인간이 되고 싶어 자기 계발을 시작하려는 분"},{"type":"blockquote","content":"혼자서는 자꾸 무너져서 함께할 동료가 필요한 분"}]',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    '매일 오전 6시 이전에 기상한 사진을 촬영합니다.',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    1443,
    'LIFESTYLE_ROUTINE',
    '05:30:00',
    '06:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["이불 개어진 사진"]',
    '["이불이 보이지 않아요"]',
    '["PERSON"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '매일 6시 기상'
);

INSERT INTO challenge (
    name,
    explain_content,
    description,
    caption_img_url,
    verify_method_content,
    verification_example_photo_url,
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
    '[{"type":"h2","content":"이 챌린지는?"},{"type":"paragraph","content":"아침 운동으로 하루를 시작하는 습관을 만드는 챌린지입니다."},{"type":"linebreak","content":""},{"type":"h2","content":"하면 좋은 점"},{"type":"blockquote","content":"몸이 먼저 깨어나요"},{"type":"blockquote","content":"오전 집중력이 올라가요"},{"type":"blockquote","content":"꾸준한 루틴을 만들기 쉬워요"}]',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    '운동 중인 모습이 담긴 사진을 업로드합니다.',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    3402,
    'FITNESS',
    '06:30:00',
    '08:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["운동 중인 사진"]',
    '["운동 장면이 보이지 않아요"]',
    '["PERSON"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '아침 운동'
);

INSERT INTO challenge (
    name,
    explain_content,
    description,
    caption_img_url,
    verify_method_content,
    verification_example_photo_url,
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
    '[{"type":"h2","content":"이 챌린지는?"},{"type":"paragraph","content":"매일 책을 읽는 시간을 확보해 독서 습관을 만드는 챌린지입니다."},{"type":"linebreak","content":""},{"type":"h2","content":"하면 좋은 점"},{"type":"blockquote","content":"짧은 시간에도 꾸준히 읽는 습관이 생겨요"},{"type":"blockquote","content":"출퇴근 전후의 자투리 시간을 잘 쓰게 돼요"}]',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    '책 표지 또는 읽고 있는 페이지가 보이도록 촬영합니다.',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    63,
    'PERSONAL_DEVELOPMENT',
    '06:30:00',
    '08:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["책 읽는 사진"]',
    '["책이 보이지 않아요"]',
    '["PERSON"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '독서 습관'
);

INSERT INTO challenge (
    name,
    explain_content,
    description,
    caption_img_url,
    verify_method_content,
    verification_example_photo_url,
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
    '[{"type":"h2","content":"이 챌린지는?"},{"type":"paragraph","content":"매일 건강한 아침 식사를 챙기며 식습관을 바꾸는 챌린지입니다."},{"type":"linebreak","content":""},{"type":"h2","content":"하면 좋은 점"},{"type":"blockquote","content":"하루 시작을 안정적으로 열 수 있어요"},{"type":"blockquote","content":"규칙적인 식사 습관이 생겨요"}]',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    '아침 식사가 보이도록 사진을 촬영합니다.',
    'https://together-mud.kakaocdn.net/dn/ddUiy9/btsdGABygpb/MsARp4M5vZdcumFmyHKoN1/c360.jpg',
    5,
    'EATING_HABITS',
    '06:30:00',
    '08:30:00',
    '[2,4,8,12]',
    '[10000,20000,30000,50000]',
    '["아침 식사 사진"]',
    '["음식이 보이지 않아요"]',
    '["PERSON"]',
    'ACTIVE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM challenge
    WHERE name = '건강한 아침 식사'
);

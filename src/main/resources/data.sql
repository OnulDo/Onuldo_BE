-- 초기 기본 데이터 정의

-- Term 테이블 약관 추가
INSERT INTO term(type, title, effective_date, content)
VALUES
    ('SERVICE', '서비스 이용 약관', '2026-05-30', '[{"type":"h2","content":"제1조 (목적)"},{"type":"paragraph","content":"본 약관은 오늘두(이하 \\\"회사\\\")가 제공하는 습관 형성 챌린지 서비스 이용에 관한 조건 및 절차, 이용자와 회사의 권리·의무 및 책임사항을 규정함을 목적으로 합니다."},{"type":"linebreak","content":""},{"type":"h2","content":"제2조 (용어의 정의)"},{"type":"paragraph","content":"\\\"챌린지\\\"는 회원이 설정한 목표 활동을 인증하는 프로그램을 말하며, \\\"파티\\\"는 3명 이상의 회원이 그룹으로 참여하는 챌린지를 의미합니다."},{"type":"linebreak","content":""},{"type":"h2","content":"제3조 (도전금 및 정산)"},{"type":"paragraph","content":"회원은 챌린지 참여 시 도전금을 예치하며, 챌린지 완료 조건 충족 시 예치금 환급과 성과 보너스가 지급됩니다."},{"type":"linebreak","content":""},{"type":"h2","content":"제4조 (인증 및 판정)"},{"type":"paragraph","content":"회원의 인증 사진은 AI 기반 이미지 인식 시스템을 통해 챌린지 조건 부합 여부를 검증하며, 판정 결과는 최종적입니다."},{"type":"linebreak","content":""},{"type":"h2","content":"제5조 (계정 관리)"},{"type":"paragraph","content":"회원은 본인의 계정 정보를 안전하게 관리할 책임이 있으며, 도용 등이 의심되는 경우 즉시 회사에 통지해야 합니다."}]'),
    ('PRIVACY', '개인정보 처리방침', '2026-05-30', '[{"type":"h2","content":"1. 수집하는 개인정보 항목"},{"type":"paragraph","content":"필수: 이메일, 닉네임, 프로필 이미지, 챌린지 인증 사진 (자동수집: 서비스 이용기록, 접속 로그, IP 주소, 기기 정보)"},{"type":"linebreak","content":""},{"type":"h2","content":"2. 개인정보 수집 및 이용 목적"},{"type":"paragraph","content":"회원 관리, 서비스 제공, 챌린지 인증 판정, 정산 처리, 고객 문의 대응, 마케팅 및 광고에 활용 (동의 시)"},{"type":"linebreak","content":""},{"type":"h2","content":"3. 개인정보 보유 및 이용기간"},{"type":"paragraph","content":"회원 탈퇴 시까지 보유하며, 관련 법령에 따라 일정 기간 보관이 필요한 정보는 해당 기간까지 별도 저장 후 파기합니다."},{"type":"linebreak","content":""},{"type":"h2","content":"4. 개인정보의 제3자 제공"},{"type":"paragraph","content":"회원의 동의 없이 개인정보를 외부에 제공하지 않으며, 예외의 경우는 관련 법령에 근거한 사항에 한합니다."},{"type":"linebreak","content":""},{"type":"h2","content":"5. 이용자의 권리"},{"type":"paragraph","content":"본인의 개인정보 조회·수정·삭제·처리정지를 언제든지 요청할 수 있으며, 마이페이지 또는 개인정보 담당자를 통해 가능합니다."}]'),
    ('REFUND', '환급 정책', '2026-05-30', '[{"type":"h2","content":"1. 도전금 환급 원칙"},{"type":"paragraph","content":"챌린지 목표 달성 시 예치한 도전금 100%가 환급되며, 목표 미달 시 실패 회차 비율에 따라 일부 몰수됩니다."},{"type":"linebreak","content":""},{"type":"h2","content":"2. 성과 보너스 지급"},{"type":"paragraph","content":"파티 챌린지에서 전원 성공한 경우 실패자 몰수금(POT)이 성공자에게 균등 분배되며 성과 보너스로 지급됩니다."},{"type":"linebreak","content":""},{"type":"h2","content":"3. 환급 금액 계산"},{"type":"paragraph","content":"환급액 = 예치금 × 성공률 (예: 30일 중 27일 성공 시 90% 환급). 계산 결과는 정산 페이지에서 확인할 수 있습니다."},{"type":"linebreak","content":""},{"type":"h2","content":"4. 출금 및 이체"},{"type":"paragraph","content":"환급 완료된 포인트는 등록된 본인 명의 계좌로 이체 신청할 수 있으며, 영업일 기준 1~3일 소요됩니다."},{"type":"linebreak","content":""},{"type":"h2","content":"5. 환급 예외 사항"},{"type":"paragraph","content":"부정 인증, 약관 위반, 챌린지 규칙 위반 시 환급 대상에서 제외될 수 있습니다."}]')
    AS new
ON DUPLICATE KEY UPDATE
                     title = new.title,
                     effective_date = new.effective_date,
                     content = new.content;

INSERT INTO term(type, title, effective_date, content)
VALUES
    ('AGE_14', '만 14세 이상 확인', '2026-07-28', '[]'),
    ('MARKETING', '마케팅 정보 수신 동의', '2026-07-28', '[]')
    AS new
ON DUPLICATE KEY UPDATE
                     title = new.title,
                     effective_date = new.effective_date,
                     content = new.content;


-- 개인 챌린지 몰수금 pot 싱글톤 행 (challenge_pot_id = 1 고정)
-- WHERE NOT EXISTS + INSERT는 원자적이지 않아 여러 인스턴스가 동시에 기동하면 둘 다 "행 없음"으로 판단해
-- 한쪽이 PK 중복 오류로 기동 실패할 수 있다. ON DUPLICATE KEY UPDATE로 원자적 UPSERT 처리(기존 행은 그대로 유지).
INSERT INTO challenge_pot (challenge_pot_id, balance, total_forfeited, total_bonus_paid, current_bonus_rate, updated_at)
VALUES (1, 0, 0, 0, 0.0250, NOW()) AS new
ON DUPLICATE KEY UPDATE challenge_pot_id = new.challenge_pot_id;

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
    '[{"type":"h2","content":"이 챌린지는?"},{"type":"paragraph","content":"세상보다 먼저 눈뜨는 21일, 나만의 새벽 30분."},{"type":"linebreak","content":""},{"type":"h2","content":"하면 좋은 점"},{"type":"blockquote","content":"아침의 나만의 30분이 생겨요"},{"type":"paragraph","content":"세상이 조용한 시간, 방해 없이 나에게 집중할 수 있어요."},{"type":"linebreak","content":""},{"type":"blockquote","content":"마음의 여유가 생겨요"},{"type":"paragraph","content":"허둥지둥 뛰는 아침 대신, 커피 한 잔의 여유를 챙겨요."},{"type":"linebreak","content":""},{"type":"blockquote","content":"생체 리듬이 정렬돼요"},{"type":"paragraph","content":"일찍 일어나면 밤에 잠도 잘 오는 선순환이 만들어져요."},{"type":"linebreak","content":""},{"type":"blockquote","content":"하루의 주도권을 되찾아요"},{"type":"paragraph","content":"\\\"시작 당한\\\" 게 아니라 \\\"시작한\\\" 감각으로 살게 돼요."},{"type":"linebreak","content":""},{"type":"h2","content":"이런 분께 추천해요"},{"type":"blockquote","content":"미라클 모닝을 여러 번 시도했지만 매번 3일을 못 넘긴 분"},{"type":"blockquote","content":"출근·등교 전 늘 시간에 쫓기는 게 지친 분"},{"type":"blockquote","content":"아침형 인간이 되고 싶어 자기 계발을 시작하려는 분"},{"type":"blockquote","content":"혼자서는 자꾸 무너져서 함께할 동료가 필요한 분"}]',
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

-- #16 파티 정산 결과 조회 API 더미데이터
-- 주의: refund_amount / bonus_amount / party_share_amount 값은
-- 실제 정산 계산 로직이 아직 없어 정책서 UI 목업("일부 성공" 케이스) 숫자를
-- 그대로 옮겨 채운 예시값입니다. 실제 정산 로직 완성 전까지는 참고용입니다.

INSERT INTO `user` (email, nickname, social_provider, email_verified, point_balance, status, login_fail_count, created_at)
SELECT '오늘두-dummy@test.com', '오늘두', 'EMAIL', true, 50000, 'ACTIVE', 0, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = '오늘두-dummy@test.com');

INSERT INTO `user` (email, nickname, social_provider, email_verified, point_balance, status, login_fail_count, created_at)
SELECT '지호-dummy@test.com', '지호', 'EMAIL', true, 50000, 'ACTIVE', 0, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = '지호-dummy@test.com');

INSERT INTO `user` (email, nickname, social_provider, email_verified, point_balance, status, login_fail_count, created_at)
SELECT '수아-dummy@test.com', '수아', 'EMAIL', true, 50000, 'ACTIVE', 0, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = '수아-dummy@test.com');

INSERT INTO `user` (email, nickname, social_provider, email_verified, point_balance, status, login_fail_count, created_at)
SELECT '도윤-dummy@test.com', '도윤', 'EMAIL', true, 50000, 'ACTIVE', 0, NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE email = '도윤-dummy@test.com');

INSERT INTO party (name, host_user_id, invite_code, invite_expires_at, status, max_members, duration_days, deposit_amount, start_triggered_at, created_at)
SELECT '정산 데모 파티',
       (SELECT user_id FROM `user` WHERE email = '오늘두-dummy@test.com'),
       'DEMO01',
       DATE_SUB(NOW(), INTERVAL 28 DAY),
       'FINISHED',
       4,
       28,
       30000,
       DATE_SUB(NOW(), INTERVAL 28 DAY),
       DATE_SUB(NOW(), INTERVAL 28 DAY)
    WHERE NOT EXISTS (SELECT 1 FROM party WHERE name = '정산 데모 파티');

INSERT INTO party_member (party_id, user_id, role, status, joined_at)
SELECT p.party_id, u.user_id, 'HOST', 'READY', p.created_at
FROM party p, `user` u
WHERE p.name = '정산 데모 파티' AND u.email = '오늘두-dummy@test.com'
  AND NOT EXISTS (SELECT 1 FROM party_member pm WHERE pm.party_id = p.party_id AND pm.user_id = u.user_id);

INSERT INTO party_member (party_id, user_id, role, status, joined_at)
SELECT p.party_id, u.user_id, 'MEMBER', 'READY', p.created_at
FROM party p, `user` u
WHERE p.name = '정산 데모 파티' AND u.email = '지호-dummy@test.com'
  AND NOT EXISTS (SELECT 1 FROM party_member pm WHERE pm.party_id = p.party_id AND pm.user_id = u.user_id);

INSERT INTO party_member (party_id, user_id, role, status, joined_at)
SELECT p.party_id, u.user_id, 'MEMBER', 'READY', p.created_at
FROM party p, `user` u
WHERE p.name = '정산 데모 파티' AND u.email = '수아-dummy@test.com'
  AND NOT EXISTS (SELECT 1 FROM party_member pm WHERE pm.party_id = p.party_id AND pm.user_id = u.user_id);

INSERT INTO party_member (party_id, user_id, role, status, joined_at)
SELECT p.party_id, u.user_id, 'MEMBER', 'READY', p.created_at
FROM party p, `user` u
WHERE p.name = '정산 데모 파티' AND u.email = '도윤-dummy@test.com'
  AND NOT EXISTS (SELECT 1 FROM party_member pm WHERE pm.party_id = p.party_id AND pm.user_id = u.user_id);

INSERT INTO party_challenge (party_id, challenge_id)
SELECT p.party_id, c.challenge_id
FROM party p, challenge c
WHERE p.name = '정산 데모 파티' AND c.name = '매일 6시 기상'
  AND NOT EXISTS (SELECT 1 FROM party_challenge pc WHERE pc.party_id = p.party_id);

INSERT INTO participation (user_id, challenge_id, party_id, participation_type, deposit_amount, duration_weeks, start_date, end_date, status)
SELECT u.user_id, c.challenge_id, p.party_id, 'PARTY', 30000, 4, DATE_SUB(CURDATE(), INTERVAL 28 DAY), CURDATE(), 'SUCCESS'
FROM `user` u, challenge c, party p
WHERE u.email = '오늘두-dummy@test.com' AND c.name = '매일 6시 기상' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM participation WHERE user_id = u.user_id AND party_id = p.party_id);

INSERT INTO participation (user_id, challenge_id, party_id, participation_type, deposit_amount, duration_weeks, start_date, end_date, status)
SELECT u.user_id, c.challenge_id, p.party_id, 'PARTY', 30000, 4, DATE_SUB(CURDATE(), INTERVAL 28 DAY), CURDATE(), 'SUCCESS'
FROM `user` u, challenge c, party p
WHERE u.email = '지호-dummy@test.com' AND c.name = '매일 6시 기상' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM participation WHERE user_id = u.user_id AND party_id = p.party_id);

INSERT INTO participation (user_id, challenge_id, party_id, participation_type, deposit_amount, duration_weeks, start_date, end_date, status)
SELECT u.user_id, c.challenge_id, p.party_id, 'PARTY', 30000, 4, DATE_SUB(CURDATE(), INTERVAL 28 DAY), CURDATE(), 'SUCCESS'
FROM `user` u, challenge c, party p
WHERE u.email = '수아-dummy@test.com' AND c.name = '매일 6시 기상' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM participation WHERE user_id = u.user_id AND party_id = p.party_id);

INSERT INTO participation (user_id, challenge_id, party_id, participation_type, deposit_amount, duration_weeks, start_date, end_date, status)
SELECT u.user_id, c.challenge_id, p.party_id, 'PARTY', 30000, 4, DATE_SUB(CURDATE(), INTERVAL 28 DAY), CURDATE(), 'FAIL'
FROM `user` u, challenge c, party p
WHERE u.email = '도윤-dummy@test.com' AND c.name = '매일 6시 기상' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM participation WHERE user_id = u.user_id AND party_id = p.party_id);

INSERT INTO settlement (participation_id, deposit_amount, r_value, refund_amount, deposit_refund_amount, bonus_amount, party_share_amount, status, processed_at, confirmed)
SELECT pt.participation_id, 30000, 1.33, 30000, 20000, 0, 10000, 'COMPLETED', NOW(), true
FROM participation pt
         JOIN `user` u ON pt.user_id = u.user_id
         JOIN party p ON pt.party_id = p.party_id
WHERE u.email = '오늘두-dummy@test.com' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM settlement s WHERE s.participation_id = pt.participation_id);

INSERT INTO settlement (participation_id, deposit_amount, r_value, refund_amount, deposit_refund_amount, bonus_amount, party_share_amount, status, processed_at, confirmed)
SELECT pt.participation_id, 30000, 1.33, 30000, 20000, 0, 10000, 'COMPLETED', NOW(), true
FROM participation pt
         JOIN `user` u ON pt.user_id = u.user_id
         JOIN party p ON pt.party_id = p.party_id
WHERE u.email = '지호-dummy@test.com' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM settlement s WHERE s.participation_id = pt.participation_id);

INSERT INTO settlement (participation_id, deposit_amount, r_value, refund_amount, deposit_refund_amount, bonus_amount, party_share_amount, status, processed_at, confirmed)
SELECT pt.participation_id, 30000, 1.33, 30000, 20000, 0, 10000, 'COMPLETED', NOW(), true
FROM participation pt
         JOIN `user` u ON pt.user_id = u.user_id
         JOIN party p ON pt.party_id = p.party_id
WHERE u.email = '수아-dummy@test.com' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM settlement s WHERE s.participation_id = pt.participation_id);

INSERT INTO settlement (participation_id, deposit_amount, r_value, refund_amount, deposit_refund_amount, bonus_amount, party_share_amount, status, processed_at, confirmed)
SELECT pt.participation_id, 30000, 0.60, 18000, 18000, 0, 0, 'COMPLETED', NOW(), true
FROM participation pt
         JOIN `user` u ON pt.user_id = u.user_id
         JOIN party p ON pt.party_id = p.party_id
WHERE u.email = '도윤-dummy@test.com' AND p.name = '정산 데모 파티'
  AND NOT EXISTS (SELECT 1 FROM settlement s WHERE s.participation_id = pt.participation_id);
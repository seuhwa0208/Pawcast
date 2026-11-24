BEGIN;

-- 대상 회원(2, 4, 6, 7, 9)의 기본 배송지가 없으면 생성
INSERT INTO address (
    member_id, label, recipient, tel, zipcode, address1, address2, is_default, created_at, updated_at
)
SELECT
    m.id,
    '기본 배송지',
    COALESCE(NULLIF(m.name, ''), '테스트회원' || m.id),
    COALESCE(NULLIF(m.tel, ''), '010-0000-' || lpad(m.id::text, 4, '0')),
    '04524',
    '서울특별시 중구 세종대로 110',
    '테스트 상세주소',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM members m
WHERE m.id IN (2, 4, 6, 7, 9)
  AND NOT EXISTS (
      SELECT 1 FROM address a WHERE a.member_id = m.id
  );

-- 기존 address 행은 있지만 recipient / tel / address1 등이 비어 있는 경우 보완
UPDATE address a
   SET recipient = COALESCE(NULLIF(a.recipient, ''), m.name, '테스트회원' || a.member_id),
       tel       = COALESCE(NULLIF(a.tel, ''), m.tel, '010-0000-' || lpad(a.member_id::text, 4, '0')),
       zipcode   = COALESCE(NULLIF(a.zipcode, ''), '04524'),
       address1  = COALESCE(NULLIF(a.address1, ''), '서울특별시 중구 세종대로 110'),
       address2  = COALESCE(NULLIF(a.address2, ''), '테스트 상세주소'),
       is_default = COALESCE(a.is_default, true),
       updated_at = CURRENT_TIMESTAMP
  FROM members m
 WHERE m.id = a.member_id
   AND a.member_id IN (2, 4, 6, 7, 9)
   AND (
       a.recipient IS NULL OR btrim(a.recipient) = ''
       OR a.tel IS NULL OR btrim(a.tel) = ''
       OR a.address1 IS NULL OR btrim(a.address1) = ''
   );

COMMIT;
-- QnA 보정: (1) 3주 넘게 답변이 없는 문의에 답변을 채우고 (2) 최근 10일 신규 문의 16건(답변 대기)을 추가합니다.
-- 이유: 기존 시드에는 2023~2024년 문의가 '답변 대기'로 남아 최장 대기 1,000일 이상으로 보였습니다.
-- pawcast_seed_3y.sql 실행 후 1회만 실행하세요(두 번 실행하면 신규 문의가 중복됩니다).
SET client_encoding TO 'UTF8';
BEGIN;
INSERT INTO qna_answers(qna_id,member_id,content,created_at,updated_at)
SELECT q.id, CASE WHEN q.id % 2 = 0 THEN 1 ELSE 3 END,
       CASE WHEN q.type='PRODUCT' THEN '문의 감사합니다. 상품 상세 페이지의 안내를 확인해 주시고, 추가 문의는 고객센터로 남겨 주세요.'
            ELSE '문의 감사합니다. 확인 후 처리해 드렸으며 추가 문의가 있으시면 언제든 남겨 주세요.' END,
       q.created_at + (abs(hashtext(q.id::text)) % 90 + 4) * interval '1 hour',
       q.created_at + (abs(hashtext(q.id::text)) % 90 + 4) * interval '1 hour'
FROM qnas q LEFT JOIN qna_answers a ON a.qna_id = q.id
WHERE a.id IS NULL AND q.deleted_at IS NULL AND q.created_at < TIMESTAMP '2026-10-30';

WITH t(n, typ, title, body, priv) AS (VALUES
 (1,'PRODUCT','원재료 문의','사료에 닭고기가 들어가나요? 알러지가 있어서 문의드려요.',false),
 (2,'GENERAL','배송 문의','주문한 상품이 아직 도착하지 않았어요. 언제쯤 받을 수 있나요?',true),
 (3,'PRODUCT','재입고 문의','품절된 옵션 재입고 일정이 궁금합니다.',false),
 (4,'GENERAL','교환/환불 문의','상품을 받았는데 교환하고 싶어요. 절차가 어떻게 되나요?',true),
 (5,'PRODUCT','급여량 문의','체중 5kg 아이는 하루에 얼마나 급여하면 되나요?',false),
 (6,'GENERAL','쿠폰 문의','신규 가입 쿠폰이 보이지 않아요.',false),
 (7,'PRODUCT','사용법 문의','자동 급수기 필터 교체 주기가 궁금해요.',false),
 (8,'GENERAL','세일 기간 문의','이번 3주년 세일은 언제까지 진행되나요?',false),
 (9,'PRODUCT','유통기한 문의','유통기한이 어떻게 되나요?',false),
 (10,'GENERAL','결제 오류 문의','결제 중 오류가 나서 중복으로 결제된 것 같아요.',true),
 (11,'PRODUCT','세척 문의','분리해서 세척해도 되나요? 식기세척기 사용이 가능한지 궁금합니다.',false),
 (12,'GENERAL','회원정보 변경 문의','휴대폰 번호를 변경하고 싶은데 어디서 하나요?',true),
 (13,'PRODUCT','옵션 문의','대용량 옵션은 언제 입고되나요?',false),
 (14,'GENERAL','배송지 변경 문의','주문 후 배송지를 바꾸고 싶어요.',true),
 (15,'GENERAL','블랙프라이데이 문의','블랙프라이데이 할인은 기존 쿠폰과 중복 적용되나요?',false),
 (16,'PRODUCT','알러지 문의','곡물이 없는 제품이 맞나요?',false))
INSERT INTO qnas(member_id,product_id,type,title,content,is_private,deleted_at,created_at,updated_at)
SELECT m.id,
       CASE WHEN t.typ='PRODUCT' THEN (SELECT id FROM products ORDER BY md5(id::text || t.n::text) LIMIT 1) END,
       t.typ, t.title, t.body, t.priv, NULL,
       TIMESTAMP '2026-11-20 10:00' - (t.n * 14) * interval '1 hour' - (abs(hashtext(t.n::text)) % 50) * interval '1 minute',
       TIMESTAMP '2026-11-20 10:00' - (t.n * 14) * interval '1 hour' - (abs(hashtext(t.n::text)) % 50) * interval '1 minute'
FROM t CROSS JOIN LATERAL (SELECT id FROM members WHERE status='ACTIVE' AND id>3 ORDER BY md5(id::text || t.n::text) LIMIT 1) m;
COMMIT;
SELECT count(*) FILTER (WHERE a.id IS NULL) AS waiting, max(extract(day FROM TIMESTAMP '2026-11-20 10:30' - q.created_at)) FILTER (WHERE a.id IS NULL) AS oldest_waiting_days, count(*) AS total
FROM qnas q LEFT JOIN qna_answers a ON a.qna_id=q.id WHERE q.deleted_at IS NULL;

-- # 기존 DB가 QnaStatus enum을 도입하기 전 생성된 경우에만 한 번 실행한다.
ALTER TABLE qnas
    ADD COLUMN IF NOT EXISTS status varchar(255) NOT NULL DEFAULT 'WAITING';

-- 기존 답변이 있는 문의는 완료 상태로 복구한다.
UPDATE qnas q
SET status = CASE
    WHEN EXISTS (SELECT 1 FROM qna_answers a WHERE a.qna_id = q.id) THEN 'ANSWERED'
    ELSE 'WAITING'
END;

-- 원인: 이미 데이터가 들어 있는 qnas 테이블에 NOT NULL 제약조건이 붙은 status 컬럼을 추가하려다 보니, 
-- 기존 행들의 값이 null이라 PostgreSQL이 막은 거예요. (엔티티에 status 필드가 추가되었거나 nullable = false가 반영된 상태)
ALTER TABLE qnas ADD COLUMN status varchar(255);
UPDATE qnas SET status = 'WAITING' WHERE status IS NULL;
ALTER TABLE qnas ALTER COLUMN status SET NOT NULL;
ALTER TABLE qnas ADD CONSTRAINT qnas_status_check CHECK (status IN ('WAITING', 'ANSWERED'));
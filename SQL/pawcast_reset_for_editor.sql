-- DBeaver / pgAdmin 같은 편집기에서 실행하는 초기화 스크립트 (psql 명령 없이 동작)
-- 용도: pawcast DB에 이미 데이터가 있거나, 시드 실행 중 오류가 나서 트랜잭션이 중단된 경우
-- 주의: pawcast DB의 모든 테이블 데이터가 삭제됩니다.
-- 실행 순서: ① 이 파일 전체 실행 → ② pawcast_seed_3y.sql 전체 실행
-- 먼저 중단된 트랜잭션을 정리합니다. ("현재 트랜잭션은 중지되어 있습니다" 오류 해결)
ROLLBACK;

-- 1) 모든 테이블 비우기 + id 시퀀스 초기화
DO $$
DECLARE r record;
BEGIN
 FOR r IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
  EXECUTE format('TRUNCATE TABLE public.%I RESTART IDENTITY CASCADE', r.tablename);
 END LOOP;
END $$;

-- 2) ERD 변경 반영: products.product_type = DOG / CAT / DEVICE
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_product_type_check;
ALTER TABLE products ADD CONSTRAINT products_product_type_check CHECK (product_type IN ('DOG','CAT','DEVICE'));

-- 3) 확인: 아래 결과가 모두 0이면 시드를 실행해도 됩니다.
SELECT 'members' AS t, count(*) FROM members UNION ALL SELECT 'products', count(*) FROM products UNION ALL SELECT 'orders', count(*) FROM orders;

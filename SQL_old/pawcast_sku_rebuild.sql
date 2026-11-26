-- ============================================================
-- Pawcast SKU 코드 재작성 (pawcast_sku_prefix_codes.csv / pawcast_sku_brand_codes.csv 규칙)
-- 형식: {분류코드}-{브랜드코드}-{상품번호 5자리}-{옵션}
--   · 상품번호 = products.id 를 5자리 0패딩 (예: id 1 -> 00001, id 2401 -> 02401)
--   · 옵션: 건식 사료(DDF, CDF)는 용량 대문자 (1KG, 300G), 나머지는 상품 내 옵션 순번 3자리 (001, 002...)
-- 실행: psql -U postgres -h localhost -d pawcast -f pawcast_sku_rebuild.sql
-- ============================================================
\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;

CREATE TEMP TABLE sku_map ON COMMIT DROP AS
WITH cat_map(category_id, prefix) AS (VALUES
    (3,'DDF'),
    (4,'DWF'),
    (5,'DFF'),
    (6,'DAF'),
    (8,'DDT'),
    (9,'DFT'),
    (10,'DJT'),
    (11,'DPT'),
    (13,'DJS'),
    (14,'DSS'),
    (15,'DGS'),
    (16,'DES'),
    (19,'CDF'),
    (20,'CWF'),
    (21,'CFF'),
    (22,'CAF'),
    (24,'CDT'),
    (25,'CFT'),
    (26,'CJT'),
    (27,'CPT'),
    (29,'CJS'),
    (30,'CSS'),
    (31,'CGS'),
    (32,'CES'),
    (35,'AFD'),
    (36,'MFD'),
    (37,'PFD'),
    (39,'WTR'),
    (40,'PWT'),
    (42,'CBW'),
    (43,'SBW'),
    (44,'HBW'),
    (46,'WFL'),
    (47,'RFL'),
    (48,'CLN')
), brand_map(brand_name, code) AS (VALUES
    ('아카나','ACN'),
    ('ANF','ANF'),
    ('바비온','BBO'),
    ('캣잇','CAT'),
    ('딩동펫','DDP'),
    ('하겐','HGN'),
    ('힐스','HLS'),
    ('하림펫푸드','HRM'),
    ('호머런펫','HRP'),
    ('이나바','INB'),
    ('인스팅트','INS'),
    ('리스펫','LSP'),
    ('내추럴발란스','NBL'),
    ('뉴트리플랜','NTP'),
    ('오리젠','ORJ'),
    ('PETKIT','PKT'),
    ('PETLIBRO','PLB'),
    ('퓨리나 원','PON'),
    ('퓨리나 프로플랜','PPL'),
    ('펫세이프','PSF'),
    ('로얄캐닌','RYC'),
    ('웰니스','WLN'),
    ('샤오미','XMI')
)
SELECT v.id AS variant_id,
       v.sku_code AS old_sku,
       cm.prefix || '-' || bm.code || '-' || lpad(p.id::text, 5, '0') || '-' ||
       CASE WHEN cm.prefix IN ('DDF','CDF')
            THEN upper(replace(v.option_label, ' ', ''))
            ELSE lpad((row_number() OVER (PARTITION BY v.product_id ORDER BY v.id))::text, 3, '0')
       END AS new_sku
FROM product_variants v
JOIN products   p  ON p.id = v.product_id
JOIN cat_map    cm ON cm.category_id = p.category_id
JOIN brand_map  bm ON bm.brand_name  = p.brand;

-- 안전 확인: 누락/중복이 있으면 중단
DO $$
DECLARE total int; mapped int; dup int;
BEGIN
    SELECT count(*) INTO total  FROM product_variants;
    SELECT count(*) INTO mapped FROM sku_map;
    SELECT count(*) INTO dup FROM (SELECT new_sku FROM sku_map GROUP BY 1 HAVING count(*) > 1) d;
    IF total <> mapped THEN RAISE EXCEPTION '규칙에 매핑되지 않는 옵션이 있습니다 (전체 %, 매핑 %)', total, mapped; END IF;
    IF dup > 0 THEN RAISE EXCEPTION '새 SKU 중복 % 건', dup; END IF;
END $$;

UPDATE product_variants v
   SET sku_code = m.new_sku, updated_at = CURRENT_TIMESTAMP
  FROM sku_map m
 WHERE m.variant_id = v.id AND v.sku_code <> m.new_sku;

COMMIT;

-- 결과 확인
SELECT sku_code FROM product_variants ORDER BY id LIMIT 8;
SELECT split_part(sku_code,'-',1) AS prefix, count(*) FROM product_variants GROUP BY 1 ORDER BY 1;

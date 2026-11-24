-- ============================================================
-- Pawcast 회원 시나리오 더미 (반려동물 / 배송지 / 주문·취소·환불·리뷰)
-- 대상 회원: 2(test001), 4, 6, 7, 9  (전부 ACTIVE / USER)
-- 실행: psql -U postgres -h localhost -d pawcast -f pawcast_member_scenarios.sql
-- 주의: 1회만 실행 (이미 TEST- 주문이 있으면 중단). 재고·판매수·리뷰수도 함께 반영됨.
-- ============================================================
\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;

DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM orders WHERE order_number LIKE 'TEST-%') THEN
    RAISE EXCEPTION '이미 시나리오 주문(TEST-*)이 있습니다. 한 번만 실행하세요.';
  END IF;
END $$;

-- 1) 반려동물 (같은 이름이 이미 있으면 건너뜀)
INSERT INTO pets (member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at)
SELECT 2,'코코','CAT',DATE '2021-04-10','코리안숏헤어',4.20,NULL,(SELECT created_at + interval '10 days' FROM members WHERE id=2),(SELECT created_at + interval '10 days' FROM members WHERE id=2)
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE member_id=2 AND name='코코');
INSERT INTO pets (member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at)
SELECT 2,'별이','DOG',DATE '2025-06-01','포메라니안',2.10,'BEEF',(SELECT created_at + interval '10 days' FROM members WHERE id=2),(SELECT created_at + interval '10 days' FROM members WHERE id=2)
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE member_id=2 AND name='별이');
INSERT INTO pets (member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at)
SELECT 4,'나비','CAT',DATE '2020-09-15','러시안블루',4.80,'FISH',(SELECT created_at + interval '10 days' FROM members WHERE id=4),(SELECT created_at + interval '10 days' FROM members WHERE id=4)
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE member_id=4 AND name='나비');
INSERT INTO pets (member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at)
SELECT 6,'두부','DOG',DATE '2019-03-22','푸들',5.60,NULL,(SELECT created_at + interval '10 days' FROM members WHERE id=6),(SELECT created_at + interval '10 days' FROM members WHERE id=6)
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE member_id=6 AND name='두부');
INSERT INTO pets (member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at)
SELECT 7,'모모','CAT',DATE '2022-01-08','브리티시숏헤어',5.10,NULL,(SELECT created_at + interval '10 days' FROM members WHERE id=7),(SELECT created_at + interval '10 days' FROM members WHERE id=7)
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE member_id=7 AND name='모모');
INSERT INTO pets (member_id,name,pet_type,birthday,breed_name,weight_kg,allergies,created_at,updated_at)
SELECT 9,'라떼','DOG',DATE '2023-07-19','비숑프리제',6.40,'CHICKEN',(SELECT created_at + interval '10 days' FROM members WHERE id=9),(SELECT created_at + interval '10 days' FROM members WHERE id=9)
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE member_id=9 AND name='라떼');

-- 2) 배송지 (같은 라벨이 이미 있으면 건너뜀)
INSERT INTO address (member_id,label,recipient,tel,zipcode,address1,address2,is_default)
SELECT 2,'회사',(SELECT name FROM members WHERE id=2),(SELECT tel FROM address WHERE member_id=2 AND is_default LIMIT 1),'06236','서울특별시 강남구 테헤란로 152','12층 1203호',false
WHERE NOT EXISTS (SELECT 1 FROM address WHERE member_id=2 AND label='회사');
INSERT INTO address (member_id,label,recipient,tel,zipcode,address1,address2,is_default)
SELECT 2,'부모님 집',(SELECT name FROM members WHERE id=2),(SELECT tel FROM address WHERE member_id=2 AND is_default LIMIT 1),'48058','부산광역시 해운대구 센텀중앙로 79','101동 1504호',false
WHERE NOT EXISTS (SELECT 1 FROM address WHERE member_id=2 AND label='부모님 집');
INSERT INTO address (member_id,label,recipient,tel,zipcode,address1,address2,is_default)
SELECT 4,'회사',(SELECT name FROM members WHERE id=4),(SELECT tel FROM address WHERE member_id=4 AND is_default LIMIT 1),'13529','경기도 성남시 분당구 판교역로 235','7층',false
WHERE NOT EXISTS (SELECT 1 FROM address WHERE member_id=4 AND label='회사');
INSERT INTO address (member_id,label,recipient,tel,zipcode,address1,address2,is_default)
SELECT 6,'회사',(SELECT name FROM members WHERE id=6),(SELECT tel FROM address WHERE member_id=6 AND is_default LIMIT 1),'04513','서울특별시 중구 세종대로 110','5층 오피스',false
WHERE NOT EXISTS (SELECT 1 FROM address WHERE member_id=6 AND label='회사');
INSERT INTO address (member_id,label,recipient,tel,zipcode,address1,address2,is_default)
SELECT 7,'친구 집',(SELECT name FROM members WHERE id=7),(SELECT tel FROM address WHERE member_id=7 AND is_default LIMIT 1),'41068','대구광역시 동구 동대구로 489','302호',false
WHERE NOT EXISTS (SELECT 1 FROM address WHERE member_id=7 AND label='친구 집');
INSERT INTO address (member_id,label,recipient,tel,zipcode,address1,address2,is_default)
SELECT 9,'회사',(SELECT name FROM members WHERE id=9),(SELECT tel FROM address WHERE member_id=9 AND is_default LIMIT 1),'34126','대전광역시 유성구 대학로 99','3층 사무실',false
WHERE NOT EXISTS (SELECT 1 FROM address WHERE member_id=9 AND label='회사');

-- 3) 시나리오 함수 (이 세션에서만 존재)
CREATE FUNCTION pg_temp.mk_order(p_mem bigint, p_no text, p_status text, p_at timestamp, p_vars bigint[], p_qtys int[],
                                 p_method text, p_pay text, p_ship text) RETURNS bigint AS $f$
DECLARE
  oid bigint; pid bigint; tot numeric := 0; fee numeric; i int; v record; ad record; mem record; sbefore int;
  msgs text[] := ARRAY['문 앞에 놓아주세요.','경비실에 맡겨주세요.','배송 전 연락 부탁드립니다.','부재 시 택배함에 넣어주세요.'];
  sh_at timestamp; dl_at timestamp;
BEGIN
  SELECT * INTO mem FROM members WHERE id = p_mem;
  SELECT * INTO ad FROM address WHERE member_id = p_mem AND is_default LIMIT 1;
  FOR i IN 1..array_length(p_vars,1) LOOP
    SELECT price INTO v FROM product_variants WHERE id = p_vars[i];
    tot := tot + v.price * p_qtys[i];
  END LOOP;
  fee := CASE WHEN tot >= 50000 THEN 0 ELSE 3000 END;
  INSERT INTO orders (member_id,order_number,status,total_amount,discount_amount,shipping_fee,final_amount,
        recipient_name,recipient_phone,recipient_email,delivery_zipcode,delivery_address1,delivery_address2,delivery_message,created_at)
  VALUES (p_mem,p_no,p_status,tot,0,fee,tot+fee,ad.recipient,ad.tel,mem.email,ad.zipcode,ad.address1,ad.address2,
        msgs[1 + abs(hashtext(p_no)) % 4],p_at) RETURNING id INTO oid;
  FOR i IN 1..array_length(p_vars,1) LOOP
    SELECT v2.price, v2.option_label, v2.stock, v2.product_id, p.name AS pname INTO v
      FROM product_variants v2 JOIN products p ON p.id = v2.product_id WHERE v2.id = p_vars[i];
    INSERT INTO order_items (order_id,product_variant_id,product_name,option_label,quantity,unit_price,total_price)
    VALUES (oid,p_vars[i],left(v.pname,50),left(v.option_label,50),p_qtys[i],v.price,v.price*p_qtys[i]);
    -- 재고 차감 + 이력 (주문 시점)
    sbefore := v.stock;
    UPDATE product_variants SET stock = stock - p_qtys[i], sales_count = sales_count + p_qtys[i],
           product_variant_status = CASE WHEN stock - p_qtys[i] <= 0 THEN 'SOLD_OUT' ELSE product_variant_status END,
           updated_at = p_at WHERE id = p_vars[i];
    UPDATE products SET sales_count = sales_count + p_qtys[i], updated_at = p_at WHERE id = v.product_id;
    INSERT INTO inventory_transactions (variant_id,member_id,transaction_type,quantity_change,stock_before,stock_after,reference_type,created_at)
    VALUES (p_vars[i],NULL,'OUTBOUND',-p_qtys[i],sbefore,sbefore-p_qtys[i],'ORDER',p_at);
  END LOOP;
  INSERT INTO payments (order_id,payment_method,provider,payment_key,idempotency_key,amount,status,paid_at,created_at)
  VALUES (oid,p_method,'DEMO_ONLY','DEMO-NOT-A-PG-KEY-'||p_no,'DEMO-IDEMPOTENCY-'||p_no,tot+fee,p_pay,
        CASE WHEN p_pay IN ('PAID','CANCELLED') THEN p_at + interval '3 minutes' END, p_at + interval '1 minute');
  IF p_ship IS NOT NULL THEN
    sh_at := p_at + interval '1 day 2 hours';
    dl_at := CASE WHEN p_ship = 'DELIVERED' THEN p_at + interval '2 days 6 hours' END;
    INSERT INTO shipments (order_id,delivery_api_provider,external_shipment_id,carrier_code,tracking_number,status,
                           last_event_at,last_synced_at,shipped_at,delivered_at)
    VALUES (oid,'DELIVERY_TRACKER','DT-'||p_no,'kr.cjlogistics',lpad(((abs(hashtext(p_no))::bigint * 7919) % 1000000000000)::text,12,'0'),
            p_ship, COALESCE(dl_at, LEAST(sh_at + interval '8 hours', now() - interval '1 hour')),
            COALESCE(dl_at, LEAST(sh_at + interval '8 hours', now() - interval '1 hour')) + interval '9 minutes', sh_at, dl_at);
  END IF;
  RETURN oid;
END $f$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.mk_refund(p_no text, p_status text, p_reason text, p_at timestamp) RETURNS void AS $f$
DECLARE r record; it record; sb int;
BEGIN
  SELECT p.id AS pay_id, p.amount, o.id AS oid INTO r FROM orders o JOIN payments p ON p.order_id = o.id WHERE o.order_number = p_no;
  INSERT INTO refunds (payment_id,provider_refund_key,idempotency_key,refund_amount,reason,status,created_at,completed_at)
  VALUES (r.pay_id, CASE WHEN p_status='COMPLETED' THEN 'DEMO-REFUND-KEY-'||p_no END, 'DEMO-REFUND-'||p_no, r.amount, p_reason, p_status, p_at,
          CASE WHEN p_status='COMPLETED' THEN p_at + interval '5 hours' END);
  IF p_status = 'COMPLETED' THEN   -- 취소 확정: 재고 복원 + 판매수 감소
    FOR it IN SELECT product_variant_id AS vid, quantity AS q FROM order_items WHERE order_id = r.oid LOOP
      SELECT stock INTO sb FROM product_variants WHERE id = it.vid;
      UPDATE product_variants SET stock = stock + it.q, sales_count = sales_count - it.q,
             product_variant_status = CASE WHEN product_variant_status='SOLD_OUT' THEN 'ON_SALE' ELSE product_variant_status END, updated_at = p_at WHERE id = it.vid;
      UPDATE products SET sales_count = sales_count - it.q WHERE id = (SELECT product_id FROM product_variants WHERE id = it.vid);
      INSERT INTO inventory_transactions (variant_id,member_id,transaction_type,quantity_change,stock_before,stock_after,reference_type,created_at)
      VALUES (it.vid,NULL,'RETURN',it.q,sb,sb+it.q,'REFUND',p_at + interval '5 hours');
    END LOOP;
  END IF;
END $f$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.mk_cancel_unpaid(p_no text, p_at timestamp) RETURNS void AS $f$
DECLARE r record; it record; sb int;
BEGIN
  SELECT id INTO r FROM orders WHERE order_number = p_no;
  FOR it IN SELECT product_variant_id AS vid, quantity AS q FROM order_items WHERE order_id = r.id LOOP
    SELECT stock INTO sb FROM product_variants WHERE id = it.vid;
    UPDATE product_variants SET stock = stock + it.q, sales_count = sales_count - it.q,
           product_variant_status = CASE WHEN product_variant_status='SOLD_OUT' THEN 'ON_SALE' ELSE product_variant_status END, updated_at = p_at WHERE id = it.vid;
    UPDATE products SET sales_count = sales_count - it.q WHERE id = (SELECT product_id FROM product_variants WHERE id = it.vid);
    INSERT INTO inventory_transactions (variant_id,member_id,transaction_type,quantity_change,stock_before,stock_after,reference_type,created_at)
    VALUES (it.vid,NULL,'RETURN',it.q,sb,sb+it.q,'REFUND',p_at);
  END LOOP;
END $f$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.mk_review(p_no text, p_idx int, p_rating int, p_content text, p_at timestamp, p_img text) RETURNS void AS $f$
DECLARE it record; rid bigint;
BEGIN
  SELECT oi.id AS item_id, o.member_id AS mem, v.product_id AS pid INTO it
    FROM orders o JOIN order_items oi ON oi.order_id = o.id JOIN product_variants v ON v.id = oi.product_variant_id
   WHERE o.order_number = p_no ORDER BY oi.id OFFSET p_idx LIMIT 1;
  INSERT INTO product_reviews (member_id,product_id,order_item_id,rating,content,is_visible,created_at,updated_at)
  VALUES (it.mem,it.pid,it.item_id,p_rating,p_content,true,p_at,p_at) RETURNING id INTO rid;
  INSERT INTO review_sentiments (review_id,sentiment,confidence,model_name,analyzed_at)
  VALUES (rid, CASE WHEN p_rating>=4 THEN '긍정' WHEN p_rating=3 THEN '중립' ELSE '부정' END, 0.90, 'SEED_RULE_LABEL', p_at + interval '1 minute');
  IF p_img IS NOT NULL THEN INSERT INTO review_images (review_id,image_path,sort_order) VALUES (rid,p_img,1); END IF;
  UPDATE products SET review_count = review_count + 1, rating_sum = rating_sum + p_rating WHERE id = it.pid;
END $f$ LANGUAGE plpgsql;

-- 4) 시나리오 주문 (상품 옵션은 재고 30개 이상 판매중 옵션 중에서 고정 순번으로 선택)


-- ---- 회원 2 ----
-- S01: 입금 대기
SELECT pg_temp.mk_order(2,'TEST-M002-S01','PENDING_PAYMENT',(now() - interval '144 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 1013 LIMIT 1)]::bigint[],ARRAY[2],'VIRTUAL_ACCOUNT','PENDING',NULL);
-- S02: 결제완료 - 취소 가능
SELECT pg_temp.mk_order(2,'TEST-M002-S02','PAID',(now() - interval '215 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 1824 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID',NULL);
-- S03: 상품 준비중
SELECT pg_temp.mk_order(2,'TEST-M002-S03','PREPARING',(now() - interval '1440 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 2635 LIMIT 1),(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6206 LIMIT 1)]::bigint[],ARRAY[1,2],'EASY_PAY','PAID',NULL);
-- S04: 배송중
SELECT pg_temp.mk_order(2,'TEST-M002-S04','SHIPPING',(now() - interval '2880 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 3446 LIMIT 1)]::bigint[],ARRAY[2],'CARD','PAID','SHIPPING');
-- S05: 배송완료 - 리뷰 작성/환불 신청 가능
SELECT pg_temp.mk_order(2,'TEST-M002-S05','DELIVERED',(now() - interval '5760 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 4257 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID','DELIVERED');
-- S06: 리뷰 작성 완료(5점)
SELECT pg_temp.mk_order(2,'TEST-M002-S06','DELIVERED',(now() - interval '12960 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 5068 LIMIT 1)]::bigint[],ARRAY[1],'EASY_PAY','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M002-S06',0,5,'우리 애가 정말 잘 먹어요. 포장도 깔끔하고 배송도 빨랐습니다. 재구매 의사 있어요.',(now() - interval '12960 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S07: 2개 중 1개만 리뷰 작성
SELECT pg_temp.mk_order(2,'TEST-M002-S07','DELIVERED',(now() - interval '21600 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 5879 LIMIT 1),(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 450 LIMIT 1)]::bigint[],ARRAY[2,3],'CARD','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M002-S07',0,4,'잘 먹는 편이에요. 용량이 넉넉해서 오래 먹일 수 있을 것 같아요.',(now() - interval '21600 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S08: 환불 요청 대기
SELECT pg_temp.mk_order(2,'TEST-M002-S08','DELIVERED',(now() - interval '28800 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6690 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID','DELIVERED');
SELECT pg_temp.mk_refund('TEST-M002-S08','PENDING','상품 불량',(now() - interval '28800 minutes')::timestamp(0) + interval '3 days' + interval '2 days' * (1));
-- S09: 환불 처리 실패
SELECT pg_temp.mk_order(2,'TEST-M002-S09','DELIVERED',(now() - interval '37440 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 7501 LIMIT 1)]::bigint[],ARRAY[1],'BANK','PAID','DELIVERED');
SELECT pg_temp.mk_refund('TEST-M002-S09','FAILED','단순 변심',(now() - interval '37440 minutes')::timestamp(0) + interval '3 days' + interval '2 days' * (1));
-- S10: 결제 후 취소 - 환불 완료
SELECT pg_temp.mk_order(2,'TEST-M002-S10','CANCELLED',(now() - interval '47520 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 8312 LIMIT 1)]::bigint[],ARRAY[2],'CARD','CANCELLED',NULL);
SELECT pg_temp.mk_refund('TEST-M002-S10','COMPLETED','주문 실수',(now() - interval '47520 minutes')::timestamp(0) + interval '2 hours' + interval '2 days' * (0));
-- S11: 미결제 취소
SELECT pg_temp.mk_order(2,'TEST-M002-S11','CANCELLED',(now() - interval '57600 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 123 LIMIT 1)]::bigint[],ARRAY[3],'VIRTUAL_ACCOUNT','FAILED',NULL);
SELECT pg_temp.mk_cancel_unpaid('TEST-M002-S11',(now() - interval '57600 minutes')::timestamp(0) + interval '30 minutes');
-- S12: 낮은 평점 리뷰(1점, 사진 포함)
SELECT pg_temp.mk_order(2,'TEST-M002-S12','DELIVERED',(now() - interval '72000 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 934 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M002-S12',0,1,'포장이 찢어진 채로 도착했어요. 교환 문의 남깁니다.',(now() - interval '72000 minutes')::timestamp(0) + interval '3 days 4 hours','/assets/products/generated/dry-food-terracotta.png');
-- S13: 오래된 주문 - 리뷰 미작성
SELECT pg_temp.mk_order(2,'TEST-M002-S13','DELIVERED',(now() - interval '108000 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 1745 LIMIT 1),(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 5316 LIMIT 1)]::bigint[],ARRAY[2,3],'EASY_PAY','PAID','DELIVERED');

-- ---- 회원 4 ----
-- S01: 입금 대기
SELECT pg_temp.mk_order(4,'TEST-M004-S01','PENDING_PAYMENT',(now() - interval '144 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 2758 LIMIT 1)]::bigint[],ARRAY[3],'VIRTUAL_ACCOUNT','PENDING',NULL);
-- S02: 결제완료 - 취소 가능
SELECT pg_temp.mk_order(4,'TEST-M004-S02','PAID',(now() - interval '215 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 3569 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID',NULL);
-- S03: 상품 준비중
SELECT pg_temp.mk_order(4,'TEST-M004-S03','PREPARING',(now() - interval '1440 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 4380 LIMIT 1),(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 7951 LIMIT 1)]::bigint[],ARRAY[2,3],'EASY_PAY','PAID',NULL);
-- S04: 배송중
SELECT pg_temp.mk_order(4,'TEST-M004-S04','SHIPPING',(now() - interval '2880 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 5191 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID','SHIPPING');
-- S05: 배송완료 - 리뷰 작성/환불 신청 가능
SELECT pg_temp.mk_order(4,'TEST-M004-S05','DELIVERED',(now() - interval '5760 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6002 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID','DELIVERED');
-- S06: 리뷰 작성 완료(5점)
SELECT pg_temp.mk_order(4,'TEST-M004-S06','DELIVERED',(now() - interval '12960 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6813 LIMIT 1)]::bigint[],ARRAY[2],'EASY_PAY','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M004-S06',0,5,'우리 애가 정말 잘 먹어요. 포장도 깔끔하고 배송도 빨랐습니다. 재구매 의사 있어요.',(now() - interval '12960 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S07: 2개 중 1개만 리뷰 작성
SELECT pg_temp.mk_order(4,'TEST-M004-S07','DELIVERED',(now() - interval '21600 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 7624 LIMIT 1),(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 2195 LIMIT 1)]::bigint[],ARRAY[3,1],'CARD','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M004-S07',0,4,'잘 먹는 편이에요. 용량이 넉넉해서 오래 먹일 수 있을 것 같아요.',(now() - interval '21600 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S08: 환불 요청 대기
SELECT pg_temp.mk_order(4,'TEST-M004-S08','DELIVERED',(now() - interval '28800 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 8435 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID','DELIVERED');
SELECT pg_temp.mk_refund('TEST-M004-S08','PENDING','상품 불량',(now() - interval '28800 minutes')::timestamp(0) + interval '3 days' + interval '2 days' * (1));
-- S09: 환불 처리 실패
SELECT pg_temp.mk_order(4,'TEST-M004-S09','DELIVERED',(now() - interval '37440 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 246 LIMIT 1)]::bigint[],ARRAY[2],'BANK','PAID','DELIVERED');
SELECT pg_temp.mk_refund('TEST-M004-S09','FAILED','단순 변심',(now() - interval '37440 minutes')::timestamp(0) + interval '3 days' + interval '2 days' * (1));
-- S10: 결제 후 취소 - 환불 완료
SELECT pg_temp.mk_order(4,'TEST-M004-S10','CANCELLED',(now() - interval '47520 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 1057 LIMIT 1)]::bigint[],ARRAY[3],'CARD','CANCELLED',NULL);
SELECT pg_temp.mk_refund('TEST-M004-S10','COMPLETED','주문 실수',(now() - interval '47520 minutes')::timestamp(0) + interval '2 hours' + interval '2 days' * (0));
-- S11: 미결제 취소
SELECT pg_temp.mk_order(4,'TEST-M004-S11','CANCELLED',(now() - interval '57600 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 1868 LIMIT 1)]::bigint[],ARRAY[1],'VIRTUAL_ACCOUNT','FAILED',NULL);
SELECT pg_temp.mk_cancel_unpaid('TEST-M004-S11',(now() - interval '57600 minutes')::timestamp(0) + interval '30 minutes');
-- S12: 낮은 평점 리뷰(1점, 사진 포함)
SELECT pg_temp.mk_order(4,'TEST-M004-S12','DELIVERED',(now() - interval '72000 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 2679 LIMIT 1)]::bigint[],ARRAY[2],'CARD','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M004-S12',0,1,'포장이 찢어진 채로 도착했어요. 교환 문의 남깁니다.',(now() - interval '72000 minutes')::timestamp(0) + interval '3 days 4 hours','/assets/products/generated/dry-food-terracotta.png');
-- S13: 오래된 주문 - 리뷰 미작성
SELECT pg_temp.mk_order(4,'TEST-M004-S13','DELIVERED',(now() - interval '108000 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 3490 LIMIT 1),(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 7061 LIMIT 1)]::bigint[],ARRAY[3,1],'EASY_PAY','PAID','DELIVERED');

-- ---- 회원 6 ----
-- S02: 결제완료 - 취소 가능
SELECT pg_temp.mk_order(6,'TEST-M006-S02','PAID',(now() - interval '215 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 4503 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID',NULL);
-- S04: 배송중
SELECT pg_temp.mk_order(6,'TEST-M006-S04','SHIPPING',(now() - interval '2880 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 5314 LIMIT 1)]::bigint[],ARRAY[2],'CARD','PAID','SHIPPING');
-- S05: 배송완료 - 리뷰 작성/환불 신청 가능
SELECT pg_temp.mk_order(6,'TEST-M006-S05','DELIVERED',(now() - interval '5760 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6125 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID','DELIVERED');
-- S06: 리뷰 작성 완료(5점)
SELECT pg_temp.mk_order(6,'TEST-M006-S06','DELIVERED',(now() - interval '12960 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6936 LIMIT 1)]::bigint[],ARRAY[1],'EASY_PAY','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M006-S06',0,5,'우리 애가 정말 잘 먹어요. 포장도 깔끔하고 배송도 빨랐습니다. 재구매 의사 있어요.',(now() - interval '12960 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S10: 결제 후 취소 - 환불 완료
SELECT pg_temp.mk_order(6,'TEST-M006-S10','CANCELLED',(now() - interval '47520 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 7747 LIMIT 1)]::bigint[],ARRAY[2],'CARD','CANCELLED',NULL);
SELECT pg_temp.mk_refund('TEST-M006-S10','COMPLETED','주문 실수',(now() - interval '47520 minutes')::timestamp(0) + interval '2 hours' + interval '2 days' * (0));

-- ---- 회원 7 ----
-- S02: 결제완료 - 취소 가능
SELECT pg_temp.mk_order(7,'TEST-M007-S02','PAID',(now() - interval '215 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 8659 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID',NULL);
-- S04: 배송중
SELECT pg_temp.mk_order(7,'TEST-M007-S04','SHIPPING',(now() - interval '2880 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 470 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID','SHIPPING');
-- S05: 배송완료 - 리뷰 작성/환불 신청 가능
SELECT pg_temp.mk_order(7,'TEST-M007-S05','DELIVERED',(now() - interval '5760 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 1281 LIMIT 1)]::bigint[],ARRAY[2],'CARD','PAID','DELIVERED');
-- S06: 리뷰 작성 완료(5점)
SELECT pg_temp.mk_order(7,'TEST-M007-S06','DELIVERED',(now() - interval '12960 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 2092 LIMIT 1)]::bigint[],ARRAY[3],'EASY_PAY','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M007-S06',0,5,'우리 애가 정말 잘 먹어요. 포장도 깔끔하고 배송도 빨랐습니다. 재구매 의사 있어요.',(now() - interval '12960 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S10: 결제 후 취소 - 환불 완료
SELECT pg_temp.mk_order(7,'TEST-M007-S10','CANCELLED',(now() - interval '47520 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 2903 LIMIT 1)]::bigint[],ARRAY[1],'CARD','CANCELLED',NULL);
SELECT pg_temp.mk_refund('TEST-M007-S10','COMPLETED','주문 실수',(now() - interval '47520 minutes')::timestamp(0) + interval '2 hours' + interval '2 days' * (0));

-- ---- 회원 9 ----
-- S02: 결제완료 - 취소 가능
SELECT pg_temp.mk_order(9,'TEST-M009-S02','PAID',(now() - interval '215 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 3916 LIMIT 1)]::bigint[],ARRAY[2],'CARD','PAID',NULL);
-- S04: 배송중
SELECT pg_temp.mk_order(9,'TEST-M009-S04','SHIPPING',(now() - interval '2880 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 4727 LIMIT 1)]::bigint[],ARRAY[3],'CARD','PAID','SHIPPING');
-- S05: 배송완료 - 리뷰 작성/환불 신청 가능
SELECT pg_temp.mk_order(9,'TEST-M009-S05','DELIVERED',(now() - interval '5760 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 5538 LIMIT 1)]::bigint[],ARRAY[1],'CARD','PAID','DELIVERED');
-- S06: 리뷰 작성 완료(5점)
SELECT pg_temp.mk_order(9,'TEST-M009-S06','DELIVERED',(now() - interval '12960 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 6349 LIMIT 1)]::bigint[],ARRAY[2],'EASY_PAY','PAID','DELIVERED');
SELECT pg_temp.mk_review('TEST-M009-S06',0,5,'우리 애가 정말 잘 먹어요. 포장도 깔끔하고 배송도 빨랐습니다. 재구매 의사 있어요.',(now() - interval '12960 minutes')::timestamp(0) + interval '3 days 4 hours',NULL);
-- S10: 결제 후 취소 - 환불 완료
SELECT pg_temp.mk_order(9,'TEST-M009-S10','CANCELLED',(now() - interval '47520 minutes')::timestamp(0),ARRAY[(SELECT id FROM product_variants WHERE product_variant_status='ON_SALE' AND stock>=30 ORDER BY id OFFSET 7160 LIMIT 1)]::bigint[],ARRAY[3],'CARD','CANCELLED',NULL);
SELECT pg_temp.mk_refund('TEST-M009-S10','COMPLETED','주문 실수',(now() - interval '47520 minutes')::timestamp(0) + interval '2 hours' + interval '2 days' * (0));

-- 5) 사후 정리: 상태 문자열 보정 없음. 결과 확인
COMMIT;

SELECT m.id AS member_id, count(*) AS orders,
       count(*) FILTER (WHERE o.status='PENDING_PAYMENT') AS pending,
       count(*) FILTER (WHERE o.status='PAID')            AS paid,
       count(*) FILTER (WHERE o.status='PREPARING')       AS preparing,
       count(*) FILTER (WHERE o.status='SHIPPING')        AS shipping,
       count(*) FILTER (WHERE o.status='DELIVERED')       AS delivered,
       count(*) FILTER (WHERE o.status='CANCELLED')       AS cancelled
  FROM members m JOIN orders o ON o.member_id = m.id WHERE o.order_number LIKE 'TEST-%' GROUP BY m.id ORDER BY m.id;
SELECT o.order_number, o.status, p.status AS pay, r.status AS refund, (SELECT count(*) FROM order_items i JOIN product_reviews pr ON pr.order_item_id=i.id WHERE i.order_id=o.id) AS reviews
  FROM orders o JOIN payments p ON p.order_id=o.id LEFT JOIN refunds r ON r.payment_id=p.id WHERE o.order_number LIKE 'TEST-M002-%' ORDER BY o.created_at DESC;

# Pawcast DB 적재 가이드 (Windows CMD 기준)

이 문서는 **2026년 주문 중심 시드(`pawcast_seed_2026_orders.sql`)** 환경에서 `pawcast` PostgreSQL 데이터베이스를 초기화하고, 단계별 SQL 스크립트 및 보정 패치를 적용하기 위한 **CMD 실행 명령어와 순서**를 정리한 공유 문서입니다.

---

## 0. 사전 준비 사항

1. **백엔드(Spring Boot) 종료**: DB 초기화 및 테이블 재생성 중 락(Lock)이나 커넥션 충돌을 피하기 위해 실행 중인 백엔드 서버를 먼저 종료합니다.
2. **작업 폴더 이동**: SQL 스크립트들이 위치한 폴더(`D:\pawcast-fin\SQL`)로 이동합니다.
3. **CMD 환경 변수 설정**: Windows **명령 프롬프트(CMD)**에서 아래 명령을 입력해 인코딩, 비밀번호, `psql` 기본 옵션을 설정합니다.
   * CMD에서 SQL을 `-c "..."`로 직접 넘길 때는 **한글 리터럴을 피하거나 UTF-8 파일(`.sql`)로 실행**하는 것을 권장합니다.

```bat
cd /d D:\pawcast-fin\SQL
chcp 65001 >nul
set PGCLIENTENCODING=UTF8
set PGPASSWORD=여기에_postgres_비밀번호
set PSQL=psql -U postgres -h localhost -v ON_ERROR_STOP=1 -q
```

---

## 1. 단계별 SQL 파일 실행 순서

### [1단계] DB 초기화 및 스키마 생성
기존 `pawcast` DB 재생성 및 테이블/제약조건 생성을 진행합니다.

```bat
%PSQL% -d postgres -f pawcast_init.sql
```

### [2단계] 2026 시드 데이터 적재
3년치 전체 시드(`pawcast_seed_3y.sql`) 대신 2026년 주문 및 연관 시드 데이터를 적재합니다.

```bat
%PSQL% -d pawcast -f pawcast_seed_2026_orders.sql
```

### [3단계] 재고 입출고 + 환불 대기/실패 데이터 적재 (2026 시드용 보정)
원본 `pawcast_inventory_refund_extra.sql`은 3년치 옵션(`variant_id`)을 기준으로 작성되어 있어, 2026 시드에 없는 `variant_id` 행을 걸러낸 뒤 `inventory_transactions`와 `refunds`에 반영합니다.

```bat
powershell -NoProfile -Command "$raw = Get-Content -Raw -Encoding UTF8 '.\pawcast_inventory_refund_extra.sql'; $body = $raw -replace '(?m)^\s*BEGIN;\s*\r?\n?', '' -replace '(?m)^\s*COMMIT;\s*\r?\n?', ''; $staged = $body -replace 'INSERT INTO inventory_transactions\(', 'INSERT INTO _stage_inv_tx('; $finalSql = \"SET client_encoding TO 'UTF8';`r`nBEGIN;`r`nCREATE TEMP TABLE _stage_inv_tx (variant_id bigint, member_id bigint, transaction_type text, quantity_change integer, stock_before integer, stock_after integer, reference_type text, created_at timestamptz) ON COMMIT DROP;`r`n\" + $staged + \"`r`nINSERT INTO inventory_transactions(variant_id, member_id, transaction_type, quantity_change, stock_before, stock_after, reference_type, created_at) SELECT s.variant_id, CASE WHEN s.member_id IS NOT NULL AND EXISTS (SELECT 1 FROM members m WHERE m.id = s.member_id) THEN s.member_id ELSE NULL END, s.transaction_type, s.quantity_change, s.stock_before, s.stock_after, s.reference_type, s.created_at FROM _stage_inv_tx s WHERE EXISTS (SELECT 1 FROM product_variants v WHERE v.id = s.variant_id);`r`nCOMMIT;\"; [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'run_inv_refund_patched.sql'), $finalSql, (New-Object System.Text.UTF8Encoding($false)))"

%PSQL% -d pawcast -f run_inv_refund_patched.sql
```

### [4단계] 공지사항 추가
```bat
%PSQL% -d pawcast -f pawcast_notices_extra.sql
```

### [5단계] 리뷰 감성 라벨 (선택)
선택 사항이므로 테스트에 필요하지 않다면 건너뛸 수 있습니다. 2026 시드(`product_reviews`)에 존재하는 리뷰 ID 기준으로만 넣으려면 아래처럼 패치 파일을 만들어 실행합니다.

```bat
powershell -NoProfile -Command "$raw = Get-Content -Raw -Encoding UTF8 '.\pawcast_review_sentiments_optional.sql'; $body = $raw -replace '(?m)^\s*BEGIN;\s*\r?\n?', '' -replace '(?m)^\s*COMMIT;\s*\r?\n?', ''; $staged = $body -replace 'INSERT INTO review_sentiments\(', 'INSERT INTO _stage_rev_sent('; $finalSql = \"SET client_encoding TO 'UTF8';`r`nBEGIN;`r`nCREATE TEMP TABLE _stage_rev_sent (review_id bigint, sentiment text, confidence numeric, analyzed_at timestamptz) ON COMMIT DROP;`r`n\" + $staged + \"`r`nINSERT INTO review_sentiments(review_id, sentiment, confidence, analyzed_at) SELECT s.review_id, s.sentiment, s.confidence, s.analyzed_at FROM _stage_rev_sent s WHERE EXISTS (SELECT 1 FROM product_reviews r WHERE r.id = s.review_id);`r`nCOMMIT;\"; [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'run_review_sentiments_2026.sql'), $finalSql, (New-Object System.Text.UTF8Encoding($false)))"
```

### [6단계] 배송 보강 (해당 파일이 폴더에 있는 경우만 실행)
```bat
%PSQL% -d pawcast -f pawcast_shipping_extra.sql
```

### [7단계] QnA 보정
```bat
%PSQL% -d pawcast -f pawcast_qna_fix.sql
```

### [8단계] QnA status 마이그레이션 (해당 파일이 폴더에 있는 경우만 실행)
```bat
%PSQL% -d pawcast -f qna_status_migration.sql
```

### [9단계] 장바구니 병합 테이블 생성
```bat
%PSQL% -d pawcast -f cart_merge_migration.sql
```

### [10단계] 테스트/관리자 계정 정리
`test001`, `admin`, `manager` 계정을 정리합니다.

```bat
%PSQL% -d pawcast -f pawcast_fix_accounts.sql
```

### [11단계] SKU 규칙 변경 및 재생성
```bat
%PSQL% -d pawcast -f pawcast_sku_rebuild.sql
```

### [12단계] 회원 시나리오 데이터 적재 (2026 시드용 보정 포함)
회원 시나리오(`TEST-M002-*`, `TEST-M004-*`, `TEST-M006-*`, `TEST-M007-*`, `TEST-M009-*`) 생성 전 대상 회원의 기본 배송지(`address`)가 비어 `recipient_name`이 `NULL`이 되는 문제를 방지하고, 2026 시드의 `product_variants` 행 수에 맞춰 `OFFSET` 범위를 보정한 뒤 실행합니다.

1. **대상 회원 기본 배송지 보완** (`address` 테이블에는 `created_at`/`updated_at` 컬럼이 없으므로 제외하고 입력)
   ```bat
   %PSQL% -d pawcast -c "BEGIN; INSERT INTO address (member_id, label, recipient, tel, zipcode, address1, address2, is_default) SELECT m.id, 'DEFAULT_ADDR', COALESCE(NULLIF(m.name, ''), 'TEST_USER_' || m.id), COALESCE(NULLIF(m.tel, ''), '010-0000-' || lpad(m.id::text, 4, '0')), '04524', 'SEOUL_TEST_ADDR_1', 'SEOUL_TEST_ADDR_2', true FROM members m WHERE m.id IN (2, 4, 6, 7, 9) AND NOT EXISTS (SELECT 1 FROM address a WHERE a.member_id = m.id); UPDATE address a SET recipient = COALESCE(NULLIF(a.recipient, ''), m.name, 'TEST_USER_' || a.member_id), tel = COALESCE(NULLIF(a.tel, ''), m.tel, '010-0000-' || lpad(a.member_id::text, 4, '0')), zipcode = COALESCE(NULLIF(a.zipcode, ''), '04524'), address1 = COALESCE(NULLIF(a.address1, ''), 'SEOUL_TEST_ADDR_1'), address2 = COALESCE(NULLIF(a.address2, ''), 'SEOUL_TEST_ADDR_2'), is_default = COALESCE(a.is_default, true) FROM members m WHERE m.id = a.member_id AND a.member_id IN (2, 4, 6, 7, 9) AND (a.recipient IS NULL OR btrim(a.recipient) = '' OR a.tel IS NULL OR btrim(a.tel) = '' OR a.address1 IS NULL OR btrim(a.address1) = ''); COMMIT;"
   ```

2. **`OFFSET` 범위 보정 스크립트 생성 및 실행**
   ```bat
   powershell -NoProfile -Command "$s = Get-Content -Raw -Encoding UTF8 '.\pawcast_member_scenarios.sql'; $fixed = [regex]::Replace($s, 'OFFSET\s+(\d+)\s+LIMIT\s+1', { param($m) $n = [int64]$m.Groups[1].Value; 'OFFSET (' + $n + ' % GREATEST((SELECT count(*) FROM product_variants WHERE product_variant_status=''ON_SALE'' AND stock>=30), 1)) LIMIT 1' }); [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'run_member_scenarios_2026.sql'), $fixed, (New-Object System.Text.UTF8Encoding($false)))"

   %PSQL% -d pawcast -f run_member_scenarios_2026.sql
   ```

### [추가] 환불 거절(`REJECTED`) 상태 반영
환불 `REJECTED` 상태 관련 SQL이 폴더에 있다면 함께 실행합니다.

```bat
%PSQL% -d pawcast -f pawcast_refund_rejected_status.sql
```

---

## 2. 최종 적재 확인 쿼리

모든 단계를 마친 후 아래 명령으로 주요 테이블의 데이터 건수를 확인합니다.

```bat
%PSQL% -d pawcast -c "SELECT count(*) AS orders_cnt FROM orders; SELECT count(*) AS members_cnt FROM members; SELECT count(*) AS variants_cnt FROM product_variants; SELECT count(*) AS tx_cnt FROM inventory_transactions; SELECT count(*) AS scenario_orders FROM orders WHERE order_number LIKE 'TEST-%%';"
```

---

## 3. 자주 겪는 이슈 요약

* **`'...' is not recognized as an internal or external command`**: CMD(`D:\pawcast-fin\SQL>`) 창에 PowerShell 변수 구문(`$...`)이나 SQL 구문을 직접 붙여넣었을 때 발생합니다. PowerShell 구문은 `powershell -NoProfile -Command "..."` 형태로 한 줄 실행하거나 PowerShell 프롬프트에서 실행하세요.
* **`"UTF8" 인코딩에서 사용할 수 없는 문자가 있음: 0xb1`**: `PGCLIENTENCODING=UTF8` 설정 상태에서 CMD `-c "..."` 인자에 한글을 직접 넣을 때 발생합니다. `-c` 인자에는 영문/숫자만 쓰거나 UTF-8 `.sql` 파일로 실행하세요.
* **`column "created_at" of relation "address" does not exist`**: `address` 테이블에는 `created_at`, `updated_at` 컬럼이 없으므로 `INSERT`/`UPDATE` 컬럼 목록에서 제외해야 합니다.
* **`"recipient_name"` 또는 `"total_amount"` `null` 값이 `not null` 제약조건 위반**: 2026 시드 환경에서 기본 배송지가 없거나 `product_variants` 행 수가 3년치 시드보다 적어 `OFFSET` 결과가 `NULL`이 될 때 발생합니다. 위 **12단계** 절차대로 배송지 보완 및 `OFFSET` 모듈로 보정 후 실행하세요.
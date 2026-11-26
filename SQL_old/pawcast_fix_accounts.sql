-- ============================================================
-- 관리자 / 매니저 / 일반회원 로그인 계정 정리 (비밀번호 모두 password1!)
--   admin    → ADMIN    (관리자 대시보드)
--   manager  → MANAGER  (매니저 대시보드)
--   test001  → USER     (일반 회원)
-- 로그인은 이메일이 아니라 "아이디(user_id)"로 합니다. (AuthService.login → findByUserId)
-- 실행: psql -U postgres -h localhost -d pawcast -f pawcast_fix_accounts.sql
-- ============================================================
BEGIN;

CREATE TEMP TABLE acc(user_id text, name text, email text, role text) ON COMMIT DROP;
INSERT INTO acc VALUES
  ('admin',   '기본 관리자',   'admin@example.com',   'ADMIN'),
  ('manager', '기본 매니저',   'manager@example.com', 'MANAGER'),
  ('test001', '기본 테스트 회원', 'test001@example.com', 'USER');

-- 1) 이미 있으면 (아이디 또는 이메일이 같은 행) 역할·상태·비밀번호 맞춤
UPDATE members m
   SET user_id = a.user_id, role = a.role, status = 'ACTIVE', deleted_at = NULL,
       password_hash = '$2a$10$DoOQZgypqpvYt2FIiYOm7.yJFO16MVmoZiecMOOjqNoEswb4QQtPK',  -- password1!
       updated_at = CURRENT_TIMESTAMP
  FROM acc a
 WHERE m.user_id = a.user_id OR m.email = a.email;

-- 2) 없으면 새로 생성
INSERT INTO members (user_id,name,email,password_hash,role,status,is_agreed_terms,is_agreed_privacy,is_agreed_efinance,is_agreed_marketing)
SELECT a.user_id,a.name,a.email,'$2a$10$DoOQZgypqpvYt2FIiYOm7.yJFO16MVmoZiecMOOjqNoEswb4QQtPK',a.role,'ACTIVE',true,true,true,false
  FROM acc a
 WHERE NOT EXISTS (SELECT 1 FROM members m WHERE m.user_id = a.user_id OR m.email = a.email);

-- 3) 기존 로그인 토큰 폐기 (역할이 바뀌었으니 다시 로그인하도록)
UPDATE refresh_tokens SET revoked = true
 WHERE member_id IN (SELECT id FROM members WHERE user_id IN ('admin','manager','test001'));

COMMIT;

SELECT id, user_id, email, role, status FROM members WHERE user_id IN ('admin','manager','test001') ORDER BY id;

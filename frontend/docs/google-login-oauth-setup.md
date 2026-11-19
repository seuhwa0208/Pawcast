# PAWCAST Google 로그인 OAuth 발급 및 설정 안내

## 1. 목적

PAWCAST에서 Google 로그인을 사용하기 위해 필요한 Google OAuth 2.0 인증정보 발급 방법과 프런트엔드·백엔드 설정 기준을 정리한다.

Google 로그인에는 일반 API Key가 아니라 다음 두 값이 필요하다.

- OAuth 2.0 Client ID
- OAuth 2.0 Client Secret

PAWCAST는 Google 인증 성공 후 Spring Boot가 사용자를 확인하고 PAWCAST 전용 Access JWT와 Refresh Token을 발급하는 구조를 사용한다.

```text
Vue Google 로그인 버튼
  → Spring Boot OAuth 시작 주소
  → Google 로그인·동의
  → Spring Boot OAuth 콜백
  → Google 사용자 정보 확인
  → MEMBERS·OAUTH_ACCOUNTS 조회 또는 등록
  → PAWCAST Access JWT·Refresh Token 발급
  → Vue 로그인 완료 화면으로 이동
```

공식 문서: [Google OpenID Connect 설정](https://developers.google.com/identity/openid-connect/openid-connect)

---

## 2. Google Cloud 프로젝트 준비

1. [Google Cloud Console](https://console.cloud.google.com/)에 접속한다.
2. 팀에서 사용할 Google 계정으로 로그인한다.
3. 화면 상단 프로젝트 선택 메뉴를 연다.
4. `새 프로젝트`를 선택한다.
5. 프로젝트 이름을 입력한다.

권장 프로젝트 이름:

```text
PAWCAST
```

이미 팀 공용 Google Cloud 프로젝트가 있다면 새로 만들지 않고 해당 프로젝트를 사용한다.

---

## 3. Google Auth Platform 설정

Google Cloud Console에서 `Google Auth Platform`으로 이동한다.

### 3.1 Branding

사용자에게 표시할 동의 화면 정보를 입력한다.

| 항목 | 권장 값 |
| --- | --- |
| 앱 이름 | PAWCAST |
| 사용자 지원 이메일 | 팀에서 관리하는 이메일 |
| 앱 로고 | PAWCAST 로고, 선택사항 |
| 홈페이지 | 운영 도메인 확정 후 입력 |
| 개인정보처리방침 | 운영 배포 전 실제 URL 입력 |
| 서비스 약관 | 운영 배포 전 실제 URL 입력 |
| 개발자 연락처 | 팀에서 확인 가능한 이메일 |

### 3.2 Audience

일반 Google 계정으로 로그인하게 하려면 사용자 유형을 `External`로 설정한다.

개발 중에는 게시 상태를 테스트로 유지하고 팀원 Google 계정을 테스트 사용자로 등록한다.

```text
Google Auth Platform
  → Audience
  → Test users
  → Add users
```

테스트 사용자로 등록되지 않은 Google 계정은 테스트 상태의 앱에 로그인하지 못할 수 있다.

### 3.3 Data Access

로그인에 필요한 최소 범위만 요청한다.

```text
openid
profile
email
```

PAWCAST 기본 로그인에는 Gmail, Google Drive, Calendar 등의 추가 권한이 필요하지 않다.

---

## 4. OAuth Client 생성

1. `Google Auth Platform → Clients`로 이동한다.
2. `Create client`를 선택한다.
3. Application type을 `Web application`으로 선택한다.
4. 이름을 입력한다.

권장 이름:

```text
PAWCAST Web Client
```

5. 승인된 리디렉션 URI를 등록한다.
6. 저장 후 발급되는 Client ID와 Client Secret을 확인한다.

공식 발급 안내: [Google 웹 클라이언트 ID 발급](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid)

---

## 5. Redirect URI 등록

현재 PAWCAST는 Vue가 직접 Google 인증 코드를 받지 않고 Spring Boot가 OAuth 콜백을 받는다.

Spring Security 기본 콜백 경로:

```text
{백엔드 주소}/login/oauth2/code/google
```

로컬 백엔드가 `localhost:8080`이면 다음 URI를 등록한다.

```text
http://localhost:8080/login/oauth2/code/google
```

백엔드를 `127.0.0.1:8080`로 실행할 가능성이 있다면 다음 URI도 별도로 등록한다.

```text
http://127.0.0.1:8080/login/oauth2/code/google
```

운영 예시:

```text
https://api.pawcast.example.com/login/oauth2/code/google
```

운영 주소는 실제 백엔드 도메인이 확정된 후 교체한다.

Google에 등록한 Redirect URI와 Spring Boot가 요청에 사용하는 URI는 다음 항목까지 정확히 일치해야 한다.

- `http` 또는 `https`
- 도메인
- 포트
- 경로
- 마지막 슬래시 유무

일치하지 않으면 Google에서 `redirect_uri_mismatch` 오류가 발생한다.

공식 안내: [Google OAuth Redirect URI](https://developers.google.com/identity/protocols/oauth2/web-server)

### Authorized JavaScript origins

현재처럼 Spring Boot가 전체 OAuth 로그인을 처리하는 서버 방식에서는 프런트엔드 JavaScript가 Google API를 직접 호출하지 않는다. 따라서 핵심 설정은 백엔드 Redirect URI다.

향후 Google Identity Services를 Vue에서 직접 사용한다면 다음 Origin을 추가한다.

```text
http://localhost:5174
```

운영 시에는 실제 프런트엔드 Origin을 등록한다.

```text
https://www.pawcast.example.com
```

Origin에는 `/login` 같은 경로를 붙이지 않는다.

---

## 6. 발급 정보 보관

발급 후 다음 두 값을 확인한다.

```text
GOOGLE_CLIENT_ID=발급받은-client-id
GOOGLE_CLIENT_SECRET=발급받은-client-secret
```

### Client ID

- 애플리케이션을 식별하는 값이다.
- 브라우저 기반 Google Identity Services에서는 공개될 수 있다.
- 현재 PAWCAST 구조에서는 Spring Boot 설정에서 관리한다.

### Client Secret

- Google OAuth 서버와 통신할 때 사용하는 비밀 인증정보다.
- Spring Boot 서버 환경변수 또는 시크릿 저장소에만 보관한다.
- Vue 코드에 넣지 않는다.
- `VITE_` 환경변수에 넣지 않는다.
- GitHub에 커밋하지 않는다.
- 메신저나 공개 문서로 전달하지 않는다.

팀원이 설정해야 할 때는 Client Secret 값을 파일에 적어 공유하지 말고, 권한이 있는 담당자가 각 개발 환경의 비공개 환경변수에 직접 설정하는 것을 권장한다.

---

## 7. Spring Boot 설정 예시

`application.yml` 예시:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - openid
              - profile
              - email
```

`application.properties` 예시:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

개발 환경변수 예시:

```text
GOOGLE_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=Google에서_발급받은_비밀값
```

실제 값이 들어간 `.env`, `application-local.yml` 또는 비밀 설정 파일은 `.gitignore`에 포함해야 한다.

---

## 8. PAWCAST 프런트엔드 연결

현재 프런트엔드는 로그인 버튼을 누르면 Spring Boot OAuth 시작 주소로 이동하도록 구성되어 있다.

```text
GET {VITE_SPRING_BASE_URL}/api/oauth2/authorization/google
```

로컬 예시:

```text
http://localhost:8080/api/oauth2/authorization/google
```

Vue는 Google Client Secret을 사용하지 않는다.

인증 성공 후 권장 흐름:

```text
Spring Boot Google OAuth 콜백 처리
  → PAWCAST 회원·소셜 계정 확인
  → PAWCAST JWT 생성
  → 프런트엔드 /oauth/callback 이동
  → Vue가 PAWCAST 로그인 상태 저장
```

---

## 9. 백엔드 처리 기준

Google에서 받은 이메일만으로 계정을 식별하면 안 된다.

Google ID Token의 `sub` 값을 Google 사용자 고유 식별자로 사용한다.

`OAUTH_ACCOUNTS` 권장 저장 구조:

```text
provider = GOOGLE
provider_user_id = Google ID Token의 sub
provider_email = Google에서 받은 이메일
member_id = 연결된 PAWCAST 회원 ID
```

권장 고유 제약조건:

```text
UNIQUE(provider, provider_user_id)
```

처리 순서:

```text
1. Google 인증 코드 수신
2. Google 토큰 교환
3. ID Token 서명·iss·aud·exp 검증
4. Google sub 확인
5. OAUTH_ACCOUNTS에서 GOOGLE + sub 조회
6. 연결 계정이면 기존 PAWCAST 회원 로그인
7. 신규 계정이면 필수 정보·약관 동의 화면으로 이동
8. 회원 및 OAUTH_ACCOUNTS 저장
9. PAWCAST Access JWT·Refresh Token 발급
```

Google 로그인 성공 자체를 PAWCAST 가입 필수정보와 약관 동의 완료로 간주하면 안 된다.

---

## 10. 로컬 테스트 절차

1. Spring Boot에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`을 설정한다.
2. Spring Boot를 Google Redirect URI에 등록한 포트로 실행한다.
3. Vue의 `VITE_SPRING_BASE_URL`을 백엔드 주소로 설정한다.
4. Google Auth Platform의 테스트 사용자에 로그인할 Google 계정을 추가한다.
5. PAWCAST 로그인 화면에서 Google 로그인 버튼을 누른다.
6. Google 로그인·동의 화면을 완료한다.
7. Spring Boot OAuth 콜백이 정상 호출되는지 확인한다.
8. 신규 사용자는 추가정보·약관 동의 화면으로 이동하는지 확인한다.
9. 기존 연결 사용자는 PAWCAST JWT 발급 후 로그인되는지 확인한다.

---

## 11. 자주 발생하는 오류

### `redirect_uri_mismatch`

- Google Console Redirect URI와 Spring Boot 요청 URI가 다르다.
- `localhost`와 `127.0.0.1`은 서로 다른 주소다.
- 포트나 마지막 `/`가 다른지 확인한다.

### `access_blocked`

- 앱이 테스트 상태인데 로그인 계정이 테스트 사용자에 등록되지 않았을 수 있다.
- Google Auth Platform의 Audience와 Test users를 확인한다.

### `invalid_client`

- Client ID 또는 Client Secret이 잘못되었다.
- 다른 Google Cloud 프로젝트에서 발급한 값을 섞어 사용했는지 확인한다.

### 로그인 후 PAWCAST 계정을 찾지 못함

- Google 이메일이 아니라 `provider=GOOGLE`, `provider_user_id=sub` 조합으로 조회하는지 확인한다.
- 신규 사용자 추가정보·약관 동의 분기가 구현되어 있는지 확인한다.

---

## 12. 팀 체크리스트

### Google Cloud 담당자

- [ ] PAWCAST Google Cloud 프로젝트 생성 또는 선택
- [ ] Branding 정보 입력
- [ ] Audience를 External로 설정
- [ ] 팀원 Google 계정을 테스트 사용자로 등록
- [ ] Web application OAuth Client 생성
- [ ] 로컬·운영 Redirect URI 등록
- [ ] Client ID와 Client Secret을 안전하게 보관

### 백엔드 담당자

- [ ] Spring Security OAuth2 Client 의존성 확인
- [ ] Client ID·Secret 환경변수 설정
- [ ] Google `sub` 기준 계정 조회
- [ ] 신규 사용자 추가정보·약관 동의 처리
- [ ] PAWCAST Access JWT·Refresh Token 발급
- [ ] OAuth 실패·취소 처리

### 프런트엔드 담당자

- [ ] `VITE_SPRING_BASE_URL` 확인
- [ ] Google 로그인 버튼에서 백엔드 OAuth 시작 주소 호출
- [ ] 신규 사용자 추가정보 화면 처리
- [ ] OAuth 성공 후 PAWCAST JWT 저장
- [ ] 로그인 실패·취소 안내


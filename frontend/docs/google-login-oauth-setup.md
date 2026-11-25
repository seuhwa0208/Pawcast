# Google 로그인 개발 설정

## 인증 구조

브라우저의 Google Identity Services(GIS)가 로그인 후 Google ID credential을 발급합니다. Vue는 이를 Spring의 `POST /api/auth/google`에 전달하고, Spring은 Google 서명·만료·audience를 검증합니다. 기존 회원은 POWCAST Access/Refresh JWT를 받고, 신규 회원 및 기존 이메일 계정은 기존 가입/연결 절차를 이어갑니다.

```text
Vue GIS 버튼 → Google ID credential → Spring POST /api/auth/google
  → Google ID token 검증 → 회원 조회/가입/연결 → POWCAST JWT 응답
```

이 구조는 OAuth authorization-code 교환을 사용하지 않으므로 Google Client Secret, Spring OAuth callback URL, `exchangeOAuthCode`가 필요하지 않습니다. Google ID token은 주소창이나 URL query에 넣지 않습니다.

## Google Cloud Console 설정

1. OAuth consent screen을 설정하고 로그인 대상 테스트 사용자를 추가합니다.
2. OAuth Client ID를 `Web application` 타입으로 생성합니다.
3. **Authorized JavaScript origins**에 브라우저에서 실제 접속할 origin을 정확히 추가합니다.

```text
http://localhost:5174
http://127.0.0.1:5174
```

이 둘은 서로 다른 origin이므로, 사용할 주소만 등록하거나 둘 다 등록해야 합니다. Origin에는 경로(`/login`)를 붙이지 않습니다. GIS에서 403이 계속되면 현재 주소창의 origin, OAuth Client ID, 게시/테스트 사용자 상태를 확인하세요.

로그인에는 `openid`, `profile`, `email` 정보가 사용됩니다. Authorized redirect URI는 이 GIS ID-token 방식에 필요하지 않습니다.

## 환경 변수

`frontend/.env`:

```dotenv
VITE_GOOGLE_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
VITE_SPRING_BASE_URL=http://localhost:8080
```

`backend/.env`:

```dotenv
GOOGLE_CLIENT_ID=xxxxxxxxxxxx.apps.googleusercontent.com
```

Google Cloud에서 발급된 Client ID는 프론트엔드와 Spring에 같은 값으로 설정해야 합니다. Spring의 `oauth.google.client-id`가 Google ID token의 audience를 검증합니다. `GOOGLE_CLIENT_SECRET`은 필요하지 않으며 설정하지 않습니다.

## 로그인 후 세션

Spring 응답의 `accessToken`, `refreshToken`, `role`, `userId`는 일반 로그인과 같은 저장·라우팅 경로를 사용합니다. 새 회원은 Google ID token을 가입 완료 시까지 앱 메모리의 pending OAuth 상태로 보유하며, 계정 연결도 같은 검증된 credential을 사용합니다. Refresh token은 Spring DB에 저장되고 일반 API 또는 FastAPI에서 Access Token이 만료되면 기존 단일 refresh 요청이 회전시킵니다.

# Pawcast 반려동물 용품 지능형 쇼핑몰

> 반려동물 정보를 기반으로 상품을 관리하고, AI 분석 서비스를 제공하는 팀 프로젝트입니다.

## 목차

1. [프로젝트 소개](#1-프로젝트-소개)
2. [주요 기능](#2-주요-기능)
3. [기술 스택 및 4-Tier 구조](#3-기술-스택-및-4-tier-구조)
4. [프로젝트 구성](#4-프로젝트-구성)
5. [Docker Hub 이미지로 실행하기](#5-docker-hub-이미지로-실행하기)
   - [1단계 — Docker Desktop 설치](#1단계--docker-desktop-설치)
   - [2단계 — 실행 파일 준비](#2단계--실행-파일-준비)
   - [3단계 — 환경변수 설정](#3단계--환경변수-설정)
   - [4단계 — Docker Compose 실행](#4단계--docker-compose-실행)
   - [5단계 — 접속 및 실행 확인](#5단계--접속-및-실행-확인)
6. [컨테이너 및 데이터 관리](#6-컨테이너-및-데이터-관리)
7. [GitHub 소스코드로 실행하기](#7-github-소스코드로-실행하기)
8. [자주 발생하는 오류](#8-자주-발생하는-오류)

---


## 1. 프로젝트 소개

Pawcast는 반려동물 용품을 판매하고 관리하는 지능형 쇼핑몰입니다. 
Vue 프론트엔드가 사용자 화면을 제공하고, Spring Boot 백엔드가 회원·상품·주문 등 일반 쇼핑몰 업무를 처리합니다. 
FastAPI AI 서비스는 반려동물 관련 AI 분석 및 추천 데이터를 생성하며, PostgreSQL은 서비스 데이터를 저장합니다.

Docker 배포에서는 아래 네 개의 서비스가 각각 컨테이너로 실행됩니다.

- **Frontend:** 사용자에게 웹 화면을 제공하고 Nginx를 통해 API 요청을 전달합니다.
- **Backend:** 회원, 인증, 상품, 장바구니, 주문, 결제, 게시판 등 일반 서비스 API를 처리합니다.
- **AI Service:** 반려동물 기반 분석 및 추천 등 AI API를 처리합니다.
- **Database:** Spring Boot와 FastAPI가 함께 사용하는 PostgreSQL 데이터베이스입니다.


## 2. 주요 기능

| 영역 | 주요 기능 |
|---|---|
| 회원 및 인증 | 회원가입, 로그인, Google 로그인, 회원 정보 관리 |
| 반려동물 | 반려동물 등록 및 관리, 반려동물 정보 기반 서비스 |
| 상품 | 상품 목록·상세 조회, 카테고리, 상품 정보 관리 |
| 쇼핑 | 장바구니, 위시리스트, 주문 및 결제 |
| 배송 및 환불 | 주문 상태 관리, 배송 처리, 환불 |
| 게시판 | 공지사항, Q&A, 상품 리뷰 |
| 관리자 | 회원·상품·주문·게시판 등 운영 관리 |
| AI 서비스 | 반려동물 정보를 활용한 분석 및 추천 데이터 생성·조회 |




## 3. 기술 스택 및 4-Tier 구조

| 구분 | 기술 | 버전 | 역할 |
|---|---|---|---|
| Frontend | Vue 3, JavaScript, Vite, Tailwind CSS, Nginx | Vite 6.0.5, Nginx 1.30.8 | 사용자 화면 및 API 요청 전달 |
| Backend | Java, Spring Boot, Spring Data JPA, Spring Security | Java 21, Spring Boot 3.5.1 | 일반 쇼핑몰 API 및 비즈니스 로직 |
| AI Service | Python, FastAPI | Python 3.12, FastAPI 0.165.0 | AI 분석 및 추천 API |
| Database | PostgreSQL | PostgreSQL 16 | 서비스 데이터 저장 |
| Infrastructure | Docker | Docker 29.8.0 | 서비스 컨테이너화 및 배포 |


### 3-1. 전체 요청 흐름

```text
사용자 브라우저
      |
      v
Frontend (Vue + Nginx)
      |
      +---- /api/* ----> Backend (Spring Boot)
      |                       |
      |                       +---- 일반 업무 데이터 조회/저장 ----+
      |                                                          |
      +---- /ai/* -----> AI Service (FastAPI)                    |
                              |                                  |
                              +---- AI 데이터 조회/저장 ---------+
                                                                 |
                                                                 v
                                                        Database (PostgreSQL)
```


### 3-2. API 라우팅

| 브라우저 요청 경로 | Nginx 전달 대상 | 설명 |
|---|---|---|
| `/` | Frontend 정적 파일 | Vue 화면 |
| `/api/` | Backend 컨테이너의 8080 포트 | Spring Boot API |
| `/ai/` | FastAPI 컨테이너의 8000 포트 | AI API |

프론트엔드는 일반 쇼핑몰 API를 `/api` 경로로 Spring Boot에 요청하고, AI 기능은 `/ai` 경로로 FastAPI에 요청합니다. Spring Boot와 FastAPI는 서로를 직접 호출하는 구조가 아니라 각각 PostgreSQL에 연결하는 구조입니다.

> Nginx의 `proxy_pass` 설정에 따라 `/api/` 또는 `/ai/` 접두 경로가 백엔드에 전달되는 방식이 달라질 수 있습니다. 배포에 사용하는 `nginx.conf`의 설정과 Spring/FastAPI의 실제 라우터 경로를 함께 확인해야 합니다.


## 4. 프로젝트 구성

저장소의 주요 디렉터리는 다음과 같습니다. 실제 파일명은 배포 브랜치의 저장소 구성을 기준으로 확인하세요.

```text
pawcast/
├── frontend/                       # Vue 3 프론트엔드
│   ├── .env.production              # 프론트엔드 배포 환경변수
│   ├── Dockerfile
│   └── nginx.conf
│
├── backend/                        # Spring Boot 백엔드
│   ├── application.yaml            # Spring Boot 설정 파일
│   ├── .env                        # 백엔드 환경변수
│   └── Dockerfile
│
├── ai/                             # FastAPI AI 서비스
│   ├── requirements.txt            # Python 패키지 의존성
│   ├── .env                        # AI 서비스 환경변수
│   └── Dockerfile
│
├── shared/                         # 서비스 간 공유 파일
│   └── export-schemas/
│       └── sales.json
│
├── pawcast_init_final.sql          # PostgreSQL 초기화 SQL
└── docker-compose.yml              # Docker 4개 서비스 실행 구성
```


## 5. Docker Hub 이미지로 실행하기

이 방법은 소스코드를 직접 빌드하지 않고 Docker Hub에 게시된 이미지를 내려받아 실행하는 방법입니다. 
실행 PC에는 Docker Desktop과 Docker Compose가 필요합니다. 
Git, JDK, Node.js, Python, PostgreSQL을 별도로 설치할 필요는 없습니다.


### 1단계 — Docker Desktop 설치

1. [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/)에서 설치 파일을 내려받아 설치합니다.
2. 필요한 경우 WSL 2 기능을 활성화하고 PC를 재시작합니다.
3. Docker Desktop을 실행하고 엔진이 정상적으로 시작될 때까지 기다립니다.


### 2단계 — 실행 파일 준비

프로젝트 배포용 `docker-compose.yml` 파일을 준비합니다. Docker Hub 이미지를 사용하는 PC에는 애플리케이션 소스코드가 없어도 되지만, Compose 파일과 환경변수 파일은 필요합니다.

CMD에서 Compose 파일이 있는 폴더로 이동합니다.

```bat
cd /d "%USERPROFILE%\Downloads\pawcast-deploy"
dir
```

`docker-compose.yml`이 표시되는지 확인합니다.


### 3단계 — 환경변수 설정

Compose 파일에서 참조하는 환경변수 파일을 준비합니다. 프로젝트에서 사용하는 실제 환경변수 이름은 `backend/.env`, `ai/.env` 및 Compose 파일의 `env_file`/`environment` 설정과 일치해야 합니다.

일반적으로 다음과 같은 값이 필요할 수 있습니다.

| 구분 | 설정 내용 | 주의 |
|---|---|---|
| PostgreSQL | DB 이름, 사용자명, 비밀번호 | 모든 서비스의 DB 접속 정보가 일치해야 함 |
| Spring Boot | DB 접속 URL, JWT 비밀키, OAuth·결제 관련 설정 등 | 실제 프로젝트 설정 키를 사용 |
| FastAPI | DB 접속 정보, AI 모델·서비스 설정 등 | 실제 프로젝트 설정 키를 사용 |
| Frontend 빌드 | `/api`, `/ai` API 기본 경로 및 외부 서비스 공개 키 | Vite 환경변수는 빌드 시점에 반영될 수 있음 |


**중요한 보안 사항**

- 실제 비밀번호, JWT Secret, OAuth Secret, 결제 Secret Key 등을 README나 GitHub에 올리지 마세요.
- `.env` 파일은 프로젝트에서 요구하는 이름과 위치에 맞춰 별도로 전달하거나 각 실행 PC에서 작성해야 합니다.
- 환경변수 파일이 없으면 Compose 실행 중 `env_file` 오류가 발생할 수 있습니다.
- `VITE_`로 시작하는 값은 프론트엔드 번들에 포함되어 브라우저에 노출될 수 있습니다. 비밀키를 넣지 마세요.
- Google OAuth는 승인된 JavaScript 원본/리디렉션 URI 등 Google Cloud Console 설정도 배포 도메인 또는 테스트 주소와 일치해야 합니다.


### 4단계 — Docker Compose 실행

Docker Desktop이 실행 중인지 확인한 다음, Compose 파일이 있는 폴더에서 실행합니다.

```bat
docker compose pull
docker compose up -d
```

- `docker compose pull`: Compose 파일에 지정된 이미지를 Docker Hub에서 내려받습니다.
- `docker compose up -d`: DB, Backend, FastAPI, Frontend 컨테이너를 백그라운드로 실행합니다.
- Compose 파일에 `build:`가 포함되어 있으면 로컬 빌드가 실행될 수 있습니다. 이미지 배포만 사용할 경우에는 이미지가 지정되어 있고 `build:`가 실행에 영향을 주지 않는지 확인하세요.

실행 상태를 확인합니다.

```bat
docker compose ps
```

로그를 확인합니다.

```bat
docker compose logs -f
```

특정 서비스만 확인하려면 다음처럼 실행합니다.

```bat
docker compose logs -f backend
docker compose logs -f fastapi
docker compose logs -f db
docker compose logs -f frontend
```

로그 확인을 종료하려면 `Ctrl + C`를 누릅니다. 로그 확인만 종료되며 백그라운드 컨테이너는 계속 실행됩니다.


### 5단계 — 접속 및 실행 확인

브라우저에서 다음 주소로 접속합니다.

| 항목 | 주소 |
|---|---|
| 쇼핑몰 웹 화면 | `http://localhost/` |
| Spring Boot API | `http://localhost/api/` |
| FastAPI 서비스 | `http://localhost/ai/` |

API의 루트 경로는 별도 화면을 제공하지 않을 수 있습니다. 따라서 `/api/` 또는 `/ai/` 접속 결과가 404라는 이유만으로 서비스 전체가 실패했다고 판단하지 말고, 프로젝트에 실제 구현된 API 경로와 컨테이너 로그를 확인하세요.


## 6. 컨테이너 및 데이터 관리

### 컨테이너 중지

```bat
docker compose stop
```


### 중지된 컨테이너 다시 시작

```bat
docker compose start
```


### 컨테이너 종료 및 제거

```bat
docker compose down
```


### 이미지 최신 버전 다시 받기

```bat
docker compose pull
docker compose up -d
```


### DB 데이터 보존

PostgreSQL 데이터는 Compose 파일에서 지정한 named volume에 저장되어야 합니다. 컨테이너를 삭제하거나 다시 만들어도 named volume을 유지하면 DB 데이터가 보존됩니다.

```bat
docker volume ls
```

**주의:** 아래 명령은 Compose에 연결된 DB 볼륨까지 삭제할 수 있으므로 데이터가 필요하다면 실행하지 마세요.

```bat
docker compose down -v
```


## 7. GitHub 소스코드로 실행하기

소스코드를 직접 수정하거나 각 서비스를 개발 모드로 실행하려는 경우에 사용하는 방법입니다.


### 1단계 — 프로그램 설치

| 프로그램 | 용도 |
|---|---|
| Git | GitHub 저장소 다운로드 |
| JDK 21 | Spring Boot 실행 및 빌드 |
| Node.js LTS | Vue / Vite 실행 |
| Python | FastAPI 실행 |
| PostgreSQL 16 또는 Docker Desktop | 데이터베이스 실행 |


### 2단계 — 저장소 다운로드

```bat
git clone https://github.com/seuhwa0208/pawcast.git
cd pawcast
```


### 3단계 — 데이터베이스 준비

PostgreSQL 16을 준비하고 프로젝트에서 사용하는 DB 이름, 사용자명, 비밀번호를 설정합니다. 초기 테이블 및 기준 데이터는 저장소에 포함된 최종 초기화 SQL 파일을 확인해 적용합니다.

예시:

```sql
CREATE DATABASE pawcast;
```

초기화 SQL은 데이터베이스에 접속한 뒤 프로젝트의 최종 SQL 파일을 실행합니다. 기존 데이터가 있는 DB에 초기화 스크립트를 실행하면 데이터가 변경되거나 삭제될 수 있으므로 반드시 백업 후 진행하세요.


### 4단계 — Backend 실행

```bat
cd backend
```

필요한 `.env` 또는 `application.yaml` 설정을 준비한 다음 Gradle Wrapper로 실행합니다.

Windows:

```bat
gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

Docker 네트워크 내부에서 DB에 접속할 때는 DB 서비스 이름(예: `db`)을 사용하고, PC에서 직접 실행할 때는 로컬 DB 주소를 사용합니다.


### 5단계 — AI Service 실행

새 터미널에서 `ai` 폴더로 이동해 Python 가상환경을 만들고 의존성을 설치합니다. 아래는 일반적인 실행 예시이며, 실제 의존성 파일과 실행 모듈명은 저장소 구성을 따릅니다.

```bat
cd ai
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

FastAPI 실행 명령은 프로젝트의 실제 애플리케이션 모듈에 맞춰 사용합니다. 예를 들어 진입점이 `main.py`라면 다음과 같습니다.

```bat
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```


### 6단계 — Frontend 실행

새 터미널에서 프론트엔드 폴더로 이동합니다.

```bat
cd frontend
npm install
npm run dev
```

터미널에 출력되는 Vite 접속 주소를 브라우저에서 엽니다. 개발 환경에서는 프론트엔드 API 프록시 및 환경변수 설정이 현재 실행 중인 Backend와 FastAPI 주소를 가리키는지 확인해야 합니다.


## 8. 자주 발생하는 오류

| 현상 | 확인 및 해결 방법 |
|---|---|
| `docker` 명령을 찾을 수 없음 | Docker Desktop 설치 여부를 확인하고 CMD를 새로 엽니다. |
| Docker 엔진 연결 오류 | Docker Desktop이 실행되고 엔진이 준비됐는지 확인합니다. |
| `docker compose` 구성 파일을 찾지 못함 | `docker-compose.yml`이 있는 폴더에서 명령을 실행했는지 확인합니다. |
| `pull access denied` 또는 이미지 다운로드 실패 | 이미지 이름·태그가 정확한지, Docker Hub 저장소가 공개 상태인지 확인합니다. 비공개 이미지라면 `docker login`이 필요합니다. |
| `env_file` 파일을 찾을 수 없음 | Compose에 적힌 상대 경로에 `.env` 파일이 존재하는지 확인합니다. |
| 컨테이너가 실행 직후 종료됨 | `docker compose ps -a`와 `docker compose logs <서비스명>`으로 원인을 확인합니다. |
| Backend가 DB에 연결하지 못함 | 컨테이너 내부 DB 주소가 `localhost`가 아니라 Compose의 DB 서비스명(예: `db`)인지, 포트·DB명·계정·비밀번호가 일치하는지 확인합니다. |
| FastAPI가 DB에 연결하지 못함 | FastAPI의 DB 접속 환경변수와 DB 서비스 상태를 확인합니다. |
| 웹 화면은 나오지만 API 요청 실패 | Nginx의 `/api/`, `/ai/` 프록시 대상·포트·경로와 백엔드 라우터 경로를 확인합니다. |
| 로그인 실패 또는 Google 로그인 실패 | 브라우저 개발자 도구의 Network/Console, Backend 로그, OAuth Client ID, 승인된 도메인·리디렉션 URI 및 쿠키 설정을 확인합니다. |
| CORS 오류 | 실제 브라우저 Origin과 Spring Security CORS 허용 Origin, 쿠키 사용 시 credentials 설정을 확인합니다. `*`와 credentials를 함께 사용할 수 없습니다. |
| 401 오류 | 토큰 누락·만료, 로그인 상태 및 Authorization 헤더를 확인합니다. |
| 403 오류 | 로그인 여부뿐 아니라 사용자 역할, 리소스 소유권, Spring Security 인가 규칙을 확인합니다. 모든 403이 CORS 문제인 것은 아닙니다. |
| 404 오류 | 요청 URL과 Nginx 경로 전달, Spring/FastAPI 라우터 경로가 일치하는지 확인합니다. |
| 409 오류 | 현재 데이터 상태와 요청한 작업의 선행 조건 또는 중복 여부를 확인합니다. |
| 500 오류 | Backend/FastAPI 로그에서 실제 예외와 DB 제약조건 오류를 확인합니다. |
| 포트 충돌 | `80`, `8080`, `8000`, `5432` 포트를 다른 프로그램이 사용 중인지 확인하고 Compose 포트 매핑을 조정합니다. |
| 초기 데이터가 적용되지 않음 | PostgreSQL 공식 이미지의 초기화 스크립트는 데이터 디렉터리가 비어 있을 때 최초 초기화 과정에서 실행됩니다. 기존 볼륨에는 파일을 추가해도 자동 재실행되지 않습니다. |

---


## 참고 및 배포 전 확인 사항

아래 항목은 배포 환경에 맞게 최종 확인한 뒤 사용하세요.

- [ ] Docker Hub에 게시된 Frontend, Backend, AI 이미지 이름과 태그가 Compose 파일과 일치하는가?
- [ ] PostgreSQL은 공식 `postgres:16` 이미지 또는 팀에서 관리하는 DB 이미지로 실행되는가?
- [ ] DB 초기화 SQL이 최초 실행 시 올바른 경로로 마운트되는가?
- [ ] Backend와 FastAPI의 DB 접속 주소가 Compose 서비스명으로 설정되어 있는가?
- [ ] `frontend/.env.production`의 API 기본 경로가 배포 Nginx 설정과 일치하는가?
- [ ] Nginx가 `/api/`는 Backend로, `/ai/`는 FastAPI로 전달하는가?
- [ ] OAuth, JWT, 결제 관련 비밀값이 이미지나 Git 저장소에 노출되지 않았는가?
- [ ] DB 볼륨을 유지하고 백업·복원 방법을 준비했는가?


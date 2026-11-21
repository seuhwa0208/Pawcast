from pathlib import Path
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from app.api.router import api_router
from app.common.database import dispose_database


@asynccontextmanager
async def lifespan(_: FastAPI):
    """Release pooled PostgreSQL connections on application shutdown."""
    yield
    await dispose_database()

app = FastAPI(
    title="PAWCAST AI Review Analyzer",
    description="스프링 부트 연동을 위한 AI 감정 분석 및 욕설 필터링 서버",
    version="1.0.0",
    lifespan=lifespan,
)

# HTML 파일 직접 실행 및 로컬 개발 서버에서의 테스트만 허용합니다.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["null"],
    allow_origin_regex=r"https?://(127\.0\.0\.1|localhost)(:\d+)?",
    allow_methods=["GET", "POST", "OPTIONS"],
    # Vue의 JWT와 Spring의 서버 간 내부 키를 모두 허용합니다.
    allow_headers=["Authorization", "Content-Type", "Internal-Api-Key"],
)

# AI 도메인 라우터 등록
app.include_router(api_router)

TEST_PAGE = (
    Path(__file__).resolve().parent
    / "static"
    / "review"
    / "review_test.html"
)


@app.get("/test", include_in_schema=False)
def review_test_page() -> FileResponse:
    """브라우저에서 실제 욕설 필터와 감성 분석 API를 시험합니다."""
    return FileResponse(TEST_PAGE)


@app.get("/")
def root():
    return {
        "message": "PAWCAST AI 분석 서버가 정상적으로 실행 중입니다!",
        "docs_url": "/docs",
    }

from fastapi import FastAPI
from app.api.review_api import router as review_router

app = FastAPI(
    title="POWCAST AI Review Analyzer",
    description="스프링 부트 연동을 위한 AI 감정 분석 및 욕설 필터링 서버",
    version="1.0.0",
)

# 라우터 등록
app.include_router(review_router)


@app.get("/")
def root():
    return {
        "message": "POWCAST AI 분석 서버가 정상적으로 실행 중입니다!",
        "docs_url": "/docs",
    }
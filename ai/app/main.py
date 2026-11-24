from fastapi import FastAPI

from app.core.config import settings
from app.routers.predict import router as predict_router


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    debug=settings.DEBUG,
)
app.include_router(predict_router)


@app.get("/", tags=["health"])
def health_check() -> dict[str, str]:
    return {
        "status": "ok",
        "service": settings.APP_NAME,
        "docs": "/docs",
    }

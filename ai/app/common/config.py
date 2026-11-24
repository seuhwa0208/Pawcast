"""기존 도메인 모듈이 공유하는 설정 호환 모듈입니다."""

from app.core.config import PROJECT_ROOT, Settings, settings

__all__ = ["PROJECT_ROOT", "Settings", "settings"]

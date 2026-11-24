"""관리자 JWT 또는 Spring 내부 API 키를 검증합니다."""

import hmac
from typing import Annotated

import jwt
from fastapi import Header, HTTPException, status

from app.common.config import settings


async def require_admin(
    authorization: Annotated[str | None, Header()] = None,
    internal_api_key: Annotated[
        str | None,
        Header(alias="Internal-Api-Key"),
    ] = None,
) -> dict:
    # Spring 서버 간 요청은 브라우저 JWT 대신 공유 내부 키로 인증합니다.
    if (
        settings.INTERNAL_API_KEY
        and internal_api_key
        and hmac.compare_digest(internal_api_key, settings.INTERNAL_API_KEY)
    ):
        return {"type": "internal", "role": "SYSTEM"}

    # Vue 브라우저 요청은 Spring이 발급한 관리자 JWT로 인증합니다.
    if not settings.JWT_SECRET:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI 서버의 JWT_SECRET 설정이 필요합니다.",
        )
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="관리자 로그인이 필요합니다.",
        )
    token = authorization.removeprefix("Bearer ").strip()
    try:
        claims = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=["HS256"],
            issuer=settings.JWT_ISSUER,
        )
    except jwt.PyJWTError as error:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효하지 않거나 만료된 로그인 토큰입니다.",
        ) from error
    if claims.get("type") != "access" or claims.get("role") not in {"ADMIN", "MANAGER"}:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="관리자 권한이 필요합니다.",
        )
    return claims

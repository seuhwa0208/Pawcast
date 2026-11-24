"""관리자·매니저 JWT와 Spring 내부 API 키를 검증합니다."""

import hmac
from typing import Annotated

import jwt
from fastapi import Header, HTTPException, status

from app.common.config import settings


async def _require_roles(
    authorization: str | None,
    internal_api_key: str | None,
    allowed_roles: set[str],
) -> dict:
    """공통 검증으로 JWT 서명·issuer·토큰종류와 역할을 일관되게 확인합니다."""
    # 신뢰된 Spring 서버 간 호출은 브라우저 역할 토큰 대신 공유 키를 사용합니다.
    if (
        settings.INTERNAL_API_KEY
        and internal_api_key
        and hmac.compare_digest(internal_api_key, settings.INTERNAL_API_KEY)
    ):
        return {"type": "internal", "role": "SYSTEM"}

    if not settings.JWT_SECRET:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI 서버의 JWT_SECRET 설정이 필요합니다.",
        )
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="로그인이 필요합니다.",
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

    if claims.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Access Token이 필요합니다.",
        )

    # Spring은 DB enum 기준 MANAGER를 발급하지만, ROLE_MANAGER 표기도 안전하게 해석합니다.
    role = claims.get("role")
    if isinstance(role, str) and role.startswith("ROLE_"):
        role = role.removeprefix("ROLE_")
    if role not in allowed_roles:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="요청한 기능을 사용할 권한이 없습니다.",
        )

    return claims


async def require_admin(
    authorization: Annotated[str | None, Header()] = None,
    internal_api_key: Annotated[
        str | None,
        Header(alias="Internal-Api-Key"),
    ] = None,
) -> dict:
    """회원 감성 통계와 재학습 등 관리자 전용 기능을 보호합니다."""
    return await _require_roles(authorization, internal_api_key, {"ADMIN"})


async def require_manager(
    authorization: Annotated[str | None, Header()] = None,
    internal_api_key: Annotated[
        str | None,
        Header(alias="Internal-Api-Key"),
    ] = None,
) -> dict:
    """매출·재고 분석 조회를 관리자와 매니저에게 허용합니다."""
    return await _require_roles(
        authorization,
        internal_api_key,
        {"ADMIN", "MANAGER"},
    )

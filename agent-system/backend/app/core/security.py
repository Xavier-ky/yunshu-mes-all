from __future__ import annotations

from dataclasses import dataclass

import jwt
from fastapi import Header, HTTPException

from app.core.config import settings


@dataclass(frozen=True)
class Principal:
    user_id: str
    roles: tuple[str, ...]
    token: str | None

    @property
    def primary_role(self) -> str:
        return self.roles[0] if self.roles else "TESTER"


def require_principal(authorization: str | None = Header(default=None)) -> Principal:
    if not authorization or not authorization.startswith("Bearer "):
        if settings.companion_jwt_enforce:
            raise HTTPException(status_code=401, detail="缺少 MES 登录凭证")
        return Principal(user_id="guest", roles=("TESTER",), token=None)

    token = authorization[7:].strip()
    if not token:
        raise HTTPException(status_code=401, detail="MES 登录凭证无效")

    try:
        # JJWT chooses the strongest HMAC variant compatible with the configured key.
        # Accept the standard HMAC family so this service stays compatible with MES tokens.
        claims = jwt.decode(token, settings.jwt_secret, algorithms=["HS256", "HS384", "HS512"])
    except jwt.PyJWTError as error:
        if settings.companion_jwt_enforce:
            raise HTTPException(status_code=401, detail="MES 登录凭证无效") from error
        return Principal(user_id="guest", roles=("TESTER",), token=None)

    user_id = str(claims.get("sub") or claims.get("userId") or "").strip()
    raw_roles = claims.get("roles") or claims.get("roleCodes") or []
    roles = tuple(str(role) for role in raw_roles) if isinstance(raw_roles, list) else (str(raw_roles),)
    if not user_id:
        raise HTTPException(status_code=401, detail="MES 登录凭证缺少用户标识")
    return Principal(user_id=user_id, roles=roles or ("TESTER",), token=token)

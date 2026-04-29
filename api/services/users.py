from sqlmodel import Session, select, or_
from passlib.hash import pbkdf2_sha256
from fastapi import HTTPException

from CRUD.users import (
    create_user,
    touch_last_login,
    validate_unique_email,
    validate_unique_username,
)
from models.users import Usuarios
from schemas.users import UserCreate, UserResponse


class UsuariosService:
    def register_user(self, db: Session, user_info: UserCreate) -> UserResponse:
        validation = self.available_email_username(db, user_info)
        if not validation["status"]:
            raise HTTPException(status_code=400, detail=validation["msg"])

        user_info.password = pbkdf2_sha256.hash(user_info.password)
        new_user = create_user(db, user_info)
        return UserResponse.model_validate(new_user)

    def login_user(self, db: Session, user_info: UserCreate) -> UserResponse:
        posible_user = db.exec(
            select(Usuarios).where(
                or_(
                    Usuarios.email == user_info.email,
                    Usuarios.nombre_usuario == user_info.nombre_usuario,
                )
            )
        ).first()
        if posible_user and pbkdf2_sha256.verify(user_info.password, posible_user.password):
            touch_last_login(db, posible_user)
            return UserResponse.model_validate(posible_user)
        raise HTTPException(status_code=401, detail="Credenciales no válidas")

    def available_email_username(self, db: Session, user_info: UserCreate) -> dict:
        if validate_unique_email(db, str(user_info.email)):
            return {"msg": "email already in use", "status": False}
        if validate_unique_username(db, user_info.nombre_usuario):
            return {"msg": "username already in use", "status": False}
        return {"msg": "ok", "status": True}

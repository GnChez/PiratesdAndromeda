from fastapi import APIRouter, Depends
from sqlmodel import Session
from db.dbconnection import get_session
from services.users import UsuariosService
from schemas.users import *
from schemas.rooms import HabitacionBase

router = APIRouter()


@router.post("/register", response_model=UserResponse, tags=["USERS"])
def create_game(user_info: UserCreate,db: Session = Depends(get_session)):
    usuarios_service = UsuariosService()
    return usuarios_service.register_user(db=db,user_info=user_info)

@router.post("/login", response_model=UserResponse, tags=["USERS"])
def join_game(join_req: UserCreate,db: Session = Depends(get_session)):
    usuarios_service = UsuariosService()
    return usuarios_service.login_user(db=db,user_info=join_req)


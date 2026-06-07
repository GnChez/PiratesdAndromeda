from dotenv import load_dotenv
import os

from sqlalchemy.engine.url import make_url
from sqlmodel import SQLModel, Session, create_engine

load_dotenv()
# En producción debe definirse (Docker Compose). Sin ella, SQLite en memoria evita fallar al importar (tests locales).
DATABASE_URL_RAW = (os.environ.get("DATABASE_URL") or "").strip() or "sqlite:///:memory:"


def _sync_database_url(url: str) -> str:
    """
    Session, create_all y get_session son síncronos; el driver debe ser pymysql (no aiomysql/asyncmy).
    Si el DSN usa un driver asyncio, SQLAlchemy intenta await en código sync → MissingGreenlet.
    """
    url = url.strip()
    if not url or url.startswith("sqlite"):
        return url or "sqlite:///:memory:"
    try:
        u = make_url(url)
    except Exception:
        return url
    dn = u.drivername
    if dn.endswith("+aiomysql"):
        return str(u.set(drivername=dn.removesuffix("+aiomysql") + "+pymysql"))
    if dn.endswith("+asyncmy"):
        return str(u.set(drivername=dn.removesuffix("+asyncmy") + "+pymysql"))
    return url


DATABASE_URL = _sync_database_url(DATABASE_URL_RAW)
engine = create_engine(DATABASE_URL, pool_pre_ping=True)


def init_db():
    SQLModel.metadata.create_all(engine)
    from db.seed import seed_catalog

    with Session(engine) as session:
        seed_catalog(session)


def get_session():
    with Session(engine) as session:
        yield session

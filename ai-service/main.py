from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, File, Form, HTTPException, Query, UploadFile
from fastapi.middleware.cors import CORSMiddleware

from gallery import PlayerGallery
from model_manager import ModelManager
from recognizer import PlayerRecognizer, decode_uploaded_image

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger("lpl_ai.main")

models = ModelManager()
gallery = PlayerGallery(models)
recognizer = PlayerRecognizer(models, gallery)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    logger.info("Initializing LPL player recognition models and gallery...")
    count = gallery.load_or_build(force_rebuild=False)
    logger.info("AI service ready: %d LPL players indexed (OCR ready=%s).", count, models.ocr_ready)
    yield


app = FastAPI(
    title="LPL Vault Player Recognition Service",
    description="Dual-path LPL player recognizer using YuNet + SFace face embeddings and RapidOCR.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "playerCount": len(gallery.entries),
        "faceModel": "OpenCV YuNet + SFace (128D)",
        "ocrEnabled": models.ocr_ready,
    }


@app.get("/gallery")
def list_gallery():
    return {
        "count": len(gallery.entries),
        "players": gallery.list_meta(),
    }


@app.post("/recognize")
async def recognize(
    file: UploadFile = File(...),
    top_k: int = Query(default=3, ge=1, le=10),
    use_ocr: bool = Query(default=True),
):
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="上传文件为空")
    if len(content) > 10 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="图片大小不能超过 10MB")

    try:
        return recognizer.recognize_bytes(content, top_k=top_k, use_ocr=use_ocr)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/gallery/rebuild")
def rebuild_gallery():
    count = gallery.rebuild()
    return {"status": "rebuilt", "playerCount": count, "players": gallery.list_meta()}


@app.post("/gallery/register")
async def register_player_sample(
    team: str = Form(...),
    playerName: str = Form(...),
    imgUrl: Optional[str] = Form(default=None),
    file: UploadFile = File(...),
):
    content = await file.read()
    try:
        bgr = decode_uploaded_image(content)
        key = (imgUrl or f"{team.strip().upper()}-{playerName.strip().upper()}").strip()
        entry = gallery.register_sample(team=team, player_name=playerName, img_url=key, bgr_img=bgr)
        return {"status": "registered", "player": entry.to_meta()}
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

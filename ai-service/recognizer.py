from __future__ import annotations

import io
import re
import time
from typing import Any, Dict, List, Optional

import cv2
import numpy as np
from PIL import Image, ImageOps

from gallery import PlayerGallery
from model_manager import ModelManager

# OpenCV SFace recommended same-person cosine threshold is 0.363
FACE_MATCH_THRESHOLD = 0.36
MAX_IMAGE_SIDE = 1280


def decode_uploaded_image(image_bytes: bytes) -> np.ndarray:
    """Decode arbitrary uploaded image bytes with EXIF orientation correction into BGR."""
    if not image_bytes:
        raise ValueError("上传图片内容为空")
    try:
        pil_img = Image.open(io.BytesIO(image_bytes))
        pil_img = ImageOps.exif_transpose(pil_img)
        if pil_img.mode != "RGB":
            pil_img = pil_img.convert("RGB")
        rgb = np.array(pil_img)
        bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    except Exception as exc:
        raise ValueError(f"无法解析图片文件，请上传常见 PNG / JPG / WebP 格式：{exc}") from exc

    h, w = bgr.shape[:2]
    max_side = max(h, w)
    if max_side > MAX_IMAGE_SIDE:
        scale = MAX_IMAGE_SIDE / float(max_side)
        bgr = cv2.resize(bgr, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)
    return bgr


def _match_ocr_for_player(ocr_texts: List[str], team: str, player_name: str) -> tuple[bool, float]:
    """
    Match OCR lines against team and playerName while preventing short-name false positives
    (e.g. 'ON' inside 'SEASON' or 'CHAMPION', 'ALE' inside 'SALE').
    Returns (ocr_matched, bonus_score).
    """
    if not ocr_texts:
        return False, 0.0

    joined_raw = " ".join(ocr_texts).upper()
    tokens = set(re.findall(r"[A-Z0-9]+", joined_raw))
    compact = re.sub(r"[^A-Z0-9]", "", joined_raw)

    t = team.upper()
    p = player_name.upper()

    # Exact team + player combo (e.g. BLG.BIN, BLG.ON, AL.ALE, TES.JACKEYLOVE)
    if f"{t}{p}" in compact:
        return True, 0.28

    # Standalone token match for player name
    if len(p) >= 4:
        if p in tokens or p in compact:
            bonus = 0.28 if t in tokens else 0.20
            return True, bonus
    elif len(p) == 3:
        # 3-char IDs like BIN, ELK, WEI, ALE -> require exact word token (or team also present)
        if p in tokens:
            bonus = 0.26 if t in tokens else 0.18
            return True, bonus
    else:
        # 2-char IDs like ON -> require team token present alongside standalone token
        if p in tokens and t in tokens:
            return True, 0.26

    return False, 0.0


def _score_to_percent(face_score: float, ocr_matched: bool, combined_score: float) -> int:
    """Convert raw cosine + OCR fusion score into an intuitive 0-99% confidence value."""
    if ocr_matched and face_score >= FACE_MATCH_THRESHOLD:
        pct = 75 + min(24, int((face_score - FACE_MATCH_THRESHOLD) / 0.50 * 24) + 10)
        return max(75, min(99, pct))
    if face_score >= FACE_MATCH_THRESHOLD:
        # Map [0.36, 0.85] -> [62%, 98%]
        ratio = min(1.0, max(0.0, (face_score - FACE_MATCH_THRESHOLD) / 0.49))
        return int(round(62 + ratio * 36))
    if ocr_matched:
        return 74
    return int(round(max(0.0, min(0.55, combined_score)) * 100))


class PlayerRecognizer:
    """Dual-path LPL player recognizer: YuNet + SFace face embedding + RapidOCR text fusion."""

    def __init__(self, models: ModelManager, gallery: PlayerGallery) -> None:
        self.models = models
        self.gallery = gallery

    def recognize_bgr(
        self, bgr_img: np.ndarray, top_k: int = 3, use_ocr: bool = True
    ) -> Dict[str, Any]:
        t0 = time.perf_counter()

        if not self.gallery.entries:
            return {
                "recognized": False,
                "message": "选手特征底库为空，请先构建底库索引",
                "faceDetected": False,
                "faceBox": None,
                "ocrTexts": [],
                "matches": [],
                "elapsedMs": int((time.perf_counter() - t0) * 1000),
            }

        query_vec, face_box, det_conf = self.models.detect_and_extract_face(bgr_img)
        face_detected = query_vec is not None
        ocr_texts = self.models.extract_ocr_texts(bgr_img) if use_ocr else []

        candidates: List[Dict[str, Any]] = []
        for entry in self.gallery.entries.values():
            best_face_score = 0.0
            if query_vec is not None and entry.embeddings:
                sims = [float(np.dot(query_vec, emb)) for emb in entry.embeddings]
                best_face_score = max(sims)

            ocr_matched, ocr_bonus = _match_ocr_for_player(
                ocr_texts, entry.team, entry.player_name
            )
            combined = min(1.0, max(0.0, best_face_score) + ocr_bonus)

            reasons: List[str] = []
            if best_face_score >= FACE_MATCH_THRESHOLD:
                reasons.append("人脸特征匹配")
            if ocr_matched:
                reasons.append("卡面文字命中")
            if not reasons and face_detected:
                reasons.append("人脸特征相近")

            candidates.append(
                {
                    "imgUrl": entry.img_url,
                    "team": entry.team,
                    "playerName": entry.player_name,
                    "score": round(combined, 4),
                    "faceScore": round(max(0.0, best_face_score), 4),
                    "ocrMatched": ocr_matched,
                    "confidencePercent": _score_to_percent(
                        best_face_score, ocr_matched, combined
                    ),
                    "matchReasons": reasons,
                }
            )

        candidates.sort(key=lambda c: (c["score"], c["faceScore"]), reverse=True)
        top_matches = candidates[: max(1, min(top_k, len(candidates)))]

        best = top_matches[0] if top_matches else None
        recognized = False
        if best is not None:
            if best["faceScore"] >= FACE_MATCH_THRESHOLD:
                recognized = True
            elif best["ocrMatched"] and (not face_detected or best["faceScore"] >= 0.22):
                recognized = True

        if recognized and best is not None:
            message = f"识别为 {best['team']} · {best['playerName']}"
        elif not face_detected and not ocr_texts:
            message = "未在图片中检测到清晰人脸或可识别的卡面文字，请上传包含选手正面清晰头像的照片或选手卡"
        else:
            message = "未匹配到库中已收录的 16 位 LPL 选手，请尝试上传更清晰的正面照"

        elapsed_ms = int((time.perf_counter() - t0) * 1000)
        return {
            "recognized": recognized,
            "message": message,
            "faceDetected": face_detected,
            "faceConfidence": round(det_conf, 4) if face_detected else 0.0,
            "faceBox": face_box,
            "ocrTexts": ocr_texts[:12],
            "matches": top_matches if (face_detected or any(m["ocrMatched"] for m in top_matches)) else [],
            "elapsedMs": elapsed_ms,
        }

    def recognize_bytes(
        self, image_bytes: bytes, top_k: int = 3, use_ocr: bool = True
    ) -> Dict[str, Any]:
        bgr_img = decode_uploaded_image(image_bytes)
        return self.recognize_bgr(bgr_img, top_k=top_k, use_ocr=use_ocr)

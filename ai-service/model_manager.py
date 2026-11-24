from __future__ import annotations

import logging
import os
import threading
import urllib.request
from pathlib import Path
from typing import List, Optional, Tuple

import cv2
import numpy as np

logger = logging.getLogger("lpl_ai.models")

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

YUNET_FILENAME = "face_detection_yunet_2023mar.onnx"
SFACE_FILENAME = "face_recognition_sface_2021dec.onnx"

YUNET_URL = (
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_detection_yunet/face_detection_yunet_2023mar.onnx"
)
SFACE_URL = (
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_recognition_sface/face_recognition_sface_2021dec.onnx"
)


def _download_file(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_suffix(dest.suffix + ".part")
    logger.info("Downloading model %s -> %s", url, dest)
    req = urllib.request.Request(url, headers={"User-Agent": "LPL-Vault-AI/1.0"})
    with urllib.request.urlopen(req, timeout=120) as resp, open(tmp, "wb") as out:
        while True:
            chunk = resp.read(1024 * 1024)
            if not chunk:
                break
            out.write(chunk)
    tmp.replace(dest)
    logger.info("Downloaded %s (%d bytes)", dest.name, dest.stat().st_size)


class ModelManager:
    """Manages YuNet face detector, SFace face recognizer, and RapidOCR engine."""

    def __init__(self, models_dir: Path = MODELS_DIR) -> None:
        self.models_dir = models_dir
        self.yunet_path = models_dir / YUNET_FILENAME
        self.sface_path = models_dir / SFACE_FILENAME

        self._lock = threading.Lock()
        self._detector: Optional[cv2.FaceDetectorYN] = None
        self._detector_relaxed: Optional[cv2.FaceDetectorYN] = None
        self._recognizer: Optional[cv2.FaceRecognizerSF] = None
        self._ocr_engine = None
        self._ocr_initialized = False
        self._ocr_available = False

    def ensure_models(self) -> None:
        if not self.yunet_path.exists() or self.yunet_path.stat().st_size < 10_000:
            _download_file(YUNET_URL, self.yunet_path)
        if not self.sface_path.exists() or self.sface_path.stat().st_size < 1_000_000:
            _download_file(SFACE_URL, self.sface_path)

        if self._detector is None or self._detector_relaxed is None:
            yunet_bytes = self.yunet_path.read_bytes()
            if self._detector is None:
                self._detector = cv2.FaceDetectorYN.create(
                    "onnx", yunet_bytes, b"", (320, 320), 0.60, 0.30, 5000
                )
            if self._detector_relaxed is None:
                self._detector_relaxed = cv2.FaceDetectorYN.create(
                    "onnx", yunet_bytes, b"", (320, 320), 0.42, 0.30, 5000
                )
        if self._recognizer is None:
            sface_bytes = self.sface_path.read_bytes()
            self._recognizer = cv2.FaceRecognizerSF.create("onnx", sface_bytes, b"")

    def _ensure_ocr(self) -> None:
        if self._ocr_initialized:
            return
        self._ocr_initialized = True
        if os.environ.get("LPL_DISABLE_OCR", "0") == "1":
            self._ocr_available = False
            return
        try:
            from rapidocr import RapidOCR  # type: ignore

            self._ocr_engine = RapidOCR()
            self._ocr_available = True
            logger.info("RapidOCR (v2/v3) initialized successfully.")
        except Exception as exc_v3:
            try:
                from rapidocr_onnxruntime import RapidOCR as LegacyRapidOCR  # type: ignore

                self._ocr_engine = LegacyRapidOCR()
                self._ocr_available = True
                logger.info("rapidocr_onnxruntime initialized successfully.")
            except Exception as exc_v1:
                logger.warning("RapidOCR unavailable (%s / %s); running face-only mode.", exc_v3, exc_v1)
                self._ocr_available = False

    @property
    def ocr_ready(self) -> bool:
        self._ensure_ocr()
        return self._ocr_available

    def detect_and_extract_face(
        self, bgr_img: np.ndarray
    ) -> Tuple[Optional[np.ndarray], Optional[List[float]], float]:
        """
        Detect the primary face in a BGR image and extract its normalized 128D SFace feature.
        Returns (embedding_128d, normalized_bbox_xywh, det_confidence).
        """
        self.ensure_models()
        assert self._detector is not None
        assert self._detector_relaxed is not None
        assert self._recognizer is not None

        h, w = bgr_img.shape[:2]
        if h < 20 or w < 20:
            return None, None, 0.0

        # Upscale very small crops (e.g. tight avatar thumbnails) so YuNet sees enough pixels
        scale = 1.0
        work_img = bgr_img
        if min(h, w) < 160:
            scale = 200.0 / min(h, w)
            work_img = cv2.resize(
                bgr_img, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC
            )

        wh, ww = work_img.shape[:2]

        with self._lock:
            self._detector.setInputSize((ww, wh))
            _, faces = self._detector.detect(work_img)
            if faces is None or len(faces) == 0:
                self._detector_relaxed.setInputSize((ww, wh))
                _, faces = self._detector_relaxed.detect(work_img)

            if faces is None or len(faces) == 0:
                return None, None, 0.0

            # Pick primary face weighted by area * confidence
            best_face = max(
                faces,
                key=lambda f: max(1.0, float(f[2])) * max(1.0, float(f[3])) * float(f[-1]),
            )

            aligned = self._recognizer.alignCrop(work_img, best_face)
            feat = self._recognizer.feature(aligned)
            vec = feat.flatten().astype(np.float32)
            norm = float(np.linalg.norm(vec))
            if norm > 0:
                vec = vec / norm

            bx, by, bw, bh = [float(v) for v in best_face[:4]]
            norm_box = [
                max(0.0, min(1.0, bx / ww)),
                max(0.0, min(1.0, by / wh)),
                max(0.0, min(1.0, bw / ww)),
                max(0.0, min(1.0, bh / wh)),
            ]
            det_score = float(best_face[-1])
            return vec, norm_box, det_score

    def extract_ocr_texts(self, bgr_img: np.ndarray) -> List[str]:
        """Run RapidOCR and return recognized text lines."""
        self._ensure_ocr()
        if not self._ocr_available or self._ocr_engine is None:
            return []

        try:
            with self._lock:
                result = self._ocr_engine(bgr_img)
        except Exception as exc:
            logger.warning("OCR inference error: %s", exc)
            return []

        texts: List[str] = []
        # RapidOCR 2.x/3.x returns a RapidOCROutput object with `.txts`
        if hasattr(result, "txts") and result.txts:
            for t in result.txts:
                if t and str(t).strip():
                    texts.append(str(t).strip())
            return texts

        # Legacy rapidocr_onnxruntime returns (result_list, elapse)
        if isinstance(result, tuple) and result and result[0]:
            for item in result[0]:
                if isinstance(item, (list, tuple)) and len(item) >= 2 and item[1]:
                    texts.append(str(item[1]).strip())
        return texts

from __future__ import annotations

import json
import logging
import os
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional

import cv2
import numpy as np

from model_manager import BASE_DIR, ModelManager

logger = logging.getLogger("lpl_ai.gallery")

DEFAULT_CARDS_DIR = (
    BASE_DIR.parent / "src" / "main" / "resources" / "static" / "lpl卡片"
)
DATA_DIR = BASE_DIR / "data"
INDEX_NPZ = DATA_DIR / "gallery_index.npz"
META_JSON = DATA_DIR / "gallery_meta.json"

IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".webp"}


def read_image_unicode(path: Path) -> Optional[np.ndarray]:
    """Windows-safe image reader supporting CJK/Unicode paths."""
    try:
        raw = np.fromfile(str(path), dtype=np.uint8)
        if raw.size == 0:
            return None
        return cv2.imdecode(raw, cv2.IMREAD_COLOR)
    except Exception as exc:
        logger.warning("Failed reading %s: %s", path, exc)
        return None


def crop_card_artwork(bgr_img: np.ndarray) -> np.ndarray:
    """
    Strip the ~20% white screenshot bar at the bottom and ~5% dark strip on the right
    present in the original LPL card PNGs (~296x497).
    """
    h, w = bgr_img.shape[:2]
    # Only apply screenshot-strip crop when aspect ratio matches the original ~0.59 tall card screenshot
    if w > 0 and (h / float(w)) > 1.55:
        return bgr_img[: int(h * 0.80), : int(w * 0.95)]
    return bgr_img


def parse_card_filename(stem: str) -> tuple[str, str]:
    """
    Parse filenames like 'BLG-BIN' or 'WBG-CRISP1' into (team, playerName).
    Matches LocalDataInitializer.java naming convention.
    """
    if "-" in stem:
        team, raw_player = stem.split("-", 1)
    else:
        team, raw_player = "LPL", stem
    player_name = re.sub(r"\d+$", "", raw_player).strip().upper()
    return team.strip().upper(), player_name


@dataclass
class PlayerEntry:
    img_url: str
    team: str
    player_name: str
    embeddings: List[np.ndarray] = field(default_factory=list)

    def to_meta(self) -> Dict[str, object]:
        return {
            "imgUrl": self.img_url,
            "team": self.team,
            "playerName": self.player_name,
            "sampleCount": len(self.embeddings),
        }


class PlayerGallery:
    """Stores normalized face embeddings for all LPL players in the catalog."""

    def __init__(self, models: ModelManager, cards_dir: Optional[Path] = None) -> None:
        self.models = models
        env_dir = os.environ.get("LPL_CARDS_DIR")
        self.cards_dir = Path(env_dir) if env_dir else (cards_dir or DEFAULT_CARDS_DIR)
        self.entries: Dict[str, PlayerEntry] = {}

    def _compute_dir_fingerprint(self) -> List[str]:
        if not self.cards_dir.exists():
            return []
        files = sorted(
            p for p in self.cards_dir.iterdir() if p.is_file() and p.suffix.lower() in IMAGE_EXTS
        )
        return [f"{p.name}:{p.stat().st_size}:{int(p.stat().st_mtime)}" for p in files]

    def load_or_build(self, force_rebuild: bool = False) -> int:
        self.models.ensure_models()
        fingerprint = self._compute_dir_fingerprint()

        if not force_rebuild and INDEX_NPZ.exists() and META_JSON.exists():
            try:
                meta = json.loads(META_JSON.read_text(encoding="utf-8"))
                if meta.get("fingerprint") == fingerprint and meta.get("entries"):
                    npz = np.load(INDEX_NPZ)
                    loaded: Dict[str, PlayerEntry] = {}
                    for item in meta["entries"]:
                        key = item["imgUrl"]
                        arr = npz[key]
                        vecs = [arr[i].astype(np.float32) for i in range(arr.shape[0])]
                        loaded[key] = PlayerEntry(
                            img_url=key,
                            team=item["team"],
                            player_name=item["playerName"],
                            embeddings=vecs,
                        )
                    self.entries = loaded
                    logger.info("Loaded %d players from cached gallery index.", len(self.entries))
                    return len(self.entries)
            except Exception as exc:
                logger.warning("Failed loading cached gallery, rebuilding: %s", exc)

        return self.rebuild()

    def rebuild(self) -> int:
        self.models.ensure_models()
        if not self.cards_dir.exists():
            raise FileNotFoundError(f"LPL cards directory not found: {self.cards_dir}")

        files = sorted(
            p for p in self.cards_dir.iterdir() if p.is_file() and p.suffix.lower() in IMAGE_EXTS
        )
        new_entries: Dict[str, PlayerEntry] = {}

        for img_path in files:
            img = read_image_unicode(img_path)
            if img is None:
                logger.warning("Skipping unreadable card image: %s", img_path.name)
                continue

            card_art = crop_card_artwork(img)
            vec, _, det_score = self.models.detect_and_extract_face(card_art)
            if vec is None:
                logger.warning("No face detected in gallery image: %s", img_path.name)
                continue

            team, player_name = parse_card_filename(img_path.stem)
            entry = PlayerEntry(
                img_url=img_path.stem,
                team=team,
                player_name=player_name,
                embeddings=[vec],
            )
            new_entries[entry.img_url] = entry
            logger.info(
                "Indexed %s (%s · %s) face_conf=%.3f",
                entry.img_url,
                entry.team,
                entry.player_name,
                det_score,
            )

        self.entries = new_entries
        self._save_cache()
        return len(self.entries)

    def register_sample(
        self, team: str, player_name: str, img_url: str, bgr_img: np.ndarray
    ) -> PlayerEntry:
        """Append a new face embedding to an existing player or register a new player."""
        vec, _, _ = self.models.detect_and_extract_face(bgr_img)
        if vec is None:
            raise ValueError("未能从上传图片中检测到清晰人脸，请更换正面清晰照")

        key = img_url.strip()
        if key in self.entries:
            self.entries[key].embeddings.append(vec)
        else:
            self.entries[key] = PlayerEntry(
                img_url=key,
                team=team.strip().upper(),
                player_name=player_name.strip().upper(),
                embeddings=[vec],
            )
        self._save_cache()
        return self.entries[key]

    def _save_cache(self) -> None:
        DATA_DIR.mkdir(parents=True, exist_ok=True)
        arrays = {
            key: np.stack(entry.embeddings, axis=0).astype(np.float32)
            for key, entry in self.entries.items()
            if entry.embeddings
        }
        if arrays:
            np.savez_compressed(INDEX_NPZ, **arrays)

        meta_payload = {
            "fingerprint": self._compute_dir_fingerprint(),
            "entries": [entry.to_meta() for entry in self.entries.values()],
        }
        META_JSON.write_text(
            json.dumps(meta_payload, ensure_ascii=False, indent=2), encoding="utf-8"
        )

    def list_meta(self) -> List[Dict[str, object]]:
        return [entry.to_meta() for entry in self.entries.values()]

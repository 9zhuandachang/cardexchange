"""
Automated accuracy & robustness test suite for LPL Player Recognition Service.
Tests:
  1. 16/16 gallery indexing coverage
  2. Face-only cropped + perturbed recognition (NO text/card borders, OCR disabled)
  3. Full card tilted + brightness-shifted + JPEG compressed dual-path recognition
  4. Negative sample rejection (blank / random noise image)
"""
from __future__ import annotations

import sys
from pathlib import Path

import cv2
import numpy as np

from gallery import PlayerGallery, crop_card_artwork, parse_card_filename, read_image_unicode
from model_manager import ModelManager
from recognizer import PlayerRecognizer


def simulate_photo_perturbation(bgr: np.ndarray, angle_deg: float, brightness: float, jpeg_q: int) -> np.ndarray:
    h, w = bgr.shape[:2]
    mat = cv2.getRotationMatrix2D((w / 2.0, h / 2.0), angle_deg, 0.96)
    rotated = cv2.warpAffine(bgr, mat, (w, h), borderMode=cv2.BORDER_REPLICATE)
    adjusted = np.clip(rotated.astype(np.float32) * brightness + 6.0, 0, 255).astype(np.uint8)
    ok, buf = cv2.imencode(".jpg", adjusted, [int(cv2.IMWRITE_JPEG_QUALITY), jpeg_q])
    assert ok
    return cv2.imdecode(buf, cv2.IMREAD_COLOR)


def main() -> int:
    models = ModelManager()
    gallery = PlayerGallery(models)
    count = gallery.load_or_build(force_rebuild=True)
    print(f"[1/4] Gallery indexed: {count} players (expected 16)")
    assert count == 16, f"Expected 16 indexed players, got {count}"

    recognizer = PlayerRecognizer(models, gallery)
    card_files = sorted(gallery.cards_dir.glob("*.png"))
    assert len(card_files) == 16

    # Test 2: Pure face-only crop + perturbation (OCR disabled)
    face_only_hits = 0
    for path in card_files:
        img = crop_card_artwork(read_image_unicode(path))
        _, box, _ = models.detect_and_extract_face(img)
        assert box is not None, f"Face missing in {path.name}"
        h, w = img.shape[:2]
        x1 = max(0, int((box[0] - box[2] * 0.25) * w))
        y1 = max(0, int((box[1] - box[3] * 0.25) * h))
        x2 = min(w, int((box[0] + box[2] * 1.25) * w))
        y2 = min(h, int((box[1] + box[3] * 1.25) * h))
        face_crop = img[y1:y2, x1:x2]
        perturbed = simulate_photo_perturbation(face_crop, angle_deg=6.0, brightness=1.08, jpeg_q=72)
        res = recognizer.recognize_bgr(perturbed, top_k=3, use_ocr=False)
        top = res["matches"][0] if res["matches"] else None
        expected_team, expected_player = parse_card_filename(path.stem)
        ok = bool(res["recognized"] and top and top["playerName"] == expected_player and top["team"] == expected_team)
        if ok:
            face_only_hits += 1
        else:
            print(f"  [MISS face-only] {path.name} -> got {top}")
    print(f"[2/4] Face-only perturbed Top-1 accuracy (OCR OFF): {face_only_hits}/16")
    assert face_only_hits == 16, f"Face-only accuracy below 16/16: {face_only_hits}/16"

    # Test 3: Full card tilted + compressed dual-path (Face + OCR)
    dual_hits = 0
    ocr_hit_count = 0
    for path in card_files:
        img = crop_card_artwork(read_image_unicode(path))
        perturbed = simulate_photo_perturbation(img, angle_deg=-7.0, brightness=0.92, jpeg_q=75)
        res = recognizer.recognize_bgr(perturbed, top_k=3, use_ocr=True)
        top = res["matches"][0] if res["matches"] else None
        expected_team, expected_player = parse_card_filename(path.stem)
        ok = bool(res["recognized"] and top and top["playerName"] == expected_player and top["team"] == expected_team)
        if ok:
            dual_hits += 1
            if top["ocrMatched"]:
                ocr_hit_count += 1
        else:
            print(f"  [MISS dual-path] {path.name} -> got {top}")
    print(f"[3/4] Dual-path tilted card Top-1 accuracy: {dual_hits}/16 (OCR assisted: {ocr_hit_count}/16)")
    assert dual_hits == 16, f"Dual-path accuracy below 16/16: {dual_hits}/16"

    # Test 4: Negative sample (no face, no text)
    noise_img = np.full((360, 480, 3), 235, dtype=np.uint8)
    cv2.circle(noise_img, (240, 180), 60, (180, 195, 200), -1)
    neg_res = recognizer.recognize_bgr(noise_img, top_k=3, use_ocr=True)
    print(f"[4/4] Negative sample recognized={neg_res['recognized']} msg='{neg_res['message']}'")
    assert neg_res["recognized"] is False, "Negative sample should not be recognized!"

    print("ALL AI RECOGNITION TESTS PASSED.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

"""Local 1:1 face verifier for the MES tester account.

The previous DeepFace/FaceNet implementation loaded TensorFlow in the shared
Windows Python runtime.  On this machine that native runtime repeatedly
crashed inside an NVIDIA graphics-driver DLL.  This service therefore uses the
CPU-only OpenCV SFace recognition model instead.  SFace is also a recognition
model exposed by DeepFace, but direct OpenCV inference avoids loading
TensorFlow entirely.  Spring Boot remains the sole JWT issuer.
"""

from __future__ import annotations

import base64
import binascii
import logging
from pathlib import Path
from typing import Any

import cv2
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


ROOT = Path(__file__).resolve().parents[1]
ENROLLMENT_DIR = ROOT / "data" / "sry_face_data"
MODEL_NAME = "OpenCV SFace (DeepFace-compatible)"
MODEL_WEIGHTS_PATH = ROOT / "data" / "models" / "face_recognition_sface_2021dec.onnx"
SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
# This is a cosine-distance ceiling: a larger number is more tolerant.  The
# value was calibrated to be forgiving of normal webcam lighting while still
# keeping a clear margin from unrelated faces.
VERIFICATION_THRESHOLD = 0.46
ENROLLMENT_EMBEDDINGS: list[np.ndarray] = []
RECOGNIZER: Any | None = None
FACE_CASCADE: Any | None = None
LOGGER = logging.getLogger("uvicorn.error")

# Force OpenCV's own CPU path.  This avoids the NVIDIA/OpenCL driver path that
# was crashing the former TensorFlow process on this Windows installation.
cv2.ocl.setUseOpenCL(False)
cv2.setNumThreads(1)

app = FastAPI(title="Yunshu MES Face Verification", version="0.2.0")


class FaceVerifyRequest(BaseModel):
    image_base64: str = Field(min_length=64, max_length=3_000_000)


def enrollment_images() -> list[Path]:
    if not ENROLLMENT_DIR.is_dir():
        return []
    return sorted(
        path
        for path in ENROLLMENT_DIR.iterdir()
        if path.is_file() and path.suffix.lower() in SUPPORTED_EXTENSIONS
    )


def decode_camera_frame(image_base64: str) -> np.ndarray:
    payload = image_base64.strip()
    if "," in payload:
        payload = payload.split(",", 1)[1]
    try:
        raw = base64.b64decode(payload, validate=True)
    except (ValueError, binascii.Error) as exc:
        raise HTTPException(status_code=422, detail="Invalid camera image.") from exc
    image = cv2.imdecode(np.frombuffer(raw, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None or image.size == 0:
        raise HTTPException(status_code=422, detail="Camera image cannot be decoded.")
    return image


def read_enrollment_image(path: Path) -> np.ndarray | None:
    """Read Unicode-named enrollment files safely on Windows."""
    try:
        raw = np.fromfile(str(path), dtype=np.uint8)
        image = cv2.imdecode(raw, cv2.IMREAD_COLOR)
    except (OSError, ValueError):
        return None
    return image if image is not None and image.size else None


def initialise_models() -> None:
    global RECOGNIZER, FACE_CASCADE
    if not MODEL_WEIGHTS_PATH.is_file():
        raise FileNotFoundError(f"SFace model is missing: {MODEL_WEIGHTS_PATH}")
    RECOGNIZER = cv2.FaceRecognizerSF.create(
        str(MODEL_WEIGHTS_PATH),
        "",
        cv2.dnn.DNN_BACKEND_OPENCV,
        cv2.dnn.DNN_TARGET_CPU,
    )
    cascade_path = Path(cv2.data.haarcascades) / "haarcascade_frontalface_default.xml"
    FACE_CASCADE = cv2.CascadeClassifier(str(cascade_path))
    if FACE_CASCADE.empty():
        raise RuntimeError("OpenCV Haar face detector is unavailable.")


def crop_largest_face(image: np.ndarray) -> np.ndarray | None:
    if FACE_CASCADE is None:
        return None
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = FACE_CASCADE.detectMultiScale(
        gray,
        scaleFactor=1.10,
        minNeighbors=5,
        minSize=(72, 72),
    )
    if len(faces) == 0:
        return None
    x, y, width, height = max(faces, key=lambda face: int(face[2]) * int(face[3]))
    padding = int(max(width, height) * 0.18)
    x0, y0 = max(0, x - padding), max(0, y - padding)
    x1 = min(image.shape[1], x + width + padding)
    y1 = min(image.shape[0], y + height + padding)
    face = image[y0:y1, x0:x1]
    if face.size == 0:
        return None
    return cv2.resize(face, (112, 112), interpolation=cv2.INTER_AREA)


def extract_embedding(image: np.ndarray) -> np.ndarray | None:
    if RECOGNIZER is None:
        return None
    face = crop_largest_face(image)
    if face is None:
        return None
    try:
        embedding = RECOGNIZER.feature(face)
    except cv2.error:
        return None
    vector = np.asarray(embedding, dtype=np.float32).reshape(-1)
    norm = float(np.linalg.norm(vector))
    return vector / norm if norm > 0 else None


def cosine_distance(left: np.ndarray, right: np.ndarray) -> float:
    return max(0.0, 1.0 - float(np.dot(left, right)))


def warm_enrollment_embeddings() -> None:
    """Build the four enrollment vectors once, outside the login path."""
    ENROLLMENT_EMBEDDINGS.clear()
    for reference in enrollment_images():
        image = read_enrollment_image(reference)
        if image is None:
            continue
        embedding = extract_embedding(image)
        if embedding is not None:
            ENROLLMENT_EMBEDDINGS.append(embedding)


@app.on_event("startup")
def prepare_enrollment_cache() -> None:
    initialise_models()
    warm_enrollment_embeddings()
    if not ENROLLMENT_EMBEDDINGS:
        raise RuntimeError("No usable tester enrollment faces were detected.")
    LOGGER.info(
        "Face-auth ready: model=%s, enrollment embeddings=%d, threshold=%.2f",
        MODEL_NAME,
        len(ENROLLMENT_EMBEDDINGS),
        VERIFICATION_THRESHOLD,
    )


def verify_against_tester(probe: np.ndarray) -> dict[str, Any]:
    if not enrollment_images() or not ENROLLMENT_EMBEDDINGS:
        raise HTTPException(status_code=503, detail="Tester face enrollment is unavailable.")

    probe_embedding = extract_embedding(probe)
    if probe_embedding is None:
        return {"verified": False, "reason": "NO_FACE_DETECTED"}

    best_distance = min(
        cosine_distance(probe_embedding, reference)
        for reference in ENROLLMENT_EMBEDDINGS
    )
    if best_distance > VERIFICATION_THRESHOLD:
        LOGGER.info(
            "Face verification rejected: distance=%.4f threshold=%.2f",
            best_distance,
            VERIFICATION_THRESHOLD,
        )
        return {
            "verified": False,
            "reason": "FACE_NOT_MATCHED",
            "distance": best_distance,
            "threshold": VERIFICATION_THRESHOLD,
        }

    LOGGER.info(
        "Face verification accepted: distance=%.4f threshold=%.2f",
        best_distance,
        VERIFICATION_THRESHOLD,
    )
    return {
        "verified": True,
        "distance": best_distance,
        "threshold": VERIFICATION_THRESHOLD,
    }


@app.get("/api/face/health")
def health() -> dict[str, Any]:
    ready = bool(ENROLLMENT_EMBEDDINGS) and RECOGNIZER is not None
    return {
        "status": "UP" if ready else "NOT_READY",
        "enrollment_count": len(enrollment_images()),
        "model": MODEL_NAME,
        "model_weights_ready": MODEL_WEIGHTS_PATH.is_file(),
        "embedding_cache_count": len(ENROLLMENT_EMBEDDINGS),
        "threshold": VERIFICATION_THRESHOLD,
    }


@app.post("/api/face/verify")
def verify(request: FaceVerifyRequest) -> dict[str, Any]:
    """Verify one in-memory browser frame; probe images are never persisted."""
    return verify_against_tester(decode_camera_frame(request.image_base64))

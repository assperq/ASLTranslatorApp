import numpy as np
from types import SimpleNamespace

from nnmodel import process_frame_for_tflite


class FakeLandmark:
    def __init__(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z


class FakeHandLandmarks:
    def __init__(self, landmarks):
        self.landmark = landmarks


class FakeResults:
    def __init__(self, landmarks):
        self.multi_hand_landmarks = landmarks


class FakeMediaPipeHands:
    def process(self, frame):
        landmarks = [
            FakeLandmark(i * 0.1, i * 0.1, i * 0.1)
            for i in range(21)
        ]

        return FakeResults([
            FakeHandLandmarks(landmarks)
        ])


def test_process_frame_returns_210_features():
    frame = np.zeros((480, 640, 3), dtype=np.uint8)

    result = process_frame_for_tflite(
        frame,
        FakeMediaPipeHands()
    )

    assert result is not None
    assert result.shape == (1, 210)


def test_process_frame_returns_float32():
    frame = np.zeros((480, 640, 3), dtype=np.uint8)

    result = process_frame_for_tflite(
        frame,
        FakeMediaPipeHands()
    )

    assert result.dtype == np.float32
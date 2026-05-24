import numpy as np

from nnmodel import process_frame_for_tflite


class LM:
    def __init__(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z


class HandLandmarks:
    def __init__(self, landmarks):
        self.landmark = landmarks


class Results:
    def __init__(self, multi_hand_landmarks):
        self.multi_hand_landmarks = multi_hand_landmarks


class FakeHands:
    def __init__(self, results):
        self._results = results

    def process(self, _img_rgb):
        return self._results


def test_process_frame_returns_210_features_when_hand_present():
    landmarks = [LM(float(i), float(i + 1), float(i + 2)) for i in range(21)]
    results = Results([HandLandmarks(landmarks)])
    hands = FakeHands(results)
    frame = np.zeros((32, 32, 3), dtype=np.uint8)

    out = process_frame_for_tflite(frame, hands)

    assert out is not None
    assert out.shape == (1, 210)


def test_process_frame_returns_none_when_no_hand():
    hands = FakeHands(Results(None))
    frame = np.zeros((32, 32, 3), dtype=np.uint8)

    out = process_frame_for_tflite(frame, hands)

    assert out is None

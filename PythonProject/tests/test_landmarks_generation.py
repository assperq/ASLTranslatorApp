import tempfile
from pathlib import Path

import cv2
import numpy as np
import pandas as pd
from getLandmarks import createLandMarksFromPath

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


class FakeHands:
    def process(self, image):
        landmarks = [
            FakeLandmark(i * 0.01, i * 0.01, i * 0.01)
            for i in range(21)
        ]

        return FakeResults([
            FakeHandLandmarks(landmarks)
        ])


def test_create_landmarks_csv():
    with tempfile.TemporaryDirectory() as tmp:
        dataset = Path(tmp) / 'dataset'
        class_dir = dataset / 'A'
        class_dir.mkdir(parents=True)

        image = np.zeros((100, 100, 3), dtype=np.uint8)
        image_path = class_dir / 'sample.jpg'

        cv2.imwrite(str(image_path), image)

        createLandMarksFromPath(
            str(dataset),
            FakeHands()
        )

        csv_path = Path('asl_landmarks_dataset_210.csv')

        assert csv_path.exists()

        df = pd.read_csv(csv_path)

        assert len(df.columns) == 211
        assert df.iloc[0]['label'] == 'A'
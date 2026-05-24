import numpy as np
from sklearn.preprocessing import LabelEncoder

import nnmodel


class FakeInterpreter:
    def __init__(self, model_path):
        self.model_path = model_path
        self._input = None

    def allocate_tensors(self):
        pass

    def get_input_details(self):
        return [{'index': 0}]

    def get_output_details(self):
        return [{'index': 1}]

    def set_tensor(self, idx, value):
        assert idx == 0
        self._input = value

    def invoke(self):
        assert self._input is not None

    def get_tensor(self, idx):
        assert idx == 1
        return np.array([[0.1, 0.8, 0.1]], dtype=np.float32)


def test_predict_with_tflite(monkeypatch):
    import tensorflow as tf

    monkeypatch.setattr(tf.lite, 'Interpreter', FakeInterpreter)

    encoder = LabelEncoder()
    encoder.fit(['A', 'B', 'C'])

    label, confidence = nnmodel.predict_with_tflite(
        'fake_model.tflite',
        np.zeros((1, 210), dtype=np.float32),
        encoder,
    )

    assert label == 'B'
    assert confidence == 0.8

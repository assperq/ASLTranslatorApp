import numpy as np
from sklearn.preprocessing import LabelEncoder

import nnmodel


class FakeInterpreter:
    def __init__(self, *args, **kwargs):
        pass

    def allocate_tensors(self):
        pass

    def get_input_details(self):
        return [{'index': 0}]

    def get_output_details(self):
        return [{'index': 0}]

    def set_tensor(self, index, tensor):
        self.tensor = tensor

    def invoke(self):
        pass

    def get_tensor(self, index):
        return np.array([[0.1, 0.8, 0.1]], dtype=np.float32)



def test_predict_with_tflite(monkeypatch):
    import tensorflow as tf

    monkeypatch.setattr(
        tf.lite,
        'Interpreter',
        FakeInterpreter
    )

    encoder = LabelEncoder()
    encoder.fit(['A', 'B', 'C'])

    sample = np.random.rand(1, 210).astype(np.float32)

    label, confidence = nnmodel.predict_with_tflite(
        'fake_model.tflite',
        sample,
        encoder
    )

    assert label == 'B'
    assert confidence > 0.7
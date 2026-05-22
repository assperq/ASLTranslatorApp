import cv2
import tensorflow as tf
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from typing import Tuple, Optional


def create_and_train_nn(
    csv_path: str,
    model_save_path: str = 'asl_model.keras',
    tflite_save_path: str = 'asl_model.tflite',
) -> Tuple[tf.keras.Model, LabelEncoder]:
    """Создает, обучает и сохраняет нейронную сеть для распознавания жестов."""
    df = pd.read_csv(csv_path)
    X = df.drop(columns=['label']).values.astype(np.float32)
    y = df['label']

    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    num_classes = len(label_encoder.classes_)

    print(f"Количество классов: {num_classes}")
    print(f"Классы: {label_encoder.classes_}")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
    )

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(X.shape[1],)),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(num_classes, activation='softmax'),
    ])

    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])

    print("Начинаем обучение нейронной сети...")
    model.fit(
        X_train,
        y_train,
        epochs=50,
        batch_size=32,
        validation_data=(X_test, y_test),
        verbose=1,
    )

    _, test_acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"Точность на тестовых данных: {test_acc:.2%}")

    model.save(model_save_path)
    print(f"Keras модель сохранена в: {model_save_path}")

    convert_to_tflite(model, tflite_save_path)
    print(f"TFLite модель сохранена в: {tflite_save_path}")

    save_label_encoder(label_encoder, 'label_encoder.pkl')
    return model, label_encoder


def convert_to_tflite(keras_model: tf.keras.Model, output_path: str) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    with open(output_path, 'wb') as f:
        f.write(tflite_model)


def save_label_encoder(encoder: LabelEncoder, filepath: str) -> None:
    import joblib

    joblib.dump(encoder, filepath)
    print(f"LabelEncoder сохранен в: {filepath}")


def load_label_encoder(filepath: str) -> LabelEncoder:
    import joblib

    return joblib.load(filepath)


def _extract_landmark_array(results) -> Optional[np.ndarray]:
    if not results.multi_hand_landmarks:
        return None

    hand_landmarks = results.multi_hand_landmarks[0]
    coords = np.array([[lm.x, lm.y, lm.z] for lm in hand_landmarks.landmark], dtype=np.float32)
    return coords


def process_frame_for_tflite_from_33(frame: np.ndarray, mediapipe_hands: any) -> Optional[np.ndarray]:
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = mediapipe_hands.process(img_rgb)
    coords = _extract_landmark_array(results)
    if coords is None:
        return None
    return coords.reshape(1, -1)


def process_frame_for_tflite(frame: np.ndarray, mediapipe_hands) -> Optional[np.ndarray]:
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = mediapipe_hands.process(img_rgb)
    coords = _extract_landmark_array(results)
    if coords is None:
        return None

    coords = coords - coords[0]
    diffs = coords[:, None, :] - coords[None, :, :]
    dist_matrix = np.linalg.norm(diffs, axis=-1)
    upper_idx = np.triu_indices(len(coords), k=1)
    distances = dist_matrix[upper_idx].astype(np.float32)
    return distances.reshape(1, -1)


def predict_with_tflite(
    tflite_model_path: str,
    input_data: np.ndarray,
    label_encoder: LabelEncoder,
) -> Tuple[str, float]:
    interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    interpreter.set_tensor(input_details[0]['index'], input_data.astype(np.float32))
    interpreter.invoke()

    probabilities = interpreter.get_tensor(output_details[0]['index'])[0]
    predicted_class_idx = int(np.argmax(probabilities))
    confidence = float(probabilities[predicted_class_idx])
    predicted_label = label_encoder.inverse_transform([predicted_class_idx])[0]
    return predicted_label, confidence

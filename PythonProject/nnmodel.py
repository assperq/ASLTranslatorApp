import cv2
import tensorflow as tf
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score
import matplotlib.pyplot as plt
from typing import Tuple, Optional


def create_and_train_nn(csv_path: str,
                        model_save_path: str = 'asl_model.keras',
                        tflite_save_path: str = 'asl_model.tflite') -> Tuple[tf.keras.Model, LabelEncoder]:
    """
    Создает, обучает и сохраняет нейронную сеть для распознавания жестов.

    Args:
        csv_path: Путь к CSV файлу с данными
        model_save_path: Путь для сохранения Keras модели
        tflite_save_path: Путь для сохранения TFLite модели

    Returns:
        Обученная модель и LabelEncoder для преобразования меток
    """
    # Загрузка данных
    df = pd.read_csv(csv_path)

    # Разделение на признаки и метки
    X = df.drop(columns=['label']).values.astype(np.float32)
    y = df['label']

    # Кодирование меток
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    num_classes = len(label_encoder.classes_)

    print(f"Количество классов: {num_classes}")
    print(f"Классы: {label_encoder.classes_}")

    # Разделение на тренировочную и тестовую выборки
    X_train, X_test, y_train, y_test = train_test_split(
        X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
    )

    # Создание модели нейронной сети
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(128, activation='relu', input_shape=(X.shape[1],)),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dense(num_classes, activation='softmax')
    ])

    # Компиляция модели
    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

    # Обучение модели
    print("Начинаем обучение нейронной сети...")
    history = model.fit(
        X_train, y_train,
        epochs=50,
        batch_size=32,
        validation_data=(X_test, y_test),
        verbose=1
    )

    # Оценка точности
    test_loss, test_acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"Точность на тестовых данных: {test_acc:.2%}")

    # Сохранение Keras модели
    model.save(model_save_path)
    print(f"Keras модель сохранена в: {model_save_path}")

    # Конвертация в TensorFlow Lite
    convert_to_tflite(model, tflite_save_path)
    print(f"TFLite модель сохранена в: {tflite_save_path}")

    # Сохранение LabelEncoder
    save_label_encoder(label_encoder, 'label_encoder.pkl')

    return model, label_encoder


def convert_to_tflite(keras_model: tf.keras.Model, output_path: str) -> None:
    """
    Конвертирует Keras модель в TensorFlow Lite формат.
    """
    # Создание конвертера
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)

    # Оптимизации для уменьшения размера и ускорения
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    # Конвертация
    tflite_model = converter.convert()

    # Сохранение
    with open(output_path, 'wb') as f:
        f.write(tflite_model)


def save_label_encoder(encoder: LabelEncoder, filepath: str) -> None:
    """
    Сохраняет LabelEncoder в файл.
    """
    import joblib
    joblib.dump(encoder, filepath)
    print(f"LabelEncoder сохранен в: {filepath}")


def load_label_encoder(filepath: str) -> LabelEncoder:
    """
    Загружает LabelEncoder из файла.
    """
    import joblib
    return joblib.load(filepath)


def process_frame_for_tflite_from_33(frame: np.ndarray,
                             mediapipe_hands: any) -> Optional[np.ndarray]:
    """
    Обрабатывает кадр и возвращает landmarks для TFLite модели.
    """
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = mediapipe_hands.process(img_rgb)

    if results.multi_hand_landmarks:
        landmarks = []
        for hand_landmarks in results.multi_hand_landmarks:
            for lm in hand_landmarks.landmark:
                landmarks.extend([lm.x, lm.y, lm.z])

        return np.array(landmarks, dtype=np.float32).reshape(1, -1)

    return None

def process_frame_for_tflite(frame: np.ndarray, mediapipe_hands) -> Optional[np.ndarray]:
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = mediapipe_hands.process(img_rgb)

    if results.multi_hand_landmarks:

        coords = []

        # берём только первую руку
        hand_landmarks = results.multi_hand_landmarks[0]

        for lm in hand_landmarks.landmark:
            coords.append([lm.x, lm.y, lm.z])

        coords = np.array(coords)
        coords = coords - coords[0]
        # вычисляем расстояния между всеми точками
        distances = []

        for i in range(len(coords)):
            for j in range(i + 1, len(coords)):
                dist = np.linalg.norm(coords[i] - coords[j])
                distances.append(dist)

        distances = np.array(distances, dtype=np.float32)

        return distances.reshape(1, -1)

    return None


def predict_with_tflite(tflite_model_path: str,
                        input_data: np.ndarray,
                        label_encoder: LabelEncoder) -> Tuple[str, float]:
    """
    Выполняет предсказание с помощью TFLite модели.

    Args:
        tflite_model_path: Путь к TFLite модели
        input_data: Входные данные (landmarks)
        label_encoder: LabelEncoder для преобразования меток

    Returns:
        Предсказанная буква и уверенность
    """
    # Загрузка TFLite модели
    interpreter = tf.lite.Interpreter(model_path=tflite_model_path)
    interpreter.allocate_tensors()

    # Получение информации о входе и выходе
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # Подготовка входных данных
    input_data = input_data.astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], input_data)

    # Выполнение inference
    interpreter.invoke()

    # Получение результатов
    output_data = interpreter.get_tensor(output_details[0]['index'])
    probabilities = output_data[0]

    # Определение предсказания
    predicted_class_idx = np.argmax(probabilities)
    confidence = probabilities[predicted_class_idx]
    predicted_label = label_encoder.inverse_transform([predicted_class_idx])[0]

    return predicted_label, confidence
import cv2
import joblib
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from typing import Optional, Tuple


def _build_model(input_dim: int, num_classes: int, normalization: tf.keras.layers.Normalization) -> tf.keras.Model:
    inputs = tf.keras.layers.Input(shape=(input_dim,))
    x = normalization(inputs)
    x = tf.keras.layers.Dense(256, activation='relu')(x)
    x = tf.keras.layers.Dropout(0.35)(x)
    x = tf.keras.layers.Dense(128, activation='relu')(x)
    x = tf.keras.layers.Dropout(0.25)(x)
    x = tf.keras.layers.Dense(64, activation='relu')(x)
    outputs = tf.keras.layers.Dense(num_classes, activation='softmax')(x)
    model = tf.keras.Model(inputs=inputs, outputs=outputs, name='asl_classifier')
    return model


def create_and_train_nn(
    csv_path: str,
    model_save_path: str = 'asl_model.keras',
    tflite_save_path: str = 'asl_model.tflite',
    label_encoder_path: str = 'label_encoder.pkl',
    random_state: int = 42,
) -> Tuple[tf.keras.Model, LabelEncoder]:
    """Создает, обучает и сохраняет NN-модель для распознавания ASL."""
    df = pd.read_csv(csv_path)
    if 'label' not in df.columns:
        raise ValueError("В CSV отсутствует колонка 'label'")

    X = df.drop(columns=['label']).values.astype(np.float32)
    y = df['label']

    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    num_classes = len(label_encoder.classes_)

    print(f"Количество классов: {num_classes}")
    print(f"Классы: {label_encoder.classes_}")

    X_train_val, X_test, y_train_val, y_test = train_test_split(
        X,
        y_encoded,
        test_size=0.2,
        random_state=random_state,
        stratify=y_encoded,
    )
    X_train, X_val, y_train, y_val = train_test_split(
        X_train_val,
        y_train_val,
        test_size=0.125,
        random_state=random_state,
        stratify=y_train_val,
    )
    print(f"Размеры выборок: train={len(X_train)}, val={len(X_val)}, test={len(X_test)}")

    normalization = tf.keras.layers.Normalization(axis=-1)
    normalization.adapt(X_train)

    model = _build_model(X.shape[1], num_classes, normalization)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy'],
    )

    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor='val_accuracy',
            patience=12,
            mode='max',
            restore_best_weights=True,
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=1e-5,
            verbose=1,
        ),
    ]

    print('Начинаем обучение нейронной сети...')
    model.fit(
        X_train,
        y_train,
        epochs=120,
        batch_size=32,
        validation_data=(X_val, y_val),
        callbacks=callbacks,
        verbose=1,
    )

    train_loss, train_acc = model.evaluate(X_train, y_train, verbose=0)
    val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
    test_loss, test_acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"Train accuracy: {train_acc:.2%} (loss={train_loss:.4f})")
    print(f"Val accuracy: {val_acc:.2%} (loss={val_loss:.4f})")
    print(f"Test accuracy: {test_acc:.2%} (loss={test_loss:.4f})")

    model.save(model_save_path)
    print(f'Keras модель сохранена в: {model_save_path}')

    convert_to_tflite(model, tflite_save_path)
    print(f'TFLite модель сохранена в: {tflite_save_path}')

    save_label_encoder(label_encoder, label_encoder_path)
    return model, label_encoder


def convert_to_tflite(keras_model: tf.keras.Model, output_path: str) -> None:
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()
    with open(output_path, 'wb') as f:
        f.write(tflite_model)


def save_label_encoder(encoder: LabelEncoder, filepath: str) -> None:
    joblib.dump(encoder, filepath)
    print(f'LabelEncoder сохранен в: {filepath}')


def load_label_encoder(filepath: str) -> LabelEncoder:
    return joblib.load(filepath)


def _extract_landmark_array(results) -> Optional[np.ndarray]:
    if not results.multi_hand_landmarks:
        return None
    hand_landmarks = results.multi_hand_landmarks[0]
    return np.array([[lm.x, lm.y, lm.z] for lm in hand_landmarks.landmark], dtype=np.float32)


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

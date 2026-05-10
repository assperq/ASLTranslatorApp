import cv2
import mediapipe as mp
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
import pandas as pd
from typing import Optional, Tuple, Any, List
from numpy.typing import NDArray

# Типы для аннотаций
DataFrame = pd.DataFrame
NDArrayFloat = NDArray[np.float64]


def learnRandomForest(csvName: str) -> RandomForestClassifier:
    """
    Обучает модель Random Forest на данных из CSV файла.

    Args:
        csvName: Путь к CSV файлу с данными

    Returns:
        Обученная модель RandomForestClassifier
    """
    # Загрузка данных
    df: DataFrame = pd.read_csv(csvName)

    # Разделение на признаки и метки
    X: DataFrame = df.drop('label', axis=1)
    y: pd.Series = df['label']

    # Разделение на тренировочную и тестовую выборки
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    # Обучение Random Forest
    model: RandomForestClassifier = RandomForestClassifier(
        n_estimators=100, random_state=42
    )
    model.fit(X_train, y_train)

    # Проверка точности
    y_pred = model.predict(X_test)
    accuracy: float = accuracy_score(y_test, y_pred)
    print(f"Точность модели: {accuracy:.2%}")

    return model


def example(imagePath: str, model: RandomForestClassifier, mediaPipeInstance: Any) -> None:
    """
    Пример использования модели для предсказания на одном изображении.

    Args:
        imagePath: Путь к изображению для анализа
        model: Обученная модель Random Forest
        mediaPipeInstance: Экземпляр MediaPipe Hands
    """
    # Загрузка изображения
    frame: Optional[NDArrayFloat] = cv2.imread(imagePath)

    if frame is None:
        print(f"Не удалось загрузить изображение: {imagePath}")
        return

    # Извлечение landmarks
    landmarks: Optional[NDArrayFloat] = process_frame(frame, mediaPipeInstance)

    if landmarks is not None:
        prediction = model.predict(landmarks)
        confidence: float = model.predict_proba(landmarks).max()
        print(f"Предсказание: {prediction[0]}, уверенность: {confidence:.2%}")
    else:
        print("Landmarks не найдены на изображении")


def process_frame(frame: NDArrayFloat, mediaPipeInstance: Any) -> Optional[NDArrayFloat]:
    """
    Обрабатывает кадр и возвращает landmarks.

    Args:
        frame: Изображение в формате BGR
        mediaPipeInstance: Экземпляр MediaPipe Hands

    Returns:
        Массив с landmarks формы (1, 63) или None если рука не обнаружена
    """
    # Конвертация в RGB
    img_rgb: NDArrayFloat = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

    # Обработка MediaPipe
    results = mediaPipeInstance.process(img_rgb)

    if results.multi_hand_landmarks:
        landmarks_list: List[float] = []
        for hand_landmarks in results.multi_hand_landmarks:
            for lm in hand_landmarks.landmark:
                landmarks_list.extend([lm.x, lm.y, lm.z])

        # Преобразование в numpy array и изменение формы
        landmarks_array: NDArrayFloat = np.array(landmarks_list, dtype=np.float64)
        return landmarks_array.reshape(1, -1)

    return None


# Если нужно, можно создать простую функцию для инициализации MediaPipe
def create_mediapipe_hands() -> Any:
    """Создает и возвращает экземпляр MediaPipe Hands"""
    mp_hands = mp.solutions.hands
    return mp_hands.Hands(
        static_image_mode=True,
        max_num_hands=2,
        min_detection_confidence=0.5
    )

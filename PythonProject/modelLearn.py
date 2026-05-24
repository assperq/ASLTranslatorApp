import mediapipe as mp
from typing import Any


def create_mediapipe_hands() -> Any:
    """Создает и возвращает экземпляр MediaPipe Hands."""
    mp_hands = mp.solutions.hands
    return mp_hands.Hands(
        static_image_mode=True,
        max_num_hands=1,
        min_detection_confidence=0.5,
    )

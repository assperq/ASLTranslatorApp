from pathlib import Path

import cv2

from modelLearn import create_mediapipe_hands
from nnmodel import (
    create_and_train_nn,
    load_label_encoder,
    predict_with_tflite,
    process_frame_for_tflite,
)


def test_model(hands, test_image_path: str = 'test.jpg') -> None:
    label_encoder = load_label_encoder('label_encoder.pkl')
    frame = cv2.imread(test_image_path)

    if frame is None:
        print(f"Не удалось загрузить изображение: {test_image_path}")
        return

    landmarks = process_frame_for_tflite(frame, hands)
    if landmarks is None:
        print('Не удалось найти landmarks на изображении')
        return

    prediction, confidence = predict_with_tflite('asl_model.tflite', landmarks, label_encoder)
    print(f'Предсказание: {prediction}')
    print(f'Уверенность: {confidence:.2%}')


def main() -> None:
    hands = create_mediapipe_hands()
    try:
        create_and_train_nn(
            csv_path='asl_landmarks_dataset_210.csv',
            model_save_path='asl_model.keras',
            tflite_save_path='asl_model.tflite',
            label_encoder_path='label_encoder.pkl',
        )

        print('\n🧪 Тестирование модели...')
        default_test_image = Path('test.jpg')
        if default_test_image.exists():
            test_model(hands, str(default_test_image))
        else:
            print('Тестовый файл test.jpg не найден, шаг инференса пропущен')
    finally:
        hands.close()


if __name__ == '__main__':
    main()

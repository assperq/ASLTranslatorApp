import cv2
from joblib._multiprocessing_helpers import mp
from createModel import load_model, save_model, convert_to_tflite
from getLandmarks import createLandMarksFromPath
from modelLearn import learnRandomForest, example, create_mediapipe_hands
from sklearn.ensemble import RandomForestClassifier

from nnmodel import process_frame_for_tflite, create_and_train_nn, predict_with_tflite, load_label_encoder


def test_model(hands):
    test_image_path = r'C:\Users\SystemX\Downloads\111.jpg'
    label_encoder = load_label_encoder('label_encoder.pkl')
    frame = cv2.imread(test_image_path)

    for label in label_encoder.classes_:
        print(label)

    if frame is not None:
        # Обработка кадра
        landmarks = process_frame_for_tflite(frame, hands)

        if landmarks is not None:
            # Предсказание с TFLite
            prediction, confidence = predict_with_tflite(
                'asl_model.tflite',
                landmarks,
                label_encoder
            )

            print(f"Предсказание: {prediction}")
            print(f"Уверенность: {confidence:.2%}")
        else:
            print("Не удалось найти landmarks на изображении")
    else:
        print("Не удалось загрузить изображение")


def main():
    # 1. Обучение и сохранение модели
    # print("🚀 Обучение нейронной сети...")
    hands = create_mediapipe_hands()
    createLandMarksFromPath(r'C:\Users\SystemX\programming\Diplom\DATA', hands)

    create_and_train_nn(
        csv_path='asl_landmarks_dataset_210.csv',
        model_save_path='asl_model.keras',
        tflite_save_path='asl_model.tflite'
    )

    # 2. Тестирование на примере
    print("\n🧪 Тестирование модели...")


    try:
        # Загрузка тестового изображения
        test_model(hands)

    finally:
        hands.close()

if __name__ == "__main__":
    main()
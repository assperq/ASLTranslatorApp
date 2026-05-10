import cv2
import pandas as pd
from pathlib import Path
from itertools import combinations
from math import sqrt

def createLandMarksFromPath(dataset_path: str, hands) -> None:
    """
    Создает CSV с 210 признаками для каждого изображения из датасета ASL.

    Args:
        dataset_path: путь к папке с датасетом (каждая подпапка = буква)
        hands: объект mediapipe.solutions.hands.Hands()
    """

    dataset_path = Path(dataset_path)
    output_csv_path = 'asl_landmarks_dataset_210.csv'

    data = []

    # Проход по папкам (буквам)
    for folder in dataset_path.iterdir():
        if not folder.is_dir():
            continue

        label = folder.name
        print(f"Обрабатываем букву: '{label}'")

        for img_path in folder.glob('*.*'):
            if img_path.suffix.lower() not in ['.jpg', '.jpeg', '.png', '.bmp']:
                continue

            try:
                img = cv2.imread(str(img_path))
                if img is None:
                    continue

                img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
                results = hands.process(img_rgb)

                if results.multi_hand_landmarks:
                    for hand_landmarks in results.multi_hand_landmarks:
                        # Сохраняем координаты всех 21 landmark
                        points = [(lm.x, lm.y, lm.z) for lm in hand_landmarks.landmark]

                        # Создаем 210 признаков = все попарные расстояния
                        features = []
                        for i, j in combinations(range(21), 2):
                            x1, y1, z1 = points[i]
                            x2, y2, z2 = points[j]
                            d = sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2 + (z1 - z2) ** 2)
                            features.append(d)

                        # Добавляем в данные вместе с меткой
                        data.append([label] + features)

            except Exception as e:
                print(f"Ошибка с файлом {img_path}: {e}")

    # Создание CSV
    if data:
        # Создаем имена колонок: label + feature_0 .. feature_209
        columns = ['label'] + [f'feature_{i}' for i in range(210)]

        df = pd.DataFrame(data, columns=columns)
        df.to_csv(output_csv_path, index=False, encoding='utf-8-sig')
        print(f"\nУспешно! Обработано {len(data)} изображений")
        print(f"Создан файл: {output_csv_path}")

        # Статистика по буквам
        print("\nСтатистика по буквам:")
        print(df['label'].value_counts())
    else:
        print("Не найдено изображений для обработки")
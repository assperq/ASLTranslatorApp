import joblib
import pickle
from pathlib import Path

from sklearn.ensemble import RandomForestClassifier

from modelLearn import learnRandomForest


def save_model(model: RandomForestClassifier, filename: str = 'rsl_model.pkl') -> None:
    """
    Сохраняет обученную модель в файл.

    Args:
        model: Обученная модель RandomForest
        filename: Имя файла для сохранения
    """
    try:
        # Используем joblib (лучше для sklearn моделей)
        joblib.dump(model, filename)
        print(f"Модель сохранена в файл: {filename}")

        # Альтернативно можно использовать pickle
        # with open(filename, 'wb') as f:
        #     pickle.dump(model, f)

    except Exception as e:
        print(f"Ошибка при сохранении модели: {e}")


def load_model(filename: str = 'rsl_model.pkl') -> RandomForestClassifier:
    """
    Загружает модель из файла.

    Args:
        filename: Имя файла с моделью

    Returns:
        Загруженная модель RandomForest
    """
    try:
        model = joblib.load(filename)
        print(f"Модель загружена из файла: {filename}")
        return model
    except Exception as e:
        print(f"Ошибка при загрузке модели: {e}")
        raise


def learnAndSaveRandomForest(csvName: str, model_filename: str = 'rsl_model.pkl') -> RandomForestClassifier:
    """
    Обучает модель и сразу сохраняет её в файл.

    Args:
        csvName: Путь к CSV файлу с данными
        model_filename: Имя файла для сохранения модели

    Returns:
        Обученная модель RandomForestClassifier
    """
    # Обучение модели
    model = learnRandomForest(csvName)

    # Сохранение модели
    save_model(model, model_filename)

    return model


def convert_to_tflite(model: RandomForestClassifier, output_filename: str = 'rsl_model.tflite') -> None:
    """
    Конвертирует модель в формат TensorFlow Lite для использования на Android.
    """
    try:
        # Сначала сохраним в временный файл
        import tempfile
        import os

        # Временное сохранение sklearn модели
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pkl') as tmp:
            joblib.dump(model, tmp.name)

            # Здесь должен быть код конвертации в TFLite
            # Для RandomForest это сложнее, чем для нейросетей
            print("Для RandomForest требуется дополнительный код конвертации")

        # Удаляем временный файл
        os.unlink(tmp.name)

    except Exception as e:
        print(f"Ошибка конвертации: {e}")


# Альтернатива: используйте ONNX для кроссплатформенности
def convert_to_onnx(model: RandomForestClassifier, output_filename: str = 'rsl_model.onnx') -> None:
    """
    Конвертирует модель в формат ONNX.
    """
    try:
        from skl2onnx import convert_sklearn
        from skl2onnx.common.data_types import FloatTensorType

        # Определяем тип входных данных
        initial_type = [('float_input', FloatTensorType([None, 63]))]  # 21 points * 3 coordinates

        # Конвертируем
        onnx_model = convert_sklearn(model, initial_types=initial_type)

        # Сохраняем
        with open(output_filename, "wb") as f:
            f.write(onnx_model.SerializeToString())

        print(f"Модель сохранена в ONNX формате: {output_filename}")

    except ImportError:
        print("Установите skl2onnx: pip install skl2onnx")
    except Exception as e:
        print(f"Ошибка конвертации в ONNX: {e}")
from nnmodel import create_and_train_nn


if __name__ == '__main__':
    create_and_train_nn(
        csv_path='asl_landmarks_dataset_210.csv',
        model_save_path='asl_model.keras',
        tflite_save_path='asl_model.tflite',
        label_encoder_path='label_encoder.pkl',
    )

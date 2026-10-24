import time
import json
import os
import struct

import numpy as np

import pandas as pd

from scipy.interpolate import interp1d

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import f1_score, classification_report, confusion_matrix

t0 = time.time()
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Conv1D, MaxPooling1D, LSTM, Dense, Dropout
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from tensorflow.keras.regularizers import l2

import matplotlib
matplotlib.use('Agg') 
import matplotlib.pyplot as plt

DATASET_PATH = "app/src/main/assets/dataset_simon_says.csv"
OUTPUT_DIR = "./training_output"
TARGET_LENGTH = 258
RANDOM_STATE = 42

from itertools import product as grid_product

GRID = {
    'conv_layers': [2, 3, 4],
    'lstm_layers': [2, 3],
    'num_aug': [3, 5],
    'dropout': [0.2, 0.3],
    'l2_coeff': [0.0001, 0.001],
    'lr': [0.001, 0.0001],
}
FIXED = {'activation': 'tanh', 'batch_size': 32}

CONFIGS = []
for combo in grid_product(*GRID.values()):
    params = dict(zip(GRID.keys(), combo))
    params.update(FIXED)
    params['name'] = (f"Conv{params['conv_layers']}_LSTM{params['lstm_layers']}"
                      f"_Aug{params['num_aug']}_Do{params['dropout']}"
                      f"_tanh_L2-{params['l2_coeff']}_LR{params['lr']}")
    CONFIGS.append(params)

def normalize_sequence_length(data, target_length=TARGET_LENGTH):
    n = len(data)
    if n == target_length:
        return data
    x_old = np.linspace(0, 1, n)
    x_new = np.linspace(0, 1, target_length)
    f = interp1d(x_old, data, kind='linear')
    return f(x_new)


def augment_sequence(sequence, num_augmentations=3):
    augmented = [sequence]
    for _ in range(num_augmentations):
        aug_seq = sequence.copy()

        noise = np.random.normal(0, 0.12 * np.std(sequence), sequence.shape)
        aug_seq = aug_seq + noise

        scale_factor = np.random.uniform(0.75, 1.25)
        aug_seq[:, :3] = aug_seq[:, :3] * scale_factor
        aug_seq[:, 3:] = aug_seq[:, 3:] * scale_factor

        if np.random.rand() > 0.5:
            stretch = np.random.uniform(0.85, 1.15)
            indices = np.linspace(0, len(aug_seq) - 1, int(len(aug_seq) / stretch))
            indices = np.clip(indices, 0, len(aug_seq) - 1).astype(int)
            aug_seq = aug_seq[indices]
            aug_seq = np.array([
                interp1d(np.linspace(0, 1, len(aug_seq)), aug_seq[:, i], kind='linear')(
                    np.linspace(0, 1, TARGET_LENGTH))
                for i in range(6)
            ]).T

        if np.random.rand() > 0.5:
            angle = np.random.uniform(-30, 30) * np.pi / 180
            cos_a, sin_a = np.cos(angle), np.sin(angle)
            rotation_matrix = np.array([
                [cos_a, -sin_a, 0],
                [sin_a, cos_a, 0],
                [0, 0, 1]
            ])
            aug_seq[:, :3] = aug_seq[:, :3] @ rotation_matrix.T
            aug_seq[:, 3:] = aug_seq[:, 3:] @ rotation_matrix.T

        magnitude_warp = np.random.uniform(0.75, 1.25, (TARGET_LENGTH, 1))
        aug_seq = aug_seq * magnitude_warp

        augmented.append(aug_seq)
    return augmented


def build_model(num_classes, conv_layers, lstm_layers, dropout, activation, l2_coeff, lr):
    reg = l2(l2_coeff)
    layers = []

    layers.append(Conv1D(64, 5, activation=activation, input_shape=(TARGET_LENGTH, 6), kernel_regularizer=reg))
    layers.append(MaxPooling1D(2))
    layers.append(Dropout(dropout))

    for i in range(conv_layers - 1):
        filters = min(64 * (i + 2), 512)
        layers.append(Conv1D(filters, 5, activation=activation, kernel_regularizer=reg))
        layers.append(MaxPooling1D(2))
        layers.append(Dropout(dropout))

    lstm_sizes = [256, 128, 64, 64]
    for i in range(lstm_layers):
        size = lstm_sizes[i] if i < len(lstm_sizes) else 64
        return_seq = (i < lstm_layers - 1)
        layers.append(LSTM(size, return_sequences=return_seq, activation=activation, kernel_regularizer=reg))
        layers.append(Dropout(dropout))

    layers.append(Dense(128, activation=activation, kernel_regularizer=reg))
    layers.append(Dropout(dropout + 0.05))
    layers.append(Dense(64, activation=activation, kernel_regularizer=reg))
    layers.append(Dropout(dropout))
    layers.append(Dense(num_classes, activation='softmax'))

    model = Sequential(layers)
    model.compile(
        optimizer=Adam(learning_rate=lr),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    return model


def save_scaler_bin(array, filepath):
    with open(filepath, 'wb') as f:
        for val in array.flatten():
            f.write(struct.pack('f', float(val)))


def convert_to_tflite(h5_path, tflite_path):
    model = tf.keras.models.load_model(h5_path)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    converter._experimental_lower_tensor_list_ops = False
    converter.experimental_enable_resource_variables = True
    tflite_model = converter.convert()
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    return len(tflite_model)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(f"{OUTPUT_DIR}/models", exist_ok=True)
    os.makedirs(f"{OUTPUT_DIR}/curves", exist_ok=True)


    df = pd.read_csv(DATASET_PATH)
    for g in sorted(df['gesture_id'].unique()):
        n_trials = df[df['gesture_id'] == g]['trial'].nunique()
  

    normalized_data = []
    for gesture in sorted(df['gesture_id'].unique()):
        gesture_df = df[df['gesture_id'] == gesture]
        for trial in sorted(gesture_df['trial'].unique()):
            trial_data = gesture_df[gesture_df['trial'] == trial].sort_values('sample_id')
            acc = trial_data[['acc_x', 'acc_y', 'acc_z']].values
            gyro = trial_data[['gyro_x', 'gyro_y', 'gyro_z']].values

            acc_norm = np.array([normalize_sequence_length(acc[:, i]) for i in range(3)]).T
            gyro_norm = np.array([normalize_sequence_length(gyro[:, i]) for i in range(3)]).T

            sequence = np.hstack([acc_norm, gyro_norm])  # [258, 6]
            normalized_data.append({'gesture': gesture, 'trial': trial, 'sequence': sequence})


    gesture_map = {gesture: i for i, gesture in enumerate(sorted(df['gesture_id'].unique()))}
    X = np.array([d['sequence'] for d in normalized_data])
    y = np.array([gesture_map[d['gesture']] for d in normalized_data])

    print(f"X shape: {X.shape}")
    print(f"y shape: {y.shape}")
    print(f"Gesture map: {gesture_map}")

    X_temp, X_test, y_temp, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_STATE, stratify=y)
    X_train, X_val, y_train, y_val = train_test_split(
        X_temp, y_temp, test_size=0.25, random_state=RANDOM_STATE, stratify=y_temp)

    X_train_reshaped = X_train.reshape(X_train.shape[0], -1)
    X_val_reshaped = X_val.reshape(X_val.shape[0], -1)
    X_test_reshaped = X_test.reshape(X_test.shape[0], -1)

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train_reshaped).reshape(X_train.shape)
    X_val_scaled = scaler.transform(X_val_reshaped).reshape(X_val.shape)
    X_test_scaled = scaler.transform(X_test_reshaped).reshape(X_test.shape)

    scaler_mean = scaler.mean_ 
    scaler_std = scaler.scale_  

    np.save(f"{OUTPUT_DIR}/scaler_mean.npy", scaler_mean)
    np.save(f"{OUTPUT_DIR}/scaler_std.npy", scaler_std)
    save_scaler_bin(scaler_mean, f"{OUTPUT_DIR}/scaler_mean.bin")
    save_scaler_bin(scaler_std, f"{OUTPUT_DIR}/scaler_std.bin")

    print(f"Scaler mean shape: {scaler_mean.shape}")
    print(f"Scaler std shape: {scaler_std.shape}")

    with open(f"{OUTPUT_DIR}/gesture_map.json", 'w') as f:
        json.dump(gesture_map, f, indent=2)


    num_classes = len(gesture_map)
    results = []

    for idx, cfg in enumerate(CONFIGS):

        t_aug = time.time()
        X_train_aug = []
        y_train_aug = []
        for i, seq in enumerate(X_train_scaled):
            augmented = augment_sequence(seq, num_augmentations=cfg['num_aug'])
            X_train_aug.extend(augmented)
            y_train_aug.extend([y_train[i]] * len(augmented))
        X_train_aug = np.array(X_train_aug)
        y_train_aug = np.array(y_train_aug)

        try:
            model = build_model(
                num_classes=num_classes,
                conv_layers=cfg['conv_layers'],
                lstm_layers=cfg['lstm_layers'],
                dropout=cfg['dropout'],
                activation=cfg['activation'],
                l2_coeff=cfg['l2_coeff'],
                lr=cfg['lr']
            )

            t_train = time.time()
            history = model.fit(
                X_train_aug, y_train_aug,
                epochs=80,
                batch_size=cfg['batch_size'],
                validation_data=(X_val_scaled, y_val),
                callbacks=[
                    EarlyStopping(monitor='val_loss', patience=8, restore_best_weights=True, verbose=1),
                    ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=3, min_lr=1e-5, verbose=1)
                ],
                verbose=2
            )
            train_time = time.time() - t_train

            train_loss, train_acc = model.evaluate(X_train_aug, y_train_aug, verbose=0)
            val_loss, val_acc = model.evaluate(X_val_scaled, y_val, verbose=0)
            test_loss, test_acc = model.evaluate(X_test_scaled, y_test, verbose=0)

            y_pred = model.predict(X_test_scaled, verbose=0).argmax(axis=1)
            test_f1 = f1_score(y_test, y_pred, average='weighted')


            model_name = f"[{idx+1}]_{cfg['name']}_test{test_acc:.4f}"
            h5_path = f"{OUTPUT_DIR}/models/{model_name}.h5"
            model.save(h5_path)

            fig, axes = plt.subplots(1, 2, figsize=(14, 5))
            axes[0].plot(history.history['loss'], label='Train', linewidth=2)
            axes[0].plot(history.history['val_loss'], label='Val', linewidth=2)
            axes[0].set_title(f'Loss | Test Acc: {test_acc:.2%}', fontweight='bold')
            axes[0].legend()
            axes[0].grid(True, alpha=0.3)
            axes[1].plot(history.history['accuracy'], label='Train', linewidth=2)
            axes[1].plot(history.history['val_accuracy'], label='Val', linewidth=2)
            axes[1].set_title(f'Accuracy | F1: {test_f1:.4f}', fontweight='bold')
            axes[1].legend()
            axes[1].grid(True, alpha=0.3)
            plt.suptitle(cfg['name'], fontsize=12, fontweight='bold')
            plt.tight_layout()
            plt.savefig(f"{OUTPUT_DIR}/curves/{model_name}.png", dpi=100, bbox_inches='tight')
            plt.close()

            results.append({
                'config': cfg['name'],
                'test_acc': test_acc,
                'test_f1': test_f1,
                'val_acc': val_acc,
                'train_acc': train_acc,
                'h5_path': h5_path,
                'epochs_trained': len(history.history['loss'])
            })

        except Exception as e:
            print(f"  ERROR: {e}")
            continue

    if not results:
        print("No models trained successfully!")
        return

    df_results = pd.DataFrame(results)
    df_results['overfit_gap'] = df_results['train_acc'] - df_results['val_acc']
    df_results = df_results.sort_values('test_acc', ascending=False)

    print("\nAll results (sorted by test_acc):")
    print(df_results[['config', 'test_acc', 'test_f1', 'val_acc', 'train_acc', 'overfit_gap', 'epochs_trained']].to_string(index=False))
    df_results.to_csv(f"{OUTPUT_DIR}/results.csv", index=False)

    inv_gesture_map = {v: k for k, v in gesture_map.items()}
    target_names = [inv_gesture_map[i] for i in range(num_classes)]

if __name__ == "__main__":
    main()

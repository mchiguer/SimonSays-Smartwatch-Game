import tensorflow as tf
import shutil
import sys
import os

ASSETS_DIR = "app/src/main/assets"
OUTPUT_DIR = "./training_output"


def convert_to_tflite(h5_path, tflite_path):
    model = tf.keras.models.load_model(h5_path)
    model.summary()

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


    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()
    out = interpreter.get_output_details()

def main():
    if len(sys.argv) < 2:
        sys.exit(1)

    h5_path = sys.argv[1]
    if not os.path.exists(h5_path):
        sys.exit(1)

    mean_bin = f"{OUTPUT_DIR}/scaler_mean.bin"
    std_bin = f"{OUTPUT_DIR}/scaler_std.bin"
    if not os.path.exists(mean_bin) or not os.path.exists(std_bin):
        sys.exit(1)

    os.makedirs(ASSETS_DIR, exist_ok=True)

    tflite_path = os.path.join(ASSETS_DIR, "move_classifier.tflite")
    convert_to_tflite(h5_path, tflite_path)

    shutil.copy2(mean_bin, os.path.join(ASSETS_DIR, "scaler_mean.bin"))
    shutil.copy2(std_bin, os.path.join(ASSETS_DIR, "scaler_std.bin"))



if __name__ == "__main__":
    main()

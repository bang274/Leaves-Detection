from train import create_model
from tensorflow.keras.preprocessing.image import load_img, img_to_array
import numpy as np
import sys
import traceback

def preprocess_image(image_path, target_size=(225, 225)):
    try:
        img = load_img(image_path, target_size=target_size)
        x = img_to_array(img)
        x = x.astype('float32') / 255.
        x = np.expand_dims(x, axis=0)
        return x
    except Exception as e:
        print(f"Error preprocessing image: {str(e)}", file=sys.stderr)
        sys.exit(1)

def main():
    if len(sys.argv) != 2:
        print("Usage: python infer.py <image_path>", file=sys.stderr)
        sys.exit(1)

    image_path = sys.argv[1]
    labels = ['Acer Palmatum', 'Acer Rubrum', 'Aesculus Hippocastanum', 'Betula Pendula', 'Fagus Sylvatica', 'Quercus Robur', 'Tilia Cordata']

    try:
        # Load model and weights
        model = create_model()
        model.load_weights('model.keras')

        # Process image and predict
        x = preprocess_image(image_path)
        predictions = model.predict(x)
        predicted_class = labels[np.argmax(predictions)]

        # Output result
        print(predicted_class)
    except Exception as e:
        print(f"Error during inference: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
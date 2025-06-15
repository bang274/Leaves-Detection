import json
import sys
import numpy as np
from tensorflow.keras.preprocessing.image import load_img, img_to_array
from train import create_model

def preprocess_image(image_path, target_size=(225, 225)):
    try:
        img = load_img(image_path, target_size=target_size)
        x = img_to_array(img)
        x = x.astype('float32') / 255.
        x = np.expand_dims(x, axis=0)
        return x
    except Exception as e:
        print(f"Error preprocessing image: {str(e)}", file=sys.stderr)
        sys.exit(2)

def main():
    if len(sys.argv) != 2:
        print("Usage: python infer.py <image_path>", file=sys.stderr)
        sys.exit(2)

    image_path = sys.argv[1]
    labels = ['Acer Palmatum', 'Acer Rubrum', 'Aesculus Hippocastanum', 'Betula Pendula', 
              'Fagus Sylvatica', 'Quercus Robur', 'Tilia Cordata']

    try:
        # Load model and weights
        model = create_model()
        model.load_weights('model.keras')

        # Process image and predict
        x = preprocess_image(image_path)
        predictions = model.predict(x)
        predicted_class = labels[np.argmax(predictions)]

        # Output JSON
        result = {
            "leaf_type": predicted_class,
            "features": ["poisionous", "round shape"]  # Empty features array as per app expectation
        }
        print(json.dumps(result))
    except Exception as e:
        print(f"Error during inference: {str(e)}", file=sys.stderr)
        sys.exit(2)

if __name__ == "__main__":
    main()
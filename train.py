from tensorflow.keras.preprocessing.image import ImageDataGenerator
from keras.models import Sequential
from keras.layers import Conv2D, MaxPooling2D, Flatten, Dense

def create_model():
    model = Sequential()
    model.add(Conv2D(32, (3, 3), input_shape=(225, 225, 3), activation='relu'))
    model.add(MaxPooling2D(pool_size=(2, 2)))
    model.add(Conv2D(64, (3, 3), activation='relu'))
    model.add(MaxPooling2D(pool_size=(2, 2)))
    model.add(Flatten())
    model.add(Dense(64, activation='relu'))
    model.add(Dense(7, activation='softmax'))
    model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])
    return model
def main():
    train_datagen = ImageDataGenerator(rescale=1./255, shear_range=0.2, zoom_range=0.2, horizontal_flip=True)
    test_datagen = ImageDataGenerator(rescale=1./255)

    train_generator = train_datagen.flow_from_directory('data/train',
                                                        target_size=(225, 225),
                                                        batch_size=32,
                                                        class_mode='categorical')

    validation_generator = test_datagen.flow_from_directory('data/test',
                                                            target_size=(225, 225),
                                                            batch_size=32,
                                                            class_mode='categorical')

    model = create_model()
    model.fit(train_generator,
                        batch_size=16,
                        epochs=5,
                        validation_data=validation_generator,
                        validation_batch_size=16
                        )

    model.save("model.keras")

if __name__ == "__main__": 
    main()
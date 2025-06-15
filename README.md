## Running the Application
1. Clone the repository.
2. Install dependencies  
   `pip install -r requirement.txt`
3. Train the model.
   `python train.py`
4. Run java app.
   `javac -cp "lib/jgraphx-4.2.2.jar:lib/json-20231013.jar" src/com/example/LeafDetectionApp.java`
   `java -cp "lib/jgraphx-4.2.2.jar:lib/json-20231013.jar:src" com.example.LeafDetectionApp`
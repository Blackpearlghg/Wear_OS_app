import pandas as pd
import numpy as np
from sklearn.svm import LinearSVC
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score

# 1. Configuration
# Assuming your watch records at roughly 50Hz, 100 rows = 2 seconds of data
WINDOW_SIZE = 100

def process_file(filepath, label):
    """Reads a CSV, splits it into windows, and extracts features."""
    try:
        df = pd.read_csv(filepath)
    except FileNotFoundError:
        print(f"Waiting for dataset: {filepath}")
        return pd.DataFrame()

    features = []

    # Iterate through the file in chunks (windows)
    for i in range(0, len(df) - WINDOW_SIZE, WINDOW_SIZE):
        window = df.iloc[i : i + WINDOW_SIZE]

        # Separate Accel and Gyro data
        accel = window[window['Sensor'] == 'ACCEL']
        gyro = window[window['Sensor'] == 'GYRO']

        # Skip window if we didn't capture enough data
        if accel.empty or gyro.empty:
            continue

        # Extract statistical features (Mean and Standard Deviation for amplitude)
        window_features = {
            'accel_x_mean': accel['X'].mean(),
            'accel_y_mean': accel['Y'].mean(),
            'accel_z_mean': accel['Z'].mean(),
            'accel_x_std': accel['X'].std(),
            'accel_y_std': accel['Y'].std(),
            'accel_z_std': accel['Z'].std(),

            'gyro_x_mean': gyro['X'].mean(),
            'gyro_y_mean': gyro['Y'].mean(),
            'gyro_z_mean': gyro['Z'].mean(),
            'gyro_x_std': gyro['X'].std(),
            'gyro_y_std': gyro['Y'].std(),
            'gyro_z_std': gyro['Z'].std(),

            'label': label
        }
        features.append(window_features)

    return pd.DataFrame(features)

def main():
    print("Extracting features from IMU data...")

    # 2. Load and process your individual datasets
    # (Make sure you have pulled these files from your watch via ADB)
    df_sitting = process_file('sitting_data.csv', 'Sitting')
    df_walking = process_file('walking_data.csv', 'Walking')
    df_falling = process_file('falling_data.csv', 'Falling')

    # Combine all data into one master dataframe
    all_data = pd.concat([df_sitting, df_walking, df_falling], ignore_index=True)

    if all_data.empty:
        print("No data found. Please record your CSVs and place them in this folder.")
        return

    # 3. Prepare data for Machine Learning
    X = all_data.drop('label', axis=1) # The features (input)
    y = all_data['label']              # The target activity (output)

    # Split into 80% training data and 20% testing data
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    # 4. Train the Linear Support Vector Classifier
    print("\nTraining LinearSVC Model...")
    model = LinearSVC(dual=False, max_iter=10000)
    model.fit(X_train, y_train)

    # 5. Evaluate the Model
    predictions = model.predict(X_test)
    accuracy = accuracy_score(y_test, predictions)

    print(f"\nModel Accuracy: {accuracy * 100:.2f}%")
    print("\nClassification Report:")
    print(classification_report(y_test, predictions))

if __name__ == "__main__":
    main()
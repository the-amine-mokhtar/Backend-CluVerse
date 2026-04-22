import pandas as pd
import numpy as np
import json

base_path = "ml/data/"

# Load files
stop_times = pd.read_csv(base_path + "stop_times.txt")
trips = pd.read_csv(base_path + "trips.txt")
calendar = pd.read_csv(base_path + "calendar.txt")
stops = pd.read_csv(base_path + "stops.txt")

# Parse time strings to minutes since midnight
def time_to_minutes(t):
    try:
        parts = str(t).split(":")
        return int(parts[0]) * 60 + int(parts[1])
    except:
        return None

stop_times["arr_min"] = stop_times["arrival_time"].apply(time_to_minutes)
stop_times["dep_min"] = stop_times["departure_time"].apply(time_to_minutes)

# For each trip: get first stop (departure) and last stop (arrival)
# Calculate total trip duration = last arrival - first departure
trip_groups = stop_times.groupby("trip_id")

records = []
for trip_id, group in trip_groups:
    group = group.sort_values("stop_sequence")
    first = group.iloc[0]
    last = group.iloc[-1]
    
    dep_minutes = first["dep_min"]
    arr_minutes = last["arr_min"]
    
    if dep_minutes is None or arr_minutes is None:
        continue
    
    duration = arr_minutes - dep_minutes
    if duration <= 0 or duration > 300:  # filter invalid (max 5h)
        continue
    
    dep_hour = int(dep_minutes // 60) % 24
    dep_stop_id = first["stop_id"]
    arr_stop_id = last["stop_id"]
    
    records.append({
        "trip_id": trip_id,
        "dep_hour": dep_hour,
        "dep_stop_id": dep_stop_id,
        "arr_stop_id": arr_stop_id,
        "duration_minutes": duration
    })

df = pd.DataFrame(records)
print(f"Total valid trips extracted: {len(df)}")
print(df.head(10))
print(f"\nDuration stats:")
print(df["duration_minutes"].describe())

# Merge with calendar via trips to get day info
trips_cal = trips.merge(calendar, on="service_id", how="left")
df = df.merge(
    trips_cal[["trip_id","monday","tuesday","wednesday",
               "thursday","friday","saturday","sunday"]], 
    on="trip_id", 
    how="left"
)

# Create features
df["is_weekend"] = ((df["saturday"]==1) | (df["sunday"]==1)).astype(float)
df["is_peak_morning"] = ((df["dep_hour"]>=7) & (df["dep_hour"]<=9)).astype(float)
df["is_peak_evening"] = ((df["dep_hour"]>=16) & (df["dep_hour"]<=19)).astype(float)
df["is_lunch"] = ((df["dep_hour"]>=12) & (df["dep_hour"]<=14)).astype(float)
df["is_friday"] = (df["friday"]==1).astype(float)
df["is_friday_lunch"] = ((df["is_friday"]==1) & (df["is_lunch"]==1)).astype(float)

# Feature matrix
feature_cols = [
    "dep_hour", "is_weekend", "is_peak_morning", 
    "is_peak_evening", "is_lunch", "is_friday_lunch"
]

df_clean = df.dropna(subset=feature_cols + ["duration_minutes"])
print(f"\nClean records for training: {len(df_clean)}")

X = df_clean[feature_cols].values.astype(float)
y = df_clean["duration_minutes"].values.astype(float)

# Normalize
X_mean = X.mean(axis=0)
X_std = X.std(axis=0) + 1e-8
X_norm = (X - X_mean) / X_std

# Linear Regression — Normal Equation (exact solution)
X_b = np.column_stack([np.ones(len(X_norm)), X_norm])
theta = np.linalg.pinv(X_b.T @ X_b) @ X_b.T @ y

bias = theta[0]
weights = theta[1:].tolist()

print(f"\n=== MODEL TRAINED ===")
print(f"Bias: {bias:.4f}")
print(f"Weights: {weights}")

# Test predictions
test_cases = [
    ([8,  0, 1, 0, 0, 0], "Monday 8h (peak morning)"),
    ([17, 0, 0, 1, 0, 0], "Tuesday 17h (peak evening)"),
    ([13, 0, 0, 0, 1, 0], "Wednesday 13h (lunch)"),
    ([3,  0, 0, 0, 0, 0], "Sunday 3h (night)"),
    ([13, 0, 0, 0, 1, 1], "Friday 13h (friday lunch)"),
    ([10, 1, 0, 0, 0, 0], "Saturday 10h (weekend)"),
]

print("\n=== TEST PREDICTIONS ===")
for features, label in test_cases:
    f = np.array(features, dtype=float)
    f_norm = (f - X_mean) / X_std
    pred = bias + np.dot(weights, f_norm)
    pred = max(15, min(300, pred))
    print(f"{label}: {pred:.1f} min")

# Save model
model = {
    "bias": bias,
    "weights": weights,
    "feature_means": X_mean.tolist(),
    "feature_stds": X_std.tolist(),
    "features": feature_cols,
    "training_samples": len(df_clean),
    "model_type": "LINEAR_REGRESSION",
    "dataset": "GTFS Tunisia UABS Banlieue",
    "description": "Predicts transport duration in minutes"
}

output_path = "src/main/resources/traffic_model.json"
with open(output_path, "w") as f:
    json.dump(model, f, indent=2)

print(f"\n✅ Model saved to {output_path}")
print(f"Training samples: {len(df_clean)}")

import numpy as np
import json
from pathlib import Path

np.random.seed(42)
n = 3200

# Generate realistic vehicle maintenance dataset
km_since_service = np.random.randint(0, 15000, n)
days_since_service = np.random.randint(0, 365, n)
fuel_level = np.random.randint(5, 100, n)
engine_condition = np.random.randint(30, 100, n)
tire_condition = np.random.randint(20, 100, n)
brake_condition = np.random.randint(20, 100, n)
oil_level = np.random.randint(10, 100, n)
usage_pressure = np.random.beta(2.2, 2.8, n)

# Latent breakdown risk signal with interactions.
# This is generated data (not a pre-trained external model),
# but with non-linear effects to avoid a simple threshold behavior.
km_norm = km_since_service / 15000
days_norm = days_since_service / 365
fuel_norm = fuel_level / 100
engine_norm = engine_condition / 100
tire_norm = tire_condition / 100
brake_norm = brake_condition / 100
oil_norm = oil_level / 100

z = (
    -3.4
    + 1.2 * km_norm
    + 0.8 * days_norm
    + 0.9 * (1 - fuel_norm)
    + 1.5 * (1 - engine_norm)
    + 0.6 * (1 - tire_norm)
    + 1.1 * (1 - brake_norm)
    + 0.8 * (1 - oil_norm)
    + 1.2 * usage_pressure
    + 0.7 * usage_pressure * (1 - engine_norm)
    + 0.5 * usage_pressure * km_norm
    + np.random.normal(0, 0.35, n)
)

failure_prob = 1.0 / (1.0 + np.exp(-z))

# Risk in [0, 100] with realistic spread and some noise.
breakdown_risk = 100 * (0.03 + 0.92 * failure_prob) + np.random.normal(0, 3.0, n)
breakdown_risk = np.clip(breakdown_risk, 0, 100)

# Feature matrix
X = np.column_stack([
    km_norm,
    days_norm,
    fuel_norm,
    engine_norm,
    tire_norm,
    brake_norm,
    oil_norm,
    usage_pressure,
])
y = breakdown_risk

# Normalize
X_mean = X.mean(axis=0)
X_std = X.std(axis=0) + 1e-8
X_norm = (X - X_mean) / X_std

# Linear Regression — Normal Equation
X_b = np.column_stack([np.ones(n), X_norm])
theta = np.linalg.pinv(X_b.T @ X_b) @ X_b.T @ y

bias = theta[0]
weights = theta[1:].tolist()

print("=== MAINTENANCE MODEL TRAINED ===")
print(f"Bias: {bias:.4f}")
print(f"Weights: {weights}")

# Test cases
test_cases = [
    ([0.1, 0.05, 0.9, 0.9, 0.9, 0.9, 0.9, 0.1], "Good vehicle"),
    ([0.8, 0.8, 0.2, 0.4, 0.3, 0.4, 0.2, 0.8], "Bad vehicle (high usage)"),
    ([0.5, 0.5, 0.5, 0.6, 0.6, 0.6, 0.5, 0.4], "Average vehicle"),
    ([1.0, 1.0, 0.05,0.3, 0.2, 0.3, 0.1, 1.0], "Critical vehicle"),
    ([0.35, 0.3, 0.75, 0.78, 0.72, 0.75, 0.73, 0.92], "Heavily used but still healthy"),
]
print("\n=== TEST PREDICTIONS ===")
for features, label in test_cases:
    f = np.array(features)
    f_norm = (f - X_mean) / X_std
    pred = bias + np.dot(weights, f_norm)
    pred = max(0, min(100, pred))
    status = "GOOD" if pred < 30 else "WARNING" if pred < 65 else "CRITICAL"
    print(f"{label}: risk={pred:.1f}% → {status}")

model = {
    "bias": bias,
    "weights": weights,
    "feature_means": X_mean.tolist(),
    "feature_stds": X_std.tolist(),
    "features": [
        "km_since_service_norm",
        "days_since_service_norm",
        "fuel_level_norm",
        "engine_condition_norm",
        "tire_condition_norm",
        "brake_condition_norm",
        "oil_level_norm",
        "usage_pressure_norm"
    ],
    "normalization": {
        "km_since_service_max": 15000,
        "days_since_service_max": 365
    },
    "training_samples": n,
    "model_type": "LINEAR_REGRESSION",
    "purpose": "Vehicle breakdown risk prediction",
    "generated_by": "Backend-Cluverse ml/train_maintenance_model.py"
}

output_path = Path(__file__).resolve().parents[1] / "src/main/resources/maintenance_model.json"
with open(output_path, "w") as f:
    json.dump(model, f, indent=2)

print(f"\n✅ Maintenance model saved at {output_path}")

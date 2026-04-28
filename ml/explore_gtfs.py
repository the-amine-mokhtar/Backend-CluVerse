import pandas as pd

# Load files — adjust path to where you extracted the zip
base_path = "ml/data/"  

print("=== STOPS ===")
stops = pd.read_csv(base_path + "stops.txt")
print(stops.head(3))
print(f"Total stops: {len(stops)}")
print(f"Columns: {stops.columns.tolist()}")

print("\n=== STOP_TIMES ===")
stop_times = pd.read_csv(base_path + "stop_times.txt")
print(stop_times.head(3))
print(f"Total records: {len(stop_times)}")
print(f"Columns: {stop_times.columns.tolist()}")

print("\n=== TRIPS ===")
trips = pd.read_csv(base_path + "trips.txt")
print(trips.head(3))
print(f"Columns: {trips.columns.tolist()}")

print("\n=== ROUTES ===")
routes = pd.read_csv(base_path + "routes.txt")
print(routes.head(3))
print(f"Columns: {routes.columns.tolist()}")

print("\n=== CALENDAR ===")
calendar = pd.read_csv(base_path + "calendar.txt")
print(calendar.head(3))
print(f"Columns: {calendar.columns.tolist()}")

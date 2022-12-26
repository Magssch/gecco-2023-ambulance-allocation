import json
import math

import pandas as pd
from coordinate_converter import ssb_grid_id_to_utm_centroid, utm_to_longitude_latitude

utm_and_latlong = {
    "easting": [],
    "northing": [],
    "lat": [],
    "long": [],
}
od_matrix = None


def euclidean_distance_id(origin, destination):
    origin_easting, origin_northing = id_to_utm[origin]
    destination_easting, destination_northing = id_to_utm[destination]
    return math.hypot(
        destination_easting - origin_easting, destination_northing - origin_northing
    )


with open("scripts/data/od_matrix4.json", "r") as f1:
    print("Loading OD matrix...")
    od_matrix = json.load(f1)

coordinate_neighbors = {coordinate: [] for coordinate in od_matrix.keys()}
id_to_utm = {coordinate: [] for coordinate in od_matrix.keys()}

for grid_id in od_matrix.keys():
    if grid_id[0] == "_":
        id_to_utm[grid_id] = [int(coordinate) for coordinate in grid_id.split("_")[1:3]]
    else:
        id_to_utm[grid_id] = [
            int(coordinate) for coordinate in ssb_grid_id_to_utm_centroid(int(grid_id))
        ]

print("Finding latlong to utm mappings...")
for grid_id in od_matrix.keys():
    try:
        grid_id_int = int(grid_id)
        easting, northing = ssb_grid_id_to_utm_centroid(grid_id_int)
        longitude, latitude = utm_to_longitude_latitude(
            ssb_grid_id_to_utm_centroid(grid_id_int)
        )
    except ValueError:
        _, easting, northing = grid_id.split("_")
        easting = int(easting)
        northing = int(northing)
        longitude, latitude = utm_to_longitude_latitude((easting, northing))
    utm_and_latlong["easting"].append(easting)
    utm_and_latlong["northing"].append(northing)
    utm_and_latlong["lat"].append(latitude)
    utm_and_latlong["long"].append(longitude)


uncalculated_cells = set([])

print("Finding all grid neighbors...")
for grid_id in od_matrix.keys():
    for possible_neighbor in od_matrix.keys():
        if grid_id != possible_neighbor:
            coordinate_euclidean_distance = euclidean_distance_id(
                grid_id, possible_neighbor
            )
            if coordinate_euclidean_distance <= 1001:
                coordinate_neighbors[grid_id].append(possible_neighbor)
    if len(coordinate_neighbors[grid_id]) == 0:
        uncalculated_cells.add(grid_id)

print("Calculating mean of distances to neighbors...")
for grid_id in od_matrix.keys():
    if grid_id in uncalculated_cells:
        continue
    neighbors = coordinate_neighbors[grid_id]
    distances = []
    for neighbor in neighbors:
        if neighbor in od_matrix[grid_id]:
            distances.append(od_matrix[grid_id][neighbor][0] / 2)
        if grid_id in od_matrix[neighbor]:
            distances.append(od_matrix[neighbor][grid_id][0] / 2)
    if len(distances) == 0:
        uncalculated_cells.add(grid_id)
        continue
    od_matrix[grid_id][grid_id] = [
        (sum(distances) / len(distances)),
        0,
    ]

print("Calculating mean of means...")
mean_of_means = [
    od_matrix[grid_id][grid_id][0]
    for grid_id in od_matrix.keys()
    if grid_id not in uncalculated_cells and len(coordinate_neighbors[grid_id]) >= 4
]
mean_of_means = sum(mean_of_means) / len(mean_of_means)

for grid_id in uncalculated_cells:
    od_matrix[grid_id][grid_id] = [mean_of_means, 0]

with open(f"scripts/data/od_matrix5.json", "w") as f2:
    json.dump(od_matrix, f2, indent=2)

utm_and_latlong_df = pd.DataFrame(utm_and_latlong)
utm_and_latlong_df.to_csv("scripts/data/utm_and_latlong.csv", index=False, header=False)

import json
import math

import pandas as pd
from coordinate_converter import (ssb_grid_id_to_utm_centroid,
                                  utm_to_latitude_longitude,
                                  utm_to_longitude_latitude)

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


with open("scripts/data/od_matrix2.json", "r") as f1:
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
print("Finding all grid neighbors and fixing problematic pairs...")
for grid_id in od_matrix.keys():
    for possible_neighbor in od_matrix.keys():
        if grid_id != possible_neighbor:
            coordinate_euclidean_distance = euclidean_distance_id(
                grid_id, possible_neighbor
            )
            if coordinate_euclidean_distance <= 1001:
                coordinate_neighbors[grid_id].append(possible_neighbor)
                if (possible_neighbor in od_matrix[grid_id] and od_matrix[grid_id][possible_neighbor][0] > 100):
                    # print(f"Found a large distance between neighbors: {od_matrix[grid_id][possible_neighbor][0]}")
                    if grid_id in od_matrix[possible_neighbor]:
                        # if od_matrix[grid_id][possible_neighbor][0]-od_matrix[possible_neighbor][grid_id][0] > 200:
                        #     print(f"Found a large distance between neighbors: {od_matrix[grid_id][possible_neighbor][0]}")
                        if 250 > od_matrix[grid_id][possible_neighbor][0]-od_matrix[possible_neighbor][grid_id][0] > 200:
                            od_matrix[grid_id][possible_neighbor][0] = (od_matrix[grid_id][possible_neighbor][0]+od_matrix[possible_neighbor][grid_id][0])/2
                        elif od_matrix[grid_id][possible_neighbor][0]-od_matrix[possible_neighbor][grid_id][0] >= 250:
                            od_matrix[grid_id][possible_neighbor][0] = od_matrix[possible_neighbor][grid_id][0]

                        # print(f"Found a large distance between neighbors: {od_matrix[grid_id][possible_neighbor][0]}")
                        # print(f"Reverse: {od_matrix[possible_neighbor][grid_id][0]}")
                        # if "_" not in grid_id and "_" not in possible_neighbor:
                        #     print(f"Grid id: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(grid_id)))}, possible neighbor: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(possible_neighbor)))}\n")
                        # else:
                        #     print(f"grid id: {grid_id}, possible neighbor: {possible_neighbor}\n")
                    #     print(f"Reverse: {od_matrix[possible_neighbor][grid_id][0]}")
                    # print(f"Grid id: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(grid_id)))}, possible neighbor: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(possible_neighbor)))}\n")
                    # problematic[grid_id] += 1
                    # problematic[possible_neighbor] += 1
                # if (grid_id in od_matrix[possible_neighbor] and od_matrix[possible_neighbor][grid_id][0] > 100):
                #     if possible_neighbor in od_matrix[grid_id] and 250 > abs(od_matrix[grid_id][possible_neighbor][0]-od_matrix[possible_neighbor][grid_id][0]) > 200:
                #         print(f"Found a large distance between neighbors: {od_matrix[grid_id][possible_neighbor][0]}")
                #         print(f"Reverse: {od_matrix[possible_neighbor][grid_id][0]}")
                #         if "_" not in grid_id and "_" not in possible_neighbor:
                #             print(f"Grid id: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(grid_id)))}, possible neighbor: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(possible_neighbor)))}\n")
                #         else:
                #             print(f"grid id: {grid_id}, possible neighbor: {possible_neighbor}\n")
                    # print(f"Found a large distance between neighbors: {od_matrix[possible_neighbor][grid_id][0]}")
                    # if possible_neighbor in od_matrix[grid_id]:
                    #     print(f"Reverse: {od_matrix[grid_id][possible_neighbor][0]}")
                    # print(f"Grid id: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(grid_id)))}, possible neighbor: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(possible_neighbor)))}\n")
                    # problematic[grid_id] += 1
                    # problematic[possible_neighbor] += 1
    if len(coordinate_neighbors[grid_id]) < 3:
        uncalculated_cells.add(grid_id)

print(f"Uncalculated cells: {len(uncalculated_cells)} out of {len(od_matrix)}")

# print(f"Found {n} large distances between neighbors")
# print("Problematic cells:")
# for key in problematic.keys():
#     if problematic[key] > 4:
#         print(f"{key}: {problematic[key]}")

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
    if len(distances) < 3:
        uncalculated_cells.add(grid_id)
        continue
    # if len(distances) == 3:
    #     print(f"Found a cell with only three neighbors: {grid_id}:  {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(grid_id)))} { sum(distances) / len(distances)}")
    od_matrix[grid_id][grid_id] = [
        (sum(distances) / len(distances)),
        0,
    ]
    # if (sum(distances) / len(distances)) > 400:
    #     print(f"Found a large distance to neighbors: {sum(distances) / len(distances)}")
    #     print(f"Grid id: {utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(grid_id)))}\n")


print(f"Uncalculated cells: {len(uncalculated_cells)} out of {len(od_matrix)}")


print("Calculating mean of means...")
mean_of_means = [
    od_matrix[grid_id][grid_id][0]
    for grid_id in od_matrix.keys()
    if grid_id not in uncalculated_cells and len(coordinate_neighbors[grid_id]) >= 3
]
mean_of_means = sum(mean_of_means) / len(mean_of_means)
print(f"Mean of means: {mean_of_means}")

for grid_id in uncalculated_cells:
    od_matrix[grid_id][grid_id] = [mean_of_means, 0]

print("Writing to file...")
with open(f"scripts/data/od_postprocessed.json", "w") as f2:
    json.dump(od_matrix, f2, indent=2)

utm_and_latlong_df = pd.DataFrame(utm_and_latlong)
utm_and_latlong_df.to_csv("scripts/data/utm_and_latlong.csv", index=False, header=False)

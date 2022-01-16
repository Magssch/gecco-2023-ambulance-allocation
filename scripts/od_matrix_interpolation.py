
import json
import math
import sys

import numpy as np

from coordinate_converter import (ssb_grid_id_to_utm_centroid,
                                  utm_to_latitude_longitude)

manual_interpolation = {}
with open('data/uninterpolatable_nearest_junction.json', 'r') as f0:
    manual_interpolation = json.load(f0)


od_unparsed = {}
od = {}
unique_coordinates = set()
interpolation_pairs = set()
uninterpolatable_pairs = {}
with open('data/od_matrix.json', 'r') as f1:
    print("Loading OD matrix...")
    od_unparsed = json.load(f1)
    unique_coordinates = set(od_unparsed.keys())
    for origin_key in unique_coordinates:
        od[origin_key] = {}
        for coordinate in unique_coordinates:
            if coordinate not in od_unparsed[origin_key]:
                interpolation_pairs.add((origin_key, coordinate))
            else:
                od[origin_key][coordinate] = od_unparsed[origin_key][coordinate][0]


coordinate_neighbors = {coordinate: [] for coordinate in unique_coordinates}
extended_coordinate_neighbors = {coordinate: [] for coordinate in unique_coordinates}
id_to_utm = {coordinate: [] for coordinate in unique_coordinates}


def euclidean_distance(origin, destination):
    origin_easting, origin_northing = origin
    destination_easting, destination_northing = destination
    return math.hypot(destination_easting - origin_easting, destination_northing - origin_northing)


def euclidean_distance_id(origin, destination):
    origin_easting, origin_northing = id_to_utm[origin]
    destination_easting, destination_northing = id_to_utm[destination]
    return math.hypot(destination_easting - origin_easting, destination_northing - origin_northing)


def interpolate_reverse(origin, destination):
    if origin in od_unparsed[destination]:
        return od_unparsed[destination][origin][0]


def interpolate_multiple_origins(origin, destination):
    nearby_times = []
    for interpolate in coordinate_neighbors[origin]:
        if destination in od_unparsed[interpolate]:
            nearby_times.append(od_unparsed[interpolate][destination][0])
    if len(nearby_times) > 1:
        return np.mean(nearby_times)


def interpolate_multiple_origins_destinations(origin, destination):
    nearby_times = []
    for origin_interpolate in coordinate_neighbors[origin]:
        for destination_interpolate in coordinate_neighbors[destination]:
            if destination_interpolate in od_unparsed[origin_interpolate]:
                nearby_times.append(od_unparsed[origin_interpolate][destination_interpolate][0])
    if len(nearby_times) > 1:
        return np.mean(nearby_times)


def interpolate_middleman(origin, destination):
    cheapest_route = None
    for origin_interpolate in extended_coordinate_neighbors[origin]:
        if origin_interpolate in od_unparsed and origin_interpolate in od_unparsed[origin] and destination in od_unparsed[origin_interpolate]:
            interpolated_route_cost = od_unparsed[origin][origin_interpolate][0] + \
                od_unparsed[origin_interpolate][destination][0]
            if cheapest_route is None or interpolated_route_cost < cheapest_route:
                cheapest_route = interpolated_route_cost
    return cheapest_route


def interpolate_double_middleman(origin, destination):
    cheapest_route = None
    origin_interpolatable_neighbors = []
    for origin_interpolate in extended_coordinate_neighbors[origin]:
        if (origin_interpolate in od_unparsed and origin_interpolate in od_unparsed[origin]) or origin in od_unparsed[origin_interpolate]:
            origin_interpolatable_neighbors.append(origin_interpolate)

    destination_interpolatable_neighbors = []
    for destination_interpolate in extended_coordinate_neighbors[destination]:
        if (destination_interpolate in od_unparsed and destination_interpolate in od_unparsed[destination]) or destination in od_unparsed[destination_interpolate]:
            destination_interpolatable_neighbors.append(destination_interpolate)

    for origin_interpolate in origin_interpolatable_neighbors:
        for destination_interpolate in destination_interpolatable_neighbors:
            origin_route_cost = None
            if origin_interpolate in od_unparsed[origin]:
                origin_route_cost = od_unparsed[origin][origin_interpolate][0]
            elif origin in od_unparsed[origin_interpolate]:
                origin_route_cost = od_unparsed[origin_interpolate][origin][0]

            destination_route_cost = None
            if destination_interpolate in od_unparsed[destination]:
                destination_route_cost = od_unparsed[destination][destination_interpolate][0]
            elif destination in od_unparsed[destination_interpolate]:
                destination_route_cost = od_unparsed[destination_interpolate][destination][0]

            middlemen_route_cost = None
            if destination_interpolate in od_unparsed[origin_interpolate]:
                middlemen_route_cost = od_unparsed[origin_interpolate][destination_interpolate][0]
            elif origin_interpolate in od_unparsed[destination_interpolate]:
                middlemen_route_cost = od_unparsed[destination_interpolate][origin_interpolate][0]

            if middlemen_route_cost is not None:
                interpolated_route_cost = middlemen_route_cost + origin_route_cost + destination_route_cost
                if cheapest_route is None or interpolated_route_cost < cheapest_route:
                    cheapest_route = interpolated_route_cost
    return cheapest_route


def interpolate_manual(origin, destination):
    try:
        if origin in manual_interpolation:
            origin_interpolate_id = manual_interpolation[origin][0]
            origin_time_to_interpolate = manual_interpolation[origin][1]
            if od[destination].get(origin_interpolate_id, None) is not None:
                return od[destination][origin_interpolate_id] + origin_time_to_interpolate
            else:
                return od[origin_interpolate_id][destination] + origin_time_to_interpolate
        if destination in manual_interpolation:
            destination_interpolate_id = manual_interpolation[destination][0]
            destination_time_to_interpolate = manual_interpolation[destination][1]
            if od[destination_interpolate_id].get(origin, None) is not None:
                return od[destination_interpolate_id][origin] + destination_time_to_interpolate
            else:
                return od[origin][destination_interpolate_id] + destination_time_to_interpolate
    except KeyError:
        return None


def print_output(progress):
    sys.stdout.write(
        f"\rInterpolation progress: {round((progress/len(interpolation_pairs))*100,1)}% |"
        f" reverse: {interpolation_count['interpolate_reverse']} |"
        f" multiple_origins: {interpolation_count['interpolate_multiple_origins']} |"
        f" multiple_orig_dests: {interpolation_count['interpolate_multiple_origins_destinations']} |"
        f" middleman: {interpolation_count['interpolate_middleman']} |"
        f" double_middleman: {interpolation_count['interpolate_double_middleman']} |"
        f" Uninterpolatable (manual handling): {len(uninterpolatable_pairs)}")
    sys.stdout.flush()


interpolation_methods = (
    interpolate_reverse,
    interpolate_multiple_origins,
    interpolate_multiple_origins_destinations,
    interpolate_middleman,
    interpolate_double_middleman,
)

interpolation_count = {
    "interpolate_reverse": 0,
    "interpolate_multiple_origins": 0,
    "interpolate_multiple_origins_destinations": 0,
    "interpolate_middleman": 0,
    "interpolate_double_middleman": 0,
    "interpolate_manual": 0,
}


def main():
    print("Parsing UTM coordinates...")
    for grid_id in unique_coordinates:
        if grid_id[0] == "_":
            id_to_utm[grid_id] = [int(coordinate) for coordinate in grid_id.split("_")[1:3]]
        else:
            id_to_utm[grid_id] = [int(coordinate) for coordinate in ssb_grid_id_to_utm_centroid(int(grid_id))]

    print("Calculating nearest neighbors...")
    for grid_id in unique_coordinates:
        for possible_neighbor in unique_coordinates:
            if grid_id != possible_neighbor:
                coordinate_euclidean_distance = euclidean_distance_id(grid_id, possible_neighbor)
                if coordinate_euclidean_distance <= 6000:
                    coordinate_neighbors[grid_id].append(possible_neighbor)
                if coordinate_euclidean_distance <= 10000:
                    extended_coordinate_neighbors[grid_id].append(possible_neighbor)

    print("Interpolating coordinates...")
    print(f"Interpolation size: {len(interpolation_pairs)}")
    num_uninterpolatable_pairs = 0
    i = 0
    for origin, destination in interpolation_pairs:
        found_interpolation = False
        for interpolation_method in interpolation_methods:
            interpolation = interpolation_method(origin, destination)
            if interpolation is not None:
                interpolation_count[interpolation_method.__name__] += 1
                od[origin][destination] = interpolation
                found_interpolation = True
                break
        if not found_interpolation:
            origin_latlong = None
            dest_latlong = None
            try:
                origin_latlong = utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(origin)))
                dest_latlong = utm_to_latitude_longitude(ssb_grid_id_to_utm_centroid(int(destination)))
            except:
                pass
            if not origin in uninterpolatable_pairs:
                uninterpolatable_pairs[origin] = {}
            uninterpolatable_pairs[origin][destination] = [origin_latlong, dest_latlong]
            num_uninterpolatable_pairs += 1
        i += 1
        if i % 200 == 0:
            print_output(i)

    print("\nPerforming second-round manual interpolation for uninterpolatable pairs...")
    for origin, destination in interpolation_pairs:
        if origin in uninterpolatable_pairs and destination in uninterpolatable_pairs[origin]:
            od[origin][destination] = interpolate_manual(origin, destination)
            if od[origin][destination] is not None:
                del uninterpolatable_pairs[origin][destination]
                interpolation_count["interpolate_manual"] += 1

    print(
        f"Successful manual interpolations: {interpolation_count['interpolate_manual']} of {num_uninterpolatable_pairs}")

    print("Performing third-round reverse interpolation for uninterpolatable pairs...")
    for origin, destination in interpolation_pairs:
        if od[origin][destination] is None:
            od[origin][destination] = od[destination][origin]
        if od[origin][destination] is not None and origin in uninterpolatable_pairs and destination in uninterpolatable_pairs[origin]:
            del uninterpolatable_pairs[origin][destination]
            interpolation_count["interpolate_manual"] += 1

    print(
        f"Successful manual interpolations (with reversing): {interpolation_count['interpolate_manual']} of {num_uninterpolatable_pairs}")

    with open(f'data/uninterpolatable_pairs.json', 'w') as f2:
        json.dump({"pairs": uninterpolatable_pairs}, f2, indent=2)

    with open(f'data/od_matrix_interpolated.json', 'w') as f3:
        json.dump(od, f3, indent=2)

    with open('data/od_nearest_neighbors.json', 'w') as f4:
        json.dump(extended_coordinate_neighbors, f4, indent=2)


if __name__ == '__main__':
    main()

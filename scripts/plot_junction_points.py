import json

import pandas as pd

import map.map_tools as map_tools


def main() -> None:
    grid_coordinates = pd.read_csv("data/grid_centroids.csv")
    unique_coordinates = set()
    coordinate_trees = []
    i = 0
    with open('data/od_matrix.json', 'r') as f1:
        print("Loading OD matrix...")
        od = json.load(f1)
        values = od.values()
        coordinate_list = [list(value.values()) for value in values]
        for coordinate_tree in coordinate_list:
            coordinate_trees.append([])
            for _, origin_junction, destination_junction in coordinate_tree:
                coordinate_trees[i].append((tuple(origin_junction), tuple(destination_junction)))
                unique_coordinates.add(tuple(origin_junction))
                unique_coordinates.add(tuple(destination_junction))
            i += 1

    junction_map = map_tools.get_map(width=3000, height=2500, location=[59.65, 11.35], zoom_start=11)
    print("Plotting junction points...")
    for _, (_, _, lat, long) in grid_coordinates.iterrows():
        map_tools.coordinates_to_circle_marker(lat, long, radius=1, color="#ff0000").add_to(junction_map)
    for lat, long in unique_coordinates:
        map_tools.coordinates_to_circle_marker(lat, long, radius=1).add_to(junction_map)

    map_tools.export_map_with_chrome(junction_map, 'junction_map', width=3000, height=2500)

    print("Plotting origin/destination pairs...")
    for i in [0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]:
        pairs_map = map_tools.get_map(width=3000, height=2500, location=[59.86, 10.86], zoom_start=12)
        for (origin_lat, origin_long), (dest_lat, dest_long) in coordinate_trees[i]:
            map_tools.points_to_line(((origin_lat, origin_long),
                                     (dest_lat, dest_long))).add_to(pairs_map)

        map_tools.export_map_with_chrome(pairs_map, f'pairs_map_{i}', width=3000, height=2500)

    for i in [0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]:
        points_map = map_tools.get_map(width=3000, height=2500, location=[59.86, 10.86], zoom_start=12)
        print("Plotting origin/destination pairs...")
        map_tools.coordinates_to_circle_marker(*coordinate_trees[i][0][0], radius=1).add_to(points_map)
        for _, (dest_lat, dest_long) in coordinate_trees[i]:
            map_tools.coordinates_to_circle_marker(dest_lat, dest_long, color="#ff0000", radius=1).add_to(points_map)

        map_tools.export_map_with_chrome(points_map, f'points_map_{i}', width=3000, height=2500)


if __name__ == '__main__':
    main()

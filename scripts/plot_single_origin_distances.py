import json

import pandas as pd

import map.geojson_tools
import map.map_tools
import map.styles
from coordinate_converter import ssb_grid_id_to_utm_centroid


def main():
    od = {}
    with open('data/od.json', 'r') as f:
        print("Loading OD matrix...")
        od = json.load(f)
    single_od = od["22610006652000"]
    destination_list_unparsed = list(single_od.keys())
    destination_list, cost_list = [], []
    for i in range(len(destination_list_unparsed)):
        try:
            destination_list.append(int(destination_list_unparsed[i]))
            cost_list.append(single_od[destination_list_unparsed[i]])
        except ValueError:
            pass
    features = [map.geojson_tools.centroid_to_geojson_square(
        *ssb_grid_id_to_utm_centroid(destination), cost) for destination, cost in zip(destination_list, cost_list)]
    map.geojson_tools.export_features(features, 'data/grid.geojson')

    heatmap = map.map_tools.get_map()

    geojson = map.map_tools.get_geojson_items('data/grid.geojson', map.styles.get_dynamic_heatmap_style(max(cost_list)))
    geojson.add_to(heatmap)

    # Plot Ullevaal location
    points = [map.geojson_tools.centroid_to_geojson(261774, 6652003)]
    circle_marker = map.map_tools.create_circle_markers(points)[0]
    circle_marker.add_to(heatmap)

    map.map_tools.export_map_with_chrome(heatmap, "ullevaal_distances", height=1500)


if __name__ == '__main__':
    main()

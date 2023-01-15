import json

import geojson_tools
import map_tools
import pandas as pd
import styles
from coordinate_converter import ssb_grid_id_to_utm_centroid


def main():
    od = {}
    with open("data/od_matrix3.json", "r") as f:
        print("Loading OD matrix...")
        od = json.load(f)
    ullevaal = "22610006652000"
    destination_list_unparsed = list(od.keys())
    destination_list, cost_list = [], []
    for i in range(len(destination_list_unparsed)):
        try:
            destination_list.append(int(destination_list_unparsed[i]))
            cost_list.append(od[destination_list_unparsed[i]][ullevaal][0])
        except ValueError:
            pass
    features = [
        geojson_tools.centroid_to_geojson_square(
            *ssb_grid_id_to_utm_centroid(destination), cost
        )
        for destination, cost in zip(destination_list, cost_list)
    ]
    geojson_tools.export_features(features, "data/grid.geojson")

    heatmap = map_tools.get_map(height=500, width=500)

    geojson = map_tools.get_geojson_items(
        "data/grid.geojson", styles.get_dynamic_heatmap_style(max(cost_list))
    )
    geojson.add_to(heatmap)

    # Plot Ullevaal location
    points = [geojson_tools.centroid_to_geojson(261774, 6652003)]
    circle_marker = map_tools.create_circle_markers(points)[0]
    circle_marker.add_to(heatmap)

    map_tools.export_map_with_chrome(
        heatmap, "ullevaal_distances", height=1000, width=1000
    )


if __name__ == "__main__":
    main()

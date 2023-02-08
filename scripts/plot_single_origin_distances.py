import json

import geojson
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

    heatmap = map_tools.get_map(height=460, width=300, location=[59.95, 11.035])


    # base_stations = pd.read_csv('data/base_stations.csv', index_col=0, usecols=['id', 'easting', 'northing'])

    # hospitals = pd.read_csv('data/hospital_coordinates.csv', usecols=['lat', 'long']).iterrows()

    geojson = map_tools.get_geojson_items(
        "data/grid.geojson", styles.get_dynamic_heatmap_style(max(cost_list))
    )
    geojson.add_to(heatmap)

    # points = [geojson.Feature(geometry=geojson.Point(coordinates.to_list()[::-1])) for _, coordinates in hospitals]
    # markers = [map_tools._point_to_circle_marker(point, radius=1, color="red") for point in points]
    # for marker in markers:
    #     marker.add_to(heatmap)

    # points = geojson_tools.dataframe_to_points(base_stations)
    # markers = [map_tools._point_to_text_marker(point, "▲") for point in points]
    # for marker in markers:
    #     marker.add_to(heatmap)


    # Plot Ullevaal location
    points = [geojson_tools.centroid_to_geojson(261774, 6652003)]
    circle_marker = map_tools.create_circle_markers(points)[0]
    circle_marker.add_to(heatmap)

    map_tools.export_map_with_chrome(
        heatmap, "ullevaal_distances", height=840, width=600
    )


if __name__ == "__main__":
    main()

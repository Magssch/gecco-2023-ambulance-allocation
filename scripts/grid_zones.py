import pandas as pd

import geojson_tools
import map_tools
import styles


def process_grids():
    grids = pd.read_csv('data/grid_zones.csv', index_col=0)

    empty_cells = pd.read_csv('data/empty_cells.csv', encoding='utf-8', index_col=0)
    empty_cells = empty_cells[['X', 'Y']].rename(columns={'X': 'easting', 'Y': 'northing'})
    empty_cells['easting'] = empty_cells['easting'].astype(int)
    empty_cells['northing'] = empty_cells['northing'].astype(int)

    grids = grids[['easting', 'northing', 'base_station']]

    grids = pd.concat([grids, empty_cells.assign(base_station=19)])
    return grids


def process_base_stations():
    base_stations = pd.read_csv('data/base_stations.csv', encoding='utf-8', index_col=0)
    base_stations = base_stations[['easting', 'northing']]
    return base_stations


def main():
    grids = process_grids()
    base_stations = process_base_stations()

    features = geojson_tools.dataframe_to_squares(grids)
    geojson_tools.export_features(features, 'data/grid.geojson')

    heatmap = map_tools.get_map()

    geojson = map_tools.get_geojson_items('data/grid.geojson', styles.zone_styles)
    geojson.add_to(heatmap)

    points = geojson_tools.dataframe_to_points(base_stations)
    circle_markers = map_tools.create_circle_markers(points)
    for circle_marker in circle_markers:
        circle_marker.add_to(heatmap)

    map_tools.export_map_with_chrome(heatmap, "grid_zones")


if __name__ == '__main__':
    main()

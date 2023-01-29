import os
from collections import Counter

import pandas as pd

import geojson_tools
import map_tools
from common import VISUALIZATION_FOLDER, ensure_folder_exists


def process_grids():
    grids = pd.read_csv('data/grid_zones.csv', index_col=0,
                        usecols=['SSBID1000M', 'easting', 'northing', 'base_station'])
    empty_cells = pd.read_csv('data/empty_cells.csv', index_col=0,
                              usecols=['SSBID1000M', 'easting', 'northing'])

    grids = pd.concat([grids, empty_cells.assign(base_station=19)])
    return grids


def process_base_stations():
    return pd.read_csv('data/base_stations.csv', index_col=0, usecols=['id', 'easting', 'northing'])


def plot() -> None:
    print('Visualizing allocations (this could take a little while time)...')
    allocation_folder = f'{VISUALIZATION_FOLDER}/allocations'
    ensure_folder_exists(allocation_folder)

    experiments = []
    for (root, _, files) in os.walk('../output/simulation', topdown=True):
        for file in files:
            experiment_name_ending = file.split('_')[-1]
            if experiment_name_ending == 'allocations.csv':
                experiments.append(os.path.join(root, file))

    grids = process_grids()
    base_stations = process_base_stations()

    for experiment in experiments:
        allocations = pd.read_csv(experiment)
        for (strategy_name, allocation) in allocations.items():
            print(f'Visualizing {strategy_name}')
            allocation_counts = Counter(allocation.values.tolist())

            features = geojson_tools.dataframe_to_squares(grids)
            geojson_tools.export_features(features, 'data/grid.geojson')

            heatmap = map_tools.get_map(width=3000, height=2500, location=[58.7, 14.073], zoom_start=9)

            points = geojson_tools.dataframe_to_points(base_stations)
            circle_markers = map_tools.create_capacity_circle_markers(points, allocation_counts)
            for circle_marker in circle_markers:
                circle_marker.add_to(heatmap)

            file_name = f'allocations/{strategy_name}'.lower()

            map_tools.export_map_with_chrome(heatmap, file_name, width=700)
    print('Done.')


if __name__ == '__main__':
    plot()

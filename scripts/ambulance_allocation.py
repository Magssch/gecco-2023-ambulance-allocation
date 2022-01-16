import os
from collections import Counter

import pandas as pd

import geojson_tools
import map_tools
from common import VISUALIZATION_FOLDER, ensure_folder_exists


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

    for experiment in experiments:
        allocations = None
        with open(experiment, 'r') as f:
            allocations = pd.read_csv(f)
        for (strategy_name, allocation) in allocations.iteritems():
            print(f'Visualizing {strategy_name}')
            allocation_counts = Counter(allocation.values.tolist())

            grids = process_grids()
            base_stations = process_base_stations()

            features = geojson_tools.dataframe_to_squares(grids)
            geojson_tools.export_features(features, 'data/grid.geojson')

            heatmap = map_tools.get_map(width=3000, height=2500, location=[58.7, 14.073], zoom_start=9)

            points = geojson_tools.dataframe_to_points(base_stations)
            circle_markers = map_tools.create_capacity_circle_markers(points, allocation_counts)
            for circle_marker in circle_markers:
                circle_marker.add_to(heatmap)

            # experiment_name = '_'.join(experiment.split('/')[-1].split('_')[:-1])
            file_name = f'allocations/{strategy_name}'.lower()

            map_tools.export_map_with_chrome(heatmap, file_name, width=700)
    print('done.')

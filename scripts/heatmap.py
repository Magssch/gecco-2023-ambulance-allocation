import pandas as pd

import geojson_tools
import map_tools
import styles


def process_dataframe():
    incidents = pd.read_csv('proprietary_data/cleaned_data.csv', index_col=0)

    counts = incidents.groupby(['xcoor', 'ycoor'], as_index=False).size()
    counts['counts'] = counts['size']
    counts.drop(['size'], axis=1, inplace=True)

    empty_cells = pd.read_csv('data/empty_cells.csv', encoding='utf-8', index_col=0)
    empty_cells = empty_cells[['X', 'Y']].rename(columns={'X': 'xcoor', 'Y': 'ycoor'})

    counts = pd.concat([counts, empty_cells.assign(counts=0)])
    counts.sort_values('counts')

    return counts


def main():
    df = process_dataframe()

    features = geojson_tools.dataframe_to_squares(df)
    geojson_tools.export_features(features, 'data/grid.geojson')

    heatmap = map_tools.get_map()

    geojson = map_tools.get_geojson_items('data/grid.geojson', styles.heatmap_style)
    geojson.add_to(heatmap)

    map_tools.export_map(heatmap, "heatmap")


if __name__ == '__main__':
    main()

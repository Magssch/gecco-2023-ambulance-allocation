import pandas as pd

from map.geojson_tools import utm_to_latitude_longitude


def apply_utm_to_longitude_latitude(row):
    row['lat'], row['long'] = utm_to_latitude_longitude((row.xcoor, row.ycoor))
    return row


def main():
    incidents = pd.read_csv('proprietary_data/processed_data.csv', index_col=0)
    centroids = incidents.groupby(['xcoor', 'ycoor'], as_index=False).size()
    centroids = centroids.apply(apply_utm_to_longitude_latitude, axis=1)
    centroids = centroids.drop(['size'], axis=1)
    centroids.to_csv('data/grid_centroids.csv', index=False)


if __name__ == '__main__':
    main()

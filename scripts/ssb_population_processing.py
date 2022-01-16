import warnings

import numpy as np
import pandas as pd

from coordinate_converter import ssb_grid_id_to_utm_centroid

warnings.simplefilter(action='ignore', category=FutureWarning)


def add_coordinates(row):
    easting, northing = ssb_grid_id_to_utm_centroid(row['SSBID1000M'])
    row['easting'] = easting
    row['northing'] = northing
    return row


def filter_out_regions(df):
    oslo = pd.read_csv('data/oslo.csv', encoding='utf-8', index_col=4)
    akershus = pd.read_csv('data/akershus.csv', encoding='utf-8', index_col=4)
    oslo_and_akershus_data = pd.concat([oslo, akershus])

    # Create indices based on SSB grids
    oslo_and_akershus = oslo_and_akershus_data.index
    df_cells = pd.Index(df['SSBID1000M'])

    # Filter out all incident cells that are not located in Oslo and Akershus
    df = df[df_cells.isin(oslo_and_akershus)]
    return df


def main():
    df = pd.DataFrame(columns=['SSBID1000M', 'pop_tot', 'year', 'easting', 'northing'])
    for year in range(2015, 2020):
        print(year)
        population = pd.read_csv(f'./data/population/{year}.csv',
                                 delimiter=';', dtype={'SSBID1000M': int, 'pop_tot': int})
        population['year'] = year
        df = df.append(population[['SSBID1000M', 'pop_tot', 'year']])

    df = filter_out_regions(df)
    df['easting'] = np.nan
    df['northing'] = np.nan
    df = df.apply(add_coordinates, axis=1)
    df.to_csv('./data/population/oslo_akershus_2015_2019.csv', index=False)


if __name__ == '__main__':
    main()

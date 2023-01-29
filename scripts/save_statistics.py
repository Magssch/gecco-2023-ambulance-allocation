from collections import Counter

import numpy as np
import pandas as pd

from common import OUTPUT_FOLDER

NUMBER_OF_BASE_STATIONS = 19


def genotype_to_phenotype(genotype):
    phenotype = np.zeros(NUMBER_OF_BASE_STATIONS, dtype=int)
    counts = Counter(genotype.dropna())
    for base_station, count in counts.items():
        phenotype[int(base_station)] = count
    return phenotype


def save_aggregated_allocations(df: pd.DataFrame, output_file_name: str) -> None:
    df = df.apply(genotype_to_phenotype)
    df.index.name = 'base_station_id'

    df.to_csv(f'{OUTPUT_FOLDER}/{output_file_name}.csv')


def save_statistics(statistics: pd.DataFrame, output_file_name: str) -> None:
    statistics = statistics.describe().transpose()
    statistics = statistics[['min', '50%', 'mean', 'std']]
    statistics = statistics.rename({'min': 'best', '50%': 'median'}, axis=1)
    statistics = statistics.round(2)
    statistics.index.name = 'strategy'

    statistics.to_csv(f'{OUTPUT_FOLDER}/{output_file_name}.csv')

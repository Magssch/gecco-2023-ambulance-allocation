import re

import matplotlib.pyplot as plt
import pandas as pd
from common import OUTPUT_FOLDER, VISUALIZATION_FOLDER


def plot_box_plot(file_name: str, output_file_name: str) -> None:
    file = f'{OUTPUT_FOLDER}/{file_name}.csv'
    df = pd.read_csv(file)

    for column in df.columns:
        # Split long CamelCased names
        if len(column) > 20:
            # CamlCase regex
            tokens = re.sub('([A-Z][a-z]+)', r' \1', re.sub('([A-Z]+)', r' \1', column)).split()
            column_with_space = '- \n'.join(tokens)
            df = df.rename(columns={column: column_with_space})

    fig, ax = plt.subplots(figsize=(10, 7))
    ax = df.boxplot()

    ax.set_title('Performance of algorithms')
    ax.set_xlabel('algorithm')
    ax.set_ylabel('average response time / (s)')

    plt.savefig(f'{VISUALIZATION_FOLDER}/{output_file_name}')
    plt.close()

import re
from collections import Counter

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.ticker import (
    FixedFormatter,
    FixedLocator,
    FuncFormatter,
    MaxNLocator,
    MultipleLocator,
)

import geojson_tools
import map_tools
import styles
from common import (
    OUTPUT_FOLDER,
    SIMULATION_FOLDER,
    VISUALIZATION_FOLDER,
    ensure_folder_exists,
)
from coordinate_converter import ssb_grid_id_to_utm_centroid
from save_statistics import save_aggregated_allocations, save_statistics

DEFAULT_COLORS = [
    "#1f77b4",
    "#ff7f0e",
    "#2ca02c",
    "#d62728",
    "#9467bd",
    "#8c564b",
    "#e377c2",
    "#7f7f7f",
    "#bcbd22",
    "#17becf",
]
FILE_EXTENSION = ".pdf"  # '.png'


def log_tick_formatter(val, pos=None):
    return r"$10^{{{:.0f}}}$".format(val)


def multi_axis_plot(df, filename, title, xlabel, ylabel, zlabel, log_scale=False):
    fig = plt.figure()
    fig.set_size_inches(8, 8)
    ax = plt.axes(projection="3d")
    ax.view_init(30, 45)
    Z = (
        df.average_response_time
        if not log_scale
        else np.log10(df.average_response_time)
    )
    ax.plot_trisurf(
        df.num_ambulances_day,
        df.num_ambulances_night,
        Z,
        linewidth=0,
        cmap="magma",
        antialiased=False,
    )
    ax.zaxis.set_major_formatter(FuncFormatter(log_tick_formatter))
    ax.set_xlabel(xlabel)
    ax.set_ylabel(ylabel)
    ax.set_zlabel(zlabel)
    ax.set_title(title)
    plt.savefig(f"{VISUALIZATION_FOLDER}/{filename}{FILE_EXTENSION}")
    plt.close()


def index_plot(
        df,
        filename,
        title,
        xlabel,
        ylabel,
        y_bottom=None,
        y_top=None,
        x_left=None,
        log_scale=False,
        legend=True,
):
    color = 0
    _, ax = plt.subplots()
    for col in df.columns:
        df.plot(
            use_index=True,
            y=col,
            logy=log_scale,
            label=col,
            color=DEFAULT_COLORS[color],
            ax=ax,
        )
        color += 1
    if not legend:
        ax.get_legend().remove()

    ax.xaxis.set_major_locator(MultipleLocator(10))
    plt.ylim(bottom=y_bottom, top=y_top)
    plt.xlim(left=x_left)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.savefig(f"{VISUALIZATION_FOLDER}/{filename}{FILE_EXTENSION}")
    plt.close()


def rank_plot(df, filename, log_scale=False):
    last_row = df.iloc[-1]
    last_row = last_row.sort_values(axis=0)
    sorting = [model for model in last_row.index.tolist()]
    df = df[sorting]
    models = ["\n" + model + "\n" for model in sorting]
    columns = df.columns.tolist()
    df = df.rename(columns={column: model for column, model in zip(columns, models)})
    color = 0
    _, ax = plt.subplots(figsize=(11, 7.65))
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    for col in df.columns:
        df.plot(
            use_index=True,
            y=col,
            logy=log_scale,
            label=col,
            color=DEFAULT_COLORS[color],
            ax=ax,
        )
        # plt.axhline(y=df[col].median(), linestyle='--', c=DEFAULT_COLORS[color])
        color += 1
    # plt.tight_layout()
    # plt.ylim(bottom=0.3)
    plt.title("Allocation performance")
    plt.xlabel("Timespan", labelpad=15)
    plt.ylabel("Rank")
    plt.gca().invert_yaxis()
    box = ax.get_position()
    ax.set_position([box.x0, box.y0, box.width, box.height])

    # Put a legend to the right of the current axis
    ax.legend(frameon=False, loc="center left", bbox_to_anchor=(1.03, 0.5))

    plt.savefig(f"{VISUALIZATION_FOLDER}/{filename}{FILE_EXTENSION}")
    plt.close()


def regular_plot(df: pd.DataFrame, output_file_name: str, log_scale=False) -> None:
    df = df.drop(columns="coords")
    df["timestamp"] = pd.to_datetime(df["timestamp"], dayfirst=True)

    color = 0
    _, ax = plt.subplots()
    for col in df.columns:
        if col == "timestamp":
            continue
        df.plot.scatter(
            x="timestamp",
            y=col,
            logy=log_scale,
            label=col,
            color=DEFAULT_COLORS[color],
            ax=ax,
        )
        plt.axhline(y=df[col].median(), linestyle="--", c=DEFAULT_COLORS[color])
        color += 1

    plt.ylim(bottom=1)
    plt.gcf().autofmt_xdate()
    plt.title("Response times")
    plt.xlabel("time")
    plt.ylabel("response time / (s)")
    plt.savefig(f"{VISUALIZATION_FOLDER}/{output_file_name}{FILE_EXTENSION}")
    plt.close()


def sorted_plot(df: pd.DataFrame, output_file_name: str, log_scale=False, zoom=False):
    df = df.drop(columns="coords")
    df = df.drop(["timestamp"], axis=1)

    for col in df:
        df[col] = df[col].sort_values(ignore_index=True)

    df["percentage"] = df.index / df.index.size
    if zoom:
        df = df[(df["percentage"] >= 0.25) & (df["percentage"] <= 0.75)]

    ax = df.plot(x="percentage", logy=log_scale)
    plt.axvline(x=0.5, linestyle="--", c="grey")

    if zoom:
        plt.xlim((0.25, 0.75))
        ax.set_xticks([0.25, 0.5, 0.75])
    else:
        ax.set_xticks([0.0, 0.25, 0.5, 0.75, 1.0])

    df.drop(["percentage"], axis=1)

    plt.title("Response time distribution")
    plt.ylabel("response time / (s)")
    plt.legend()
    ax.figure.savefig(f"{VISUALIZATION_FOLDER}/{output_file_name}{FILE_EXTENSION}")
    plt.close()


def plot_box_plot(df: pd.DataFrame, output_file_name: str) -> None:
    for column in df.columns:
        # Split long CamelCased names
        if len(column) > 20:
            # CamelCase regex
            tokens = re.sub('([A-Z][a-z]+)', r' \1', re.sub('([A-Z]+)', r' \1', column)).split()
            column_with_space = '- \n'.join(tokens)
            df = df.rename(columns={column: column_with_space})

    fig, ax = plt.subplots(figsize=(10, 7))
    ax = df.boxplot()

    ax.set_title('Performance of algorithms')
    ax.set_xlabel('algorithm')
    ax.set_ylabel('average response time / (s)')

    plt.savefig(f'{VISUALIZATION_FOLDER}/{output_file_name}{FILE_EXTENSION}')
    plt.close()


def visualize_sls_run(df: pd.DataFrame, output_file_name: str) -> None:
    name = output_file_name.split("/")[-1]

    fig, ax = plt.subplots()
    ax.plot(df.index, df['current'], label="current")
    ax.plot(df.index, df['best'], label="best")

    locators = []
    for i, (t, f) in enumerate(zip(df['tries'], df['flips'])):
        if t != 0.0 and f == 0.0:
            locators.append(i)
            ax.axvline(x=i, linestyle="--", c="k")

    ax.set_title(name.upper())
    ax.set_xlabel("tries")
    ax.set_ylabel("fitness")

    ax.xaxis.set_major_locator(FixedLocator(locators))
    ax.xaxis.set_major_formatter(FixedFormatter(list(df.index + 1)))
    ax.xaxis.set_minor_locator(MultipleLocator(1))

    plt.legend()

    plt.savefig(f"{VISUALIZATION_FOLDER}/{output_file_name}{FILE_EXTENSION}")
    plt.close()


def visualize_ga_run(df: pd.DataFrame, output_file_name: str) -> None:
    algorithm = output_file_name.split("/")[-1]
    fig, ax1 = plt.subplots()
    ax1.set_title(algorithm.upper())

    color = "tab:blue"
    ax1.set_xlabel("generation")
    ax1.set_ylabel("average fitness", color=color)
    ax1.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax1.plot(df['average'], color=color)
    ax1.tick_params(axis="y", labelcolor=color)
    ax1.plot(df['best'], linestyle="--", color="black", label="elite")

    ax2 = ax1.twinx()

    color = "tab:orange"
    ax2.set_ylabel("entropy", color=color)
    ax2.plot(df['diversity'], color=color)
    ax2.tick_params(axis="y", labelcolor=color)

    fig.tight_layout()
    ax1.legend()
    plt.savefig(f"{VISUALIZATION_FOLDER}/{output_file_name}{FILE_EXTENSION}")
    plt.close()


def combine_files(output_file_name, left_file_name, right_file_name, should_merge=True):
    left_df = pd.read_csv(f"{SIMULATION_FOLDER}/{left_file_name}.csv")
    right_df = pd.read_csv(f"{SIMULATION_FOLDER}/{right_file_name}.csv")

    if should_merge:
        df = pd.merge(left_df, right_df, on="timestamp")
    else:
        df = pd.concat([left_df, right_df], axis=1)
    df.to_csv(f"{OUTPUT_FOLDER}/{output_file_name}.csv", index=False)


def visualize_geographic_response_time_distribution(df: pd.DataFrame, output_file_name: str, method_name: str) -> None:
    df_agg = df.groupby(["coords"]).mean(numeric_only=True).reset_index()
    grid_list = df_agg["coords"].tolist()
    cost_list = df_agg[method_name].tolist()
    features = [
        geojson_tools.centroid_to_geojson_square(
            *ssb_grid_id_to_utm_centroid(destination), cost
        )
        for destination, cost in zip(grid_list, cost_list)
    ]
    geojson_tools.export_features(features, "data/grid.geojson")

    heatmap = map_tools.get_map(height=450, width=380)

    geojson = map_tools.get_geojson_items("data/grid.geojson", styles.get_dynamic_heatmap_style(max(cost_list)))
    geojson.add_to(heatmap)

    file_name = f'{output_file_name}_{method_name}'.lower()
    map_tools.export_map_with_chrome(heatmap, file_name, height=450, width=380)


def load_grid_zones() -> pd.DataFrame:
    grids = pd.read_csv('data/grid_zones.csv', index_col=0,
                        usecols=['SSBID1000M', 'easting', 'northing', 'base_station'])
    empty_cells = pd.read_csv('data/empty_cells.csv', index_col=0,
                              usecols=['SSBID1000M', 'easting', 'northing'])

    grids = pd.concat([grids, empty_cells.assign(base_station=19)])
    return grids


def load_base_stations() -> pd.DataFrame:
    return pd.read_csv('data/base_stations.csv', index_col=0, usecols=['id', 'easting', 'northing'])


def allocation_plot(df: pd.DataFrame, output_file_name: str) -> None:
    print('Visualizing allocations (this could take a little while)...')
    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/{output_file_name}")

    grids = load_grid_zones()
    base_stations = load_base_stations()

    for (strategy_name, allocation) in df.items():
        print(f'Visualizing {strategy_name}')
        allocation_counts = Counter(allocation.values.tolist())

        features = geojson_tools.dataframe_to_squares(grids)
        geojson_tools.export_features(features, 'data/grid.geojson')

        heatmap = map_tools.get_map(width=3000, height=2500, location=[58.7, 14.073], zoom_start=9)

        points = geojson_tools.dataframe_to_points(base_stations)
        circle_markers = map_tools.create_capacity_circle_markers(points, allocation_counts)
        for circle_marker in circle_markers:
            circle_marker.add_to(heatmap)

        file_name = f'{output_file_name}/{strategy_name}'.lower()
        map_tools.export_map_with_chrome(heatmap, file_name, width=700)
    print('Done.')


def visualize_first_experiment(include_allocations=False) -> None:
    print("Visualizing first experiment data...")
    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/first_experiment")
    file = f"{SIMULATION_FOLDER}/first_experiment_response_times.csv"
    df = pd.read_csv(file)

    visualize_geographic_response_time_distribution(df, "first_experiment/geographic_distribution",
                                                    "PopulationProportionate")

    regular_plot(df, "first_experiment/response_times")
    regular_plot(df, "first_experiment/response_times_log", log_scale=True)

    sorted_plot(df, "first_experiment/response_times_sorted")
    sorted_plot(df, "first_experiment/response_times_sorted_log", log_scale=True)
    sorted_plot(
        df, "first_experiment/response_times_sorted_log_zoom", log_scale=True, zoom=True
    )

    runs = pd.read_csv(f"{SIMULATION_FOLDER}/first_experiment_runs.csv")
    plot_box_plot(runs, f"first_experiment/runs")
    save_statistics(runs, "first_experiment_run_statistics")

    allocations = pd.read_csv(f"{SIMULATION_FOLDER}/first_experiment_allocations.csv")
    save_aggregated_allocations(allocations, "first_experiment_phenotypes")

    sls_run = pd.read_csv(f"{SIMULATION_FOLDER}/first_experiment_fsls.csv")
    visualize_sls_run(sls_run, "first_experiment/sls")
    ga_run = pd.read_csv(f"{SIMULATION_FOLDER}/first_experiment_ga.csv")
    visualize_ga_run(ga_run, "first_experiment/ga")
    ma_run = pd.read_csv(f"{SIMULATION_FOLDER}/first_experiment_ma.csv")
    visualize_ga_run(ma_run, "first_experiment/ma")

    if include_allocations:
        allocation_plot(allocations, "first_experiment/allocations")

    print("Done.")


def visualize_third_experiment():
    def file_path(sub_experiment):
        return (
            f"{SIMULATION_FOLDER}/third_experiment_response_times_{sub_experiment}.csv"
        )

    print("Visualizing third experiment data...")
    sub_experiments = {
        "One week": "one_week",
        "Two weeks": "two_weeks",
        "One month": "one_month",
        "Three months": "three_months",
        "One year": "one_year",
    }
    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/third_experiment")

    strategies = pd.read_csv(file_path(list(sub_experiments.values())[0])).columns
    statistics = pd.DataFrame(columns=["timespan", *strategies])
    statistics.set_index("timespan", inplace=True)

    for sub_experiment in sub_experiments:
        df = pd.read_csv(file_path(sub_experiments.get(sub_experiment)))
        statistics.loc[sub_experiment, df.columns] = df.mean()

    index_plot(
        statistics,
        "third_experiment/response_times",
        title="Average response times",
        xlabel="Timespan",
        ylabel="response time / (s)",
        y_bottom=800,
    )
    rank_plot(
        statistics.rank(1, ascending=True, method="first"),
        "third_experiment/model_rank",
    )

    print("done.")


def visualize_fourth_experiment():
    print("Visualizing fourth experiment data...")

    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/fourth_experiment")
    file = f"{SIMULATION_FOLDER}/fourth_experiment_average_response_times_ratio.csv"
    df = pd.read_csv(file)
    df.set_index("num_ambulances", inplace=True)
    index_plot(
        df,
        "fourth_experiment/average_response_times_ratio_log",
        title="Average response times",
        xlabel="Number of day ambulances",
        ylabel="average response time / (s)",
        log_scale=True,
    )

    index_plot(
        df,
        "fourth_experiment/average_response_times_ratio",
        title="Average response times",
        xlabel="Number of day ambulances",
        ylabel="average response time / (s)",
        y_bottom=0,
        y_top=5000,
    )

    file = f"{SIMULATION_FOLDER}/fourth_experiment_average_response_times_all.csv"
    df = pd.read_csv(file)
    df = df[(df.num_ambulances_day >= 5) & (df.num_ambulances_night >= 5)]
    multi_axis_plot(
        df,
        "fourth_experiment/average_response_times_all",
        title="Average response times",
        xlabel="No. day ambulances",
        ylabel="No. night ambulances",
        zlabel="average response time / (s)",
        log_scale=True,
    )
    print("done.")


def configure_matplotlib() -> None:
    font = {"family": "serif", "size": 14}
    figure = {
        "autolayout": True,
    }
    matplotlib.rc("font", **font)
    matplotlib.rc("figure", **figure)

    if FILE_EXTENSION == ".pdf":
        matplotlib.use("PDF")
    print(f"matplotlib backend: {matplotlib.get_backend()}")


def main() -> None:
    configure_matplotlib()

    visualize_first_experiment(include_allocations=False)
    # visualize_third_experiment()
    # visualize_fourth_experiment()

    # ambulance_allocation.plot()


if __name__ == "__main__":
    main()

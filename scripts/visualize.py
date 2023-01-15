import ambulance_allocation
import geojson_tools
import map_tools
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import styles
from common import (
    OUTPUT_FOLDER,
    SIMULATION_FOLDER,
    VISUALIZATION_FOLDER,
    ensure_folder_exists,
)
from coordinate_converter import ssb_grid_id_to_utm_centroid
from matplotlib.ticker import (
    FixedFormatter,
    FixedLocator,
    FuncFormatter,
    MaxNLocator,
    MultipleLocator,
)
from plots.box_plot import plot_box_plot
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


def regular_plot(df, filename, log_scale=False):
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
    plt.savefig(f"{VISUALIZATION_FOLDER}/{filename}{FILE_EXTENSION}")
    plt.close()


def sorted_plot(df, filename, log_scale=False, zoom=False):
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
    ax.figure.savefig(f"{VISUALIZATION_FOLDER}/{filename}{FILE_EXTENSION}")
    plt.close()


def visualize_sls_run(filename: str):
    name = filename.split("_")[-1]
    file = f"{SIMULATION_FOLDER}/{filename}.csv"
    tries, flips, current, best = np.loadtxt(
        file, unpack=True, skiprows=1, delimiter=","
    )

    x = np.arange(len(tries))

    fig, ax = plt.subplots()
    ax.plot(x, current, label="current")
    ax.plot(x, best, label="best")

    locators = []
    for i, (t, f) in enumerate(zip(tries, flips)):
        if t != 0.0 and f == 0.0:
            locators.append(i)
            ax.axvline(x=i, linestyle="--", c="k")

    ax.set_title(name.upper())
    ax.set_xlabel("tries")
    ax.set_ylabel("fitness")

    ax.xaxis.set_major_locator(FixedLocator(locators))
    ax.xaxis.set_major_formatter(FixedFormatter(list(x + 1)))
    ax.xaxis.set_minor_locator(MultipleLocator(1))

    plt.legend()

    fig.savefig(f"{VISUALIZATION_FOLDER}/second_experiment/{name}{FILE_EXTENSION}")
    plt.close()


def visualize_ga_run(filename: str):
    algorithm = filename.split("_")[-1]
    file = f"{SIMULATION_FOLDER}/{filename}.csv"
    best, average, entropy = np.loadtxt(file, unpack=True, skiprows=1, delimiter=",")
    fig, ax1 = plt.subplots()
    ax1.set_title(algorithm.upper())

    color = "tab:blue"
    ax1.set_xlabel("generation")
    ax1.set_ylabel("average fitness", color=color)
    ax1.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax1.plot(average, color=color)
    ax1.tick_params(axis="y", labelcolor=color)
    ax1.plot(best, linestyle="--", color="black", label="elite")

    ax2 = ax1.twinx()

    color = "tab:orange"
    ax2.set_ylabel("entropy", color=color)
    ax2.plot(entropy, color=color)
    ax2.tick_params(axis="y", labelcolor=color)

    fig.tight_layout()
    ax1.legend()
    fig.savefig(f"{VISUALIZATION_FOLDER}/second_experiment/{algorithm}{FILE_EXTENSION}")
    plt.close()


def comparison_plot():
    file = f"{SIMULATION_FOLDER}/second_experiment_best_fitness_at_termination.csv"
    df = pd.read_csv(file)
    n_bins = 5

    fig, ax = plt.subplots()
    ax.hist(df, n_bins, label=df.columns)

    ax.set_title("Performance of algorithms")
    ax.set_xlabel("best fitness at termination")
    ax.set_ylabel("runs ending with this fitness")

    fig.tight_layout()
    ax.legend()
    fig.savefig(
        f"{VISUALIZATION_FOLDER}/second_experiment/comparison_histogram{FILE_EXTENSION}"
    )
    plt.close()


def combine_files(output_file_name, left_file_name, right_file_name, should_merge=True):
    left_df = pd.read_csv(f"{SIMULATION_FOLDER}/{left_file_name}.csv")
    right_df = pd.read_csv(f"{SIMULATION_FOLDER}/{right_file_name}.csv")

    if should_merge:
        df = pd.merge(left_df, right_df, on="timestamp")
    else:
        df = pd.concat([left_df, right_df], axis=1)
    df.to_csv(f"{OUTPUT_FOLDER}/{output_file_name}.csv", index=False)


def visualize_geographic_response_time_distribution(df, method_name):
    df_agg = df.groupby("coords").mean().reset_index()
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

    geojson = map_tools.get_geojson_items(
        "data/grid.geojson", styles.get_dynamic_heatmap_style(max(cost_list))
    )
    geojson.add_to(heatmap)

    map_tools.export_map_with_chrome(
        heatmap, f"{method_name}_response_times", height=900, width=760
    )


def visualize_first_experiment():
    print("Visualizing first experiment data...")
    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/first_experiment")
    file = f"{SIMULATION_FOLDER}/first_experiment_response_times.csv"
    df = pd.read_csv(file)

    visualize_geographic_response_time_distribution(df, "PopulationProportionate")
    df.drop(columns="coords", inplace=True)

    regular_plot(df, "first_experiment/response_times")
    regular_plot(df, "first_experiment/response_times_log", log_scale=True)

    df.drop(["timestamp"], axis=1, inplace=True)  # not needed in the latter plots

    sorted_plot(df, "first_experiment/response_times_sorted")
    sorted_plot(df, "first_experiment/response_times_sorted_log", log_scale=True)
    sorted_plot(
        df, "first_experiment/response_times_sorted_log_zoom", log_scale=True, zoom=True
    )

    plot_box_plot(
        "simulation/first_experiment_distribution",
        f"first_experiment/distribution_box_plot{FILE_EXTENSION}",
    )

    save_aggregated_allocations(
        "first_experiment_allocations", "first_experiment_allocations"
    )
    save_statistics("first_experiment_distribution", "first_experiment_statistics")

    print("done.")


def visualize_second_experiment():
    print("Visualizing second experiment data...")
    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/second_experiment")
    file = f"{SIMULATION_FOLDER}/second_experiment_response_times.csv"
    df = pd.read_csv(file)

    regular_plot(df, "second_experiment/response_times")
    regular_plot(df, "second_experiment/response_times_log", log_scale=True)

    df.drop(["timestamp"], axis=1, inplace=True)  # not needed in the latter plots

    sorted_plot(df, "second_experiment/response_times_sorted")
    sorted_plot(df, "second_experiment/response_times_sorted_log", log_scale=True)
    sorted_plot(
        df,
        "second_experiment/response_times_sorted_log_zoom",
        log_scale=True,
        zoom=True,
    )

    visualize_sls_run("second_experiment_sls")
    visualize_ga_run("second_experiment_ga")
    visualize_ga_run("second_experiment_ma")

    comparison_plot()
    plot_box_plot(
        "simulation/second_experiment_best_fitness_at_termination",
        f"second_experiment/distribution_box_plot{FILE_EXTENSION}",
    )

    save_statistics(
        "second_experiment_best_fitness_at_termination", "second_experiment_statistics"
    )
    save_aggregated_allocations(
        "second_experiment_allocations", "second_experiment_allocations"
    )

    print("done.")


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


def visualize_first_and_second():
    print("Visualizing first and second experiment data...")
    ensure_folder_exists(f"{VISUALIZATION_FOLDER}/first_and_second_experiment")
    file = f"{OUTPUT_FOLDER}/first_and_second_experiment.csv"
    df = pd.read_csv(file)

    regular_plot(df, "first_and_second_experiment/response_times")
    regular_plot(df, "first_and_second_experiment/response_times_log", log_scale=True)

    df.drop(["timestamp"], axis=1, inplace=True)  # not needed in the latter plots

    sorted_plot(df, "first_and_second_experiment/response_times_sorted")
    sorted_plot(
        df, "first_and_second_experiment/response_times_sorted_log", log_scale=True
    )
    sorted_plot(
        df,
        "first_and_second_experiment/response_times_sorted_log_zoom",
        log_scale=True,
        zoom=True,
    )

    plot_box_plot(
        "first_and_second_experiment_distribution",
        f"first_and_second_experiment/distribution_box_plot{FILE_EXTENSION}",
    )
    save_statistics(
        "../first_and_second_experiment_distribution",
        "first_and_second_experiment_statistics",
    )
    print("done.")


def configure_matplotlib():
    font = {"family": "serif", "size": 14}
    figure = {
        "autolayout": True,
    }
    matplotlib.rc("font", **font)
    matplotlib.rc("figure", **figure)

    if FILE_EXTENSION == ".pdf":
        matplotlib.use("PDF")
    print(f"matplotlib backend: {matplotlib.get_backend()}")


def main():
    configure_matplotlib()

    # combine_files(
    #     "first_and_second_experiment",
    #     "first_experiment_response_times",
    #     "second_experiment_response_times",
    # )
    # combine_files(
    #     "first_and_second_experiment_distribution",
    #     "first_experiment_distribution",
    #     "second_experiment_best_fitness_at_termination",
    #     False,
    # )

    visualize_first_experiment()
    visualize_second_experiment()
    visualize_third_experiment()
    visualize_fourth_experiment()

    visualize_first_and_second()

    ambulance_allocation.plot()


if __name__ == "__main__":
    main()

import os

import pandas as pd
from common import (SIMULATION_FOLDER, VISUALIZATION_FOLDER,
                    ensure_folder_exists)
from save_statistics import save_aggregated_allocations, save_statistics
from visualize import (allocation_plot, plot_box_plot, regular_plot,
                       sorted_plot, visualize_fourth_experiment,
                       visualize_ga_run,
                       visualize_geographic_response_time_distribution,
                       visualize_sls_run)


def split_result_name(result_name: str) -> tuple[str, str]:
    index = result_name.index("experiment") + len("experiment")
    experiment_name = result_name[:index]
    result_type = result_name[index + 1:]
    return experiment_name, result_type


def get_visualization_name(result_name: str, folder_name: str = None, suffix: str = None) -> str:
    visualization_path = ""

    if folder_name is not None:
        visualization_path += folder_name
        visualization_path += "/"

    visualization_path += result_name

    if suffix is not None:
        visualization_path += "_"
        visualization_path += suffix

    return visualization_path


def collect_experiment_files() -> list[str]:
    output_files = os.listdir(SIMULATION_FOLDER)
    csv_files = list(filter(lambda f: f.endswith('.csv'), output_files))
    return csv_files


def visualize_results(experiment_files: list[str], include_allocations=False) -> None:
    for result_file_name in experiment_files:
        result_path = os.path.join(SIMULATION_FOLDER, result_file_name)
        result_name, _ = result_file_name.rsplit('.', maxsplit=1)
        experiment_name, result_type = split_result_name(result_name)

        ensure_folder_exists(f"{VISUALIZATION_FOLDER}/{experiment_name}")
        df = pd.read_csv(result_path)

        if result_type == "response_times" and "new_third" not in result_file_name:
            visualize_geographic_response_time_distribution(
                df,
                get_visualization_name('geographic_distribution', experiment_name),
                "PopulationProportionate"
            )

            regular_plot(df, get_visualization_name(result_type, experiment_name))
            regular_plot(df, get_visualization_name(result_type, experiment_name, "log"), log_scale=True)

            sorted_plot(df, get_visualization_name(result_type, experiment_name, "sorted"))
            sorted_plot(df, get_visualization_name(result_type, experiment_name, "sorted_log"), log_scale=True)
            sorted_plot(df, get_visualization_name(result_type, experiment_name, "sorted_log_zoom"), log_scale=True,
                        zoom=True)
        elif result_type == "allocations":
            if include_allocations:
                allocation_plot(df, "first_experiment/allocations")
            save_aggregated_allocations(df, get_visualization_name(experiment_name, suffix="phenotypes"))
        elif result_type == "runs":
            plot_box_plot(df, get_visualization_name(result_type, experiment_name))
            save_statistics(df, get_visualization_name(experiment_name, suffix="run_statistics"))
        elif "sls" in result_type:
            visualize_sls_run(df, get_visualization_name(result_type, experiment_name))
        elif "ga" in result_type or "ma" in result_type:
            visualize_ga_run(df, get_visualization_name(result_type, experiment_name))
        else:
            print(f'Could not find a plot for {result_name}')


def main():
    experiment_files = collect_experiment_files()
    visualize_results(experiment_files, include_allocations=False)
    visualize_fourth_experiment()


if __name__ == '__main__':
    main()

import os
import pandas as pd
from common import SIMULATION_FOLDER, VISUALIZATION_FOLDER, ensure_folder_exists
from visualize import regular_plot, sorted_plot, visualize_sls_run, visualize_ga_run


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
    print('Collecting experiment files...')
    output_files = os.listdir(SIMULATION_FOLDER)
    csv_files = list(filter(lambda f: f.endswith('.csv'), output_files))
    print('Done.')
    return csv_files


def visualize_ma_run(param):
    pass


def visualize_results(experiment_files: list[str]) -> None:
    for result_file_name in experiment_files:
        result_path = os.path.join(SIMULATION_FOLDER, result_file_name)
        result_name, _ = result_file_name.rsplit('.', maxsplit=1)

        df = pd.read_csv(result_path)
        match result_name.split("_"):
            case [experiment_name, 'experiment', *result_type] if result_type == ['response', 'times']:
                ensure_folder_exists(f"{VISUALIZATION_FOLDER}/{experiment_name}")

                regular_plot(df, get_visualization_name(result_name, experiment_name))
                regular_plot(df, get_visualization_name(result_name, experiment_name, "log"), log_scale=True)

                df.drop(["timestamp"], axis=1, inplace=True)  # not needed in the latter plots
                sorted_plot(df, get_visualization_name(result_name, experiment_name))
                sorted_plot(df, get_visualization_name(result_name, experiment_name, "log"), log_scale=True)
            case [experiment_name, 'experiment', algorithm, additional_info] if "sls" in algorithm:
                visualize_sls_run(get_visualization_name(algorithm, experiment_name, additional_info))
            case [experiment_name, 'experiment', algorithm, additional_info] if "ga" in algorithm:
                visualize_ga_run(get_visualization_name(algorithm, experiment_name, additional_info))
            case [experiment_name, 'experiment', algorithm, additional_info] if "ma" in algorithm:
                visualize_ma_run(get_visualization_name(algorithm, experiment_name, additional_info))
            case no_match:
                print(f'Could not find a plot for {no_match}')


def main():
    experiment_files = collect_experiment_files()
    visualize_results(experiment_files)


if __name__ == '__main__':
    main()

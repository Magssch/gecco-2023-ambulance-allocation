import os

OUTPUT_FOLDER = '../output'
SIMULATION_FOLDER = f'{OUTPUT_FOLDER}/simulation'
VISUALIZATION_FOLDER = f'{OUTPUT_FOLDER}/visualization'


def ensure_folder_exists(folder):
    if not os.path.exists(folder):
        os.makedirs(folder)


ensure_folder_exists(SIMULATION_FOLDER)
ensure_folder_exists(VISUALIZATION_FOLDER)

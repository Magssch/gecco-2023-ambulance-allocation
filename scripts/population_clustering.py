import math

POPULATION_STATISTICS_FILE = 'data/population/oslo_akershus_2015_2019.csv'
GRID_OUTPUT_FILE = 'data/grid_zones.csv'
BASE_STATIONS_INPUT_FILE = 'data/base_stations.csv'
BASE_STATIONS_OUTPUT_FILE = 'data/base_stations_with_population.csv'
YEAR_OF_INTEREST = '2019'


def load_population_statistics():
    population_statistics = []

    with open(POPULATION_STATISTICS_FILE, 'r') as f:
        header = f.readline().rstrip().split(',')

        for line in f.readlines():
            line = line.rstrip().split(',')
            line = dict(zip(header, line))
            line['easting'] = int(float(line['easting']))
            line['northing'] = int(float(line['northing']))
            line['pop_tot'] = int(line['pop_tot'])

            if line['year'] == YEAR_OF_INTEREST:
                population_statistics.append(list(line.values()))

    return header, population_statistics


def load_base_stations():
    base_stations = []

    with open(BASE_STATIONS_INPUT_FILE, 'r') as f:
        header = f.readline().rstrip().split(',')

        for line in f.readlines():
            line = line.rstrip().split(',')
            line = dict(zip(header, line))
            line['easting'] = int(line['easting'])
            line['northing'] = int(line['northing'])
            base_stations.append(list(line.values()) + [0])

    return header, base_stations


def save_grid_zones(header, grid_zones):
    with open(GRID_OUTPUT_FILE, 'w') as f:
        f.write(','.join(header) + '\n')

        for i, grid_zone in enumerate(grid_zones):
            line = ','.join(str(value) for value in grid_zone)

            if i < len(grid_zones) - 1:
                line += '\n'

            f.write(line)


def save_base_stations(header, base_stations):
    with open(BASE_STATIONS_OUTPUT_FILE, 'w') as f:
        f.write(','.join(header) + '\n')

        for i, base_station in enumerate(base_stations):
            line = ','.join(str(value) for value in base_station)

            if i < len(base_stations) - 1:
                line += '\n'

            f.write(line)


def main():
    base_station_header, base_stations = load_base_stations()
    grid_zone_header, population_statistics = load_population_statistics()

    for grid_cell in population_statistics:

        grid_cell_location = grid_cell[-2:]
        grid_cell_population = grid_cell[1]

        minimum_distance = float('inf')
        nearest_base_station = None

        for i, base_station in enumerate(base_stations):

            base_station_location = base_station[-3:-1]
            distance = math.dist(base_station_location, grid_cell_location)

            if distance < minimum_distance:
                minimum_distance = distance
                nearest_base_station = i

        base_stations[nearest_base_station][-1] += grid_cell_population
        grid_cell.append(nearest_base_station)

    save_base_stations(base_station_header + ['population'], base_stations)
    save_grid_zones(grid_zone_header + ['base_station'], population_statistics)


if __name__ == '__main__':
    main()

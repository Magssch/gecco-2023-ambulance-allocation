import json
import os
import pathlib
from platform import node
from time import time

import numpy as np
import pandas as pd
import proprietary_scripts.ferd.routeplanner as ferd
import psycopg2

from coordinate_converter import latitude_longitude_to_utm, utm_to_ssb_grid_id

graphdir = str(pathlib.Path().resolve().joinpath(
    "proprietary_data").joinpath("ta-nor-2022-03-17-a"))

namebase = os.path.join(graphdir, "ta-nor-dynamic")

# Her setter dere opp access string til veidata database
schema_name = 'ferd_ta_2021_12_no'
db_connect_str = 'host=localhost dbname=ferd user=ferduser password=marioman1 ' + \
    f'options=-csearch_path={schema_name},citydb,citydb_pkg,public'

ferd_conn = psycopg2.connect(db_connect_str)
ferd_cur = ferd_conn.cursor()

# Dette laster dataene og setter opp kostnadsfunksjoner for kjøretid og avstand
graph = ferd.Graph(namebase, "RO")

distFunc = ferd.CostFunction(namebase + "-dist.cf", "RO")
timeFunc = ferd.CostFunction(namebase + "-time.cf", "RO")
closedFunc = ferd.CostFunction(namebase + "-closed.cf", "RW")
timeClosedFunc = ferd.CostFunction([closedFunc, timeFunc], "MAX")
distClosedFunc = ferd.CostFunction([distFunc, closedFunc], "MAX")


coreNum = 0
timeCores = []
while True:
    corename = namebase + "-time-core" + str(coreNum)
    if not os.path.isfile(corename + ".nid"):
        break
    timeCores.append(ferd.CoreGraph(corename, "RW"))
    if coreNum == 0:
        timeCores[0].setBaseGraph(graph)
    else:
        timeCores[coreNum].setBaseGraph(timeCores[coreNum - 1])
    coreNum += 1

timeCoreSearch = ferd.Search(graph, timeClosedFunc, timeCores)

coreNum = 0
distCores = []
while True:
    corename = namebase + "-dist-core" + str(coreNum)
    if not os.path.isfile(corename + ".nid"):
        break
    distCores.append(ferd.CoreGraph(corename, "RW"))
    coreNum += 1

distCoreSearch = ferd.Search(graph, distClosedFunc, distCores)

grid_coordinates = pd.read_csv("data/grid_centroids.csv")

base_station_coordinates = pd.read_csv("data/base_station_coordinates.csv")


def execute_query(cursor, _lat, _long, radius):
    cursor.execute(f"""WITH pnt AS (SELECT ST_Point(%s, %s) as geog)
                        SELECT id, ST_Distance(pnt.geog, jc.geog) dist, ST_Y(jc.geog::geometry), ST_X(jc.geog::geometry)
                        FROM {schema_name}.jc, pnt
                        WHERE ST_DWithin(jc.geog, pnt.geog, {radius})
                        ORDER BY dist""", [_long, _lat])


ssb_grid_ids = []
grid_coordinate_list = []
junction_coordinate_list = []
junction_distances = []
node_list = []

n = len(grid_coordinates) + len(base_station_coordinates)
i = 0

print("- Coordinate row processing -")

coordinate_start_index = {}
coordinate_end_index = {}

base_stations = False
for coordinate_dataset in [grid_coordinates, base_station_coordinates]:
    for _, (_, (lat, long)) in enumerate(coordinate_dataset.iterrows()):
        coordinate_start_index[(lat, long)] = len(node_list)
        execute_query(ferd_cur, lat, long, 50)
        point_list = [tuple for tuple in ferd_cur]
        increments = 0
        while len(point_list) <= 6:
            increments += 100
            execute_query(ferd_cur, lat, long, increments)
            point_list = [tuple for tuple in ferd_cur]
        if increments > 1500:
            print(f"{lat}, {long} is too far away from the network")
        for node_id, j_distance, j_lat, j_long in point_list[:6][::-1]:
            junctions = graph.getNode(node_id)
            for junction in junctions:
                if len(junctions) > 0:
                    node_list.append(junction)
                    junction_distances.append(j_distance)
                    grid_coordinate_list.append((lat, long))
                    junction_coordinate_list.append((j_lat, j_long))
                    easting, northing = latitude_longitude_to_utm(lat, long)
                    if base_stations:
                        ssb_grid_ids.append(f'_{easting:.0f}_{northing:.0f}')
                    else:
                        ssb_grid_ids.append(utm_to_ssb_grid_id(easting, northing))
        coordinate_end_index[(lat, long)] = len(node_list)
        i += 1
        if i % 10 == 0:
            print(f'{i} of {n} coordinate rows processed')
    base_stations = True


n = len(node_list)
k = 0

print("- OD matrix time calculation -")

od = {(grid_id): {} for grid_id in ssb_grid_ids}

missing_distances = 0

for coordinates, start_index in coordinate_start_index.items():
    start = time()
    origins = node_list[start_index:coordinate_end_index[coordinates]]
    destinations = node_list[:start_index] + node_list[coordinate_end_index[coordinates]:]
    ssb_grid_id_slice = ssb_grid_ids[:start_index] + ssb_grid_ids[coordinate_end_index[coordinates]:]
    junction_coordinate_slice = junction_coordinate_list[:start_index] + \
        junction_coordinate_list[coordinate_end_index[coordinates]:]
    multiSearch = ferd.MultiSearch(timeCoreSearch, origins, destinations)
    for i in range(len(origins)):
        for j in range(len(destinations)):
            cost = multiSearch.cost(i, j)
            if cost < 1000:
                if cost < od[ssb_grid_ids[start_index]].get(ssb_grid_id_slice[j], [np.inf])[0]:
                    if cost >= 23:
                        cost = timeCoreSearch(origins[i], destinations[j])[0]
                    od[ssb_grid_ids[start_index]][ssb_grid_id_slice[j]] = [
                        cost, junction_coordinate_slice[i], junction_coordinate_slice[j]]
                    continue
            missing_distances += 1
    k += 1
    print(f'{k} of {len(coordinate_start_index)} coordinate sets processed, {missing_distances} distance pairs unresolved / last run done in in {time() - start} seconds')


with open(f'data/od_matrix.json', 'w') as f:
    json.dump(od, f, indent=2)

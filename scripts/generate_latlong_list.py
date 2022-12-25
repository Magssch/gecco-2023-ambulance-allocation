import json

import pandas as pd
from coordinate_converter import ssb_grid_id_to_utm_centroid, utm_to_longitude_latitude

utm_and_latlong = {
    "easting": [],
    "northing": [],
    "lat": [],
    "long": [],
}
with open("data/od_matrix2.json", "r") as f1:
    print("Loading OD matrix...")
    od_matrix = json.load(f1)
    for grid_id in od_matrix.keys():
        try:
            grid_id_int = int(grid_id)
            easting, northing = ssb_grid_id_to_utm_centroid(grid_id_int)
            longitude, latitude = utm_to_longitude_latitude(
                ssb_grid_id_to_utm_centroid(grid_id_int)
            )
        except ValueError:
            _, easting, northing = grid_id.split("_")
            easting = int(easting)
            northing = int(northing)
            longitude, latitude = utm_to_longitude_latitude((easting, northing))
        utm_and_latlong["easting"].append(easting)
        utm_and_latlong["northing"].append(northing)
        utm_and_latlong["lat"].append(latitude)
        utm_and_latlong["long"].append(longitude)

utm_and_latlong_df = pd.DataFrame(utm_and_latlong)
utm_and_latlong_df.to_csv("data/utm_and_latlong.csv", index=False, header=False)

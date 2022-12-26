import json
import math
from concurrent.futures import ThreadPoolExecutor
from time import time
from typing import Any

import numpy as np
import pandas as pd
import polyline
import requests
from coordinate_converter import (
    latitude_longitude_to_utm,
    snap_utm_to_ssb_grid,
    utm_to_ssb_grid_id,
)


def format_coords(coords: np.ndarray) -> str:
    """
    Formats NumPy array of (lat, long) coordinates into a concatenated string formatted
    for the OSRM server.
    """
    coords = ";".join([f"{lon:f},{lat:f}" for lat, lon in coords])
    return coords


def format_options(options: dict[str, str]) -> str:
    """
    Formats dictionary of additional options to your OSRM request into a
    concatenated string format.
    """
    options = "&".join([f"{k}={v}" for k, v in options.items()])
    return options


class Connection:
    """Interface for connecting to and interacting with OSRM server."""

    def __init__(self, host: str, port: str):
        self.host = host
        self.port = port

    def make_request(
        self, service: str, coords, options: dict[str, str] | None
    ) -> dict[str, Any]:
        """
        Forwards your request to the OSRM server and returns a dictionary of the JSON
        response.
        """
        coords = format_coords(coords)
        options = format_options(options) if options else ""
        url = f"http://{self.host}:{self.port}/{service}/v1/car/{coords}?{options}"
        r = requests.get(url)
        return r.json()

    def route_dt(self, coords: np.ndarray):
        """Returns the distance/time to travel a given route."""
        x = self.make_request(
            service="route",
            coords=coords,
            options={"steps": "false", "overview": "false"},
        )
        return x["routes"][0]

    def route_polyline(
        self, coords, resolution: str = "low"
    ) -> tuple[list[tuple[float, float]], Any]:
        """Returns polyline of route path as a list of (lat, lon) coordinates."""
        assert resolution in ("low", "high")

        if resolution == "low":
            options = {"overview": "simplified"}
        elif resolution == "high":
            options = {"overview": "full"}
        x = self.make_request(
            service="route", coords=coords, options={"steps": "false", **options}
        )
        return polyline.decode(x["routes"][0]["geometry"]), x["routes"][0]

    def find_distance(self, origin, destination, find_path=False):
        if find_path:
            path, route = conn.route_polyline(
                (
                    origin,
                    destination,
                )
            )
            return (
                route["duration"],
                route["distance"],
                path,
            )
        else:
            route = conn.route_dt(
                (
                    origin,
                    destination,
                )
            )
            return (
                route["duration"],
                route["distance"],
            )


if __name__ == "__main__":
    conn = Connection(host="localhost", port="5001")

    grid_coordinates = pd.read_csv("scripts/data/grid_centroids.csv")
    utm_and_latlong = pd.read_csv("scripts/data/utm_and_latlong.csv")
    grid_coordinates["grid"] = True
    grid_coordinates["is_base_station"] = False
    base_station_coordinates = pd.read_csv("scripts/data/base_station_coordinates.csv")
    hospital_coordinates = pd.read_csv("scripts/data/hospital_coordinates.csv")
    base_station_coordinates["grid"] = False
    base_station_coordinates["is_base_station"] = True
    hospital_coordinates["grid"] = False
    hospital_coordinates["is_base_station"] = False

    coordinates = pd.concat(
        [
            grid_coordinates[["lat", "long", "grid", "is_base_station"]],
            base_station_coordinates[["lat", "long", "grid", "is_base_station"]],
            hospital_coordinates[["lat", "long", "grid", "is_base_station"]],
        ]
    )
    latlongs_to_utm = {
        **{
            (coors[2], coors[3]): (coors[0], coors[1])
            for (_, coors) in utm_and_latlong.iterrows()
        },
        **{
            (coors["lat"], coors["long"]): latitude_longitude_to_utm(
                coors["lat"], coors["long"]
            )
            for (_, coors) in coordinates.iterrows()
        },
    }

    utm_to_grid_id = {
        (easting, northing): str(utm_to_ssb_grid_id(easting, northing))
        for easting, northing in latlongs_to_utm.values()
    }

    od = {
        utm_to_grid_id[latlongs_to_utm[(coors["lat"], coors["long"])]]: {}
        for (_, coors) in grid_coordinates.iterrows()
    }

    for (_, coors) in base_station_coordinates.iterrows():
        easting, northing = latlongs_to_utm[(coors["lat"], coors["long"])]
        od[f"_{easting:.0f}_{northing:.0f}"] = {}

    for (_, coors) in hospital_coordinates.iterrows():
        easting, northing = latlongs_to_utm[(coors["lat"], coors["long"])]
        od[f"_{easting:.0f}_{northing:.0f}"] = {}

    unique_grid_ids = set(od.keys())
    unique_grid_coords = [
        (coors["lat"], coors["long"]) for (_, coors) in grid_coordinates.iterrows()
    ]

    n = (
        len(grid_coordinates)
        + len(base_station_coordinates)
        + len(hospital_coordinates)
    )
    i = 0

    extra_grids = {}

    # Read cached OD matrix
    od_cached = {}
    with open(f"scripts/data/od_matrix3.json", "r") as f1:
        od_cached = json.load(f1)
        for k, v in od_cached.items():
            od[k] = v

    with ThreadPoolExecutor() as executor:
        for (
            _,
            (origin_lat, origin_long, grid_origin, origin_is_base_station),
        ) in coordinates.iterrows():
            t1 = time()
            easting_orig, northing_orig = latlongs_to_utm[(origin_lat, origin_long)]
            futures = {}
            origin_key = (
                utm_to_grid_id[(easting_orig, northing_orig)]
                if grid_origin
                else f"_{easting_orig:.0f}_{northing_orig:.0f}"
            )
            g = 0
            for (
                _,
                (
                    destination_lat,
                    destination_long,
                    grid_destination,
                    destination_is_base_station,
                ),
            ) in coordinates.iterrows():
                if origin_lat == destination_lat and origin_long == destination_long:
                    continue
                easting_dest, northing_dest = latlongs_to_utm[
                    (destination_lat, destination_long)
                ]
                destination_key = (
                    utm_to_grid_id[(easting_dest, northing_dest)]
                    if grid_destination
                    else f"_{easting_dest:.0f}_{northing_dest:.0f}"
                )
                if destination_is_base_station or (
                    not od[origin_key]
                    or destination_key not in od[origin_key]
                    or not od[origin_key][destination_key]
                ):
                    futures[destination_key] = executor.submit(
                        conn.find_distance,
                        (origin_lat, origin_long),
                        (destination_lat, destination_long),
                        find_path=destination_is_base_station,
                    )
                    g += 1
            for destination, future in futures.items():
                od[origin_key][destination] = future.result()

            for destination in od[origin_key]:
                if len(od[origin_key][destination]) > 2 and not isinstance(
                    od[origin_key][destination][2][0], int
                ):
                    for j, path_point in enumerate(od[origin_key][destination][2]):
                        grid_id = utm_to_ssb_grid_id(
                            *snap_utm_to_ssb_grid(
                                latitude_longitude_to_utm(*path_point)
                            )
                        )
                        od[origin_key][destination][2][j] = grid_id
                        if (
                            str(grid_id) not in od
                            or len(od[str(grid_id)]) < len(unique_grid_ids) - 1
                        ) and str(grid_id) not in extra_grids:
                            extra_grids[str(grid_id)] = path_point
            i += 1
            t2 = time()
            print("--------------------")
            print(
                "Processed origin:",
                origin_key,
                " - number of destinations that was not cached:",
                g,
            )
            print(
                f"{i} of {n + len(extra_grids)} coordinate rows processed",
                f"- Completed in {(t2-t1):.4f}s",
            )
            print("Extra grid IDs to find routes for: ", len(extra_grids))

        print("Now finding extra grid IDs")
        k = 1
        for grid_id in extra_grids:
            if grid_id not in od:
                od[grid_id] = {}
        for grid_id, coords in extra_grids.items():
            print("Find paths from: ", grid_id, k, "of", len(extra_grids))
            futures = {}
            for (
                destination_grid_id,
                desintation_coords,
            ) in zip(unique_grid_ids, unique_grid_coords):
                if destination_grid_id not in od[grid_id]:
                    futures[destination_grid_id] = executor.submit(
                        conn.find_distance,
                        (coords[0], coords[1]),
                        (desintation_coords[0], desintation_coords[1]),
                        find_path=False,
                    )
            for destination, future in futures.items():
                od[grid_id][destination] = future.result()
            k += 1

    with open(f"scripts/data/od_matrix4.json", "w") as f2:
        json.dump(od, f2, indent=2)
        # exit(1)

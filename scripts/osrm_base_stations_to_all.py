import json
import math
from concurrent.futures import ThreadPoolExecutor
from time import time
from typing import Any

import numpy as np
import pandas as pd
import polyline
import requests
from coordinate_converter import utm_to_latitude_longitude


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

    grid_cells = pd.read_csv("data/population/oslo_akershus_2015_2019.csv")
    # grid_cells = grid_cells[grid_cells.year == 2017]
    len(grid_cells)
    base_station_coordinates = pd.read_csv("data/base_stations.csv")

    i = 0
    n = len(base_station_coordinates)

    od = {}

    with ThreadPoolExecutor() as executor:
        for (
            _,
            (easting_orig, northing_orig),
        ) in base_station_coordinates[["easting", "northing"]].iterrows():
            orig_id = f"{easting_orig}_{northing_orig}"
            od[orig_id] = {}
            t1 = time()
            futures = {}
            for (
                _,
                 (easting_dest, northing_dest),
            ) in list(grid_cells[["easting", "northing"]].iterrows()):
                if f"{int(easting_dest)}_{int(northing_dest)}" in futures:
                    continue
                futures[f"{int(easting_dest)}_{int(northing_dest)}"] = executor.submit(
                    conn.find_distance,
                    utm_to_latitude_longitude((easting_orig, northing_orig)),
                    utm_to_latitude_longitude((easting_dest, northing_dest)),
                    find_path=False,
                )
            for destination, future in futures.items():
                od[orig_id][destination] = future.result()
            i += 1
            t2 = time()
            print(
                f"{i} of {n} base stations processed",
                f"- Completed in {(t2-t1):.4f}s",
            )

    with open(f"data/base_station_to_all.json", "w") as f2:
        json.dump(od, f2, indent=2)
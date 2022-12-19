import json
from concurrent.futures import ThreadPoolExecutor
from typing import Any

import numpy as np
import pandas as pd
import polyline
import requests
from coordinate_converter import latitude_longitude_to_utm, utm_to_ssb_grid_id


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

    def find_distance(self, origin, destination):
        path, route = conn.route_polyline(
            (
                origin,
                destination,
            )
        )
        arr = path
        if len(path) > 7:
            arr = np.array(path)
            arr = arr[np.round(np.linspace(0, len(arr) - 1, 7)).astype(int)]
            arr = arr.tolist()
        return (
            route["duration"],
            route["distance"],
            arr,
        )


if __name__ == "__main__":
    conn = Connection(host="localhost", port="5001")

    grid_coordinates = pd.read_csv("scripts/data/grid_centroids.csv")
    grid_coordinates["grid"] = True
    base_station_coordinates = pd.read_csv("scripts/data/base_station_coordinates.csv")
    base_station_coordinates["grid"] = False

    coordinates = pd.concat(
        [
            grid_coordinates[["lat", "long", "grid"]],
            base_station_coordinates[["lat", "long", "grid"]],
        ]
    )

    latlongs_to_utm = {
        (coors["lat"], coors["long"]): latitude_longitude_to_utm(
            coors["lat"], coors["long"]
        )
        for (_, coors) in grid_coordinates.iterrows()
    }
    utm_to_grid_id = {
        (easting, northing): utm_to_ssb_grid_id(easting, northing)
        for easting, northing in latlongs_to_utm.values()
    }

    od = {
        utm_to_grid_id[latlongs_to_utm[(coors["lat"], coors["long"])]]: {}
        for (_, coors) in grid_coordinates.iterrows()
    }

    for (_, coors) in base_station_coordinates.iterrows():
        easting, northing = latlongs_to_utm[(coors["lat"], coors["long"])]
        od[f"_{easting:.0f}_{northing:.0f}"] = {}

    n = len(grid_coordinates) + len(base_station_coordinates)
    i = 0

    for (_, (origin_lat, origin_long, grid_origin)) in coordinates.iterrows():
        easting_orig, northing_orig = latlongs_to_utm[(origin_lat, origin_long)]
        futures = {}
        origin_key = (
            utm_to_grid_id[(easting_orig, northing_orig)]
            if grid_origin
            else f"_{easting_orig:.0f}_{northing_orig:.0f}"
        )
        with ThreadPoolExecutor() as executor:
            for (
                _,
                (destination_lat, destination_long, grid_destination),
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
                futures[destination_key] = executor.submit(
                    conn.find_distance,
                    (origin_lat, origin_long),
                    (destination_lat, destination_long),
                )
            od[origin_key] = {
                destination: future.result() for destination, future in futures.items()
            }
            # for (
            #     _,
            #     (destination_lat, destination_long, grid_destination),
            # ) in coordinates.iterrows():
            # if origin_lat == destination_lat and origin_long == destination_long:
            #     continue

            # easting_orig, northing_orig = latlongs_to_utm[(origin_lat, origin_long)]
            # easting_dest, northing_dest = latlongs_to_utm[
            #     (destination_lat, destination_long)
            # ]
            # origin_key = (
            #     utm_to_grid_id[(easting_orig, northing_orig)]
            #     if grid_origin
            #     else f"_{easting_orig:.0f}_{northing_orig:.0f}"
            # )
            # destination_key = (
            #     utm_to_grid_id[(easting_dest, northing_dest)]
            #     if grid_destination
            #     else f"_{easting_dest:.0f}_{northing_dest:.0f}"
            # )
            # od[origin_key][destination_key] = (
            #     route["duration"],
            #     route["distance"],
            #     arr,
            # )
        i += 1
        print(f"{i} of {n} coordinate rows processed")
        # if i == 1:
    with open(f"scripts/data/od_matrix.json", "w") as f:
        json.dump(od, f, indent=2)
    # exit(1)

import io
import os
import platform
import time
from collections import Counter
from pathlib import Path

import PIL.Image
import folium
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from webdriver_manager.chrome import ChromeDriverManager

from common import VISUALIZATION_FOLDER
from styles import allocation_coloring

MAP_WIDTH = 400
MAP_HEIGHT = 800


def get_map(width=MAP_WIDTH, height=MAP_HEIGHT, location=None, zoom_start=8):
    if location is None:
        location = [60.045, 11.133]
    return folium.Map(
        height=height,
        width=width,
        location=location,
        tiles="cartodbpositron",
        zoom_start=zoom_start,
        zoom_control=False,
    )


def _point_to_circle_marker(point, radius, weight=3, color="#000000") -> folium.Marker:
    return folium.CircleMarker(
        location=point["geometry"]["coordinates"][::-1],
        radius=radius,
        weight=weight,
        color=color,
        fill=True,
        fill_color=color,
        fill_opacity=0.7,
    )


def points_to_line(points, weight=3, color="#000000") -> folium.PolyLine:
    return folium.PolyLine(points, weight=weight, color=color, fill_opacity=0.7)


def coordinates_to_circle_marker(lat, long, radius, weight=3, color="#000000") -> folium.Marker:
    return folium.CircleMarker(
        location=(lat, long),
        radius=radius,
        weight=weight,
        color=color,
        fill=True,
        fill_color=color,
        fill_opacity=0.7,
    )


def _point_to_text_marker(point, text) -> folium.Marker:
    return folium.Marker(
        location=point["geometry"]["coordinates"][::-1],
        icon=folium.DivIcon(
            icon_size=(100, 100),
            icon_anchor=(3, 9),
            html=text,
        ),
    )


def create_capacity_circle_markers(points, allocation: Counter[int]) -> list[folium.Marker]:
    return [
        _point_to_circle_marker(point, 12, 0, allocation_coloring(allocation[i]))
        for i, point in enumerate(points)
    ] + [_point_to_text_marker(point, allocation[i]) for i, point in enumerate(points)]


def create_circle_markers(points):
    return list(map(lambda point: _point_to_circle_marker(point, 3), points))


def get_geojson_items(geojson_file_name, style_function):
    geojson_items = folium.GeoJson(geojson_file_name, name="geojson", style_function=style_function)
    os.remove(geojson_file_name)  # Clean up temp file
    return geojson_items


def export_map(folium_map: folium.Map, file_name, width=MAP_WIDTH):
    image_data = folium_map._to_png(delay=7)
    image = PIL.Image.open(io.BytesIO(image_data))

    _, height = image.size
    image = image.crop((0, 0, width, height))

    image.save(f"{VISUALIZATION_FOLDER}/{file_name}.png")


def export_map_with_chrome(folium_map: folium.Map, file_name, width=MAP_WIDTH, height=MAP_HEIGHT):
    html_file = f"{VISUALIZATION_FOLDER}/{file_name}.html"
    png_file = f"{VISUALIZATION_FOLDER}/{file_name}.png"

    # Temporarily save as html
    folium_map.save(html_file)

    # Setup Chrome
    options = Options()
    options.headless = True
    driver_path = (
        "/usr/bin/chromedriver"
        if platform.system() == "Linux"
        else "/usr/local/bin/chromedriver"
    )
    driver = webdriver.Chrome(ChromeDriverManager().install(), options=options)
    driver.set_window_size(4000, 3000)

    # Open in Chrome and save screenshot
    driver.get(f"file://{Path.cwd()}/{html_file}")
    time.sleep(1)
    driver.save_screenshot(png_file)

    # Crop & save
    image = PIL.Image.open(png_file)
    image = image.crop((0, 0, width, height))
    image.save(f"{VISUALIZATION_FOLDER}/{file_name}.png")

    # Clean up
    os.remove(html_file)

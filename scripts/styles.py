from functools import partial

import matplotlib

from colors import ALLOCATION_COLORS, HEATMAP_COLORS, ZONE_COLORS

MAGMA = matplotlib.cm.get_cmap('magma')


def _get_allocation_coloring(count):
    return ALLOCATION_COLORS[min(count, len(ALLOCATION_COLORS) - 1)]


def _heatmap_color_mapper(count):
    if count < 1:
        return HEATMAP_COLORS['0']
    if count < 10:
        return HEATMAP_COLORS['1-9']
    elif count < 100:
        return HEATMAP_COLORS['10-99']
    elif count < 1000:
        return HEATMAP_COLORS['100-999']
    elif count < 10000:
        return HEATMAP_COLORS['1000-9999']
    return HEATMAP_COLORS['≥10000']


def _sequential_color_mapper(value, max_value):
    return matplotlib.colors.to_hex(MAGMA(256 - int(256 * value / max_value)))


def _zone_color_mapper(zone):
    return ZONE_COLORS[zone]


def heatmap_style(feature):
    count = feature['geometry']['properties']['data']
    return {
        'color': '#000000',
        'fillOpacity': 0.8,
        'weight': 0 if count > 0 else 0.1,
        'fillColor': _heatmap_color_mapper(count)
    }


def get_dynamic_heatmap_style(max_value):
    def dynamic_heatmap_style(feature, max_value):
        value = feature['geometry']['properties']['data']
        return {
            'color': '#000000',
            'fillOpacity': 0.8,
            'weight': 0 if value > 0 else 0.1,
            'fillColor': _sequential_color_mapper(value, max_value)
        }
    return partial(dynamic_heatmap_style, max_value=max_value)


def zone_styles(feature):
    zone = feature['geometry']['properties']['data']
    return {
        'color': '#000000',
        'fillOpacity': 1,
        'weight': 0.1 if zone == len(ZONE_COLORS) - 1 else 0,
        'fillColor': _zone_color_mapper(zone)
    }


def allocation_coloring(count):
    return _get_allocation_coloring(count)

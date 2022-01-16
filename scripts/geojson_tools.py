from math import floor

import geojson
import utm

FALSE_EASTING = 2_000_000


def snap_utm_to_ssb_grid(utm_coordinate, grid_cell_size=1000):
    easting, northing = utm_coordinate
    return (
        floor((easting + FALSE_EASTING) / grid_cell_size) * grid_cell_size - FALSE_EASTING,
        floor(northing / grid_cell_size) * grid_cell_size
    )


def utm_to_latitude_longitude(utm_coordinate):
    easting, northing = utm_coordinate
    return utm.to_latlon(easting, northing, zone_number=33, northern=True, strict=False)


def utm_to_longitude_latitude(utm_coordinate):
    easting, northing = utm_coordinate
    return list(utm.to_latlon(easting, northing, zone_number=33, northern=True, strict=False)[::-1])


def centroid_to_ssb_grid_points(easting, northing):
    x_c, y_c = snap_utm_to_ssb_grid((easting, northing))
    ssb_grid_points = [(x_c, y_c)]

    for x_offset, y_offset in [(1000, 0), (1000, 1000), (0, 1000)]:
        ssb_grid_points.append((x_c + x_offset, y_c + y_offset))

    ssb_grid_points.append((x_c, y_c))
    return ssb_grid_points


def centroid_to_geojson(easting, northing):
    coordinates = utm_to_longitude_latitude((easting, northing))
    return geojson.Feature(geometry=geojson.Point(coordinates))


def centroid_to_geojson_square(easting, northing, data):
    ssb_grid_points = centroid_to_ssb_grid_points(easting, northing)
    ssb_grid_points_long_lat = list(map(utm_to_longitude_latitude, ssb_grid_points))

    return geojson.Feature(geometry=geojson.Polygon([
        ssb_grid_points_long_lat
    ], properties={
        'data': data
    }))


def dataframe_to_points(df):
    return df.apply(lambda row: centroid_to_geojson(*row), axis=1).tolist()


def dataframe_to_squares(df):
    return df.apply(lambda row: centroid_to_geojson_square(*row), axis=1).tolist()


def export_features(features: geojson.Feature, file_name):
    with open(file_name, 'w', encoding='utf8') as file:
        geojson.dump(geojson.FeatureCollection(features), file)

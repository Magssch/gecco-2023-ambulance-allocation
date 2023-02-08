import argparse
import functools
import shutil
from statistics import mode
from time import time

import pandas as pd

from plots.funnel import plot_funnel


def print_step(func):
    def wrapper(*args):
        print(f"Running step {func.__name__}...")
        t1 = time()
        return_value = func(*args)
        t2 = time()
        print(f"- Completed in {(t2 - t1):.4f}s")
        print(f"- Input size was {args[0].shape[0]}")
        print(f"- Output size was {return_value.shape[0]}")
        funnel_statistics.append((func.__name__, args[0].shape[0]))
        return return_value

    return wrapper


def build_pipeline(*steps, step_decorator=print_step):
    def reversed_compose(f, g):
        y = step_decorator(g)
        return lambda x: y(f(x))

    return functools.reduce(reversed_compose, steps, lambda x: x)


def filter_regions(df):
    oslo = pd.read_csv("data/oslo.csv", encoding="utf-8", index_col=4)
    akershus = pd.read_csv("data/akershus.csv", encoding="utf-8", index_col=4)
    oslo_and_akershus_data = pd.concat([oslo, akershus])

    # Create indices based on SSB grids
    oslo_and_akershus = oslo_and_akershus_data.index
    df_cells = pd.Index(df["ssbid1000M"])

    # Filter out all incident cells that are not located in Oslo and Akershus
    df = df[df_cells.isin(oslo_and_akershus)]
    return df


def select_features(df):
    features_to_keep = [
        "rykker_ut",
        "ank_hentested",
        "avg_hentested",
        "ank_levsted",
        "ledig",
        "xcoor",
        "ycoor",
        "hastegrad",
        "tiltak_type",
    ]
    return df[features_to_keep]


def filter_incomplete_years(df):
    return df[df.index.year >= 2015]


def keep_period_of_interest(df, buffer_size=0):
    time = df.index + pd.Timedelta(hours=buffer_size)
    august_2017 = df[time.year == 2017]
    all_2018 = df[time.year == 2018]
    return pd.concat([august_2017, all_2018])


def filter_urgency_levels(df):
    return df[df["hastegrad"].isin(["A", "H"])]


def filter_dispatch_types(df):
    return df[df["tiltak_type"] != "Operativ Leder"]


def aggregate_concurrent_incidents(df):
    # df with this index is considered concurring df
    index = ["tidspunkt", "xcoor", "ycoor", "hastegrad"]
    total_vehicles_assigned = df.groupby(index).size()

    df["ledig_ikketransport"] = df["ledig"]
    df["ledig_transport"] = df["ledig"]

    # Count number of vehicles with patient transport duty per incident
    transporting_vehicles = df.groupby(index).count()["avg_hentested"]
    transporting_vehicles.name = "transporting_vehicles"

    # Subtract transporting vehicles from total
    non_transporting_vehicles = total_vehicles_assigned - transporting_vehicles
    non_transporting_vehicles.name = "non_transporting_vehicles"

    # Append aggregate columns to dataset
    grouped_data = pd.concat([non_transporting_vehicles, transporting_vehicles], axis=1)
    df = df.set_index([df.index, *index[1:]]).join(grouped_data)

    agg_strategy = {
        "tiltak_type": mode,
        "rykker_ut": min,
        "ank_hentested": min,
        "avg_hentested": min,
        "ledig_ikketransport": min,
        "ledig_transport": max,
        "non_transporting_vehicles": min,
        "transporting_vehicles": min,
    }

    # Remove 'duplicate' rows and keep first occurrence of subevent
    df = df.groupby(index).agg(agg_strategy)

    # Convert counts to int
    df[["non_transporting_vehicles", "transporting_vehicles"]] = df[
        ["non_transporting_vehicles", "transporting_vehicles"]
    ].astype(int)

    return df


def convert_to_datetime(df):
    df["varslet"] = pd.to_datetime(df["varslet"], dayfirst=True)
    df["rykker_ut"] = pd.to_datetime(df["rykker_ut"], dayfirst=True)
    df["ank_hentested"] = pd.to_datetime(df["ank_hentested"], dayfirst=True)
    df["avg_hentested"] = pd.to_datetime(df["avg_hentested"], dayfirst=True)
    df["ank_levsted"] = pd.to_datetime(df["ank_levsted"], dayfirst=True)
    df["ledig"] = pd.to_datetime(df["ledig"], dayfirst=True)
    df["tidspunkt"] = pd.to_datetime(df["tidspunkt"], dayfirst=True)
    df = df.set_index("tidspunkt")
    return df


def filter_erroneous_timestamps(df):
    df = df.drop(df[(df.ank_hentested < df.index)].index)
    df = df.drop(df[(df.rykker_ut < df.index)].index)
    return df


def filter_response_time_outliers(df):
    df = df.drop(
        df[
            ((df.ank_hentested - df.index) > pd.Timedelta(hours=6))
            & (~df.hastegrad.isin(["V1", "V", "V2"]))
            ].index
    )
    df = df.drop(
        df[
            ((df.rykker_ut - df.index) > pd.Timedelta(hours=6))
            & (~df.hastegrad.isin(["V1", "V", "V2"]))
            ].index
    )
    return df


def set_index(df):
    df = df.reset_index()
    df = df.set_index(df["tidspunkt"])
    return df


def sort_index(df):
    df = df.sort_index()
    return df


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "-k",
        dest="keep_all_years",
        type=int,
        default=False,
        action=argparse.BooleanOptionalAction,
    )
    return parser.parse_args()


def save_data(df, output_data_file):
    plot_funnel(*zip(*funnel_statistics))

    df.to_csv(output_data_file, index=False)

    destination_file = "../src/main/resources/incidents.csv"
    shutil.copyfile(output_data_file, destination_file)


def main():
    args = get_args()

    global funnel_statistics
    funnel_statistics = []

    input_data_file = "proprietary_data/cleaned_data.csv"
    output_data_file = "proprietary_data/processed_data.csv"

    buffer_size = 4  # hours

    fields = [
        "tidspunkt",
        "varslet",
        "rykker_ut",
        "ank_hentested",
        "avg_hentested",
        "ank_levsted",
        "ledig",
        "xcoor",
        "ycoor",
        "hastegrad",
        "tiltak_type",
        "ssbid1000M",
    ]

    df = pd.read_csv(
        input_data_file,
        encoding="utf-8",
        escapechar="\\",
        usecols=fields,
        parse_dates=True,
    )

    def filter_years(df):
        if args.keep_all_years:
            return df
        return keep_period_of_interest(df, buffer_size=buffer_size)

    def save_intermediate(df):
        if args.keep_all_years:
            intermediate_data_file = "proprietary_data/half_processed_data.csv"
            temp_df = df[
                [
                    "xcoor",
                    "ycoor",
                    "hastegrad",
                    "tiltak_type",
                    "rykker_ut",
                    "ank_hentested",
                    "avg_hentested",
                    "ledig",
                ]
            ]
            temp_df = set_index(temp_df)
            temp_df = sort_index(temp_df)
            temp_df.to_csv(intermediate_data_file, index=False)
        return df

    steps = [
        convert_to_datetime,
        filter_years,
        filter_regions,
        filter_incomplete_years,
        filter_erroneous_timestamps,
        filter_response_time_outliers,
        select_features,
        save_intermediate,
        filter_dispatch_types,
        filter_urgency_levels,
        aggregate_concurrent_incidents,
        set_index,
        sort_index,
    ]

    pipeline = build_pipeline(*steps)
    df = pipeline(df)

    save_data(df, output_data_file)


if __name__ == "__main__":
    main()

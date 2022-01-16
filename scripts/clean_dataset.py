import pandas as pd
import regex as re


def convert_to_datetime(df):
    df['varslet'] = pd.to_datetime(df['varslet'], dayfirst=True)
    df['tiltak_opprettet'] = pd.to_datetime(df['tiltak_opprettet'], dayfirst=True)
    df['rykker_ut'] = pd.to_datetime(df['rykker_ut'], dayfirst=True)
    df['ank_hentested'] = pd.to_datetime(df['ank_hentested'], dayfirst=True)
    df['avg_hentested'] = pd.to_datetime(df['avg_hentested'], dayfirst=True)
    df['ank_levsted'] = pd.to_datetime(df['ank_levsted'], dayfirst=True)
    df['ledig'] = pd.to_datetime(df['ledig'], dayfirst=True)
    df['tidspunkt'] = pd.to_datetime(df['tidspunkt'], dayfirst=True)
    return df


def csv_parser(raw_data_path, cleaned_data_path):
    with open(raw_data_path, 'r', encoding='windows-1252') as input_file, open(cleaned_data_path, 'w', encoding='utf-8') as output_file:
        # Fix missing value in CSV header id
        header = input_file.readline()
        header = header.replace('""', '"id"')
        output_file.write(header)

        # Escape comma in geometry field
        for line in input_file:
            if re.match(r'.*\(.*,.*\).*', line):
                line = re.sub(r'\([^,()]+\K,', '\\,', line)
            output_file.write(line)


def main():
    data_folder = 'proprietary_data'
    raw_data_path = f'{data_folder}/raw_data.csv'
    cleaned_data_path = f'{data_folder}/cleaned_data.csv'
    csv_parser(raw_data_path, cleaned_data_path)
    df = pd.read_csv(cleaned_data_path, encoding='utf-8', escapechar='\\')
    df = convert_to_datetime(df)
    df.to_csv(cleaned_data_path, index=False)


if __name__ == '__main__':
    main()

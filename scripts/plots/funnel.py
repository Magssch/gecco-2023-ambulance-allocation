from common import VISUALIZATION_FOLDER
from plotly import graph_objects as go


def plot_funnel(labels, sizes):
    labels = list(map(lambda label: label.replace('_', ' ').capitalize(), labels))
    fig = go.Figure(go.Funnel(
        y=labels,
        x=sizes,
        textposition='inside',
        marker={"color": ['#1f77b4'] * len(labels)},
        textfont={'family': 'Times New Roman, serif'})
    )
    fig.update_layout(plot_bgcolor='#fff', font_family='Times New Roman')

    fig.write_image(f'{VISUALIZATION_FOLDER}/data_funnel.pdf')

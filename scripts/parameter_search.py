import subprocess
import sys
from itertools import product

import pandas as pd

# Define the parameters for the grid search

optimizer = "ma"

meta_parameters = {
    "number_of_runs": 1,
    "running_time": 3 * 60,
    # "max_permutations": 2,
}
# 3,40,4,5,0.2,0.1,0.15,true,true
param_grid = {
    "seeding_size": [3],
    "population_size": [30, 40],
    "elite_size": [4],
    "tournament_size": [5],
    "crossover_probability": [0.1,0.15,0.2],
    "mutation_probability": [0.05,0.1,0.15],
    "improve_probability": [0.05,0.1,0.15,0.2] if optimizer == "ma" else [0.0],
    "use_swap_mutation": ["true"],
    "improve_operator": ["operatorcritic", "sls", "robinhood"] if optimizer == "ma" else [""],
    "restart_probability": [0.025],
    "noise_probability": [0.65],
    "lazy_neighborhood_size": [80],
}

# Create a list of all permutations in the parameter grid
parameter_permutations_list = list(product(*param_grid.values()))

print("Starting parameter search... Will run " + str(len(parameter_permutations_list)) + " experiments.")

# Convert the list of permutations to a comma and semicolon-separated string
parameter_permutations = ";".join(
    [",".join([str(val) for val in single_run]) for single_run in parameter_permutations_list])

# Run the Java file with all the parameters
output = subprocess.run(
    [
        "MVN_LOCAL_REPO=$(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]') && java -cp target/classes:${MVN_LOCAL_REPO}/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar:${MVN_LOCAL_REPO}/com/google/code/gson/gson/2.10/gson-2.10.jar --enable-preview no.ntnu.ambulanceallocation.experiments.ParameterSearch -optimizer=" +
        str(optimizer) +
        " -paramList=\"" +
        str(parameter_permutations) +
        "\" -numberOfRuns=" +
        str(meta_parameters["number_of_runs"]) +
        " -runningTime=" +
        str(meta_parameters["running_time"])
    ],
    text=True,
    shell=True,
    check=True,
    stdout=subprocess.PIPE,
    stderr=sys.stdout,
)
# Extract the fitness value from the output
fitness = output.stdout

results = {
    "seeding_size": [params[0] for params in parameter_permutations_list],
    "population_size": [params[1] for params in parameter_permutations_list],
    "elite_size": [params[2] for params in parameter_permutations_list],
    "tournament_size": [params[3] for params in parameter_permutations_list],
    "crossover_probability": [params[4] for params in parameter_permutations_list],
    "mutation_probability": [params[5] for params in parameter_permutations_list],
    "improve_probability": [params[6] for params in parameter_permutations_list],
    "use_swap_mutation": [params[7] for params in parameter_permutations_list],
    "improve_operator": [params[8] for params in parameter_permutations_list],
    "restart_probability": [params[9] for params in parameter_permutations_list],
    "noise_probability": [params[10] for params in parameter_permutations_list],
    "lazy_neighborhood_size": [params[11] for params in parameter_permutations_list],
    "fitness": fitness.split(","),
}
# Print the final dataframe
pd.DataFrame(results).to_csv("output/parameter_search.csv", index=False)

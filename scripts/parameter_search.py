import subprocess
import sys
from itertools import product

import pandas as pd

# Define the parameters for the grid search

optimizer = "ga"

param_grid = {
    "seeding_size": [1, 2, 3],
    "population_size": [30, 40, 50],
    "elite_size": [2, 4, 6],
    "tournament_size": [2, 4, 6],
    "crossover_probability": [0.05, 0.1, 0.15, 0.2, 0.25, 0.3],
    "mutation_probability": [0.05, 0.1, 0.15, 0.2, 0.25, 0.3],
    "improve_probability": [0.05, 0.1, 0.15, 0.2, 0.25, 0.3],
}

# Create an empty dataframe to store the results
results = []
# Loop through all the combinations of parameters
for (
    seeding_size,
    population_size,
    elite_size,
    tournament_size,
    crossover_probability,
    mutation_probability,
    improve_probability,
) in list(product(*param_grid.values())):
    print("Running experiment")
    # Run the Java file with the current parameters
    output = subprocess.run(
        [
            "MVN_LOCAL_REPO=$(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]') && java -cp target/classes:${MVN_LOCAL_REPO}/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar:${MVN_LOCAL_REPO}/com/google/code/gson/gson/2.10/gson-2.10.jar --enable-preview no.ntnu.ambulanceallocation.experiments.ParameterSearch -optimizer="
            + str(optimizer)
            + " -seedingSize="
            + str(seeding_size)
            + " -populationSize="
            + str(population_size)
            + " -eliteSize="
            + str(elite_size)
            + " -tournamentSize="
            + str(tournament_size)
            + " -crossoverProbability="
            + str(crossover_probability)
            + " -mutationProbability="
            + str(mutation_probability)
            + " -improveProbability="
            + str(improve_probability)
        ],
        text=True,
        shell=True,
        check=True,
        stdout=subprocess.PIPE,
        stderr=sys.stdout,
    )
    # Extract the fitness value from the output
    fitness = output.stdout
    # Add the results to the dataframe
    results.append(
        {
            "seeding_size": seeding_size,
            "population_size": population_size,
            "elite_size": elite_size,
            "tournament_size": tournament_size,
            "crossover_probability": crossover_probability,
            "mutation_probability": mutation_probability,
            "improve_probability": improve_probability,
            "fitness": fitness,
        },
    )

# Print the final dataframe
pd.DataFrame(results).to_csv("output/parameter_search.csv", index=False)

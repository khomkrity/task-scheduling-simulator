# Task Scheduling Simulator

This project contains a task scheduling simulator that reads environment and workflow settings from configuration files and applies various scheduling algorithms to the tasks defined in the workflows. The scheduling results can then be exported in a convenient format for further analysis.

## Main Class Description

The `Main` class represents the entry point of the task scheduling simulator. The `main` method of this class:

1. Reads the configuration file to set up the environment and workflow settings.
2. Initializes the list of scheduling algorithms to be used in the simulation.
3. Runs the simulation for each combination of workflow and scheduling algorithm.
4. Optionally, prints the final schedule of tasks for each workflow to the console.
5. Optionally, exports the final results in a json format.

The `simulate` method of the `Main` class runs a simulation of task scheduling using the provided workflow settings, environment settings, and scheduling algorithm names. The method returns a list of `SchedulingResult` objects, each representing the outcome of a scheduling simulation for a given workflow and algorithm.

## Usage

Before running the simulator, ensure that the configuration file is correctly set up with the paths to the workflow files and environment settings. The simulator currently supports the following scheduling algorithms (The details of this algorithm can be found in following DOI):

- **HEFT (Heterogeneous Earliest Finish Time)**: [10.1109/71.993206](https://doi.org/10.1109/71.993206).
- **CPOP (Critical Path on Processor)**: [10.1109/71.993206](https://doi.org/10.1109/71.993206).
- **HSV (Heterogeneous Selection Value)**: [10.1016/j.jpdc.2015.04.005](https://doi.org/10.1016/j.jpdc.2015.04.005).
- **PPTS (Predict Priority Task Scheduling)**: [10.1145/3339186.3339206](https://doi.org/10.1145/3339186.3339206).
- **PEFT (Predict Earliest Finish Time)**: [10.1109/TPDS.2013.57](https://doi.org/10.1109/TPDS.2013.57).
- **IPEFT (Improved Predict Earliest Finish Time)**: [10.1002/cpe.3944](http://dx.doi.org/10.1002/cpe.3944).
- **IPPTS (Improved Priority Task Scheduling)**: [10.1109/TPDS.2020.3041829](https://doi.org/10.1109/TPDS.2020.3041829).

To run the simulator, execute the `Main` class in your Java environment. The results of the simulation can then be viewed in the console or exported in a JSON format for further analysis.

## Building & Running

Make sure you have a compatible version of Java installed on your system. Then, compile and run the project with the following commands:

```bash
javac Main.java
java Main
```

Please replace the paths and filenames as per your system settings.

## Contributing

We welcome contributions! Please submit a pull request with your improvements.

## License

This project is licensed under the terms of the MIT license.

## Contact

If you have any questions, feel free to reach out or open an issue.

---
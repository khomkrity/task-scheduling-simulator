package com.mfu.taskscheduling.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mfu.taskscheduling.output.SchedulingResult;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * The Printer class provides a utility method for printing a list of Result objects in a
 * formatted manner.
 */
public class Printers {
    /**
     * Prints the provided list of Result objects in a formatted manner. The method iterates
     * through the list and outputs the relevant information for each result, such as workflow name,
     * number of tasks, number of processors, algorithm name, makespan, speedup, efficiency, and
     * schedule length ratio (normalized makespan).
     *
     * @param schedulingResults the list of Result objects to be printed
     */
    public static void print(List<SchedulingResult> schedulingResults) {
        schedulingResults.forEach(schedulingResult -> {
            StringBuilder output = new StringBuilder();
            output.append("workflow: ").append(schedulingResult.getWorkflowName()).append('\n')
                    .append("algorithm: ").append(schedulingResult.getAlgorithmName()).append('\n')
                    .append("number of tasks: ").append(schedulingResult.getNumberOfTask()).append('\n')
                    .append("number of processors: ").append(schedulingResult.getNumberOfProcessor()).append('\n')
                    .append("makespan: ").append(schedulingResult.getMakespan()).append('\n')
                    .append("speedup: ").append(schedulingResult.getSpeedup()).append('\n')
                    .append("efficiency: ").append(schedulingResult.getEfficiency()).append('\n')
                    .append("throughput: ").append(schedulingResult.getThroughput()).append("\n")
                    .append("schedule length ratio (normalized makespan): ").append(schedulingResult.getScheduleLengthRatio()).append("\n")
                    .append("resource utilization: ").append(schedulingResult.getProcessorResults()).append("\n\n");
            System.out.println(output);
        });
    }

    /**
     * Exports a list of Result objects to a JSON file for further analysis and processing.
     * The method uses Gson to serialize the results and writes the JSON data to the specified file.
     * <p>
     * This method is useful for exporting scheduling simulation results to a format that can be
     * easily parsed and analyzed in other programming languages, such as Python.
     * </p>
     *
     * @param schedulingResults  the list of Result objects containing the scheduling simulation results
     * @param fileName the name of the file to save the exported JSON data (without the file extension)
     * @throws RuntimeException if there is an IOException while writing to the file
     */
    public static void export(List<SchedulingResult> schedulingResults, String fileName) {
        System.out.println("exporting results...");
        // Set pretty-printing for better readability
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(schedulingResults);

        try (FileWriter file = new FileWriter("src/main/resources/result/" + fileName + ".json")) {
            file.write(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

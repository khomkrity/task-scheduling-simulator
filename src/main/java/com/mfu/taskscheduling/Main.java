package com.mfu.taskscheduling;

import com.mfu.taskscheduling.algorithm.SchedulingAlgorithm;
import com.mfu.taskscheduling.algorithm.SchedulingAlgorithmFactory;
import com.mfu.taskscheduling.algorithm.SchedulingAlgorithmName;
import com.mfu.taskscheduling.input.EnvironmentSetting;
import com.mfu.taskscheduling.input.WorkflowSetting;
import com.mfu.taskscheduling.output.SchedulingResult;
import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Config;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Printers;
import com.mfu.taskscheduling.util.Schedulers;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the entry point of the task scheduling simulator.
 */
public class Main {
    /**
     * The entry point of the task scheduling simulator.
     * <p>
     * The main method performs the following steps:
     * 1. Reads the configuration file to set up the environment and workflow settings.
     * 2. Initializes the list of scheduling algorithms to be used in the simulation.
     * 3. Runs the simulation for each combination of workflow and scheduling algorithm.
     * 4. Prints the final schedule of tasks for each workflow to the console (optional).
     * 5. Exports the final results in a json format (optional).
     * </p>
     *
     * @param args the command-line arguments (not used)
     * @throws Exception if there is an error in parsing configuration files or workflows
     */
    public static void main(String[] args) throws Exception {
        // config
        Config config = new Config("src/main/resources/config.properties");
        EnvironmentSetting environmentSetting = new EnvironmentSetting(config.getProperty("environmentSettingPath"));
        WorkflowSetting workflowSetting = new WorkflowSetting(config.getProperty("workflowDirectoryPath"));
        // algorithms
        List<SchedulingAlgorithmName> algorithmNames = List.of(
                SchedulingAlgorithmName.HEFT,
                SchedulingAlgorithmName.CPOP,
                SchedulingAlgorithmName.HSV,
                SchedulingAlgorithmName.PPTS,
                SchedulingAlgorithmName.PEFT,
                SchedulingAlgorithmName.IPEFT,
                SchedulingAlgorithmName.IPPTS
        );
        // simulation
        List<SchedulingResult> schedulingResults = simulate(workflowSetting, environmentSetting, algorithmNames);
        // results
        Printers.export(schedulingResults, "sipht-2");
    }

    /**
     * Runs a simulation of task scheduling using the provided workflow settings, environment settings, and scheduling algorithm names.
     * The method returns a list of SchedulingResult objects, each representing the outcome of a scheduling simulation for a given workflow and algorithm.
     * <p>
     * This method works as follows:
     * 1. It reads and parses each workflow from the provided paths.
     * 2. It computes task priorities based on the parsed workflow tasks and available processors.
     * 3. It then creates and runs each specified scheduling algorithm on the tasks and processors.
     * 4. Finally, it collects the results of each simulation, including the tasks as they were scheduled by the algorithm.
     *
     * @param workflowSetting    the workflow settings, including paths to workflow files
     * @param environmentSetting the environment settings, including processor specifications and constraints
     * @param algorithmNames     the list of names of scheduling algorithms to be applied to each workflow
     * @return a list of SchedulingResult objects containing the simulation results for each combination of workflow and scheduling algorithm
     * @throws Exception if there is an error in parsing the workflow files
     */
    private static List<SchedulingResult> simulate(WorkflowSetting workflowSetting, EnvironmentSetting environmentSetting, List<SchedulingAlgorithmName> algorithmNames) throws Exception {
        boolean hasPortConstraint = environmentSetting.hasPortConstraint();
        boolean addPseudoTask = environmentSetting.addPseudoTask();
        boolean useMockData = environmentSetting.useMockData();

        int numberOfResults = workflowSetting.getWorkflowPaths().size() * algorithmNames.size() * environmentSetting.getProcessors().size();
        List<SchedulingResult> schedulingResults = new ArrayList<>(numberOfResults);

        for (String workflowPath : workflowSetting.getWorkflowPaths()) {
            String workflowName = workflowSetting.getWorkflowName(workflowPath);
            List<Task> inputTasks = workflowSetting.parseWorkflowFile(workflowPath, hasPortConstraint, addPseudoTask, useMockData);
            Priority priority = new Priority(inputTasks);
            for (List<Processor> processors : environmentSetting.getProcessors()) {
                priority.computeTaskPriorities(inputTasks, processors);
                for (SchedulingAlgorithm schedulingAlgorithm : SchedulingAlgorithmFactory.createAlgorithms(algorithmNames, priority)) {
                    List<Task> scheduledTasks = runSchedulingAlgorithm(schedulingAlgorithm, inputTasks, processors, hasPortConstraint);
                    schedulingResults.add(new SchedulingResult(workflowName, schedulingAlgorithm.toString(), scheduledTasks, processors, priority));
                    reset(inputTasks, processors);
                }
                Costs.reset();
            }
        }

        return schedulingResults;
    }

    /**
     * Runs a single scheduling algorithm on the given tasks and processors, taking into account any port constraints.
     * Returns the scheduled list of tasks after running the scheduling algorithm.
     *
     * @param schedulingAlgorithm the scheduling algorithm to run
     * @param tasks               the list of input tasks to be scheduled
     * @param processors          the list of available processors
     * @param hasPortConstraint   a boolean indicating whether port constraints should be considered during scheduling
     * @return the scheduled list of tasks after running the scheduling algorithm
     */
    private static List<Task> runSchedulingAlgorithm(SchedulingAlgorithm schedulingAlgorithm, List<Task> tasks, List<Processor> processors, boolean hasPortConstraint) {
        List<Task> scheduledTasks = schedulingAlgorithm.run(tasks, processors);
        Schedulers.schedule(scheduledTasks, hasPortConstraint);
        return scheduledTasks;
    }

    /**
     * Resets the state of tasks and processors for the next scheduling algorithm run.
     * It is necessary to call this method after each scheduling algorithm execution to ensure that tasks and processors
     * are in their initial state for the next algorithm run.
     *
     * @param tasks      the list of tasks to reset
     * @param processors the list of processors to reset
     */
    private static void reset(List<Task> tasks, List<Processor> processors) {
        tasks.forEach(Task::reset);
        processors.forEach(Processor::reset);
    }
}

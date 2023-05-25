package com.mfu.taskscheduling.output;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Results;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.util.Workflows;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a workflow scheduling algorithm applied to a set of tasks and processors.
 * Stores information about the workflow, algorithm, tasks, processors, and various performance metrics.
 */
public class SchedulingResult {
    private final String workflowName;
    private final String algorithmName;
    private final List<TaskResult> taskResults;
    private final List<ProcessorResult> processorResults;
    private final double makespan;
    private final double speedup;
    private final double efficiency;
    private final double scheduleLengthRatio;
    private final double throughput;
    // TODO: TEST PURPOSES
    private final int numberOfTask;
    private final int numberOfProcessor;
    private final int numberOfEntry;
    private final int numberOfExit;
    private final int addedPseudoEntry;
    private final int addedPseudoExit;
    private final double communicationToComputationRatio;
    private final int width;
    private int maxInDegree;
    private int maxOutDegree;
    private int maxSibling;
    private final int height;
    private final double density;
    private final int numberOfEdge;
    private final double averageTaskDegree;
    private double totalLength;
    private final double totalComputationCost;
    private final double totalCommunicationCost;
    private final double criticalPathCost;
    private final List<Double> upwardRanks;
    private final List<Double> downwardRanks;
    private final List<Double> optimisticCosts;
    private final List<Double> pessimisticCosts;
    private final List<Double> heterogeneousUpwardRanks;
    private final List<Double> predictCosts;
    private final List<Double> predictRanks;
    private final int numberOfCriticalTask;


    /**
     * Constructs a new Result instance with the provided information.
     *
     * @param workflowName  the name of the workflow associated with this result
     * @param algorithmName the name of the scheduling algorithm used in this result
     * @param tasks         a list of tasks in the workflow
     * @param processors    a list of processors available for scheduling tasks
     */
    public SchedulingResult(String workflowName, String algorithmName, List<Task> tasks, List<Processor> processors, Priority priority) {
        List<Task> criticalTasks = Workflows.getCriticalPath(tasks, priority);


        this.numberOfTask = tasks.size();
        this.numberOfProcessor = processors.size();
        this.workflowName = workflowName;
        this.algorithmName = algorithmName;
        this.taskResults = new ArrayList<>(numberOfTask);
        this.processorResults = new ArrayList<>(numberOfProcessor);
//        tasks.forEach(task -> taskResults.add(new TaskResult(task)));
//        processors.forEach(processor -> processorResults.add(new ProcessorResult(processor, Results.getTotalRunningTime(processors))));
        this.makespan = Results.getMakespan(tasks);
        this.speedup = Results.getSpeedup(tasks, processors, makespan);
        this.efficiency = Results.getEfficiency(processors, speedup);
        this.scheduleLengthRatio = Results.getScheduleLengthRatio(criticalTasks, processors, makespan);
        this.throughput = Results.getThroughput(numberOfTask, makespan);

        //TODO: TEST PURPOSES
        double criticalPathCost = Costs.getSumAverageComputationCost(criticalTasks, processors);
        double averageBandwidth = StatUtils.percentile(Costs.getBandwidths(processors), 50);
        for (int i = 0, j = 1; i < criticalTasks.size() - 1; i++, j++) {
            criticalPathCost += Costs.getCommunicationCost(criticalTasks.get(i), criticalTasks.get(j), averageBandwidth);
        }
        this.communicationToComputationRatio = Costs.getCommunicationToComputationRatio(tasks, processors);
        this.width = Workflows.getWidth(tasks);
        this.height = Workflows.getHeight(tasks);
        this.numberOfEdge = Workflows.getTotalNumberOfEdge(tasks);
        this.averageTaskDegree = Workflows.getAverageTaskDegree(tasks);
        this.density = Workflows.getDensity(tasks);
        this.numberOfEntry = Workflows.getRoots(tasks).get(0).getChildren().size();
        this.numberOfExit = Workflows.getExits(tasks).get(0).getParents().size();
        this.addedPseudoEntry = Workflows.getRoots(tasks).get(0).getLength() == 0 ? 1 : 0;
        this.addedPseudoExit = Workflows.getExits(tasks).get(0).getLength() == 0 ? 1 : 0;



        this.criticalPathCost = criticalPathCost;
        numberOfCriticalTask = criticalTasks.size();
        this.totalComputationCost = Costs.getSumAverageComputationCost(tasks, processors);
        this.totalCommunicationCost = Costs.getSumAverageCommunicationCost(tasks, processors);

        this.upwardRanks = new ArrayList<>(priority.getUpwardRanks().values());
        this.downwardRanks = new ArrayList<>(priority.getDownwardRanks().values());
        this.optimisticCosts = new ArrayList<>(numberOfTask);
        this.pessimisticCosts = new ArrayList<>(numberOfTask);
        this.heterogeneousUpwardRanks = new ArrayList<>(numberOfTask);
        this.predictCosts = new ArrayList<>(numberOfTask);
        this.predictRanks = new ArrayList<>(numberOfTask);
        Map<Task, Map<Processor, Double>> upwardRankMatrix = priority.getUpwardRankMatrix();
        Map<Task, Map<Processor, Double>> optimisticCostTable = priority.getOptimisticCostTable();
        Map<Task, Map<Processor, Double>> pessimisticCostTable = priority.getPessimisticCostTable();
        Map<Task, Map<Processor, Double>> predictCostTable = priority.getPredictCostMatrix();
        for (Task task : tasks) {
            maxSibling = Math.max(maxSibling, task.getSiblings().size());
            totalLength += task.getLength();
            maxInDegree = Math.max(maxInDegree, task.getParents().size());
            maxOutDegree = Math.max(maxOutDegree, task.getChildren().size());
            heterogeneousUpwardRanks.add(priority.getAverageRankValue(upwardRankMatrix, task) * task.getChildren().size());
            optimisticCosts.add(priority.getAverageRankValue(optimisticCostTable, task));
            pessimisticCosts.add(priority.getAverageRankValue(pessimisticCostTable, task));
            predictCosts.add(priority.getAverageRankValue(predictCostTable, task));
            predictRanks.add(priority.getAverageRankValue(predictCostTable, task) * task.getChildren().size());
        }
    }

    /**
     * @return the name of the workflow associated with this result
     */
    public String getWorkflowName() {
        return workflowName;
    }

    /**
     * @return the name of the scheduling algorithm used in this result
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * @return the number of tasks scheduled in this result
     */
    public int getNumberOfTask() {
        return taskResults.size();
    }

    /**
     * @return the number of processors used in this result
     */
    public int getNumberOfProcessor() {
        return processorResults.size();
    }

    /**
     * @return the makespan (total execution time) of this result
     */
    public double getMakespan() {
        return makespan;
    }

    /**
     * @return the speedup of this result, calculated as the ratio of the sequential execution time to the makespan
     */
    public double getSpeedup() {
        return speedup;
    }

    /**
     * @return the efficiency of this result, calculated as the ratio of speedup to the number of processors
     */
    public double getEfficiency() {
        return efficiency;
    }

    /**
     * @return the schedule length ratio (normalized makespan) of this result
     */
    public double getScheduleLengthRatio() {
        return scheduleLengthRatio;
    }

    /**
     * @return the throughput per minute in this result
     */
    public double getThroughput() {
        return throughput;
    }

    /**
     * @return the list of results produced from each scheduled task
     */
    public List<TaskResult> getTaskResults() {
        return taskResults;
    }

    /**
     * @return the list of results produced from each processor
     */
    public List<ProcessorResult> getProcessorResults() {
        return processorResults;
    }
}


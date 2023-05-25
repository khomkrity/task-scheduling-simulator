package com.mfu.taskscheduling.algorithm;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.task.Priority;
import com.mfu.taskscheduling.task.Task;
import com.mfu.taskscheduling.util.Costs;
import com.mfu.taskscheduling.util.Schedulers;
import org.apache.commons.math3.stat.StatUtils;

import java.util.*;

/**
 * Represents the Improved Predict Earliest Finish Time (IPEFT) scheduling algorithm.
 * The details of the algorithm can be found in the paper with DOI: <a target="blank" href="http://dx.doi.org/10.1002/cpe.3944">10.1002/cpe.3944</a>.
 */
public class IPEFT extends SchedulingAlgorithm {
    private final Priority priority;
    private final Map<Processor, List<Task>> schedules;
    private final Map<Task, Map<Processor, Double>> criticalNodeCostTable;
    private final Set<Task> criticalNodes;
    private final Set<Task> readyTasks;
    private final Set<Task> executedTasks;
    private final Map<Task, Double> averageEarliestStartTimes;
    private final Map<Task, Double> averageLatestStartTimes;

    /**
     * Creates a new instance of the IPEFT scheduling algorithm.
     */
    public IPEFT(Priority priority) {
        this.priority = priority;
        schedules = new HashMap<>();
        averageEarliestStartTimes = new HashMap<>();
        averageLatestStartTimes = new HashMap<>();
        criticalNodeCostTable = new HashMap<>();
        criticalNodes = new HashSet<>();
        readyTasks = new HashSet<>();
        executedTasks = new HashSet<>();
    }

    @Override
    protected void prepare(List<Task> tasks, List<Processor> processors) {
        tasks.stream().filter(Task::isEntry).forEach(readyTasks::add);
        tasks.forEach(task -> criticalNodeCostTable.put(task, new HashMap<>()));
        processors.forEach(processor -> schedules.put(processor, new ArrayList<>()));
    }

    @Override
    protected void execute(List<Task> tasks, List<Processor> processors) {
        // prioritization phase
        calculateAverageStartTime(tasks, processors);
        setCriticalNodes(tasks);
        setPriority(tasks, processors);
        calculateCriticalNodeCostTable(tasks, processors);
        // allocation phase
        allocateTasks(readyTasks, executedTasks, processors);
    }

    private void calculateAverageStartTime(List<Task> tasks, List<Processor> processors) {
        for (Task task : tasks) {
            getAverageEarliestStartTime(task, processors);
        }
        for (Task task : tasks) {
            getAverageLatestStartTime(task, processors);
        }
    }

    private double getAverageEarliestStartTime(Task task, List<Processor> processors) {
        if (averageEarliestStartTimes.containsKey(task)) {
            return averageEarliestStartTimes.get(task);
        }

        double averageEarliestStartTime = Double.MIN_VALUE;
        for (Task parent : task.getParents()) {
            double averageComputationCost = StatUtils.mean(Costs.getComputationCosts(parent, processors));
            double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
            double averageCommunicationCost = Costs.getCommunicationCost(parent, task, averageBandwidth);
            averageEarliestStartTime = Math.max(averageEarliestStartTime, getAverageEarliestStartTime(parent, processors) + averageComputationCost + averageCommunicationCost);
        }

        if (task.isEntry()) averageEarliestStartTime = 0.0;
        averageEarliestStartTimes.put(task, averageEarliestStartTime);

        return averageEarliestStartTimes.get(task);
    }

    private double getAverageLatestStartTime(Task task, List<Processor> processors) {
        if (averageLatestStartTimes.containsKey(task)) {
            return averageLatestStartTimes.get(task);
        }

        double averageLatestStartTime = Double.MAX_VALUE;
        double averageComputationCost = StatUtils.mean(Costs.getComputationCosts(task, processors));
        for (Task child : task.getChildren()) {
            double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
            double averageCommunicationCost = Costs.getCommunicationCost(task, child, averageBandwidth);
            averageLatestStartTime = Math.min(averageLatestStartTime, getAverageLatestStartTime(child, processors) - averageCommunicationCost);
        }

        if (task.isExit()) averageLatestStartTime = averageEarliestStartTimes.get(task);
        else averageLatestStartTime -= averageComputationCost;

        averageLatestStartTimes.put(task, averageLatestStartTime);
        return averageLatestStartTimes.get(task);
    }

    private void setCriticalNodes(List<Task> tasks) {
        for (Task task : tasks) {
            double averageEarliestStartTime = this.averageEarliestStartTimes.get(task);
            double averageLatestStartTime = this.averageLatestStartTimes.get(task);
            if (Priority.isEqual(averageEarliestStartTime, averageLatestStartTime)) {
                criticalNodes.add(task);
            }
        }
    }

    private void calculateCriticalNodeCostTable(List<Task> tasks, List<Processor> processors) {
        for (Task task : tasks) {
            for (Processor processor : processors) {
                calculateCriticalNodeCostTable(task, processors, processor);
            }
        }
    }

    private double calculateCriticalNodeCostTable(Task task, List<Processor> processors, Processor selectedProcessor) {
        if (criticalNodeCostTable.get(task).containsKey(selectedProcessor)) {
            return criticalNodeCostTable.get(task).get(selectedProcessor);
        }

        double criticalNodeCost = Double.MIN_VALUE;
        for (Task childTask : task.getChildren()) {
            double min = Double.MAX_VALUE;
            for (Processor otherProcessor : processors) {
                double childComputationCost = Costs.getComputationCost(childTask, otherProcessor);
                double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
                double averageCommunicationCost = Costs.getCommunicationCost(task, childTask, averageBandwidth);
                double communicationCost = selectedProcessor == otherProcessor ? 0.0 : averageCommunicationCost;
                double childCost = calculateCriticalNodeCostTable(childTask, processors, otherProcessor) + childComputationCost + communicationCost;
                min = Math.min(min, childCost);
            }
            criticalNodeCost = Math.max(criticalNodeCost, min);
        }

        if (task.isExit()) criticalNodeCost = 0.0;
        criticalNodeCostTable.get(task).put(selectedProcessor, criticalNodeCost);
        return criticalNodeCost;
    }

    private void setPriority(List<Task> tasks, List<Processor> processors) {
        for (Task task : tasks) {
            double averageComputationCost = StatUtils.mean(Costs.getComputationCosts(task, processors));
            double averagePessimisticCost = priority.getAverageRankValue(priority.getPessimisticCostTable(), task);
            task.setPriority(averagePessimisticCost + averageComputationCost);
        }
    }

    private void allocateTasks(Set<Task> readyTasks, Set<Task> executedTasks, List<Processor> processors) {
        while (!readyTasks.isEmpty()) {
            Task maxRankTask = readyTasks.stream()
                    .max(Comparator.comparingDouble(Task::getPriority))
                    .orElse(null);
            allocateTask(maxRankTask, processors);
            updateSchedulingList(readyTasks, executedTasks, maxRankTask);
        }
    }

    private void allocateTask(Task task, List<Processor> processors) {
        Processor chosenProcessor = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;
        boolean containsCriticalNode = containsCriticalNode(task);

        for (Processor processor : processors) {
            double earliestStartTime = Schedulers.getEarliestStartTime(task, processor, true);
            finishTime = Schedulers.findEarliestFinishTime(schedules.get(processor), task, processor, earliestStartTime, false);
            double criticalNodeCost = criticalNodeCostTable.get(task).get(processor);
            double currentEFT = containsCriticalNode ? finishTime : finishTime + criticalNodeCost;
            if (currentEFT < earliestFinishTime) {
                earliestFinishTime = finishTime;
                bestReadyTime = earliestStartTime;
                chosenProcessor = processor;
            }
        }
        Schedulers.findEarliestFinishTime(schedules.get(chosenProcessor), task, chosenProcessor, bestReadyTime, true);
    }

    private boolean containsCriticalNode(Task task) {
        if (criticalNodes.contains(task)) return false;
        for (Task child : task.getChildren()) {
            if (criticalNodes.contains(child)) return true;
        }
        return false;
    }
}
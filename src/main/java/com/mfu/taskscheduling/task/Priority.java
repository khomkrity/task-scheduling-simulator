package com.mfu.taskscheduling.task;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.util.Costs;
import org.apache.commons.math3.stat.StatUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Priority {
    private final Map<Task, Double> upwardRanks;
    private final Map<Task, Double> downwardRanks;
    private final Map<Task, Map<Processor, Double>> upwardRankMatrix;
    private final Map<Task, Map<Processor, Double>> optimisticCostTable;
    private final Map<Task, Map<Processor, Double>> pessimisticCostTable;
    private final Map<Task, Map<Processor, Double>> predictCostMatrix;

    public Priority(List<Task> tasks){
        upwardRanks = new HashMap<>();
        downwardRanks = new HashMap<>();
        upwardRankMatrix = new HashMap<>();
        optimisticCostTable = new HashMap<>();
        pessimisticCostTable = new HashMap<>();
        predictCostMatrix = new HashMap<>();
        for (Task task : tasks) {
            upwardRankMatrix.put(task, new HashMap<>());
            optimisticCostTable.put(task, new HashMap<>());
            pessimisticCostTable.put(task, new HashMap<>());
            predictCostMatrix.put(task, new HashMap<>());
        }
    }

    public void computeTaskPriorities(List<Task> tasks, List<Processor> processors) {
        for (Task task : tasks) {
            calculateUpwardRank(upwardRanks, task, processors);
            calculateDownwardRank(downwardRanks, task, processors);
            for (Processor processor : processors) {
                calculateUpwardRankMatrix(upwardRankMatrix, task, processors, processor);
                calculateOptimisticCostTable(optimisticCostTable, task, processors, processor);
                calculatePessimisticCostTable(pessimisticCostTable, task, processors, processor);
                calculatePredictCostMatrix(predictCostMatrix, task, processors, processor);
            }
        }
    }

    private double calculateUpwardRank(Map<Task, Double> upwardRanks, Task task, List<Processor> processors) {
        if (upwardRanks.containsKey(task)) {
            return upwardRanks.get(task);
        }

        double upwardRank = 0.0;
        double averageComputationCost = StatUtils.mean(Costs.getComputationCosts(task, processors));
        for (Task child : task.getChildren()) {
            double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
            double averageCommunicationCost = Costs.getCommunicationCost(task, child, averageBandwidth);
            double cost = calculateUpwardRank(upwardRanks, child, processors) + averageCommunicationCost;
            upwardRank = Math.max(upwardRank, cost);
        }

        if (task.isExit()) upwardRank = averageComputationCost;
        else upwardRank += averageComputationCost;
        upwardRanks.put(task, upwardRank);
        return upwardRanks.get(task);
    }


    private double calculateDownwardRank(Map<Task, Double> downwardRanks, Task task, List<Processor> processors) {
        if (downwardRanks.containsKey(task)) {
            return downwardRanks.get(task);
        }

        double downwardRank = 0.0;
        for (Task parent : task.getParents()) {
            double averageComputationCost = StatUtils.mean(Costs.getComputationCosts(parent, processors));
            double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
            double averageCommunicationCost = Costs.getCommunicationCost(parent, task, averageBandwidth);
            double cost = calculateDownwardRank(downwardRanks, parent, processors) + averageComputationCost + averageCommunicationCost;
            downwardRank = Math.max(downwardRank, cost);
        }

        if (task.isEntry()) downwardRank = 0.0;
        downwardRanks.put(task, downwardRank);
        return downwardRanks.get(task);
    }


    private double calculateUpwardRankMatrix(Map<Task, Map<Processor, Double>> upwardRankMatrix, Task selectedTask, List<Processor> processors, Processor selectedProcessor) {
        if (upwardRankMatrix.get(selectedTask).containsKey(selectedProcessor)) {
            return upwardRankMatrix.get(selectedTask).get(selectedProcessor);
        }
        double upwardRank = 0.0;
        double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
        for (Task childTask : selectedTask.getChildren()) {
            double computationCost = Costs.getComputationCost(selectedTask, selectedProcessor);
            double averageCommunicationCost = Costs.getCommunicationCost(selectedTask, childTask, averageBandwidth);
            double childRank = calculateUpwardRankMatrix(upwardRankMatrix, childTask, processors, selectedProcessor) + computationCost + averageCommunicationCost;
            upwardRank = Math.max(upwardRank, childRank);
        }
        if (selectedTask.isExit()) upwardRank = Costs.getComputationCost(selectedTask, selectedProcessor);
        upwardRankMatrix.get(selectedTask).put(selectedProcessor, upwardRank);
        return upwardRank;
    }


    private double calculateOptimisticCostTable(Map<Task, Map<Processor, Double>> optimisticCostTable, Task selectedTask, List<Processor> processors, Processor selectedProcessor) {
        if (optimisticCostTable.get(selectedTask).containsKey(selectedProcessor)) {
            return optimisticCostTable.get(selectedTask).get(selectedProcessor);
        }
        double optimisticCost = 0.0;
        for (Task childTask : selectedTask.getChildren()) {
            double minCost = Double.MAX_VALUE;
            for (Processor otherProcessor : processors) {
                double childComputationCost = Costs.getComputationCost(childTask, otherProcessor);
                double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
                double averageCommunicationCost = Costs.getCommunicationCost(selectedTask, childTask, averageBandwidth);
                double communicationCost = selectedProcessor == otherProcessor ? 0.0 : averageCommunicationCost;
                double childCost = calculateOptimisticCostTable(optimisticCostTable, childTask, processors, otherProcessor) + childComputationCost + communicationCost;
                minCost = Math.min(minCost, childCost);
            }
            optimisticCost = Math.max(optimisticCost, minCost);
        }

        if (selectedTask.isExit()) optimisticCost = 0.0;
        optimisticCostTable.get(selectedTask).put(selectedProcessor, optimisticCost);
        return optimisticCost;
    }


    private double calculatePessimisticCostTable(Map<Task, Map<Processor, Double>> pessimisticCostTable, Task selectedTask, List<Processor> processors, Processor selectedProcessor) {
        if (pessimisticCostTable.get(selectedTask).containsKey(selectedProcessor)) {
            return pessimisticCostTable.get(selectedTask).get(selectedProcessor);
        }
        double pessimisticCost = 0.0;
        for (Task childTask : selectedTask.getChildren()) {
            double max = 0.0;
            for (Processor otherProcessor : processors) {
                double childComputationCost = Costs.getComputationCost(childTask, otherProcessor);
                double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
                double averageCommunicationCost = Costs.getCommunicationCost(selectedTask, childTask, averageBandwidth);
                double communicationCost = selectedProcessor == otherProcessor ? 0.0 : averageCommunicationCost;
                double childCost = calculatePessimisticCostTable(pessimisticCostTable, childTask, processors, otherProcessor) + childComputationCost + communicationCost;
                max = Math.max(max, childCost);
            }
            pessimisticCost = Math.max(pessimisticCost, max);
        }

        if (selectedTask.isExit()) pessimisticCost = 0.0;
        pessimisticCostTable.get(selectedTask).put(selectedProcessor, pessimisticCost);
        return pessimisticCost;
    }

    private double calculatePredictCostMatrix(Map<Task, Map<Processor, Double>> predictCostMatrix, Task selectedTask, List<Processor> processors, Processor selectedProcessor) {
        if (predictCostMatrix.get(selectedTask).containsKey(selectedProcessor)) {
            return predictCostMatrix.get(selectedTask).get(selectedProcessor);
        }
        double predictCost = 0.0;
        for (Task childTask : selectedTask.getChildren()) {
            double minCost = Double.MAX_VALUE;
            for (Processor otherProcessor : processors) {
                double parentComputationCost = Costs.getComputationCost(selectedTask, otherProcessor);
                double childComputationCost = Costs.getComputationCost(childTask, otherProcessor);
                double averageBandwidth = StatUtils.mean(Costs.getBandwidths(processors));
                double averageCommunicationCost = Costs.getCommunicationCost(selectedTask, childTask, averageBandwidth);
                double communicationCost = selectedProcessor == otherProcessor ? 0.0 : averageCommunicationCost;
                double childCost = calculatePredictCostMatrix(predictCostMatrix, childTask, processors, otherProcessor) + parentComputationCost + childComputationCost + communicationCost;
                minCost = Math.min(minCost, childCost);
            }
            predictCost = Math.max(predictCost, minCost);
        }

        if (selectedTask.isExit())
            predictCost = Costs.getComputationCost(selectedTask, selectedProcessor);
        predictCostMatrix.get(selectedTask).put(selectedProcessor, predictCost);
        return predictCost;
    }

    /**
     * Returns true if the absolute difference between the two double values is less than 1e-10, and false otherwise.
     *
     * @param a the first double value to compare
     * @param b the second double value to compare
     * @return true if the difference between a and b is within the acceptable range of error, false otherwise
     */
    public static boolean isEqual(double a, double b) {
        // TODO: BIG DECIMAL?
        double epsilon = 1e-10;
        return Math.abs(a - b) < epsilon;
    }

    public double getAverageRankValue(Map<Task, Map<Processor, Double>> rankMatrix, Task task) {
        Map<Processor, Double> ranks = rankMatrix.get(task);
        double[] costs = ranks.values().stream().mapToDouble(Double::doubleValue).toArray();
        return StatUtils.mean(costs);
    }

    public Map<Task, Double> getUpwardRanks() {
        return upwardRanks;
    }

    public Map<Task, Double> getDownwardRanks() {
        return downwardRanks;
    }

    public Map<Task, Map<Processor, Double>> getUpwardRankMatrix() {
        return upwardRankMatrix;
    }

    public Map<Task, Map<Processor, Double>> getOptimisticCostTable() {
        return optimisticCostTable;
    }

    public Map<Task, Map<Processor, Double>> getPessimisticCostTable() {
        return pessimisticCostTable;
    }

    public Map<Task, Map<Processor, Double>> getPredictCostMatrix() {
        return predictCostMatrix;
    }
}

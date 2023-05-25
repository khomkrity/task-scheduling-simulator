package com.mfu.taskscheduling.output;

import com.mfu.taskscheduling.processor.Processor;
import com.mfu.taskscheduling.util.Results;

/**
 * Represents the result data for a processor after the scheduling algorithm execution.
 */
public class ProcessorResult {
    private final String name;
    private final double mips;
    private final double bandwidth;
    private final double resourceUtilization;

    /**
     * Constructs a new ProcessorResult instance based on the provided processor and its total running time.
     *
     * @param processor        the processor object containing the necessary information for this result
     * @param totalRunningTime the total running time of all processors in the system
     */
    public ProcessorResult(Processor processor, double totalRunningTime) {
        this.name = processor.getName();
        this.mips = processor.getMips();
        this.bandwidth = processor.getBandwidth();
        this.resourceUtilization = Results.getResourceUtilization(processor, totalRunningTime);
    }

    /**
     * Returns the name of the processor.
     *
     * @return the name of the processor
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the resource utilization percentage of the processor.
     *
     * @return the resource utilization percentage
     */
    public double getResourceUtilization() {
        return resourceUtilization;
    }

    /**
     * Returns the computation capacity (MIPS) of the processor.
     *
     * @return the MIPS value
     */
    public double getMips() {
        return mips;
    }

    /**
     * Returns the bandwidth of the processor.
     *
     * @return the bandwidth
     */
    public double getBandwidth() {
        return bandwidth;
    }

    /**
     * Returns a string representation of the processor result.
     *
     * @return a string representation of the processor result
     */
    public String toString() {
        return name + ": " + resourceUtilization + "%";
    }

}

package com.mfu.taskscheduling.processor;

/**
 * Represents a processor in a task scheduling problem.
 * A processor is responsible for executing tasks and has various properties, such as MIPS (millions of instructions per second),
 * bandwidth, and cost per MIPS. The class also tracks the processor's ready time and estimated ready time,
 * which are used to manage task assignments and scheduling.
 */
public class Processor {
    private final String name;
    private final int id;
    private final double mips;
    private final double bandwidth;
    private final double costPerMips;
    private double readyTime;
    private double estimatedReadyTime;
    private double runningTime;

    /**
     * Constructs a Processor object with the given parameters.
     *
     * @param id          the processor's ID
     * @param name        the processor's name
     * @param mips        the processor's millions of instructions per second (MIPS) value
     * @param bandwidth   the processor's bandwidth
     * @param costPerMips the processor's cost per MIPS
     */
    public Processor(int id, String name, double mips, double bandwidth, double costPerMips) {
        this.id = id;
        this.name = name;
        this.mips = mips;
        this.bandwidth = bandwidth;
        this.costPerMips = costPerMips;
    }

    /**
     * Returns the processor's name.
     *
     * @return the processor's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the processor's ID.
     *
     * @return the processor's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the processor's MIPS value.
     *
     * @return the processor's MIPS value
     */
    public double getMips() {
        return mips;
    }

    /**
     * Returns the processor's bandwidth.
     *
     * @return the processor's bandwidth
     */
    public double getBandwidth() {
        return bandwidth;
    }

    /**
     * Returns the processor's cost per MIPS.
     *
     * @return the processor's cost per MIPS
     */
    public double getCostPerMips() {
        return costPerMips;
    }

    /**
     * Returns the processor's ready time.
     *
     * @return the processor's ready time
     */
    public double getReadyTime() {
        return readyTime;
    }

    /**
     * Sets the processor's ready time to the maximum of the current ready time and the given ready time.
     *
     * @param readyTime the ready time to compare and set
     */
    public void setReadyTime(double readyTime) {
        this.readyTime = Math.max(this.readyTime, readyTime);
    }

    /**
     * Returns the processor's estimated ready time.
     *
     * @return the processor's estimated ready time
     */
    public double getEstimatedReadyTime() {
        return estimatedReadyTime;
    }

    /**
     * Sets the processor's estimated ready time.
     *
     * @param estimatedReadyTime the estimated ready time to set
     */
    public void setEstimatedReadyTime(double estimatedReadyTime) {
        this.estimatedReadyTime = estimatedReadyTime;
    }

    /**
     * Resets the processor's ready time, estimated ready time and accumulated busy time to their default values (0.0).
     */
    public void reset() {
        double defaultValue = 0.0;
        this.readyTime = defaultValue;
        this.estimatedReadyTime = defaultValue;
        this.runningTime = defaultValue;
    }

    /**
     * Returns the processor's total running time.
     *
     * @return the processor's total running time
     */
    public double getRunningTime() {
        return runningTime;
    }

    /**
     * Adds the processor's accumulated running time.
     *
     * @param runningTime the current processor's running time
     */
    public void addRunningTime(double runningTime) {
        this.runningTime += runningTime;
    }
}

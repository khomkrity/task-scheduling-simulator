package com.mfu.taskscheduling.algorithm.util;

/**
 * This is a record class representing a time interval or slot in the task schedule.
 * A Timeslot object has a start time and a finish time, defining the period during which
 * a certain task is allocated to be processed.
 * <p>
 * The start time and finish time should be in the same time unit
 * and represent the same point in time from which they measure.
 * <p>
 * Instances of this class are immutable. Once a Timeslot object is created, its start and
 * finish times cannot be changed.
 *
 * @param startTime  The start time of the timeslot.
 * @param finishTime The finish time of the timeslot.
 */
public record Timeslot(double startTime, double finishTime) {
}

package automaton;

import com.google.common.collect.BoundType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@AllArgsConstructor
public class Guard {
    private List<Interval> intervals;

    public static final Guard COMPLEMENT_GUARD = Guard.create(Interval.COMPLEMENT_INTERVAL);

    public static Guard create(Interval interval) {
        List<Interval> intervals = new ArrayList<>();
        intervals.add(interval);
        return new Guard(intervals);
    }

    public static Guard create() {
        return new Guard(new ArrayList<>());
    }

    public void putInterval(Interval interval){
        if(intervals != null && !intervals.contains(interval)){
            intervals.add(interval);
            intervals.sort(new Comparator<Interval>() {
                @Override
                public int compare(Interval o1, Interval o2) {
                    if (o1.lowerEndpoint() > o2.lowerEndpoint()) {
                        return 1;
                    } else if (o1.lowerEndpoint() < o2.lowerEndpoint()) {
                        return -1;
                    } else {
                        if (o1.lowerBoundType() == BoundType.OPEN && o2.lowerBoundType() == BoundType.CLOSED) {
                            return 1;
                        } else if (o1.lowerBoundType() == BoundType.CLOSED && o2.lowerBoundType() == BoundType.OPEN) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                }
            });
        }
    }

    public boolean enableAction(double clockVal){
        for (Interval interval : intervals) {
            if (interval.contains(clockVal)) {
                return true;
            }
        }
        return false;
    }

    public List<Double> getEndpoints(double regionNormalization) {
        List<Double> endpoints = new ArrayList<>();
        for (Interval interval : intervals) {
            if (interval.lowerBoundType() == BoundType.CLOSED) {
                endpoints.add(interval.lowerEndpoint());
            } else {
                endpoints.add(interval.lowerEndpoint()-regionNormalization);
            }
        }
        return endpoints;
    }

    public boolean hasBound(){
        Interval lastInterval = intervals.get(intervals.size() - 1);
        return lastInterval.hasBound();
    }

    public double getMaxBound(){
        if (hasBound()) {
            Interval lastInterval = intervals.get(intervals.size() - 1);
            return lastInterval.upperBoundType() == BoundType.CLOSED ? lastInterval.upperEndpoint(): lastInterval.upperEndpoint() - 0.5;
        }
        return Integer.MAX_VALUE;
    }

    public double getMinBound(){
        if (intervals.get(0).lowerBoundType() == BoundType.OPEN)
            return intervals.get(0).lowerEndpoint() + 0.5;
        return intervals.get(0).lowerEndpoint();
    }

    public double getRandomValue (double minValue, int bound) {
        List<Interval> validInterviews = new ArrayList<>();
        for (Interval interview : intervals) {
            if (!interview.hasBound() || interview.upperEndpoint() >= minValue) {
                validInterviews.add(interview);
            }
        }
        return validInterviews.get((int) (Math.random() * validInterviews.size())).getRandomValue(minValue, bound);
    }

    public List<Interval> getCompleteIntervals(){
        List<Interval> completeIntervals = new ArrayList<>();
        double left = 0, right;
        BoundType leftBoundType = BoundType.CLOSED, rightBoundType;
        for (Interval interval : intervals) {
            right = interval.lowerEndpoint();
            rightBoundType = interval.lowerBoundType() == BoundType.CLOSED ? BoundType.OPEN : BoundType.CLOSED;
            if (left < right || (leftBoundType == BoundType.CLOSED && rightBoundType == BoundType.CLOSED)) {
                completeIntervals.add(Interval.create(left, leftBoundType, right, rightBoundType));
            }
            if (interval.hasBound()) {
                left = interval.upperEndpoint();
                leftBoundType = interval.upperBoundType() == BoundType.CLOSED ? BoundType.OPEN : BoundType.CLOSED;
            } else {
                break;
            }
        }
        if (hasBound()) {
            completeIntervals.add(Interval.create(left, leftBoundType));
        }
        return completeIntervals;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Interval interval : intervals){
            sb.append(interval).append("U");
        }
        sb.deleteCharAt(sb.lastIndexOf("U"));
        return sb.toString();
    }
}

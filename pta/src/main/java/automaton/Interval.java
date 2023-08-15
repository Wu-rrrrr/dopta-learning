package automaton;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Interval {
    private Range<Double> interval;
    private Random random;

    public static final Interval COMPLEMENT_INTERVAL = Interval.create(0);

    public static Interval create(String string){
        Interval res = new Interval();
        BoundType leftBoundType, rightBoundType;
        double left,right;
        string = string.trim();
        if (string.charAt(0) == '[') {
            leftBoundType = BoundType.CLOSED;
        } else {
            leftBoundType = BoundType.OPEN;
        }
        if (string.charAt(string.length() - 1) == ']') {
            rightBoundType = BoundType.CLOSED;
        } else {
            rightBoundType = BoundType.OPEN;
        }
        String[] points = string.split("\\,|\\[|\\(|\\]|\\)");
        left = Double.parseDouble(points[1]);

        if("+".equals(points[2])) {
            res.interval = Range.downTo(left, leftBoundType);
        }
        else{
            right = Double.parseDouble(points[2]);
            res.interval = Range.range(left, leftBoundType, right, rightBoundType);
        }
        res.random = new Random();
        return res;
    }
    public static Interval create(double left, BoundType leftBoundType, double right, BoundType rightBoundType){
        Interval res = new Interval();
        res.interval = Range.range(left, leftBoundType, right, rightBoundType);
        res.random = new Random();
        return res;
    }
    public static Interval create(double left, double right){
        Interval res = new Interval();
        BoundType leftBoundType, rightBoundType;
        double leftFloor = Math.floor(left);
        double rightFloor = Math.floor(right);
        if(left > leftFloor){
            leftBoundType = BoundType.OPEN;
            left = leftFloor;
        }
        else {
            leftBoundType = BoundType.CLOSED;
        }
        if(right > rightFloor){
            rightBoundType = BoundType.CLOSED;
            right = rightFloor;
        }
        else {
            rightBoundType = BoundType.OPEN;
        }
        res.interval = Range.range(left, leftBoundType, right, rightBoundType);
        res.random = new Random();
        return res;
    }
    public static Interval create(double left, BoundType leftBoundType){
        Interval res = new Interval();
        res.interval = Range.downTo(left, leftBoundType);
        res.random = new Random();
        return res;
    }
    public static Interval create(double left){
        Interval res = new Interval();
        BoundType leftBoundType;
        double leftFloor = Math.floor(left);
        if(left > leftFloor){
            leftBoundType = BoundType.OPEN;
            left = leftFloor;
        }
        else {
            leftBoundType = BoundType.CLOSED;
        }
        res.interval = Range.downTo(left, leftBoundType);
        res.random = new Random();
        return res;
    }

    public double getMaxBound(){
        if (interval.hasUpperBound()) {
            return interval.upperBoundType() == BoundType.CLOSED ? interval.upperEndpoint() : interval.upperEndpoint() - 0.5;
        }
        return Double.MAX_VALUE;
    }

    public double getRandomValue(double minVal, int upperBound){
        double left = interval.lowerBoundType() == BoundType.CLOSED ? interval.lowerEndpoint() : interval.lowerEndpoint() + 0.5;
        double right = interval.hasUpperBound() ? (interval.upperBoundType() == BoundType.CLOSED ? interval.upperEndpoint() : interval.upperEndpoint() - 0.5) : upperBound;
        if (right - left == 0) {
            return left;
        }
        if (minVal > left)
            left = minVal;
        if (right == left)
            return left;
        if (right-left < 0)
            throw new Error(String.format("random value in interview fail for %s, min:%f", interval.toString(), minVal));
        int k = random.nextInt((int) ((right - left) * 2));
        return left + k / 2.0;
    }

    public boolean hasBound(){ return interval.hasUpperBound();}
    public BoundType lowerBoundType() { return interval.lowerBoundType();}
    public BoundType upperBoundType() { return interval.upperBoundType();}
    public double lowerEndpoint(){ return interval.lowerEndpoint();}
    public double upperEndpoint(){ return interval.upperEndpoint();}

    public boolean isConnected(Interval interval) {
        return this.interval.isConnected(interval.getInterval());
    }

    public Interval span (Interval interval) {
        return new Interval(this.interval.span(interval.getInterval()), new Random());
    }

    public boolean contains(double number){
        return interval.contains(number);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (interval.lowerBoundType() == BoundType.CLOSED) {
            sb.append("[");
        } else {
            sb.append("(");
        }
        sb.append(interval.lowerEndpoint()).append(",");
        if (interval.hasUpperBound()) {
            sb.append(interval.upperEndpoint());
            if (interval.upperBoundType() == BoundType.CLOSED) {
                sb.append("]");
            } else {
                sb.append(")");
            }
        } else {
            sb.append("+)");
        }
        return sb.toString();
    }
}

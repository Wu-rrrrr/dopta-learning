package automaton;

import automaton.Input;
import automaton.Location;
import automaton.Output;
import automaton.Transition;
import com.google.common.collect.BoundType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import trace.ResetTimedTrace;
import trace.TimedInput;
import trace.TimedOutput;
import trace.TimedTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PTA {
    private Location initial = null;
    private Set<Input> inputs = new HashSet<>();
    private Set<Location> locations = new HashSet<>();

    public void addLocation(Location l) {
        locations.add(l);
    }

    public void removeState(Location l) {
        locations.remove(l);
    }

    public FastImmPair<Location, Double> getStateReachedByResetLogicalTimedTrace(ResetTimedTrace logicalTimedTrace) {
        if (logicalTimedTrace.length() == 1) {
            return FastImmPair.of(initial, 0.0);
        }
        Location currentLocation = initial;
        for (FastImmPair<TimedInput, TimedOutput> step : logicalTimedTrace.getTrace()) {
            boolean existed = false;
            Set<Transition> successors = currentLocation.getTransitions().get(step.left.getInput())
                    .stream().filter(transition -> transition.getGuard().enableAction(step.left.getClockVal()))
                    .collect(Collectors.toSet());
            for (Transition successor : successors) {
                if (successor.getTarget().getLabel().equals(step.right.getOutput())
                        && successor.isReset() == step.right.isReset()) {
                    existed = true;
                    currentLocation = successor.getTarget();
                }
            }
            if (!existed)
                return null;
        }
        FastImmPair<TimedInput, TimedOutput> lastStep = logicalTimedTrace.get(logicalTimedTrace.length()-2);
        return FastImmPair.of(currentLocation, lastStep.right.isReset() ? 0.0 : lastStep.left.getClockVal());
    }

    public Map<TimedOutput, Double> getDistribution(Location source, double clockVal, TimedInput input) {
        Map<TimedOutput, Double> distribution = new HashMap<>();
        Set<Transition> successors = source.getTransitions().get(input.getInput())
                .stream().filter(transition -> transition.getGuard().enableAction(clockVal + input.getClockVal()))
                .collect(Collectors.toSet());
        successors.forEach(transition -> distribution.put(TimedOutput.create(transition.isReset(), transition.getTarget().getLabel().getSymbol()),
                transition.getProbability()));
        return distribution;
    }

    public FastImmPair<Location, Double> getStateReachedByDelayTimedTrace(TimedTrace delayTimedTrace) {
        if (delayTimedTrace.length() < 1) {
            return FastImmPair.of(initial, 0.0);
        }
        Location currentLocation = initial;
        double currentClockValuation = 0.0;
        for (FastImmPair<TimedInput, Output> step : delayTimedTrace.getTrace()) {
            boolean existed = false;
            double finalClockValuation = currentClockValuation + step.left.getClockVal();
            Set<Transition> successors = currentLocation.getTransitions().get(step.left.getInput())
                    .stream().filter(transition -> transition.getGuard().enableAction(finalClockValuation))
                    .collect(Collectors.toSet());
            for (Transition successor : successors) {
                if (successor.getTarget().getLabel().equals(step.right)) {
                    existed = true;
                    currentLocation = successor.getTarget();
                    currentClockValuation = successor.isReset() ? 0.0 : finalClockValuation;
                }
            }
            if (!existed)
                return null;
        }
        return FastImmPair.of(currentLocation, currentClockValuation);
    }

    public PTA complement() {
        Location sink = new Location(locations.size(), Output.sink());
        for (Input input : inputs) {
            sink.addTransition(Guard.COMPLEMENT_GUARD, input, 1.0, true, sink);
        }

        PTA copied = deepCopy();

        boolean needComplete = false;
        for (Location source : copied.locations) {
            for (Input input : inputs) {
                Set<Transition> successors = source.getTransitions().get(input);
                if (successors == null || successors.isEmpty()) {
                    source.addTransition(Guard.COMPLEMENT_GUARD, input, 1.0, true, sink);
                    continue;
                }
                Set<Interval> intervalSet = new HashSet<>();
                for (Transition succ : successors) {
                    intervalSet.addAll(succ.getGuard().getIntervals());
                }
                List<Interval> intervals = new ArrayList<>(intervalSet);
                intervals.sort(new Comparator<Interval>() {
                    @Override
                    public int compare(Interval o1, Interval o2) {
                        int leftEndpointComp = Double.compare(o1.lowerEndpoint(), o2.lowerEndpoint());
                        if (leftEndpointComp != 0)
                            return leftEndpointComp;
                        else {
                            if (o1.lowerBoundType() == BoundType.CLOSED)
                                return -1;
                            else if (o1.lowerBoundType() == BoundType.OPEN)
                                return 1;
                            else
                                return 0;
                        }
                    }
                });

                List<Interval> completeInterviews = new ArrayList<>();
                Interval interval = intervals.get(0);
                if (interval.lowerBoundType() == BoundType.OPEN || interval.lowerEndpoint() > 0) {
                    completeInterviews.add(Interval.create(0, BoundType.CLOSED, interval.lowerEndpoint(),
                            interval.lowerBoundType() == BoundType.CLOSED ? BoundType.OPEN : BoundType.CLOSED));
                }
                interval = intervals.get(intervals.size() - 1);
                if (interval.hasBound()) {
                    completeInterviews.add(Interval.create(interval.upperEndpoint(),
                            interval.upperBoundType() == BoundType.CLOSED ? BoundType.OPEN : BoundType.CLOSED));
                }
                for (int i = 0; i + 1 < intervals.size(); i++) {
                    Interval i1 = intervals.get(i);
                    Interval i2 = intervals.get(i + 1);
                    if (!i1.isConnected(i2)) {
                        completeInterviews.add(Interval.create(i1.upperEndpoint(),
                                i1.upperBoundType() == BoundType.CLOSED ? BoundType.OPEN : BoundType.CLOSED,
                                i2.lowerEndpoint(),
                                i2.lowerBoundType() == BoundType.CLOSED ? BoundType.OPEN : BoundType.CLOSED));
                    }
                }
                if (!completeInterviews.isEmpty()) {
                    needComplete = true;
                    source.addTransition(new Guard(completeInterviews), input, 1.0, true, sink);
                }
            }
        }

        if (needComplete) {
            copied.addLocation(sink);
        }
        return copied;
    }

    public PTA deepCopy() {
        Map<Location, Location> locationMap = new HashMap<>();
        for (Location l : locations) {
            Location copiedLocation = new Location(l.getId(), l.getLabel());
            locationMap.put(l, copiedLocation);
        }

        for (Location source : locations) {
            Location copiedSource = locationMap.get(source);
            for (Transition successor : source.getAllTransitions()) {
                copiedSource.addTransition(successor.getGuard(), successor.getInput(), successor.getProbability(), successor.isReset(), locationMap.get(successor.getTarget()));
            }
        }

        return new PTA(locationMap.get(initial), new HashSet<>(inputs), new HashSet<>(locationMap.values()));
    }

    public PTA reverseComplement () {
        PTA copied = deepCopy();
        for (Location source : locations) {
            if (!source.getLabel().equals(Output.sink())) {
                for (Input input : inputs) {
                    source.getTransitions().get(input).removeIf(transition -> transition.getTarget().getLabel().equals(Output.sink()));
                }
            } else {
                copied.locations.remove(source);
            }
        }

        return copied;
    }

    public List<Transition> sortTran(Set<Transition> transitions) {
        List<Transition> transitionList = new ArrayList<>(transitions);
        transitionList.sort(new Comparator<Transition>() {
            @Override
            public int compare(Transition t1, Transition t2) {
                int sourceComp = Integer.compare(t1.getSource().getId(), t2.getSource().getId());
                if (sourceComp != 0)
                    return sourceComp;
                int inputComp = t1.getInput().getSymbol().compareTo(t2.getInput().getSymbol());
                if (inputComp != 0)
                    return inputComp;
                int guardComp = Double.compare(t1.getGuard().getMinBound(), t2.getGuard().getMinBound());
                if (guardComp != 0)
                    return guardComp;
                int probComp = Double.compare(t1.getProbability(), t2.getProbability());
                if (probComp != 0)
                    return probComp;
                return Integer.compare(t1.getTarget().getId(), t2.getTarget().getId());
            }
        });
        return transitionList;
    }

    public void show(){
        System.out.printf("init: (%d, %s)\n", initial.getId(), initial.getLabel());
        System.out.print("Location: ");
        for(Location s: locations){
            System.out.printf("(%d, %s)\t", s.getId(), s.getLabel());
        }
        System.out.println();
        System.out.println("Transitions: ");
        for (Location l : locations) {
            for (Input input : inputs) {
                Set<Transition> successors = l.getTransitions().get(input);
                if (successors != null && !successors.isEmpty()) {
                    List<Transition> sortedTrans = sortTran(successors);
                    sortedTrans.forEach(System.out::println);
                }
            }
        }
    }
}

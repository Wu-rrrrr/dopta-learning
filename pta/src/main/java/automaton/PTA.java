package automaton;

import automaton.Input;
import automaton.Location;
import automaton.Output;
import automaton.Transition;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.BoundType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.tuple.Triple;
import trace.*;
import trace.base.Trace;
import utils.FastImmPair;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PTA {
    private Location initial = null;
    private Set<Input> inputs = new HashSet<>();
    private Set<Location> locations = new HashSet<>();

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(String.format("initial: %d, %s \n", initial.getId(), initial.getLabel()));
        str.append(String.format("inputs: %s \n", inputs.toString()));
        str.append(locations.toString() + "\n");
        for (Location l : locations) {
            for (Transition t : l.getAllTransitions()) {
                str.append(t.toString()).append("\n");
            }
        }
        return str.toString();
    }

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
            if (!existed) {
                System.out.println(logicalTimedTrace);
                System.out.printf("location: %s, input: %s\n", currentLocation.getTraceRep(), step.left);
                return null;
            }
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

    public Triple<Location, Double, List<Boolean>> getStateReachedByLogicalTimedTrace(TimedTrace logicalTimedTrace) {
        if (logicalTimedTrace.length() == 1) {
            return Triple.of(initial, 0.0, new ArrayList<>());
        }
        Location currentLocation = initial;
        double clockVal = 0.0;
        List<Boolean> resets = new ArrayList<>();
        for (FastImmPair<TimedInput, Output> step : logicalTimedTrace.getTrace()) {
            boolean existed = false;
            if (step.left.getClockVal() - clockVal < 0) {
                return null;
            }
            Set<Transition> successors = currentLocation.getTransitions().get(step.left.getInput())
                    .stream().filter(transition -> transition.getGuard().enableAction(step.left.getClockVal()))
                    .collect(Collectors.toSet());
            for (Transition successor : successors) {
                if (successor.getTarget().getLabel().equals(step.right)) {
                    existed = true;
                    resets.add(successor.isReset());
                    clockVal = successor.isReset() ? 0.0 : step.left.getClockVal();
                    currentLocation = successor.getTarget();
                }
            }
            if (!existed) {
//                System.out.println(logicalTimedTrace);
//                System.out.printf("location: %s, input: %s\n", currentLocation.getTraceRep(), step.left);
                return null;
            }
        }

        return Triple.of(currentLocation, clockVal, resets);
    }

    public Map<TimedOutput, Double> getDistribution(Location source, TimedInput input) {
        Map<TimedOutput, Double> distribution = new HashMap<>();
        Set<Transition> successors = source.getTransitions().get(input.getInput())
                .stream().filter(transition -> transition.getGuard().enableAction(input.getClockVal()))
                .collect(Collectors.toSet());
        successors.forEach(transition -> distribution.put(TimedOutput.create(transition.isReset(), transition.getTarget().getLabel().getSymbol()),
                transition.getProbability()));
        return distribution;
    }

    public OutputDistribution outputDistributionQuery(TimedIncompleteTrace logicalTimedTestSeq) {
        TimedTrace trace = logicalTimedTestSeq.getTrace();
        TimedInput lastInput = logicalTimedTestSeq.get(logicalTimedTestSeq.length() - 1).right;
        Triple<Location, Double, List<Boolean>> triple = getStateReachedByLogicalTimedTrace(trace);
        if (triple == null || triple.getLeft() == null) {
//            System.out.printf("seq: %s, reason: unreachable%n", logicalTimedTestSeq);
            return new OutputDistribution();
        }
        if (lastInput.getClockVal() - triple.getMiddle() < 0) {
//            System.out.printf("seq: %s, reason: logical time error!%n", logicalTimedTestSeq);
            return new OutputDistribution();
        }
        Map<TimedOutput, Double> distribution = getDistribution(triple.getLeft(), lastInput);
        return new OutputDistribution(triple.getRight(), distribution);
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

    public void storeMemory(String path) {
        JSONObject pta = new JSONObject();
        pta.put("init", initial.getId());
        JSONObject locations = new JSONObject();
        for (Location l : this.locations) {
            locations.put(String.valueOf(l.getId()), l.getLabel().getSymbol());
        }
        pta.put("location", locations);
        int counter = 0;
        JSONObject transitions = new JSONObject();
        for (Location l : this.locations) {
            for (Map.Entry<Input, Set<Transition>> entry :
                    l.getTransitions().entrySet()) {
                Input input = entry.getKey();
                for (Transition t : entry.getValue()) {
                    Guard g = t.getGuard();
                    boolean reset = t.isReset();
                    Location target = t.getTarget();
                    double prob = t.getProbability();
                    for (Interval interval : g.getIntervals()) {
                        JSONArray transition = new JSONArray();
                        transition.add(l.getId());
                        transition.add(input.getSymbol());
                        transition.add(interval.toString());
                        transition.add(reset);
                        transition.add(target.getId());
                        transition.add(prob);
                        transitions.put(String.valueOf(counter), transition);
                        counter++;
                    }
                }
            }
        }
        pta.put("transition", transitions);

        String jsonStr = pta.toString();
        try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(path))))) {
            writer.write(jsonStr);
        }catch (IOException e){
            e.printStackTrace();
        }
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

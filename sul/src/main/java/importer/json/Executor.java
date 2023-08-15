package importer.json;

import automaton.*;
import trace.TimedIncompleteTrace;
import utils.FastImmPair;

import java.util.*;
import java.util.stream.Collectors;

public class Executor {
    Location currentLocation = null;
    private double currentClockVal = 0.0;
    private PTA adapter = null;
    private long seed = -1;
    private Random rnd = null;

    private int precision = 1000;
    private Map<Location, List<Transition>> transitions;

    public Executor(PTA adapter, long seed) {
        this.adapter = adapter;
        this.seed = seed;
        if (seed == -1) {
            this.seed = System.currentTimeMillis();
        }
        this.rnd = new Random(this.seed);

        transitions = new HashMap<>();
        for (Location source : adapter.getLocations()) {
            transitions.put(source, new ArrayList<>(source.getAllTransitions()));
        }
    }

    public String reset() {
        currentLocation = adapter.getInitial();
        currentClockVal = 0;

        return currentLocation.getLabel().getSymbol();
    }

    private Location sink() {
        for (Location l : adapter.getLocations()) {
            if (l.getLabel().equals(Output.sink()))
                return l;
        }
        return null;
    }

    public OutputDistribution execute(TimedIncompleteTrace logicalTimedTestSeq) {
        return adapter.outputDistributionQuery(logicalTimedTestSeq);
    }

    public FastImmPair<Boolean, String> execute(String input, double delayTime) {
//        if (delayTime < 0) {
//            currentClockVal = 0.0;
//            currentLocation = sink();
//            return FastImmPair.of(true, currentLocation.getLabel().getSymbol());
//        }
        if (delayTime < 0) {
            return null;
        }

//        List<Transition> transFromState = new ArrayList<>();
//        for (Transition successor : transitions.get(currentLocation)) {
//            if (successor.getInput().equals(Input.create(input))
//                    && successor.getGuard().enableAction(currentClockVal + delayTime)) {
//                transFromState.add(successor);
//            }
//        }
//        List<Transition> transFromState = currentLocation.getAllTransitions()
//                .stream().filter(transition -> transition.getInput().equals(Input.create(input))
//                        && transition.getGuard().enableAction(currentClockVal + delayTime)).collect(Collectors.toList());
//        if (transFromState.isEmpty()) {
//            System.out.println("target transition is not complete");
//            System.exit(0);
//        }
        double selectionProbability = (double) rnd.nextInt(precision) / precision;
//        double selectionProbability = rnd.nextDouble();
        for (Transition t : transitions.get(currentLocation)) {
            if (t.getInput().equals(Input.create(input)) && t.getGuard().enableAction(currentClockVal + delayTime)) {
                if (selectionProbability <= t.getProbability()) {
                    currentLocation = t.getTarget();
                    currentClockVal = t.isReset() ? 0.0 : currentClockVal + delayTime;
                    return FastImmPair.of(t.isReset(), currentLocation.getLabel().getSymbol());
                } else {
                    selectionProbability -= t.getProbability();
                }
            }
        }

        // self loop if input is not enabled in current state
        currentClockVal = currentClockVal + delayTime;
        return FastImmPair.of(false, currentLocation.getLabel().getSymbol());
    }
}

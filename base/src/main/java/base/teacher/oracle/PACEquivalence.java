package base.teacher.oracle;

import automaton.Location;
import automaton.PTA;
import automaton.Transition;
import base.Compatibility;
import base.learner.Answer;
import base.teacher.observationTree.ObservationTree;
import org.apache.commons.lang3.tuple.Triple;
import trace.*;
import utils.FastImmPair;

import java.util.*;
import java.util.stream.Collectors;

public class PACEquivalence extends EquivalenceOracle {

    private double unambiguousThreshold;
    private double epsilon;
    private double delta;

    private final OutputOracle outputOracle;

    public PACEquivalence(RandomTesting samplingMachine,
                          ObservationTree tree, OutputOracle outputOracle, Compatibility compatibilityChecker) {
        super(samplingMachine, tree, compatibilityChecker);
        this.outputOracle = outputOracle;
    }

    @Override
    public void init(Map<String, Object> parameters) {
        this.unambiguousThreshold = (double) parameters.get("unambiguousThreshold");
        this.epsilon = (double) parameters.get("epsilon");
        this.delta = (double) parameters.get("delta");
    }

    @Override
    public FastImmPair<Boolean, ResetTimedIncompleteTrace> findCex(int rounds, PTA hypo, double unambiguousRatio) {
        if (isChaosReachable(hypo) || unambiguousRatio < unambiguousThreshold) {
            if (unambiguousRatio < unambiguousThreshold)
                System.out.println("unambiguous ratio < unambiguous threshold");
            return FastImmPair.of(false, null);
        }

        int nrTest = (int) (1 / epsilon * (Math.log(1 / delta) + (rounds + 1) * Math.log(2)));
        Optional<ResetTimedTrace> cex = samplingMachine.sampleForFindCex(hypo, nrTest);
        if (cex.isPresent()) {
            return FastImmPair.of(false, cex.get().dropLastOutput());
        }

        List<ResetTimedTrace> sampledTraces = samplingMachine.getSampledTraces();
        sampledTraces.sort(new Comparator<ResetTimedTrace>() {
            @Override
            public int compare(ResetTimedTrace o1, ResetTimedTrace o2) {
                return Integer.compare(o1.length(), o2.length());
            }
        });
        List<TimedIncompleteTrace> incompleteTraces = new ArrayList<>();
        while (true) {
            List<ResetTimedTrace> copied = new ArrayList<>(sampledTraces);
            for (ResetTimedTrace sampledTrace : copied) {
                ResetTimedIncompleteTrace incompleteTrace = sampledTrace.dropLastOutput();
                // answer 必合法
                Answer treeAnswer =
                        tree.outputFrequenciesAndCompleteness(incompleteTrace.convert().getSteps());

                boolean isComplete = treeAnswer.isComplete();
                if (!isComplete) {
//                    System.out.printf("%s-%s\n", prefix.convert(), lastInput);
                    incompleteTraces.add(incompleteTrace.convert());
                    continue;
                }
                sampledTraces.remove(sampledTrace);

                Map<TimedOutput, Integer> treeFreq = treeAnswer.getFrequencies();
                Map<TimedOutput, Integer> hypFreq = getHypoFrequencies(hypo, incompleteTrace);
                if (!compatibilityChecker.compatible(treeFreq, hypFreq)) {
                    System.out.println(Optional.of(incompleteTrace));
                    return FastImmPair.of(false, incompleteTrace);
                }
            }
            if (incompleteTraces.isEmpty())
                break;
            List<ResetTimedTrace> sampled = outputOracle.performQueries(incompleteTraces);
            for (ResetTimedTrace trace : sampled) {
//                System.out.println(trace);
                tree.addObservationTrace(trace);
            }
            System.out.printf("未完全采样的序列有：%d\n", incompleteTraces.size());
            incompleteTraces.clear();
        }

        return FastImmPair.of(true, null);
    }

    public static Map<TimedOutput, Integer> getHypoFrequencies(PTA hypo, ResetTimedIncompleteTrace incompleteTrace) {
        ResetTimedTrace trace = incompleteTrace.getTrace();
        TimedInput lastInput = incompleteTrace.get(incompleteTrace.length()-1).right;

        Location location = hypo.getStateReachedByResetLogicalTimedTrace(trace).left;
        Map<TimedOutput, Integer> frequencies = new HashMap<>();
        Set<Transition> successors = location.getTransitions().get(lastInput.getInput())
                .stream().filter(transition -> transition.getGuard().enableAction(lastInput.getClockVal()))
                .collect(Collectors.toSet());
        for (Transition succ : successors) {
            frequencies.put(TimedOutput.create(succ.isReset(), succ.getTarget().getLabel().getSymbol()), succ.getFrequency());
        }
        return frequencies;
    }

}

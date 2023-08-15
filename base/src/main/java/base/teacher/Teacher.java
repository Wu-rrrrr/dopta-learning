package base.teacher;

import automaton.Input;
import automaton.Output;
import automaton.OutputDistribution;
import automaton.PTA;
import base.Compatibility;
import base.learner.Answer;
import base.teacher.observationTree.ObservationTree;
import base.teacher.oracle.*;
import base.teacher.oracle.Convergence;
import base.teacher.oracle.conv.ConvergenceCriterion;
import suls.SUL;
import trace.*;
import utils.FastImmPair;

import java.util.*;

public class Teacher {

    private final EquivalenceOracle equivalenceOracle;
    private final OutputOracle outputOracle;
    private final ObservationTree tree;

    public Teacher(Random randomSource, double stopProb, int bound, double regionNormalization,
                   Set<Input> inputs, SUL sul, OutputOracle outputOracle,
                   Compatibility compatibilityChecker, ConvergenceCriterion convCrit, EqMode mode) {
        tree = new ObservationTree(inputs);
        this.outputOracle = outputOracle;
        RandomTesting samplingMachine = new RandomTesting(outputOracle, sul, tree,
                randomSource, stopProb, inputs, bound, regionNormalization);
        if (mode == EqMode.PAC) {
            this.equivalenceOracle = new PACEquivalence(samplingMachine, tree, outputOracle, compatibilityChecker);
        } else {
            this.equivalenceOracle = new Convergence(samplingMachine, tree, compatibilityChecker, convCrit);
        }
    }

    private Map<String, Object> parameters;
    public void init() {
        equivalenceOracle.init(parameters);
    }

    public void setPacParameters(double unambiguousThreshold, double epsilon, double delta) {
        parameters = new HashMap<>();
        parameters.put("unambiguousThreshold", unambiguousThreshold);
        parameters.put("epsilon", epsilon);
        parameters.put("delta", delta);
    }

    public void setConvParameters(int maxTrie) {
        parameters = new HashMap<>();
        parameters.put("maxTrie", maxTrie);
    }

    public Answer query(ResetTimedTrace prefix, TimedSuffixTrace suffix) {
        return tree.outputFrequenciesAndCompleteness(prefix, suffix);
    }

    public FastImmPair<Map<TimedOutput, Integer>, Boolean> frequencyQuery(ResetTimedIncompleteTrace ts) {
        Answer treeFrequenciesAndCompleteness =
                tree.outputFrequenciesAndCompleteness(ts.convert().getSteps());
        return FastImmPair.of(treeFrequenciesAndCompleteness.getFrequencies(), treeFrequenciesAndCompleteness.isComplete());
    }

    public Output getInitialOutput() {
        return outputOracle.initOutput();
    }

    public void refine(List<TimedIncompleteTrace> incompleteTraces) {
        List<ResetTimedTrace> sampledTraces = outputOracle.performQueries(incompleteTraces);
        sampledTraces.forEach(tree::addObservationTrace);
    }

    public FastImmPair<Boolean, ResetTimedIncompleteTrace> equivalenceQuery(int rounds, PTA hypothesis, double unambiguousRatio) {
        return equivalenceOracle.findCex(rounds, hypothesis, unambiguousRatio);
    }

    public OutputDistribution outputDistributionQuery(ResetTimedTrace trace, TimedSuffixTrace suffix) {
        return outputOracle.outputDistributionQuery(trace, suffix);
    }
}

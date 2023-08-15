package evaluation.config;

import automaton.Input;
import base.Compatibility;
import base.learner.Learner;
import base.learner.LearningSetting;
import base.teacher.Teacher;
import base.teacher.oracle.EqMode;
import base.teacher.oracle.OutputOracle;
import base.teacher.oracle.conv.ConvergenceCriterion;
import cTree.ClassificationTree;
import evaluation.learner_proxy.LearnerInstance;
import suls.LoggingSUL;
import suls.SUL;
import trace.ResetTimedTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ClassificationTreeConfig implements LearnerConfig{

    public ClassificationTreeConfig(long seed, SUL sul, Compatibility compChecker, Set<Input> inputs, int batchSizeFill,
                                  ConvergenceCriterion convCrit,
                                  double unambiguousThreshold, double epsilon, double delta, int maxTries,
                                  double stopProbTest, int bound, double regionNormalization, EqMode mode) {
        super();
        this.seed = seed;
        this.sul = new LoggingSUL(sul);
        this.compChecker = compChecker;
        this.inputs = inputs;
        this.batchSizeFill = batchSizeFill;
        this.convCrit = convCrit;
        this.stopProbTest = stopProbTest;
        this.bound = bound;
        this.regionNormalization = regionNormalization;
        this.mode = mode;
        this.unambiguousThreshold = unambiguousThreshold;
        this.epsilon = epsilon;
        this.delta = delta;
        this.maxTries = maxTries;

    }

    private boolean ignoreChaosInTesting = false;
    private boolean stopTestingAtCex = true;

    private long seed;
    private LoggingSUL sul = null;
    private final Compatibility compChecker;
    private final Set<Input> inputs;
    private final int batchSizeFill;
    private final ConvergenceCriterion convCrit;
    private final double stopProbTest;
    private final int bound;
    private final double regionNormalization;

    private EqMode mode;
    private double unambiguousThreshold;
    private final double epsilon;
    private final double delta;
    private final int maxTries;

    @Override
    public LearnerInstance instantiate() throws Exception {
        sul.clearTraces();
        Random randomSource = new Random(seed);
        OutputOracle outputOracle = new OutputOracle(randomSource, sul, batchSizeFill);
        Teacher teacher = new Teacher(randomSource, stopProbTest, bound, regionNormalization,
                inputs, sul, outputOracle, compChecker, convCrit, mode);
        if (mode == EqMode.PAC) {
            teacher.setPacParameters(unambiguousThreshold, epsilon, delta);
        } else {
            teacher.setConvParameters(maxTries);
        }

        Learner learner = new ClassificationTree(inputs, compChecker, teacher);
        LearningSetting setting = new LearningSetting(Integer.MAX_VALUE, null);

        teacher.init();

        return new LearnerInstance(setting, learner) {

            @Override
            public List<ResetTimedTrace> loggedSampleTraces() {
                List<ResetTimedTrace> traces = new ArrayList<>(sul.getTraces());
                sul.clearTraces();
                return traces;
            }

            @Override
            public long getNrSteps() {
                return sul.getNrTests();
            }

            @Override
            public long getNrTests() {
                return sul.getNrSample();
            }
        };
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public int getBound() {
        return this.bound;
    }

    @Override
    public String description() {
        return "tree" + (compChecker.isAdaptive() ? "(adapt.)" :"");
    }

    @Override
    public String fileNameBase() {
        return "CTree_pta" + (compChecker.isAdaptive() ? "_adaptive" : "");
    }

    @Override
    public List<FastImmPair<String, String>> parameters() {
        List<FastImmPair<String, String>> parameters = new ArrayList<>();
        parameters.add(FastImmPair.of("seed", Long.toString(seed)));
        parameters.add(FastImmPair.of("compatibility checker", compChecker.description()));
        parameters.add(FastImmPair.of("n_resample", Integer.toString(batchSizeFill)));
        parameters.add(FastImmPair.of("stopping criterion", convCrit.description()));
        parameters.add(FastImmPair.of("stop-probability test", Double.toString(stopProbTest)));
        parameters.add(FastImmPair.of("adaptive alpha", Boolean.toString(compChecker.isAdaptive())));
        parameters.add(FastImmPair.of("ignore chaos", Boolean.toString(ignoreChaosInTesting)));
        parameters.add(FastImmPair.of("equivalence oracle mode", mode.name()));
        parameters.add(FastImmPair.of("unambiguousThreshold", Double.toString(unambiguousThreshold)));
        parameters.add(FastImmPair.of("epsilon", Double.toString(epsilon)));
        parameters.add(FastImmPair.of("delta", Double.toString(delta)));
        parameters.add(FastImmPair.of("maxTries", Integer.toString(maxTries)));
        return parameters;
    }
}

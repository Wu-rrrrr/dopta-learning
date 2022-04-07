package evaluation.config.experiment_configs;

import base.Compatibility;
import base.teacher.oracle.EqMode;
import base.teacher.oracle.conv.RoundBasedAndUnambigTraceCriterion;
import evaluation.config.ClassificationTreeConfig;
import evaluation.config.LearnerConfig;
import evaluation.config.ObservationTableConfig;
import suls.SUL;

public class RandomModelConfig126420 {
    private static double unambiguousThreshold = 1;
    private static double epsilon = 0.01;
    private static double delta = 0.01;
    private static int maxTries = 1000;
    private static double stopProbTest = 0.5;
    private static int bound = 21;
    private static double regionNormalization = 0.1;

    public static LearnerConfig observationTable(long seed, SUL sul, EqMode mode) {
        int batchSizeFill = 200;
        return new ObservationTableConfig(seed, sul, new Compatibility(0.05, false, true),
                sul.getInputs(), batchSizeFill,
                new RoundBasedAndUnambigTraceCriterion(3, 200, 0.99),
                unambiguousThreshold, epsilon, delta, maxTries,
                stopProbTest, bound, regionNormalization, mode);
    }

    public static LearnerConfig classificationTree(long seed, SUL sul, EqMode mode) {
        int batchSizeFill = 200;
        return new ClassificationTreeConfig(seed, sul, new Compatibility(0.05, false, true),
                sul.getInputs(), batchSizeFill,
                new RoundBasedAndUnambigTraceCriterion(3, 200, 0.99),
                unambiguousThreshold, epsilon, delta, maxTries,
                stopProbTest, bound, regionNormalization, mode);
    }
}

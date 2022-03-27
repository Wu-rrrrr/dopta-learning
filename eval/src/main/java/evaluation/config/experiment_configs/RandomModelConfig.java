package evaluation.config.experiment_configs;

import evaluation.config.LearnerConfig;
import evaluation.config.ObservationTableConfig;
import base.Compatibility;
import base.teacher.oracle.EqMode;
import base.teacher.oracle.conv.RoundBasedAndUnambigTraceCriterion;
import suls.SUL;

public class RandomModelConfig {

    private static double unambiguousThreshold = 0.99;
    private static double epsilon = 0.01;
    private static double delta = 0.01;
    private static int maxTries = 1000;
    private static double stopProbTest = 0.35;
    private static int bound = 21;
    private static double regionNormalization = 0.1;

    public static LearnerConfig observationTable(long seed, SUL sul, EqMode mode) {
        int batchSizeFill = 800;
        return new ObservationTableConfig(seed, sul, new Compatibility(0.05, false, true),
                sul.getInputs(), batchSizeFill,
                new RoundBasedAndUnambigTraceCriterion(3, 200, 0.99),
                unambiguousThreshold, epsilon, delta, maxTries,
                stopProbTest, bound, regionNormalization, mode);
    }
}
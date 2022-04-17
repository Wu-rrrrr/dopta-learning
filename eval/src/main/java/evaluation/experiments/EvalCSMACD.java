package evaluation.experiments;

import base.teacher.oracle.EqMode;
import evaluation.Evaluator;
import evaluation.config.experiment_configs.CSMACDConfig;
import importer.json.JsonSUL;

public class EvalCSMACD {
    public static void main(String args[]) throws Exception{
        long seed = System.currentTimeMillis();
        String trueSulFile = "eval/src/main/resources";
        String modelName = "CSMA-CD";

        Evaluator evaluator = new Evaluator(seed);
        for (int i = 1; i <= 10; i++) {
            JsonSUL trueSUL = JsonSUL.getPtaFromJsonFile(String.format("%s/%s.json", trueSulFile, modelName));
            trueSUL.init(seed);

            evaluator.setSul(trueSUL, modelName, i);
			evaluator.addConfig(CSMACDConfig.observationTable(seed, trueSUL, EqMode.PAC));
            evaluator.addConfig(CSMACDConfig.classificationTree(seed, trueSUL, EqMode.PAC));

            evaluator.evalTesting(20000, "results_only_testing/csma-cd");
        }

    }
}

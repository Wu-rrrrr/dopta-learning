package evaluation.experiments;

import base.teacher.oracle.EqMode;
import evaluation.Evaluator;
import evaluation.config.experiment_configs.ProtocolConfig;
import importer.json.JsonSUL;

public class EvalRootContention {
    public static void main(String args[]) throws Exception{
        long seed = System.currentTimeMillis();
        String trueSulFile = "eval/src/main/resources";
        String modelName = "Root-Contention";

        Evaluator evaluator = new Evaluator(seed);
        for (int i = 1; i <= 1; i++) {
            JsonSUL trueSUL = JsonSUL.getPtaFromJsonFile(String.format("%s/%s.json", trueSulFile, modelName));
            trueSUL.init(seed);

            evaluator.setSul(trueSUL, modelName, i);
            evaluator.addConfig(ProtocolConfig.observationTable(seed, trueSUL, EqMode.PAC));
//            evaluator.addConfig(ProtocolConfig.classificationTree(seed, trueSUL, EqMode.PAC));

            evaluator.evalProtocol(20000, "results_only_testing/root-contention");

        }

    }
}

import base.Compatibility;
import evaluation.SampleTest;
import importer.json.JsonSUL;
import org.junit.Test;

import java.io.IOException;


public class EvaluatePassRatioTest {
    @Test
    public void evaluatePassRatioTest() throws Exception {
        String[] methods = new String[2];
        methods[0] = "table";
        methods[1] = "tree";
        String trueSulFile = "D:\\Documents\\research\\code\\ptalearning\\eval\\src\\main\\resources\\randomModels";
        String hypoSulFile = "D:\\Documents\\research\\code\\ptalearning\\results_only_testing\\randomModels\\hypothesis";
        String modelName = "6_4_4_20";
        int bound = 21;
        Compatibility compChecker = new Compatibility(0.05, false, true);

        for (int i = 0; i < 2; i++) {
            double min = 1;
            double max = 0;
            double sum = 0;
            for (int j = 1; j <= 10; j++) {
                JsonSUL trueSUL = JsonSUL.getPtaFromJsonFile(String.format("%s/%s-%d.json", trueSulFile, modelName, j));
                JsonSUL hypoSul = JsonSUL.getPtaFromJsonFile(String.format("%s/%s-%d_hypo_%s.json", hypoSulFile, modelName, j, methods[i]));

                SampleTest sampleTest = new SampleTest(trueSUL.getTargetModel());
                sampleTest.setCompChecker(compChecker);

                sampleTest.evaluatePassRatio(hypoSul.getTargetModel(), 20000, bound);
                double passingRatio = sampleTest.getPassRatio();
                if (passingRatio <= min) {
                    min = passingRatio;
                }
                if (passingRatio >= max) {
                    max = passingRatio;
                }
                sum += passingRatio;
            }
            double avg = sum / 10;
            System.out.println(methods[i]);
            System.out.printf("min: %f, avg: %f, max: %f\n", min, avg, max);
        }

    }

    @Test
    public void evaluatePACParameterPassRatioTest() throws Exception {
        String[] methods = new String[2];
        methods[0] = "table";
        methods[1] = "tree";
        String trueSulFile = "D:\\Documents\\research\\code\\ptalearning\\eval\\src\\main\\resources\\randomModels";
        String hypoSulFile = "D:\\Documents\\research\\code\\ptalearning\\results_only_testing\\randomModels\\hypothesis";
        String modelName = "10_2_7_40";
        int bound = 41;
        Compatibility compChecker = new Compatibility(0.05, false, true);

        String[] parameters = new String[]{"01", "001", "0001"};

        for (int k = 0; k < 3; k++) {
            System.out.println(parameters[k]);
            for (int i = 0; i < 2; i++) {
                double min = 1;
                double max = 0;
                double sum = 0;
                for (int j = 1; j <= 10; j++) {
                    JsonSUL trueSUL = JsonSUL.getPtaFromJsonFile(String.format("%s/%s-%d.json", trueSulFile, modelName, j));
                    JsonSUL hypoSul = JsonSUL.getPtaFromJsonFile(String.format("%s/%s_%s-%d_hypo_%s.json", hypoSulFile, parameters[k], modelName, j, methods[i]));

                    SampleTest sampleTest = new SampleTest(trueSUL.getTargetModel());
                    sampleTest.setCompChecker(compChecker);

                    sampleTest.evaluatePassRatio(hypoSul.getTargetModel(), 20000, bound);
                    double passingRatio = sampleTest.getPassRatio();
                    if (passingRatio <= min) {
                        min = passingRatio;
                    }
                    if (passingRatio >= max) {
                        max = passingRatio;
                    }
                    sum += passingRatio;
                }
                double avg = sum / 10;
                System.out.println(methods[i]);
                System.out.printf("min: %f, avg: %f, max: %f\n", min, avg, max);
            }
        }

    }

    @Test
    public void evalProtocol() throws IOException {
        String[] methods = new String[2];
        methods[0] = "table";
        methods[1] = "tree";
        String trueSulFile = "D:\\Documents\\research\\code\\ptalearning\\eval\\src\\main\\resources";
        String hypoSulFile = "D:\\Documents\\research\\code\\ptalearning\\results_only_testing\\root-contention\\hypothesis";
        String modelName = "Root-Contention";
        int bound = 1701;
        Compatibility compChecker = new Compatibility(0.05, false, true);

        for (int i = 0; i < 2; i++) {
            JsonSUL trueSUL = JsonSUL.getPtaFromJsonFile(String.format("%s/%s.json", trueSulFile, modelName));
            JsonSUL hypoSul = JsonSUL.getPtaFromJsonFile(String.format("%s/%s_hypo_%s.json", hypoSulFile, modelName, methods[i]));

            SampleTest sampleTest = new SampleTest(trueSUL.getTargetModel());
            sampleTest.setCompChecker(compChecker);
            sampleTest.evaluatePassRatio(hypoSul.getTargetModel(), 20000, bound);
            double passingRatio = sampleTest.getPassRatio();
            System.out.printf("%s: %f\n", methods[i], passingRatio);
        }
    }
}

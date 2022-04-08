package evaluation.experiments;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;

public class RandomModelGenerator {
    private static final int SOURCE = 0;
    private static final int INPUT = 1;
    private static final int GUARD = 2;
    private static final int RESET = 3;
    private static final int TARGET = 4;
    private static final int PROBABILITY = 5;
    private static final double[][] probabilityCombination= {{0.6, 0.4}, {0.5, 0.3, 0.2}, {0.2, 0.4, 0.4}, {0.7, 0.3}, {0.5, 0.5}};

    public static void generate(int locationNumber,int inputNumber, int nrLabel, int upperBound, int probabilityEdgeRatio){
        upperBound *= 2;
        for (int i = 1; i <= 10; i++){
            String pta = locationNumber+"_" +inputNumber + "_" + nrLabel + "_" + upperBound / 2 + "-" + i;
            JSONObject json = new JSONObject();
            try {
                Random random = new Random();
                json.put("init", String.valueOf(0));

                Map<String, List<Integer>> labelMap = new HashMap<>();
                JSONObject locations = new JSONObject();
                for (int j = 0; j < locationNumber; j++){
                    String label = "l" + (j < nrLabel ? j : random.nextInt(nrLabel));
                    locations.put(String.valueOf(j), label);
                    labelMap.putIfAbsent(label, new ArrayList<>());
                    labelMap.get(label).add(j);
                }
                json.put("location", locations);

                int index = 0;
                JSONObject transitions = new JSONObject();
                for (int source = 0; source < locationNumber; source++) {
                    for (int j = 0; j < inputNumber; j++) {
                        String input = String.valueOf((char) ('a' + j));
                        int minLeftEndpoint = 0;
                        while (random.nextInt(5) > 0 && minLeftEndpoint < upperBound) {
                            // generate randomly guard
                            StringBuilder guard = new StringBuilder();
                            int left = minLeftEndpoint + random.nextInt(upperBound - minLeftEndpoint);
                            if (left % 2 == 0) {
                                guard.append("[").append(left / 2);
                            } else {
                                guard.append("(").append((left - 1) / 2);
                            }
                            guard.append(",");
                            if (random.nextBoolean()) {
                                guard.append("+)");
                                minLeftEndpoint = upperBound;
                            } else {
                                int right = left + random.nextInt(upperBound - left);
                                if (right % 2 == 0) {
                                    guard.append(right / 2).append("]");
                                } else {
                                    guard.append((right + 1) / 2).append(")");
                                }
                                minLeftEndpoint = right + 1;
                            }

                            // generate randomly distribution
                            if (random.nextInt(10) >= probabilityEdgeRatio) {
                                boolean reset = random.nextBoolean();
                                int target = random.nextInt(locationNumber);
                                JSONArray transition = new JSONArray();
                                transition.add(SOURCE, source);
                                transition.add(INPUT, input);
                                transition.add(GUARD, guard.toString());
                                transition.add(RESET, reset);
                                transition.add(TARGET, target);
                                transition.add(PROBABILITY, 1.0);
                                transitions.put(String.valueOf(index++), transition);
                            } else {
                                List<String> avaliableLabels = new ArrayList<>(labelMap.keySet());
                                double[] probabilities = probabilityCombination[random.nextInt(probabilityCombination.length)];
                                for (int k = 0; k < probabilities.length; k++) {
                                    String selectedLabel = avaliableLabels.get(random.nextInt(avaliableLabels.size()));
                                    avaliableLabels.remove(selectedLabel);

                                    List<Integer> targets = labelMap.get(selectedLabel);
                                    int target = targets.get(random.nextInt(targets.size()));
                                    boolean reset = random.nextBoolean();
                                    double probability = probabilities[k];

                                    JSONArray transition = new JSONArray();
                                    transition.add(SOURCE, source);
                                    transition.add(INPUT, input);
                                    transition.add(GUARD, guard.toString());
                                    transition.add(RESET, reset);
                                    transition.add(TARGET, target);
                                    transition.add(PROBABILITY, probability);
                                    transitions.put(String.valueOf(index++), transition);
                                }
                            }
                        }
                    }
                }
                json.put("transition", transitions);
            }catch (JSONException je){
                je.printStackTrace();
            }

            String jsonStr = json.toString();
            String filePath= "eval/src/main/resources/randomModels/" + pta + ".json";
            try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filePath))))) {
                writer.write(jsonStr);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        RandomModelGenerator.generate(4, 4, 3, 20, 1);
    }
}

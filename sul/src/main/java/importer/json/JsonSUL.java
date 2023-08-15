package importer.json;

import automaton.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import lombok.NoArgsConstructor;
import suls.SUL;
import trace.TimedIncompleteTrace;
import utils.FastImmPair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;

@NoArgsConstructor
public class JsonSUL implements SUL {
    private PTA target;
    private Executor executor;

    private static final int SOURCE = 0;
    private static final int INPUT = 1;
    private static final int GUARD = 2;
    private static final int RESET = 3;
    private static final int TARGET = 4;
    private static final int PROBABILITY = 5;


    private void getSULFromJson(String json) {
        JSONObject jsonObject = JSON.parseObject(json);

        int initId = Integer.parseInt(jsonObject.getString("init"));

        Set<Location> locations = new HashSet<>();
        Map<Integer, Location> locationMap = new HashMap<>();
        JSONObject locationJsonObject = jsonObject.getJSONObject("location");
        for(Map.Entry<String, Object> entry : locationJsonObject.entrySet()){
            int id = Integer.parseInt(entry.getKey());
            Location location = new Location(id, Output.create(entry.getValue().toString()));
            locations.add(location);
            locationMap.put(id, location);
        }

        Set<Input> inputs = new HashSet<>();
        JSONObject tranJsonObject = jsonObject.getJSONObject("transition");
        for (int i = 0; i < tranJsonObject.size(); i++) {

            JSONArray array = tranJsonObject.getJSONArray(String.valueOf(i));

            int sourceId = Integer.parseInt(array.getString(SOURCE));
            Location sourceLocation = locationMap.get(sourceId);

            Input inputSymbol = Input.create(array.getString(INPUT));
            inputs.add(inputSymbol);

            String g = array.getString(GUARD);
            Guard guard = getGuardFromString(g);

            boolean reset = array.getBoolean(RESET);

            int targetId = Integer.parseInt(array.getString(TARGET));
            Location targetLocation = locationMap.get(targetId);

            double probability = array.getDouble(PROBABILITY);

            sourceLocation.addTransition(guard, inputSymbol, probability, reset, targetLocation);
        }

        target = new PTA(locationMap.get(initId), inputs, locations).complement();
    }

    private Guard getGuardFromString(String s){
        if(s == null){
            return null;
        }
        List<Interval> guard = new ArrayList<>();
        String[] intervals = s.split("U");
        for (String value : intervals) {
            Interval interval = Interval.create(value);
            guard.add(interval);
        }
        return new Guard(guard);
    }

    public static JsonSUL getPtaFromJsonFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        String str = null;
        StringBuilder json = new StringBuilder();
        while ((str = reader.readLine()) != null) {
            json.append(str);
        }
        JsonSUL sul = new JsonSUL();
        sul.getSULFromJson(json.toString());
        return sul;
    }

    public PTA getTargetModel() {
        return target;
    }

    @Override
    public void init(long seed) throws Exception {
        executor = new Executor(target, seed);
    }

    @Override
    public String reset() {
        return executor.reset();
    }

    @Override
    public FastImmPair<Boolean, String> execute(String input, double clockValue) {
        if (executor == null)
            throw new IllegalStateException("Executor not initialised");

        return executor.execute(input, clockValue);
    }

    @Override
    public Set<Input> getInputs() {
        return target.getInputs();
    }

    @Override
    public OutputDistribution execute(TimedIncompleteTrace logicalTimedTestSeq) {
        return executor.execute(logicalTimedTestSeq);
    }
}

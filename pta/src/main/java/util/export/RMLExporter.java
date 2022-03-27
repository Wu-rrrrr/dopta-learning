/*******************************************************************************
 * Lmdp* - L*-Based Learning of Markov Decision Processes
 *  Copyright (C) 2019 TU Graz
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package util.export;

import automaton.*;
import com.google.common.collect.BoundType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Class for exporting Markov decision processes in the standard, guarded-command-like format
 * used by Prism model-checker (http://prismmodelchecker.org/).
 * 
 * @author Martin Tappler
 *
 */
public class RMLExporter {

	protected static final String locationVar = "loc";
	protected static final String stepInc = "(steps'=min(BOUND,steps + 1))";
	protected static final String clockVar = "x";
	private String modelName;
	private Set<Input> inputs;
	private Set<Output> outputs;
	
	
	public RMLExporter(String modelName, Set<Input> inputs, Set<Output> outputs){
		this.modelName = modelName;
		this.inputs = inputs;
		this.outputs = outputs;
	}
	public String toRML(PTA pta){
		return toRML(pta, -1);
	}
	
	public void toFile(PTA pta, String fileName) throws IOException{
		toFile(pta, -1, fileName);
	}
	public void toFile(PTA pta, int stepBound, String fileName) throws IOException{
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		try(FileWriter fw = new FileWriter(f)){
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(toRML(pta,stepBound));
			bw.flush();
		}
	}
	
	
	public String toRML(PTA pta, int stepBound){
		StringBuilder sb = new StringBuilder();
		appendLine(sb,modelType());
		appendStepBountConstant(sb,stepBound);
		appendLine(sb, "module " + modelName);
		Map<Integer,Integer> stateIdsRemapping = remapStates(pta);
		Integer initialIntId = stateIdsRemapping.get(pta.getInitial().getId());
		appendLine(sb, clockVariable());
		appendLine(sb, locationVariable(stateIdsRemapping,initialIntId));
		appendStepCountVar(sb,stepBound);
		appendTransitionDefinitions(sb,pta,stateIdsRemapping, stepBound > -1);
		appendLine(sb, "endmodule");
		appendOutLabels(sb,pta,stateIdsRemapping);
		return sb.toString();
	}
	public void appendStepBountConstant(StringBuilder sb, int stepBound) {
		if(stepBound > -1)
			appendLine(sb, String.format("const int BOUND = %d;", stepBound));
	}
	public void appendStepCountVar(StringBuilder sb, int stepBound) {
		if(stepBound > -1){
			 appendLine(sb,"steps : [0..BOUND] init 0;");
			  
		}
	}
	private void appendOutLabels(StringBuilder sb, PTA pta, Map<Integer, Integer> locationIdsRemapping) {
		Set<String> labels =  outputs.stream()
				.flatMap(o -> o.getSatisfiedProps().stream())
				.collect(Collectors.toSet());
		for(String l : labels){
			appendOutputSymbol(l, sb, pta, locationIdsRemapping);
		}
	}
	private void appendOutputSymbol(String label, StringBuilder sb, PTA pta, Map<Integer, Integer> locationIdsRemapping) {
		String labelFormula =
				pta.getLocations().stream()
					  .filter(s -> s.getLabel().getSatisfiedProps().contains(label))
					  .map(s -> String.format("%s=%d", locationVar,locationIdsRemapping.get(s.getId())))
					  .collect(Collectors.joining("|"));
		appendLine(sb,String.format("label \"%s\" = %s;", label,labelFormula));
	}
	private void appendTransitionDefinitions(StringBuilder sb, PTA pta, Map<Integer, Integer> locationIdsRemapping,
			boolean hasStepBound) {
		for(Location s : pta.getLocations()) {
			appendTransitionsForState(sb,s, locationIdsRemapping, hasStepBound);
		}
	}
	private void appendTransitionsForState(StringBuilder sb, Location l,
										   Map<Integer, Integer> stateIdsRemapping, boolean hasStepBound){
		for(Input input : inputs){
			Set<Transition> tsForInput = l.getTransitions().get(input);
			Map<Guard, Set<Transition>> guardSetMap = new HashMap<>();
			for (Transition successor : tsForInput) {
				guardSetMap.putIfAbsent(successor.getGuard(), new HashSet<>());
				guardSetMap.get(successor.getGuard()).add(successor);
			}
			for (Map.Entry<Guard, Set<Transition>> entry : guardSetMap.entrySet()) {
				appendTransitionDefinitions(sb,input, entry.getKey(), entry.getValue(),l, stateIdsRemapping,hasStepBound);
			}

		}
	}

	private String getGuardString(Guard guard) {
		StringBuilder str = new StringBuilder();
		for (Interval interval : guard.getIntervals()) {
			str.append("&");
			if (interval.lowerBoundType() == BoundType.CLOSED) {
				str.append(String.format("%s>=%d", clockVar, (int) interval.lowerEndpoint()));
			} else {
				str.append(String.format("%s>%d", clockVar, (int) interval.lowerEndpoint()));
			}
			if (interval.hasBound()) {
				str.append("&");
				if (interval.upperBoundType() == BoundType.CLOSED) {
					str.append(String.format("%s<=%d", clockVar, (int) interval.upperEndpoint()));
				} else {
					str.append(String.format("%s<%d", clockVar, (int) interval.upperEndpoint()));
				}
			}
		}
		return str.toString();
	}

	private void appendTransitionDefinitions(StringBuilder sb, Input input, Guard guard, Set<Transition> ts,
			Location l, Map<Integer, Integer> stateIdsRemapping, boolean hasStepBound) {
		if(ts.isEmpty())
			return;
		appendLine(sb, String.format("[%s] %s=%d %s -> ",input,
				locationVar,
				stateIdsRemapping.get(l.getId()), getGuardString(guard)));
		String transDefs = ts.stream()
		  .map(t -> {
		  String transString = String.format("%.10f : (%s'=%d)", 
				  t.getProbability(),
				  locationVar,
				  stateIdsRemapping.get(t.getTarget().getId()));
		  transString += hasStepBound ? ("&" + stepInc) : "";
		  return transString;
		  })
		  .collect(Collectors.joining(" + "));
		appendLine(sb, transDefs + ";");
	}
	
	private String locationVariable(Map<Integer, Integer> locationIdsRemapping, Integer initialIntId) {
		return String.format("%s : [0..%d] init %d;",locationVar, locationIdsRemapping.size() - 1,initialIntId);
	}

	private String clockVariable() {
		return String.format("%s : clock;", clockVar);
	}

	private Map<Integer, Integer> remapStates(PTA pta) {
		Map<Integer,Integer> result = new HashMap<>();
		int i = 0;
		for(Location l : pta.getLocations()){
			result.put(l.getId(), i++);
		}
		return result;
	}
	public String modelType(){
		return "pta";
	}
	public void appendLine(StringBuilder sb, String line){
		sb.append(line);
		sb.append(System.lineSeparator());
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
}
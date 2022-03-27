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
package base.teacher.observationTree;

import automaton.Input;
import automaton.Output;
import trace.TimedInput;
import trace.TimedOutput;
import utils.FastImmPair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A node in the tree storing the multiset of system traces. 
 * 
 * @author Martin Tappler
 *
 */
public class Node {
	private Map<FastImmPair<TimedOutput, TimedInput>,Node> children = new HashMap<>();
	private Map<TimedOutput, Integer> outputFrequencies = new HashMap<>();
	private boolean complete = ObservationTree.completenessThreshold == 0 ? true : false;
	// TODO add this field to table cells
	private int overallNrOutputs = 0;

	public void addOutput(TimedOutput output, Set<Input> inputs) {
		if(getOutputFrequencies().containsKey(output)){
			getOutputFrequencies().put(output, getOutputFrequencies().get(output)+1);
		} else {
			getOutputFrequencies().put(output, 1);
			// to ensure that we have all possible extension of trace extensions (with arbitrary inputs) in the tree
			for(Input input : inputs){
				children.put(FastImmPair.of(output, TimedInput.create(input)), new Node());
			}
		}
		setOverallNrOutputs(getOverallNrOutputs() + 1);
		if(getOverallNrOutputs() >= ObservationTree.completenessThreshold){
			complete = true;
		} 
	}

	public void addOutput(TimedOutput output) {
		if(getOutputFrequencies().containsKey(output)){
			getOutputFrequencies().put(output, getOutputFrequencies().get(output)+1);
		} else {
			getOutputFrequencies().put(output, 1);
			// to ensure that we have all possible extension of trace extensions (with arbitrary inputs) in the tree
		}
		setOverallNrOutputs(getOverallNrOutputs() + 1);
		if(getOverallNrOutputs() >= ObservationTree.completenessThreshold){
			complete = true;
		}
	}

	public void addOutput(TimedOutput output, TimedInput input) {
		if(getOutputFrequencies().containsKey(output)){
			getOutputFrequencies().put(output, getOutputFrequencies().get(output)+1);
		} else {
			getOutputFrequencies().put(output, 1);
			// to ensure that we have all possible extension of trace extensions (with arbitrary inputs) in the tree
			children.put(FastImmPair.of(output, input), new Node());
		}
		setOverallNrOutputs(getOverallNrOutputs() + 1);
		if(getOverallNrOutputs() >= ObservationTree.completenessThreshold){
			complete = true;
		}
	}

	public Node getOrCreateChild(FastImmPair<TimedOutput, TimedInput> step) {
		Node child = children.get(step);
		if(child == null){
			// this should not happen anymore actually
			child = new Node();
			children.put(step, child);
		}
		return child;
	}

	public FastImmPair<Node, Boolean> getChildFromTimedStep(FastImmPair<Output, TimedInput> step) {
		Boolean reset = null;
		for (FastImmPair<TimedOutput, TimedInput> child : children.keySet()) {
			if (child.left.getOutput().equals(step.left)) {
				reset = child.left.isReset();
				if (child.right.equals(step.right)) {
					return FastImmPair.of(children.get(child), reset);
				}
			}
		}

		for (TimedOutput timedOutput : outputFrequencies.keySet()) {
			if (timedOutput.getOutput().equals(step.left)) {
				reset = timedOutput.isReset();
				break;
			}
		}
		return FastImmPair.of(null, reset);
	}

	public Node getChild(FastImmPair<TimedOutput, TimedInput> resetTimedStep) {
		return  children.get(resetTimedStep);
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public Map<TimedOutput, Integer> getOutputFrequencies() {
		return outputFrequencies;
	}

	public void setOutputFrequencies(Map<TimedOutput, Integer> outputFrequencies) {
		this.outputFrequencies = outputFrequencies;
	}

	public Set<FastImmPair<TimedOutput,TimedInput>> getSuccSymbols() {
		return children.keySet();
	}

	public int getOverallNrOutputs() {
		return overallNrOutputs;
	}

	public void setOverallNrOutputs(int overallNrOutputs) {
		this.overallNrOutputs = overallNrOutputs;
	}
}

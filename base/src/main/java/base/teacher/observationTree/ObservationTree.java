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
import org.apache.commons.lang3.tuple.Triple;
import trace.*;
import utils.FastImmPair;

import java.util.*;

/**
 * Class storing the multiset of system traces. It implements both frequency and completeness 
 * queries through the functions named <code>outputFrequenciesAndCompleteness</code>.
 * 
 * @author Martin Tappler
 *
 */
public class ObservationTree {

	private Set<Input> inputs = null;
	private Node root = null;
	// TODO make nice
	public static int completenessThreshold = 200;
	public Set<Input> getInputs() { return inputs;}
	public ObservationTree(Set<Input> inputs){
		setRoot(new Node());
		this.inputs = inputs;
	}
	public boolean addObservationTrace(ResetTimedTrace trace){
		return addObservationTrace(getRoot(),trace);
	}

	private boolean addObservationTrace(Node current, ResetTimedTrace trace) {
		if (trace.isEmpty()) {
//			current.addOutput(trace.getLastOutput(), inputs);
			current.addOutput(trace.getLastOutput());
			return current.isComplete();
		}

		TimedOutput output = trace.getFirstOutput();
		TimedInput input = trace.getIthInput(1);
		current.addOutput(output, input);
		Node next = current.getOrCreateChild(FastImmPair.of(output, input));
		return addObservationTrace(next, trace.suffix(1));
	}

	public Triple<List<Boolean>, Map<TimedOutput, Integer>, Boolean> outputFrequenciesAndCompleteness(ResetTimedTrace trace,
			TimedSuffixTrace suffixTrace){
		TimedIncompleteTrace incTrace = new TimedIncompleteTrace(trace.convert(), suffixTrace);
		Triple<List<Boolean>, Map<TimedOutput, Integer>, Boolean> result = outputFrequenciesAndCompleteness(incTrace.getSteps());
		List<Boolean> suffixResets = new ArrayList<>();
		if (incTrace.length() > trace.length()) {
			suffixResets = result.getLeft().subList(trace.length(), incTrace.length());
		}
		return Triple.of(suffixResets, result.getMiddle(), result.getRight());
	}
	public Triple<List<Boolean>, Map<TimedOutput, Integer>, Boolean> outputFrequenciesAndCompleteness(List<FastImmPair<Output,TimedInput>> incompleteTrace){
		List<Boolean> resets = new ArrayList<>();
		Node current = getRoot();
		for (int i = 0; i < incompleteTrace.size(); i++) {
			FastImmPair<Output, TimedInput> outputTimedInputFastImmPair = incompleteTrace.get(i);
			boolean lastIsComplete = current.isComplete();
			FastImmPair<Node, Boolean> next = current.getChildFromTimedStep(outputTimedInputFastImmPair);
			if (next.left == null) {
				// 不存在对应的 timed output 在 output frequencies
				if (next.right == null && lastIsComplete) {
					for (; i < incompleteTrace.size(); i++) {
						resets.add(true);
					}
					lastIsComplete = true;
				} else {
					if (next.right != null) {
						double delayTime = next.right ? outputTimedInputFastImmPair.right.getClockVal()
								: outputTimedInputFastImmPair.right.getClockVal() - incompleteTrace.get(i-1).right.getClockVal();
						if (delayTime < 0) {
							for (; i < incompleteTrace.size(); i++) {
								resets.add(true);
							}
							lastIsComplete = true;
						} else {
							resets = new ArrayList<>();
							lastIsComplete = false;
						}
					} else {
						resets = new ArrayList<>();
						lastIsComplete = false;
					}
				}

				return Triple.of(resets, Collections.<TimedOutput, Integer>emptyMap(), lastIsComplete);
			}

			current = next.left;
			resets.add(next.right);
		}

		return Triple.of(resets, current.getOutputFrequencies(), current.isComplete());
	}
	// return all shortest traces leading to incomplete nodes
	public List<ResetTimedIncompleteTrace> findIncomplete() {
		List<ResetTimedIncompleteTrace> tracesToIncomplete = new ArrayList<>();
		findIncomplete(getRoot(), ResetTimedIncompleteTrace.empty(), tracesToIncomplete);
		System.out.println("Traces to incomplete: " + tracesToIncomplete.size());
		return tracesToIncomplete;
	}
	private void findIncomplete(Node current, ResetTimedIncompleteTrace currentTrace, List<ResetTimedIncompleteTrace> tracesToIncomplete) {
		if(current.getSuccSymbols().isEmpty() && !current.isComplete()){
			tracesToIncomplete.add(currentTrace);
		} else {
			for(FastImmPair<TimedOutput, TimedInput> succSymbol : current.getSuccSymbols()){
				ResetTimedIncompleteTrace nextTrace = (ResetTimedIncompleteTrace) currentTrace.append(succSymbol);
				findIncomplete(current.getChild(succSymbol), nextTrace, tracesToIncomplete);
			}
		}
	}

	public Node getRoot() {
		return root;
	}
	public void setRoot(Node root) {
		this.root = root;
	}
	
}

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
import base.learner.Answer;
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
	public static int completenessThreshold = 150;
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

	public Answer outputFrequenciesAndCompleteness(ResetTimedTrace trace,
												   TimedSuffixTrace suffixTrace){
		TimedIncompleteTrace incTrace = new TimedIncompleteTrace(trace.convert(), suffixTrace);
		Answer result = outputFrequenciesAndCompleteness(incTrace.getSteps());
		if (result.isValid()) {
			List<Boolean> suffixResets = new ArrayList<>();
			if (incTrace.length() > trace.length()) {
				suffixResets = result.getResets().subList(trace.length(), incTrace.length());
			}
			result.setResets(suffixResets);
		}
		return result;
	}
	public Answer outputFrequenciesAndCompleteness(List<FastImmPair<Output,TimedInput>> incompleteTrace){
		List<Boolean> resets = new ArrayList<>();
		resets.add(true);
		Node current = getRoot();
		for (int i = 0; i < incompleteTrace.size(); i++) {
			FastImmPair<Output, TimedInput> outputTimedInputFastImmPair = incompleteTrace.get(i);
			boolean lastIsComplete = current.isComplete();
			FastImmPair<Node, Boolean> next = current.getChildFromTimedStep(outputTimedInputFastImmPair);
			if (next.left == null) {
				// 存在对应的timed output在上一步的frequency，考虑逻辑时间合法性
				if (next.right != null) {
					double delayTime = next.right ? outputTimedInputFastImmPair.right.getClockVal()
							: outputTimedInputFastImmPair.right.getClockVal() - incompleteTrace.get(i-1).right.getClockVal();
					// 逻辑时间不合法，不需要再测试
					if (delayTime < 0) {
						return Answer.InvalidAnswer();
					}
					lastIsComplete = false;
				}

				return Answer.setValidAnswer(new ArrayList<>(), Collections.<TimedOutput, Integer>emptyMap(), lastIsComplete);
			}

			current = next.left;
			resets.add(next.right);
		}

		return Answer.setValidAnswer(resets, new HashMap<>(current.getOutputFrequencies()), current.isComplete());
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

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
package base.teacher.oracle.conv;

import automaton.Location;
import automaton.Output;
import automaton.PTA;
import automaton.Transition;
import base.Compatibility;
import base.teacher.observationTree.Node;
import base.teacher.observationTree.ObservationTree;
import base.teacher.oracle.ShortTraceToChaosGenerator;
import trace.ResetTimedIncompleteTrace;
import trace.ResetTimedTrace;
import trace.TimedInput;
import trace.TimedOutput;
import utils.FastImmPair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Equivalence query strategy that checks for conformance between the multiset of system traces 
 * collected so far (stored in an object of the class <code>ObservationTree</code>) and the current
 * hypothesis. It checks conformance based on a <code>CompatibilityChecker</code>, thus e.g. 
 * with Hoeffding tests implemented by <code>HoeffdingExtendedChecker</code>.
 * 
 * 
 * @author Martin Tappler
 *
 */
public class TreeBasedDistributionCexFinder {

	private Compatibility compChecker = null;
	// block cex once they have been added until we find no new cex, to avoid
	// adding the same useless cex over and over (it may happen that a non-spuriousness
	// cex is added, which is found to be useless during trimming of the observation table)
	// in case we do not find any cex, we clear the blocked cex and try searching again

	public TreeBasedDistributionCexFinder(Compatibility compChecker) {
		this.compChecker = compChecker;
	}

	public Optional<ResetTimedTrace> findCex(ObservationTree tree, PTA hypo) {
		Node root = tree.getRoot();
		ResetTimedIncompleteTrace currentTrace = ResetTimedIncompleteTrace.empty();
		ResetTimedTrace cex = findCex(hypo, root, currentTrace);
		if(cex != null){
			System.out.println("We found a cex with distribution information from the tree.");
			return Optional.of(cex);
		}
		return Optional.empty();
	}

	private ResetTimedTrace findCex(PTA hypo, Node root, ResetTimedIncompleteTrace currentTrace) {
		return findCexBFS(root, currentTrace, hypo);
	}

	private ResetTimedTrace findCexBFS(Node root, ResetTimedIncompleteTrace empty, PTA hypo) {
		LinkedList<FastImmPair<Node, ResetTimedIncompleteTrace>> schedule = new LinkedList<>();
		schedule.add(FastImmPair.of(root, empty));
		while(!schedule.isEmpty()){
			FastImmPair<Node, ResetTimedIncompleteTrace> current = schedule.removeFirst();
			Node currentNode = current.getLeft();
			ResetTimedIncompleteTrace currentTrace = current.getRight();
			if(!currentNode.isComplete())
				continue;
			Map<TimedOutput, Integer> treeFreqs = currentNode.getOutputFrequencies();
			FastImmPair<Map<TimedOutput, Integer>, Optional<ResetTimedTrace>> hypoDistAndLen =
					getDistributionAfterTrace(hypo, currentTrace);
			boolean spuriousCex = false;
			// skip empty trace && 
			// null signals chaos
			if (currentTrace.length() > 0 && hypoDistAndLen.getRight() != null) {
				// short cex, i.e. output possible in tree but not in Hyp.
				if (hypoDistAndLen.getRight().isPresent()) {
					 // transform to RowTrace
					return hypoDistAndLen.getRight().get();
				} else { 
					// now we need to check whether this trace is a long cex, i.e. 
					// if it shows incompatibility 
					if (!compChecker.compatible(hypoDistAndLen.getLeft(), treeFreqs)) {
						return new ResetTimedTrace(currentTrace, treeFreqs.keySet().iterator().next());
					}
				}
			}
			// if we did not find a spurious cex then we carry on with search
			for (FastImmPair<TimedOutput, TimedInput> succSymbol : currentNode.getSuccSymbols()) {
				Node nextNode = currentNode.getChild(succSymbol);
				ResetTimedIncompleteTrace nextTrace = (ResetTimedIncompleteTrace) currentTrace.append(succSymbol);
				schedule.add(FastImmPair.of(nextNode, nextTrace));
			}
		}
		
		return null;
	}

//	private boolean extractDistSuffix(ResetTimedIncompleteTrace cex, ObservationTable obsTable, MDP hypo) {
//		obsTable.ensureConsistencyWithTree();
//		for(int splitPos = 1; splitPos < cex.length(); splitPos++){
//			Triple<RowTrace,FastImmPair<Input,Output>, ColumnTrace> decompositionWithTrans = cex.splitAt(splitPos);
//			RowTrace prefix = decompositionWithTrans.getLeft();
//			FastImmPair<Input, Output> trans = decompositionWithTrans.getMiddle();
//			ColumnTrace suffix = decompositionWithTrans.getRight();
//			RowTrace prefixAndTrans = prefix.append(trans);
//
//			RowTrace prefixRep = obsTable.getRepresentative(prefix);
//			RowTrace prefixTransRep = obsTable.getRepresentative(prefixAndTrans);
//			if(prefixRep == null || prefixTransRep == null){
//				return false;
//			}
//			IncompleteTrace firstMerge = new IncompleteTrace(prefixRep.append(trans), suffix);
//			IncompleteTrace secondMerge = new IncompleteTrace(prefixAndTrans, suffix);
//			if(!compatibleTreeFreqs(firstMerge,secondMerge,obsTable)){
//				obsTable.updateE(suffix);
//				System.out.println("Added distinguishing suffix: " + suffix);
//				return true;
//			}
//		}
//		return false;
//	}
	

//	private boolean compatibleTreeFreqs(IncompleteTrace firstMerge, IncompleteTrace secondMerge,
//			ObservationTable obsTable) {
//		FastImmPair<Map<Output, Integer>, Boolean> treeFreqsFirst = treeFreqsForTrace(firstMerge,obsTable);
//		FastImmPair<Map<Output, Integer>, Boolean> treeFreqsSecond = treeFreqsForTrace(secondMerge,obsTable);
//		if(treeFreqsFirst.getRight() && treeFreqsSecond.getRight())
//			return compChecker.compatible(treeFreqsFirst.getLeft(), treeFreqsSecond.getLeft());
//		else
//			return true;
//	}

//	private static FastImmPair<Map<Output, Integer>, Boolean> treeFreqsForTrace(IncompleteTrace trace, ObservationTable obsTable) {
//		return obsTable.outputFrequenciesForTrace(trace);
//	}

	private static FastImmPair<Map<TimedOutput, Integer>, Optional<ResetTimedTrace>> getDistributionAfterTrace(PTA hypo,
			ResetTimedIncompleteTrace trace) {
		Location current = hypo.getInitial();
		Map<TimedOutput, Integer> outputDist = null;
		int i = 0;
		for (i = 0; i < trace.length(); i++) {
			FastImmPair<TimedOutput, TimedInput> currentStep = trace.get(i);
			assert current != null;
			Set<Transition> trans = current.getTransitions().get(currentStep.getRight().getInput()).stream().filter(transition -> transition.getGuard().enableAction(currentStep.right.getClockVal()))
					.collect(Collectors.toSet());
			outputDist = new HashMap<>();
			for (Transition t : trans) {
				outputDist.put(TimedOutput.create(t.isReset(), t.getTarget().getLabel().getSymbol()), t.getFrequency());
			}
		
			if (leadsToChaos(trans))
				return FastImmPair.of(outputDist, null);
			current = null;
			if (i + 1 < trace.length()) {
				TimedOutput nextOutput = trace.get(i + 1).getLeft();
				for (Transition t : trans) {
					if (t.isReset() == nextOutput.isReset() && t.getTarget().getLabel().equals(nextOutput.getOutput())) {
						current = t.getTarget();
						break;
					}
				}
				if (current == null) // index + 1 = length of prefix
					return FastImmPair.of(outputDist, Optional.<ResetTimedTrace>of(new ResetTimedTrace((ResetTimedIncompleteTrace) trace.prefix(i+1), nextOutput)));
			}
		}
		return FastImmPair.of(outputDist, Optional.empty());
	}

	private static boolean leadsToChaos(Set<Transition> trans) {
		return trans.size() == 1 && trans.iterator().next().getTarget().getLabel().equals(Output.chaos());
	}
}

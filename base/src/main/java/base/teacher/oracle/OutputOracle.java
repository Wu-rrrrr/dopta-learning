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
package base.teacher.oracle;

import automaton.Output;
import automaton.OutputDistribution;
import suls.SUL;
import trace.*;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * This class implements most of the functionality of refine queries including the 
 * <code>sampleSul</code> (here implemented by the function <code>perform</code>) function used by 
 * refine queries in the paper  "L*-Based Learning of Markov Decision Processes". It does not 
 * specify which test sequences should be resampled. 
 * 
 * @author Martin Tappler
 *
 */
public class OutputOracle {

	private Random random = null;
	private SUL sul = null;
	private int batchSize;
//	private Property property;
	
	public OutputOracle(Random random, SUL sul, int batchSize) {
		super();
		this.random = random;
		this.sul = sul;
		this.batchSize = batchSize;
	}

	public OutputDistribution outputDistributionQuery(ResetTimedTrace trace, TimedSuffixTrace suffix) {
		TimedIncompleteTrace logicalTimedTestSeq = new TimedIncompleteTrace(trace.convert(), suffix);
		OutputDistribution outputDistribution = sul.execute(logicalTimedTestSeq);
		List<Boolean> resets = outputDistribution.getResets();
		if (!resets.isEmpty()) {
			for (int i = 0; i < trace.length() - 1; i++) {
				resets.remove(0);
			}
		}

//		if (outputDistribution.getDistribution() == null) {
//			System.out.println(trace.toString() + suffix);
//		}
		return outputDistribution;
	}
	public List<ResetTimedTrace> performQueries(List<TimedIncompleteTrace> traces) {
		List<ResetTimedTrace> queryResults = new ArrayList<>();
		List<TimedIncompleteTrace> preferred = null;
		List<TimedIncompleteTrace> other = null;

//		if(property != null){
//			preferred = traces.stream().filter(t -> property.evaluate(t)).collect(Collectors.toList());
////			other = preferred.isEmpty() ? traces : new ArrayList<>();
//			other = traces.stream().filter(t -> !property.evaluate(t)).collect(Collectors.toList());
//		} else {
//			preferred = traces;
//			other = new ArrayList<>();
//		}

		preferred = traces;
		other = new ArrayList<>();

		for(int nrQueries = 0; nrQueries < batchSize; nrQueries++){
			Collections.shuffle(preferred,random); // otherwise we will try to perform the same queries all the time
			Collections.shuffle(other,random);
			
			TimedIncompleteTrace chosenTrace = choose(preferred,other);
			ResetTimedTrace singleResult = perform(chosenTrace,preferred,other);
			queryResults.add(singleResult);
		}
		return queryResults;
	}
	public ResetTimedTrace perform(TimedIncompleteTrace chosenTrace,  List<TimedIncompleteTrace> preferredOtherTraces,
			List<TimedIncompleteTrace> otherTraces) {
		double currentLogicalTime = 0.0;
		Output sulOutput = Output.create(sul.reset());
		ResetTimedTrace resultTrace = ResetTimedTrace.empty(sulOutput);
		for(int i = 0; i < chosenTrace.length(); i++){
			FastImmPair<Output, TimedInput> currentStep = chosenTrace.getSteps().get(i);
			Output expectedOutput = currentStep.getLeft();
			if(!sulOutput.equals(expectedOutput)){
				TimedIncompleteTrace newChosenTrace = findAnotherTrace(resultTrace,sulOutput, preferredOtherTraces,
						otherTraces);
				if(newChosenTrace == null)
					return resultTrace;
				else
					chosenTrace = newChosenTrace;
			}
			FastImmPair<Boolean, String> output = sul.execute(currentStep.getRight().getInput().getSymbol(),
					currentStep.right.getClockVal()-currentLogicalTime);
			if (output == null)
				return resultTrace;
			sulOutput = Output.create(output.right);
			currentLogicalTime = output.left ? 0.0 : currentStep.right.getClockVal();
			resultTrace = resultTrace.append(FastImmPair.of(currentStep.getRight(), TimedOutput.create(output.left, output.right)));
		}
		return resultTrace;
	}
	private TimedIncompleteTrace findAnotherTrace(ResetTimedTrace prefix, Output outputAfterPrefix,
			List<TimedIncompleteTrace> preferredOtherTraces, List<TimedIncompleteTrace> otherTraces) {
		TimedIncompleteTrace foundTrace = findTraceWithPrefix(prefix, outputAfterPrefix,preferredOtherTraces);
		if(foundTrace == null)
			return findTraceWithPrefix(prefix, outputAfterPrefix, otherTraces);
		else
			return foundTrace;
	}
	private TimedIncompleteTrace findTraceWithPrefix(ResetTimedTrace prefix, Output outputAfterPrefix,
			List<TimedIncompleteTrace> otherTraces) {
		for(TimedIncompleteTrace trace : otherTraces){
			if(prefix.length() + 1 > trace.length())
				continue;
			boolean isPrefix = true;
			Output output = prefix.getFirstOutput().getOutput();
			for (int i = 0; i < prefix.length()-1; i++) {
				FastImmPair<TimedInput, TimedOutput> step = prefix.get(i);
				FastImmPair<Output, TimedInput> pair = trace.getSteps().get(i);
				if (!output.equals(pair.left) || !step.left.equals(pair.right)) {
					isPrefix = false;
					break;
				}
				output = step.right.getOutput();
			}
			if(!isPrefix)
				continue;
			if(trace.get(prefix.length()).getLeft().equals(outputAfterPrefix))
				return trace;
		}
		return null;
	}
	public ResetTimedTrace perform(ResetTimedTrace chosenTrace) {
		TimedOutput sulOutput = TimedOutput.createInit(sul.reset());
		ResetTimedTrace resultTrace = ResetTimedTrace.empty(sulOutput.getOutput());
		TimedOutput expectedOutput = chosenTrace.getFirstOutput();
		for(int i = 0; i < chosenTrace.length(); i++){
			if(!sulOutput.equals(expectedOutput)){
				return resultTrace;
			}
			FastImmPair<TimedInput, TimedOutput> currentStep = chosenTrace.get(i);
			expectedOutput = currentStep.right;
			FastImmPair<Boolean, String> output = sul.execute(currentStep.left.getInput().getSymbol(), currentStep.left.getClockVal());
			if (output == null)
				return resultTrace;
			sulOutput = TimedOutput.create(output);

			resultTrace = (ResetTimedTrace) resultTrace.append(FastImmPair.of(currentStep.left, sulOutput));
		}
		return resultTrace;
	}
	private TimedIncompleteTrace choose(List<TimedIncompleteTrace> preferred, List<TimedIncompleteTrace> other) {
		if(!preferred.isEmpty())
			return preferred.get(random.nextInt(preferred.size()));
		else
			return other.get(random.nextInt(other.size()));
	}
	public Output initOutput() {
		return Output.create(sul.reset());
	}

//	public void enablePreferPropertySat(Property property) {
//		this.property = property;
//	}

}

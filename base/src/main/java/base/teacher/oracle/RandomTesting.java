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

import automaton.*;
import base.teacher.observationTree.ObservationTree;
import suls.SUL;
import trace.ResetTimedTrace;
import trace.TimedInput;
import trace.TimedOutput;
import utils.FastImmPair;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Base class for all testing-based implementations of equivalence queries. 
 * Basically, this class performs online-testing of the SUL in parallel with the current hypotheis 
 * to detect structural inequivalence, i.e. find traces that are observable on the SUL, but not on 
 * the hypothesis. 
 * 
 * The choice of inputs is delegated to subclasses.
 * 
 * @author Martin Tappler
 *
 */
public class RandomTesting {
	protected ObservationTree tree;
	protected OutputOracle outputOracle;
	protected SUL sul;

	protected Random random;
	protected double stopProb;
	protected int nrTest;

	protected List<Input> inputList;
	protected int bound;
	protected double regionNormalization;

	private boolean stopAtCex = true;
	private List<ResetTimedTrace> sampledTraces;

	private int uninterestingSamplesThreshold = 100;
	private Output uninterestingOutput = Output.sink();

	private List<ResetTimedTrace> candidates;

	/**
	 * Constructor 
	 *
	 * @param outputOracle interface trace executor on SUL
	 * @param random random number generator
	 * @param stopProb P_stop
	 * @param inputs Sigma^I
	 * @param regionNormalization theta
	 */
	public RandomTesting(OutputOracle outputOracle, SUL sul, ObservationTree tree, Random random,
						 double stopProb, Set<Input> inputs, int bound, double regionNormalization) {
		super();
		this.tree = tree;
		this.outputOracle = outputOracle;
		this.sul = sul;
		this.random = random;
		this.stopProb = stopProb;
		this.inputList = new ArrayList<>(inputs);
		this.bound = bound;
		this.regionNormalization = regionNormalization;
	}

	public List<ResetTimedTrace> getSampledTraces() {
		return sampledTraces;
	}

	public Optional<ResetTimedTrace> sampleForFindCex(PTA hypo, int nrTest) {
		this.nrTest = nrTest;
		sampledTraces = new ArrayList<>();
		Optional<ResetTimedTrace> discriminating = searchForDiscriminatingTest(hypo);
		discriminating.ifPresent(System.out::println);
		return discriminating;
	}

	private Optional<ResetTimedTrace> searchForDiscriminatingTest(PTA hypo) {
		Optional<ResetTimedTrace> discriminatingTest = Optional.empty();
		int partition = nrTest / 2;

		candidates = new ArrayList<>();
		candidates.add(ResetTimedTrace.empty(Output.create(sul.reset())));
		// random walking
		for (int tries = 0; tries < nrTest; tries++) {
			FastImmPair<ResetTimedTrace, Boolean> potentiallyDiscriminating = findDiscriminatingTestSingle(hypo, tries < partition);
			if (!potentiallyDiscriminating.left.lastOutput().equals(uninterestingOutput)) {
				candidates.add(potentiallyDiscriminating.left);
			}
			tree.addObservationTrace(potentiallyDiscriminating.getLeft());
			sampledTraces.add(potentiallyDiscriminating.left);
			boolean isDiscriminating = potentiallyDiscriminating.getRight();
			if (isDiscriminating){
				System.out.println("We have a discriminating test after " + (tries +1) + " tries: ");
				discriminatingTest = Optional.of(potentiallyDiscriminating.getLeft());
				return discriminatingTest;
			}
		}
		return discriminatingTest;
	}

	private FastImmPair<ResetTimedTrace, Boolean> findDiscriminatingTestSingle(PTA hypo, boolean randomSample) {
		ResetTimedTrace selectedTrace = candidates.get(random.nextInt(candidates.size()));
		int minSampleSize = randomSample ? 1 : selectedTrace.length();

		Location currentHypState = hypo.getInitial();
		ResetTimedTrace currenTrace = ResetTimedTrace.empty(Output.create(sul.reset()));
		double currentLogicalTime = 0.0;
		int tries = 1;
		do {
			TimedInput delayTimedInput;
			if (randomSample || currenTrace.length() >= selectedTrace.length()) {
				delayTimedInput = chooseRandomInput(currentLogicalTime);
			} else {
				TimedInput selectedLogicalTimedInput = selectedTrace.getIthInput(currenTrace.length());
				delayTimedInput = TimedInput.create(selectedLogicalTimedInput.getInput().getSymbol(), selectedLogicalTimedInput.getClockVal() - currentLogicalTime);
			}
			FastImmPair<Boolean, String> sulOutput = sul.execute(delayTimedInput.getInput().getSymbol(), delayTimedInput.getClockVal());
			if (sulOutput == null) {
				throw new Error(String.format("sul output error, trace: %s,delay timed action: %s",currenTrace,delayTimedInput));
			}

			TimedOutput nextSulOutput = TimedOutput.create(sulOutput);
			TimedInput logicalTimedInput = delayTimedInput.convertLogicalTime(currentLogicalTime);
			if (logicalTimedInput.getClockVal() > bound) {
				throw new Error("delay time selected incorrect");
			}
			currenTrace = currenTrace.append(FastImmPair.of(logicalTimedInput, nextSulOutput));
			Optional<Location> nextHypState = executeHyp(currentHypState, logicalTimedInput, nextSulOutput);
			if (nextHypState.isEmpty()) {
				if (leadsToChaos(currentHypState, logicalTimedInput))
					if(!stopAtCex)
						return FastImmPair.of(extendedRandom(currenTrace), false);
					else
						return FastImmPair.of(currenTrace, false);
				else{
//						while (executeHyp(currentHypState, logicalTimedInput, nextSulOutput).isEmpty()) {
//							executeTraceOnSUL(currenTrace);
//							double nextRegion = logicalTimedInput.getClockVal() -
//						}

						return FastImmPair.of(currenTrace, true);
					}
			}
			// inconsistent with selected trace
			if (!randomSample && (currenTrace.length() >= selectedTrace.length() || !nextSulOutput.equals(selectedTrace.get(currenTrace.length()-2).right))) {
				randomSample = true;
				minSampleSize = currenTrace.length();
			}

			currentLogicalTime = nextSulOutput.isReset() ? 0.0 : logicalTimedInput.getClockVal();
			currentHypState = nextHypState.get();
			// 设置参数控制不感兴趣的采样
			if (currentHypState.getLabel().equals(uninterestingOutput) && tries < uninterestingSamplesThreshold) {
				tree.addObservationTrace(currenTrace);
				tries++;
				currenTrace = ResetTimedTrace.empty(Output.create(sul.reset()));
				currentHypState = hypo.getInitial();
				currentLogicalTime = 0.0;
			}
		} while (!currentHypState.getLabel().equals(uninterestingOutput) && (random.nextDouble() < stopProb || currenTrace.length() <= minSampleSize));
		return FastImmPair.of(currenTrace, false);
	}

	private void executeTraceOnSUL(ResetTimedTrace trace) {
		boolean flag = true;
		while (flag) {
			double clockVal = 0;
			sul.reset();
			if (trace.length() == 1) {
				return;
			}
			List<FastImmPair<TimedInput, TimedOutput>> steps = trace.getTrace();
			int length = steps.size();
			for (FastImmPair<TimedInput, TimedOutput> step :
					steps) {
				TimedInput delayTimedInput = new TimedInput(step.left.getInput(), step.left.getClockVal() - clockVal);
				FastImmPair<Boolean, String> sulOutput = sul.execute(delayTimedInput.getInput().getSymbol(), delayTimedInput.getClockVal());
				if (step.right.isReset() == sulOutput.getLeft() && step.right.getOutput().getSymbol().equals(sulOutput.right)) {
					break;
				}
				if (sulOutput.left) {
					clockVal = 0;
				} else {
					clockVal = step.left.getClockVal();
				}
				length--;
			}
			flag = length > 0;
		}
	}

	private ResetTimedTrace extendedRandom(ResetTimedTrace currenTrace) {
		double currentLogicalTime = currenTrace.getLastOutput().isReset() ? 0.0 : currenTrace.getIthInput(currenTrace.length()-1).getClockVal();
		do {
			TimedInput delayTimedInput = chooseRandomInput(currentLogicalTime);
			FastImmPair<Boolean, String> output = sul.execute(delayTimedInput.getInput().getSymbol(), delayTimedInput.getClockVal());
			if (output == null)
				return currenTrace;
			TimedOutput nextSulOutput = TimedOutput.create(output);
			TimedInput logicalTimedInput = delayTimedInput.convertLogicalTime(currentLogicalTime);
			currenTrace = currenTrace.append(FastImmPair.of(logicalTimedInput, nextSulOutput));
			currentLogicalTime = nextSulOutput.isReset() ? 0.0 : logicalTimedInput.getClockVal();
		} while (random.nextDouble() >= stopProb);
		return currenTrace;
	}

	protected boolean leadsToChaos(Location currentHypState, TimedInput timedInput) {
		if (currentHypState.getTransitions().get(timedInput.getInput()) == null) {
			return false;
		}
		for (Transition t : currentHypState.getTransitions().get(timedInput.getInput())) {
			if (t.getGuard().enableAction(timedInput.getClockVal()) && t.getTarget().getLabel().equals(Output.chaos())) {
				return true;
			}
		}
		return false;
	}

	protected boolean leadsToSink(Location currentHypState, TimedInput timedInput) {
		for (Transition t : currentHypState.getTransitions().get(timedInput.getInput())) {
			if (t.getGuard().enableAction(timedInput.getClockVal()) && t.getTarget().getLabel().equals(Output.chaos()))
				return true;
		}
		return false;
	}

	protected Optional<Location> executeHyp(Location currentHypState, TimedInput timedInput, TimedOutput nextSulOutput) {
		Set<Transition> transForInput = currentHypState.getTransitions().get(timedInput.getInput());
		if (transForInput == null) {
			System.out.println(currentHypState.getTransitions());
		} else {
			for (Transition t : transForInput) {
				if (t.getGuard().enableAction(timedInput.getClockVal())
						&& t.isReset() == nextSulOutput.isReset()
						&& t.getTarget().getLabel().equals(nextSulOutput.getOutput()))
					return Optional.of(t.getTarget());
			}
		}
		return Optional.empty();
	}

	protected TimedInput chooseRandomInput(double currentLogicalTime) {
		Input input = inputList.get(random.nextInt(inputList.size()));
		int base = (int) Math.ceil(currentLogicalTime * 2);
		double randomLogicalTime = base + random.nextInt(2 * bound - base);
		if (randomLogicalTime % 2 == 1) {
			randomLogicalTime = Math.floor(randomLogicalTime / 2) + regionNormalization;
		} else {
			randomLogicalTime /= 2;
		}
		return TimedInput.create(input.getSymbol(), randomLogicalTime - currentLogicalTime);
	}

	public boolean isStopAtCex() {
		return stopAtCex;
	}

	public void setStopAtCex(boolean stopAtCex) {
		this.stopAtCex = stopAtCex;
	}
}

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
		for (int tries = 0; tries < nrTest; tries++) {
			FastImmPair<ResetTimedTrace, Boolean> potentiallyDiscriminating = findDiscriminatingTestSingle(hypo);
			tree.addObservationTrace(potentiallyDiscriminating.getLeft());
			sampledTraces.add(potentiallyDiscriminating.left);
			boolean isDiscriminating = potentiallyDiscriminating.getRight();
			if (isDiscriminating && discriminatingTest.isEmpty()){
				System.out.println("We have a discriminating test after " + (tries +1) + " tries: ");
				discriminatingTest = Optional.of(potentiallyDiscriminating.getLeft());
				if(stopAtCex)
					break;
			}
		}
		return discriminatingTest;
	}

	private FastImmPair<ResetTimedTrace, Boolean> findDiscriminatingTestSingle(PTA hypo) {
		Location currentHypState = hypo.getInitial();
		TimedOutput sulOutput = TimedOutput.createInit(sul.reset());
		ResetTimedTrace currenTrace = ResetTimedTrace.empty(sulOutput.getOutput());
		double currentLogicalTime = 0.0;
		// Set<OutputSymbol> hypOutput =
		// currentHypState.stream().map(State::getLabel).collect(Collectors.toSet());
//		List<Input> inputsExecuted = new ArrayList<>();
		do {
			TimedInput delayTimedInput = chooseRandomInput(currentLogicalTime);
			FastImmPair<Boolean, String> output = sul.execute(delayTimedInput.getInput().getSymbol(), delayTimedInput.getClockVal());
			if (output == null)
				return FastImmPair.of(currenTrace, false);
			TimedOutput nextSulOutput = TimedOutput.create(output);

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
//					System.out.println("Hyp outputs: " + currentHypState.getTransitions().get(input).stream()
//							.map(t -> t.getTarget().getLabel().toString()).collect(Collectors.joining(",")));
					if(!stopAtCex)
						return FastImmPair.of(extendedRandom(currenTrace), true);
					else
						return FastImmPair.of(currenTrace, true);
				}
			}

			currentLogicalTime = nextSulOutput.isReset() ? 0.0 : logicalTimedInput.getClockVal();
			currentHypState = nextHypState.get();
		} while (!currentHypState.getLabel().equals(Output.sink()) && random.nextDouble() >= stopProb);
		return FastImmPair.of(currenTrace, false);
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

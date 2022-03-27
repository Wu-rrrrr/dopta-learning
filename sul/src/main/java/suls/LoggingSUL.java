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
package suls;

import automaton.Input;
import automaton.Output;
import trace.ResetTimedTrace;
import trace.TimedInput;
import trace.TimedOutput;
import trace.TimedTrace;
import trace.base.Trace;
import utils.FastImmPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Wrapper class for SULs that stores traces that are executed on the SUL. A trace starts when the 
 * wrapped SUL is reset and ends before the next reset. 
 * 
 * @author Martin Tappler
 *
 */
public class LoggingSUL implements SUL {

	private List<ResetTimedTrace> traces = new ArrayList<>();
	private ResetTimedTrace currentTrace = null;
	private SUL wrappedSUL = null;
	
	public LoggingSUL(SUL wrappedSUL) {
		super();
		this.wrappedSUL = wrappedSUL;
	}
	private boolean log = true;
	
	public void disableLogging(){
		if(log){
			if(currentTrace != null){
				traces.add(currentTrace);
			}
			currentTrace = null;
		}
		log = false;
	}
	public void enableLogging(){
		log = true;
	}
	@Override
	public void init(long seed) throws Exception {
		wrappedSUL.init(seed);
	}

	@Override
	public String reset() {
		String initialOutput = wrappedSUL.reset();
		if(log){
			if(currentTrace != null){
				traces.add(currentTrace);
			}
			currentTrace = ResetTimedTrace.empty(new Output(initialOutput));
		}
		return initialOutput;
	}

	@Override
	public FastImmPair<Boolean, String> execute(String input, double clockVal) {
		FastImmPair<Boolean, String> timedOutput = wrappedSUL.execute(input, clockVal);
		if(log){
			if(currentTrace != null && timedOutput != null){
				currentTrace = (ResetTimedTrace) currentTrace.append(FastImmPair.of(TimedInput.create(input, clockVal), TimedOutput.create(timedOutput)));
			}
		}
		return timedOutput;
	}

	@Override
	public Set<Input> getInputs() {
		return wrappedSUL.getInputs();
	}
	public List<ResetTimedTrace> getTraces() {
		if(log && currentTrace != null){
			traces.add(currentTrace);
		}
		return traces;
	}
	
	public void clearTraces(){
		traces.clear();
	}

}

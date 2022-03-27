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
import trace.TimedInput;
import utils.FastImmPair;

import java.util.Set;

/**
 * Wrapper class for SULs to track how many steps (inputs) are performed. 
 * 
 * @author Martin Tappler
 *
 */
public class StepTrackingSUL implements SUL {

	private int steps = 0;
	private SUL wrappedSUL = null;

	public StepTrackingSUL(SUL wrappedSUL) {
		super();
		this.wrappedSUL = wrappedSUL;
	}
	private boolean track = true;
	
	public void disableTracking(){
		track = false;
	}
	public void enableTracking(){
		track = true;
	}
	@Override
	public void init(long seed) throws Exception {
		wrappedSUL.init(seed);
	}

	@Override
	public String reset() {
		return wrappedSUL.reset();
	}

	@Override
	public FastImmPair<Boolean, String> execute(String input, double clockVal) {
		if(track)
			steps++;
		return wrappedSUL.execute(input, clockVal);
	}

	public Set<Input> getInputs() {
		return wrappedSUL.getInputs();
	}
	public int getSteps() {
		return steps;
	}

}

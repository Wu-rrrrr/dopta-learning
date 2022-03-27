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
import utils.FastImmPair;

import java.util.Set;

/**
 * Main interface to a system under learning (SUL).
 * 
 * @author Martin Tappler
 *
 */
public interface SUL {

	/**
	 * Initialise the system under learning prior to using it.
	 * 
	 * @param seed
	 *            seed for random number generator controlling probabilistic
	 *            behaviour
	 * 
	 * @throws Exception
	 *             generic exception that could be thrown by implementing
	 *             classes (e.g. IO errors, if a SUL is backed by a file that
	 *             needs to be read)
	 */
	public abstract void init(long seed) throws Exception;

	/**
	 * Reset the SUL to its initial state.
	 * 
	 * @return the initial output of the SUL.
	 */
	public abstract String reset();

	/**
	 * Execute a single input, changing the SUL state.
	 * 
	 * @param input
	 *            the input to be executed.
	 * @return the output produced by the new SUL state
	 */
	public abstract FastImmPair<Boolean, String> execute(String input, double clockValue);

	/**
	 * Accessor for the alphabet comprising inputs and outputs of the SUL.
	 * 
	 * @return alphabet of the SUL
	 */
	public abstract Set<Input> getInputs();

}

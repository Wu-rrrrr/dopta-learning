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
package evaluation.config;

import automaton.Output;
import automaton.PTA;
import base.teacher.oracle.ShortTraceToChaosGenerator;
import evaluation.learner_proxy.LearnerInstance;
import trace.ResetTimedTrace;
import utils.FastImmPair;

import java.util.List;

public interface LearnerConfig {
	LearnerInstance instantiate() throws Exception;
	String description();
	List<FastImmPair<String, String>> parameters();
	String fileNameBase();
	void setSeed(long seed);
	default void setSampleTraces(List<ResetTimedTrace> sample){
	}
	int getBound();
	public static void removeChaosIfUnreachable(PTA learnedModel) {
		boolean isChaosReachable = !ShortTraceToChaosGenerator
				.findTracesToChaos(learnedModel, 0.1).isEmpty();
		if(!isChaosReachable){
			learnedModel.getLocations().removeIf(s -> s.getLabel().equals(Output.chaos()));
		}
	}
}

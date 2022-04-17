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
package evaluation.learner_proxy;

import java.util.List;
import automaton.PTA;
import base.learner.Learner;
import base.learner.LearningSetting;
import lombok.AllArgsConstructor;
import trace.ResetTimedTrace;

@AllArgsConstructor
public abstract class LearnerInstance {
	protected LearningSetting setting;
	protected Learner learner;

	public PTA learn() throws Exception {
		learner.learn(setting);
		return setting.getHypothesis();
	}
	public abstract List<ResetTimedTrace> loggedSampleTraces();
	public abstract long getNrSteps();
	public abstract long getNrTests();
	public int getNrRounds() {
		return setting.getRounds();
	}
	public int getNrEq() {return setting.getNrEq();}
}

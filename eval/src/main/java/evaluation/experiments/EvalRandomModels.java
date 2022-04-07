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
package evaluation.experiments;

import evaluation.Evaluator;
import evaluation.config.experiment_configs.RandomModelConfig;
import base.teacher.oracle.EqMode;
import importer.json.JsonSUL;

public class EvalRandomModels {
	public static void main(String args[]) throws Exception{
		long seed = System.currentTimeMillis();
		String trueSulFile = "eval/src/main/resources/randomModels";
		String modelName = "4_4_3_10";

		Evaluator evaluator = new Evaluator(seed);
		for (int i = 1; i <= 10; i++) {
			JsonSUL trueSUL = JsonSUL.getPtaFromJsonFile(String.format("%s/%s-%d.json", trueSulFile, modelName, i));
			trueSUL.init(seed);

			evaluator.setSul(trueSUL, modelName, i);
			evaluator.addConfig(RandomModelConfig.observationTable(seed, trueSUL, EqMode.PAC));
			evaluator.addConfig(RandomModelConfig.classificationTree(seed, trueSUL, EqMode.PAC));

			evaluator.evalTesting(20000, "results_only_testing/randomModels");
		}


//		Evaluator evaluator = new Evaluator(trueSUL, modelName+"-"+index, seed);
//		evaluator.addConfig(RandomModelConfig.observationTable(seed, trueSUL, EqMode.PAC));
//		evaluator.addConfig(RandomModelConfig.classificationTree(seed, trueSUL, EqMode.PAC));
//
//		evaluator.evalTesting(20000, "results_only_testing/randomModels");
	}
}

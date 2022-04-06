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
package evaluation;

import evaluation.config.LearnerConfig;
import evaluation.leaner_proxy.LearnerInstance;
import evaluation.prism.PrismInterface;
import evaluation.prism.PrismModelExporter;
import automaton.PTA;
import importer.json.JsonSUL;
import suls.LoggingSUL;
import suls.SUL;
import trace.ResetTimedTrace;
import util.export.DotExporter;
import utils.FastImmPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Evaluator {

	private LoggingSUL sul = null;
	private PTA trueModel;
	private List<LearnerConfig> learnConfigs = new ArrayList<>();
	private String sulName;
	private boolean outputTrueDotModel = false;
	private SampleTest sampleTesting;
	
	public Evaluator(JsonSUL wrappedSUl, String trueSulName, long seed) throws Exception{
		sul = new LoggingSUL(wrappedSUl);
		sul.init(seed);
		sulName = trueSulName;
		trueModel = wrappedSUl.getTargetModel();
		sampleTesting = new SampleTest(trueModel);
	}
	
	public void addConfig(LearnerConfig newConfig){
		learnConfigs.add(newConfig);
	}
	
	public static String loadPrismFileName() throws FileNotFoundException, IOException{
		Properties props = new Properties();
		props.load(new FileInputStream("eval.properties"));
		return props.getProperty("prism-location");
	}

	public void evalTesting(int samplingNum, String outputPath) throws Exception{
		System.out.printf("Computing passing ratio of %d sampled traces:\n", samplingNum);
		if(outputTrueDotModel)
			outputTrueModelAsDot(outputPath);
		new File(outputPath).mkdirs();
		List<ResetTimedTrace> allLoggedTraces = new ArrayList<>();
		EvalMeasurement measurement = new EvalMeasurement("testing accuracy", sulName);
		for(LearnerConfig config : learnConfigs){
			config.setSampleTraces(allLoggedTraces);
			LearnerInstance learner = config.instantiate();
			System.out.println("Evaluating " + config.description());
			long startOflearning = System.currentTimeMillis();
			PTA learnedPTA = learner.learn();
			long endOfLearning = System.currentTimeMillis();
			System.out.println("Finished learning with " + config.description());
			sampleTesting.evaluatePassRatio(learnedPTA, samplingNum, config.getBound());
			EvalResult result = new EvalResult(config.description(), config.parameters());
			result.setLearningTime(endOfLearning-startOflearning);
			result.setModelSize(learnedPTA.getLocations().size());
			result.setNrSteps(learner.getNrSteps());
			result.setNrTests(learner.getNrTests());
			result.setNrRounds(learner.getNrRounds());
			result.setPassRatio(sampleTesting.getPassRatio());
			result.setKlDivergence(sampleTesting.getKlDivergence());

			measurement.addResult(result);
			allLoggedTraces.addAll(learner.loggedSampleTraces());
		}
		measurement.persist(outputPath + "/" + String.format("%s_results.log", sulName));
		System.out.println("Experiment finished.");
	}

	// use all if properties is empty
	public void evalPropertyProbabilitiesAndTesting(int samplingNum, String pathToPrism,
			String outputPath, String propertyFileName, String truePrismFile, int... properties) throws Exception{
		System.out.println("Computing probabilities of properties:" );
		if(outputTrueDotModel)
			outputTrueModelAsDot(outputPath);
		new File(outputPath).mkdirs();
		List<ResetTimedTrace> allLoggedTraces = new ArrayList<>();
		EvalMeasurement measurement = new EvalMeasurement("property-probabilities", sulName);
		computeOptimalProbalities(measurement,pathToPrism,truePrismFile, propertyFileName,properties);
		for(LearnerConfig config : learnConfigs){
			config.setSampleTraces(allLoggedTraces);
			LearnerInstance learner = config.instantiate();
			System.out.println("Evaluating " + config.description());
			long startOflearning = System.currentTimeMillis();
			PTA learnedPTA = learner.learn();
			long endOfLearning = System.currentTimeMillis();
			System.out.println("Finished learning with " + config.description());
			sampleTesting.evaluatePassRatio(learnedPTA, samplingNum, config.getBound());
			EvalResult result = computePropertyProbabilies(config,pathToPrism,outputPath,
					config.fileNameBase(),learnedPTA, propertyFileName, properties);
			result.setLearningTime(endOfLearning-startOflearning);
			result.setModelSize(learnedPTA.getLocations().size());
			result.setNrSteps(learner.getNrSteps());
			result.setNrTests(learner.getNrTests());
			result.setNrRounds(learner.getNrRounds());
			result.setPassRatio(sampleTesting.getPassRatio());
			result.setKlDivergence(sampleTesting.getKlDivergence());
			
			measurement.addResult(result);
			allLoggedTraces.addAll(learner.loggedSampleTraces());
		}
		measurement.persist(outputPath + "/" + String.format("%s_results.log", sulName));
		System.out.println("Experiment finished.");
	}

	private void outputTrueModelAsDot(String outputPath) throws IOException {
		exportDotFile(outputPath, String.format("%s_true_model", sulName), trueModel);
	}

	private void computeOptimalProbalities(EvalMeasurement measurement, 
			String pathToPrism, String truePrismFile, String propertyFileName, int[] properties) throws IOException {
		PrismInterface prismInterface = new PrismInterface(pathToPrism,truePrismFile, propertyFileName);
		List<FastImmPair<String, Double>> results = null;
		if(properties.length == 0)
			results = prismInterface.computeAllProbabilities();
		else{
			List<Integer> propertyList = new ArrayList<>();
			for(int p : properties){
				propertyList.add(p);
			}
			results = prismInterface.computeProbabilities(propertyList);
		}
		System.out.println("True probabilities");
		for(FastImmPair<String, Double> r : results){
			if(r.getRight() > -1)
				System.out.println(r.getLeft() + " = " + r.getRight());
			else 
				System.out.println(r.getLeft() + " = " + r.getRight());
			if(r.getRight() > -1)
				measurement.addAdditionalInformation(FastImmPair.of(r.getLeft(), r.getRight().toString()));
			else
				measurement.addAdditionalInformation(FastImmPair.of(r.getLeft(), "unknown value"));
		}
	}

	private EvalResult computePropertyProbabilies(LearnerConfig config, String pathToPrism,String outputPathPrismFiles, 
			String fileNameBase, PTA learnedPTA, String propertyFileName, int[] properties) throws Exception {
		EvalResult result = new EvalResult(config.description(), config.parameters());
		String fullFileName = exportPrismFile(outputPathPrismFiles, fileNameBase, learnedPTA);
		exportDotFile(outputPathPrismFiles, fileNameBase, learnedPTA.reverseComplement());
		
		PrismInterface prismInterface = new PrismInterface(pathToPrism, fullFileName, propertyFileName);
		List<FastImmPair<String, Double>> results = null;
		if(properties.length == 0)
			results = prismInterface.computeAllProbabilities();
		else{
			List<Integer> propertyList = new ArrayList<>();
			for(int p : properties){
				propertyList.add(p);
			}
			results = prismInterface.computeProbabilities(propertyList);
		}
		for(FastImmPair<String, Double> r : results){
			System.out.println(r.getLeft() + " = " + r.getRight());
			result.addResult(new SingleResult(r.getLeft(), r.getRight().toString()));
		}
		
		return result;
	}

	private void exportDotFile(String outputPathPrismFiles, String fileNameBase, PTA learnedMDP) throws IOException {
		String fullDotFileName = outputPathPrismFiles + "/" + fileNameBase + ".dot";
		DotExporter dotExporter = new DotExporter();
		dotExporter.writeToFile(learnedMDP, fullDotFileName);
	}

	private String exportPrismFile(String outputPathPrismFiles, String fileNameBase, PTA learnedPTA) throws Exception {
		String fullFileName = outputPathPrismFiles + "/" + fileNameBase + ".prism";
		PrismModelExporter.convert(learnedPTA, fullFileName, fileNameBase);
		return fullFileName;
	}

	public boolean isOutputTrueDotModel() {
		return outputTrueDotModel;
	}

	public void setOutputTrueDotModel(boolean outputTrueDotModel) {
		this.outputTrueDotModel = outputTrueDotModel;
	}
	
}

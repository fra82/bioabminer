package es.imim.ibi.bioab.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Train and store CRF tagger models
 * See: http://www.chokkan.org/software/crfsuite/manual.html for parameter / option settings
 * 
 * @author Francesco ROnzano
 *
 */
public class TrainWekaClassifier {

	private static final String WEKA_CLASSIFIER_FULLY_QUALIFIED_CLASS = "weka.classifiers.trees.RandomForest";
	
	
	public static void main(String[] args) {
		
		String inputTrainingFile = "/home/ronzano/Desktop/Hackathon_PLN/ARFF_FILES/" + "abbrv_v_BARR17_train_and_test_sentScop_true_FILTERED.arff";
		String outputModelPath = "/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerResources/abbreviationSpottingModels/" + "abbrv_v_BARR17_train_and_test_sentScop_true_FILTERED_abbr_type.model";

		BufferedReader readerTraining = null;
		try {
			System.out.println("Loading training instances from: " + inputTrainingFile + "...");
			readerTraining = new BufferedReader(new FileReader(inputTrainingFile));
			Instances trainingInstances = new Instances(readerTraining);
			trainingInstances.setRelationName("Abbreviation_Type_Classifier"); 
			trainingInstances.setClassIndex(trainingInstances.numAttributes() - 1);
			readerTraining.close();
			
			// Filter attributes
			Instances filteredARFF = null;
			try {				
				Remove removeFilter = new Remove();
				removeFilter.setOptions(weka.core.Utils.splitOptions("-R 85,87"));
				removeFilter.setInputFormat(trainingInstances);
				filteredARFF = Filter.useFilter(trainingInstances, removeFilter);
				filteredARFF.setClassIndex(filteredARFF.numAttributes() - 1);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			ArffSaver saver = new ArffSaver();
			saver.setInstances(filteredARFF);
			saver.setFile(new File("/home/ronzano/Desktop/Hackathon_PLN/ARFF_FILES/" + "abbrv_v_BARR17_train_and_test_sentScop_true_FILTERED_APPO.arff"));
			saver.writeBatch();
			
			System.out.println("Start training classifier " + WEKA_CLASSIFIER_FULLY_QUALIFIED_CLASS + "...");
			Classifier classIf = null;
			Class<?> clazz = Class.forName(WEKA_CLASSIFIER_FULLY_QUALIFIED_CLASS);
			
			classIf = (Classifier) clazz.newInstance();
			
			classIf.buildClassifier(filteredARFF);
			weka.core.SerializationHelper.write(outputModelPath, classIf);
			
			System.out.println("Stored trained model to: " + outputModelPath);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

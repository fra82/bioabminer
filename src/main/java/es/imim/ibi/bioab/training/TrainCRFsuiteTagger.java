package es.imim.ibi.bioab.training;

import java.io.IOException;

import com.github.jcrfsuite.CrfTrainer;
import com.github.jcrfsuite.util.CrfSuiteLoader;
import com.github.jcrfsuite.util.Pair;

/**
 * Train and store CRF tagger models
 * See: http://www.chokkan.org/software/crfsuite/manual.html for parameter / option settings
 * 
 * @author Francesco Ronzano
 *
 */
public class TrainCRFsuiteTagger {

	private static final String DEFAULT_ALGORITHM = "lbfgs"; // lbfgs l2sgd
	private static final String DEFAULT_GRAPHICAL_MODEL_TYPE = "crf1d";
	public static final String DEFAULT_ENCODING = "UTF-8";

	static {
		try {
			CrfSuiteLoader.load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		String inputTrainingFile = null;
		String outputModelPath = null;
		
		try {
			// CRF model for LONG form detection (BOI)
			inputTrainingFile = "/home/ronzano/Desktop/Hackathon_PLN/CRFsuite_FILES/"
					+ "BOI_LF_abbrv_v_BARR17_train_and_test_sentScop_true.crfstrain";
			outputModelPath = "/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerResources/abbreviationSpottingModels/"
					+ "BOI_LF_abbrv_v_BARR17_train_and_test_sentScop_true.model";
			
			CrfTrainer.train(inputTrainingFile, outputModelPath,
					DEFAULT_ALGORITHM, DEFAULT_GRAPHICAL_MODEL_TYPE, DEFAULT_ENCODING, 
					new Pair<String, String>("c1", "0.01"), new Pair<String, String>("c2", "0.01"),
					new Pair<String, String>("epsilon", "0.00001"), new Pair<String, String>("delta", "0.00001"), 
					new Pair<String, String>("num_memories", "6"), new Pair<String, String>("max_iterations", "1000"));
			System.out.println("Stored model to: " + outputModelPath);
		} catch (IOException e) {
			System.out.println("Exception while training model");
			e.printStackTrace();
		}
		
		try {
			// CRF model for SHORT ABBRTYPE form type detection (ABBRTYPE)
			inputTrainingFile = "/home/ronzano/Desktop/Hackathon_PLN/CRFsuite_FILES/"
					+ "BOI_SF_ABBRTYPE_abbrv_v_BARR17_train_and_test_sentScop_true.crfstrain";
			outputModelPath = "/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerResources/abbreviationSpottingModels/"
					+ "BOI_SF_ABBRTYPE_abbrv_v_BARR17_train_and_test_sentScop_true.model";
			
			CrfTrainer.train(inputTrainingFile, outputModelPath,
					DEFAULT_ALGORITHM, DEFAULT_GRAPHICAL_MODEL_TYPE, DEFAULT_ENCODING, 
					new Pair<String, String>("c1", "0.01"), new Pair<String, String>("c2", "0.01"),
					new Pair<String, String>("epsilon", "0.00001"), new Pair<String, String>("delta", "0.00001"), 
					new Pair<String, String>("num_memories", "6"), new Pair<String, String>("max_iterations", "1000"));
			System.out.println("Stored model to: " + outputModelPath);
		} catch (IOException e) {
			System.out.println("Exception while training model");
			e.printStackTrace();
		}
		
		try {
			// CRF model for SHORT form type detection (BOI)
			inputTrainingFile = "/home/ronzano/Desktop/Hackathon_PLN/CRFsuite_FILES/"
					+ "BOI_SF_abbrv_v_BARR17_train_and_test_sentScop_true.crfstrain";
			outputModelPath = "/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerResources/abbreviationSpottingModels/"
					+ "BOI_SF_abbrv_v_BARR17_train_and_test_sentScop_true.model";
			
			CrfTrainer.train(inputTrainingFile, outputModelPath,
					DEFAULT_ALGORITHM, DEFAULT_GRAPHICAL_MODEL_TYPE, DEFAULT_ENCODING, 
					new Pair<String, String>("c1", "0.01"), new Pair<String, String>("c2", "0.01"),
					new Pair<String, String>("epsilon", "0.00001"), new Pair<String, String>("delta", "0.00001"), 
					new Pair<String, String>("num_memories", "6"), new Pair<String, String>("max_iterations", "1000"));
			System.out.println("Stored model to: " + outputModelPath);
		} catch (IOException e) {
			System.out.println("Exception while training model");
			e.printStackTrace();
		}

	}
}

/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.exec.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.gateutils.generic.PropertyManager;
import org.backingdata.mlfeats.FeatUtil;
import org.backingdata.mlfeats.FeatureSet;
import org.backingdata.mlfeats.exception.FeatSetConsistencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jcrfsuite.util.Pair;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import es.imim.ibi.bioab.feature.TokenFeatureGenerator;
import es.imim.ibi.bioab.nlp.freeling.FreelingParser;
import es.imim.ibi.bioab.nlp.mate.MateParser;
import gate.Annotation;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import weka.classifiers.misc.InputMappedClassifier;
import weka.classifiers.misc.SerializedClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;


/**
 * Classifier of abbreviation type.
 * 
 * Identifies the abbreviations of an abbreviation token in a text 
 * previously parsed by Freeling and dependency parsed by MATE.
 * 
 * @author Francesco Ronzano
 */
@CreoleResource(name = "BioAB Abbreviation Type Classifier Module")
public class BioABabbrvTypeClassifier extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(BioABabbrvTypeClassifier.class);

	private static InputMappedClassifier classifier = null;
	private static Object classifierSynch = new Object();
	private static Instances headerModel = null;
	private static MultiFilter multiFilter = new MultiFilter();
	private static Object multiFilterSynch = new Object();

	public static final String mainAnnSet = "BioAB";

	public static final String abbreviationType = "Abbreviation";

	public static final String abbrvTypeFeat = "AbbrvTypeClass";

	// Where to read input textual annotations and features
	private String tokenAnnSet = FreelingParser.mainAnnSet;
	private String tokenType = FreelingParser.tokenType;
	private String tokenLemmaFeat = FreelingParser.tokenType_lemmaFeatName;
	private String tokenPOSFeat = FreelingParser.tokenType_POSFeatName;
	private String tokenDepFunctFeat = MateParser.depKindFeat;
	private String sentenceAnnSet = FreelingParser.mainAnnSet;
	private String sentenceType = FreelingParser.sentenceType;

	private FeatureSet<Document, TokenFeatureGenerationContext> featSet = null;


	private static boolean isInitialized = false;
	private static Random rnd = new Random();


	// Setters and getters
	public String getTokenAnnSet() {
		return tokenAnnSet;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.mainAnnSet, comment = "The name of the annotation set where to read token annotations from.")
	public void setTokenAnnSet(String tokenAnnSet) {
		this.tokenAnnSet = tokenAnnSet;
	}

	public String getTokenType() {
		return tokenType;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.tokenType, comment = "The annotation type of tokens.")
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getTokenLemmaFeat() {
		return tokenLemmaFeat;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.tokenType_lemmaFeatName, comment = "The name of the token features that contains the lemma of the token.")
	public void setTokenLemmaFeat(String tokenLemmaFeat) {
		this.tokenLemmaFeat = tokenLemmaFeat;
	}

	public String getTokenPOSFeat() {
		return tokenPOSFeat;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.tokenType_lemmaFeatName, comment = "The name of the token features that contains the POS of the token.")
	public void setTokenPOSFeat(String tokenPOSFeat) {
		this.tokenPOSFeat = tokenPOSFeat;
	}

	public String getTokenDepFunctFeat() {
		return tokenDepFunctFeat;
	}

	@RunTime
	@CreoleParameter(defaultValue = MateParser.depKindFeat, comment = "The name of the token features that contains the dependency relation of the token towards its head.")
	public void setTokenDepFunctFeat(String tokenDepFunctFeat) {
		this.tokenDepFunctFeat = tokenDepFunctFeat;
	}

	public String getSentenceAnnSet() {
		return sentenceAnnSet;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.mainAnnSet, comment = "The name of the annotation set where to read sentence annotations from.")
	public void setSentenceAnnSet(String sentenceAnnSet) {
		this.sentenceAnnSet = sentenceAnnSet;
	}

	public String getSentenceType() {
		return sentenceType;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.sentenceType, comment = "The annotation type of sentences.")
	public void setSentenceType(String sentenceType) {
		this.sentenceType = sentenceType;
	}

	@Override
	public Resource init() {

		if(!isInitialized) {

			try {
				String bioABminerResourceFolder = PropertyManager.getProperty("resourceFolder.fullPath");
				if(!bioABminerResourceFolder.endsWith(File.separator)) bioABminerResourceFolder += File.separator;

				classifier = null;

				/* Load model from file */
				try {
					logger.info("Loading classifier...");

					boolean modelLoaded = this.loadClassificationModels(bioABminerResourceFolder);

					if(modelLoaded) {
						logger.info("Classifiers loaded.");
					}
					else {
						GenericUtil.notifyException("Impossible to load the classifier", new Exception("Error while loading classifier"), logger);
					}

				} catch (Exception ex) {
					ex.printStackTrace();
					logger.error("Exception while loading classifier ---> " + ex.getMessage());
					return this;
				}

				// Load ARFF filter
				try {
					multiFilter = new MultiFilter();
					multiFilter.setFilters(TokenFeatureGenerator.getFilterArrayForARFF());
					File inputDataStructure = new File(bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + "abbrv_v_BARR17_train_and_test_sentScop_true_abbr_type_HEAD.arff");
					BufferedReader reader = new BufferedReader(new FileReader(inputDataStructure.getAbsolutePath()));
					Instances inputDataStructureInst = new Instances(reader);
					inputDataStructureInst.setClassIndex(inputDataStructureInst.numAttributes() - 1);
					multiFilter.setInputFormat(inputDataStructureInst);
				} catch (Exception e) {
					logger.error("\nError loading Weka filter ---> " + e.getMessage());
					e.printStackTrace();
				}

				isInitialized = true;

			} catch (Exception e) {
				GenericUtil.notifyException("Initializing BioAB Abbreviation Type Classifier Module", e, logger);
			}

		}

		try {
			// Instantiate feature generator classes by passing the BioAB miner resource folder
			// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
			featSet = TokenFeatureGenerator.generateFeatureSet(PropertyManager.getProperty("resourceFolder.fullPath"));
		} catch (Exception e) {
			GenericUtil.notifyException("Initializing BioAB Abbreviation Type Classifier Module", e, logger);
		}

		return this;
	}

	@Override
	public void execute() {

		if(!isInitialized) {
			this.init();
		}

		long t1 = System.currentTimeMillis();

		Integer sentenceIDappo = Integer.MAX_VALUE;
		Integer tokenCount = 0;

		// Select all the sentences of the document
		// Computing document level features
		List<Annotation> documentTokenAnnList = new ArrayList<Annotation>();
		Map<Integer, Integer> tokenIDtoSentenceIDAPPOmap = new HashMap<Integer, Integer>();
		List<Pair<Integer, Annotation>> sentenceIdsOrdered = new ArrayList<Pair<Integer, Annotation>>();
		LinkedHashMap<Integer, List<Pair<Integer, Annotation>>> sentenceIdToTokenListMap = new LinkedHashMap<Integer, List<Pair<Integer, Annotation>>>();
		Map<Annotation, Annotation> tokenAnnoToAbbrebviationAnnoMap = new HashMap<Annotation, Annotation>();
		for(Annotation abbreviationAnnotation : GATEutils.getAnnInDocOrder(this.document, mainAnnSet, abbreviationType)) {
			List<Annotation> tokensOfAbbreviation = GATEutils.getAnnInDocOrderContainedAnn(this.document, this.tokenAnnSet, this.tokenType, abbreviationAnnotation);
			documentTokenAnnList.addAll(tokensOfAbbreviation);

			// Set Sentence ID of each token
			for(Annotation tokenOfAbbreviation : tokensOfAbbreviation) {
				tokenAnnoToAbbrebviationAnnoMap.put(tokenOfAbbreviation, abbreviationAnnotation);

				List<Annotation> sentenceAnnList = GATEutils.getAnnInDocOrderIntersectAnn(this.document, this.sentenceAnnSet, this.sentenceType, tokenOfAbbreviation);
				if(sentenceAnnList != null && sentenceAnnList.size() > 0) {
					tokenIDtoSentenceIDAPPOmap.put(tokenOfAbbreviation.getId(), sentenceAnnList.get(0).getId());

					if(sentenceIdToTokenListMap != null && !sentenceIdToTokenListMap.containsKey(sentenceAnnList.get(0).getId())) {
						sentenceIdToTokenListMap.put(sentenceAnnList.get(0).getId(), new ArrayList<Pair<Integer, Annotation>>());
						sentenceIdsOrdered.add(new Pair<Integer, Annotation>(sentenceAnnList.get(0).getId(), sentenceAnnList.get(0)));
					}

					sentenceIdToTokenListMap.get(sentenceAnnList.get(0).getId()).add(new Pair<Integer, Annotation>(tokenOfAbbreviation.getId(), tokenOfAbbreviation));

					if(sentenceAnnList.size() > 1) {
						logger.warn("Multiple sentence id for token!");
					}
				}
				else {
					logger.warn("No sentence id for token! Generating a random one");
					tokenIDtoSentenceIDAPPOmap.put(tokenOfAbbreviation.getId(), --sentenceIDappo);
				}
			}
		}

		String documentID = ((this.document.getName() != null && this.document.getName().trim().length() > 0) ? document.getName() : "DOC_ID_" + rnd.nextInt(100000));

		// Create a training example for each token of the document - in the documentTokenAnnList
		for(Annotation documentTokenAnn : documentTokenAnnList) {

			// Set training context
			TokenFeatureGenerationContext trCtx = new TokenFeatureGenerationContext(this.document);
			trCtx.setCoreTokenAnn(documentTokenAnn);

			// Set document global features
			trCtx.setDocumentTokenList(documentTokenAnnList);
			trCtx.setTokenIDtoSentenceIDmap(tokenIDtoSentenceIDAPPOmap);

			// Set Sentence ID
			trCtx.setSentenceID("" + tokenIDtoSentenceIDAPPOmap.get(documentTokenAnn.getId()));
			trCtx.setGATEsentenceID(tokenIDtoSentenceIDAPPOmap.get(documentTokenAnn.getId()));

			// Set Document ID
			trCtx.setDocumentID(documentID);

			featSet.addElement(this.document, trCtx);
			tokenCount++;

			System.out.print("+");
			if(tokenCount % 100 == 0) {
				System.out.print("\n > " + tokenCount + " tokens > ");
			} 
		}

		// Generate ARFF
		Instances ARFFinstances = null;
		try {
			ARFFinstances = FeatUtil.wekaInstanceGeneration(featSet, "abbrv_v_" + documentID);
			featSet.emptySet();
		} catch (FeatSetConsistencyException e) {
			logger.error("ERROR while generating ARFF / CRF suite features.");
			e.printStackTrace();
		}

		// Filter ARFF
		Instances filteredARFF_step1 = null;
		Instances filteredARFF_step2 = null;
		try {
			synchronized(multiFilterSynch) {
				filteredARFF_step1 = Filter.useFilter(ARFFinstances, multiFilter);
			}
			filteredARFF_step1.setClassIndex(filteredARFF_step1.numAttributes() - 1);

			Remove removeFilter = new Remove();
			removeFilter.setOptions(weka.core.Utils.splitOptions("-R 85,87"));
			removeFilter.setInputFormat(filteredARFF_step1);
			filteredARFF_step2 = Filter.useFilter(filteredARFF_step1, removeFilter);
			filteredARFF_step2.setClassIndex(filteredARFF_step2.numAttributes() - 1);

		} catch (Exception e) {
			logger.error("ERROR while generating ARFF / CRF suite features.");
			e.printStackTrace();
		}

		// ***************************************************************
		// ***************************************************************
		// ***** Abbreviations spotting (SF type) ************************
		try {
			if(filteredARFF_step2 != null) {
				if(documentTokenAnnList.size()!= filteredARFF_step2.numInstances()) {
					System.out.println("Error!!! Different number of tokens than weka instances (" + documentTokenAnnList.size() + " / " + filteredARFF_step2.numInstances() + ")");
				}

				/* REDIRECTING STD OUT AND ERR - START */
				PrintStream out = System.out;
				PrintStream err = System.err;
				System.setOut(new PrintStream(new OutputStream() {
					@Override public void write(int b) throws IOException {}
				}));
				System.setErr(new PrintStream(new OutputStream() {
					@Override public void write(int b) throws IOException {}
				}));

				try {
					for(int tokIdx = 0; tokIdx < documentTokenAnnList.size(); tokIdx++) {

						Annotation tokenAnno = documentTokenAnnList.get(tokIdx);

						try {
							// Classify instance
							Instance inst = filteredARFF_step2.instance(tokIdx);

							Double classInst = -1d;
							synchronized(classifierSynch) {
								classInst = classifier.classifyInstance(inst);
							}
							String annotationType = headerModel.attribute(headerModel.numAttributes() -1).value((int) classInst.intValue());
							logger.debug("\n---\n* Instance classified as: " + annotationType);
							double[] classDistibInst = classifier.distributionForInstance(inst);
							/* original code end */

							// ADD PROBABILITY OF EACH CLASS AF FEATURES WITH NAMES STARTING WITH 'PROB_'
							FeatureMap fm = Factory.newFeatureMap();
							Map<String, Double> classProbabilityMap = new HashMap<String, Double>();
							for(int i = 0; i < classDistibInst.length; i++) {
								logger.debug("    -> Instance: " + headerModel.attribute(headerModel.numAttributes() -1).value((int) i) + " (" + i + ") --> " + classDistibInst[i]);
								fm.put(headerModel.attribute(headerModel.numAttributes() -1).value((int) i), classDistibInst[i]);
								classProbabilityMap.put(headerModel.attribute(headerModel.numAttributes() -1).value((int) i), classDistibInst[i]);
								// Add to sentence feature set the probability assigned to each class
								tokenAnno.getFeatures().put("PROB_" + headerModel.attribute(headerModel.numAttributes() -1).value((int) i), new Double(classDistibInst[i]));
								if(tokenAnno != null) {
									tokenAnno.getFeatures().put("PROB_" + headerModel.attribute(headerModel.numAttributes() -1).value((int) i), new Double(classDistibInst[i]));
								}
							}

							// GENERATE ANNOTATION FEATURE
							tokenAnno.getFeatures().put(abbrvTypeFeat, annotationType);

							// Add features to Abbreviation annotation
							if(tokenAnnoToAbbrebviationAnnoMap != null && tokenAnnoToAbbrebviationAnnoMap.containsKey(tokenAnno) && tokenAnnoToAbbrebviationAnnoMap.get(tokenAnno) != null) {
								tokenAnnoToAbbrebviationAnnoMap.get(tokenAnno).setFeatures((tokenAnnoToAbbrebviationAnnoMap.get(tokenAnno).getFeatures() == null) ? Factory.newFeatureMap() : tokenAnnoToAbbrebviationAnnoMap.get(tokenAnno).getFeatures());
								tokenAnnoToAbbrebviationAnnoMap.get(tokenAnno).getFeatures().put(abbrvTypeFeat + "_" + tokenAnno.getId(), annotationType);
							}

						} catch (Exception e) {
							e.printStackTrace();
							GenericUtil.notifyException("Impossible classify token: " + GATEutils.getAnnotationText(tokenAnno, document).orElse("NOT_PRESENT"), e, logger);
						}
						
					}
				} catch (Exception e) {
					e.printStackTrace();
					GenericUtil.notifyException("Classifier error", e, logger);
				}
				finally {
					System.setOut(out);
					System.setErr(err);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			GenericUtil.notifyException("Generic error", e, logger);
		}


		long needed = System.currentTimeMillis() - t1;
		logger.debug("   - End tagging document: " + (((this.document.getName() != null) ? this.document.getName() : "NULL")));
		logger.debug("     in (seconds): " + (needed / 1000));
		logger.debug("********************************************");
	}


	public boolean loadClassificationModels(String bioABminerResourceFolder) throws Exception {

		File classifierModel = new File(bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + "abbrv_v_BARR17_train_and_test_sentScop_true_FILTERED_abbr_type.model");
		File classifierDataStructure = new File(bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + "abbrv_v_BARR17_train_and_test_sentScop_true_FILTERED_abbr_type_HEAD.arff");

		logger.info("Classifier model file: " + classifierModel.getAbsolutePath());
		logger.info("Classifier structure file: " + classifierDataStructure.getAbsolutePath());

		if(classifierModel == null || !classifierModel.exists()) {
			GenericUtil.notifyException("BOI Impossible to load classifier model file: " + bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + ".model", new Exception("Error while loading classification model"), logger);
			return false;
		}

		if(classifierDataStructure == null || !classifierDataStructure.exists()) {
			GenericUtil.notifyException("BOI Impossible to load classifier data structure file: " + bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + ".arff", new Exception("Error while loading classification data structure"), logger);
			return false;
		}

		// Load classifier BOI
		SerializedClassifier coreClassifier = new SerializedClassifier();
		coreClassifier.setModelFile(classifierModel);
		coreClassifier.setDebug(false);

		// Load InputMappedClassifier and set the just loaded model as classifier
		classifier = new InputMappedClassifier();
		classifier.setClassifier(coreClassifier);

		// DataSource source = new DataSource(classifierDataStructure.getAbsolutePath());
		// headerModel = source.getDataSet();

		BufferedReader reader = new BufferedReader(new FileReader(classifierDataStructure.getAbsolutePath()));
		headerModel = new Instances(reader);
		headerModel.setClassIndex(headerModel.numAttributes() - 1);
		classifier.setModelHeader(headerModel);

		classifier.setDebug(false);
		classifier.setSuppressMappingReport(true);			
		classifier.setTrim(true);
		classifier.setIgnoreCaseForNames(false);

		return true;
	}

	public boolean resetAnnotations() {
		List<Annotation> bioABannoList = GATEutils.getAnnInDocOrder(this.document, mainAnnSet, abbreviationType);

		for(Annotation bioABanno : bioABannoList) {
			if(bioABanno != null && bioABanno.getFeatures() != null) {
				bioABanno.getFeatures().remove(abbrvTypeFeat);

				Set<String> stringFeaturesNameToDel = new HashSet<String>();
				for(Entry<Object, Object> featureEntry : bioABanno.getFeatures().entrySet()) {
					if(featureEntry != null && featureEntry.getKey() != null && featureEntry.getKey() instanceof String &&
							((String) featureEntry.getKey()).startsWith("PROB_")) {
						stringFeaturesNameToDel.add((String) featureEntry.getKey());
					}
				}

				for(String featureToDel : stringFeaturesNameToDel) {
					bioABanno.getFeatures().remove(featureToDel);
				}
			}
		}

		return true;
	}

	public static void main(String[] args) {

	}
}
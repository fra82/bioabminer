package es.imim.ibi.bioab.exec;

import java.io.File;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.backingdata.gateutils.GATEinit;
import org.backingdata.gateutils.generic.PropertyManager;
import org.backingdata.nlp.utils.Manage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.exec.resource.BioABabbrvLFspotter;
import es.imim.ibi.bioab.exec.resource.BioABabbrvSpotter;
import es.imim.ibi.bioab.exec.resource.BioABabbrvTypeClassifier;
import es.imim.ibi.bioab.nlp.freeling.FreelingParser;
import es.imim.ibi.bioab.nlp.mate.MateParser;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;

/**
 * Core BioAB Miner (Biomedical Abbreviations Miner) class that
 * exposes a set of static method useful to:
 * - initialize BioAB Miner
 * - load text document or PDF of scientific publications
 * - annotate abbreviations from these texts
 * - annotate the long form associated to each abbreviation where it occurs next to the same abbreviation
 * - extract lists of abbreviations / long forms
 * 
 * @author Francesco Ronzano
 *
 */
public class BioABminer {

	private static Logger logger = LoggerFactory.getLogger(BioABabbrvSpotter.class);

	private static FreelingParser FreelingParser_Resource = null;
	private static MateParser MateParser_Resource = null;
	private static BioABabbrvSpotter BioABabbrvSpotter_Resource = null;
	private static BioABabbrvTypeClassifier BioABabbrvTypeClassifier_Resource = null;
	private static BioABabbrvLFspotter BioABabbrvLFspotter_Resource = null;

	private static boolean isInitializedALL = false;
	private static boolean isInitializedNLP = false;
	private static boolean isInitializedABBRV = false;
	
	/**
	 * Initialize BioAB miner
	 * 
	 * @param bioABminerPropertyFilePath full local path to the BioAB miner property file
	 * 
	 */
	public static void initAll(String bioABminerPropertyFilePath) {

		if(!isInitializedALL) {

			// Set the full path of the configuration file of BioAB Miner
			PropertyManager.setPropertyFilePath(bioABminerPropertyFilePath);

			// Initialize GATE
			try {
				GATEinit.initGate(PropertyManager.getProperty("gate.home"), PropertyManager.getProperty("gate.plugins"));
			}
			catch (Exception e) {
				logger.error("\nError initializing GATE ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Init NLP-utils library by passing the BioAB miner resource folder
			// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
			try {
				Manage.setResourceFolder(PropertyManager.getProperty("resourceFolder.fullPath"));
			}
			catch (Exception e) {
				logger.error("\nError initializing NLP-utils library ---> " + e.getMessage());
				e.printStackTrace();
			}
			
			initNLP(bioABminerPropertyFilePath);
			initABBRV(bioABminerPropertyFilePath);
			
			isInitializedALL = true;
		}

	}
	
	/**
	 * Initialize NLP Modules: GATE, Freeling and MATE
	 * 
	 * @param bioABminerPropertyFilePath
	 */
	public static void initNLP(String bioABminerPropertyFilePath) {

		if(!isInitializedNLP) {

			// Set the full path of the configuration file of BioAB Miner
			PropertyManager.setPropertyFilePath(bioABminerPropertyFilePath);

			// Initialize GATE
			try {
				GATEinit.initGate(PropertyManager.getProperty("gate.home"), PropertyManager.getProperty("gate.plugins"));
			}
			catch (Exception e) {
				logger.error("\nError initializing GATE ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Init NLP-utils library by passing the BioAB miner resource folder
			// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
			try {
				Manage.setResourceFolder(PropertyManager.getProperty("resourceFolder.fullPath"));
			}
			catch (Exception e) {
				logger.error("\nError initializing NLP-utils library ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Instantiate Freeling parser
			try {
				Gate.getCreoleRegister().registerComponent(FreelingParser.class);

				FeatureMap FreelingParserfm = Factory.newFeatureMap();
				FreelingParserfm.put("analysisLang", "SPA");
				FreelingParserfm.put("addAnalysisLangToAnnSetName", "true");
				FreelingParser_Resource = (FreelingParser) gate.Factory.createResource(FreelingParser.class.getName(), FreelingParserfm);
			}
			catch (Exception e) {
				logger.error("\nError loading Freeling ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Instantiate MATE parser
			try {
				Gate.getCreoleRegister().registerComponent(MateParser.class);

				FeatureMap MateParserfm = Factory.newFeatureMap();
				// Specify the annotation set and type for sentences and tokens identified by Freeling
				MateParserfm.put("sentenceAnnotationSetToAnalyze", FreelingParser.mainAnnSet + "_SPA");
				MateParserfm.put("sentenceAnnotationTypeToAnalyze", FreelingParser.sentenceType);
				MateParserfm.put("tokenAnnotationSetToAnalyze", FreelingParser.mainAnnSet + "_SPA");
				MateParserfm.put("tokenAnnotationTypeToAnalyze", FreelingParser.tokenType);

				// Parse sentences no longler than 120 tokens
				MateParserfm.put("excludeThreshold", 120);

				// Set the path of the MATE models for Spanish available in the NLP-utils resource folder
				// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
				String NLPutilsResourceFolder = PropertyManager.getProperty("resourceFolder.fullPath");
				NLPutilsResourceFolder = (NLPutilsResourceFolder.endsWith(File.separator)) ? NLPutilsResourceFolder : NLPutilsResourceFolder + File.separator;
				String baseModelPath = NLPutilsResourceFolder + "mate_models" + File.separator;
				MateParserfm.put("lemmaModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.lemmatizer.model");
				MateParserfm.put("postaggerModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.postagger.model");
				MateParserfm.put("parserModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.parser.model");
				MateParserfm.put("srlModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.srl-4.21.srl-rr.model");

				MateParser_Resource = (MateParser) gate.Factory.createResource(MateParser.class.getName(), MateParserfm);
			}
			catch (Exception e) {
				logger.error("\nError loading Freeling ---> " + e.getMessage());
				e.printStackTrace();
			}


			// Instantiate BioAB spotter
			try {
				Gate.getCreoleRegister().registerComponent(BioABabbrvSpotter.class);

				FeatureMap BioABspotterFm = Factory.newFeatureMap();
				BioABspotterFm.put("tokenAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABspotterFm.put("tokenType", FreelingParser.tokenType);
				BioABspotterFm.put("tokenLemmaFeat", FreelingParser.tokenType_lemmaFeatName);
				BioABspotterFm.put("tokenPOSFeat", FreelingParser.tokenType_POSFeatName);
				BioABspotterFm.put("tokenDepFunctFeat", MateParser.depKindFeat);
				BioABspotterFm.put("sentenceAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABspotterFm.put("sentenceType", FreelingParser.sentenceType);

				BioABabbrvSpotter_Resource = (BioABabbrvSpotter) gate.Factory.createResource(BioABabbrvSpotter.class.getName(), BioABspotterFm);
			}
			catch (Exception e) {
				logger.error("\nError loading BioAB spotter ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Instantiate BioAB Type Classifier
			try {
				Gate.getCreoleRegister().registerComponent(BioABabbrvTypeClassifier.class);

				FeatureMap BioABtypeClassifierFm = Factory.newFeatureMap();
				BioABtypeClassifierFm.put("tokenAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABtypeClassifierFm.put("tokenType", FreelingParser.tokenType);
				BioABtypeClassifierFm.put("tokenLemmaFeat", FreelingParser.tokenType_lemmaFeatName);
				BioABtypeClassifierFm.put("tokenPOSFeat", FreelingParser.tokenType_POSFeatName);
				BioABtypeClassifierFm.put("tokenDepFunctFeat", MateParser.depKindFeat);
				BioABtypeClassifierFm.put("sentenceAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABtypeClassifierFm.put("sentenceType", FreelingParser.sentenceType);

				BioABabbrvTypeClassifier_Resource = (BioABabbrvTypeClassifier) gate.Factory.createResource(BioABabbrvTypeClassifier.class.getName(), BioABtypeClassifierFm);
			}
			catch (Exception e) {
				logger.error("\nError loading BioAB spotter ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Instantiate BioAB LF Spotter
			try {
				Gate.getCreoleRegister().registerComponent(BioABabbrvLFspotter.class);

				FeatureMap BioABLFspotterFm = Factory.newFeatureMap();
				BioABLFspotterFm.put("tokenAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABLFspotterFm.put("tokenType", FreelingParser.tokenType);
				BioABLFspotterFm.put("tokenLemmaFeat", FreelingParser.tokenType_lemmaFeatName);
				BioABLFspotterFm.put("tokenPOSFeat", FreelingParser.tokenType_POSFeatName);
				BioABLFspotterFm.put("tokenDepFunctFeat", MateParser.depKindFeat);
				BioABLFspotterFm.put("sentenceAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABLFspotterFm.put("sentenceType", FreelingParser.sentenceType);
				BioABLFspotterFm.put("chunkAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABLFspotterFm.put("chunkType", FreelingParser.chunkType);
				BioABLFspotterFm.put("chunkLabelFeat", FreelingParser.chunkType_labelFeatName);
				
				BioABabbrvLFspotter_Resource = (BioABabbrvLFspotter) gate.Factory.createResource(BioABabbrvLFspotter.class.getName(), BioABLFspotterFm);
			}
			catch (Exception e) {
				logger.error("\nError loading BioAB spotter ---> " + e.getMessage());
				e.printStackTrace();
			}

			isInitializedNLP = true;
		}

	}
	
	/**
	 * Initialize abbreviation extraction modules: BioAB Abbreviation Spotter, BioAB Abbreviation Type Classifier and BioAB Abbreviation Long Form Spotter
	 * 
	 * @param bioABminerPropertyFilePath
	 */
	public static void initABBRV(String bioABminerPropertyFilePath) {

		if(!isInitializedABBRV) {

			// Set the full path of the configuration file of BioAB Miner
			PropertyManager.setPropertyFilePath(bioABminerPropertyFilePath);

			// Initialize GATE
			try {
				GATEinit.initGate(PropertyManager.getProperty("gate.home"), PropertyManager.getProperty("gate.plugins"));
			}
			catch (Exception e) {
				logger.error("\nError initializing GATE ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Init NLP-utils library by passing the BioAB miner resource folder
			// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
			try {
				Manage.setResourceFolder(PropertyManager.getProperty("resourceFolder.fullPath"));
			}
			catch (Exception e) {
				logger.error("\nError initializing NLP-utils library ---> " + e.getMessage());
				e.printStackTrace();
			}
			
			// Instantiate BioAB spotter
			try {
				Gate.getCreoleRegister().registerComponent(BioABabbrvSpotter.class);

				FeatureMap BioABspotterFm = Factory.newFeatureMap();
				BioABspotterFm.put("tokenAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABspotterFm.put("tokenType", FreelingParser.tokenType);
				BioABspotterFm.put("tokenLemmaFeat", FreelingParser.tokenType_lemmaFeatName);
				BioABspotterFm.put("tokenPOSFeat", FreelingParser.tokenType_POSFeatName);
				BioABspotterFm.put("tokenDepFunctFeat", MateParser.depKindFeat);
				BioABspotterFm.put("sentenceAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABspotterFm.put("sentenceType", FreelingParser.sentenceType);

				BioABabbrvSpotter_Resource = (BioABabbrvSpotter) gate.Factory.createResource(BioABabbrvSpotter.class.getName(), BioABspotterFm);
			}
			catch (Exception e) {
				logger.error("\nError loading BioAB spotter ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Instantiate BioAB Type Classifier
			
			try {
				Gate.getCreoleRegister().registerComponent(BioABabbrvTypeClassifier.class);

				FeatureMap BioABtypeClassifierFm = Factory.newFeatureMap();
				BioABtypeClassifierFm.put("tokenAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABtypeClassifierFm.put("tokenType", FreelingParser.tokenType);
				BioABtypeClassifierFm.put("tokenLemmaFeat", FreelingParser.tokenType_lemmaFeatName);
				BioABtypeClassifierFm.put("tokenPOSFeat", FreelingParser.tokenType_POSFeatName);
				BioABtypeClassifierFm.put("tokenDepFunctFeat", MateParser.depKindFeat);
				BioABtypeClassifierFm.put("sentenceAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABtypeClassifierFm.put("sentenceType", FreelingParser.sentenceType);

				BioABabbrvTypeClassifier_Resource = (BioABabbrvTypeClassifier) gate.Factory.createResource(BioABabbrvTypeClassifier.class.getName(), BioABtypeClassifierFm);
			}
			catch (Exception e) {
				logger.error("\nError loading BioAB spotter ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Instantiate BioAB LF Spotter
			try {
				Gate.getCreoleRegister().registerComponent(BioABabbrvLFspotter.class);

				FeatureMap BioABLFspotterFm = Factory.newFeatureMap();
				BioABLFspotterFm.put("tokenAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABLFspotterFm.put("tokenType", FreelingParser.tokenType);
				BioABLFspotterFm.put("tokenLemmaFeat", FreelingParser.tokenType_lemmaFeatName);
				BioABLFspotterFm.put("tokenPOSFeat", FreelingParser.tokenType_POSFeatName);
				BioABLFspotterFm.put("tokenDepFunctFeat", MateParser.depKindFeat);
				BioABLFspotterFm.put("sentenceAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABLFspotterFm.put("sentenceType", FreelingParser.sentenceType);
				BioABLFspotterFm.put("chunkAnnSet", FreelingParser.mainAnnSet + "_SPA");
				BioABLFspotterFm.put("chunkType", FreelingParser.chunkType);
				BioABLFspotterFm.put("chunkLabelFeat", FreelingParser.chunkType_labelFeatName);
				
				BioABabbrvLFspotter_Resource = (BioABabbrvLFspotter) gate.Factory.createResource(BioABabbrvLFspotter.class.getName(), BioABLFspotterFm);
			}
			catch (Exception e) {
				logger.error("\nError loading BioAB spotter ---> " + e.getMessage());
				e.printStackTrace();
			}

			isInitializedABBRV = true;
		}

	}
	


	/**
	 * Load a GATE Document from text
	 * 
	 * @param docText
	 * @return
	 */
	public static Document getDocumentFormText(String docText) {

		Document gateDoc;

		if(!StringUtils.isEmpty(docText)) {
			try {
				gateDoc = gate.Factory.newDocument(docText);
				return gateDoc;
			} catch (ResourceInstantiationException e) {
				logger.error("\nError loading GATE document from text - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * Load a GATE Document from a GATE XML file
	 * 
	 * @param docText
	 * @return
	 */
	public static Document getDocumentFormGATEXMLfile(String filePath) {

		Document gateDoc;

		if(!StringUtils.isEmpty(filePath)) {
			try {
				gateDoc = gate.Factory.newDocument(new File(filePath).toURI().toURL());
				return gateDoc;
			} catch (Exception e) {
				logger.error("\nError loading GATE document from text - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}
		}

		return null;
	}



	/**
	 * Load a GATE Document from the PDF file of a scientific publication
	 * (GROBID - https://github.com/kermitt2/grobid - is exploited to extract structured text from the PDF)
	 * 
	 * @return
	 */
	public static Document getDocumentFormPDF() {

		// TO IMPLEMENT...

		return null;
	}


	/**
	 * Apply Freeling and Mate to the Document
	 * 
	 * @return
	 */
	public static Document extractNLPfeatures(Document gateDocToParse) {

		if(gateDocToParse != null) {
			try {
				FreelingParser_Resource.setDocument(gateDocToParse);
				FreelingParser_Resource.execute();
				FreelingParser_Resource.setDocument(null);
			} catch (Exception e) {
				logger.error("\nError parsing GATE document by Freeling - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}


			try {
				MateParser_Resource.setDocument(gateDocToParse);
				MateParser_Resource.execute();
				MateParser_Resource.setDocument(null);
			} catch (Exception e) {
				logger.error("\nError parsing GATE document by MATE - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}

			return gateDocToParse;
		}

		return null;
	}


	/**
	 * Tag the abbreviations and acronym in the document by mans of the 
	 * {@link es.imim.ibi.bioab.exec.resource.BioABabbrvSpotter} processing resources
	 * that exploits CRFsuit sequence taggers
	 * 
	 * @return
	 */
	public static Document extractAbbreviations(Document gateDocToParse) {

		if(gateDocToParse != null) {
			try {
				BioABabbrvSpotter_Resource.setDocument(gateDocToParse);
				BioABabbrvSpotter_Resource.execute();
				BioABabbrvSpotter_Resource.setDocument(null);
			} catch (Exception e) {
				logger.error("\nError parsing GATE document by BioAB Abbreviation Spotter - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}
			
			try {
				BioABabbrvTypeClassifier_Resource.setDocument(gateDocToParse);
				BioABabbrvTypeClassifier_Resource.execute();
				BioABabbrvTypeClassifier_Resource.setDocument(null);
			} catch (Exception e) {
				logger.error("\nError parsing GATE document by BioAB Type Classifier - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}
			
			Set<String> abbrevTypes = gateDocToParse.getAnnotations(BioABabbrvSpotter.mainAnnSet).getAllTypes();
			for(String abbrevType : abbrevTypes) {
				System.out.println("    ABBREV : " + abbrevType + " > " + gateDocToParse.getAnnotations(BioABabbrvSpotter.mainAnnSet).get(abbrevType).size());
			}
			
			try {
				BioABabbrvLFspotter_Resource.setDocument(gateDocToParse);
				BioABabbrvLFspotter_Resource.execute();
				BioABabbrvLFspotter_Resource.setDocument(null);
			} catch (Exception e) {
				logger.error("\nError parsing GATE document by BioAB Long Form Spotter - have you initialized BioABminet by calling BioABminer.initAll(String bioABminerPropertyFilePath)? ---> " + e.getMessage());
				e.printStackTrace();
			}
			
			return gateDocToParse;
		}

		return null;
	}
	
	


}

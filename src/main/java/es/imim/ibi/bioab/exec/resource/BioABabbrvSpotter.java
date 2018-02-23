package es.imim.ibi.bioab.exec.resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.gateutils.generic.PropertyManager;
import org.backingdata.mlfeats.FeatUtil;
import org.backingdata.mlfeats.FeatureSet;
import org.backingdata.mlfeats.exception.FeatSetConsistencyException;
import org.backingdata.mlfeats.ext.CRFsuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jcrfsuite.CrfTagger;
import com.github.jcrfsuite.util.Pair;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import es.imim.ibi.bioab.feature.TokenFeatureGenerator;
import es.imim.ibi.bioab.feature.TokenFeatureGeneratorCRFsuite;
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
import gate.util.InvalidOffsetException;
import third_party.org.chokkan.crfsuite.Attribute;
import third_party.org.chokkan.crfsuite.Item;
import third_party.org.chokkan.crfsuite.ItemSequence;
import third_party.org.chokkan.crfsuite.StringList;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Remove;


/**
 * Spotter of abbreviations, abbreviation types and long forms inside the sentences of a text.
 * 
 * Identifies abbreviations, abbreviation types and long forms inside the text previously parsed 
 * by Freeling and dependency parsed by MATE.
 * 
 * @author Francesco Ronzano
 */
@CreoleResource(name = "BioAB Abbreviation Spotter Module")
public class BioABabbrvSpotter extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(BioABabbrvSpotter.class);

	public static final String mainAnnSet = "BioAB";

	public static final String abbreviationType = "Abbreviation";

	public static final String derived_abbrvType = "Abbreviation_DERIVED";
	public static final String global_abbrvType = "Abbreviation_GLOBAL";
	public static final String contextual_abbrvType = "Abbreviation_CONTEXTUAL";
	public static final String multiple_abbrvType = "Abbreviation_MULTIPLE";
	public static final String short_abbrvType = "Abbreviation_SHORT";

	public static final String longFormType = "LongFormCandidate";

	// Where to read input textual annotations and features
	private String tokenAnnSet = FreelingParser.mainAnnSet;
	private String tokenType = FreelingParser.tokenType;
	private String tokenLemmaFeat = FreelingParser.tokenType_lemmaFeatName;
	private String tokenPOSFeat = FreelingParser.tokenType_POSFeatName;
	private String tokenDepFunctFeat = MateParser.depKindFeat;
	private String sentenceAnnSet = FreelingParser.mainAnnSet;
	private String sentenceType = FreelingParser.sentenceType;

	private FeatureSet<Document, TokenFeatureGenerationContext> featSet = null;

	// Load taggers
	private static CrfTagger crfTagger_SF = null;
	private static Object crfTagger_SFSynch = new Object();
	private static CrfTagger crfTagger_SFtype = null;
	private static Object crfTagger_SFtypeSynch = new Object();
	private static CrfTagger crfTagger_LF = null;
	private static Object crfTagger_LFSynch = new Object();

	private static String arffToInitFilter = "/home/ronzano/Desktop/Hackathon_PLN/ARFF_FILES/abbrv_v_BARR17_train_and_test_sentScop_true.arff";
	private static MultiFilter multiFilter = new MultiFilter();
	private static Object multiFilterSynch = new Object();

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

				// Load CRFsuite models
				try {
					crfTagger_SF = new CrfTagger(bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + "BOI_SF_abbrv_v_BARR17_train_and_test_sentScop_true.model");
					crfTagger_SFtype = new CrfTagger(bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + "BOI_SF_ABBRTYPE_abbrv_v_BARR17_train_and_test_sentScop_true.model");
					crfTagger_LF = new CrfTagger(bioABminerResourceFolder + "abbreviationSpottingModels" + File.separator + "BOI_LF_abbrv_v_BARR17_train_and_test_sentScop_true.model");
				} catch (Exception e) {
					logger.error("\nError loading CRFsuite models ---> " + e.getMessage());
					e.printStackTrace();
				}

				// Load ARFF filter
				try {
					ObjectInputStream objectinputstream = null;
					try {
						FileInputStream streamIn = new FileInputStream(bioABminerResourceFolder + "MultiFilterSerialized.ser");
						objectinputstream = new ObjectInputStream(streamIn);
						multiFilter = (MultiFilter) objectinputstream.readObject();
					} catch (Exception e) {
						logger.error("\nError loading Weka filter ---> " + e.getMessage());
						e.printStackTrace();
					} finally {
						if(objectinputstream != null){
							objectinputstream .close();
						}
					}

				} catch (Exception e) {
					logger.error("\nError loading Weka filter ---> " + e.getMessage());
					e.printStackTrace();
				}

				isInitialized = true;

			} catch (Exception e) {
				GenericUtil.notifyException("Initializing BioAB Abbreviation Spotter Module", e, logger);
			}

		}

		try {
			// Instantiate feature generator classes by passing the BioAB miner resource folder
			// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
			featSet = TokenFeatureGenerator.generateFeatureSet(PropertyManager.getProperty("resourceFolder.fullPath"));
		} catch (Exception e) {
			GenericUtil.notifyException("Initializing BioAB Abbreviation Spotter Module", e, logger);
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
		List<Annotation> documentTokenAnnList = GATEutils.getAnnInDocOrder(this.document, this.tokenAnnSet, this.tokenType);

		// Start processing token level features: create Token ID -> Sentence ID map
		Map<Integer, Integer> tokenIDtoSentenceIDAPPOmap = new HashMap<Integer, Integer>();
		List<Pair<Integer, Annotation>> sentenceIdsOrdered = new ArrayList<Pair<Integer, Annotation>>();
		LinkedHashMap<Integer, List<Pair<Integer, Annotation>>> sentenceIdToTokenListMap = new LinkedHashMap<Integer, List<Pair<Integer, Annotation>>>();
		for(Annotation documentTokenAnn : documentTokenAnnList) {

			// Set Sentence ID
			List<Annotation> sentenceAnnList = GATEutils.getAnnInDocOrderIntersectAnn(this.document, this.sentenceAnnSet, this.sentenceType, documentTokenAnn);
			if(sentenceAnnList != null && sentenceAnnList.size() > 0) {
				tokenIDtoSentenceIDAPPOmap.put(documentTokenAnn.getId(), sentenceAnnList.get(0).getId());

				if(sentenceIdToTokenListMap != null && !sentenceIdToTokenListMap.containsKey(sentenceAnnList.get(0).getId())) {
					sentenceIdToTokenListMap.put(sentenceAnnList.get(0).getId(), new ArrayList<Pair<Integer, Annotation>>());
					sentenceIdsOrdered.add(new Pair<Integer, Annotation>(sentenceAnnList.get(0).getId(), sentenceAnnList.get(0)));
				}

				sentenceIdToTokenListMap.get(sentenceAnnList.get(0).getId()).add(new Pair<Integer, Annotation>(documentTokenAnn.getId(), documentTokenAnn));

				if(sentenceAnnList.size() > 1) {
					logger.warn("Multiple sentence id for token!");
				}
			}
			else {
				logger.warn("No sentence id for token! Generating a random one");
				tokenIDtoSentenceIDAPPOmap.put(documentTokenAnn.getId(), --sentenceIDappo);
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
		Instances filteredARFF = null;
		try {
			synchronized(multiFilterSynch) {
				filteredARFF = Filter.useFilter(ARFFinstances, multiFilter);
			}
			filteredARFF.setClassIndex(filteredARFF.numAttributes() - 1);
		} catch (Exception e) {
			logger.error("ERROR while generating ARFF / CRF suite features.");
			e.printStackTrace();
		}

		// Prepare for SF / SFtype / LF tagging
		Instances filteredARFF_SF = null;
		String CRFsuiteTaggingFile_GLOBAL = null;
		try {
			Remove removeFilter = new Remove();
			removeFilter.setOptions(weka.core.Utils.splitOptions("-R 87-88"));
			removeFilter.setInputFormat(filteredARFF);
			filteredARFF_SF = Filter.useFilter(filteredARFF, removeFilter);
			filteredARFF_SF.setClassIndex(filteredARFF_SF.numAttributes() - 1);

			// Conversion from ARFF
			CRFsuiteTaggingFile_GLOBAL = CRFsuite.fromARFFtoCRFsuite(filteredARFF_SF, "SENT_ID", true, null);
		} catch (Exception e) {
			logger.error("ERROR while generating ARFF / CRF suite features.");
			e.printStackTrace();
		}

		// Here I have in the CRFsuiteTaggingFile_SF the crfsuite file to tag by means of the models


		// ***************************************************************
		// ***************************************************************
		// ***** Abbreviations spotting (SF) *****************************

		// Apply sequence tagger model to identify SF
		List<List<Pair<String, Double>>> taggedSentences_SF = new ArrayList<List<Pair<String, Double>>>();

		Pair<List<ItemSequence>, List<StringList>> taggingSequences_SF = readItemSequencesToTagFromString(CRFsuiteTaggingFile_GLOBAL);
		synchronized(crfTagger_SFSynch) {
			for (ItemSequence xseq: taggingSequences_SF.getFirst()) {
				taggedSentences_SF.add(crfTagger_SF.tag(xseq));
			}
		}

		// Report annotations back to original document
		long startOffsetBegin_SF = -1l;
		long startOffsetEnd_SF = -1l;
		String previousToken_Label_SF = "_UNDEFINED_";
		String previousToken_Probability_SF = "_UNDEFINED_";
		for(int sentenceIdx_SF = 0; sentenceIdx_SF < taggedSentences_SF.size(); sentenceIdx_SF++) {
			// Original annotation IDs
			Pair<Integer, Annotation> GATEsentenceID = sentenceIdsOrdered.get(sentenceIdx_SF);
			List<Pair<Integer, Annotation>> tokenIDsOfSentence = (GATEsentenceID != null && GATEsentenceID.first != null) ? sentenceIdToTokenListMap.get(GATEsentenceID.first) : new ArrayList<Pair<Integer, Annotation>>();
			List<Pair<String, Double>> taggedSentenceTokens = taggedSentences_SF.get(sentenceIdx_SF);

			if(tokenIDsOfSentence.size() != taggedSentenceTokens.size()) {
				System.out.println("ERROR: Not matching tagged token sequence token num. with sentence token num. "
						+ " (sentence ID: " + ((GATEsentenceID != null && GATEsentenceID.first != null) ? GATEsentenceID.first : "NULL") + ") - "
						+ " (sentence text: " + GATEutils.getAnnotationText(GATEsentenceID.second, this.document).orElse("NO TEXT") + ") - "
						+ " (sentence token num.: " + ((tokenIDsOfSentence != null) ? tokenIDsOfSentence.size() + "" : "NULL") + ") - "
						+ " (tagged sequence token num.: " + ((taggedSentenceTokens != null) ? taggedSentenceTokens.size() + "" : "NULL") + ") - ");
			}
			else {
				if(taggedSentenceTokens != null) {
					System.out.println("SF - Sentence number: " + sentenceIdx_SF + " with " + tokenIDsOfSentence.size() + " tokens and " + taggedSentenceTokens.size() + " CRFsuite labels.");
					for(int tokenIdx = 0; tokenIdx < taggedSentenceTokens.size(); tokenIdx++) {
						Pair<String, Double> taggedToken = taggedSentenceTokens.get(tokenIdx);
						Pair<Integer, Annotation> tokenAnnPair = tokenIDsOfSentence.get(tokenIdx);
						if(taggedToken != null && tokenAnnPair != null) {
							Annotation tokenAnn = tokenAnnPair.second;

							String taggedToken_Label = taggedToken.first != null ? taggedToken.first : "_UNDEFINED_";
							String taggedToken_Probability = taggedToken.second != null ? taggedToken.second + "" : "_UNDEFINED_";

							// Adding GATE token annotation label and probability features
							// System.out.println("   > " + tokenIdx + " > " + taggedToken.getFirst() + " > " + taggedToken.getSecond() + " > GATE TOKEN: " + GATEutils.getAnnotationText(tokenAnn, this.document).orElse("NO TEXT"));

							tokenAnn.setFeatures((tokenAnn.getFeatures() != null) ? tokenAnn.getFeatures() : Factory.newFeatureMap());
							tokenAnn.getFeatures().put("CRFsuite_SF_label", taggedToken_Label);
							tokenAnn.getFeatures().put("CRFsuite_SF_prob", taggedToken_Probability);

							if(taggedToken_Label.trim().endsWith("B")) {
								if(startOffsetBegin_SF == -1) startOffsetBegin_SF = tokenAnn.getStartNode().getOffset();
								startOffsetEnd_SF = tokenAnn.getEndNode().getOffset();
							}
							else if(taggedToken_Label.trim().endsWith("I")) {
								if(startOffsetBegin_SF == -1) startOffsetBegin_SF = tokenAnn.getStartNode().getOffset();
								startOffsetEnd_SF = tokenAnn.getEndNode().getOffset();
							}
							else if(taggedToken_Label.trim().endsWith("O")) {
								// Create annotation
								if(startOffsetBegin_SF != -1 && startOffsetEnd_SF != -1) {
									try {
										FeatureMap fm = gate.Factory.newFeatureMap();
										fm.put("CRFsuite_SF_label", previousToken_Label_SF);
										fm.put("CRFsuite_SF_prob", previousToken_Probability_SF);
										this.document.getAnnotations(mainAnnSet).add(startOffsetBegin_SF, startOffsetEnd_SF, abbreviationType, fm);
									} catch (InvalidOffsetException e) {
										logger.error("Error while creating annotation from " + startOffsetBegin_SF + ", to " + startOffsetEnd_SF + " type " + abbreviationType);
										e.printStackTrace();
									}
								}

								startOffsetBegin_SF = -1l;
								startOffsetEnd_SF = -1l;
							}
							else if(startOffsetBegin_SF == -1 && startOffsetEnd_SF != -1) {
								startOffsetBegin_SF = -1l;
								startOffsetEnd_SF = -1l;
							}

							previousToken_Label_SF = taggedToken_Label;
							previousToken_Probability_SF = taggedToken_Probability;

						}
						else {
							System.out.println("ERROR: Impossible to retrieve token label of token number " + tokenIdx + " of sentence number: " + sentenceIdx_SF
									+ " (sentence ID: " + ((GATEsentenceID != null && GATEsentenceID.first != null) ? GATEsentenceID.first : "NULL") + ") - "
									+ " (sentence text: " + GATEutils.getAnnotationText(GATEsentenceID.second, this.document).orElse("NO TEXT") + ") - "
									+ " (sentence token num.: " + ((tokenIDsOfSentence != null) ? tokenIDsOfSentence.size() + "" : "NULL") + ") - "
									+ " (tagged sequence token num.: " + ((taggedSentenceTokens != null) ? taggedSentenceTokens.size() + "" : "NULL") + ") - ");
						}
					}
				}
			}
		}


		// ***************************************************************
		// ***************************************************************
		// ***** Abbreviations spotting (SF type) ************************

		// Apply sequence tagger model to identify SF
		List<List<Pair<String, Double>>> taggedSentences_SFtype = new ArrayList<List<Pair<String, Double>>>();

		Pair<List<ItemSequence>, List<StringList>> taggingSequences_SFtype = readItemSequencesToTagFromString(CRFsuiteTaggingFile_GLOBAL);
		synchronized(crfTagger_SFtypeSynch) {
			for (ItemSequence xseq: taggingSequences_SFtype.getFirst()) {
				taggedSentences_SFtype.add(crfTagger_SFtype.tag(xseq));
			}
		}

		// Report annotations back to original document
		long startOffsetBegin_SFtype = -1l;
		long startOffsetEnd_SFtype = -1l;
		String previousToken_Label_SFtype = "_UNDEFINED_";
		String previousToken_Probability_SFtype = "_UNDEFINED_";
		String currentType = null;
		for(int sentenceIdx_SFtype = 0; sentenceIdx_SFtype < taggedSentences_SFtype.size(); sentenceIdx_SFtype++) {
			// Original annotation IDs
			Pair<Integer, Annotation> GATEsentenceID = sentenceIdsOrdered.get(sentenceIdx_SFtype);
			List<Pair<Integer, Annotation>> tokenIDsOfSentence = (GATEsentenceID != null && GATEsentenceID.first != null) ? sentenceIdToTokenListMap.get(GATEsentenceID.first) : new ArrayList<Pair<Integer, Annotation>>();
			List<Pair<String, Double>> taggedSentenceTokens = taggedSentences_SFtype.get(sentenceIdx_SFtype);

			if(tokenIDsOfSentence.size() != taggedSentenceTokens.size()) {
				System.out.println("ERROR: Not matching tagged token sequence token num. with sentence token num. "
						+ " (sentence ID: " + ((GATEsentenceID != null && GATEsentenceID.first != null) ? GATEsentenceID.first : "NULL") + ") - "
						+ " (sentence text: " + GATEutils.getAnnotationText(GATEsentenceID.second, this.document).orElse("NO TEXT") + ") - "
						+ " (sentence token num.: " + ((tokenIDsOfSentence != null) ? tokenIDsOfSentence.size() + "" : "NULL") + ") - "
						+ " (tagged sequence token num.: " + ((taggedSentenceTokens != null) ? taggedSentenceTokens.size() + "" : "NULL") + ") - ");
			}
			else {
				if(taggedSentenceTokens != null) {
					System.out.println("SF type - Sentence number: " + sentenceIdx_SFtype + " with " + tokenIDsOfSentence.size() + " tokens and " + taggedSentenceTokens.size() + " CRFsuite labels.");
					for(int tokenIdx = 0; tokenIdx < taggedSentenceTokens.size(); tokenIdx++) {
						Pair<String, Double> taggedToken = taggedSentenceTokens.get(tokenIdx);
						Pair<Integer, Annotation> tokenAnnPair = tokenIDsOfSentence.get(tokenIdx);
						if(taggedToken != null && tokenAnnPair != null) {
							Annotation tokenAnn = tokenAnnPair.second;

							String taggedToken_Label = taggedToken.first != null ? taggedToken.first : "_UNDEFINED_";
							String taggedToken_Probability = taggedToken.second != null ? taggedToken.second + "" : "_UNDEFINED_";

							// Adding GATE token annotation label and probability features
							// System.out.println("   > " + tokenIdx + " > " + taggedToken.getFirst() + " > " + taggedToken.getSecond() + " > GATE TOKEN: " + GATEutils.getAnnotationText(tokenAnn, this.document).orElse("NO TEXT"));

							tokenAnn.setFeatures((tokenAnn.getFeatures() != null) ? tokenAnn.getFeatures() : Factory.newFeatureMap());
							tokenAnn.getFeatures().put("CRFsuite_SFtype_label", taggedToken_Label);
							tokenAnn.getFeatures().put("CRFsuite_SFtype_prob", taggedToken_Probability);

							// DERIVED,GLOBAL,CONTEXTUAL,NONE,MULTIPLE,SHORT
							if(currentType != null && taggedToken_Label.trim().endsWith(currentType)) {
								// ANNO -> ANNO
								if(startOffsetBegin_SFtype == -1) startOffsetBegin_SFtype = tokenAnn.getStartNode().getOffset();
								startOffsetEnd_SFtype = tokenAnn.getEndNode().getOffset();
							}
							if(currentType != null && !taggedToken_Label.trim().endsWith(currentType)) {
								// ANNO -> DIFFERENT_ANNO or NONE

								// 1) Create annotation
								if(startOffsetBegin_SFtype != -1 && startOffsetEnd_SFtype != -1) {
									String abbrteviationTypeAnno = null;

									try {
										FeatureMap fm = gate.Factory.newFeatureMap();
										fm.put("CRFsuite_SFtype_label", previousToken_Label_SFtype);
										fm.put("CRFsuite_SFtype_prob", previousToken_Probability_SFtype);

										abbrteviationTypeAnno = (abbrteviationTypeAnno == null && currentType.trim().equals("DERIVED")) ? derived_abbrvType : abbrteviationTypeAnno;
										abbrteviationTypeAnno = (abbrteviationTypeAnno == null && currentType.trim().equals("GLOBAL")) ? global_abbrvType : abbrteviationTypeAnno;
										abbrteviationTypeAnno = (abbrteviationTypeAnno == null && currentType.trim().equals("CONTEXTUAL")) ? contextual_abbrvType : abbrteviationTypeAnno;
										abbrteviationTypeAnno = (abbrteviationTypeAnno == null && currentType.trim().equals("MULTIPLE")) ? multiple_abbrvType : abbrteviationTypeAnno;
										abbrteviationTypeAnno = (abbrteviationTypeAnno == null && currentType.trim().equals("SHORT")) ? short_abbrvType : abbrteviationTypeAnno;

										abbrteviationTypeAnno = (abbrteviationTypeAnno == null) ? "-" : abbrteviationTypeAnno;
										this.document.getAnnotations(mainAnnSet).add(startOffsetBegin_SFtype, startOffsetEnd_SFtype, abbrteviationTypeAnno, fm);
									} catch (InvalidOffsetException e) {
										logger.error("Error while creating annotation from " + startOffsetBegin_SFtype + ", to " + startOffsetEnd_SFtype + " type " + abbrteviationTypeAnno);
										e.printStackTrace();
									}
								}

								// 2) If not NONE, get new one
								if(taggedToken_Label.trim().endsWith("NONE")) {
									currentType = null;
									startOffsetBegin_SFtype = -1l;
									startOffsetEnd_SFtype = -1l;
								}
								else {
									currentType = taggedToken_Label.trim().substring(taggedToken_Label.trim().lastIndexOf("=") + 1);
									startOffsetBegin_SFtype = tokenAnn.getStartNode().getOffset();
									startOffsetEnd_SFtype = tokenAnn.getEndNode().getOffset();
								}
							}
							else if(currentType == null && !taggedToken_Label.trim().endsWith("NONE")) {
								// NONE -> ANNO
								currentType = taggedToken_Label.trim().substring(taggedToken_Label.trim().lastIndexOf("=") + 1);
								startOffsetBegin_SFtype = tokenAnn.getStartNode().getOffset();
								startOffsetEnd_SFtype = tokenAnn.getEndNode().getOffset();
							}
							else if(currentType == null && taggedToken_Label.trim().endsWith("NONE")) {
								// NONE -> NONE
								startOffsetBegin_SFtype = -1l;
								startOffsetEnd_SFtype = -1l;
							}
							else if(startOffsetBegin_SFtype == -1 && startOffsetEnd_SFtype != -1) {
								currentType = null;
								startOffsetBegin_SFtype = -1l;
								startOffsetEnd_SFtype = -1l;
							}

							previousToken_Label_SFtype = taggedToken_Label;
							previousToken_Probability_SFtype = taggedToken_Probability;

						}
						else {
							System.out.println("ERROR: Impossible to retrieve token label of token number " + tokenIdx + " of sentence number: " + sentenceIdx_SFtype
									+ " (sentence ID: " + ((GATEsentenceID != null && GATEsentenceID.first != null) ? GATEsentenceID.first : "NULL") + ") - "
									+ " (sentence text: " + GATEutils.getAnnotationText(GATEsentenceID.second, this.document).orElse("NO TEXT") + ") - "
									+ " (sentence token num.: " + ((tokenIDsOfSentence != null) ? tokenIDsOfSentence.size() + "" : "NULL") + ") - "
									+ " (tagged sequence token num.: " + ((taggedSentenceTokens != null) ? taggedSentenceTokens.size() + "" : "NULL") + ") - ");
						}
					}
				}
			}
		}

		// ***************************************************************
		// ***************************************************************
		// ***** Abbreviations spotting (LF) *****************************

		// Apply sequence tagger model to identify SF
		List<List<Pair<String, Double>>> taggedSentences_LF = new ArrayList<List<Pair<String, Double>>>();

		Pair<List<ItemSequence>, List<StringList>> taggingSequences_LF = readItemSequencesToTagFromString(CRFsuiteTaggingFile_GLOBAL);
		synchronized(crfTagger_LFSynch) {
			for (ItemSequence xseq: taggingSequences_LF.getFirst()) {
				taggedSentences_LF.add(crfTagger_LF.tag(xseq));
			}
		}

		// Report annotations back to original document
		long startOffsetBegin_LF = -1l;
		long startOffsetEnd_LF = -1l;
		String previousToken_Label_LF = "_UNDEFINED_";
		String previousToken_Probability_LF = "_UNDEFINED_";
		for(int sentenceIdx_LF = 0; sentenceIdx_LF < taggedSentences_LF.size(); sentenceIdx_LF++) {
			// Original annotation IDs
			Pair<Integer, Annotation> GATEsentenceID = sentenceIdsOrdered.get(sentenceIdx_LF);
			List<Pair<Integer, Annotation>> tokenIDsOfSentence = (GATEsentenceID != null && GATEsentenceID.first != null) ? sentenceIdToTokenListMap.get(GATEsentenceID.first) : new ArrayList<Pair<Integer, Annotation>>();
			List<Pair<String, Double>> taggedSentenceTokens = taggedSentences_LF.get(sentenceIdx_LF);

			if(tokenIDsOfSentence.size() != taggedSentenceTokens.size()) {
				System.out.println("ERROR: Not matching tagged token sequence token num. with sentence token num. "
						+ " (sentence ID: " + ((GATEsentenceID != null && GATEsentenceID.first != null) ? GATEsentenceID.first : "NULL") + ") - "
						+ " (sentence text: " + GATEutils.getAnnotationText(GATEsentenceID.second, this.document).orElse("NO TEXT") + ") - "
						+ " (sentence token num.: " + ((tokenIDsOfSentence != null) ? tokenIDsOfSentence.size() + "" : "NULL") + ") - "
						+ " (tagged sequence token num.: " + ((taggedSentenceTokens != null) ? taggedSentenceTokens.size() + "" : "NULL") + ") - ");
			}
			else {
				if(taggedSentenceTokens != null) {
					System.out.println("LF - Sentence number: " + sentenceIdx_LF + " with " + tokenIDsOfSentence.size() + " tokens and " + taggedSentenceTokens.size() + " CRFsuite labels.");
					for(int tokenIdx = 0; tokenIdx < taggedSentenceTokens.size(); tokenIdx++) {
						Pair<String, Double> taggedToken = taggedSentenceTokens.get(tokenIdx);
						Pair<Integer, Annotation> tokenAnnPair = tokenIDsOfSentence.get(tokenIdx);
						if(taggedToken != null && tokenAnnPair != null) {
							Annotation tokenAnn = tokenAnnPair.second;

							String taggedToken_Label = taggedToken.first != null ? taggedToken.first : "_UNDEFINED_";
							String taggedToken_Probability = taggedToken.second != null ? taggedToken.second + "" : "_UNDEFINED_";

							// Adding GATE token annotation label and probability features
							// System.out.println("   > " + tokenIdx + " > " + taggedToken.getFirst() + " > " + taggedToken.getSecond() + " > GATE TOKEN: " + GATEutils.getAnnotationText(tokenAnn, this.document).orElse("NO TEXT"));

							tokenAnn.setFeatures((tokenAnn.getFeatures() != null) ? tokenAnn.getFeatures() : Factory.newFeatureMap());
							tokenAnn.getFeatures().put("CRFsuite_LF_label", taggedToken_Label);
							tokenAnn.getFeatures().put("CRFsuite_LF_prob", taggedToken_Probability);

							if(taggedToken_Label.trim().endsWith("B")) {
								if(startOffsetBegin_LF == -1) startOffsetBegin_LF = tokenAnn.getStartNode().getOffset();
								startOffsetEnd_LF = tokenAnn.getEndNode().getOffset();
							}
							else if(taggedToken_Label.trim().endsWith("I")) {
								if(startOffsetBegin_LF == -1) startOffsetBegin_LF = tokenAnn.getStartNode().getOffset();
								startOffsetEnd_LF = tokenAnn.getEndNode().getOffset();
							}
							else if(taggedToken_Label.trim().endsWith("O") && startOffsetBegin_LF != -1 && startOffsetEnd_LF != -1) {
								// Create annotation
								if(startOffsetBegin_LF != -1 && startOffsetEnd_LF != -1) {
									try {
										FeatureMap fm = gate.Factory.newFeatureMap();
										fm.put("CRFsuite_LF_label", previousToken_Label_LF);
										fm.put("CRFsuite_LF_prob", previousToken_Probability_LF);
										this.document.getAnnotations(mainAnnSet).add(startOffsetBegin_LF, startOffsetEnd_LF, longFormType, fm);
									} catch (InvalidOffsetException e) {
										logger.error("Error while creating annotation from " + startOffsetBegin_LF + ", to " + startOffsetEnd_LF + " type " + longFormType);
										e.printStackTrace();
									}
								}

								startOffsetBegin_LF = -1l;
								startOffsetEnd_LF = -1l;
							}
							else if(startOffsetBegin_LF == -1 && startOffsetEnd_LF != -1) {
								startOffsetBegin_LF = -1l;
								startOffsetEnd_LF = -1l;
							}

							previousToken_Label_LF = taggedToken_Label;
							previousToken_Probability_LF = taggedToken_Probability;

						}
						else {
							System.out.println("ERROR: Impossible to retrieve token label of token number " + tokenIdx + " of sentence number: " + sentenceIdx_LF
									+ " (sentence ID: " + ((GATEsentenceID != null && GATEsentenceID.first != null) ? GATEsentenceID.first : "NULL") + ") - "
									+ " (sentence text: " + GATEutils.getAnnotationText(GATEsentenceID.second, this.document).orElse("NO TEXT") + ") - "
									+ " (sentence token num.: " + ((tokenIDsOfSentence != null) ? tokenIDsOfSentence.size() + "" : "NULL") + ") - "
									+ " (tagged sequence token num.: " + ((taggedSentenceTokens != null) ? taggedSentenceTokens.size() + "" : "NULL") + ") - ");
						}
					}
				}
			}
		}


		long needed = System.currentTimeMillis() - t1;
		logger.debug("   - End tagging document: " + (((this.document.getName() != null) ? this.document.getName() : "NULL")));
		logger.debug("     in (seconds): " + (needed / 1000));
		logger.debug("********************************************");
	}


	private Pair<List<ItemSequence>, List<StringList>> readItemSequencesToTagFromString(String inputCRFsuiteString) {
		List<ItemSequence> xseqs = new ArrayList<ItemSequence>();
		List<StringList> yseqs = new ArrayList<StringList>();

		ItemSequence xseq = new ItemSequence();
		StringList yseq = new StringList();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(inputCRFsuiteString.getBytes())))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() > 0) {
					String[] fields = line.split("\t");
					// Add label
					yseq.add(fields[0]);
					// Add item which is a list of attributes
					Item item = new Item();
					for (int i = 1; i < fields.length; i++) {
						String field = fields[i];
						String[] colonSplit = field.split(":", 2);
						if (colonSplit.length == 2) {
							try {
								// See if the feature has a scaling value.
								double val = Double.valueOf(colonSplit[1]);
								item.add(new Attribute(colonSplit[0], val));
							} catch (NumberFormatException e) {
								// There was no scaling value.
								item.add(new Attribute(field));
							}
						} else {
							item.add(new Attribute(field));
						}
					}
					xseq.add(item);

				} else {
					xseqs.add(xseq);
					yseqs.add(yseq);
					xseq = new ItemSequence();
					yseq = new StringList();
				}
			}
			if (!xseq.isEmpty()) {
				// Add the last one
				xseqs.add(xseq);
				yseqs.add(yseq);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Pair<List<ItemSequence>, List<StringList>>(xseqs, yseqs);
	}


	private static void initializeAndStoreWekaFilter() throws Exception {

		String bioABminerResourceFolder = PropertyManager.getProperty("resourceFolder.fullPath");
		if(!bioABminerResourceFolder.endsWith(File.separator)) bioABminerResourceFolder += File.separator;

		// Instantiate feature generator classes by passing the BioAB miner resource folder
		// Resource folder can be downloaded at: http://backingdata.org/bioab/BioAB-resources-1.0.tar.gz
		FeatureSet<Document, TokenFeatureGenerationContext> featSet = TokenFeatureGenerator.generateFeatureSet(PropertyManager.getProperty("resourceFolder.fullPath"));

		// Initialize ARFF filter
		try {
			/* LOAD AND STORE */
			Instances ARFFinstancesFilteredCRFsuiteToInitFilter = null;

			multiFilter.setFilters(TokenFeatureGeneratorCRFsuite.getFilterArrayForCRFsuite());

			// Load ARFF
			Instances ARFFinstancesToInitFilter = null;
			try {
				BufferedReader reader_training = new BufferedReader(new FileReader(arffToInitFilter));
				ARFFinstancesToInitFilter = new Instances(reader_training);
				logger.info("\nLoaded ARFF file (arffToInitFilter): " + arffToInitFilter + " ...");
				reader_training.close();
				ARFFinstancesToInitFilter.setClassIndex(ARFFinstancesToInitFilter.numAttributes() - 1);
			}
			catch (Exception e) {
				logger.error("\nError loading ARFF file: " + arffToInitFilter + " ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Apply filter to init it
			try {
				multiFilter.setInputFormat(ARFFinstancesToInitFilter);
				ARFFinstancesFilteredCRFsuiteToInitFilter = Filter.useFilter(ARFFinstancesToInitFilter, multiFilter);
				ARFFinstancesFilteredCRFsuiteToInitFilter.setClassIndex(ARFFinstancesFilteredCRFsuiteToInitFilter.numAttributes() - 1);
			} catch (Exception e) {
				logger.error("\nError loading ARFF filter ---> " + e.getMessage());
				e.printStackTrace();
			}

			// Store filter
			ObjectOutputStream oos = null;
			FileOutputStream fout = null;
			try{
				fout = new FileOutputStream("/home/ronzano/Desktop/Hackathon_PLN/MultiFilterSerialized.ser", true);
				oos = new ObjectOutputStream(fout);
				oos.writeObject(multiFilter);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if(oos != null){
					oos.close();
				} 
			}

		} catch (Exception e) {
			logger.error("\nError loading Weka filter ---> " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean resetAnnotations() {
		this.document.removeAnnotationSet(mainAnnSet);
		return true;
	}

	public static void main(String[] args) {

	}


}
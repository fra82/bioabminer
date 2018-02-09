package es.imim.ibi.bioab.nlp.mate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.backingdata.gateutils.GATEfiles;
import org.backingdata.gateutils.GATEinit;
import org.backingdata.gateutils.GATEutils;
import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.gateutils.generic.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import es.imim.ibi.bioab.nlp.freeling.FreelingParser;
import es.imim.ibi.bioab.nlp.freeling.FreelingParserUtilities;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.util.GateException;
import gate.util.OffsetComparator;
import se.lth.cs.srl.Parse;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.pipeline.Reranker;
import se.lth.cs.srl.pipeline.Step;
import se.lth.cs.srl.preprocessor.Preprocessor;


/**
 * Mate-tools parser (pos tagger, lemmatizer, dep parser and semantic role labeller)
 * REF: https://code.google.com/p/mate-tools/
 * 
 * Receives as input a tokenized and sentence split text. By specifying the annotation set and type of sentences and tokens, parse each
 * sentence by means of Mate and add to each token annotation the parsing results as features.
 * 
 * To parse Spanish / English texts you need to locally download the MATE models for Spanish / English available in the NLP-utils resource folder
 * Resource folder can be downloaded at: http://backingdata.org/nlputils/NLPutils-resources-1.0.tar.gz 
 * To access information / description of the NLP-utils library: http://nlp-utils.readthedocs.io/en/latest/)
 * 
 * MULTIPLE INSTANCES:
 * No duplicated resources in case of multiple instantiations of the class - optimized multiple instances.
 * Instances synchronized on single resources (parser and semantic rule labeler)
 * 
 * THREAD SAFETY OF EACH INSTANCE:
 * NOT thread safe (can't be used by two or more threads in parallel)
 * 
 * @author Francesco Ronzano
 */
@CreoleResource(name = "MATE Parser")
public class MateParser extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(MateParser.class);

	public static AtomicDouble totSecondsProcessing = new AtomicDouble(0d);
	public AtomicDouble localSecondsProcessing = new AtomicDouble(0d);

	public static final String posFeat = "category";
	public static final String lemmaFeat = "lemma";
	public static final String gateIdFeat = "gateId";
	public static final String depKindFeat = "depFunct";
	public static final String depTargetIdFeat = "depTargetId";
	public static final String depInternalIdFeat = "depInternalId";
	public static final String SRLpartTagNFeat = "srlP_tag_";
	public static final String SRLpartSenseNFeat = "srlP_sense_";
	public static final String SRLpartRoodIdNFeat = "srlP_root_";
	public static final String SRLrootSenseFeat = "srlR_sense";

	private static Preprocessor pp = null;
	private static Object ppSyncObj = new Object();
	private static SemanticRoleLabeler srl = null;
	private static Object srlSyncObj = new Object();

	// URLs of the model for lemmatizer, POS tagger, morphological analyzer and tagger
	private String lemmaModelPath = null;
	private String postaggerModelPath = null;
	private String parserModelPath = null;
	private String srlModelPath = null;

	// Input set for annotation
	private String sentenceAnnotationSetToAnalyze = FreelingParser.mainAnnSet;
	private String sentenceAnnotationTypeToAnalyze = FreelingParser.sentenceType;
	private String tokenAnnotationSetToAnalyze = FreelingParser.mainAnnSet;
	private String tokenAnnotationTypeToAnalyze = FreelingParser.tokenType;
	private Integer excludeThreshold;


	public String getSentenceAnnotationSetToAnalyze() {
		return sentenceAnnotationSetToAnalyze;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.mainAnnSet, comment = "Set name of the annotation set where the sentences to parse are annotated")
	public void setSentenceAnnotationSetToAnalyze(
			String sentenceAnnotationSetToAnalyze) {
		this.sentenceAnnotationSetToAnalyze = sentenceAnnotationSetToAnalyze;
	}

	public String getSentenceAnnotationTypeToAnalyze() {
		return sentenceAnnotationTypeToAnalyze;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.sentenceType, comment = "The type of sentence annotations")
	public void setSentenceAnnotationTypeToAnalyze(
			String sentenceAnnotationTypeToAnalyze) {
		this.sentenceAnnotationTypeToAnalyze = sentenceAnnotationTypeToAnalyze;
	}

	public String getTokenAnnotationSetToAnalyze() {
		return tokenAnnotationSetToAnalyze;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.mainAnnSet, comment = "Set name of the annotation set where the token of the sentences to parse are annotated")
	public void setTokenAnnotationSetToAnalyze(String tokenAnnotationSetToAnalyze) {
		this.tokenAnnotationSetToAnalyze = tokenAnnotationSetToAnalyze;
	}

	public String getTokenAnnotationTypeToAnalyze() {
		return tokenAnnotationTypeToAnalyze;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.tokenType, comment = "The type of token annotations")
	public void setTokenAnnotationTypeToAnalyze(String tokenAnnotationTypeToAnalyze) {
		this.tokenAnnotationTypeToAnalyze = tokenAnnotationTypeToAnalyze;
	}

	public Integer getExcludeThreshold() {
		return this.excludeThreshold;
	}

	@RunTime
	@CreoleParameter(defaultValue = "0", comment = "The value of exclude threshold of the parser.")
	public void setExcludeThreshold(Integer excludeThreshold) {
		this.excludeThreshold = excludeThreshold;
	}

	public String getLemmaModelPath() {
		return lemmaModelPath;
	}

	@CreoleParameter(comment = "Full path to the lemmatizer model.")
	public void setLemmaModelPath(String lemmaModelPath) {
		this.lemmaModelPath = lemmaModelPath;
	}

	public String getPostaggerModelPath() {
		return postaggerModelPath;
	}

	@CreoleParameter(comment = "Full path to the POS tagger model.")
	public void setPostaggerModelPath(String postaggerModelPath) {
		this.postaggerModelPath = postaggerModelPath;
	}

	public String getParserModelPath() {
		return parserModelPath;
	}

	@CreoleParameter(comment = "Full path to the dep parser model.")
	public void setParserModelPath(String parserModelPath) {
		this.parserModelPath = parserModelPath;
	}

	public String getSrlModelPath() {
		return srlModelPath;
	}

	@CreoleParameter(comment = "Full path to the semantic role labeller model.")
	public void setSrlModelPath(String srlModelPath) {
		this.srlModelPath = srlModelPath;
	}

	private static boolean isInitialized = false;

	@Override
	public Resource init() {
		try {
			List<String> argumentList = new ArrayList<String>();
			argumentList.add("eng");

			if(lemmaModelPath != null && (new File(lemmaModelPath)).exists() && (new File(lemmaModelPath)).isFile()) {
				argumentList.add("-lemma");
				argumentList.add(lemmaModelPath);
			}
			else {
				logger.warn("Lemmatizer model file not provided or invalid");
			}

			if(postaggerModelPath != null && (new File(postaggerModelPath)).exists() && (new File(postaggerModelPath)).isFile()) {
				argumentList.add("-tagger");
				argumentList.add(postaggerModelPath);
			}
			else {
				logger.warn("POStagger model file not provided or invalid");
			}

			if(parserModelPath != null && (new File(parserModelPath)).exists() && (new File(parserModelPath)).isFile()) {
				argumentList.add("-parser");
				argumentList.add(parserModelPath);
			}
			else {
				logger.warn("Dep parser model file not provided or invalid");
			}

			if(srlModelPath != null && (new File(srlModelPath)).exists() && (new File(srlModelPath)).isFile()) {
				argumentList.add("-srl");
				argumentList.add(srlModelPath);
			}
			else {
				logger.warn("SRL model file not provided or invalid");
			}

			// Set options
			String[] arguments = argumentList.toArray(new String[argumentList.size()]);

			CompletePipelineCMDLineOptions options = new CompletePipelineCMDLineOptions();
			options.parseCmdLineArgs(arguments);

			if(pp == null) {
				pp = Language.getLanguage().getPreprocessor(options);
			}

			Parse.parseOptions = options.getParseOptions();

			if(srl == null) {
				if (options.reranker) {
					srl = new Reranker(Parse.parseOptions);
				} else {
					ZipFile zipFile = new ZipFile(Parse.parseOptions.modelFile);
					if (Parse.parseOptions.skipPI) {
						srl = Pipeline.fromZipFile(zipFile, new Step[] { Step.pd, Step.ai, Step.ac });
					} else {
						srl = Pipeline.fromZipFile(zipFile);
					}
					zipFile.close();
				}
			}

			isInitialized = true;

		} catch (Exception e) {
			GenericUtil.notifyException("Initializing Mate-tools", e, logger);
		}

		return this;
	}

	@Override
	public void execute() {

		localSecondsProcessing.set(0d);

		if(!isInitialized) {
			this.init();
		}

		int parsedSentences = 0;

		long t1 = System.currentTimeMillis();

		Document gateDoc = this.document;

		// Reference to the current document to parse
		logger.debug("   - Start parsing document: " + ((gateDoc.getName() != null && gateDoc.getName().length() > 0) ? gateDoc.getName() : "NO_NAME") );

		int threshold = getExcludeThreshold();

		// Check input parameters
		sentenceAnnotationSetToAnalyze = StringUtils.defaultString(sentenceAnnotationSetToAnalyze, FreelingParser.mainAnnSet);
		sentenceAnnotationTypeToAnalyze = StringUtils.defaultString(sentenceAnnotationTypeToAnalyze, FreelingParser.sentenceType);
		tokenAnnotationSetToAnalyze = StringUtils.defaultString(tokenAnnotationSetToAnalyze, FreelingParser.mainAnnSet);
		tokenAnnotationTypeToAnalyze = StringUtils.defaultString(tokenAnnotationTypeToAnalyze, FreelingParser.tokenType);

		// Get all the sentence annotations (sentenceAnnotationSet) from the input annotation set (inputAnnotationSet)
		AnnotationSet inputAnnotationSet = gateDoc.getAnnotations(sentenceAnnotationSetToAnalyze);
		AnnotationSet sentenceAnnotationSet = inputAnnotationSet.get(sentenceAnnotationTypeToAnalyze);

		// Sort sentences
		List<Annotation> sentencesSorted = sortSetenceList(sentenceAnnotationSet);

		parsedSentences += annotateSentences(sentencesSorted, gateDoc, threshold);

		long needed = System.currentTimeMillis() - t1;
		logger.debug("   - End parsing document: " + gateDoc.getName());
		logger.debug("     in (seconds): " + (needed / 1000) + ", parsed: " + parsedSentences + ", unparsed: " + (sentencesSorted.size() - parsedSentences) );
		logger.debug("********************************************");
	}


	/**
	 * Annotate by means of the parser a set of sentences 
	 * 
	 * @param sentencesSorted list of sentences to annotate
	 * @param doc document the sentences belong to
	 * @param t threshold for the parser
	 * @return
	 */
	public int annotateSentences(List<Annotation> sentencesSorted, Document doc, int t) {

		int parsedSentences = 0;

		// Parse each sentence
		for (Annotation actualSentence : sentencesSorted) {

			try {

				// References to the document (actualDoc) and the sentence (actualSentence) to parse
				Document actualDoc = doc;

				// Generated sorted token list
				List<Annotation> sortedTokens = GATEutils.getAnnInDocOrderContainedAnn(actualDoc, tokenAnnotationSetToAnalyze, tokenAnnotationTypeToAnalyze, actualSentence);

				if(sortedTokens == null || sortedTokens.size() < 1) {
					continue;
				}

				List<String> tokensToProcess = new ArrayList<String>();
				tokensToProcess.add("<root>");
				for (int i = 0; i < sortedTokens.size(); i++) {
					tokensToProcess.add(String.valueOf(sortedTokens.get(i).getFeatures().get("formString")));
				}

				// **********************************************
				// PROCESS:
				Sentence s = null;

				if(tokensToProcess.size() > 0 && tokensToProcess.size() <= t) {

					String sentenceToParse = null;
					sentenceToParse = tokensToProcess.stream().collect(Collectors.joining(" "));
					logger.debug("Parsing sentence: " + sentenceToParse);

					sentenceToParse = sentenceToParse.replace("\n", " ").trim();

					if(StringUtils.isNotBlank(sentenceToParse)) {
						synchronized(ppSyncObj) {
							long startProc = System.currentTimeMillis();
							s = new Sentence(pp.preprocess(tokensToProcess.toArray(new String[tokensToProcess.size()])));
							totSecondsProcessing.addAndGet(((double) (System.currentTimeMillis() - startProc) / 1000d));
							localSecondsProcessing.addAndGet(((double) (System.currentTimeMillis() - startProc) / 1000d));
						}
						synchronized(srlSyncObj) {
							long startProc = System.currentTimeMillis();
							srl.parseSentence(s);
							totSecondsProcessing.addAndGet(((double) (System.currentTimeMillis() - startProc) / 1000d));
							localSecondsProcessing.addAndGet(((double) (System.currentTimeMillis() - startProc) / 1000d));
						}
						parsedSentences++;
					}
				}
				else {
					Optional<String> annText = GATEutils.getAnnotationText(actualSentence, actualDoc);
					logger.debug("Impossible to parse the sentence " + ((tokensToProcess.size() > t) ? "(token size " + tokensToProcess.size() + " greater than threshold t " + t + ")" : "" ) + ": " + annText.orElse("NOT_PRESENT"));
				}

				Annotation token = null;
				FeatureMap fm = null;
				for(int w = 0; w < sortedTokens.size(); w++) {
					token = sortedTokens.get(w);
					fm = token.getFeatures();

					if(s != null) {
						Word word = s.get(w+1);

						// The following two annotations are internal to the parser
						// fm.put("seq", w);
						fm.put(gateIdFeat, token.getId());

						// WORD FEATS
						fm.put(posFeat, StringUtils.defaultString(word.getPOS(), ""));
						fm.put(lemmaFeat, StringUtils.defaultString(word.getLemma(), ""));

						// DEP PARSER
						fm.put(depInternalIdFeat, word.getHeadId());

						String depRel = word.getDeprel();
						if(depRel != null && depRel.equals("sentence")) {
							depRel = "ROOT"; // sentence is the root dep rel in Spanish parsing
						}
						fm.put(depKindFeat, StringUtils.defaultString(depRel, ""));
						if(word.getHeadId() > 0) {
							fm.put(depTargetIdFeat, sortedTokens.get(word.getHeadId() - 1).getId());
						}

						// SRL
						List<Predicate> predicates = s.getPredicates();
						for (int j = 0; j < predicates.size(); ++j) {
							Predicate pred = predicates.get(j);
							String tag = pred.getArgumentTag(word);
							if (StringUtils.isNotBlank(tag)) {
								Integer SRLid = 1;
								while(fm.containsKey("srlA_tag_" + SRLid)) {
									SRLid++;
									if(SRLid > 30) break;
								}
								fm.put(SRLpartTagNFeat + SRLid, StringUtils.defaultString(tag, ""));
								fm.put(SRLpartRoodIdNFeat + SRLid, sortedTokens.get(pred.getIdx() - 1).getId());
								fm.put(SRLpartSenseNFeat + SRLid, StringUtils.defaultString(pred.getSense(), ""));
							}
							else if(pred.getIdx() == w+1) {
								fm.put(SRLrootSenseFeat, StringUtils.defaultString(pred.getSense(), ""));
							}
						}
					}
				}

			} catch (Exception e) {
				GenericUtil.notifyException("Error parsing sentence: " + ((actualSentence != null) ? actualSentence.toString() : "NULL"), e, logger);
			}

		}

		return parsedSentences;
	}


	/**
	 * Given a {@link AnnotationSet} instance, returns a sorted list of its elements.
	 * Sorting is done by position (offset) in the document.
	 * 
	 * @param sentences {@link Annotation}
	 * 
	 * @return Sorted list of {@link Annotation} instances.
	 */
	public List<Annotation> sortSetenceList(AnnotationSet sentences) {
		List<Annotation> sentencesSorted = new ArrayList<Annotation>(sentences);
		Collections.sort(sentencesSorted, new OffsetComparator());
		return sentencesSorted;
	}


	// ************************************************************************************************
	// ************************************************************************************************
	// ************************************************************************************************

	// inputOurputTestDir: base path to read input file to parse from and to write paresed file to
	private static String inputOurputTestDir = "/home/francesco/Downloads/SEPLN/EXAMPLE_COLLECTION_v3/pdln54/";
	private static String urlGATEannotatedDocument = "file://" + inputOurputTestDir + "pdln54__RomanMGZ15/pdln54__RomanMGZ15_DRI_v1.xml"; 
	private static String urlGATEoutputDocument = inputOurputTestDir + "Apdln54__RomanMGZ15/pdln54__RomanMGZ15_DRI_v1" + "_DEPPARSED.xml";

	public static void mainOLD(String[] args) {

		// Set URL document
		URL urlDocument = null;
		try {
			urlDocument = new URL(args[0]);
		} catch (MalformedURLException murle) {
			logger.error(murle.getMessage());
			// murle.printStackTrace();
		} catch (Exception e) {
			logger.error(e.getMessage());
			// e.printStackTrace();
		}
		if(urlDocument == null) {
			try {
				urlDocument = new URL(urlGATEannotatedDocument);
			} catch (MalformedURLException murle) {
				logger.error(murle.getMessage());
			}
		} 

		// Initialize and execute parser
		try {
			Gate.setGateHome(new File("/home/francesco/Desktop/DRILIB_EXP/DRIresources-3.4/gate_home"));
			Gate.setPluginsHome(new File("/home/francesco/Desktop/DRILIB_EXP/DRIresources-3.4/gate_home/plugins"));
			Gate.setSiteConfigFile(new File("/home/francesco/Desktop/DRILIB_EXP/DRIresources-3.4/gate_home/gate_uc.xml"));
			Gate.setUserConfigFile(new File("/home/francesco/Desktop/DRILIB_EXP/DRIresources-3.4/gate_home/gate_uc.xml"));


			Gate.init();

			logger.debug("Loading document to parse: " + urlDocument);
			Document doc = Factory.newDocument(urlDocument);
			logger.debug("Loaded document: " + doc.getContent().toString());

			logger.debug("\n-----------------------------------------------\n");


			// Instantiate parser
			MateParser parser = new MateParser();

			// Set parser parameters

			String baseModelPath = "/home/francesco/Downloads/TRANSITION_PARSER/";
			parser.setLemmaModelPath(baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model");
			parser.setPostaggerModelPath(baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.postagger.model");
			parser.setParserModelPath(baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.parser.model");
			parser.setSrlModelPath(baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.srl-4.1.srl.model");

			parser.setExcludeThreshold(70);

			// Initialize parser
			parser.init();

			// Set document and start parsing
			parser.setDocument(doc);
			parser.execute();

			// Storing parsed document
			try {
				OutputStreamWriter os_pw = null;
				PrintWriter pw = null;
				try {
					File outFile = new File(urlGATEoutputDocument);
					if(!outFile.exists()) {
						outFile.createNewFile();
					}
					os_pw = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8);
					pw = new PrintWriter(os_pw, true);
				} catch (FileNotFoundException e1) {
					logger.error("Error opening output file --> " + e1.getMessage() );
					e1.printStackTrace();
				}

				pw.print(doc.toXml());
				pw.flush();
				pw.close();
			} catch (Exception e) {
				logger.error("ERROR, IMPOSSIBLE TO SAVE: " + doc.getName());
				e.printStackTrace();
			}

		} catch(GateException ge) {
			logger.error("ERROR (GateException): while executing parser " + ge.getMessage());
			ge.printStackTrace();
		}
	}

	public boolean resetAnnotations() {
		return resetAnnotationsTS(this.document);
	}

	public boolean resetAnnotationsTS(Document gateDoc) {

		// Check input parameters
		sentenceAnnotationSetToAnalyze = StringUtils.defaultString(sentenceAnnotationSetToAnalyze, FreelingParser.mainAnnSet);
		sentenceAnnotationTypeToAnalyze = StringUtils.defaultString(sentenceAnnotationTypeToAnalyze, FreelingParser.sentenceType);
		tokenAnnotationSetToAnalyze = StringUtils.defaultString(tokenAnnotationSetToAnalyze, FreelingParser.mainAnnSet);
		tokenAnnotationTypeToAnalyze = StringUtils.defaultString(tokenAnnotationTypeToAnalyze, FreelingParser.tokenType);

		// Remove from all token of sentences the annotation features added by the parser
		List<Annotation> sentenceList = GATEutils.getAnnInDocOrder(gateDoc, sentenceAnnotationSetToAnalyze, sentenceAnnotationTypeToAnalyze);
		if(sentenceList != null && sentenceList.size() > 0) {
			for(Annotation sentence : sentenceList) {
				List<Annotation> tokenList = GATEutils.getAnnInDocOrderContainedAnn(gateDoc, tokenAnnotationSetToAnalyze, tokenAnnotationTypeToAnalyze, sentence);

				if(tokenList != null && tokenList.size() > 0) {
					for(Annotation token : tokenList) {
						// Remove features: gateId, category, lemma, depSentInternal, func, dependency,
						// srlA_tag, srlA_root, srlA_sense, srlR_tag
						if(token != null && token.getFeatures() != null) {
							token.getFeatures().remove(gateIdFeat);
							token.getFeatures().remove(posFeat);
							token.getFeatures().remove(lemmaFeat);
							token.getFeatures().remove(depInternalIdFeat);
							token.getFeatures().remove(depKindFeat);
							token.getFeatures().remove(depTargetIdFeat);

							Integer SRLid = 1;
							while(true) {
								if(token.getFeatures().containsKey(SRLpartTagNFeat + SRLid)) {
									token.getFeatures().remove(SRLpartTagNFeat + SRLid);
									token.getFeatures().remove(SRLpartRoodIdNFeat + SRLid);
									token.getFeatures().remove(SRLpartSenseNFeat + SRLid);
								}
								else {
									break;
								}
								SRLid++;
							}
							token.getFeatures().remove(SRLrootSenseFeat);
						}
					}
				}

			}
		}

		return true;
	}

	/**
	 * Example of usage of the MateParser to parse a Spanish and an English text
	 * 
	 * To execute these examples you need to locally download the MATE models for Spanish / English available in the NLP-utils resource folder
	 * Resource folder can be downloaded at: http://backingdata.org/nlputils/NLPutils-resources-1.0.tar.gz 
	 * To access information / description of the NLP-utils library: http://nlp-utils.readthedocs.io/en/latest/)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Set the full path to the BioAB Miner property file
		PropertyManager.setPropertyFilePath("/full/path/to/BioAbMinerConfig.properties");

		String strES = "El chico se ha ido de Barcelona. No volverá hasta el próximo verano.";

		try {
			GATEinit.initGate(PropertyManager.getProperty("gate.home"), PropertyManager.getProperty("gate.plugins"));
			Gate.getCreoleRegister().registerComponent(FreelingParser.class);
			Gate.getCreoleRegister().registerComponent(MateParser.class);

			// Parse the text by Freeling so as to have:
			// - sentence annotation of type FreelingParser.sentenceType in the annotation set FreelingParser.mainAnnSet
			// - token annotation of type FreelingParser.tokenType in the annotation set FreelingParser.mainAnnSet
			Document gateDoc = FreelingParserUtilities.parseText(strES, "SPA");

			// Instantiate MATE parser
			FeatureMap MateParserfm = Factory.newFeatureMap();
			// Specify the annotation set and type for sentences and tokens identified by Freeling
			MateParserfm.put("sentenceAnnotationSetToAnalyze", FreelingParser.mainAnnSet);
			MateParserfm.put("sentenceAnnotationTypeToAnalyze", FreelingParser.sentenceType);
			MateParserfm.put("tokenAnnotationSetToAnalyze", FreelingParser.mainAnnSet);
			MateParserfm.put("tokenAnnotationTypeToAnalyze", FreelingParser.tokenType);
			
			// Parse sentences no longler than 120 tokens
			MateParserfm.put("excludeThreshold", 120);

			// Set the path of the MATE models for Spanish available in the NLP-utils resource folder
			// Resource folder can be downloaded at: http://backingdata.org/nlputils/NLPutils-resources-1.0.tar.gz 
			// To access information / description of the NLP-utils library: http://nlp-utils.readthedocs.io/en/latest/)
			String NLPutilsResourceFolder = "/home/ronzano/Downloads/NLPutils-resources-1.0";
			NLPutilsResourceFolder = (NLPutilsResourceFolder.endsWith(File.separator)) ? NLPutilsResourceFolder : NLPutilsResourceFolder + File.separator;
			String baseModelPath = NLPutilsResourceFolder + "mate_models" + File.separator;
			MateParserfm.put("lemmaModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.lemmatizer.model");
			MateParserfm.put("postaggerModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.postagger.model");
			MateParserfm.put("parserModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.parser.model");
			MateParserfm.put("srlModelPath", baseModelPath + "CoNLL2009-ST-Spanish-ALL.anna-3.3.srl-4.21.srl-rr.model");
			
			MateParser MateParser_Resource = (MateParser) gate.Factory.createResource(MateParser.class.getName(), MateParserfm);

			// Set document and start parsing by MATE
			MateParser_Resource.setDocument(gateDoc);
			MateParser_Resource.execute();
			MateParser_Resource.setDocument(null);			
			
			String storageFilePath = "/path/to/store/annotated/text/Freeling_and_Mate_ESstr.xml";
			GATEfiles.storeGateXMLToFile(gateDoc, storageFilePath);

			System.out.println("Stored to '" + storageFilePath + "' the parsed text - original text:\n" + strES);

			System.out.println("EXECUTED");
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
		
		String strEN = "The man abandoned Barcelona. He will not be back before next summer.";

		try {
			GATEinit.initGate(PropertyManager.getProperty("gate.home"), PropertyManager.getProperty("gate.plugins"));
			Gate.getCreoleRegister().registerComponent(FreelingParser.class);
			Gate.getCreoleRegister().registerComponent(MateParser.class);

			// Parse the text by Freeling so as to have:
			// - sentence annotation of type FreelingParser.sentenceType in the annotation set FreelingParser.mainAnnSet
			// - token annotation of type FreelingParser.tokenType in the annotation set FreelingParser.mainAnnSet
			Document gateDoc = FreelingParserUtilities.parseText(strEN, "ENG");

			// Instantiate MATE parser
			FeatureMap MateParserfm = Factory.newFeatureMap();
			// Specify the annotation set and type for sentences and tokens identified by Freeling
			MateParserfm.put("sentenceAnnotationSetToAnalyze", FreelingParser.mainAnnSet);
			MateParserfm.put("sentenceAnnotationTypeToAnalyze", FreelingParser.sentenceType);
			MateParserfm.put("tokenAnnotationSetToAnalyze", FreelingParser.mainAnnSet);
			MateParserfm.put("tokenAnnotationTypeToAnalyze", FreelingParser.tokenType);
			
			// Parse sentences no longler than 120 tokens
			MateParserfm.put("excludeThreshold", 120);

			// Set the path of the MATE models for English available in the NLP-utils resource folder
			// Resource folder can be downloaded at: http://backingdata.org/nlputils/NLPutils-resources-1.0.tar.gz 
			// To access information / description of the NLP-utils library: http://nlp-utils.readthedocs.io/en/latest/)
			String NLPutilsResourceFolder = "/home/ronzano/Downloads/NLPutils-resources-1.0";
			NLPutilsResourceFolder = (NLPutilsResourceFolder.endsWith(File.separator)) ? NLPutilsResourceFolder : NLPutilsResourceFolder + File.separator;
			String baseModelPath = NLPutilsResourceFolder + "mate_models" + File.separator;
			MateParserfm.put("lemmaModelPath", baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model");
			MateParserfm.put("postaggerModelPath", baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.postagger.model");
			MateParserfm.put("parserModelPath", baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.parser.model");
			MateParserfm.put("srlModelPath", baseModelPath + "CoNLL2009-ST-English-ALL.anna-3.3.srl-4.1.srl.model");
			
			MateParser MateParser_Resource = (MateParser) gate.Factory.createResource(MateParser.class.getName(), MateParserfm);

			// Set document and start parsing by MATE
			MateParser_Resource.setDocument(gateDoc);
			MateParser_Resource.execute();
			MateParser_Resource.setDocument(null);
			
			String storageFilePath = "/path/to/store/annotated/text/Freeling_and_Mate_ENstr.xml";
			GATEfiles.storeGateXMLToFile(gateDoc, storageFilePath);

			System.out.println("Stored to '" + storageFilePath + "' the parsed text - original text:\n" + strEN);

			System.out.println("EXECUTED");
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
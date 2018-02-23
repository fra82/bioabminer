package es.imim.ibi.bioab.exec.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.gateutils.generic.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.generator.StringInList;
import es.imim.ibi.bioab.nlp.freeling.FreelingParser;
import es.imim.ibi.bioab.nlp.mate.MateParser;
import gate.Annotation;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;


/**
 * Link the abbreviations with the corresponding Long Form if any init he same sentence (rule-based approach).
 * 
 * @author Francesco Ronzano
 */
@CreoleResource(name = "BioAB Abbreviation Long Form Spotter Module")
public class BioABabbrvLFspotter extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(BioABabbrvLFspotter.class);

	public static final String mainAnnSet = "BioAB";
	public static final String parenthesisType = "Abbreviation_Parenthesis";
	public static final String SEDOMtype = "Abbreviation_fromSEDOM";
	public static final String longFormType = "LONG";
	public static final String shortFormType = "SHORT";

	private static Map<String, Set<String>> abbreviationLFmap = new HashMap<String, Set<String>>();

	// Where to read input textual annotations and features
	private String tokenAnnSet = FreelingParser.mainAnnSet;
	private String tokenType = FreelingParser.tokenType;
	private String tokenLemmaFeat = FreelingParser.tokenType_lemmaFeatName;
	private String tokenPOSFeat = FreelingParser.tokenType_POSFeatName;
	private String tokenDepFunctFeat = MateParser.depKindFeat;
	private String sentenceAnnSet = FreelingParser.mainAnnSet;
	private String sentenceType = FreelingParser.sentenceType;
	private String chunkAnnSet = FreelingParser.mainAnnSet;
	private String chunkType = FreelingParser.sentenceType;
	private String chunkLabelFeat = FreelingParser.sentenceType;

	private static boolean isInitialized = false;
	private static Random rnd = new Random();

	// STATS
	public static Map<Integer, Integer> longFormBySpan = new HashMap<Integer, Integer>();
	public static Integer candidateMatchASingleSpanLongForm = 0;
	public static Integer candidateMatchAMultipleSpanLongForm = 0;
	public static Integer longFormWithAtLeastOneCandidateSpanMatch = 0;
	public static Integer longFormWithoutAnyCandidateSpanMatch = 0;
	public static List<String> longFormWithoutAnyCandidateSpanMatchList = new ArrayList<String>();
	public static Map<String, Integer> longFormMatchByTypeSingle = new HashMap<String, Integer>();
	public static Map<String, Integer> longFormMatchByTypeMulti = new HashMap<String, Integer>();

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

	public String getChunkAnnSet() {
		return chunkAnnSet;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.mainAnnSet, comment = "The annotation set where to read chunks from.")
	public void setChunkAnnSet(String chunkAnnSet) {
		this.chunkAnnSet = chunkAnnSet;
	}

	public String getChunkType() {
		return chunkType;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.chunkType, comment = "The annotation type of chunks.")
	public void setChunkType(String chunkType) {
		this.chunkType = chunkType;
	}

	public String getChunkLabelFeat() {
		return chunkLabelFeat;
	}

	@RunTime
	@CreoleParameter(defaultValue = FreelingParser.chunkType_labelFeatName, comment = "The name of the token features that contains the label of the chunk.")
	public void setChunkLabelFeat(String chunkLabelFeat) {
		this.chunkLabelFeat = chunkLabelFeat;
	}

	@Override
	public Resource init() {

		if(!isInitialized) {

			try {
				String bioABminerResourceFolder = PropertyManager.getProperty("resourceFolder.fullPath");
				if(!bioABminerResourceFolder.endsWith(File.separator)) bioABminerResourceFolder += File.separator;

				// Load list
				StringInList instanceCr = new StringInList();
				BufferedReader br = null;
				try {

					br = new BufferedReader(new InputStreamReader(instanceCr.getClass().getResourceAsStream("/langres/sedom/abbrvList_5_2_2018_EXTENDED.csv"), "UTF-8"));
					String line;
					int lineCounter = -1;
					while ((line = br.readLine()) != null) {
						try {
							lineCounter++;
							if(lineCounter == 0) {
								continue;
							}

							String[] abbreviationLFline = line.split("\t");

							if(abbreviationLFline != null && abbreviationLFline.length == 4) {
								String abbrev = abbreviationLFline[0];
								String longForm = abbreviationLFline[3];

								if(abbrev != null && !abbrev.trim().equals("") && longForm != null && !longForm.trim().equals("")) {

									if(abbreviationLFmap.containsKey(abbrev.trim().toLowerCase())) {
										abbreviationLFmap.get(abbrev.trim().toLowerCase()).add(longForm.trim().toLowerCase());
									}
									else {
										Set<String> longFormSet = new HashSet<String>();
										longFormSet.add(longForm.trim().toLowerCase());
										abbreviationLFmap.put(abbrev.trim().toLowerCase(), longFormSet);
									}
								}
							}


						}
						catch(Exception e) {
							System.out.println("Error while loading SEDOM abbreviation dictionary line (" + lineCounter + "): " + line);
						}
					}

					logger.info("SEDOM: loaded list of length: " + abbreviationLFmap.size() + " abbreviations.");

				} catch (Exception e) {
					logger.error("IMPOSSIBLE TO LOAD LIST OF SEDOM ABBREVIATIONS-LONG FORMS.");
					e.printStackTrace();
				}


				isInitialized = true;

			} catch (Exception e) {
				GenericUtil.notifyException("Initializing BioAB Abbreviation Spotter Module", e, logger);
			}

		}

		return this;
	}

	@Override
	public void execute() {

		if(!isInitialized) {
			this.init();
		}

		long t1 = System.currentTimeMillis();

		// **********************************************************
		// Get all abbreviations to consider
		Map<String, Set<Annotation>> abbreviationToScanSet = new HashMap<String, Set<Annotation>>();

		// > FROM CRF / RF
		List<Annotation> abbreviaitonCRF_List = GATEutils.getAnnInDocOrder(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.abbreviationType);
		for(Annotation abbreviaitonCRF : abbreviaitonCRF_List) {
			String annoOffsetKey = abbreviaitonCRF.getStartNode().getOffset() + "_" + abbreviaitonCRF.getEndNode().getOffset();
			if(!abbreviationToScanSet.containsKey(annoOffsetKey)) abbreviationToScanSet.put(annoOffsetKey, new HashSet<Annotation>());
			abbreviationToScanSet.get(annoOffsetKey).add(abbreviaitonCRF);
		}


		List<Annotation> abbreviaitonCRFshort_LIST = GATEutils.getAnnInDocOrder(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.short_abbrvType);
		for(Annotation abbreviaitonCRFshort : abbreviaitonCRFshort_LIST) {
			String annoOffsetKey = abbreviaitonCRFshort.getStartNode().getOffset() + "_" + abbreviaitonCRFshort.getEndNode().getOffset();
			if(!abbreviationToScanSet.containsKey(annoOffsetKey)) abbreviationToScanSet.put(annoOffsetKey, new HashSet<Annotation>());
			abbreviationToScanSet.get(annoOffsetKey).add(abbreviaitonCRFshort);
		}


		// > TOKEN NOT DETECTED BY CRF / RF AS ABBREVIATIONS WITH LESS THAN 4 CHARS AND IN PARENTHESIS
		Set<Annotation> abbreviaitonInParenthesis_SET = getTokenInParenthesisNotInSet();
		for(Annotation abbreviaitonInParenthesis : abbreviaitonInParenthesis_SET) {
			String annoOffsetKey = abbreviaitonInParenthesis.getStartNode().getOffset() + "_" + abbreviaitonInParenthesis.getEndNode().getOffset();
			if(!abbreviationToScanSet.containsKey(annoOffsetKey)) abbreviationToScanSet.put(annoOffsetKey, new HashSet<Annotation>());
			abbreviationToScanSet.get(annoOffsetKey).add(abbreviaitonInParenthesis);
		}


		for(Entry<String, Set<Annotation>> abbreviationToScan : abbreviationToScanSet.entrySet()) {
			for(Annotation ann : abbreviationToScan.getValue()) {
				logger.debug("      > Candidate abbreviation " + abbreviationToScan.getKey() + ": '" + GATEutils.getAnnotationText(ann, this.document).orElse("NULL") + "' (" + ann.getType() + " " + ((ann.getFeatures() != null) ? ann.getFeatures() : "") + ")");
			}
		}


		// **********************************************************
		// Look for a Long Form for each abbreviation
		for(Entry<String, Set<Annotation>> abbreviationToScan : abbreviationToScanSet.entrySet()) {
			try {
				spotLongForm(abbreviationToScan.getValue());
			} catch (Exception e) {
				GenericUtil.notifyException("Error looking for Long Form of the abbreviation " + GATEutils.getAnnotationText(abbreviationToScan.getValue().iterator().next(), this.document).orElse("NULL"), e, logger);
			}
		}


		long needed = System.currentTimeMillis() - t1;
		logger.debug("   - End tagging document: " + (((this.document.getName() != null) ? this.document.getName() : "NULL")));
		logger.debug("     in (seconds): " + (needed / 1000));
		logger.debug("********************************************");
	}



	private void spotLongForm(Set<Annotation> shortFormSet) {

		Annotation shortForm = shortFormSet.iterator().next();

		// Retrieve Long forms
		List<Annotation> sentenceList = GATEutils.getAnnInDocOrderIntersectAnn(this.document, this.sentenceAnnSet, this.sentenceType, shortForm);

		if(sentenceList != null && sentenceList.size() > 0 && sentenceList.get(0) != null) {

			if(sentenceList.size() > 1) {
				logger.debug("More then one sentence identified for abbreviation: " +  GATEutils.getAnnotationText(shortForm, this.document).orElse("NULL"));
			} 

			Annotation sentenceAnno = sentenceList.get(0);

			logger.debug("********************************************");
			logger.debug("   Analyzing short form: " + GATEutils.getAnnotationText(shortForm, this.document).orElse("NULL"));
			logger.debug("   Document: " + ((this.document.getName() == null) ? "NULL" : this.document.getName()));
			logger.debug("   Sentence: " + GATEutils.getAnnotationText(sentenceAnno, this.document).orElse("NULL"));
			long startOffsetCtx = (shortForm.getStartNode().getOffset() > 35l) ? shortForm.getStartNode().getOffset() - 35l : 0l;
			long endOffsetCtx = (shortForm.getEndNode().getOffset() + 35l < gate.Utils.lengthLong(this.document)) ? shortForm.getEndNode().getOffset() + 35l : gate.Utils.lengthLong(this.document);
			try {
				logger.debug("   Context: " + this.document.getContent().getContent(startOffsetCtx, endOffsetCtx).toString());
			} catch (InvalidOffsetException e) {
				logger.debug("   Context: ERROR");
			}
			logger.debug("********************************************");

			// Get all long forms to consider
			logger.debug("");
			logger.debug("SELECTING CANDIDATE LONG FORMS...");
			Map<String, Set<Annotation>> longFormsToScanMap = new HashMap<String, Set<Annotation>>();

			// > FROM CRF / RF
			List<Annotation> longFormCRF_List = GATEutils.getAnnInDocOrderContainedAnn(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.longFormType, sentenceAnno);
			logger.debug(" longFormCRF_List: " + longFormCRF_List.size());
			for(Annotation longFormCRF : longFormCRF_List) {
				String annoOffsetKey = longFormCRF.getStartNode().getOffset() + "_" + longFormCRF.getEndNode().getOffset();
				if(!longFormsToScanMap.containsKey(annoOffsetKey)) longFormsToScanMap.put(annoOffsetKey, new HashSet<Annotation>());
				longFormsToScanMap.get(annoOffsetKey).add(longFormCRF);
				// logger.debug("      > CRF long form: '" + GATEutils.getAnnotationText(longFormCRF, this.document).orElse("NULL") + "' (" + longFormCRF.getType() + ")");
			}
			logger.debug(" TOTAL SELECTED long form: " + longFormsToScanMap.size());

			// > FROM chunks
			List<Annotation> longFormChunk_List = GATEutils.getAnnInDocOrderContainedAnn(this.document, this.chunkAnnSet, this.chunkType, sentenceAnno);
			logger.debug("      > The sentence contains " + longFormChunk_List.size() + " chunks.");
			longFormChunk_List = longFormChunk_List.stream().filter(anno -> this.checkChunkType(anno)).collect(Collectors.toList());
			logger.debug(" longFormChunk_List: " + longFormChunk_List.size());
			for(Annotation longFormChunk : longFormChunk_List) {
				String annoOffsetKey = longFormChunk.getStartNode().getOffset() + "_" + longFormChunk.getEndNode().getOffset();
				if(!longFormsToScanMap.containsKey(annoOffsetKey)) longFormsToScanMap.put(annoOffsetKey, new HashSet<Annotation>());
				longFormsToScanMap.get(annoOffsetKey).add(longFormChunk);
				// logger.debug("      > Chunk long form: '" + GATEutils.getAnnotationText(longFormChunk, this.document).orElse("NULL") + "' (" + longFormChunk.getType() + ", " + GATEutils.getStringFeature(longFormChunk, this.chunkLabelFeat).orElse("NULL") + ")");
			}
			logger.debug(" TOTAL SELECTED long form: " + longFormsToScanMap.size());

			// > FROM SEDOM
			List<Annotation> longFormSEDOM_List = annotateSEDOMlognForms(shortForm, sentenceAnno);
			logger.debug(" longFormSEDOM_List: " + longFormSEDOM_List.size());
			for(Annotation longFormSEDOM : longFormSEDOM_List) {
				String annoOffsetKey = longFormSEDOM.getStartNode().getOffset() + "_" + longFormSEDOM.getEndNode().getOffset();
				if(!longFormsToScanMap.containsKey(annoOffsetKey)) longFormsToScanMap.put(annoOffsetKey, new HashSet<Annotation>());
				longFormsToScanMap.get(annoOffsetKey).add(longFormSEDOM);
				// logger.debug("      > SEDOM long form: '" + GATEutils.getAnnotationText(longFormSEDOM, this.document).orElse("NULL") + "' (" + longFormSEDOM.getType() + ")");
			}
			logger.debug(" TOTAL SELECTED long form: " + longFormsToScanMap.size());

			// Remove distant LF
			longFormsToScanMap = longFormFarRemoval(shortForm, longFormsToScanMap);
			logger.debug(" TOTAL SELECTED long form after filter: " + longFormsToScanMap.size());

			// Retrieve other facets of the abbreviation
			boolean betweenParenthesis = isTokenBetweenParenthesis(shortForm);
			boolean SHORTabbrvType = isSHORTabbrvType(shortForm);

			// PRINT INFO
			logger.debug("");
			logger.debug("CANDIDATE LONG FORMS:");
			if(longFormsToScanMap == null || longFormsToScanMap.size() == 0) {
				logger.debug("   >>> NONE <<<");
			}

			for(Entry<String, Set<Annotation>> longFormToScan : longFormsToScanMap.entrySet()) {
				for(Annotation ann : longFormToScan.getValue()) {
					logger.debug("      " + longFormToScan.getKey() + "> Candidate long form: '" + GATEutils.getAnnotationText(ann, this.document).orElse("NULL") + "' (" + ann.getType() + " " + ((ann.getFeatures() != null) ? ann.getFeatures() : "") + ")");
				}
			}

			logger.debug("   betweenParenthesis: " + betweenParenthesis);
			logger.debug("   SHORTabbrvType: " + SHORTabbrvType);
			logger.debug("********************************************");

			
			/* START STATS */
			List<Annotation> shortGSlist = GATEutils.getAnnInDocOrderContainedAnn(this.document, "GoldStandard", "SHORT", shortForm);
			if(shortGSlist != null && shortGSlist.size() == 1 && shortGSlist.get(0) != null) {
				try {
					Annotation shortGS = shortGSlist.get(0);

					String relId_SF = "";
					for(Entry<Object, Object> shortGSfeat : shortGS.getFeatures().entrySet()) {
						if(shortGSfeat != null && shortGSfeat.getKey() != null && shortGSfeat.getKey() instanceof String && ((String) shortGSfeat.getKey()).startsWith("relationIDstr")) {
							relId_SF = (String) shortGSfeat.getValue();
						}
					}

					if(relId_SF != null && !relId_SF.trim().equals("")) {
						List<Annotation> intersectingSetnenceList = GATEutils.getAnnInDocOrderIntersectAnn(this.document, this.sentenceAnnSet, this.sentenceType, shortGS);
						for(Annotation intersectingSetnence : intersectingSetnenceList) {

							List<Annotation> longFormsList = GATEutils.getAnnInDocOrderContainedAnn(this.document, "GoldStandard", "LONG", intersectingSetnence);

							List<Annotation> matchingLlongFormsList = new ArrayList<Annotation>();
							for(Annotation longFormAnno : longFormsList) {
								String relId_LF = "";
								for(Entry<Object, Object> longFormAnnoFeat : longFormAnno.getFeatures().entrySet()) {
									if(longFormAnnoFeat != null && longFormAnnoFeat.getKey() != null && longFormAnnoFeat.getKey() instanceof String && ((String) longFormAnnoFeat.getKey()).startsWith("relationIDstr")) {
										relId_LF = (String) longFormAnnoFeat.getValue();
									}
								}

								if(relId_LF != null && relId_LF.equals(relId_SF)) {
									// Found matching long form
									matchingLlongFormsList.add(longFormAnno);
								}
							}

							if(matchingLlongFormsList != null && matchingLlongFormsList.size() > 0) {
								if(!longFormBySpan.containsKey(matchingLlongFormsList.size())) longFormBySpan.put(matchingLlongFormsList.size(), 0);
								longFormBySpan.put(matchingLlongFormsList.size(), longFormBySpan.get(matchingLlongFormsList.size()) + 1);

								// Does it match any long form candidate?
								boolean match = false;
								for(Annotation matchingLongF : matchingLlongFormsList) {

									for(Entry<String, Set<Annotation>> longFormToScan : longFormsToScanMap.entrySet()) {
										long startCandidate = Long.valueOf(longFormToScan.getKey().split("_")[0]);
										long endCandidate = Long.valueOf(longFormToScan.getKey().split("_")[1]);

										if(matchingLongF.getStartNode().getOffset() == startCandidate && matchingLongF.getEndNode().getOffset() == endCandidate) {
											match = true;

											SortedSet<String> setTypes = new TreeSet<String>();
											for(Annotation anno : longFormToScan.getValue()) {
												setTypes.add(anno.getType() + "_" + ((anno.getType().equals(this.chunkType)) ? GATEutils.getStringFeature(anno, this.chunkLabelFeat).orElse("NULL") : ""));
											}

											if(matchingLlongFormsList.size() == 1) {
												candidateMatchASingleSpanLongForm += 1;

												if(!String.join("-", setTypes).equals("")) {
													if(!longFormMatchByTypeSingle.containsKey(String.join("-", setTypes))) longFormMatchByTypeSingle.put(String.join("-", setTypes), 0);
													longFormMatchByTypeSingle.put(String.join("-", setTypes), longFormMatchByTypeSingle.get(String.join("-", setTypes)) + 1);
												}

											}
											else {
												candidateMatchAMultipleSpanLongForm += 1;

												if(!String.join("-", setTypes).equals("")) {
													if(!longFormMatchByTypeMulti.containsKey(String.join("-", setTypes))) longFormMatchByTypeMulti.put(String.join("-", setTypes), 0);
													longFormMatchByTypeMulti.put(String.join("-", setTypes), longFormMatchByTypeMulti.get(String.join("-", setTypes)) + 1);
												}
											}

										}
									}

									if(match) {
										break;
									}
								}

								if(match) {
									longFormWithAtLeastOneCandidateSpanMatch += 1;
								}
								else {
									longFormWithoutAnyCandidateSpanMatch += 1;
									logger.debug("!!!ERROR!!! > Sentence: " + GATEutils.getAnnotationText(intersectingSetnence, this.document).orElse("NULL") + 
											"\nSHORT: " + GATEutils.getAnnotationText(shortGS, this.document).orElse("NULL") + 
											" - DOC: " + ((this.document.getName() != null) ? this.document.getName() : "NO_NAME") + 
											" - LONG " + matchingLlongFormsList.stream().map(anno -> GATEutils.getAnnotationText(anno, this.document).orElse("___")).collect(Collectors.joining(" / ")));
									longFormWithoutAnyCandidateSpanMatchList.add("Sentence: " + GATEutils.getAnnotationText(intersectingSetnence, this.document).orElse("NULL") + 
											"\nSHORT: " + GATEutils.getAnnotationText(shortGS, this.document).orElse("NULL") + 
											" - DOC: " + ((this.document.getName() != null) ? this.document.getName() : "NO_NAME") + 
											" - LONG " + matchingLlongFormsList.stream().map(anno -> GATEutils.getAnnotationText(anno, this.document).orElse("___")).collect(Collectors.joining(" / ")));
									
								}

							}

						}
					}

				}
				catch(Exception e) {
					e.printStackTrace();
				}

			}
			/* END STATS */

			

			// Chose the long form
			logger.debug("");
			logger.debug("CHOSING AMONG CANDIDATE LONG FORMS...");
			try {
				logger.debug("   with context: " + this.document.getContent().getContent(startOffsetCtx, endOffsetCtx).toString());
			} catch (InvalidOffsetException e) {
				logger.debug("   Context: ERROR");
			}
			


		}
		else {
			logger.debug("No containing sentence identified for abbreviation: " +  GATEutils.getAnnotationText(shortForm, this.document).orElse("NULL"));
		}


	}

	/**
	 * New short form identification
	 * 
	 * @param abbreviationSet
	 * @return
	 */
	private Set<Annotation> getTokenInParenthesisNotInSet() {

		Set<Annotation> addedAbbreviations = new HashSet<Annotation>();

		// Add abbreviations not detected by CRF / RF and with less than 4 letters in parenthesis
		List<Annotation> sentenceAnnList = GATEutils.getAnnInDocOrder(this.document, this.sentenceAnnSet, this.sentenceType);
		for(Annotation sentenceAnn : sentenceAnnList) {
			if(sentenceAnn != null) {
				List<Annotation> tokenAnnInSameSentList = GATEutils.getAnnInDocOrderContainedAnn(this.document, this.tokenAnnSet, this.tokenType, sentenceAnn);

				for(int tokenIndex = 0; tokenIndex < tokenAnnInSameSentList.size(); tokenIndex++) {
					Annotation tokenAnnInSameSent = tokenAnnInSameSentList.get(tokenIndex);

					if(tokenAnnInSameSent != null) {

						// Get ABBREVIATION inside same sentence to check if there is not an overlapping ABBREVIATION
						List<Annotation> ABBREVIATIONannInSameSentList = GATEutils.getAnnInDocOrderContainedAnn(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.abbreviationType, sentenceAnn);
						ABBREVIATIONannInSameSentList.addAll(GATEutils.getAnnInDocOrderContainedAnn(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.short_abbrvType, sentenceAnn));
						boolean foundOverlappingABBREVIATION = false;
						if(ABBREVIATIONannInSameSentList != null) {
							for(Annotation ABBREVIATIONannInSameSentAnn : ABBREVIATIONannInSameSentList) {
								if(tokenAnnInSameSent.getStartNode().getOffset() >= ABBREVIATIONannInSameSentAnn.getStartNode().getOffset() && tokenAnnInSameSent.getStartNode().getOffset() < ABBREVIATIONannInSameSentAnn.getEndNode().getOffset()) {
									foundOverlappingABBREVIATION = true;
									break;
								}
							}
						}

						if(foundOverlappingABBREVIATION) {
							break;
						}

						// Get previous and following tokens
						Annotation previousToken = (tokenIndex - 1 >= 0) ? tokenAnnInSameSentList.get(tokenIndex - 1) : null;
						Annotation nextToken = (tokenIndex + 1 < tokenAnnInSameSentList.size()) ? tokenAnnInSameSentList.get(tokenIndex + 1) : null;

						if(previousToken != null && nextToken != null && 
								GATEutils.getAnnotationText(previousToken, this.document).orElse("NONE").equals("(") && GATEutils.getAnnotationText(nextToken, this.document).orElse("NONE").equals(")") ) {
							String ABBRstring = GATEutils.getAnnotationText(tokenAnnInSameSent, this.document).orElse(null);
							if(ABBRstring != null && ABBRstring.trim().length() < 4) {
								try {
									FeatureMap fmAbbrv = Factory.newFeatureMap();
									fmAbbrv.put("TYPE", "BETWEEN_APRENTHESIS");
									Integer annotationID = this.document.getAnnotations(mainAnnSet).add(tokenAnnInSameSent.getStartNode().getOffset(), tokenAnnInSameSent.getEndNode().getOffset(), parenthesisType, fmAbbrv);
									// System.out.println("getEntities > Added abbreviation - string < 4 chars in parenthesis: " + GATEutils.getDocumentText(gateDoc, tokenAnnInSameSent.getStartNode().getOffset(), tokenAnnInSameSent.getEndNode().getOffset()).orElse("---"));

									addedAbbreviations.add(this.document.getAnnotations(mainAnnSet).get(annotationID));

								} catch (InvalidOffsetException e) {
									e.printStackTrace();
								}
							}
						}

					}
				}
			}
		}

		return addedAbbreviations;
	}


	private boolean checkChunkType(Annotation anno) {
		boolean considerChunkLongForm = false;

		if(anno != null && anno.getType().equals(this.chunkType) && anno.getFeatures() != null) {
			String chunkLabel = GATEutils.getStringFeature(anno, this.chunkLabelFeat).orElse("___NO_FEATURE__");

			if(chunkLabel.startsWith("grup-nom")) {
				considerChunkLongForm = true;
			}
			if(chunkLabel.equals("n-fs") || chunkLabel.equals("n-ms") || chunkLabel.equals("sn")) {
				considerChunkLongForm = true;
			}
			if(chunkLabel.equals("w-ms") || chunkLabel.equals("w-fs")) {
				considerChunkLongForm = true;
			}

		}

		/*
		grup-nom-fp=19
		grup-nom-fs=143
		grup-nom-mp=12
		grup-nom-ms=159
		n-fs=33
		sn=267
		w-ms=88
		w-fs=35
		 */

		return considerChunkLongForm;
	}


	private List<Annotation> annotateSEDOMlognForms(Annotation shortFormAnno, Annotation sentenceAnno) {
		List<Annotation> retLongFormSEDOMlist = new ArrayList<Annotation>();

		String shortFormString = GATEutils.getAnnotationText(shortFormAnno, this.document).orElse("");
		String sentenceString = GATEutils.getAnnotationText(sentenceAnno, this.document).orElse("");

		if(shortFormString != null && !shortFormString.trim().equals("") && 
				abbreviationLFmap != null && abbreviationLFmap.containsKey(shortFormString.trim().toLowerCase()) &&
				abbreviationLFmap.get(shortFormString.trim().toLowerCase()) != null && abbreviationLFmap.get(shortFormString.trim().toLowerCase()).size() > 0) {

			Set<String> longFormSEDOMlist = abbreviationLFmap.get(shortFormString.trim().toLowerCase());

			for(String longFormSEDOM : longFormSEDOMlist) {
				if(longFormSEDOM != null && sentenceString != null && sentenceString.toLowerCase().contains(longFormSEDOM.trim().toLowerCase())) {
					int firstIndex = sentenceString.toLowerCase().indexOf(longFormSEDOM.trim().toLowerCase());

					try {
						FeatureMap fmSEDOM_LF = Factory.newFeatureMap();
						fmSEDOM_LF.put("TYPE", "BETWEEN_APRENTHESIS");
						this.document.getAnnotations(mainAnnSet).add(sentenceAnno.getStartNode().getOffset() + ((long) firstIndex), 
								sentenceAnno.getStartNode().getOffset() + ((long) firstIndex + longFormSEDOM.trim().length()), SEDOMtype, fmSEDOM_LF);
					}
					catch(Exception e) {
						logger.debug("Impossible to create annotation of SEDOM string " + longFormSEDOM + " occurring in sentence '" + sentenceString + "' ---> " + e.getMessage());	
					}
				}
			}
		}

		return retLongFormSEDOMlist;
	}


	private boolean isTokenBetweenParenthesis(Annotation shortFormAnno) {

		boolean isINPATRENTHESIS = false;
		String previousText = "";
		if(shortFormAnno.getStartNode().getOffset() >= 2l) {
			previousText = GATEutils.getDocumentText(this.document, shortFormAnno.getStartNode().getOffset() - 2l, shortFormAnno.getStartNode().getOffset()).orElse(null);

		}
		String afterText = "";
		if(shortFormAnno.getEndNode().getOffset() + 2l < gate.Utils.lengthLong(this.document)) {
			afterText = GATEutils.getDocumentText(this.document, shortFormAnno.getEndNode().getOffset(), shortFormAnno.getEndNode().getOffset() + 2l).orElse(null);
		}

		if(previousText != null && previousText.contains("(") && afterText != null && afterText.contains(")")) {
			isINPATRENTHESIS = true;
		}

		return isINPATRENTHESIS;
	}


	private boolean isSHORTabbrvType(Annotation shortFormAnno) {

		boolean isSHORTabbrvType = false;
		if(shortFormAnno != null) {

			List<Annotation> shortAbbrTypeAnnoList = GATEutils.getAnnInDocOrderIntersectAnn(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.short_abbrvType, shortFormAnno);

			if(shortAbbrTypeAnnoList != null && shortAbbrTypeAnnoList.size() > 0) {
				isSHORTabbrvType = true;
			}

		}

		return isSHORTabbrvType;
	}


	private Map<String, Set<Annotation>> longFormFarRemoval(Annotation shortFormAnno, Map<String, Set<Annotation>> longFormList) {
		Map<String, Set<Annotation>> longFormFilteredMapRet = new HashMap<String, Set<Annotation>>();

		if(shortFormAnno != null && longFormList != null && longFormList.size() > 0) {
			for(Entry<String, Set<Annotation>> candidateLongFormMapEntry : longFormList.entrySet()) {
				if(candidateLongFormMapEntry != null) {
					long startOffset = Long.valueOf(candidateLongFormMapEntry.getKey().split("_")[0]);
					long endOffset = Long.valueOf(candidateLongFormMapEntry.getKey().split("_")[1]);

					if((endOffset <= shortFormAnno.getStartNode().getOffset() && endOffset >= shortFormAnno.getStartNode().getOffset() - 3l) || 
							(startOffset <= shortFormAnno.getEndNode().getOffset() + 3l && startOffset >= shortFormAnno.getEndNode().getOffset()) ) {
						longFormFilteredMapRet.put(candidateLongFormMapEntry.getKey(), candidateLongFormMapEntry.getValue());
					}
				}
			}
		}

		return longFormFilteredMapRet;
	}


	public boolean resetAnnotations() {
		this.document.getAnnotations(mainAnnSet).removeAll(this.document.getAnnotations(mainAnnSet).get(longFormType));
		this.document.getAnnotations(mainAnnSet).removeAll(this.document.getAnnotations(mainAnnSet).get(shortFormType));
		return true;
	}

	public static void main(String[] args) {

	}


}
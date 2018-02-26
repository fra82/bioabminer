/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.gateutils.generic.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import es.imim.ibi.bioab.feature.TokenAnnConst;
import es.imim.ibi.bioab.feature.generator.StringInList;
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
	public static final String finalAnnoSet = "BioABresult";
	public static final String parenthesisType_ShortForm = "Abbreviation_Parenthesis";
	public static final String SEDOMtype_CandidateLF = "LongFormCandidate_fromSEDOM";
	public static final String NoFirstToken_CandidateLF = "LongFormCandidate_noFirstTok";
	public static final String longFormType = "LONG";
	public static final String shortFormType = "SHORT";
	public static final String shortLongFormType = "SF - LF";

	private static int relationID = 0;

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
		
		// Remove overlapping abbreviations
		Set<String> abbreviationToRemove = new HashSet<String>();
		for(Entry<String, Set<Annotation>> abbrCandidate : abbreviationToScanSet.entrySet()) {
			
			boolean foundContained = false;
			for(Entry<String, Set<Annotation>> abbrCandidate_INT : abbreviationToScanSet.entrySet()) {
				if(!abbrCandidate.getKey().equals(abbrCandidate_INT.getKey()) && abbrCandidate.getValue().size() > 0 && abbrCandidate_INT.getValue().size() > 0) {
					Annotation abbrv = abbrCandidate.getValue().iterator().next();
					Annotation abbrvInt = abbrCandidate_INT.getValue().iterator().next();
					
					if(abbrv.getStartNode().getOffset() <= abbrvInt.getStartNode().getOffset() && abbrv.getEndNode().getOffset() >= abbrvInt.getEndNode().getOffset()) {
						foundContained = true;
					}
				}
			}
			
			if(foundContained) {
				abbreviationToRemove.add(abbrCandidate.getKey());
			}
		}
		
		for(String abbreviationToRemoveStr : abbreviationToRemove) {
			abbreviationToScanSet.remove(abbreviationToRemoveStr);
		}
		
		
		// **********************************************************
		// Look for a Long Form for each abbreviation
		for(Entry<String, Set<Annotation>> abbreviationToScan : abbreviationToScanSet.entrySet()) {
			try {
				
				// Get set of SHORT and LONG form candidates
				Set<Annotation> shortFormSet = abbreviationToScan.getValue();
				Map<String, Set<Annotation>> longFormsToScanMap = spotLongForm(shortFormSet);

				// Chose the long form
				Annotation shortForm = abbreviationToScan.getValue().iterator().next();
				long startOffsetCtx = (shortForm.getStartNode().getOffset() > 35l) ? shortForm.getStartNode().getOffset() - 35l : 0l;
				long endOffsetCtx = (shortForm.getEndNode().getOffset() + 35l < gate.Utils.lengthLong(this.document)) ? shortForm.getEndNode().getOffset() + 35l : gate.Utils.lengthLong(this.document);

				logger.debug("");
				logger.debug("CHOSING AMONG CANDIDATE LONG FORMS...");
				try {
					logger.debug("   with context: " + this.document.getContent().getContent(startOffsetCtx, endOffsetCtx).toString());
				} catch (InvalidOffsetException e) {
					logger.debug("   Context: ERROR");
				}

				Set<Annotation> chosenLFset = null;
				String choiceStrategy = null;

				if(longFormsToScanMap != null) {

					// If only one candidate and the abbreviation is SHORT, choose that match
					if(longFormsToScanMap.size() == 1) {

						Annotation longFormAnno = longFormsToScanMap.entrySet().iterator().next().getValue().iterator().next();
						Set<Annotation> longFormAnnoSet = longFormsToScanMap.entrySet().iterator().next().getValue();

						String shortText = GATEutils.getAnnotationText(shortForm, this.document).orElse(null);
						String longText = GATEutils.getAnnotationText(longFormAnno, this.document).orElse(null);

						if(shortText.length() >= longText.length()) {
							chosenLFset = null;
						}
						else {

							boolean isLF_CHUNK = false;
							for(Annotation longAnnElem : longFormAnnoSet) {
								if(longAnnElem.getType().equals(this.chunkType)) {
									isLF_CHUNK = true;
								}
							}

							boolean isLF_CRF = false;
							for(Annotation longAnnElem : longFormAnnoSet) {
								if(longAnnElem.getType().equals(BioABabbrvSpotter.abbreviationType)) {
									isLF_CRF = true;
								}
							}

							boolean isLF_SEDOM = false;
							for(Annotation longAnnElem : longFormAnnoSet) {
								if(longAnnElem.getType().equals(SEDOMtype_CandidateLF)) {
									isLF_SEDOM = true;
								}
							}

							if(isLF_CRF || isLF_SEDOM) {
								// The only candidate long is derived from CRF / SEDOM, accept it

								chosenLFset = longFormsToScanMap.entrySet().iterator().next().getValue();
								choiceStrategy = "ST_only_one_LF_candidate_CRF_SEDOM";

							}
							else {		
								// The only candidate long is derived from a CHUNK

								// ATTENTION: implement choice strategy
								double matchScore = matchShortLong(this.document, shortFormSet, longFormAnnoSet);

								if(matchScore > 0.5) {
									chosenLFset = longFormsToScanMap.entrySet().iterator().next().getValue();
									choiceStrategy = "ST_only_one_LF_candidate_CHUNK_" + matchScore;
								}
							}

						}
					}
					else if(longFormsToScanMap.size() > 1) {
						// If more than one candidate...


						// Rate all candidate
						Map<String, Double> annotaRatingMap = new HashMap<String, Double>();

						for(Entry<String, Set<Annotation>> annToRate : longFormsToScanMap.entrySet()) {
							double rankValue = matchShortLong(this.document, shortFormSet, annToRate.getValue());
							annotaRatingMap.put(annToRate.getKey(), rankValue);
							logger.debug(" MULTI LF RANKING > '" + GATEutils.getAnnotationText(annToRate.getValue().iterator().next(), this.document).orElse("NULL") + " > RANK: " + rankValue);
						}

						// Choose best rank
						Set<String> bestRankSet = new HashSet<String>();
						double bestRankValue = -1d;
						for(Entry<String, Double> ratedAnn : annotaRatingMap.entrySet()) {
							if(ratedAnn.getValue().doubleValue() == bestRankValue) {
								bestRankSet.add(ratedAnn.getKey());
							}
							else if(ratedAnn.getValue().doubleValue() > bestRankValue) {
								bestRankValue = ratedAnn.getValue().doubleValue();
								bestRankSet = new HashSet<String>();
								bestRankSet.add(ratedAnn.getKey());
							}
						}


						if(bestRankValue > 0.1d) {
							if(bestRankSet.size() == 1 && bestRankValue > 0.5) {
								// More than one best rank
								chosenLFset = longFormsToScanMap.get(bestRankSet.iterator().next());
								choiceStrategy = "ST_3_MULTIPLE_LF_single_best_rank_" + bestRankValue;
							}
							else {
								logger.debug("MULTIPLE BEST-RANK LF:");

								Set<String> bestRankNotChunk = new HashSet<String>();
								for(String bestRank : bestRankSet) {
									Set<Annotation> longFormAnnoSet_INT = longFormsToScanMap.get(bestRank);

									boolean isLF_CHUNK_INT = false;
									for(Annotation longAnnElem : longFormAnnoSet_INT) {
										if(longAnnElem.getType().equals(this.chunkType)) {
											isLF_CHUNK_INT = true;
										}
									}

									if(!isLF_CHUNK_INT) {
										bestRankNotChunk.add(bestRank);
									}
								}


								if(bestRankNotChunk.size() == 1) {
									// If there is only one CRF / SEDOM LF among the multiple best rank candidate
									chosenLFset = longFormsToScanMap.get(bestRankNotChunk.iterator().next());
									choiceStrategy = "ST_3_MULTIPLE_LF_multi_best_rank_only_one_CRF_SEDOM_" + bestRankValue;
								}
								else if(bestRankValue > 0.5) {
									// If there are multiple candidate with best rank and the best rank is greater than 0.5

									// DO NOTHING

								}

							}
						}
						else {
							logger.debug("getRelations > NONE OF THE CANDIDATES CHOSEN, HIGHEST RANK = 0");
						}

					}
				}

				// Special case: PREVIOUS MATCHING IN SENTENCE
				// Special case 1 for STRATEGY 3: (fibrosis intersticial y atrofia tubular [FI y AT])
				//  > Check for the previous SF in the sentence if not get start offset of sentence
				if(chosenLFset == null) {
					try {
						List<Annotation> sentenceList = GATEutils.getAnnInDocOrderIntersectAnn(this.document, this.sentenceAnnSet, this.sentenceType, shortForm);

						if(sentenceList != null && sentenceList.size() > 0 && sentenceList.get(0) != null) {

							if(sentenceList.size() > 1) {
								logger.debug("More then one sentence identified for abbreviation: " +  GATEutils.getAnnotationText(shortForm, this.document).orElse("NULL"));
							} 

							Annotation sentenceAnno = sentenceList.get(0);

							// Get the end offset of the preceding abbreviation that is more than 15 chars before the beginning of the current one or get the start offset of the sentence
							String previousAbbreviationsInSentenceSel = "";
							long endOffsetOfPreviousAbbreviationsInSentenceSel = sentenceAnno.getStartNode().getOffset();
							for(Entry<String, Set<Annotation>> abbreviationToScan_INT : abbreviationToScanSet.entrySet()) {
								long startOffsetOfAbbreviation = Long.valueOf(abbreviationToScan_INT.getKey().split("_")[0]);
								long endOffsetOfAbbreviation = Long.valueOf(abbreviationToScan_INT.getKey().split("_")[1]);

								if(startOffsetOfAbbreviation >= sentenceAnno.getStartNode().getOffset() && endOffsetOfAbbreviation <= sentenceAnno.getEndNode().getOffset() &&
										endOffsetOfAbbreviation <= shortForm.getStartNode().getOffset() && (shortForm.getStartNode().getOffset() - endOffsetOfAbbreviation) > 15l && 
										(endOffsetOfPreviousAbbreviationsInSentenceSel == -1 || endOffsetOfPreviousAbbreviationsInSentenceSel < endOffsetOfAbbreviation)) {
									previousAbbreviationsInSentenceSel = abbreviationToScan_INT.getKey();
									endOffsetOfPreviousAbbreviationsInSentenceSel = endOffsetOfAbbreviation;
								}
							}

							//  > Get the LFs between the startOffsetSerach and the abbrevAnn start offset
							List<Annotation> previousLFannCOMPLETEList_CRF = GATEutils.getAnnInDocOrderContainedOffset(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.longFormType, 
									endOffsetOfPreviousAbbreviationsInSentenceSel, shortForm.getStartNode().getOffset());
							List<Annotation> previousLFannCOMPLETEList_CHUNK = GATEutils.getAnnInDocOrderContainedOffset(this.document, this.chunkAnnSet, this.chunkType, 
									endOffsetOfPreviousAbbreviationsInSentenceSel, shortForm.getStartNode().getOffset());
							previousLFannCOMPLETEList_CHUNK = previousLFannCOMPLETEList_CHUNK.stream().filter(anno -> this.checkChunkType(anno)).collect(Collectors.toList());
							List<Annotation> previousLFannCOMPLETEList = new ArrayList<Annotation>();
							previousLFannCOMPLETEList.addAll(previousLFannCOMPLETEList_CRF);
							previousLFannCOMPLETEList.addAll(previousLFannCOMPLETEList_CHUNK);
							List<Annotation> previousLFannCOMPLETEListReverse = Lists.reverse(previousLFannCOMPLETEList); // Scan from the one that is far away 
							for(Annotation previousLF : previousLFannCOMPLETEListReverse) {
								Set<Annotation> previousLFlist = new HashSet<Annotation>();
								previousLFlist.add(previousLF);
								double matchSL = matchShortLong(this.document,  abbreviationToScan.getValue(), previousLFlist);
								if(previousLF != null && matchSL > 0.5d) {
									chosenLFset = new HashSet<Annotation>();
									chosenLFset.add(previousLF);
									choiceStrategy = "ST_PREVIOUS_MATCHING_" + matchSL;
									break;
								}
							}
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
				// End special case: PREVIOUS MATCHING IN SENTENCE
				
				
				// Special case: SEDOM
				if(chosenLFset == null) {
					try {
						
						for(Entry<String, Set<Annotation>> lfset : longFormsToScanMap.entrySet()) {
							boolean isLF_SEDOM = false;
							for(Annotation longAnnElem : lfset.getValue()) {
								if(longAnnElem.getType().equals(SEDOMtype_CandidateLF)) {
									isLF_SEDOM = true;
								}
							}
							
							if(isLF_SEDOM) {
								chosenLFset = lfset.getValue();
								choiceStrategy = "ST_SEDOM_MATCH";
								break;
							}
						}
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
				// End special case: SEDOM
				
				
				// End special case: REMOVE ARTICLE LF
				if(chosenLFset == null) {
					try {
						
						for(Entry<String, Set<Annotation>> lfset : longFormsToScanMap.entrySet()) {
							
							List<Annotation> tokenAnnotationList = GATEutils.getAnnInDocOrderContainedAnn(this.document, this.tokenAnnSet, this.tokenType, lfset.getValue().iterator().next());
							
							if(tokenAnnotationList != null && tokenAnnotationList.size() > 1 && tokenAnnotationList.get(0) != null) {
								String tokenPOS = GATEutils.getStringFeature(tokenAnnotationList.get(0), TokenAnnConst.tokenPOSFeat).orElse("___").trim().toLowerCase();
								if(tokenPOS.equals("c") || tokenPOS.endsWith("p") || tokenPOS.endsWith("d")) {
									try {
										Integer longFormWithoutFirstTokenID = this.document.getAnnotations(mainAnnSet).add(tokenAnnotationList.get(1).getStartNode().getOffset(), 
												lfset.getValue().iterator().next().getEndNode().getOffset(), NoFirstToken_CandidateLF, Factory.newFeatureMap());
										Annotation longFormWithoutFirstTokenAnno = this.document.getAnnotations(mainAnnSet).get(longFormWithoutFirstTokenID);
										
										Set<Annotation> previousLFlist = new HashSet<Annotation>();
										previousLFlist.add(longFormWithoutFirstTokenAnno);
										double matchSL = matchShortLong(this.document,  abbreviationToScan.getValue(), previousLFlist);
										if(longFormWithoutFirstTokenAnno != null && matchSL > 0.5d) {
											chosenLFset = new HashSet<Annotation>();
											chosenLFset.add(longFormWithoutFirstTokenAnno);
											choiceStrategy = "ST_WITHOUT_FIRST_TOKEN_" + matchSL;
											break;
										}
									}
									catch(Exception e) {
										/* Do nothing */
									}
									
								}
								
							}
							
						}
						
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
				// End special case: REMOVE ARTICLE LF
				
				
				generateSF_LFanno(chosenLFset, choiceStrategy, abbreviationToScan.getValue());

			} catch (Exception e) {
				GenericUtil.notifyException("Error looking for Long Form of the abbreviation " + GATEutils.getAnnotationText(abbreviationToScan.getValue().iterator().next(), this.document).orElse("NULL"), e, logger);
			}
		}
		
		// Transfet annotations from the mainAnnSet to the finalAnnoSet
		GATEutils.transferAnnotations(this.document, longFormType, longFormType, mainAnnSet, finalAnnoSet, null);
		GATEutils.transferAnnotations(this.document, shortFormType, shortFormType, mainAnnSet, finalAnnoSet, null);
		GATEutils.transferAnnotations(this.document, shortLongFormType, shortLongFormType, mainAnnSet, finalAnnoSet, null);

		GATEutils.transferAnnotations(this.document, BioABabbrvSpotter.contextual_abbrvType, "CONTEXTUAL", mainAnnSet, finalAnnoSet, null);
		GATEutils.transferAnnotations(this.document, BioABabbrvSpotter.derived_abbrvType, "DERIVED", mainAnnSet, finalAnnoSet, null);
		GATEutils.transferAnnotations(this.document, BioABabbrvSpotter.multiple_abbrvType, "MULTIPLE", mainAnnSet, finalAnnoSet, null);
		GATEutils.transferAnnotations(this.document, BioABabbrvSpotter.global_abbrvType, "GLOBAL", mainAnnSet, finalAnnoSet, null);
		
		
		long needed = System.currentTimeMillis() - t1;
		logger.debug("   - End tagging document: " + (((this.document.getName() != null) ? this.document.getName() : "NULL")));
		logger.debug("     in (seconds): " + (needed / 1000));
		logger.debug("********************************************");
	}

	private Map<String, Set<Annotation>> spotLongForm(Set<Annotation> shortFormSet) {

		Map<String, Set<Annotation>> longFormsToScanMap = new HashMap<String, Set<Annotation>>();
		
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

			// > FROM CRF / RF
			List<Annotation> longFormCRF_List = GATEutils.getAnnInDocOrderContainedAnn(this.document, BioABabbrvSpotter.mainAnnSet, BioABabbrvSpotter.longFormType, sentenceAnno);
			logger.debug(" longFormCRF_List: " + longFormCRF_List.size());
			for(Annotation longFormCRF : longFormCRF_List) {
				String annoOffsetKey = longFormCRF.getStartNode().getOffset() + "_" + longFormCRF.getEndNode().getOffset();
				if(!longFormsToScanMap.containsKey(annoOffsetKey)) longFormsToScanMap.put(annoOffsetKey, new HashSet<Annotation>());
				longFormsToScanMap.get(annoOffsetKey).add(longFormCRF);
				logger.debug("      > CRF long form: '" + GATEutils.getAnnotationText(longFormCRF, this.document).orElse("NULL") + "' (" + longFormCRF.getType() + ")");
			}
			logger.debug(" TOTAL SELECTED long form: " + longFormsToScanMap.size());

			// > FROM chunks
			List<Annotation> longFormChunk_List = GATEutils.getAnnInDocOrderContainedAnn(this.document, this.chunkAnnSet, this.chunkType, sentenceAnno);
			logger.debug("      > The sentence contains " + longFormChunk_List.size() + " chunks.");
			createNewChunks();
			longFormChunk_List = longFormChunk_List.stream().filter(anno -> this.checkChunkType(anno)).collect(Collectors.toList());
			logger.debug(" longFormChunk_List: " + longFormChunk_List.size());
			for(Annotation longFormChunk : longFormChunk_List) {
				String annoOffsetKey = longFormChunk.getStartNode().getOffset() + "_" + longFormChunk.getEndNode().getOffset();
				if(!longFormsToScanMap.containsKey(annoOffsetKey)) longFormsToScanMap.put(annoOffsetKey, new HashSet<Annotation>());
				longFormsToScanMap.get(annoOffsetKey).add(longFormChunk);
				logger.debug("      > Chunk long form: '" + GATEutils.getAnnotationText(longFormChunk, this.document).orElse("NULL") + "' (" + longFormChunk.getType() + ", " + GATEutils.getStringFeature(longFormChunk, this.chunkLabelFeat).orElse("NULL") + ")");
			}
			logger.debug(" TOTAL SELECTED long form: " + longFormsToScanMap.size());

			// > FROM SEDOM
			Set<Annotation> longFormSEDOM_Set = annotateSEDOMlognForms(shortForm, sentenceAnno);
			logger.debug(" longFormSEDOM_Set: " + longFormSEDOM_Set.size());
			for(Annotation longFormSEDOM : longFormSEDOM_Set) {
				String annoOffsetKey = longFormSEDOM.getStartNode().getOffset() + "_" + longFormSEDOM.getEndNode().getOffset();
				if(!longFormsToScanMap.containsKey(annoOffsetKey)) longFormsToScanMap.put(annoOffsetKey, new HashSet<Annotation>());
				longFormsToScanMap.get(annoOffsetKey).add(longFormSEDOM);
				logger.debug("      > SEDOM long form: '" + GATEutils.getAnnotationText(longFormSEDOM, this.document).orElse("NULL") + "' (" + longFormSEDOM.getType() + ")");
			}
			logger.debug(" TOTAL SELECTED long form: " + longFormsToScanMap.size());

			// Remove distant or overlapping LF
			longFormsToScanMap = longFormFarOverlapRemoval(shortForm, longFormsToScanMap);
			logger.debug(" TOTAL SELECTED long form after filter: " + longFormsToScanMap.size());

			// Retrieve other facets of the abbreviation
			boolean betweenParenthesis = isTokenBetweenParenthesis(shortForm);
			boolean SHORTabbrvType = isSHORTabbrvType(shortForm);

			// Remove LF following the SHORT form if in parenthesis
			if(betweenParenthesis) {
				Set<String> longFormToDelSet = new HashSet<String>();
				long shortFormStartOffset = shortForm.getStartNode().getOffset();
				for(Entry<String, Set<Annotation>> longFormsToScan : longFormsToScanMap.entrySet()) {
					long longFormEndNode = Long.valueOf(longFormsToScan.getKey().split("_")[1]);
					if(longFormEndNode > shortFormStartOffset) {
						longFormToDelSet.add(longFormsToScan.getKey());
						logger.debug("      > Removing long form before parenthesis of short form: '" + GATEutils.getAnnotationText(longFormsToScan.getValue().iterator().next(), this.document).orElse("NULL") + 
								"' (" + longFormsToScan.getValue().iterator().next().getType() + ")");
					}
				}

				for(String longFormToDel : longFormToDelSet) {
					longFormsToScanMap.remove(longFormToDel);
				}
				logger.debug(" TOTAL SELECTED long form after parenthesis removal: " + longFormsToScanMap.size());
			}

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

		}
		
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
								logger.debug("The following LF has no candidate span matching it:\n"
										+ "LONG " + matchingLlongFormsList.stream().map(anno -> GATEutils.getAnnotationText(anno, this.document).orElse("___")).collect(Collectors.joining(" / "))
										+ " SHORT: " + GATEutils.getAnnotationText(shortGS, this.document).orElse("NULL")
										+ " DOC: " + ((this.document.getName() != null) ? this.document.getName() : "NO_NAME")
										+ "\n Sentence: " + GATEutils.getAnnotationText(intersectingSetnence, this.document).orElse("NULL"));
								longFormWithoutAnyCandidateSpanMatchList.add("The following LF has no candidate span matching it:\n"
										+ "LONG " + matchingLlongFormsList.stream().map(anno -> GATEutils.getAnnotationText(anno, this.document).orElse("___")).collect(Collectors.joining(" / "))
										+ " SHORT: " + GATEutils.getAnnotationText(shortGS, this.document).orElse("NULL")
										+ " DOC: " + ((this.document.getName() != null) ? this.document.getName() : "NO_NAME")
										+ "\n Sentence: " + GATEutils.getAnnotationText(intersectingSetnence, this.document).orElse("NULL"));
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
		

		return longFormsToScanMap;
	}


	private void generateSF_LFanno(Set<Annotation> chosenCandidateLFset, String strategy, Set<Annotation> shortFormSet) throws InvalidOffsetException {

		// If the LF starts with article or prop
		if(chosenCandidateLFset != null && chosenCandidateLFset.size() > 0 && strategy != null && shortFormSet != null && shortFormSet.size() > 0) {

			Annotation chosenCandidateLF = chosenCandidateLFset.iterator().next();

			boolean startOrEndWithSpecialChars = false;
			String LFtext = GATEutils.getAnnotationText(chosenCandidateLF, this.document).orElse("");
			long offsetToRemFronNewLF = 0l;
			if(LFtext.toLowerCase().trim().startsWith("la ") || LFtext.toLowerCase().trim().startsWith("el ") ||
					LFtext.toLowerCase().trim().startsWith("lo ") || LFtext.toLowerCase().trim().startsWith("de ") ||
					LFtext.toLowerCase().trim().startsWith("un ") || LFtext.toLowerCase().trim().startsWith("uno ") ||
					LFtext.toLowerCase().trim().startsWith("una ") || 
					LFtext.toLowerCase().trim().startsWith("e ") || LFtext.toLowerCase().trim().startsWith("o ") || LFtext.toLowerCase().trim().startsWith("y ")) {
				startOrEndWithSpecialChars = true;

				if(LFtext.toLowerCase().trim().startsWith("la ") || LFtext.toLowerCase().trim().startsWith("el ") ||
						LFtext.toLowerCase().trim().startsWith("lo ") || LFtext.toLowerCase().trim().startsWith("de ") ||
						LFtext.toLowerCase().trim().startsWith("un ")) {
					offsetToRemFronNewLF = 3l;
				}
				else if(LFtext.toLowerCase().trim().startsWith("uno ") || LFtext.toLowerCase().trim().startsWith("una ")) {
					offsetToRemFronNewLF = 4l;
				}
				else if(LFtext.toLowerCase().trim().startsWith("e ") || LFtext.toLowerCase().trim().startsWith("o ") || LFtext.toLowerCase().trim().startsWith("y ")) {
					offsetToRemFronNewLF = 2l;
				}
			}

			if(startOrEndWithSpecialChars && offsetToRemFronNewLF > 0l) {
				long newStartNode = chosenCandidateLFset.iterator().next().getStartNode().getOffset() + offsetToRemFronNewLF;

				if(newStartNode < chosenCandidateLFset.iterator().next().getEndNode().getOffset()) {
					// Old Candidate LONG
					this.document.getAnnotations(BioABabbrvSpotter.mainAnnSet).add(chosenCandidateLF.getStartNode().getOffset(), chosenCandidateLF.getEndNode().getOffset(),
							BioABabbrvSpotter.longFormType + "_OLD", chosenCandidateLF.getFeatures());
					// New Candidate LONG
					Integer newLongId = this.document.getAnnotations(BioABabbrvSpotter.mainAnnSet).add(newStartNode, chosenCandidateLF.getEndNode().getOffset(),
							BioABabbrvSpotter.longFormType, chosenCandidateLF.getFeatures());
					if(newLongId != null) {
						Annotation newChosenCandidateLF = this.document.getAnnotations(mainAnnSet).get(newLongId);
						if(newChosenCandidateLF != null) {
							chosenCandidateLF = newChosenCandidateLF;
						}
					}
				}
			}

			// Generate LF - SF annotation
			if(chosenCandidateLF != null) {
				int relID = relationID++;

				// New Candidate LONG
				Integer newLongId = this.document.getAnnotations(mainAnnSet).add(chosenCandidateLF.getStartNode().getOffset(), chosenCandidateLF.getEndNode().getOffset(),
						longFormType, chosenCandidateLF.getFeatures());
				Annotation LONG_FORM = null;
				if(newLongId != null) {
					Annotation newChosenLF = this.document.getAnnotations(mainAnnSet).get(newLongId);
					if(newChosenLF != null) {
						LONG_FORM = newChosenLF;
						LONG_FORM.setFeatures((chosenCandidateLF.getFeatures() == null) ? Factory.newFeatureMap() : chosenCandidateLF.getFeatures());
						LONG_FORM.getFeatures().put("strategy", ((strategy != null) ? strategy : "NULL"));
						LONG_FORM.getFeatures().put("relationID", relID + "");
					}
				}

				if(LONG_FORM != null) {

					// New Candidate SHORT
					Annotation shortForm = shortFormSet.iterator().next();
					Integer newShortId = this.document.getAnnotations(mainAnnSet).add(shortForm.getStartNode().getOffset(), shortForm.getEndNode().getOffset(),
							shortFormType, chosenCandidateLF.getFeatures());
					Annotation SHORT_FORM = null;
					if(newShortId != null) {
						Annotation newChosenSF = this.document.getAnnotations(mainAnnSet).get(newShortId);
						if(newChosenSF != null) {
							SHORT_FORM = newChosenSF;
							SHORT_FORM.setFeatures((shortForm.getFeatures() == null) ? Factory.newFeatureMap() : shortForm.getFeatures());
							SHORT_FORM.getFeatures().put("strategy", ((strategy != null) ? strategy : "NULL"));
							SHORT_FORM.getFeatures().put("relationID", relID + "");
						}
					}

					if(SHORT_FORM != null) {
						long startSF_LFann = (shortForm.getStartNode().getOffset() < LONG_FORM.getStartNode().getOffset()) ? shortForm.getStartNode().getOffset() : LONG_FORM.getStartNode().getOffset();
						long endSF_LFann = (shortForm.getEndNode().getOffset() < LONG_FORM.getEndNode().getOffset()) ? LONG_FORM.getEndNode().getOffset() : shortForm.getEndNode().getOffset();

						// Add the real long form corresponding to the short one, chosen among the set of "LONG_CANDIDATE" (chosen by ML)
						// and "Chunk" annotations (added by Freeling)
						// Removed: LONG_CANDIDATE
						// try {
						// 	FeatureMap relFm = Factory.newFeatureMap();
						// 	relFm.put("strategy", ((choiceStrategy != null) ? choiceStrategy : "NULL"));
						// 	gateDoc.getAnnotations("ABBRV_" + execVersion + "_FINAL_" + gsVersion).add(chosenLF.getStartNode().getOffset(), chosenLF.getEndNode().getOffset(), "LONG", relFm);
						// } catch (InvalidOffsetException e) {
						// 	e.printStackTrace();
						// }

						FeatureMap fm = Factory.newFeatureMap();
						fm.put("strategy", ((strategy != null) ? strategy : "NULL"));
						fm.put("relationID", relID + "");
						Integer newLongShortId = this.document.getAnnotations(mainAnnSet).add(startSF_LFann, endSF_LFann, shortLongFormType, fm);

						Annotation LONG_SHORT_FORM = null;
						if(newLongShortId != null) {
							Annotation newChosenLSF = this.document.getAnnotations(mainAnnSet).get(newLongShortId);
							if(newChosenLSF != null) {
								LONG_SHORT_FORM = newChosenLSF;
								logger.info("Generated " + shortLongFormType + " annotaiton: " + GATEutils.getAnnotationText(LONG_SHORT_FORM, this.document));
								logger.info("   > " + shortFormType + " annotaiton: " + GATEutils.getAnnotationText(SHORT_FORM, this.document));
								logger.info("   > " + longFormType + " annotaiton: " + GATEutils.getAnnotationText(LONG_FORM, this.document));
								logger.info("   > STRATEGY: " + ((strategy != null) ? strategy : "NULL"));
							}
						}


					}
					else {
						logger.debug("Issues generating SHORT form!");
					}

				}
				else {
					logger.debug("Issues generating LONG form!");
				}


			}
			else {
				// DO NOT REMOVE THE SHORT ANNOTATION - IT IS MORE LIKELY IT IS A SHORT ANNOTATION FOR WHICH THE LF COULDN'T BE FOUND
				// if(isSHORT) {
				// Remove the SHORT annotation for the short form since no matching LF has been found
				// abbrvInSentToDel.add(shortIntersectAnnList.get(0));
				// }

			}

		}

	}


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
									Integer annotationID = this.document.getAnnotations(mainAnnSet).add(tokenAnnInSameSent.getStartNode().getOffset(), tokenAnnInSameSent.getEndNode().getOffset(), parenthesisType_ShortForm, fmAbbrv);
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
	
	
	private void createNewChunks() {
		
		List<Annotation> chunkAnno = GATEutils.getAnnInDocOrder(this.document, this.chunkAnnSet, this.chunkType);
		
		for(int i = 0; i < chunkAnno.size(); i++) {
			if(i < (chunkAnno.size() - 1) ) {
				Annotation currentChunk = chunkAnno.get(i);
				Annotation nextChunk = chunkAnno.get(i+1);
				
				if(currentChunk != null && nextChunk != null) {
					String currentChunkLabel = GATEutils.getStringFeature(currentChunk, this.chunkLabelFeat).orElse(null);
					String nextChunkLabel = GATEutils.getStringFeature(currentChunk, this.chunkLabelFeat).orElse(null);
					if(!Strings.isNullOrEmpty(currentChunkLabel) && !Strings.isNullOrEmpty(nextChunkLabel)) {
						
						// Case 1: n-ms / grup-nom-ms + sp-de
						if((currentChunkLabel.equals("n-ms") || currentChunkLabel.equals("grup-nom-ms")) && nextChunkLabel.equals("sp-de")) {
							try {
								FeatureMap fm = Factory.newFeatureMap();
								fm.put("type", "DERIVED BY MERGING TWO CHUNKS");
								fm.put("label", currentChunkLabel + "___" + nextChunkLabel);
								this.document.getAnnotations(this.chunkAnnSet).add(currentChunk.getStartNode().getOffset(), nextChunk.getEndNode().getOffset(), this.chunkType, fm);
							} catch (InvalidOffsetException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	
	private boolean checkChunkType(Annotation anno) {
		boolean considerChunkLongForm = false;

		if(anno != null && anno.getType().equals(this.chunkType) && anno.getFeatures() != null) {
			String chunkLabel = GATEutils.getStringFeature(anno, this.chunkLabelFeat).orElse("___NO_FEATURE__");

			if(chunkLabel.startsWith("grup-nom")) {
				considerChunkLongForm = true;
			}
			if(chunkLabel.startsWith("n") || chunkLabel.equals("sn")) {
				considerChunkLongForm = true;
			}
			if(chunkLabel.startsWith("w")) {
				considerChunkLongForm = true;
			}

		}
		
		return considerChunkLongForm;
	}


	private Set<Annotation> annotateSEDOMlognForms(Annotation shortFormAnno, Annotation sentenceAnno) {
		Set<Annotation> retLongFormSEDOMset = new HashSet<Annotation>();

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
						fmSEDOM_LF.put("TYPE", "FROM_SEDOM");
						Integer sedomLFAnnoID = this.document.getAnnotations(mainAnnSet).add(sentenceAnno.getStartNode().getOffset() + ((long) firstIndex), 
								sentenceAnno.getStartNode().getOffset() + ((long) firstIndex + longFormSEDOM.trim().length()), SEDOMtype_CandidateLF, fmSEDOM_LF);
						retLongFormSEDOMset.add(this.document.getAnnotations(mainAnnSet).get(sedomLFAnnoID));
					}
					catch(Exception e) {
						logger.debug("Impossible to create annotation of SEDOM string " + longFormSEDOM + " occurring in sentence '" + sentenceString + "' ---> " + e.getMessage());	
					}
				}
			}
		}

		return retLongFormSEDOMset;
	}

	public double matchShortLong(Document gateDoc, Set<Annotation> shortAnnSet, Set<Annotation> longAnnSet) {

		if(shortAnnSet != null && longAnnSet != null && shortAnnSet.size() > 0 && longAnnSet.size() > 0) {

			Annotation shortAnn = shortAnnSet.iterator().next();
			Annotation longAnn = longAnnSet.iterator().next();

			String shortText = GATEutils.getAnnotationText(shortAnn, gateDoc).orElse("");
			shortText = shortText.replaceAll("\\s+","");
			String longText = GATEutils.getAnnotationText(longAnn, gateDoc).orElse(null);

			boolean isSF_SHORT = false;
			for(Annotation shortAnnElem : shortAnnSet) {
				if(shortAnnElem.getType().equals(BioABabbrvSpotter.short_abbrvType)) {
					isSF_SHORT = true;
				}
			}

			boolean isLF_CHUNK = false;
			for(Annotation longAnnElem : longAnnSet) {
				if(longAnnElem.getType().equals(this.chunkType)) {
					isLF_CHUNK = true;
				}
			}

			boolean isLF_CRF = false;
			for(Annotation longAnnElem : longAnnSet) {
				if(longAnnElem.getType().equals(BioABabbrvSpotter.abbreviationType)) {
					isLF_CRF = true;
				}
			}

			boolean isLF_SEDOM = false;
			for(Annotation longAnnElem : longAnnSet) {
				if(longAnnElem.getType().equals(SEDOMtype_CandidateLF)) {
					isLF_SEDOM = true;
				}
			}


			if(shortText != null && longText != null && !shortText.trim().equals("") && !longText.trim().equals("") && shortText.length() < longText.length()) {

				List<Annotation> longTextTokens = GATEutils.getAnnInDocOrderContainedAnn(gateDoc, this.tokenAnnSet,this.tokenType, longAnn);

				// Not consider long forms starting with article or conjunction
				if(beginWithArticleOrConj(longAnn, gateDoc)) {
					return 0d;
				}

				// STRATEGY 1 - Match first letter
				String abbrv1 = ""; // Only n / a / g
				String abbrv2 = ""; // All tokens
				for(Annotation annTok : longTextTokens) {
					if(annTok != null) {
						String tokenStr = GATEutils.getAnnotationText(annTok, gateDoc).orElse("").trim();						
						String tokenPOS = GATEutils.getStringFeature(annTok, TokenAnnConst.tokenPOSFeat).orElse("___").trim().toLowerCase();
						tokenPOS = tokenPOS.trim().toLowerCase();
						if(tokenPOS.equals("n") || tokenPOS.endsWith("a") || tokenPOS.endsWith("g")) {
							abbrv1 += (tokenStr != null) ? tokenStr.trim().toLowerCase().charAt(0) : tokenStr;
						}
						abbrv2 += (tokenStr != null) ? tokenStr.trim().toLowerCase().charAt(0) : tokenStr;
					}
				}

				// All POS n / a / g
				String shortTextNoPunct = shortText.replaceAll("-", "").replaceAll("_", "").replaceAll("/", "").replaceAll("\\*", "\\\\*");
				if(!shortText.equals(shortTextNoPunct)) {
					// SSystem.out.println("SHORT TEXT: " + shortText + " > NO PUNCT: " + shortTextNoPunct);
				}
				if(abbrv1.length() == abbrv2.length() && shortText.length() > 1 && 
						(shortText.trim().toLowerCase().equals(abbrv1.trim().toLowerCase()) || shortTextNoPunct.trim().toLowerCase().equals(abbrv1.trim().toLowerCase()))) {
					// SSystem.out.println("EVAL: " + shortText + " long: " + longText + " - " + ((isSF_SHORT) ? 1.0 : 0.6));
					return (isSF_SHORT) ? 1.0 : 0.510;
				}

				// POS other than n / a / g
				if(abbrv1.length() != abbrv2.length() && shortText.length() > 1 && 
						(shortText.trim().toLowerCase().equals(abbrv1.trim().toLowerCase()) || shortText.trim().toLowerCase().equals(abbrv2.trim().toLowerCase()) ||
								shortTextNoPunct.trim().toLowerCase().equals(abbrv1.trim().toLowerCase()) || shortTextNoPunct.trim().toLowerCase().equals(abbrv2.trim().toLowerCase())) ) {
					// SSystem.out.println("EVAL: " + shortText + " long: " + longText + " - " +  ((isSF_SHORT) ? 0.95 : 0.55));
					return (isSF_SHORT) ? 0.9 : 0.509;
				}

				// Auto build abbreviation
				String abbrv3 = "";
				String[] longFormSplit = longText.split(" ");
				for(String longElem : longFormSplit) {
					if(longElem != null && longElem.length() > 2) {
						abbrv3 += longElem.trim().toLowerCase().charAt(0);
					}
				}

				if(shortText.length() > 1 && shortText.trim().toLowerCase().equals(abbrv3.trim().toLowerCase())) {
					// System.out.println("EVAL: " + shortText + " long: " + longText + " - " + ((isSF_SHORT) ? 0.93 : 0.53));
					return (isSF_SHORT) ? 0.8 : 0.508;
				}


				// System.out.println("EVAL: " + shortText + " long: " + longText + " - " + 0.1);
				return 0.1d;
			}
		}

		// System.out.println("EVAL - " + 0);
		return 0d;
	}


	/**
	 * Check first token POS
	 * 
	 * @param ann
	 * @param doc
	 * @return
	 */
	public boolean beginWithArticleOrConj(Annotation ann, Document doc) {

		if(ann != null && doc != null) {
			List<Annotation> longTextTokens = GATEutils.getAnnInDocOrderContainedAnn(doc, this.tokenAnnSet, this.tokenType, ann);

			if(longTextTokens != null && longTextTokens.size() > 0 && longTextTokens.get(0) != null) {
				String POSofFirstToken = GATEutils.getStringFeature(longTextTokens.get(0), "POS").orElse("____").trim().toLowerCase();
				if(POSofFirstToken.toLowerCase().equals("c") || POSofFirstToken.toLowerCase().equals("s") || POSofFirstToken.toLowerCase().equals("d") || POSofFirstToken.toLowerCase().equals("p")) {
					return true;
				}
			}
		}

		return false;		
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


	private Map<String, Set<Annotation>> longFormFarOverlapRemoval(Annotation shortFormAnno, Map<String, Set<Annotation>> longFormList) {
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
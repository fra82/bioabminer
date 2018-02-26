package es.imim.ibi.bioab.exec.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.backingdata.gateutils.GATEutils;
import org.backingdata.gateutils.generic.PropertyManager;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import es.imim.ibi.bioab.nlp.freeling.FreelingParser;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import gate.util.SimpleFeatureMapImpl;

/**
 * Collection of static methods to convert PDF documents of scientific publications by means of GROBID (https://github.com/kermitt2/grobid).
 * 
 * @author Francesco Ronzano
 *
 */
public class GROBIDloader {

	private static Logger logger = LoggerFactory.getLogger(GROBIDloader.class);

	public static String mainAnnoSet = "GROBIDstructure";
	public static String abstractAnnoType = "abstract";
	public static String bibEntryAnnoType = "bibEntry";
	public static String headerAnnoType = "header";
	public static String secTitleAnnoType = "sectTitle";
	public static String paperTitleAnnoType = "title";
	public static String tableAnnoType = "table";
	public static String figureAnnoType = "figure";
	public static String keywordsAnnoType = "keywords";
	public static String authorAnnoType = "author";
	public static String affiliationAnnoType = "affiliation";
	public static String addressAnnoType = "address";

	private static Random rnd = new Random();
	private static boolean isInitialized = false;

	public static void initGROBID() {

		if(isInitialized) {
			return;
		}
		else {
			try {
				logger.info("Initializing GROBID...");

				String bioABminerResourceFolder = PropertyManager.getProperty("resourceFolder.fullPath");
				if(!bioABminerResourceFolder.endsWith(File.separator)) bioABminerResourceFolder += File.separator;

				GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(bioABminerResourceFolder + "grobid-home_0_5_1//grobid-home"));       
				GrobidProperties.getInstance(grobidHomeFinder);

				logger.info("GROBID correctly initialized (home set to: " + ((GrobidProperties.get_GROBID_HOME_PATH() != null) ? GrobidProperties.get_GROBID_HOME_PATH() : "NULL") + ").");
				isInitialized = true;
			}
			catch (Exception e) {
				logger.error("IMPOSSIBLE TO LOAD GROBID PDF TO TEXT CONVERTER.");
				e.printStackTrace();
			}

		}

	}


	public static Document parsePDF(byte[] PDFbyteArray, String PDFfileName) throws Exception {

		initGROBID();

		Document retDocument = null;

		String bioABminerResourceFolder = PropertyManager.getProperty("resourceFolder.fullPath");
		if(!bioABminerResourceFolder.endsWith(File.separator)) bioABminerResourceFolder += File.separator;

		// Create temp file
		String tempFileName = rnd.nextInt(100000) + "_tempGROBID_PDF";
		File tempPDFfile = null;
		try {
			tempPDFfile = File.createTempFile(tempFileName, ".pdf", new File(bioABminerResourceFolder + "grobid-home_0_5_1" + File.separator  + "tmp"));
			FileOutputStream fos = new FileOutputStream(tempPDFfile);
			fos.write(PDFbyteArray);
			fos.close();
		} catch (IOException e) {
			logger.error("Creating temporal PDF file");
			e.printStackTrace();
		}


		// Execute GROBID
		String GROBIDresult = "";
		try {
			Engine engine = GrobidFactory.getInstance().createEngine();
			GROBIDresult = engine.fullTextToTEI(tempPDFfile, GrobidAnalysisConfig.defaultInstance());
		} 
		catch (Exception e) {
			logger.error("Converting PDF by GROBID");
			e.printStackTrace();
		}

		// Delete temp file
		try {
			tempPDFfile.delete();
		} 
		catch (Exception e) {
			logger.error("Deleting temporal PDF file");
			e.printStackTrace();
		}


		if(GROBIDresult != null && GROBIDresult.length() > 600) {

			try {
				retDocument = Factory.newDocument(GROBIDresult);

				if(retDocument.getContent().size() <= 30l) {
					retDocument = null;
					throw new Exception("Document textual content long less than 30 chars. Ignored document - text contents too short");

				}
			} catch (ResourceInstantiationException e) {
				retDocument = null;
				e.printStackTrace();
				throw new Exception("Error while instantiating Document from PDF file contents");
			}

			if(retDocument != null) {
				// Set name feature of the document
				retDocument.getFeatures().put("name", (StringUtils.isNotBlank(PDFfileName) ? PDFfileName : "NO_NAME"));
			}
		}
		else {
			logger.info("Error while converting PDF file by GROBID");
		}

		// Empty temporary folder
		try {
			File GROBIDtempFolder = new File(bioABminerResourceFolder + "grobid-home_0_5_1" + File.separator  + "tmp");
			if(GROBIDtempFolder != null && GROBIDtempFolder.exists() && GROBIDtempFolder.isDirectory()) {
				File[] files = GROBIDtempFolder.listFiles();
				if(files != null) {
					for(File f: files) {
						if(f.isDirectory()) {
							deleteFolder(f);
						} else {
							try {
								f.delete();
							}
							catch (Exception e) {
								// DO NOTHING
							}
						}
					}
				}

				files = GROBIDtempFolder.listFiles();
				logger.info("GROBID temporary folder empty (" + files.length +" files contained).");
			}
			else {
				logger.info("Impossible to remove contents of GROBID temporary folder.");
			}
		}
		catch (Exception e) {
			// DO NOTHING
		}

		return retDocument;
	}

	private static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) { //some JVMs return null for empty dirs
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					try {
						f.delete();
					}
					catch (Exception e) {
						// DO NOTHING
					}
				}
			}
		}
		try {
			folder.delete();
		}
		catch (Exception e) {
			// DO NOTHING
		}
	}


	public static Document parsePDF(String absoluteFilePath) throws Exception {

		if(absoluteFilePath == null || absoluteFilePath.length() == 0) {
			throw new Exception("Invalid PDF file absolute path (null or empty String)");
		}

		File inputPDF = new File(absoluteFilePath);
		if(!inputPDF.exists()) {
			throw new Exception("The file at: '" + absoluteFilePath + "' does not exist");
		}

		Document retDocument = null;

		byte[] originalPDF = {};
		Path filePath = Paths.get(absoluteFilePath);
		try {
			originalPDF = Files.readAllBytes(filePath);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception("Error while reading PDF file");
		}

		String fileName = (new File(absoluteFilePath)).getName().replace(".pdf", "") + "_GROBID.xml";
		retDocument = parsePDF(originalPDF, fileName);

		return retDocument;
	}


	public static Document parsePDF(File file) throws Exception {

		if(file == null) {
			throw new Exception("Invalid File object (null)");
		}

		if(!file.exists()) {
			throw new Exception("Invalid File object (does not exist)");
		}

		String absoluteFilePath = file.getAbsolutePath();

		Document retDocument = parsePDF(absoluteFilePath);

		return retDocument;
	}


	public static Document parsePDF(URL url) throws Exception {
		if(url == null) {
			throw new Exception("Invalid URL (null)");
		}

		Document retDocument = null;

		try {
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36");
			connection.setRequestProperty("Accept", "application/pdf");

			InputStream in = connection.getInputStream();

			int contentLength = connection.getContentLength();

			ByteArrayOutputStream tmpOut;
			if (contentLength != -1) {
				tmpOut = new ByteArrayOutputStream(contentLength);
			} else {
				tmpOut = new ByteArrayOutputStream(45000000);
			}

			byte[] buf = new byte[2048];
			while (true) {
				int len = in.read(buf);
				if (len == -1) {
					break;
				}
				tmpOut.write(buf, 0, len);
			}
			in.close();
			tmpOut.close();

			retDocument = parsePDF(tmpOut.toByteArray(), url.getPath().toString());

		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception("Error while instantiating Document from PDF URL");
		}

		return retDocument;
	}


	public static Document sanitizeSentences(Document gateDoc, String sentenceAnnoSet, String sentenceAnnoType) {

		String originalGROBIDannoSet = "Original markups";

		// Transfer annotations from originalGROBIDannoSet to mainAnnoSet
		GATEutils.transferAnnotations(gateDoc, "abstract", abstractAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "biblStruct", bibEntryAnnoType, originalGROBIDannoSet, mainAnnoSet, getBiblioEntries);
		GATEutils.transferAnnotations(gateDoc, "teiHeader", headerAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "head", secTitleAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "title", paperTitleAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "table", figureAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "figure", tableAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "keywords", keywordsAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "author", authorAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "affiliation", affiliationAnnoType, originalGROBIDannoSet, mainAnnoSet, null);
		GATEutils.transferAnnotations(gateDoc, "address", addressAnnoType, originalGROBIDannoSet, mainAnnoSet, null);

		// Create reference and new annotation sets
		AnnotationSet originalSentenceAnnoSet = ((sentenceAnnoSet != null && !sentenceAnnoSet.equals(""))? gateDoc.getAnnotations(sentenceAnnoSet) : gateDoc.getAnnotations());
		AnnotationSet outputSentenceAnnoSet = gateDoc.getAnnotations(FreelingParser.mainAnnSet + "_SPA");

		// Copy all the (inputSentenceAStypeAppo) sentences as annotations of type (inputSentenceAStypeAppo + "_OLD") and delete the original annotations
		Set<Integer> annIdOfOldSentences = new HashSet<Integer>();
		Set<Integer> annIdToDelete = new HashSet<Integer>();
		List<Annotation> inputSentenceAnnotations = gate.Utils.inDocumentOrder(originalSentenceAnnoSet.get(sentenceAnnoType));
		inputSentenceAnnotations.stream().forEach((ann) -> {
			if(ann != null) {
				ann.getFeatures().put("OLD_SENTENCE", "TO_DELETE");

				// Generate a copy of the sentence with type sufficed by "_OLD"
				try {
					Integer annToDeleteId = originalSentenceAnnoSet.add(ann.getStartNode().getOffset(), ann.getEndNode().getOffset(), sentenceAnnoType + "_OLD", ann.getFeatures());
					annIdOfOldSentences.add(annToDeleteId);
				} catch (InvalidOffsetException e) {
					e.printStackTrace();
				}

				// Delete the original annotation
				annIdToDelete.add(ann.getId());
			}
		});

		if(annIdToDelete != null && annIdToDelete.size() > 0) {
			for(Integer annIdToDel : annIdToDelete) {
				if(annIdToDel != null) {
					Annotation sentenceAnn = originalSentenceAnnoSet.get(sentenceAnnoType).get(annIdToDel);
					if(sentenceAnn != null) {
						originalSentenceAnnoSet.remove(sentenceAnn);
					}
				}
			}
		}


		// Adding sentences from the GROBID abstract annotations / XML elements
		List<Annotation> abstractAnnList = GATEutils.getAnnInDocOrder(gateDoc, mainAnnoSet, abstractAnnoType);
		if(abstractAnnList.size() > 0) {
			for(Annotation abstractAnn : abstractAnnList) {

				// Go through sentences overlapping annotation and add as sentences
				AnnotationSet intersectingSentences = originalSentenceAnnoSet.get(sentenceAnnoType + "_OLD", abstractAnn.getStartNode().getOffset(), abstractAnn.getEndNode().getOffset());
				Iterator<Annotation> intersectingSentencesIter = intersectingSentences.iterator();
				while(intersectingSentencesIter.hasNext()) {
					Annotation sentenceAnn = intersectingSentencesIter.next();

					FeatureMap fm = new SimpleFeatureMapImpl();
					fm.put("GROBID_from", "abstract");

					// Import PDFX features in the new sentence annotation (names prefixed by 'PDFEXT_')
					for(Map.Entry<Object, Object> entry : abstractAnn.getFeatures().entrySet()) {
						try {
							String featName = (String) entry.getKey();
							fm.put("GROBID__" + featName, entry.getValue());
						}
						catch (Exception e) {

						}
					}

					Integer newSentenceId = addSentence(gateDoc, outputSentenceAnnoSet, sentenceAnnoType, sentenceAnn.getStartNode().getOffset(), sentenceAnn.getEndNode().getOffset(), fm);
					fm.put("gateID", newSentenceId);
				}
			}
		}


		// Adding sentences from the GROBID div annotations
		Set<Annotation> extractAnnoSet = new HashSet<Annotation>();
		extractAnnoSet.addAll(GATEutils.getAnnInDocOrder(gateDoc, originalGROBIDannoSet, "body"));
		extractAnnoSet.addAll(GATEutils.getAnnInDocOrder(gateDoc, originalGROBIDannoSet, "div"));

		Set<Integer> addedSentenceIDs = new HashSet<Integer>();
		if(extractAnnoSet != null && extractAnnoSet.size() > 0) {
			Iterator<Annotation> extractAnnoSetIter = extractAnnoSet.iterator();

			while(extractAnnoSetIter.hasNext()) {
				Annotation textContentAnn = extractAnnoSetIter.next();

				// Go through sentences overlapping annotation and add as sentences
				AnnotationSet intersectingSentences = originalSentenceAnnoSet.get(sentenceAnnoType + "_OLD", textContentAnn.getStartNode().getOffset(), textContentAnn.getEndNode().getOffset());
				Iterator<Annotation> intersectingSentencesIter = intersectingSentences.iterator();
				while(intersectingSentencesIter.hasNext()) {
					Annotation sentenceAnn = intersectingSentencesIter.next();

					if(sentenceAnn == null || sentenceAnn.getId() == null) {
						continue;
					}

					// Check if the sentence is included or is equal to an header [-5, +5] chars offset
					boolean sentenceEqualToSectionTitle = false;

					List<String> headersAnnName = new ArrayList<String>();
					headersAnnName.add("H1");

					for(String headName : headersAnnName) {
						AnnotationSet headersAnns = gateDoc.getAnnotations(mainAnnoSet).get(headName);
						if(headersAnns != null && headersAnns.size() > 0) {
							Iterator<Annotation> headersAnnsIter = headersAnns.iterator();
							while(headersAnnsIter.hasNext()) {
								Annotation headersAnnsElem = headersAnnsIter.next();
								if(headersAnnsElem != null) {
									Long startOffsetHeadersAnnsElem = headersAnnsElem.getStartNode().getOffset();
									Long endOffsetHeadersAnnsElem = headersAnnsElem.getEndNode().getOffset();

									if((sentenceAnn.getStartNode().getOffset() <= startOffsetHeadersAnnsElem + 5l) && (sentenceAnn.getStartNode().getOffset() >= startOffsetHeadersAnnsElem - 5l) &&
											(sentenceAnn.getEndNode().getOffset() <= endOffsetHeadersAnnsElem + 5l) && (sentenceAnn.getEndNode().getOffset() >= endOffsetHeadersAnnsElem - 5l)) {
										sentenceEqualToSectionTitle = true;
										break;
									}
								}
							}
						}
					}

					if(sentenceEqualToSectionTitle) {
						continue;
					}

					// Check if the sentence has not been already added
					if(addedSentenceIDs.contains(sentenceAnn.getId())) {
						continue;
					}
					else {
						addedSentenceIDs.add(sentenceAnn.getId());
					}

					// Adding sentence annotation
					FeatureMap fm = new SimpleFeatureMapImpl();

					// Import PDFX features in the new sentence annotation (names prefixed by 'PDFX_')
					// boolean confidenceTextChunkPossible = false;
					for(Map.Entry<Object, Object> entry : textContentAnn.getFeatures().entrySet()) {
						try {
							String featName = (String) entry.getKey();
							fm.put("GROBID__" + featName, entry.getValue());
						}
						catch (Exception e) {

						}
					}

					Integer newSentenceId = addSentence(gateDoc, outputSentenceAnnoSet, sentenceAnnoType, sentenceAnn.getStartNode().getOffset(), sentenceAnn.getEndNode().getOffset(), fm);
					fm.put("gateID", newSentenceId);

				}
			}
		}

		// Add title as sentence
		List<Annotation> paperTitleAnnoList = GATEutils.getAnnInDocOrder(gateDoc, mainAnnoSet, paperTitleAnnoType);
		if(paperTitleAnnoList != null && paperTitleAnnoList.size() > 0) {
			for(Annotation anno : paperTitleAnnoList) {
				FeatureMap fm = new SimpleFeatureMapImpl();

				fm.put("GROBID_from", "title");

				// Import PDFX features in the new sentence annotation (names prefixed by 'PDFX_')
				// boolean confidenceTextChunkPossible = false;
				for(Map.Entry<Object, Object> entry : anno.getFeatures().entrySet()) {
					try {
						String featName = (String) entry.getKey();
						fm.put("GROBID__" + featName, entry.getValue());
					}
					catch (Exception e) {

					}
				}


				Integer newSentenceId = addSentence(gateDoc, outputSentenceAnnoSet, sentenceAnnoType, anno.getStartNode().getOffset(), anno.getEndNode().getOffset(), fm);
				fm.put("gateID", newSentenceId);
			}
		}

		// Add title as sentence
		List<Annotation> paperSecTitleAnnoList = GATEutils.getAnnInDocOrder(gateDoc, mainAnnoSet, secTitleAnnoType);
		if(paperSecTitleAnnoList != null && paperSecTitleAnnoList.size() > 0) {
			for(Annotation anno : paperSecTitleAnnoList) {
				FeatureMap fm = new SimpleFeatureMapImpl();

				fm.put("GROBID_from", "secTitle");

				// Import PDFX features in the new sentence annotation (names prefixed by 'PDFX_')
				// boolean confidenceTextChunkPossible = false;
				for(Map.Entry<Object, Object> entry : anno.getFeatures().entrySet()) {
					try {
						String featName = (String) entry.getKey();
						fm.put("GROBID__" + featName, entry.getValue());
					}
					catch (Exception e) {

					}
				}


				Integer newSentenceId = addSentence(gateDoc, outputSentenceAnnoSet, sentenceAnnoType, anno.getStartNode().getOffset(), anno.getEndNode().getOffset(), fm);
				fm.put("gateID", newSentenceId);
			}
		}

		// If there is no abstract, all sentences from the first to the beginning of the first section are abstract
		List<Annotation> abstractAnnotationList = GATEutils.getAnnInDocOrder(gateDoc, mainAnnoSet, abstractAnnoType);
		if(CollectionUtils.isEmpty(abstractAnnotationList)) {
			Optional<Annotation> h1AnnList = GATEutils.getFirstAnnotationInDocOrder(gateDoc, mainAnnoSet, secTitleAnnoType);
			if(h1AnnList.isPresent()) {
				List<Annotation> abstractSentenceList = GATEutils.getAnnInDocOrderContainedOffset(gateDoc, sentenceAnnoSet, sentenceAnnoType,
						0l, h1AnnList.get().getStartNode().getOffset());
				if(abstractSentenceList.size() > 0) {
					Long initialAbstractOffset = abstractSentenceList.get(0).getStartNode().getOffset();
					Long finalAbstractOffset = abstractSentenceList.get(abstractSentenceList.size() - 1).getEndNode().getOffset();
					try {
						gateDoc.getAnnotations(mainAnnoSet).add(initialAbstractOffset, finalAbstractOffset, abstractAnnoType, Factory.newFeatureMap());
					} catch (InvalidOffsetException e) {
						/* Do nothing */
					}
				}
			}
		}

		// Delete all sentences and header annotations that are after the first bibliographic entry or the section headers that have more than 14 tokens
		Annotation firstBibEntryAnn = GATEutils.getFirstAnnotationInDocOrder(gateDoc, mainAnnoSet, bibEntryAnnoType).orElse(null);
		if(firstBibEntryAnn != null) {
			Long firstBibEntryStartOffset = firstBibEntryAnn.getStartNode().getOffset();
			if(firstBibEntryStartOffset != null) {
				Set<Integer> annIdToRemove = new HashSet<Integer>();

				List<String> annTypesToRemove = new ArrayList<String>();
				annTypesToRemove.add(sentenceAnnoType);
				annTypesToRemove.add("H1");

				for(String annTypeToRem : annTypesToRemove) {
					List<Annotation> sentenceAnnList_1 = GATEutils.getAnnInDocOrder(gateDoc, mainAnnoSet, annTypeToRem);
					List<Annotation> sentenceAnnList_2 = GATEutils.getAnnInDocOrder(gateDoc, FreelingParser.mainAnnSet + "_SPA", annTypeToRem);
					Set<Annotation> sentenceSet = new HashSet<Annotation>();
					sentenceSet.addAll(sentenceAnnList_1);
					sentenceSet.addAll(sentenceAnnList_2);
					for(Annotation sent : sentenceAnnList_2) {
						if(sent != null && sent.getStartNode().getOffset() >= firstBibEntryStartOffset && sent.getId() != null) {
							annIdToRemove.add(sent.getId());
						}
					}
				}

				if(annIdToRemove.size() > 0) {
					for(Integer annIdToRem : annIdToRemove) {
						Annotation annToRem = gateDoc.getAnnotations(mainAnnoSet).get(annIdToRem);
						if(annToRem != null) {
							gateDoc.getAnnotations(mainAnnoSet).remove(annToRem);
						}

						annToRem = gateDoc.getAnnotations(sentenceAnnoSet).get(annIdToRem);
						if(annToRem != null) {
							gateDoc.getAnnotations(FreelingParser.mainAnnSet + "_SPA").remove(annToRem);
						}
					}
				}

			}
		}

		// Remove all the annotation ids of sentences to remove - with type sentenceAnnoType + "_OLD"		
		if(annIdOfOldSentences != null && annIdOfOldSentences.size() > 0) {
			for(Integer annIdOfOldSent : annIdOfOldSentences) {
				if(annIdOfOldSent != null) {
					Annotation sentenceAnn = originalSentenceAnnoSet.get(sentenceAnnoType + "_OLD").get(annIdOfOldSent);
					if(sentenceAnn != null) {
						originalSentenceAnnoSet.remove(sentenceAnn);
					}
				}
			}
		}

		return gateDoc;
	}


	/**
	 * Internal utility function to add a sentence annotation of type outputAStypeAppo to the annotation sets outputAs
	 * and outputAsOriginal, starting at startNode, ending at endNode and with features map equal to fm.
	 * 
	 * This method, before adding a new sentence annotation, performs the following steps:
	 *    - removes the header of section eventually included in the sentence
	 *    - left and right trims the sentence span
	 *    - check for duplicated sentence annotations
	 * 
	 * @param doc
	 * @param outputAs
	 * @param outputAsOriginal
	 * @param outputAStypeAppo
	 * @param startNode
	 * @param endNode
	 * @param fm
	 * @return
	 */
	private static Integer addSentence(Document doc, AnnotationSet outputAs, String outputAStypeAppo, Long startNode, Long endNode, FeatureMap fm) {
		Integer newSentId = null;

		try {
			// Check if not header in sentence - if header in sentence, remove it
			List<String> headersAnnName = new ArrayList<String>();
			headersAnnName.add(paperTitleAnnoType);
			headersAnnName.add(secTitleAnnoType);
			headersAnnName.add(affiliationAnnoType);
			headersAnnName.add(authorAnnoType);
			headersAnnName.add(addressAnnoType);
			headersAnnName.add(keywordsAnnoType);
			headersAnnName.add(figureAnnoType);
			headersAnnName.add(tableAnnoType);

			for(String headName : headersAnnName) {
				AnnotationSet headersAnns = doc.getAnnotations(mainAnnoSet).get(headName, startNode, endNode);
				if(headersAnns != null && headersAnns.size() > 0) {
					Iterator<Annotation> headersAnnsIter = headersAnns.iterator();
					while(headersAnnsIter.hasNext()) {
						Annotation headersAnnsElem = headersAnnsIter.next();
						if(headersAnnsElem != null) {
							Long endOffsetHeadersAnnsElem = headersAnnsElem.getEndNode().getOffset();
							if(endOffsetHeadersAnnsElem != null && endOffsetHeadersAnnsElem > startNode && endOffsetHeadersAnnsElem < endNode) {
								startNode = endOffsetHeadersAnnsElem;
							}

							Long startOffsetHeadersAnnsElem = headersAnnsElem.getStartNode().getOffset();
							if(startOffsetHeadersAnnsElem != null && startOffsetHeadersAnnsElem > startNode && startOffsetHeadersAnnsElem < endNode) {
								endNode = startOffsetHeadersAnnsElem;
							}
						}
					}
				}
			}

			// Trim the new sentence annotation
			try {
				String sentContent = doc.getContent().getContent(startNode, endNode).toString();
				if(sentContent != null && sentContent.length() > 0) {
					for(int i = 0; i < sentContent.length(); i++) {
						char ch = sentContent.charAt(i);
						if(ch == ' ' || ch == '\n' || ch == '\t') {
							if(startNode < (endNode - 1)) {
								startNode = startNode + 1;
							}
						}
						else {
							break;
						}
					}

					for(int i = (sentContent.length() - 1); i >= 0; i--) {
						char ch = sentContent.charAt(i);
						if(ch == ' ' || ch == '\n' || ch == '\t') {
							if(startNode < (endNode - 1)) {
								endNode = endNode - 1;
							}
						}
						else {
							break;
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			// Check if the sentence is at least 10 chars long
			boolean tooFewChars = false;
			if( (endNode - startNode) < 10l) {
				tooFewChars = true;
			}

			// Check if the same sentence annotation was not already present, in order not to duplicate it
			boolean alreadyCreatedSentence = false;
			AnnotationSet asCheck = outputAs.getCovering(outputAStypeAppo, startNode, endNode);
			if(asCheck != null && asCheck.size() > 0) {
				alreadyCreatedSentence = true;
			}

			if(!tooFewChars && !alreadyCreatedSentence) {	
				newSentId = outputAs.add(startNode, endNode, outputAStypeAppo, fm);
			}

		} catch (InvalidOffsetException e) {
			logger.error("ERROR, InvalidOffsetException - " + e.getLocalizedMessage());
			e.printStackTrace();
		}

		return newSentId;
	}

	private static Predicate<Annotation> getBiblioEntries = (Annotation ann) -> {
		if(ann != null && ann.getFeatures() != null && ann.getFeatures().containsKey("xml:id")) {
			return true;
		}
		return false;
	};

}

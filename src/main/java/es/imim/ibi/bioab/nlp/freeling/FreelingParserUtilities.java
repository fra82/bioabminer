/**
 * BioAB Miner: Biomedical Abbreviations Miner
 */
package es.imim.ibi.bioab.nlp.freeling;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.backingdata.gateutils.GATEfiles;
import org.backingdata.gateutils.generic.GenericUtil;

import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.util.GateException;

/**
 * Collection of static method to parse texts by means of es.imim.ibi.bioab.nlp.FreelingParser
 *  
 * @author Francesco Ronzano
 *
 */
public class FreelingParserUtilities {


	/**
	 * Read all the GATE XML files contained in the rootFolder and its sub-folder and having name ending in 
	 * fileNameEndingFilter (if specified) and process their content by means of the FreelingParser in the specified language.
	 * 
	 * It is possible to parse all the GATE document annotations of a given annotation set and type treated as single sentences
	 * by specifying the annotation set (sentenceAnnSet) and the annotation type (sentenceAnnType)
	 * 
	 * The results of GATE XML document analysis are stored to a file in the same path of the original one and with the name of the
	 * original file suffixed by the string "_FreelingParsed.xml".
	 * 
	 * You need to have the GATE framework initialized to execute this method. To this purpose, you can use the method 
	 * org.backingdata.gateutils.GATEinit.initGate(homeFolder, pluginsFolder), by specifying the full local path of the GATE.
	 * 
	 * @param rootFolder full path of the folder to look for all GATE XML files to parse by Freeling (search is performed in this folder and in all the folders of its subtree)
	 * @param fileNameEndingFilter if not null or empty, filter files by selecting only the ones ending with this string
	 * @param sentenceAnnSet if not null, in each GATE XML document only the annotations of the GATE annotation set with this name are processed
	 * @param sentenceAnnType if not null (and sentenceAnnSet is not null) in each GATE XML document only the annotation type with this name and belonging
	 * to the annotation set (sentenceAnnSet) are parsed by Freeling as single sentences
	 * @param documentLanguage one of "SPA" / "ENG" / "CAT"
	 * 
	 * @throws GateException 
	 */
	public static void parseAllGATE_XMLfilesInFolder(String rootFolder, String fileNameEndingFilter, 
			String sentenceAnnSet, String sentenceAnnType, String documentLanguage) throws GateException {

		if(StringUtils.isEmpty(rootFolder)) {
			System.out.println("Empty or null rootFolder path");
			return;
		}

		File rootFolderFile = new File(rootFolder);
		if(rootFolderFile == null || !rootFolderFile.exists() || !rootFolderFile.isDirectory()) {
			System.out.println("Not existing root folder (" + rootFolder + ")");
			return;
		}

		if(documentLanguage == null) {
			System.out.println("Null document language");
			return;
		}

		IOFileFilter fileFilter = TrueFileFilter.INSTANCE;
		if(!StringUtils.isEmpty(fileNameEndingFilter)) {
			fileFilter = new SuffixFileFilter(".java");
		}

		if(!Gate.isInitialised()) {
			System.out.println("GATE is not initialized. Before invoking this method, please initialize GATE. To this purpose you can also use the "
					+ "method org.backingdata.gateutils.GATEinit.initGate(homeFolder, pluginsFolder), by specifying the full local path of the GATE home and plugin folders.");
			return;
		}

		FeatureMap FreelingParserfm = Factory.newFeatureMap();
		FreelingParserfm.put("analysisLang", documentLanguage);
		if(!StringUtils.isEmpty(sentenceAnnSet) && !StringUtils.isEmpty(sentenceAnnType)) {
			FreelingParserfm.put("sentenceAnnotationSetToAnalyze", sentenceAnnSet);
			FreelingParserfm.put("sentenceAnnotationTypeToAnalyze", sentenceAnnType);
		}
		FreelingParser FreelingParser_Resource = null;
		Gate.getCreoleRegister().registerComponent(FreelingParser.class);
		FreelingParser_Resource = (FreelingParser) gate.Factory.createResource(FreelingParser.class.getName(), FreelingParserfm);

		FreelingParser_Resource.init();

		List<File> fileCollection = (List<File>) FileUtils.listFiles(rootFolderFile, fileFilter, TrueFileFilter.INSTANCE);

		int fileCorrectlyParser = 0;
		for(int fileIndex = 0; fileIndex < fileCollection.size(); fileIndex++) {
			File fileToParse = fileCollection.get(fileIndex);

			System.out.println("Start processing GATE XML file: " + fileToParse.getName() + " (" + fileIndex + " file over " + fileCollection.size() + " files to process)");

			try {
				Document gateDoc = GATEfiles.loadGATEfromXMLfile(fileToParse.getAbsolutePath());

				FreelingParser_Resource.setDocument(gateDoc);
				FreelingParser_Resource.execute();
				FreelingParser_Resource.setDocument(null);

				GATEfiles.storeGateXMLToFile(gateDoc, fileToParse.getAbsolutePath().replace(".xml", "") + "_FreelingParsed.xml");

				fileCorrectlyParser++;
			}
			catch(Exception e) {
				System.out.println("Exception parsing file: " + ((fileToParse != null) ? fileToParse.getName() : "NULL"));
				e.printStackTrace();
			}
		}

		System.out.println("Correctly parsed " + fileCorrectlyParser + " files over " + fileCollection.size());
	}


	/**
	 * Read all the TXT files (UFT-8) contained in the rootFolder and its sub-folder and having name ending in 
	 * fileNameEndingFilter (if specified) and process their content by means of the FreelingParser in the specified language.
	 * 
	 * The results of TXT document analysis are stored to a GATE XML file in the same path of the original one and with name of the original
	 * file suffixed by the string "_FreelingParsed.xml".
	 * 
	 * You need to have the GATE framework initialized to execute this method. To this purpose, you can use the method 
	 * org.backingdata.gateutils.GATEinit.initGate(homeFolder, pluginsFolder), by specifying the full local path of the GATE.
	 * 
	 * @param rootFolder full path of the folder to look for all GATE XML files to parse by Freeling (search is performed in this folder and in all the folders of its subtree)
	 * @param fileNameEndingFilter if not null or empty, filter files by selecting only the ones ending with this string
	 * @param documentLanguage one of "SPA" / "ENG" / "CAT"
	 * @throws GateException
	 */
	public static void parseAllTXTfilesInFolder(String rootFolder, String fileNameEndingFilter, String documentLanguage) throws GateException {

		if(StringUtils.isEmpty(rootFolder)) {
			System.out.println("Empty or null rootFolder path");
			return;
		}

		File rootFolderFile = new File(rootFolder);
		if(rootFolderFile == null || !rootFolderFile.exists() || !rootFolderFile.isDirectory()) {
			System.out.println("Not existing root folder (" + rootFolder + ")");
			return;
		}

		if(documentLanguage == null) {
			System.out.println("Null document language");
			return;
		}

		IOFileFilter fileFilter = TrueFileFilter.INSTANCE;
		if(!StringUtils.isEmpty(fileNameEndingFilter)) {
			fileFilter = new SuffixFileFilter(".java");
		}

		if(!Gate.isInitialised()) {
			System.out.println("GATE is not initialized. Before invoking this method, please initialize GATE. To this purpose you can also use the "
					+ "method org.backingdata.gateutils.GATEinit.initGate(homeFolder, pluginsFolder), by specifying the full local path of the GATE home and plugin folders.");
			return;
		}

		FeatureMap FreelingParserfm = Factory.newFeatureMap();
		FreelingParserfm.put("analysisLang", documentLanguage);
		FreelingParser FreelingParser_Resource = null;
		Gate.getCreoleRegister().registerComponent(FreelingParser.class);
		FreelingParser_Resource = (FreelingParser) gate.Factory.createResource(FreelingParser.class.getName(), FreelingParserfm);

		FreelingParser_Resource.init();

		List<File> fileCollection = (List<File>) FileUtils.listFiles(rootFolderFile, fileFilter, TrueFileFilter.INSTANCE);

		int fileCorrectlyParser = 0;
		for(int fileIndex = 0; fileIndex < fileCollection.size(); fileIndex++) {
			File fileToParse = fileCollection.get(fileIndex);

			System.out.println("Start processing TXT file: " + fileToParse.getName() + " (" + fileIndex + " file over " + fileCollection.size() + " files to process)");

			try {
				Document gateDoc = gate.Factory.newDocument(GenericUtil.readUTF8stringFromFile(fileToParse.getAbsolutePath()));

				FreelingParser_Resource.setDocument(gateDoc);
				FreelingParser_Resource.execute();
				FreelingParser_Resource.setDocument(null);

				GATEfiles.storeGateXMLToFile(gateDoc, fileToParse.getAbsolutePath().replace(".txt", "") + "_FreelingParsed.xml");

				fileCorrectlyParser++;
			}
			catch(Exception e) {
				System.out.println("Exception parsing file: " + ((fileToParse != null) ? fileToParse.getName() : "NULL"));
				e.printStackTrace();
			}
		}

		System.out.println("Correctly parsed " + fileCorrectlyParser + " files over " + fileCollection.size());
	}

	/**
	 * Parse an UTF-8 string by Freeling and return the results as a gate.Document instance.
	 * 
	 * You need to have the GATE framework initialized to execute this method. To this purpose, you can use the method 
	 * org.backingdata.gateutils.GATEinit.initGate(homeFolder, pluginsFolder), by specifying the full local path of the GATE.
	 * 
	 * @param text
	 * @param documentLanguage
	 * @return
	 * @throws GateException 
	 */
	public static Document parseText(String text, String documentLanguage) throws GateException {

		if(StringUtils.isEmpty(text)) {
			System.out.println("Empty or null text to parse");
			return null;
		}
		
		if(documentLanguage == null) {
			System.out.println("Null document language");
			return null;
		}

		if(!Gate.isInitialised()) {
			System.out.println("GATE is not initialized. Before invoking this method, please initialize GATE. To this purpose you can also use the "
					+ "method org.backingdata.gateutils.GATEinit.initGate(homeFolder, pluginsFolder), by specifying the full local path of the GATE home and plugin folders.");
			return null;
		}

		FeatureMap FreelingParserfm = Factory.newFeatureMap();
		FreelingParserfm.put("analysisLang", documentLanguage);
		FreelingParser FreelingParser_Resource = null;
		Gate.getCreoleRegister().registerComponent(FreelingParser.class);
		FreelingParser_Resource = (FreelingParser) gate.Factory.createResource(FreelingParser.class.getName(), FreelingParserfm);

		FreelingParser_Resource.init();


		try {
			Document gateDoc = gate.Factory.newDocument(text);

			FreelingParser_Resource.setDocument(gateDoc);
			FreelingParser_Resource.execute();
			FreelingParser_Resource.setDocument(null);

			return gateDoc;
		}
		catch(Exception e) {
			System.out.println("Exception parsing text.");
			e.printStackTrace();
		}
		
		return null;
	} 

}

package es.imim.ibi.bioab.exec.resource;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.gateutils.generic.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.nlp.freeling.FreelingParser;
import es.imim.ibi.bioab.nlp.mate.MateParser;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;


/**
 * Link the abbreviations with the corresponding Long Form if any nit he same sentence (rule-based approach).
 * 
 * @author Francesco Ronzano
 */
@CreoleResource(name = "BioAB Abbreviation Long Form Spotter Module")
public class BioABabbrvLFspotter extends AbstractLanguageAnalyser implements ProcessingResource, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LoggerFactory.getLogger(BioABabbrvLFspotter.class);

	public static final String mainAnnSet = "BioAB";
	
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

		


		long needed = System.currentTimeMillis() - t1;
		logger.debug("   - End tagging document: " + (((this.document.getName() != null) ? this.document.getName() : "NULL")));
		logger.debug("     in (seconds): " + (needed / 1000));
		logger.debug("********************************************");
	}
	

	public boolean resetAnnotations() {
		// TO IMPLEMENT
		return true;
	}

	public static void main(String[] args) {

	}


}
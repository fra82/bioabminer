/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.feature.generator;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Annotation;
import gate.Document;
import gate.Factory;

/**
 * Check if the token string is in a list of words
 * 
 * @author Francesco Ronzano
 *
 */
public class StringInList implements FeatCalculator<Double, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(StringInList.class);

	private Integer relativePosition = -1;
	private String mavenResourcePath = "";
	private Boolean isCaseInsensitive = false;
	private boolean sentenceScoped = false;
	private Set<String> wordList = new HashSet<String>();

	public StringInList() {
		// Do not use this constructor
	}

	public StringInList(Integer relPosition, String mvnPath, boolean isCasInsens, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.mavenResourcePath = mvnPath;
		this.isCaseInsensitive = isCasInsens;
		this.sentenceScoped = sentScoped;

		// Load list
		StringInList instanceCr = new StringInList();
		BufferedReader br = null;
		try {

			br = new BufferedReader(new InputStreamReader(instanceCr.getClass().getResourceAsStream(this.mavenResourcePath), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				wordList.add( (isCaseInsensitive) ? line.toLowerCase() : line );
			}

			logger.info("Loaded list of length: " + wordList.size() + " from: " + ((this.mavenResourcePath != null) ? this.mavenResourcePath : "NULL"));
		} catch (Exception e) {
			logger.error("IMPOSSIBLE TO LOAD LIST OF STRINGS FROM " + ((this.mavenResourcePath != null) ? this.mavenResourcePath : "NULL"));
			e.printStackTrace();
		}

	}

	@Override
	public MyDouble calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyDouble retValue = new MyDouble(-1d);

		if(doc != null && ctxDoc != null && relativePosition != null) {
			Annotation tokenAnnToConsider = null;
			if(relativePosition == 0) {
				tokenAnnToConsider = ctxDoc.getCoreTokenAnn();
			}
			else {
				int indexOfTokenPositionZero = ctxDoc.getDocumentTokenList().indexOf(ctxDoc.getCoreTokenAnn());
				int newPosition = indexOfTokenPositionZero + relativePosition;

				if(newPosition >= 0 && newPosition < ctxDoc.getDocumentTokenList().size()) {
					tokenAnnToConsider = ctxDoc.getDocumentTokenList().get(newPosition);
				}
			}

			if(tokenAnnToConsider != null) {
				Integer sentenceIDofTokenAnnToConsider = (ctxDoc.getTokenIDtoSentenceIDmap().containsKey(tokenAnnToConsider.getId())) ? ctxDoc.getTokenIDtoSentenceIDmap().get(tokenAnnToConsider.getId()) : null; 
				if(!sentenceScoped || (sentenceScoped && sentenceIDofTokenAnnToConsider != null && sentenceIDofTokenAnnToConsider.equals(ctxDoc.getGATEsentenceID()))) {
					retValue.setValue(0d);
					
					String text = GATEutils.getAnnotationText(tokenAnnToConsider, doc).orElse(null);
					try {
						if(wordList.contains((isCaseInsensitive) ? text.toLowerCase() : text)) {
							retValue.setValue(Double.valueOf(1d));
						}
					} catch (Exception e) {
						logger.warn("Error while checking list presence (" + 
								((this.mavenResourcePath != null) ? this.mavenResourcePath : "NULL") + ") with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
					}
				}
			}
		}
		else {
			logger.warn("Error while computing in list presence (" + 
					((this.mavenResourcePath != null) ? this.mavenResourcePath : "NULL") + ")  with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}

		return retValue;
	}

}

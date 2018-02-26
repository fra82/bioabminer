/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.feature.generator;


import org.backingdata.gateutils.GATEutils;
import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Annotation;
import gate.Document;
import gate.Factory;

/**
 * Get the feature value of the token annotation in the context (surrounding tokens)
 * 
 * @author Francesco Ronzano
 *
 */
public class FeatureValueOfContext implements FeatCalculator<String, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(FeatureValueOfContext.class);

	private Integer relativePosition = -1;
	private String featureExtractName = "";
	private boolean sentenceScoped = false;

	public FeatureValueOfContext(Integer relPosition, String featureToExtractName, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.featureExtractName = featureToExtractName;
	}

	@Override
	public MyString calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyString retValue = new MyString("UNDEFINED");

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
					try {
						retValue.setValue("__NO_FEAT");
						
						String featureVal = "";
						if(this.featureExtractName != null) {
							featureVal = GATEutils.getStringFeature(tokenAnnToConsider, this.featureExtractName).orElse(null);
						}
						else {
							featureVal = GATEutils.getAnnotationText(tokenAnnToConsider, ctxDoc.getGateDoc()).orElse(null);						
						}
						
						if(featureVal != null && featureVal.length() > 0) {
							retValue.setValue(featureVal);
						}
					} catch (Exception e) {
						logger.warn("Error while computing " +  this.featureExtractName + " of context with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
					}
				}
			}
		}
		else {
			logger.warn("Error (" +  this.featureExtractName + ") with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
		}
		
		// Transform POS to universal ones
		/*
		if(this.featureExtractName != null && retValue != null && retValue.getValue() != null && this.featureExtractName.equals(TokenAnnConst.tokenPOSFeat)) {
			UniversalPOStag univPOS = null;
			if(FeatureGenerator.langExec.equals(LangENUM.English)) {
				univPOS = UniversalPOSmapper.fromEnglishToUniversal(retValue.getValue());
			}
			else if(FeatureGenerator.langExec.equals(LangENUM.Spanish)) {
				univPOS = UniversalPOSmapper.fromSpanishToUniversal(retValue.getValue());
			}
			
			if(univPOS != null) {
				retValue.setValue(univPOS.toString());
			}
		}
		*/
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}

		return retValue;
	}

}

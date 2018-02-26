/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.feature.generator;


import java.util.List;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.TokenAnnConst;
import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Annotation;
import gate.Document;
import gate.Factory;

/**
 * Count how many time a token with that feature is present in the document
 * 
 * @author Francesco Ronzano
 *
 */
public class FeatureRepetitionsOfContext implements FeatCalculator<Double, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(FeatureRepetitionsOfContext.class);

	private Integer relativePosition = -1;
	private String featureExtractName = "";
	private boolean sentenceScoped = false;

	public FeatureRepetitionsOfContext(Integer relPosition, String featureToExtractName, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.featureExtractName = featureToExtractName;
		this.sentenceScoped = sentScoped;
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
					try {
						retValue.setValue(0d);
						
						String featureVal = "";
						if(this.featureExtractName != null) {
							featureVal = GATEutils.getStringFeature(tokenAnnToConsider, this.featureExtractName).orElse(null);
						}
						else {
							featureVal = GATEutils.getAnnotationText(tokenAnnToConsider, ctxDoc.getGateDoc()).orElse(null);						
						}
						
						if(featureVal != null && featureVal.length() > 0) {
							List<Annotation> getAllDocTokens = GATEutils.getAnnInDocOrder(doc, TokenAnnConst.tokenAnnSet, TokenAnnConst.tokenType);
							double counterOfRepetitions = 0d;
							for(Annotation tokAnn : getAllDocTokens) {
								String featureValInt = GATEutils.getStringFeature(tokAnn, this.featureExtractName).orElse(null);
								if(this.featureExtractName != null) {
									featureValInt = GATEutils.getStringFeature(tokAnn, this.featureExtractName).orElse(null);
								}
								else {
									featureValInt = GATEutils.getAnnotationText(tokAnn, ctxDoc.getGateDoc()).orElse(null);						
								}
								
								if(featureValInt != null && featureValInt.equals(featureVal) && !tokAnn.getId().equals(tokenAnnToConsider.getId())) {
									counterOfRepetitions = counterOfRepetitions + 1d;
								}
							}
							retValue.setValue(counterOfRepetitions);
						}
					} catch (Exception e) {
						logger.warn("Error while computing POS of context with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
					}
				}
			}
		}
		else {
			logger.warn("Error with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}

		return retValue;
	}

}

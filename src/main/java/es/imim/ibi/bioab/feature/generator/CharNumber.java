package es.imim.ibi.bioab.feature.generator;


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
 * Compute the number of chars of the token
 * 
 * @author Francesco Ronzano
 *
 */
public class CharNumber implements FeatCalculator<Double, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(CharNumber.class);

	private Integer relativePosition = -1;
	private boolean sentenceScoped = false;

	public CharNumber(Integer relPosition, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.sentenceScoped = sentScoped;
	}

	@Override
	public MyDouble calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyDouble retValue = new MyDouble(null);

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
						retValue.setValue(Double.valueOf(text.length()));
					} catch (Exception e) {
						logger.warn("Error while computing char number with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
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

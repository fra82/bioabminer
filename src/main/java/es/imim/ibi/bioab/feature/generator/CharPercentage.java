package es.imim.ibi.bioab.feature.generator;


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
 * Compute the percentage of characters of a certain type
 * 
 * @author Francesco Ronzano
 *
 */
public class CharPercentage implements FeatCalculator<Double, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(CharPercentage.class);

	public enum PercentageType {
		UPPERCASE, NUMERIC, PUNCTUATION;
	} 

	private Integer relativePosition = -1;
	private PercentageType percentageType = null;
	private boolean sentenceScoped = false;

	public CharPercentage(Integer relPosition, PercentageType percType, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.percentageType = percType;
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
						Double totChars = Double.valueOf(text.length()); 
						Double numMatchingChars = 0d;
						switch(this.percentageType) {
						case UPPERCASE:
							for (int k = 0; k < text.length(); k++) {
								if (Character.isUpperCase(text.charAt(k))) {
									numMatchingChars = numMatchingChars + 1d;
								}
							}
							break;

						case NUMERIC:
							for (int k = 0; k < text.length(); k++) {
								if (Character.isDigit(text.charAt(k))) {
									numMatchingChars = numMatchingChars + 1d;
								}
							}
							break;

						case PUNCTUATION:
							for (int k = 0; k < text.length(); k++) {
								if (TokenAnnConst.punctutations.contains(String.valueOf(text.charAt(k)))) {
									numMatchingChars = numMatchingChars + 1d;
								}
							}
							break;
						}

						retValue.setValue((totChars > 0d) ? numMatchingChars / totChars : 0d);

					} catch (Exception e) {
						logger.warn("Error while computing percentage type " + this.percentageType + " number with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
					}
				}
			}
		}
		else {
			logger.warn("Error percentage type " + this.percentageType + " with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}

		return retValue;
	}

}

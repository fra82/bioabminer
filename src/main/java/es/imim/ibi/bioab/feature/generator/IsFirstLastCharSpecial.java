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
 * Compute the percentage of characters of a certain type in a token
 * 
 * @author Francesco Ronzano
 *
 */
public class IsFirstLastCharSpecial implements FeatCalculator<Double, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(IsFirstLastCharSpecial.class);

	public enum CharCheckType {
		UPPERCASE, NUMERIC, PUNCTUATION;
	} 

	public enum CharConsideredType {
		FIRST, LAST;
	} 

	private Integer relativePosition = -1;
	private CharCheckType charCheckType = null;
	private CharConsideredType charConsideredType = null;
	private boolean sentenceScoped = false;

	public IsFirstLastCharSpecial(Integer relPosition, CharCheckType cCheckType, CharConsideredType cConsideredType, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.charCheckType = cCheckType;
		this.charConsideredType = cConsideredType;
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
					retValue.setValue(0d);
					
					String text = GATEutils.getAnnotationText(tokenAnnToConsider, doc).orElse(null);

					try {
						char charConsidered = ' ';

						switch(this.charConsideredType) {
						case FIRST:
							charConsidered = text.charAt(0);
							break;
						case LAST:
							charConsidered = text.charAt(text.length() - 1);
							break;
						}

						switch(this.charCheckType) {
						case UPPERCASE:
							if (charConsidered != ' ' && Character.isUpperCase(charConsidered)) {
								retValue.setValue(1d);
							}
							break;

						case NUMERIC:
							if (charConsidered != ' ' && Character.isDigit(charConsidered)) {
								retValue.setValue(1d);
							}
							break;

						case PUNCTUATION:
							if (charConsidered != ' ' && TokenAnnConst.punctutations.contains(String.valueOf(charConsidered))) {
								retValue.setValue(1d);
							}
							break;
						}

					} catch (Exception e) {
						logger.warn("Error while computing " + this.charConsideredType + " char of type " + this.charCheckType + " number with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
					}
				}
			}
		}
		else {
			logger.warn("Error percentage type computing " + this.charConsideredType + " char of type " + this.charCheckType + " with relative position: " + ((relativePosition != null) ? relativePosition : "NULL"));
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}
		
		return retValue;
	}

}

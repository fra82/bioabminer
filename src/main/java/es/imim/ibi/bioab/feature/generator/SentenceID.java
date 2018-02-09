package es.imim.ibi.bioab.feature.generator;


import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Document;
import gate.Factory;

/**
 * Get the sentence ID string of the sentence the token belongs to
 * 
 * @author Francesco Ronzano
 *
 */
public class SentenceID implements FeatCalculator<String, Document, TokenFeatureGenerationContext> {
	
	private static Logger logger = LoggerFactory.getLogger(SentenceID.class);
	
	@Override
	public MyString calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyString retValue = new MyString("__NO_SENTID_");
		
		if(doc != null && ctxDoc != null && ctxDoc.getSentenceID() != null && !ctxDoc.getSentenceID().trim().equals("")) {
			retValue.setValue(ctxDoc.getSentenceID());
		}
		else {
			logger.warn("Error: impossible to define sentence ID");
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}
		
		return retValue;
	}
	
}

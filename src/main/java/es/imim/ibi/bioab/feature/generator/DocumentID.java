/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.feature.generator;


import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Document;
import gate.Factory;

/**
 * Get the Document ID string
 * 
 * @author Francesco Ronzano
 *
 */
public class DocumentID implements FeatCalculator<String, Document, TokenFeatureGenerationContext> {
	
	private static Logger logger = LoggerFactory.getLogger(DocumentID.class);
	
	@Override
	public MyString calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyString retValue = new MyString("__NO_DOCID_");
		
		if(doc != null && ctxDoc != null && ctxDoc.getDocumentID() != null && !ctxDoc.getDocumentID().trim().equals("")) {
			retValue.setValue(ctxDoc.getDocumentID());
		}
		else {
			logger.warn("Error: impossible to define document ID");
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}
		
		return retValue;
	}
	
}

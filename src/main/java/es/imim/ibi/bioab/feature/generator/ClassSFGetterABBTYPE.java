/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.feature.generator;

import java.util.ArrayList;
import java.util.List;

import org.backingdata.gateutils.GATEutils;
import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyString;

import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Annotation;
import gate.Document;
import gate.Factory;

/**
 * Get the class: SHORT, MULTIPLE, GLOBAL, CONTEXTUAL, DERIVED of a token
 * 
 * @author Francesco Ronzano
 *
 */
public class ClassSFGetterABBTYPE implements FeatCalculator<String, Document, TokenFeatureGenerationContext> {

	public enum AbbreviationType {
		SHORT, MULTIPLE, GLOBAL, DERIVED; // Not considered CONTEXTUAL
	} 

	public ClassSFGetterABBTYPE() {
		super();
	}

	@Override
	public MyString calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyString retValue = new MyString("NONE");

		if(doc != null && ctxDoc != null) {
			Annotation coreTokenAnn = ctxDoc.getCoreTokenAnn();

			List<Annotation> SHORTintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "SHORT", coreTokenAnn);

			List<Annotation> MULTIPLEintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "MULTIPLE", coreTokenAnn);

			List<Annotation> GLOBALintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "GLOBAL", coreTokenAnn);

			// List<Annotation> CONTEXTUALintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "CONTEXTUAL", coreTokenAnn);

			List<Annotation> DERIVEDintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "DERIVED", coreTokenAnn);

			List<Annotation> globalShortFormList = new ArrayList<Annotation>();
			globalShortFormList.addAll(SHORTintersectList);
			globalShortFormList.addAll(MULTIPLEintersectList);
			globalShortFormList.addAll(GLOBALintersectList);
			// globalShortFormList.addAll(CONTEXTUALintersectList);
			globalShortFormList.addAll(DERIVEDintersectList);

			if(globalShortFormList.size() > 0) {
				if(SHORTintersectList.size() > 0) {
					retValue.setValue("SHORT");
				}
				else if(MULTIPLEintersectList.size() > 0) {
					retValue.setValue("MULTIPLE");
				}
				else if(GLOBALintersectList.size() > 0) {
					retValue.setValue("GLOBAL");
				}
				else if(DERIVEDintersectList.size() > 0) {
					retValue.setValue("DERIVED");
				}
				else {
					retValue.setValue("NONE");
				}
				
				// Not considered CONTEXTUAL
				// else if(CONTEXTUALintersectList.size() > 0) {
				//	retValue.setValue("CONTEXTUAL");
				// }
			}

		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}

		return retValue;
	}

}

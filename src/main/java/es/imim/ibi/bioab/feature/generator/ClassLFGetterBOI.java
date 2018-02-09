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
 * Get the class: B, I, O of a LONG form token
 * 
 * @author Francesco Ronzano
 *
 */
public class ClassLFGetterBOI implements FeatCalculator<String, Document, TokenFeatureGenerationContext> {
	
	public enum AbbreviationType {
		LONG, SF_LF;
	} 
	
	private AbbreviationType abbrvType = null;
	
	
	public ClassLFGetterBOI(AbbreviationType abbrvType) {
		super();
		this.abbrvType = abbrvType;
	}

	@Override
	public MyString calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyString retValue = new MyString("O");
		
		if(doc != null && ctxDoc != null) {
			Annotation coreTokenAnn = ctxDoc.getCoreTokenAnn();
			
			List<Annotation> LONGintersectList = new ArrayList<Annotation>();
			if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.LONG)) {
				LONGintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "LONG", coreTokenAnn);
			}
			
			if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.SF_LF)) {
				LONGintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "SF - LF", coreTokenAnn);
			}
			
			List<Annotation> globalLongFormList = new ArrayList<Annotation>();
			globalLongFormList.addAll(LONGintersectList);
			
			boolean isCoreTokenAbbrv = (globalLongFormList.size() > 0) ? true : false;
			
			int coreTokenIndex = ctxDoc.getDocumentTokenList().indexOf(coreTokenAnn);
			
			boolean isPrevCoreTokenAbbrv = false;
			if(coreTokenIndex > 0) {
				int previousTokenIndex = coreTokenIndex - 1;
				Annotation prevCoreToken = ctxDoc.getDocumentTokenList().get(previousTokenIndex);
				
				for(Annotation longFormAnn : globalLongFormList) {
					if(longFormAnn.overlaps(coreTokenAnn) && longFormAnn.overlaps(prevCoreToken)) {
						isPrevCoreTokenAbbrv = true;
					}
				}
			}
			
			if(isPrevCoreTokenAbbrv && isCoreTokenAbbrv) {
				retValue.setValue("I");
			}
			else if(!isPrevCoreTokenAbbrv && isCoreTokenAbbrv) {
				retValue.setValue("B");
			}
			else if(!isCoreTokenAbbrv) {
				retValue.setValue("O");
			}
			
		}
		
		if(TokenFeatureGenerationContext.reportFeatInGate) {
			ctxDoc.getCoreTokenAnn().setFeatures((ctxDoc.getCoreTokenAnn().getFeatures() != null) ? ctxDoc.getCoreTokenAnn().getFeatures() : Factory.newFeatureMap());
			ctxDoc.getCoreTokenAnn().getFeatures().put("FEAT_" + featName, ((retValue.getValue() != null) ? retValue.getValue() : "") + "");
		}
		
		return retValue;
	}
	
}

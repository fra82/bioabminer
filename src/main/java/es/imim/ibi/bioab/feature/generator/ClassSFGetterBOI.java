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
 * Get the class: B, I, O of a SHORT form token
 * 
 * @author Francesco Ronzano
 *
 */
public class ClassSFGetterBOI implements FeatCalculator<String, Document, TokenFeatureGenerationContext> {
	
	public enum AbbreviationType {
		SHORT, MULTIPLE, GLOBAL, DERIVED; // Not considered CONTEXTUAL
	} 
	
	private AbbreviationType abbrvType = null;
	
	
	public ClassSFGetterBOI(AbbreviationType abbrvType) {
		super();
		this.abbrvType = abbrvType;
	}

	@Override
	public MyString calculateFeature(Document doc, TokenFeatureGenerationContext ctxDoc, String featName) {
		MyString retValue = new MyString("O");
		
		if(doc != null && ctxDoc != null) {
			Annotation coreTokenAnn = ctxDoc.getCoreTokenAnn();
			
			List<Annotation> SHORTintersectList = new ArrayList<Annotation>();
			if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.SHORT)) {
				SHORTintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "SHORT", coreTokenAnn);
			}
						
			List<Annotation> MULTIPLEintersectList = new ArrayList<Annotation>();
			if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.MULTIPLE)) {
				MULTIPLEintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "MULTIPLE", coreTokenAnn);
			}
			
			List<Annotation> GLOBALintersectList = new ArrayList<Annotation>();
			if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.GLOBAL)) {
				GLOBALintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "GLOBAL", coreTokenAnn);
			}
			
			// List<Annotation> CONTEXTUALintersectList = new ArrayList<Annotation>();
			// if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.CONTEXTUAL)) {
			// 	CONTEXTUALintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "CONTEXTUAL", coreTokenAnn);
			// }
			
			List<Annotation> DERIVEDintersectList = new ArrayList<Annotation>();
			if(this.abbrvType == null || this.abbrvType.equals(AbbreviationType.DERIVED)) {
				DERIVEDintersectList = GATEutils.getAnnInDocOrderIntersectAnn(doc, "GoldStandard", "DERIVED", coreTokenAnn);
			}
			
			
			List<Annotation> globalShortFormList = new ArrayList<Annotation>();
			globalShortFormList.addAll(SHORTintersectList);
			globalShortFormList.addAll(MULTIPLEintersectList);
			globalShortFormList.addAll(GLOBALintersectList);
			// globalShortFormList.addAll(CONTEXTUALintersectList);
			globalShortFormList.addAll(DERIVEDintersectList);
			
			boolean isCoreTokenAbbrv = (globalShortFormList.size() > 0) ? true : false;
			
			int coreTokenIndex = ctxDoc.getDocumentTokenList().indexOf(coreTokenAnn);
			
			boolean isPrevCoreTokenAbbrv = false;
			if(coreTokenIndex > 0) {
				int previousTokenIndex = coreTokenIndex - 1;
				Annotation prevCoreToken = ctxDoc.getDocumentTokenList().get(previousTokenIndex);
				
				for(Annotation shortFormAnn : globalShortFormList) {
					if(shortFormAnn.overlaps(coreTokenAnn) && shortFormAnn.overlaps(prevCoreToken)) {
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

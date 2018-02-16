package es.imim.ibi.bioab.feature.generator;


import org.backingdata.gateutils.GATEutils;
import org.backingdata.mlfeats.base.FeatCalculator;
import org.backingdata.mlfeats.base.MyDouble;
import org.backingdata.nlp.utils.langres.wikifreq.LangENUM;
import org.backingdata.nlp.utils.langres.wikifreq.WikipediaLemmaTermFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.feature.TokenAnnConst;
import es.imim.ibi.bioab.feature.TokenFeatureGenerationContext;
import gate.Annotation;
import gate.Document;
import gate.Factory;

/**
 * Frequency in Wikipedia (CA, ES, EN) of words / tokens in relative position with respect to the current word / token
 * 
 * @author Francesco Ronzano
 *
 */
public class WikiFreqOfContext implements FeatCalculator<Double, Document, TokenFeatureGenerationContext> {

	private static Logger logger = LoggerFactory.getLogger(WikiFreqOfContext.class);

	private Integer relativePosition = -1;
	private LangENUM language = LangENUM.Spanish;
	private boolean sentenceScoped = false;

	public WikiFreqOfContext(Integer relPosition, LangENUM lang, boolean sentScoped) {
		relPosition = (relPosition != null) ? relPosition : 0;
		this.relativePosition = relPosition;
		this.language = lang;
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
					
					String lemmaFeature = GATEutils.getStringFeature(tokenAnnToConsider, TokenAnnConst.tokenLemmaFeat).orElse(null);
					try {
						Integer lemmaFrequency = WikipediaLemmaTermFrequency.getLemmaOccurrencesCount(this.language, lemmaFeature);
						Double lemmaFrequencyScaled = (double) (lemmaFrequency + 1 - WikipediaLemmaTermFrequency.getMinLemmaOccurrencesCount(this.language)) / (double) (WikipediaLemmaTermFrequency.getMaxLemmaOccurrencesCount(this.language) + 1 - WikipediaLemmaTermFrequency.getMinLemmaOccurrencesCount(this.language));
						retValue.setValue(10d * lemmaFrequencyScaled);
					} catch (Exception e) {
						logger.warn("Error while computing lemma frequency.");
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

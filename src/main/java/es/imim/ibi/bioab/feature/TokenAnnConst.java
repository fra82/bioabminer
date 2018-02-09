package es.imim.ibi.bioab.feature;

import es.imim.ibi.bioab.nlp.freeling.FreelingParser;

/**
 * Constant values to set the name of the annotation sets and annotations types where to look for NLP textual features,
 * used to generate the ARFF files
 * 
 * @author Francesco Ronzano
 *
 */
public class TokenAnnConst {
	public static final String tokenAnnSet = FreelingParser.mainAnnSet + "_SPA";
	public static final String tokenType = FreelingParser.tokenType;
	public static final String tokenLemmaFeat = FreelingParser.tokenType_lemmaFeatName;
	public static final String tokenPOSFeat = FreelingParser.tokenType_POSFeatName;
	public static final String tokenDepFunctFeat = "depFunct";
	public static final String sentenceAnnSet = FreelingParser.mainAnnSet + "_SPA";
	public static final String sentenceType = FreelingParser.sentenceType;
	
	
	public static final String punctutations = "(){}[]?¿!\\*+.,:;-_'´\"";//add all the ones you want.
}

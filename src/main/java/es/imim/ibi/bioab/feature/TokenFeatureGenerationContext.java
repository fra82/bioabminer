package es.imim.ibi.bioab.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gate.Annotation;
import gate.Document;

/**
 * Context class of each training instance
 * 
 * @author Francesco Ronzano
 *
 */
public class TokenFeatureGenerationContext {
	
	public static boolean reportFeatInGate = true; 
	
	private Document gateDoc;
	
	// Token calculator fields
	private Annotation coreTokenAnn;
	private String sentenceID;
	private String documentID;
	private Integer GATEsentenceID;
	private List<Annotation> documentTokenList;
	private Map<Integer, Integer> tokenIDtoSentenceIDmap = new HashMap<Integer, Integer>();
	
	// Constructor
	public TokenFeatureGenerationContext(Document gateDocument) {
		super();
		
		this.gateDoc = gateDocument;
	}
	
	// Getters and setters
	public Document getGateDoc() {
		return gateDoc;
	}

	public void setGateDoc(Document gateDoc) {
		this.gateDoc = gateDoc;
	}

	public Annotation getCoreTokenAnn() {
		return coreTokenAnn;
	}

	public void setCoreTokenAnn(Annotation coreTokenAnn) {
		this.coreTokenAnn = coreTokenAnn;
	}

	public String getSentenceID() {
		return sentenceID;
	}

	public void setSentenceID(String sentenceID) {
		this.sentenceID = sentenceID;
	}

	public Integer getGATEsentenceID() {
		return GATEsentenceID;
	}

	public void setGATEsentenceID(Integer gATEsentenceID) {
		GATEsentenceID = gATEsentenceID;
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public List<Annotation> getDocumentTokenList() {
		return documentTokenList;
	}

	public void setDocumentTokenList(List<Annotation> documentTokenList) {
		this.documentTokenList = documentTokenList;
	}

	public Map<Integer, Integer> getTokenIDtoSentenceIDmap() {
		return tokenIDtoSentenceIDmap;
	}

	public void setTokenIDtoSentenceIDmap(Map<Integer, Integer> tokenIDtoSentenceIDmap) {
		this.tokenIDtoSentenceIDmap = tokenIDtoSentenceIDmap;
	}
	
}

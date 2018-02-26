package es.imim.ibi.bioab.exec.model;

public class Abbreviation {
	
	private String type = "";
	private String abbreviation = "";
	private String longForm = "";
	private String sentence = "";
	
	
	// Setters and getters	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAbbreviationm() {
		return abbreviation;
	}
	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}
	public String getLongForm() {
		return longForm;
	}
	public void setLongForm(String longForm) {
		this.longForm = longForm;
	}
	public String getSentence() {
		return sentence;
	}
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	
	@Override
	public String toString() {
		return "Abbreviation [type=" + ((type != null) ? type : "-") + 
				", shortForm=" + ((abbreviation != null) ? abbreviation : "-") + 
				", longForm=" + ((longForm != null) ? longForm : "-") + 
				", sentence=" + ((sentence != null) ? sentence : "-") + "]";
	}
	
}

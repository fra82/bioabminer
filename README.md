# Biomedical Abbreviation Miner (BioAB Miner)

This repository contains the code of the BioAB Miner Project, to be presented at the 2nd Spanish Hackathon of NLP Technologies [http://www.hackathonplantl.es/](http://www.hackathonplantl.es/).  

The extraction of abbreviations from Biomedical literature is an essential step to support more complex semantic analysis of contents.  

BioAB Miner is a self-contained Java framework (based on [MATE parser](http://www.hackathonplantl.es/)) that supports the identification and classifications of abbreviations in Spanish biomedical scientific texts by relying on machine learning approaches trained over a corpus of manually annotated documents. Once identified abbreviations, BioAB Miner exploits a set of rules based on linguistic features of the text to search for each abbreviation the associated long form.  

BioAB Miner can extract abbreviation both from plain text and from PDF documents thans to the integration of [GROBID](https://github.com/kermitt2/grobid), a PDF-to-text converter tailored to scientific publications.  

BioAB Miner relies on the following tools:  
+ Linguistic analysis of biomedical texts: [Freeling](http://nlp.lsi.upc.edu/freeling/node/1) and [MATE parser](http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/matetools.en.html).  
+ Machine learning approaches to erxtract and characterize biomedical abbbrecitaion: [CRFsuite](https://github.com/chokkan/crfsuite) for token sequence tagging (COnditional Random Fields) and [Weka](https://www.cs.waikato.ac.nz/ml/weka/) for abbreviation type classification (Random Forest).  
+ PDF-to-text extraction: [GROBID](https://github.com/kermitt2/grobid).  
+ Linguistic annotation management: [GATE](https://gate.ac.uk/).  
  
  
To use BioAB Miner you need to:  
+ Clone this GitHUB project  
+ Download the resource folder from: [http://backingdata.org/bioabminer/](http://backingdata.org/bioabminer/BioAbMinerResources-0.1.zip)  
+ Decopress the folder lovally to your PC  
+ Modify the bioabminer.property file contained in this folder by writing the /full/local/path/to/the/resource/folder  
+ Start executing the example code below that shows how to extract abbtrevitaions from plain texts and PDF documents.  

  


BioAB Miner code sample:  

```  

// Initialize BioAB Miner by specifying the full path of the property file
BioABminer.initALL("/full/local/path/to/BioAbMinerConfig.properties");



// ***************************************************************************************
// Extract abbreviation from a PDF document of a scientific publication by means of GROBID
Document docPDF = BioABminer.getDocumentFormPDF("/home/ronzano/Desktop/Hackathon_PLN/INPUT_PDF_PUBLICATION.pdf");
BioABminer.extractNLPfeatures(docPDF);
BioABminer.extractAbbreviations(docPDF);
// Document stored as GATE XML with abbreviation annotations in the annotation set "BioABresult"
GATEfiles.storeGateXMLToFile(docPDF, "/home/ronzano/Desktop/Hackathon_PLN/PARSED_PDF_PUBLICATION_GATE_DOCUMENT.xml");

List<Abbreviation> PDFabbrvList = BioABminer.getAbbreviationList(docPDF);
for(Abbreviation abbrv : PDFabbrvList) {
	System.out.println(" PDF ABBREVIATION: " + abbrv.toString());
}
		
		
// **************************************
// Extract abbreviation from a plain text
Document docTXT = BioABminer.getDocumentFormText("Desde el punto de vista inmunológico, queda por solventar la prevención de las lesiones "
		+ "crónicas del injerto (fibrosis intersticial y atrofia tubular [FI y AT]) y la aparición del rechazo mediado por anticuerpos.");
BioABminer.extractNLPfeatures(docTXT);
BioABminer.extractAbbreviations(docTXT);
// Document stored as GATE XML with abbreviation annotations in the annotation set "BioABresult"
GATEfiles.storeGateXMLToFile(docTXT, "/home/ronzano/Desktop/Hackathon_PLN/TEXT_GATE_DOCUMENT.xml");

List<Abbreviation> TXTabbrvList = BioABminer.getAbbreviationList(docTXT);
for(Abbreviation abbrv : TXTabbrvList) {
	System.out.println(" TXT ABBREVIATION: " + abbrv.toString());
}
	

// **************************************************************
// Extract abbreviation from a document stored as a GATE XML file
Document docGATEXML = BioABminer.getDocumentFormGATEXMLfile("/home/ronzano/Desktop/Hackathon_PLN/INPUT_GATE_XML_DOCUMENT.xml");

BioABminer.extractNLPfeatures(docGATEXML);
BioABminer.extractAbbreviations(docGATEXML);
// Document stored as GATE XML with abbreviation annotations in the annotation set "BioABresult"
GATEfiles.storeGateXMLToFile(docGATEXML, "/home/ronzano/Desktop/Hackathon_PLN/PARSED_INPUT_GATE_XML_DOCUMENT.xml");

List<Abbreviation> GATEXMLabbrvList = BioABminer.getAbbreviationList(docGATEXML);
for(Abbreviation abbrv : GATEXMLabbrvList) {
	System.out.println(" GATE XML ABBREVIATION: " + abbrv.toString());
} 


```  


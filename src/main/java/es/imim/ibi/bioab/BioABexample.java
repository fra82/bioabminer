/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab;

import java.util.List;

import org.backingdata.gateutils.GATEfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.imim.ibi.bioab.exec.BioABminer;
import es.imim.ibi.bioab.exec.model.Abbreviation;
import gate.Document;

/**
 * Example class that shows how to use the BioAB Miner tool to extract 
 * abbreviations from biomedical texts.
 * 
 * @author Francesco Ronzano
 *
 */
public class BioABexample {
	
	private static Logger logger = LoggerFactory.getLogger(BioABexample.class);
	
	public static void main(String[] args) {
				
		// Initialize BioAB Miner by specifying the full path of the property file
		BioABminer.initALL("/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerConfig.properties");
		
		
		
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
	}

}

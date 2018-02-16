package es.imim.ibi.bioab;

import org.backingdata.gateutils.GATEfiles;
import org.backingdata.gateutils.generic.GenericUtil;
import org.backingdata.mlfeats.ext.CRFsuite;

import es.imim.ibi.bioab.exec.BioABminer;
import gate.Document;

/**
 * Example class that shows how to use the BioAB Miner tool to extract 
 * abbreviations from biomedical texts.
 * 
 * @author Francesco Ronzano
 *
 */
public class BioABexample {
	
	
	public static void main(String[] args) {

		// Initialize BioAB Miner by specifying the full path of the property file
		BioABminer.init("/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerConfig.properties");
		
		// Parse GATE XML documents and store abbreviations as GATE text annotations
		String inputGATEdoc = "/home/ronzano/Desktop/Hackathon_PLN/TrainingDocuments_BARR/DOC_128_BARR_ibereval_training_full_MANanno_1v_PROC.xml";
		Document docToParse = BioABminer.getDocumentFormGATEXMLfile(inputGATEdoc);
		long startTime = System.currentTimeMillis();
		// BioABminer.NLPtoolToDocument(docToParse);
		BioABminer.extractAbbreviations(docToParse);
		System.out.println(" > Doc parsed in " + (System.currentTimeMillis() - startTime) + " ms - " + inputGATEdoc);
		GATEfiles.storeGateXMLToFile(docToParse, "/home/ronzano/Downloads/BioABdocument1.xml");
		
		inputGATEdoc = "/home/ronzano/Desktop/Hackathon_PLN/TrainingDocuments_BARR/DOC_2001_BARR_ibereval_training_full_MANanno_1v_PROC.xml";
		docToParse = BioABminer.getDocumentFormGATEXMLfile(inputGATEdoc);
		startTime = System.currentTimeMillis();
		// BioABminer.NLPtoolToDocument(docToParse);
		BioABminer.extractAbbreviations(docToParse);
		System.out.println(" > Doc parsed in " + (System.currentTimeMillis() - startTime) + " ms - " + inputGATEdoc);
		GATEfiles.storeGateXMLToFile(docToParse, "/home/ronzano/Downloads/BioABdocument3.xml");
		
		inputGATEdoc = "/home/ronzano/Desktop/Hackathon_PLN/TrainingDocuments_BARR/DOC_1946_BARR_ibereval_training_full_MANanno_1v_PROC.xml";
		docToParse = BioABminer.getDocumentFormGATEXMLfile(inputGATEdoc);
		startTime = System.currentTimeMillis();
		// BioABminer.NLPtoolToDocument(docToParse);
		BioABminer.extractAbbreviations(docToParse);
		System.out.println(" > Doc parsed in " + (System.currentTimeMillis() - startTime) + " ms - " + inputGATEdoc);
		GATEfiles.storeGateXMLToFile(docToParse, "/home/ronzano/Downloads/BioABdocument2.xml");
		
	}
	
}

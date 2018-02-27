package es.imim.ibi.bioab;

import java.io.File;
import java.util.Set;

import org.backingdata.gateutils.GATEfiles;
import org.backingdata.gateutils.eval.AnnotationEvaluator;

import es.imim.ibi.bioab.exec.BioABminer;
import es.imim.ibi.bioab.exec.resource.BioABabbrvLFspotter;
import gate.Document;

public class Eval {

	public static void main(String[] args) {
		// Initialize BioAB Miner by specifying the full path of the property file
		BioABminer.initABBRV("/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerConfig.properties");


		// PDF TEST	
		/*
				Document doc = BioABminer.getDocumentFormPDF("/home/ronzano/Desktop/Hackathon_PLN/PDF_TEST/10.1.1.107.4029.pdf");
				BioABminer.extractNLPfeatures(doc);
				GATEfiles.storeGateXMLToFile(doc, "/home/ronzano/Desktop/Hackathon_PLN/PDF_TEST/10.1.1.107.4029.xml");

				doc = BioABminer.getDocumentFormPDF("/home/ronzano/Desktop/Hackathon_PLN/PDF_TEST/5414-4759-1-PB.pdf");
				BioABminer.extractNLPfeatures(doc);
				GATEfiles.storeGateXMLToFile(doc, "/home/ronzano/Desktop/Hackathon_PLN/PDF_TEST/5414-4759-1-PB.xml");

				doc = BioABminer.getDocumentFormPDF("/home/ronzano/Desktop/Hackathon_PLN/PDF_TEST/5420-4765-1-PB.pdf");
				BioABminer.extractNLPfeatures(doc);
				GATEfiles.storeGateXMLToFile(doc, "/home/ronzano/Desktop/Hackathon_PLN/PDF_TEST/5420-4765-1-PB.xml");

				System.out.println("Execution terminated");

				System.exit(1);
		 */

		/*
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
		 */


		File[] BARRfiles = (new File("/home/ronzano/Desktop/Hackathon_PLN/TrainingDocuments_BARR")).listFiles();

		AnnotationEvaluator evalAnno = new AnnotationEvaluator("GoldStandard", "BioAB", null, 0);

		int fileCounter = 0;
		for(File BARRfile : BARRfiles) {
			if(BARRfile != null && BARRfile.exists() && BARRfile.isFile() && BARRfile.getName().endsWith(".xml")) {
				try {

					// if(++fileCounter > 3) {
					// 	break;
					// }

					System.out.println("\n-------------------------------------------------");
					System.out.println(" > Start processing document: " + BARRfile.getName() + "...");
					Document docToParse = BioABminer.getDocumentFormGATEXMLfile(BARRfile.getAbsolutePath());
					docToParse.setName(BARRfile.getName());
					long startTime = System.currentTimeMillis();
					Set<String> goldStandardTypes = docToParse.getAnnotations("GoldStandard").getAllTypes();
					for(String goldStandardType : goldStandardTypes) {
						System.out.println("    GS : " + goldStandardType + " > " + docToParse.getAnnotations("GoldStandard").get(goldStandardType).size());
					}
					BioABminer.extractAbbreviations(docToParse);
					System.out.println(" > Doc parsed in " + (System.currentTimeMillis() - startTime) + " ms - " + BARRfile.getName());
					GATEfiles.storeGateXMLToFile(docToParse, "/home/ronzano/Downloads/" + BARRfile.getName().replace(".xml", "_TEST.xml"));
					// System.out.println(" > Doc stored to " + "/home/ronzano/Downloads/" + BARRfile.getName().replace(".xml", "_TEST.xml"));

					evalAnno.processDocument(docToParse);

					docToParse.cleanup();
					System.gc();

					System.out.println(" > STAT > longFormBySpan >" + BioABabbrvLFspotter.longFormBySpan);
					System.out.println(" > STAT > candidateMatchASingleSpanLongForm >" + BioABabbrvLFspotter.candidateMatchASingleSpanLongForm);
					System.out.println(" > STAT > candidateMatchAMultipleSpanLongForm >" + BioABabbrvLFspotter.candidateMatchAMultipleSpanLongForm);
					System.out.println(" > STAT > candidateMatchAMultipleSpanLongForm >" + BioABabbrvLFspotter.candidateMatchAMultipleSpanLongForm);
					System.out.println(" > STAT > longFormWithoutAnyCandidateSpanMatch >" + BioABabbrvLFspotter.longFormWithoutAnyCandidateSpanMatch);
					System.out.println(" > STAT > longFormMatchByTypeSingle >" + BioABabbrvLFspotter.longFormMatchByTypeSingle);
					System.out.println(" > STAT > longFormMatchByTypeMulti >" + BioABabbrvLFspotter.longFormMatchByTypeMulti);

				}
				catch(Exception e) {
					System.out.println(" > Exception parsing doc: " + BARRfile.getAbsolutePath());
					e.printStackTrace();
				} 
			}
		}

		System.out.println("\n\n\n");

		System.out.println(" >>> " + evalAnno.getFscoreMeasures(true));

		System.out.println("\n\n\n");

		for(String longFormWithoutAnyCandidateSpanMatch : BioABabbrvLFspotter.longFormWithoutAnyCandidateSpanMatchList) {
			System.out.println(" > STAT > LongFormNotMatch >" + longFormWithoutAnyCandidateSpanMatch);
		}

		System.out.println("\n\n\n");

		System.out.println(" > STAT > longFormBySpan >" + BioABabbrvLFspotter.longFormBySpan);
		System.out.println(" > STAT > candidateMatchASingleSpanLongForm >" + BioABabbrvLFspotter.candidateMatchASingleSpanLongForm);
		System.out.println(" > STAT > candidateMatchAMultipleSpanLongForm >" + BioABabbrvLFspotter.candidateMatchAMultipleSpanLongForm);
		System.out.println(" > STAT > candidateMatchAMultipleSpanLongForm >" + BioABabbrvLFspotter.candidateMatchAMultipleSpanLongForm);
		System.out.println(" > STAT > longFormWithoutAnyCandidateSpanMatch >" + BioABabbrvLFspotter.longFormWithoutAnyCandidateSpanMatch);
		System.out.println(" > STAT > longFormMatchByTypeSingle >" + BioABabbrvLFspotter.longFormMatchByTypeSingle);
		System.out.println(" > STAT > longFormMatchByTypeMulti >" + BioABabbrvLFspotter.longFormMatchByTypeMulti);

		System.out.println("\n\n\n");

		System.out.println(" >>> " + evalAnno.getFscoreMeasures(false));


	}

}

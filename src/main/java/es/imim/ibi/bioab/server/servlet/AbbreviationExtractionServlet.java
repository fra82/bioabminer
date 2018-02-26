/**
 * Biomedical Abbreviation Miner (BioAB Miner)
 * 
 */
package es.imim.ibi.bioab.server.servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import es.imim.ibi.bioab.exec.BioABminer;
import es.imim.ibi.bioab.exec.pdf.GROBIDloader;
import es.imim.ibi.bioab.server.template.TemplateUtils;
import gate.Document;


/**
 * Servlet to generate comorbidity analysis
 * 
 * @author Francesco Ronzano
 *
 */
public class AbbreviationExtractionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(AbbreviationExtractionServlet.class);
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
	
	public void init() throws ServletException {

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		out.println(TemplateUtils.generateHTMLwebFormTemplate(null));
		out.flush();
	}

	public static ImmutablePair<String, String> checkParameterString(String paramString, String paramName) {
		String causeLeft = "";
		String valueRight = "";

		if(Strings.isNullOrEmpty(paramString)) {
			causeLeft = "Null or empty String parameter: " + ((paramName != null) ? paramName : "-");
		}
		else {
			causeLeft = null;
			valueRight = paramString;
		}

		return new ImmutablePair<String, String>(causeLeft, valueRight);
	}

	public static ImmutablePair<String, Integer> checkParameterInteger(String paramString, String paramName) {
		String causeLeft = "";
		Integer valueRight = null;

		Integer paramInt = null;
		try {
			paramInt = Integer.valueOf(paramString);
		}
		catch(Exception e) {
			/* Do nothing */
		}

		if(Strings.isNullOrEmpty(paramString) || paramInt == null) {
			causeLeft = "Null, empty or not Integer parameter: " + ((paramName != null) ? paramName : "-");
		}
		else {
			causeLeft = null;
			valueRight = paramInt;
		}

		return new ImmutablePair<String, Integer>(causeLeft, valueRight);
	}

	public static ImmutablePair<String, Double> checkParameterDouble(String paramString, String paramName) {
		String causeLeft = "";
		Double valueRight = null;

		Double paramDouble = null;
		try {
			paramDouble = Double.valueOf(paramString);
		}
		catch(Exception e) {
			/* Do nothing */
		}

		if(Strings.isNullOrEmpty(paramString) || paramDouble == null) {
			causeLeft = "Null, empty or not Double parameter: " + ((paramName != null) ? paramName : "-");
		}
		else {
			causeLeft = null;
			valueRight = paramDouble;
		}

		return new ImmutablePair<String, Double>(causeLeft, valueRight);
	}
	
	private static String getClientIp(HttpServletRequest request) {
        String remoteAddr = "NO_IP";

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Date currentDate = new Date();
		System.out.println("Received Processing request from IP: " + getClientIp(request) + " on " + dateFormat.format(currentDate));
		
		PrintWriter out = response.getWriter();

		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if( !isMultipart ) {
			// >>> Process text
			ImmutablePair<String, String> textToAnalyze_check = checkParameterString(request.getParameter("textToAnalyze"), "textToAnalyze");
			String textToAnalyze = (textToAnalyze_check.getLeft() == null) ? textToAnalyze_check.getRight() : null;
			
			if(textToAnalyze != null && !textToAnalyze.trim().equals("")) {
				
				Document doc = BioABminer.getDocumentFormText(textToAnalyze);
				BioABminer.extractNLPfeatures(doc);
				BioABminer.extractAbbreviations(doc);
				
				out.println(TemplateUtils.generateHTM_TXTtemplate(doc));
				out.flush();
				
				doc.cleanup();
				System.gc();
			}
			else {
				// Error while analyzing text
				out.println(TemplateUtils.generateHTMLwebFormTemplate("Please, specify the text to parse. The text you provided is:"
						+ ((textToAnalyze == null) ? "_NULL" : ((textToAnalyze.trim().equals("")) ? " EMPTY" : " INCLUDE ERRORS"))));
				out.flush();
			}
		}
		else {
			// >>> Process PDF
			try {

				String checkVarErrors = "";

				// Patient data file path and column names
				Part paperFilePart = request.getPart("pdfFile");
				if(paperFilePart != null) {
					if(paperFilePart.getSize() <= 0l) {
						checkVarErrors += "Impossible to read PDF file contents" + "\n";
					}
					else if(paperFilePart.getSize() >= 5048576l) {
						out.println(TemplateUtils.generateHTMLwebFormTemplate("Attention: the PDF file uploaded is greater than 5Mb. BioAB Miner "
								+ "will soon support the on-line analysis of large file. "
								+ "Currently, you can use the Java tool BioAB miner (https://github.com/fra82/bioabminer) "
								+ "to performe analysis of this PDF file on your PC."));
						out.flush();
						return;
					}
				}
				else {
					checkVarErrors += "PDF file not uploaded" + "\n";
				}
				
				if(Strings.isNullOrEmpty(checkVarErrors)) {
					
					Document doc = GROBIDloader.parsePDF(IOUtils.toByteArray(paperFilePart.getInputStream()), paperFilePart.getName());
					BioABminer.extractNLPfeatures(doc);
					BioABminer.extractAbbreviations(doc);
					
					out.println(TemplateUtils.generateHTM_PDFtemplate(doc));
					out.flush();
					
					doc.cleanup();
					System.gc();
				}
				else {
					out.println(TemplateUtils.generateHTMLwebFormTemplate(checkVarErrors));
					out.flush();
				}


			}
			catch(Exception e) {
				out.println(TemplateUtils.generateHTMLwebFormTemplate("An exception occurred while processing data: " + e.getMessage()));
				out.flush();
			}
		}
	}
	
	public void destroy() {
		// do nothing.
	}
}
package es.imim.ibi.bioab.server.template;

import java.io.IOException;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Strings;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import gate.Document;
import gate.util.InvalidOffsetException;

/**
 * HTML generation utilities
 * 
 * @author Francesco Ronzano
 *
 */
public class TemplateUtils {

	private static Random rnd = new Random();
	private static Integer countReloadCSSJS = rnd.nextInt(Integer.MAX_VALUE);

	private static Configuration cfg = null;

	private static DecimalFormat decimFormat = new DecimalFormat("#######0.##########");

	static {

		cfg = new Configuration(Configuration.getVersion());
		cfg.setClassForTemplateLoading((new TemplateUtils()).getClass(), "/webtempl");
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setLocale(Locale.US);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

		decimFormat.setRoundingMode(RoundingMode.HALF_UP);
	}


	public static String generateHTM_TXTtemplate(Document parsedDoc) {

		// Populate template
		Map<String, Object> input = new HashMap<String, Object>();
		
		input.put("CSSJScount", countReloadCSSJS);
		countReloadCSSJS++;
		
		Template template = null;
		try {
			input.put("originalText", (parsedDoc != null) ? parsedDoc.getContent().getContent(0l, gate.Utils.lengthLong(parsedDoc)).toString() : "INCORRECT ORIGINAL TEXT");
			
			template = cfg.getTemplate("analysisResultsTXT.ftl");
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidOffsetException e) {
			e.printStackTrace();
		}

		StringWriter stringWriter = new StringWriter();
		try {
			template.process(input, stringWriter);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringWriter.toString();
	}
	
	
	public static String generateHTM_PDFtemplate(Document parsedDoc) {

		// Populate template
		Map<String, Object> input = new HashMap<String, Object>();
		
		input.put("CSSJScount", countReloadCSSJS);
		countReloadCSSJS++;

		


		Template template = null;
		try {
			input.put("originalText", (parsedDoc != null) ? parsedDoc.toXml() : "INCORRECT ORIGINAL TEXT");
			
			template = cfg.getTemplate("analysisResultsPDF.ftl");
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		StringWriter stringWriter = new StringWriter();
		try {
			template.process(input, stringWriter);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringWriter.toString();
	}

	public static String generateHTMLwebFormTemplate(String errorMessage) {

		// Populate template
		Map<String, Object> input = new HashMap<String, Object>();
		if(!Strings.isNullOrEmpty(errorMessage)) {
			input.put("errorMsg", errorMessage);
		}
		
		input.put("CSSJScount", countReloadCSSJS);
		countReloadCSSJS++;
		
		Template template = null;
		try {
			template = cfg.getTemplate("webForm.ftl");
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		StringWriter stringWriter = new StringWriter();
		try {
			template.process(input, stringWriter);
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringWriter.toString();
	}
	
	
	public static void main(String[] args) {
		// TemplateUtils.generateHTMLanalysisResTemplate(null, null, null, null);
	}


}

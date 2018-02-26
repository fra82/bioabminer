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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import es.imim.ibi.bioab.server.template.TemplateUtils;

/**
 * Servlet to get data for comorbidity analysis
 * 
 * @author Francesco Ronzano
 *
 */
public class WebFormManagerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(WebFormManagerServlet.class);
	
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy MM dd HH:mm:ss");

	public void init() throws ServletException {

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

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Date currentDate = new Date();
		System.out.println("Received Form request from IP: " + getClientIp(request) + " on " + dateFormat.format(currentDate));
		
		PrintWriter out = response.getWriter();
		out.println(TemplateUtils.generateHTMLwebFormTemplate(null));
		out.flush();
	}
	
	public static ImmutablePair<String, String> checkParameterString(String paramString, String paramName) {
		String causeLeft = "";
		String valueRight = "";
		
		ImmutablePair<String, String> retPair = new ImmutablePair<String, String>(causeLeft, valueRight);
		
		if(Strings.isNullOrEmpty(paramString)) {
			causeLeft = "Null or empty String parameter: " + ((paramName != null) ? paramName : "-");
		}
		else {
			causeLeft = null;
			valueRight = paramString;
		}
		
		return retPair;
	}
	
	public static ImmutablePair<String, Integer> checkParameterInteger(String paramString, String paramName) {
		String causeLeft = "";
		Integer valueRight = null;
		
		ImmutablePair<String, Integer> retPair = new ImmutablePair<String, Integer>(causeLeft, valueRight);
		
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
		
		return retPair;
	}
	
	public static ImmutablePair<String, Double> checkParameterDouble(String paramString, String paramName) {
		String causeLeft = "";
		Double valueRight = null;
		
		ImmutablePair<String, Double> retPair = new ImmutablePair<String, Double>(causeLeft, valueRight);
		
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
		
		return retPair;
	}

	public void destroy() {
		// do nothing.
	}
}
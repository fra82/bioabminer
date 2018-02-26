package es.imim.ibi.bioab.server;

import java.io.File;

import javax.servlet.MultipartConfigElement;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import es.imim.ibi.bioab.exec.BioABminer;
import es.imim.ibi.bioab.exec.pdf.GROBIDloader;
import es.imim.ibi.bioab.server.servlet.AbbreviationExtractionServlet;
import es.imim.ibi.bioab.server.servlet.WebFormManagerServlet;
import gate.Document;


/**
 * Jetty server runner
 * 
 * @author Francesco Ronzano
 *
 */
public class BioABserver {
	
	public static void main(String[] args) throws Exception {
		
		// Initialize BioAB Miner by specifying the full path of the property file
		BioABminer.initALL("/home/ronzano/Desktop/Hackathon_PLN/BioAbMinerConfig.properties");
		GROBIDloader.initGROBID();
		
		Document doc = BioABminer.getDocumentFormText("This is an example text.");
		BioABminer.extractNLPfeatures(doc);
		BioABminer.extractAbbreviations(doc);
		doc.cleanup();
		
		
		Server server = new Server();

		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8181);
		server.setConnectors(new Connector[] { connector });
		connector.setIdleTimeout(90000);
		
		// Add static content handler
		ResourceHandler rh0 = new ResourceHandler();
        ContextHandler context0 = new ContextHandler();
        context0.setContextPath("/bioabminer/css");
        ClassLoader classLoader = (new BioABserver()).getClass().getClassLoader();
        File dir0 = new File(classLoader.getResource("webtempl/css").getPath());
        System.out.println("CSS dir path: " + dir0.getAbsolutePath());
        context0.setBaseResource(Resource.newResource(dir0));
        context0.setHandler(rh0);

        ResourceHandler rh1 = new ResourceHandler();
        ContextHandler context1 = new ContextHandler();
        context1.setContextPath("/bioabminer/js");
        File dir1 = new File(classLoader.getResource("webtempl/js").getPath());
        System.out.println("JS dir path: " + dir1.getAbsolutePath());
        context1.setBaseResource(Resource.newResource(dir1));
        context1.setHandler(rh1);
	    
	    
	    // Add processing servlet
        ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
        ServletHolder fileUploadServletHolder = new ServletHolder(new AbbreviationExtractionServlet());
        fileUploadServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement("data/tmp"));
        context.addServlet(fileUploadServletHolder, "/bioabminer/fileUpload");
		context.addServlet(WebFormManagerServlet.class, "/bioabminer/home");
		
		

		ContextHandlerCollection handlers = new ContextHandlerCollection();
		handlers.setHandlers(new Handler[] { context0, context1, context, new DefaultHandler() });
		server.setHandler(handlers);

		server.start();
		server.join();
		
		
	}
	
}

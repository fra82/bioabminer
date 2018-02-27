<html><head>
  <meta http-equiv="content-type" content="text/html; charset=UTF-8">

  <title>BioAB Miner</title>

  <!-- Jquery and JqueryUI -->
  <script src="http://backingdata.org/bioabminer/js/jquery-3.2.1.min.js?v=${CSSJScount!'---'}"></script>
  <script src="http://backingdata.org/bioabminer/js/jquery-ui.min.js?v=${CSSJScount!'---'}"></script>
  <script src="http://backingdata.org/bioabminer/js/jquery.form-validator.min.js?v=${CSSJScount!'---'}"></script> <!-- http://www.formvalidator.net/ -->
  <link rel="stylesheet" href="http://backingdata.org/bioabminer/css/jquery-ui.min.css?v=${CSSJScount!'---'}" type="text/css" />
  
  <!-- custom -->
  <link rel="stylesheet" href="http://backingdata.org/bioabminer/css/main.css?v=${CSSJScount!'---'}" type="text/css" />
  
<script>

$(document).ready(function() {
	$.validate();
});

</script>

</head>
<body>
  <div id="page-wrap">
	<div style="text-align:center;margin-top:30px;margin-bottom:15px;">
		<h1>Biomedical Abbreviations Miner (BioAB Miner)</h1>
		<h2>Online demo</h2>
		developed by the
  <b><a href="http://grib.imim.es/research/integrative-biomedical-informatics/index.html" target="_blank">Integrative Biomedical Informatics Group</a></b><br/>
  <div style="font-size:100%">
  part of the Research Programme on Biomedical Informatics (GRIB)<br/>
  <a href="http://www.imim.es/" target="_blank">Hospital del Mar Medical Research Institute (IMIM)</a> AND
  DCEXS of the <a href="http://www.upf.edu/" target="_blank">Universitat Pompeu Fabra</a><br/>
  Barcelona</div>
	</div>
	<div style="text-align:left;border: margin:15px; padding:5px;">
		Biomedical Abbreviations Miner (BioAB Miner) is a Biomedical Abbreviation Extraction tool, useful to spot abbreviations and long forms in Spanish scientific literature.<br/>
		The source code of this tool is available at: <a href="https://github.com/fra82/bioabminer" target="_blank">https://github.com/fra82/bioabminer</a>.<br/>
	</div>
	
	<#if (errorMsg)??>
	
	<div style="text-align:left;" class="errorDiv">
	<h2>The following errors occurred while processing your dataset.</h2>
	Please, look at the error log below, <b>correct the error in the Web form opened in another tab of your browser</b> tab and click again 
	the 'Extract abbreviations!' button to trigger the analysis of your data.<br/><br/>
	${errorMsg!'No errors.'}
	</div>
	
	<#else>
	
	<!-- Process text -->
	<div style="text-align:center;" class="sendForm">
		<h2>Process text:</h2>
		<form action="fileUpload" method="post" name="TEXTform" id="TEXTformID" target="_blank">
		<textarea style="width:99%;height:250px;" name="textToAnalyze" id="textToAnalyzeID"> Entre otros factores, se debe tener en cuenta que estos pacientes
		 presentan un riesgo adicional de ETEV debido a la propia obesidad (IMC>30). Tras la cirugía bariátrica el riesgo de padecer eventos tromboembólicos varía según las series consultadas. En líneas generales, se estima que el peligro de EP (embolia pulmonar) es del 0,8% y el de TVP (trombosis venosa profunda) de 1,7%. La mortalidad global por ETEV se estima del 0,1 al 2%.</textarea>
		<div class="buttonSubmit">
			<button id="submitButton" type="submit" class="buttonStyle">Extract abbreviations!</button>
		</div>
		</form>
	</div>
	
	<!-- Process PDF file -->
	<div style="text-align:center;" class="sendForm">
		<h2>Process scientific publications PDF file:</h2>
		<form action="fileUpload" method="post" enctype="multipart/form-data" name="PDFform" id="PDFformID" target="_blank">
		<div class="sendFormElem">Select PDF file to process by means of BioAB miner:&nbsp;<input name="pdfFile" type="file" id="pdfFileID" /></div>
		<div class="buttonSubmit">
			<button id="submitButton" type="submit" class="buttonStyle">Extract abbreviations!</button>
		</div>
		</form>
	</div>
	
	</#if>
	
	<br/><br/><br/><br/>

</body></html>

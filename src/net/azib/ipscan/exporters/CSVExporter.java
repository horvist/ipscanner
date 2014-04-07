/**
 * 
 */
package net.azib.ipscan.exporters;

import java.io.IOException;

import net.azib.ipscan.config.Config;
import net.azib.ipscan.config.ScannerConfig;

/**
 * CSV Exporter
 *
 * @author Anton Keks
 */
public class CSVExporter extends AbstractExporter {

	/* CSV delimiter character */
	static String DELIMETER = ",";
	/* Delimiter escaping character (if data contains DELIMETER) */
	static final String DELIMETER_ESCAPED = ".";
	
	//gsanta lehet nem a legjobb megoldas
	private ScannerConfig scannerConfig;
	
	public CSVExporter() {
		scannerConfig = Config.getConfig().forScanner();
	}
	//gsanta
	
	public String getId() {
		return "exporter.csv";
	}

	public String getFilenameExtension() {
		return "csv";
	}
	
	public void setFetchers(String[] fetcherNames) throws IOException {
		if (!append) {
			output.write(csvSafeString(fetcherNames[0]));
			for (int i = 1; i < fetcherNames.length; i++) {
				output.write(DELIMETER);
				output.write(csvSafeString(fetcherNames[i]));			
			}
			output.println();
		}
	}

	public void nextAdressResults(Object[] results) throws IOException {
		output.write(csvSafeString(results[0]));
		for (int i = 1; i < results.length; i++) {
			Object result = results[i];
			output.write(DELIMETER);
			output.write(csvSafeString(result));
		}
		output.println();
	}

	/**
	 * @return a safe string to be output in CSV format (it doesn't contain the DELIMETER)
	 */
	String csvSafeString(Object o) {
		//gsanta
		DELIMETER = scannerConfig.csvSeparator;
		//gsanta
		if (o == null)
			return "";
		return o.toString().replace(DELIMETER, DELIMETER_ESCAPED);
	}
}
package cl.facele.docele.transformer.logica;


public class SetLocationApp {
	// private static Logger logger = Logger.getLogger(SetLocationApp.class);

	static {
		try {
			String str = SetLocationApp.class.getResource(
					"SetLocationApp.class").getPath();
			// logger.debug("ruta class: " + str);
			if (str.startsWith("file:"))
				str = str.substring(5);

			int n;
			if (str.contains("/bin/cl/facele/"))
				n = str.indexOf("/bin/cl/facele/");
			else
				n = str.indexOf("/lib/");

			str = str.substring(0, n);

			if (str.contains("%20"))
				str = str.replaceAll("%20", " ");

			// logger.debug("Sistema Operativo: " +
			// System.getProperty("os.name"));
			if (System.getProperty("os.name").toUpperCase().contains("WIN"))
				str = str.substring(1);

			// logger.debug("DOCELE_HOME: " + str);

			System.setProperty("facele.home", str);

		} catch (Exception e) {
			try {
				throw new Exception(
						"El aplicativo no esta ubicado dentro del directorio '.../DocEle/lib/'.");
			} catch (Exception e1) {
				// logger.error(e1.getMessage());
			}
		}

	}
}

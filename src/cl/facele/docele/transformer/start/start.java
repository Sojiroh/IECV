package cl.facele.docele.transformer.start;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import cl.facele.docele.transformer.logica.SetLocationApp;
import cl.facele.docele.transformer.logica.TransformerIECV;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class start {
    private static Path dirDTE;
    private static DirectoryStream<Path> directory;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		new SetLocationApp();
		
                //dirDTE = Paths.get(System.getProperty("user.home"), "Downloads","libros");
		dirDTE = Paths.get("D:\\Descargas\\libros 2");
                directory = Files.newDirectoryStream(dirDTE);
                for (Path filePath : directory) {
		File file = new File(filePath.toString());
		System.out.println(filePath.toString());
		String txt = "";
		try {
                        TransformerIECV iecv = new TransformerIECV();
			txt = iecv.getTXT(file);
			System.out.println(txt);
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
			BufferedWriter estadoStart = new BufferedWriter(new FileWriter("C:\\Users\\Shupelupe\\Documents\\" +
					System.currentTimeMillis()+ ".txt"));
			estadoStart.write(txt);
			estadoStart.close();		
			
		} catch (Exception e) {
			System.out.println(e);
		}
                }
	}
}

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JavaClassFileVersionChecker {

	private static final Map<Integer, String> javaVersionMap;
	static {
		Map<Integer, String> tempMap = new HashMap<>();
		tempMap.put(45, "Java 1.1");
		tempMap.put(46, "Java 1.2");
		tempMap.put(47, "Java 1.3");
		tempMap.put(48, "Java 1.4");
		tempMap.put(49, "Java 5");
		tempMap.put(50, "Java 6");
		tempMap.put(51, "Java 7");
		tempMap.put(52, "Java 8");
		tempMap.put(53, "Java 9");
		tempMap.put(54, "Java 10");
		tempMap.put(55, "Java 11");
		tempMap.put(56, "Java 12");
		tempMap.put(57, "Java 13");
		tempMap.put(58, "Java 14");
		tempMap.put(59, "Java 15");
		tempMap.put(60, "Java 16");
		tempMap.put(61, "Java 17");
		javaVersionMap = Collections.unmodifiableMap(tempMap);
	}

	private static int maxMajorV = Integer.MIN_VALUE;
	private static int minMajorV = Integer.MAX_VALUE;
	private static int classCounter = 0;
	private static final String classFileEnding = ".class";

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("First argument must be a path to a Jar file or a path which contains class files");
			return;
		}
		if (args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("h") || args[0].equalsIgnoreCase("-?")
				|| args[0].equalsIgnoreCase("?")) {
			System.out.println("Java Class File Version Checker:\n" + 
					"Checks the major&minor version of all class files within a .jar file or within a directory (recursive)");
			System.out.println("E.g.: java JavaClassFileVersionChecker path/to/JarToCheck.jar");
			System.out.println("      java JavaClassFileVersionChecker path/to/dir/with/clasFilesToCheck/");
			return;
		}
		String path = args[0];

		if (path.endsWith(".jar")) {
			iterateOverClassFilesInJar(path);
		} else {
			iterateOverClassFilesInPath(path);
		}

		System.out.println("\nSummary for " + path);
		System.out.println("Checked " + classCounter + " class files");
		System.out.println("Highest major version: " + maxMajorV + " (" + javaVersionMap.get(maxMajorV) + ")");
		System.out.println("Lowest major version: " + minMajorV + " (" + javaVersionMap.get(minMajorV) + ")");
	}

	private static void iterateOverClassFilesInJar(String pathToJar) throws IOException {
		ZipFile jarFile = new ZipFile(pathToJar);
		Enumeration<? extends ZipEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.getName().endsWith(classFileEnding)) {
				checkVersionForClassInputStream(jarFile.getInputStream(entry), entry.toString());
			}
		}
	}

	private static void iterateOverClassFilesInPath(String path) throws IOException {
		Files.walk(Paths.get(path)).filter(f -> f.getFileName().toString().endsWith(classFileEnding)).forEach(f -> {
			checkVersionForClassFile(f.toFile());
		});
	}

	private static void checkVersionForClassFile(File classFile) {
		try {
			checkVersionForClassInputStream(new FileInputStream(classFile), classFile.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void checkVersionForClassInputStream(InputStream classInputStream, String pathToClass) {
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(classInputStream);
			if (dis.readInt() != 0xCAFEBABE) {
				System.err.println(pathToClass + " is not a valid class file!");
				return;
			}
			classCounter++;
			int minorV = dis.readUnsignedShort();
			int majorV = dis.readUnsignedShort();
			System.out.println(pathToClass + ": " + majorV + "." + minorV + " (" + javaVersionMap.get(majorV) + ")");
			maxMajorV = Math.max(majorV, maxMajorV);
			minMajorV = Math.min(majorV, minMajorV);
		} catch (IOException e) {
			System.err.println("Unable to check class " + pathToClass + ": " + e.getMessage());
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
				}
			}
		}
	}
}

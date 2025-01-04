package com.ak.zipp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryZipperTest {

	@TempDir
	static Path sourceRootDir;  // dir level 0

	@TempDir
	static Path destRootDir;  // dir level 0

	//	Path sourceDir;
	// the 2 subDir-s of rootDir
	static Path d1, d2;  

	// subDir-s of d1
	static Path d3;    

	@BeforeAll
	static void setup() throws IOException {

		//// setup the test dir-s
		// level-1
		Files.createDirectories(d1 = sourceRootDir.resolve("d1"));
		Files.createDirectories(d2 = sourceRootDir.resolve("d2"));

		// level-2
		Files.createDirectories(d3 = sourceRootDir.resolve("d3"));

		//// setup the files
		// tempDir
		Files.createFile(sourceRootDir.resolve(".p.q"));
		Files.createFile(sourceRootDir.resolve(".r"));
		Files.createFile(sourceRootDir.resolve("aFile"));
		Files.createFile(sourceRootDir.resolve("p.tx"));
		Files.createFile(sourceRootDir.resolve("d."));

		// d1
		Files.createFile(d1.resolve("apple.txt"));

		// d2
		// empty folder

		// d3
		Files.createFile(d3.resolve("d.p.q"));
		Files.createFile(d3.resolve("dpq"));
		Files.createFile(d3.resolve("a.log"));

		//		testInstance = new DirectoryZipper(null);


	}

	static Path defaultSourceDir = Paths.get(System.getProperty("user.dir"));

	static String aFreshZipFilename() {  return "zipFN" + System.currentTimeMillis() + ".zip";  }

	@Test
	void testBasicZipping() throws IOException {
		Path zipToPath = null;
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp -z "+zipToFile;
		try {
			DirectoryZipper.pipe(commandLine.split(" +"));

			zipToPath = defaultSourceDir.resolve(zipToFile);
			assertTrue(Files.exists(zipToPath), "Zip file should be created.");
		} finally {
			Files.deleteIfExists(zipToPath);
		}
	}

	@Test
	void testRecursiveZipping() throws IOException {
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp -s "+sourceRootDir+" -d "+destRootDir+" -z "+zipToFile;
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = destRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		try (ZipFile zipFile = new ZipFile(zipToPath.toString())) {
			assertNotNull(zipFile.getEntry( "aFile"), "Zip file should contain all files at root folder -- `a`.");
			assertNotNull(zipFile.getEntry(".r"), "Zip file should contain all files at root folder -- `.r`.");
			assertNotNull(zipFile.getEntry("d1"), "Zip file should contain sub-folders --  `d1`.");
			assertNotNull(zipFile.getEntry("d2"), "Zip file should contain sub-folders --  `d2`.");
			// nope - not File.separator for zipFile.getEntry()
			assertNotNull(zipFile.getEntry("d3/a.log"), "Zip file should contain full contents of sub-folders --  `d3\\a.log`.");
			//		} finally { 
			//			Files.deleteIfExists(zipToPath);
		}
	}

	@Test
	void testNonRecursiveZipping() throws IOException {
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp -s "+sourceRootDir+" -d "+destRootDir+" -z "+zipToFile +" -nr";
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = destRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		try (ZipFile zipFile = new ZipFile(zipToPath.toString())) {
			assertNotNull(zipFile.getEntry( "aFile"), "Zip file should contain all files at root folder -- `a`.");
			assertNotNull(zipFile.getEntry(".r"), "Zip file should contain all files at root folder -- `.r`.");
			assertNull(zipFile.getEntry("d1"), "Zip file should NOT contain sub-folders --  `d1`.");
			assertNull(zipFile.getEntry("d2"), "Zip file should NOT contain sub-folders --  `d2`.");
			// nope - not File.separator for zipFile.getEntry()
			assertNull(zipFile.getEntry("d3/a.log"), "Zip file should NOT contain contents of sub-folders --  `d3\\a.log`.");
			//		} finally { 
			//			Files.deleteIfExists(zipToPath);
		}
	}

	@Test
	void testZipFileNotZipped() throws IOException {
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp -s "+sourceRootDir+" -d "+sourceRootDir+" -z "+zipToFile; 
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = sourceRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		try (ZipFile zipFile = new ZipFile(zipToPath.toString())) {
			assertNull(zipFile.getEntry(zipToFile), "Zip file should NOT contain the zipFile itself -- `"+zipToFile +"`.");
		}
	}

	public static Set<Path> getDirContents(Path directoryPath) throws IOException {
		try (Stream<Path> stream = Files.list(directoryPath)) {
			return stream.collect(Collectors.toSet());
		}
	}

	@Test
	void testDefaultFilename() throws IOException {
		Path dTemp = destRootDir.resolve("temp");
		String commandLine = "zipp -s "+sourceRootDir+" -d "+dTemp+" -nr";
		Files.createDirectories(dTemp);

		String dateBefore = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		DirectoryZipper.pipe(commandLine.split(" +"));
		String dateAfter = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

		Set<Path> destAfter = getDirContents(dTemp);
		assertTrue(destAfter.size()==1, "Destination folder "+dTemp+ " should now contain the zipped file only .");
		Path zipToPath = destAfter.iterator().next();
		String zipToFile = zipToPath.getFileName().toString();

		assertTrue(zipToFile.startsWith(sourceRootDir.getFileName().toString()));
		assertTrue(zipToFile.endsWith("_" + "Zipped-on" +"_" + dateBefore+".zip")
				|| zipToFile.endsWith("_" + "Zipped-on" +"_" + dateAfter+".zip"));
		assertTrue(zipToFile.length() == sourceRootDir.getFileName().toString().length() 
				+ ("_"+System.currentTimeMillis()).length()
				+ ("_" + "Zipped-on" +"_" + dateBefore+".zip").length());
	}

	@Test
	void testDotNotationForFolders() throws IOException {
		String destName = "someDestFolder"+System.currentTimeMillis();
		Path destFolder = Files.createDirectories(Paths.get(".."+File.separator+destName));
		Path zipToPath = null;
		try {
			String srcName = "someSrcFolder"+System.currentTimeMillis();
			Path srcFolder = Files.createDirectories(Paths.get("."+File.separator+srcName));

			Files.createFile(srcFolder.resolve("file1.txt"));
			Files.createFile(srcFolder.resolve("file2.txt"));

			String zipToFile = aFreshZipFilename();
			String commandLine = "zipp "
					+ "-s "+"." +File.separator+srcName
					+" -d "+".."+File.separator+destName
					+" -z "+zipToFile
					+" -nr";
			DirectoryZipper.pipe(commandLine.split(" +"));

			zipToPath = destFolder.resolve(zipToFile);
			assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

			try (ZipFile zipFile = new ZipFile(zipToPath.toString())) {
				assertNotNull(zipFile.getEntry("file1.txt")); } 
		} finally { 
			Files.deleteIfExists(zipToPath);
			Files.deleteIfExists(destFolder);
		}
	}

	@Test
	void testZipFileNameConflict() throws IOException {
		String zipToFile = aFreshZipFilename();
		Files.createFile(destRootDir.resolve(zipToFile));
		String msgToBe = "Filename "+zipToFile+" is taken-- a file by that name already exists in "+destRootDir+".";

		String commandLine = "zipp -s "+sourceRootDir+" -d "+destRootDir+" -z "+zipToFile +" -nr";
		Exception exception = assertThrows(RuntimeException.class, () 
				-> DirectoryZipper.pipe(commandLine.split(" +")));
		assertTrue(exception.getMessage().equals(msgToBe), "Exception should indicate file conflict.");
	}	

	@Test
	void testIllegalSwitch() throws IOException {
		String illegalSwitch = "-illegal";
		String msgPart = "Invalid switch ["
				+ illegalSwitch
				+ "] must be one of ";
		String commandLine = "zipp "+ illegalSwitch;
		Exception exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe(commandLine.split(" +")));
		assertTrue(exception.getMessage().startsWith(msgPart), "Exception should indicate illegal switch -- "+illegalSwitch);

		String commandLine2 = "zipp -s folderA "+ illegalSwitch;
		exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe(commandLine2.split(" +")));
		assertTrue(exception.getMessage().startsWith(msgPart), "Exception should indicate illegal switch -- "+illegalSwitch);
	}	

	@Test
	void testDuplicateSwitch() throws IOException {
		String illegalSwitch = "-s";
		String msgPart = "Duplicate use of switch: "+illegalSwitch;
		String commandLine = "zipp "
				+ "-srcDir folder1 "
				+ illegalSwitch
				+ " folder2";
		Exception exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe(commandLine.split(" +")));		
		assertTrue(exception.getMessage().equals(msgPart), "Exception should indicate duplicate switch -- "+illegalSwitch);
	}	

	@Test
	void testNrArguments() throws IOException {
		String commandLine = "zipp -nr sth ";

		String msgToBe = "The switch -noRecurse does NOT take any arguments";
		Exception exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe(commandLine.split(" +")));
		assertTrue(exception.getMessage().contains(msgToBe), 
				"Exception should indicate illegal argument for switch -- "+Switch.NORECURSE);
	}	

	@Test
	void testTooManyArgumentsForSingleValued() throws IOException {
		String msgPart = " can NOT take multiple arguments";
		Exception exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe("zipp -s sth other".split(" +")));
		assertTrue(exception.getMessage().endsWith(msgPart), 
				"Exception should indicate multiple arguments for single-argument switch -- ");

		exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe("zipp -d sth other".split(" +")));
		assertTrue(exception.getMessage().endsWith(msgPart), 
				"Exception should indicate multiple arguments for single-argument switch -- ");

		exception = assertThrows(IllegalArgumentException.class, () 
				-> DirectoryZipper.pipe("zipp -z sth other".split(" +")));
		assertTrue(exception.getMessage().endsWith(msgPart), 
				"Exception should indicate multiple arguments for single-argument switch -- ");
	}	

	@Test
	void testFilenamesWithNoPathInfo() throws IOException {
		String[] switches = new String[] {"-i", "-e", "-di", "-de", "-z"};

		String illegalFilenameArgument = sourceRootDir+File.separator+"file1.txt";
		String msgPart = " takes file names without the path info";

		Exception exception = null;

		for (String theSwitch: switches) {
			exception = assertThrows(IllegalArgumentException.class, () 
					-> DirectoryZipper.pipe(("zipp " 
							+ theSwitch 
							+ " "
							+ illegalFilenameArgument)
							.split(" +")));
			assertTrue(exception.getMessage().endsWith(msgPart)
					&& exception.getMessage().contains(illegalFilenameArgument), 
					"Exception should indicate that the argument for "
							+ theSwitch+ " should not contain path info -- "+illegalFilenameArgument);
		}
	}	

	public static Set<String> extractZipEntryNames(Path zipFile) throws IOException {
		Set<String> entryNames = new HashSet<>();
		try (FileSystem zipFileSystem = FileSystems.newFileSystem(zipFile, new HashMap<>())) {
			for (Path root : zipFileSystem.getRootDirectories()) 
				Files.walk(root)
				.filter(Files::isRegularFile)
				//                   .forEach(path -> entryNames.add(path.toString()));
				.forEach(path -> entryNames.add(path.toString().replace("/", File.separator)));

		}
		return entryNames;
	}

	@Test
	void testIncludeExclude() throws IOException {
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp "
				+ " -s "+sourceRootDir
				+ " -d "+destRootDir
				+ " -z "+zipToFile 
				+ " -i "+" *.* a*"
				+ " -e "+" *p* "
				+ " -nr "
				;
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = destRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		Set<String> in  = extractZipEntryNames(zipToPath); 
		assertTrue(in.size()==2);
		assertTrue(in.contains(File.separator+"aFile"));  // no File.separator on ZipEntry content
		assertTrue(in.contains(File.separator+".r"));
	}

	static Set<String> getAllFiles(Path folder) throws IOException {
		Set<String> all = Files.walk(folder)
				.filter(Files::isRegularFile)
				.map(path -> path.toString().substring(folder.toString().length()))
				.collect(Collectors.toSet());
		return all;
	}

	@Test
	void testIncludeExcludeNotForSubfolders() throws IOException {
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp "
				+ " -s "+sourceRootDir
				+ " -d "+destRootDir
				+ " -z "+zipToFile 
				+ " -i "+" *.* "
				+ " -e "+" *a* "
				;
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = destRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		Set<String> in  = extractZipEntryNames(zipToPath); 
		Set<String> out = getAllFiles(sourceRootDir);
		out.removeAll(in);

		assertTrue(out.size()==2);
		assertTrue(out.contains(File.separator+"aFile"));
		assertTrue(out.contains(File.separator+"d"));
	}

	@Test
	void testDeepIncludeExclude() throws IOException {
		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp "
				+ " -s "+sourceRootDir
				+ " -d "+destRootDir
				+ " -z "+zipToFile 
				+ " -di "+" *.* "
				+ " -de "+" *a* "
				;
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = destRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		Set<String> in  = extractZipEntryNames(zipToPath); 
		Set<String> all = getAllFiles(sourceRootDir);
		Set<String> out = new HashSet<>(all);
		out.removeAll(in);

		assertTrue(out.size()==3);
		assertTrue(out.contains(File.separator+"d1"+File.separator+"apple.txt"));
		assertTrue(out.contains(File.separator+"d3"+File.separator+"a.log"));
		assertTrue(out.contains(File.separator+"d3"+File.separator+"dpq"));
		assertTrue(in .contains(File.separator+"aFile"), "Should contain the root-folder file `aFile` even thou -de switch excludes it ");
		assertTrue(in .contains(File.separator+"d"), "Should contain the root-folder file `d` even thou -de switch excludes it ");
	}

	@Test
	void testAllFilters() throws IOException {
		String rootFilters  = " -i "+" *a* "
				+ " -e "+" *.* ";
		String subFilters  =  " -de "+" *a* "
				+ " -di "+" *.* ";

		String zipToFile = aFreshZipFilename();
		String commandLine = "zipp "
				+ " -s "+sourceRootDir
				+ " -d "+destRootDir
				+ " -z "+zipToFile 
				+ rootFilters
				+ subFilters;
		DirectoryZipper.pipe(commandLine.split(" +"));

		Path zipToPath = destRootDir.resolve(zipToFile);
		assertTrue(Files.exists(zipToPath), "Zip file "+zipToPath+ " should be created.");

		Set<String> in  = extractZipEntryNames(zipToPath); 
		Set<String> all = getAllFiles(sourceRootDir);
		Set<String> out = new HashSet<>(all);
		out.removeAll(in);

		assertTrue(in .size()==2);
		assertTrue(out.size()==7);
		assertTrue(in .contains(File.separator+"aFile"));
		assertTrue(in .contains(File.separator+"d3"+File.separator+"d.p.q"));

		//-----------------------------------------------

		String zipToFile2 = aFreshZipFilename();
		String commandLine2 = "zipp "
				+ " -s "+sourceRootDir
				+ " -d "+destRootDir
				+ " -z "+zipToFile2 
				+ subFilters;
		DirectoryZipper.pipe(commandLine2.split(" +"));
		Path zipToPath2 = destRootDir.resolve(zipToFile2);
		Set<String> in2  = extractZipEntryNames(zipToPath2); 

		//------------------------		

		String zipToFile3 = aFreshZipFilename();
		String commandLine3 = "zipp "
				+ " -s "+sourceRootDir
				+ " -d "+destRootDir
				+ " -z "+zipToFile3 
				+ rootFilters;
		DirectoryZipper.pipe(commandLine3.split(" +"));
		Path zipToPath3 = destRootDir.resolve(zipToFile3);
		Set<String> in3  = extractZipEntryNames(zipToPath3);
		
		in2.retainAll(in3);
		assertTrue(in.equals(in2));

	}

}

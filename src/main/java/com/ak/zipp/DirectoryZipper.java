package com.ak.zipp;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DirectoryZipper {
	// cache for repeated lookups
	// <K,V> -- K: folder, V: filenames to be zipped in that folder.  
	private Map<Path, Set<String>> dirContentFiles = new HashMap<>();

private Path sourceDir;  
private Path destinationDir;
	private Path zipFilePath; 
	private EnumMap<Switch, Set<String>> zipCommand;

	private DirectoryZipper(EnumMap<Switch, Set<String>> processCommand) {
		zipCommand = processCommand;
	}

	public static void pipe(String[] args) {
		EnumMap<Switch, Set<String>> zipCommand = CommandParser.processCommand(args);
		if (zipCommand==null)
			return; 
		DirectoryZipper dp = new DirectoryZipper(zipCommand);
		dp.setDirectories();
		dp.zipDirWithSwitches();
	}

	private void setDirectories() {
		Set<String> tmpSet; 
		String tmpString=null; 
		
		// set the source folder
		if ((tmpSet=zipCommand.get(Switch.SRCDIR))==null || tmpSet.isEmpty()
				|| !Files.isDirectory(Paths.get(tmpString=tmpSet.iterator().next())))
			 sourceDir = Paths.get(System.getProperty("user.dir"));
		else sourceDir = Paths.get(tmpString);

		// set the destination folder
		if ((tmpSet=zipCommand.get(Switch.DSTDIR))==null || tmpSet.isEmpty()
				|| !Files.isDirectory(Paths.get(tmpString=tmpSet.iterator().next())))
			destinationDir = Paths.get(System.getProperty("user.dir"));
		else destinationDir = Paths.get(tmpString);

		// set the destination file name
		if ((tmpSet=zipCommand.get(Switch.ZIPFILE))!=null && !tmpSet.isEmpty()
				&& !(tmpString=tmpSet.iterator().next()).trim().isEmpty() ) {
			if ( Files.exists(destinationDir.resolve(tmpString)) ) 
				throw new RuntimeException("Filename "+tmpString+" is taken-- a file by that name already exists in "+destinationDir+".");
		} else tmpString = sourceDir.getFileName() + "_" + System.currentTimeMillis() + "_" + 
				"Zipped-on" + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".zip";
		
		// place the destination-zip file inside the destination folder
		zipFilePath = destinationDir.resolve(tmpString);

	}

    /**
     * Compresses the contents of a directory into a ZIP file based on specified switches.
     *
     * <p>The method traverses the source directory and adds its contents to a ZIP archive.
     * The behavior of the compression process is controlled by command-line switches that
     * can include or exclude specific files, set recursive behavior, and define the output
     * ZIP file location and name.</p>
     *
     * <p>Supported switches include:</p>
     * <ul>
     *   <li><b>SRCDIR</b>: Specifies the source directory to compress. Defaults to the current working directory.</li>
     *   <li><b>DSTDIR</b>: Specifies the destination directory for the ZIP file. Defaults to the current working directory.</li>
     *   <li><b>ZIPFILE</b>: Specifies the name of the ZIP file. Defaults to a generated name based on the source directory and timestamp.</li>
     *   <li><b>NORECURSE</b>: Prevents recursion into subdirectories.</li>
     *   <li><b>INCLUDE</b> / <b>EXCLUDE</b>: Include or exclude files in the source directory based on patterns.</li>
     *   <li><b>DEEPINCLUDE</b> / <b>DEEPEXCLUDE</b>: Include or exclude files in subdirectories based on patterns.</li>
     * </ul>
     *
     * <p>The ZIP file is created in the specified or default destination directory.
     * If a file name for the ZIP archive is provided and already exists, an exception is thrown.</p>
     *
     * @throws IOException if an error occurs while creating or writing to the ZIP file
     * @throws RuntimeException if there is an issue with the provided switches 
     */


		private void zipDirWithSwitches() {
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
			Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					// Skip the zip file itself
					if (file.equals(zipFilePath)) 
						return FileVisitResult.SKIP_SUBTREE;

					// skip if the file is excluded from the zip-list 
					if (skip(file))
						return FileVisitResult.SKIP_SUBTREE;
					
					// Write each file to the zip
					String zipEntryName = sourceDir.relativize(file).toString();
					zos.putNextEntry(new ZipEntry(zipEntryName.replace(File.separator, "/")));
					Files.copy(file, zos);
					zos.closeEntry();
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (!sourceDir.equals(dir) && zipCommand.containsKey(Switch.NORECURSE))
						return FileVisitResult.SKIP_SUBTREE;

					String zipEntryName = sourceDir.relativize(dir).toString() + "/";
					zos.putNextEntry(new ZipEntry(zipEntryName.replace(File.separator, "/")));
					zos.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Determines whether a specified file should be skipped during processing.
	 *
	 * <p>The method checks if the file's parent folder 
	 * has been processed before. If not, it filters the folder's 
	 * content based on inclusion and exclusion rules and caches the result. 
	 * The file is skipped if its name 
	 * is not in the set of eligible files for its folder.</p>
	 *
	 * @param file the {@link Path} representing the file to evaluate
	 * @return {@code true} if the file should be skipped; {@code false} otherwise
	 * @throws IOException 
	 */
	private boolean skip(Path file) throws IOException {
		Path folder = file.getParent();
		Set<String> theZippables = null;
		if ((theZippables=dirContentFiles.get(folder))==null) {
			theZippables = filter(folder, 
					zipCommand.get(folder.equals(sourceDir)?Switch.INCLUDE:Switch.DEEPINCLUDE), 
					zipCommand.get(folder.equals(sourceDir)?Switch.EXCLUDE:Switch.DEEPEXCLUDE)					
					);
			dirContentFiles.put(folder, theZippables);
		}
		return !theZippables.contains(file.getFileName().toString());
	}

	/**
	 * Filters the files in a specified folder based on inclusion and exclusion patterns.
	 *
	 * <p>This method retrieves all regular files in the given folder and filters them based
	 * on the specified inclusion and exclusion patterns. Files that match any of the inclusion
	 * patterns and do not match any of the exclusion patterns are returned.
	 *
	 * @param folder the folder containing the files to be filtered; must not be {@code null}
	 * @param includeSet the set of inclusion patterns; must not be {@code null}. Patterns can
	 *                   include wildcards {@code *} and {@code ?}.
	 * @param excludeSet the set of exclusion patterns; must not be {@code null}. Patterns can
	 *                   include wildcards {@code *} and {@code ?}.
	 * @return a set of file names that match the inclusion patterns but not the exclusion patterns
	 * @throws IOException 
	 * @throws RuntimeException if an I/O error occurs while reading files from the folder
	 * @throws NullPointerException if {@code folder}, {@code includeSet}, or {@code excludeSet} is {@code null}
	 */
	private static Set<String> filter(Path folder, Set<String> includeSet, Set<String> excludeSet) throws IOException {
		Set<String> fileNames = new HashSet<>();
			fileNames = Files.list(folder)
					.filter(Files::isRegularFile)
					.map(path -> path.getFileName().toString())
					.collect(Collectors.toSet());

		Set<String> include = includeSet.equals(INCLUDE_DEFAULT)?fileNames:filterByPatterns(fileNames, includeSet);
		Set<String> exclude = excludeSet.equals(EXCLUDE_DEFAULT)?EXCLUDE_DEFAULT:filterByPatterns(fileNames, excludeSet);

		include.removeAll(exclude);
		return include;
	}

	static final Set<String> INCLUDE_DEFAULT=Set.of("*");
	static final Set<String> EXCLUDE_DEFAULT=Set.of();
	
	/**
	 * Filters a set of file names by matching them against a set of patterns.
	 *
	 * <p>The patterns support wildcards where {@code *} matches zero or more characters,
	 * and {@code ?} matches exactly one character. File names that match any of the
	 * patterns are included in the result.
	 *
	 * @param fileNames the set of file names to filter; must not be {@code null}
	 * @param patterns the set of patterns to match against; must not be {@code null}
	 * @return a set of file names that match any of the specified patterns
	 * @throws NullPointerException if {@code fileNames} or {@code patterns} is {@code null}
	 */
	private static Set<String> filterByPatterns(Set<String> fileNames, Set<String> patterns) {
        return fileNames.stream()
                .filter(fileName -> patterns.stream()
                        .anyMatch(pattern -> {
                            String adjustedPattern = (pattern.endsWith(".")
                                    ? pattern.substring(0, pattern.length() - 1)
                                    : pattern)
                                    .replace(".", "\\.")
                                    .replace("?", ".?")
                                    .replace("*", ".*?");
                            return Pattern.matches(adjustedPattern, fileName);
                        }))
                .collect(Collectors.toSet());
    }

}

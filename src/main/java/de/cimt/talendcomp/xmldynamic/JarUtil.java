package de.cimt.talendcomp.xmldynamic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarUtil {
	
	private String classFilesDirPath = null;
	private String jarFilePath = null;
	
	
	public String getClassFilesDir() {
		return classFilesDirPath;
	}
	
	public void setClassFilesDir(String classFilesDir) {
		if (classFilesDir == null || classFilesDir.trim().isEmpty()) {
			throw new IllegalArgumentException("classFilesDir cannot be null or empty");
		}
		this.classFilesDirPath = classFilesDir.trim();
	}
	
	public String getJarFilePath() {

		return jarFilePath;
	}
	
	public void setJarFilePath(String jarFilePath) {
		if (jarFilePath == null || jarFilePath.trim().isEmpty()) {
			throw new IllegalArgumentException("JarFilePath cannot be null or empty");
		}
		this.jarFilePath = jarFilePath.trim();
	}
	
	
	public void create() throws Exception {
		if (classFilesDirPath == null) {
			throw new IllegalStateException("classFilesDir not set");
		}
		File classFilesDir = new File(classFilesDirPath);
		
		if (classFilesDir.exists()) {
			if (classFilesDir.canRead() == false) {
				throw new Exception("Source dir: " + classFilesDir.getAbsolutePath() + " is not readable !");
			}
		}
		
		if (jarFilePath == null) {
			throw new IllegalStateException("jarFilePath not set");
		}
		File jarFile = new File(jarFilePath);
		File jarFileParent = jarFile.getParentFile();
		if (jarFileParent.exists()) {
			if (jarFileParent.isFile()) {
				throw new Exception("cannot use target dir: " + jarFileParent.getAbsolutePath() + " already exists as file!");
			} else if (jarFileParent.canWrite() == false) {
				throw new Exception("Target dir: " + jarFileParent.getAbsolutePath() + " is read only!");
			}
		} else {
			if (jarFileParent.mkdirs() == false) {
				throw new Exception("Could not create jar output dir: " + jarFileParent.getAbsolutePath());
			}
		}
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		JarOutputStream target = null;
		try {
			target = new JarOutputStream(new FileOutputStream(jarFile), manifest);
			add(new File(classFilesDirPath), target);
		} finally {
			if (target != null) {
				target.close();
			}
		}
	}
	
	private void add(File sourceDir, JarOutputStream target) throws IOException {
		BufferedInputStream in = null;
		try {
			if (sourceDir.isDirectory()) {
				String name = sourceDir.getPath().replace("\\", "/");
				if (name.isEmpty() == false) {
					if (name.endsWith("/") == false) {
						name += "/";
					}
					JarEntry entry = new JarEntry(name);
					entry.setTime(sourceDir.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				for (File nestedFile : sourceDir.listFiles()) {
					add(nestedFile, target);
				}
				return;
			}

			JarEntry entry = new JarEntry(sourceDir.getPath().replace("\\", "/"));
			entry.setTime(sourceDir.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(sourceDir));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1) {
					break;
				}
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}

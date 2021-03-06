package com.pack.tools.novdl.comp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pack.tools.novdl.BookManager;
import com.pack.tools.novdl.db.BookDbManager;

public class FileDataFix {
	private Set<String> updated = new HashSet<String>();
	private List<String> newUpdates = new ArrayList<>();

	public boolean fixData(File file) {
		if (this.isUpdated(file)) {
			return false;
		}
		if (file.exists() && file.isFile() && file.canRead()) {
			try {
				final BufferedReader reader = new BufferedReader(new FileReader(file));
				final StringBuffer buffer = new StringBuffer();
				String line = null;
				while ((line = reader.readLine()) != null) {
					buffer.append(line + "\n");
				}
				reader.close();
				final CompositeExecutor fixExcecutor = new CompositeExecutor();
				fixExcecutor.addComposite(new TextReplaceComposite("�", "!"));
				// fixExcecutor.addComposite(new ContentRemoverComposite("<!--
				// Composite Start -->",
				// "<!-- Composite End -->"));
				fixExcecutor.addComposite(
						new FirstContentRemovalComposite("<center><div id=\"ezoic-pub-ad-placeholder", "</div>\n</center>"));
				fixExcecutor.addComposite(new FirstContentRemovalComposite("<center><script async", "</script>\n</center>"));
				fixExcecutor.addComposite(new FirstContentRemovalComposite("<br><center><div data-pw-desk", "</div></center><br>"));
				fixExcecutor.addComposite(new FirstContentRemovalComposite("<br><center><script id=\"m_iframe_tag\"", "</script></center><br>"));
				// fixExcecutor.addComposite(new
				// ContentReplaceComposite("<br><br>", ""));
				// fixExcecutor.addComposite(new ContentRemoverComposite("",
				// ""));
				final String data = fixExcecutor.execute(buffer.toString());
				if (!data.equals(buffer.toString()) && file.canWrite()) {
					final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
					writer.write(data);
					writer.flush();
					writer.close();
					System.out.println("updated File:" + file.getAbsolutePath());
					this.newUpdates.add(file.getAbsolutePath());
					return true;
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean isUpdated(File file) {
		return updated.contains(file.getAbsolutePath());
	}

	private void loadUpdatedFiles() {
		File file = BookDbManager.getDataDir().resolve("FixerUpdted.txt").toFile();
		if (file != null && file.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.trim().length() > 0) {
						updated.add(line);
					}
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void writeUpdates() {
	    if(newUpdates.isEmpty()) {
	        return;
	    }
		File file = BookDbManager.getDataDir().resolve("FixerUpdted.txt").toFile();
		try {
			String data = "";
			for (String str : newUpdates) {
				data += str + "\n";
			}
			if (!file.exists()) {
				file.createNewFile();
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.write(data);
			writer.flush();
			writer.close();
			this.newUpdates = new ArrayList<>();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public FileDataFix() {
		loadUpdatedFiles();
	}

	public boolean checkPath(File file) {
		if (file.isDirectory()) {
			final File files[] = file.listFiles();
			for (final File f : files) {
				checkPath(f);
			}
		} else if (file.isFile()) {
			if (file.getName().endsWith(".xhtml")) {
				fixData(file);
			}
		}
		writeUpdates();
		return false;
	}

	public static void main(String args[]) {
		final FileDataFix fixer = new FileDataFix();
		final File file = BookManager.getPathFull().toFile();
		fixer.checkPath(file);
	}
}

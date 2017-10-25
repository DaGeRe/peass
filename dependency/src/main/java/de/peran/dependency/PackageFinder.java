package de.peran.dependency;

import java.io.File;

public class PackageFinder {
	public static String getLowestPackageOverall(File projectFolder) {
		File src = new File(projectFolder, "src");
		File main = new File(src, "main");
		File mainJava = new File(src, "java");
		File test = new File(src, "test");
		String lowestPackage = "";
		if (mainJava.exists()) {
			String javaLowestPackage = getLowestPackage(mainJava);
			if (!javaLowestPackage.equals("") && !lowestPackage.equals("")) {
				lowestPackage = greatestCommonPrefix(lowestPackage, javaLowestPackage);
			} else {
				lowestPackage = javaLowestPackage;
			}
		} else {
			if (main.exists()) {
				File java = new File(main, "java");
				if (java.exists()) {
					lowestPackage = getLowestPackage(java);
				} else {
					lowestPackage = getLowestPackage(main);
				}
			}
		}

		if (test.exists()) {
			String testLowestPackage;
			File java = new File(test, "java");
			if (java.exists()) {
				testLowestPackage = getLowestPackage(java);
			} else {
				testLowestPackage = getLowestPackage(test);
			}
			lowestPackage = greatestCommonPrefix(lowestPackage, testLowestPackage);
		}
		return lowestPackage;
	}

	private static String getLowestPackage(File root) {
		String lowest = "";
		File current = root;
		while (current.listFiles().length == 1) {
			File candidate = current.listFiles()[0];
			if (candidate.isDirectory()) {
				lowest += candidate.getName() + ".";
				current = candidate;
			} else {
				break;
			}
		}
		return lowest.length() > 0 ? lowest.substring(0, lowest.length() - 1) : lowest;

	}

	public static String greatestCommonPrefix(String a, String b) {
		int minLength = Math.min(a.length(), b.length());
		for (int i = 0; i < minLength; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a.substring(0, minLength);
	}
}

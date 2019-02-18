package de.peran;

public class Environment {
	public static final boolean DRAW_RESULTS = System.getenv("DRAW_RESULTS") != null ? Boolean.parseBoolean(System.getenv("DRAW_RESULTS")) : false;
}

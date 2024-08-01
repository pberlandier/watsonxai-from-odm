package com.acme;

public enum Sentiment {
	POSITIVE, NEGATIVE, UNKNOWN;
	
	public static Sentiment map(String value) {
		switch (value) {
			case "negative": return NEGATIVE;
			case "positive": return POSITIVE;
			default: return UNKNOWN;
		}
	}
}

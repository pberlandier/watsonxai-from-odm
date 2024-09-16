package com.acme;

public enum Sentiment {
	POSITIVE, NEGATIVE, UNKNOWN;
	
	public static Sentiment map(String value) {
		if ( value == null ) {
			return UNKNOWN;
		}
		switch (value) {
			case "negative": return NEGATIVE;
			case "positive": return POSITIVE;
			default: return UNKNOWN;
		}
	}
}

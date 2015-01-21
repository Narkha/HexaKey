package es.csc.android.hexakey;

import java.util.HashMap;

import android.content.Context;
import android.view.inputmethod.InputMethodSubtype;

public class LetterRecourcesCache extends HashMap<String, Integer> {
	private static final String LETTERS_XML_PREFIX = "letters_";	
	private static final String DEFAULT_LOCALE = "default";
	
	public int get(Context context, InputMethodSubtype subtype) {
		String locale = subtype.getLocale();
		if (this.containsKey(locale)) {
			return get(locale);
		}
		else {
			int resourceId = findLocaleOrDefault(context, locale);
			put(locale, resourceId);
			return resourceId;
		}
	}

	private int findLocaleOrDefault(Context context, String locale) {
		int resourceId = context.getResources().getIdentifier(LETTERS_XML_PREFIX + locale, "xml", context.getPackageName());		
		if (resourceId == 0) {
			resourceId = context.getResources().getIdentifier(LETTERS_XML_PREFIX + DEFAULT_LOCALE, "xml", context.getPackageName());
		}
		return resourceId;
	}
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package es.csc.android.hexakey;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.view.inputmethod.InputMethodSubtype;

public class LatinKeyboardSetCache {
	public static final String LETTERS_XML_PREFIX = "letters_";
	
	private static final String DEFAULT_LOCALE = "default";
	
	private static Map<String, LatinKeyboardSet> cache;
	
	static {
		cache = new HashMap<String, LatinKeyboardSet>();
	}

	
	public static LatinKeyboardSet getKeyboardLayoutSet(Context context, InputMethodSubtype subtype) {
		String locale = subtype.getLocale();
		if (!existsLocaleKeyboard(context, locale)) {
			locale = DEFAULT_LOCALE;
		}
		
		if (cache.containsKey(locale)) {
			return cache.get(locale);			
		}		
		else {
			return createAndAddKeyboardSet(context, locale);
		}
	}

	private static LatinKeyboardSet createAndAddKeyboardSet(Context context, String locale) {		
		LatinKeyboardSet result = new LatinKeyboardSet(context, locale);
		cache.put(locale, result);
		return result;
	}

	private static boolean existsLocaleKeyboard(Context context, String locale) {
		return context.getResources().getIdentifier(LETTERS_XML_PREFIX + locale, "xml", context.getPackageName()) != 0;		
	}
}

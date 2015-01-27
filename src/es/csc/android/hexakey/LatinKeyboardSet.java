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

import android.content.Context;
import android.content.res.Resources;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodSubtype;

public class LatinKeyboardSet {
	public static final int LETTERS_KEYBOARD = 1;
	public static final int NUMBERS_KEYBOARD = 2;
	
	private static LetterRecourcesCache letterResourcesCache;
	
	private Context context;
	private int lettersResourceId;
	
    private LatinKeyboard lettersKeyboard;
    private LatinKeyboard symbolsKeyboard;
    private LatinKeyboard symbolsShiftedKeyboard;
    private LatinKeyboard numbersKeyboard;
    
    private LatinKeyboard defaultKeyboard;
    private LatinKeyboard currentKeyboard;
        
    private long lastShiftTime;
    private boolean capsLock;
    
    static {
    	letterResourcesCache = new LetterRecourcesCache();
    }
    
	public LatinKeyboardSet(Context context, InputMethodSubtype subtype) {
		this.context = context;
		
		lettersResourceId = letterResourcesCache.get(context, subtype);
        lettersKeyboard = new LatinKeyboard(context, lettersResourceId);        					
        						
        symbolsKeyboard = new LatinKeyboard(context, R.xml.symbols);
        symbolsShiftedKeyboard = new LatinKeyboard(context, R.xml.symbols_shift);
        
        numbersKeyboard = new LatinKeyboard(context, R.xml.numbers);
        
        defaultKeyboard = currentKeyboard = lettersKeyboard;
	}
	
	public void recreateKeyboards() {
		LatinKeyboard newLettersKeyboard = new LatinKeyboard(context, lettersResourceId);        					
		
		LatinKeyboard newSymbolsKeyboard = new LatinKeyboard(context, R.xml.symbols);
		LatinKeyboard newSymbolsShiftedKeyboard = new LatinKeyboard(context, R.xml.symbols_shift);
        
		LatinKeyboard newNumbersKeyboard = new LatinKeyboard(context, R.xml.numbers);
		
		updateDefaultKeyboard(newLettersKeyboard, newNumbersKeyboard);
		
		updateCurrentKeyboard(newLettersKeyboard, newSymbolsKeyboard,
				newSymbolsShiftedKeyboard, newNumbersKeyboard);
		
		lettersKeyboard = newLettersKeyboard;
		symbolsKeyboard = newSymbolsKeyboard;
		symbolsShiftedKeyboard = newSymbolsShiftedKeyboard;
		numbersKeyboard = newNumbersKeyboard;	
	}

	private void updateDefaultKeyboard(LatinKeyboard newLettersKeyboard,
			LatinKeyboard newNumbersKeyboard) {
		if (defaultKeyboard == lettersKeyboard) {
			defaultKeyboard = newLettersKeyboard;
		}
		else {			
			defaultKeyboard = newNumbersKeyboard;
		}
	}

	private void updateCurrentKeyboard(LatinKeyboard newLettersKeyboard,
			LatinKeyboard newSymbolsKeyboard,
			LatinKeyboard newSymbolsShiftedKeyboard,
			LatinKeyboard newNumbersKeyboard) {
		
		if (currentKeyboard == lettersKeyboard) {
			this.currentKeyboard = newLettersKeyboard;
		}
		else if (currentKeyboard == symbolsKeyboard) {
			currentKeyboard = newSymbolsKeyboard;
		}
		else if (currentKeyboard == symbolsShiftedKeyboard) {
			currentKeyboard = newSymbolsShiftedKeyboard;
		}
		else {			
			this.currentKeyboard = newNumbersKeyboard;
		}
	}
	
	public void resetStatus() {
		currentKeyboard = defaultKeyboard;
		capsLock = false;
	}
	
	public LatinKeyboard getCurrentKeyboard() {
		return currentKeyboard;
	}

	public void updateKeyboardType(EditorInfo attribute) {
		updateKeyboardType( typeFromAttribute(attribute) );
	}

	public int typeFromAttribute(EditorInfo attribute) {		
		// TODO cover all the cases
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
        	case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_NUMBER:                            
            case InputType.TYPE_CLASS_PHONE:
            	return NUMBERS_KEYBOARD;
                
            case InputType.TYPE_CLASS_TEXT:            	                
            default:
            	return LETTERS_KEYBOARD;
        }
	}

	public void updateKeyboardType(int type) {        
        switch (type) {                    
            case NUMBERS_KEYBOARD:
            	defaultKeyboard = currentKeyboard = numbersKeyboard;
            	break;
                
            case LETTERS_KEYBOARD:       
            default:
            	defaultKeyboard = currentKeyboard = lettersKeyboard;
        }
	}
	
	public boolean isKeyboardType(EditorInfo attribute) {	
		return isKeyboardType(typeFromAttribute(attribute));
	}
	
	public boolean isKeyboardType(int keyboardType) {	     
        switch (keyboardType) {
            case NUMBERS_KEYBOARD:            
                return currentKeyboard == numbersKeyboard;
                
            case LETTERS_KEYBOARD:       
            default:
            	return currentKeyboard == lettersKeyboard || 
                		currentKeyboard == symbolsKeyboard ||
                		currentKeyboard == symbolsShiftedKeyboard;
        }
	}
	

	
	public int getKeyboardType() {	     
        if (currentKeyboard == numbersKeyboard) {
        	return NUMBERS_KEYBOARD;
        }
        else {
        	return LETTERS_KEYBOARD;
        }
	}

	public void setImeOptions(Resources res, int options) {
		currentKeyboard.setImeOptions(res, options);	
	}

	public void changeKeyboardMode() {
		if (currentKeyboard == lettersKeyboard) {
			currentKeyboard = symbolsKeyboard;
		}
		else {
			currentKeyboard = lettersKeyboard;
		}		
		
		currentKeyboard.setShifted(false);
	}
	
	public boolean isCapsLockEnabled() {
		return capsLock;
	}

	/**
	 * update the information when the key is pressed
	 * 
	 * @result true if the keyboard has changed (symbols to symbols shifted or viceversa)
	 * @result false if the keyboard has not changed (is necesay call outside setH
	 */
	public void handleShift() {
        if (lettersKeyboard == currentKeyboard) {   
    		checkToggleCapsLock();
        } 
        else if (currentKeyboard == symbolsKeyboard) {
        	currentKeyboard = symbolsShiftedKeyboard;
        	symbolsShiftedKeyboard.setShifted(true);
        } 
        else {
        	currentKeyboard = symbolsKeyboard;
            symbolsKeyboard.setShifted(true);
        }
	}
	
    private void checkToggleCapsLock() {
    	long now = System.currentTimeMillis();
    	if (capsLock) {
    		capsLock = false;
    		lastShiftTime = 0;
    	}
    	else {
    		if (lettersKeyboard.isShifted() && (lastShiftTime + 400 > now)) {
	            capsLock = true;
	            lastShiftTime = 0;
	    	}
	    	else {
	        	capsLock = false;
	            lastShiftTime = now;
	        }
    	}
    }
}

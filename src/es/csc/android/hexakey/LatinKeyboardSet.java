package es.csc.android.hexakey;

import android.content.Context;
import android.content.res.Resources;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public class LatinKeyboardSet {
	public static final int LETTERS_KEYBOARD = 1;
	public static final int SYMBOLS_KEYBOARD = 2;
	public static final int SGIFT_SYMBOLS_KEYBOARD = 3;
	
    private LatinKeyboard symbolsKeyboard;
    private LatinKeyboard symbolsShiftedKeyboard;
    private LatinKeyboard lettersKeyboard;
    
    private LatinKeyboard defaultKeyboard;
    private LatinKeyboard currentKeyboard;
        
    private long lastShiftTime;
    private boolean capsLock;
    
	public LatinKeyboardSet(Context context) {
        lettersKeyboard = new LatinKeyboard(context, R.xml.letters);
        symbolsKeyboard = new LatinKeyboard(context, R.xml.symbols);
        symbolsShiftedKeyboard = new LatinKeyboard(context, R.xml.symbols_shift);
        
        defaultKeyboard = currentKeyboard = lettersKeyboard;
	}


	public void resetStatus() {
		currentKeyboard = defaultKeyboard;
	}
	
	public LatinKeyboard getCurrentKeyboard() {
		return currentKeyboard;
	}

	public void updateKeyboardType(EditorInfo attribute) {
		updateKeyboardType( typeFromAttribute(attribute) );
	}

	public int typeFromAttribute(EditorInfo attribute) {		
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:                
            case InputType.TYPE_CLASS_PHONE:
            	return SYMBOLS_KEYBOARD;
                
            case InputType.TYPE_CLASS_TEXT:            	                
            default:
            	return LETTERS_KEYBOARD;
        }
	}

	public void updateKeyboardType(int type) {        
        switch (type) {
            case SYMBOLS_KEYBOARD:            
                currentKeyboard = symbolsKeyboard;
                break;
                
            case LETTERS_KEYBOARD:       
            default:
            	currentKeyboard = lettersKeyboard;
        }
	}
	
	public boolean isKeyboardType(int keyboardType) {	     
        switch (keyboardType) {
            case SYMBOLS_KEYBOARD:            
                return currentKeyboard == symbolsKeyboard;
                
            case LETTERS_KEYBOARD:       
            default:
            	return currentKeyboard == lettersKeyboard;
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
	public boolean isLettersUpperCase() {
		return capsLock || !currentKeyboard.isShifted();
	}

	public boolean handleShift() {
        if (lettersKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            return false;
        } 
        else if (currentKeyboard == symbolsKeyboard) {
        	currentKeyboard = symbolsShiftedKeyboard;
        	symbolsShiftedKeyboard.setShifted(true);
            return true;
        } 
        else {
        	currentKeyboard = symbolsKeyboard;
            symbolsKeyboard.setShifted(true);
        	return true;
        }
	}
	
    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (lastShiftTime + 800 > now) {
            capsLock = !capsLock;
            lastShiftTime = 0;
        } 
        else {
            lastShiftTime = now;
        }        
    }

}
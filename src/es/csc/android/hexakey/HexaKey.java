/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.csc.android.hexakey;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.IBinder;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

import es.csc.android.hexakey.R;

public class HexaKey extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;

    private InputMethodManager inputMethodManager;

    private LatinKeyboardView inputView;
    
    private int lastDisplayWidth;
    
    private LatinKeyboardSet keyboardSet;
    
    private String wordSeparators;
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override 
    public void onCreate() {
        super.onCreate();
        inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        wordSeparators = getResources().getString(R.string.word_separators);
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override 
    public void onInitializeInterface() {
        if (keyboardSet != null && !isScreenRotation()) {
            return;            
        }
        
        lastDisplayWidth = getMaxWidth();
        
        final InputMethodSubtype subtype = inputMethodManager.getCurrentInputMethodSubtype();
        keyboardSet = new LatinKeyboardSet(this, subtype);
    }
    
    private boolean isScreenRotation() {
    	return getMaxWidth() != lastDisplayWidth;
    }    

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override 
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        if (inputView != null && ! keyboardSet.isKeyboardType(attribute)) {
        	inputView.clearBackground();
        }
        
        keyboardSet.updateKeyboardType(attribute);
        updateCapsLockState(attribute);
        keyboardSet.setImeOptions(getResources(), attribute.imeOptions);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override 
    public View onCreateInputView() {
        inputView = (LatinKeyboardView) getLayoutInflater()
        				.inflate(R.layout.input, null);
		inputView.autoAdjustPadding(getMaxWidth());
        inputView.setOnKeyboardActionListener(this);
        setLatinKeyboard(keyboardSet.getCurrentKeyboard());
        return inputView;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT) 
    private void setLatinKeyboard(LatinKeyboard nextKeyboard) {
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
	        final boolean shouldSupportLanguageSwitchKey =
	                inputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());
	        nextKeyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
    	}
        inputView.setKeyboard(nextKeyboard);
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override 
    public View onCreateCandidatesView() {
       return null;
    }    
    
    @Override 
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        
        setLatinKeyboard(keyboardSet.getCurrentKeyboard());
        inputView.closing();
        final InputMethodSubtype subtype = inputMethodManager.getCurrentInputMethodSubtype();
        inputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onFinishInputView (boolean finishingInput) {
        keyboardSet.resetStatus();
        super.onFinishInputView(finishingInput);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override 
    public void onFinishInput() {
        super.onFinishInput();
        
        if (inputView != null) {
            inputView.closing();
        }
    }
    
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        inputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override 
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && inputView != null) {
                    if (inputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;                
        }
        
        return super.onKeyDown(keyCode, event);
    }

    private void updateCapsLockState(EditorInfo attr) {
        if (attr != null && inputView != null 
        		&& keyboardSet.isKeyboardType(LatinKeyboardSet.LETTERS_KEYBOARD)) {
            inputView.setShifted( keyboardSet.isCapsLockEnabled() || isFirstCapitalLetter(attr) );            
        }
    }

	private boolean isFirstCapitalLetter(EditorInfo attr) {
		boolean firstLetterAndCapital = false;
		
		EditorInfo ei = getCurrentInputEditorInfo();
		if (ei != null && ei.inputType != InputType.TYPE_NULL) {
			// For some weird reason, sometimes the result is false in the first call and true in the second...
		    firstLetterAndCapital = getCurrentInputConnection().getCursorCapsMode(attr.inputType) != 0
		    						|| getCurrentInputConnection().getCursorCapsMode(attr.inputType) != 0;		 
		}
		return firstLetterAndCapital;
	}
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            sendKey(primaryCode);
            updateCapsLockState(getCurrentInputEditorInfo());
        } 
        else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } 
        else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } 
        else if (primaryCode == LatinKeyboard.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            return;
        } 
        else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } 
        else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && inputView != null) {
        	keyboardSet.changeKeyboardMode();
        	setLatinKeyboard(keyboardSet.getCurrentKeyboard());
        } 
        else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateCapsLockState(getCurrentInputEditorInfo());
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
    }
    
    private void handleBackspace() {
        keyDownUp(KeyEvent.KEYCODE_DEL);
        updateCapsLockState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (inputView == null) {
            return;
        }
        android.util.Log.d("___", "handleShift");
        
        LatinKeyboard preShifted = keyboardSet.getCurrentKeyboard();
        keyboardSet.handleShift();
        if (preShifted == keyboardSet.getCurrentKeyboard()) {
        	inputView.setShifted( keyboardSet.isCapsLockEnabled() || !inputView.isShifted() );        	
        }    
        else {
        	setLatinKeyboard(keyboardSet.getCurrentKeyboard());
        }
    }
    
     private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (inputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
                        
        getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        
        if (isAlphabet(primaryCode)) {            
        	updateCapsLockState(getCurrentInputEditorInfo());
        }
    }
    
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    private void handleClose() {
        requestHideSelf(0);
        inputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleLanguageSwitch() {
        final InputMethodSubtype previousSubtype = inputMethodManager.getCurrentInputMethodSubtype();
        inputMethodManager.switchToNextInputMethod(getToken(), true);
        final InputMethodSubtype newSubtype = inputMethodManager.getCurrentInputMethodSubtype();
        
        if (previousSubtype != newSubtype) {        	
        	keyboardSet = new LatinKeyboardSet(this, newSubtype);
        	setLatinKeyboard(keyboardSet.getCurrentKeyboard());
        }
    }
    
    private String getWordSeparators() {
        return wordSeparators;
    }
    
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    public void swipeRight() {
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }
}

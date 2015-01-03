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
    private boolean capsLock;
    private long lastShiftTime;
    
    private LatinKeyboard symbolsKeyboard;
    private LatinKeyboard symbolsShiftedKeyboard;
    private LatinKeyboard lettersKeyboard;
    
    private LatinKeyboard curKeyboard;
    
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
        if (lettersKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == lastDisplayWidth) return;
            lastDisplayWidth = displayWidth;
        }
        lettersKeyboard = new LatinKeyboard(this, R.xml.letters);
        symbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        symbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
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
        setLatinKeyboard(lettersKeyboard);
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

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override 
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                curKeyboard = symbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                curKeyboard = symbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                curKeyboard = lettersKeyboard;               
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                curKeyboard = lettersKeyboard;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        curKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override 
    public void onFinishInput() {
        super.onFinishInput();
        
        curKeyboard = lettersKeyboard;
        if (inputView != null) {
            inputView.closing();
        }
    }
    
    @Override 
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        setLatinKeyboard(curKeyboard);
        inputView.closing();
        final InputMethodSubtype subtype = inputMethodManager.getCurrentInputMethodSubtype();
        inputView.setSubtypeOnSpaceKey(subtype);
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

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && inputView != null && lettersKeyboard == inputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            inputView.setShifted(capsLock || caps != 0);
        }
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
            updateShiftKeyState(getCurrentInputEditorInfo());
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
            Keyboard current = inputView.getKeyboard();
            if (current == symbolsKeyboard || current == symbolsShiftedKeyboard) {
                setLatinKeyboard(lettersKeyboard);
            } 
            else {
                setLatinKeyboard(symbolsKeyboard);
                symbolsKeyboard.setShifted(false);
            }
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
        updateShiftKeyState(getCurrentInputEditorInfo());
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
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (inputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = inputView.getKeyboard();
        if (lettersKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            inputView.setShifted(capsLock || !inputView.isShifted());
        } else if (currentKeyboard == symbolsKeyboard) {
            symbolsKeyboard.setShifted(true);
            setLatinKeyboard(symbolsShiftedKeyboard);
            symbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == symbolsShiftedKeyboard) {
            symbolsShiftedKeyboard.setShifted(false);
            setLatinKeyboard(symbolsKeyboard);
            symbolsKeyboard.setShifted(false);
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
        inputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (lastShiftTime + 800 > now) {
            capsLock = !capsLock;
            lastShiftTime = 0;
        } else {
            lastShiftTime = now;
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

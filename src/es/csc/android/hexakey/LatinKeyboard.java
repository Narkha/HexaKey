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

import es.csc.android.hexakey.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

public class LatinKeyboard extends Keyboard {

    private Key enterKey;
    private Key spaceKey;
    /**
     * Stores the current state of the mode change key. Its width will be dynamically updated to
     * match the region of {@link #modeChangeKey} when {@link #modeChangeKey} becomes invisible.
     */
    private Key modeChangeKey;
    /**
     * Stores the current state of the language switch key (a.k.a. globe key). This should be
     * visible while {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod(IBinder)}
     * returns true. When this key becomes invisible, its width will be shrunk to zero.
     */
    private Key languageSwitchKey;
    /**
     * Stores the size and other information of {@link #modeChangeKey} when
     * {@link #languageSwitchKey} is visible. This should be immutable and will be used only as a
     * reference size when the visibility of {@link #languageSwitchKey} is changed.
     */
    private Key savedModeChangeKey;
    /**
     * Stores the size and other information of {@link #languageSwitchKey} when it is visible.
     * This should be immutable and will be used only as a reference size when the visibility of
     * {@link #languageSwitchKey} is changed.
     */
    private Key savedLanguageSwitchKey;
    
    public LatinKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public LatinKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser) {
        Key key = new LatinKey(res, parent, x, y, parser);
        if (key.codes[0] == 10) {
            enterKey = key;
        } else if (key.codes[0] == ' ') {
            spaceKey = key;
        } else if (key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
            modeChangeKey = key;
            savedModeChangeKey = new LatinKey(res, parent, x, y, parser);
        } else if (key.codes[0] == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            languageSwitchKey = key;
            savedLanguageSwitchKey = new LatinKey(res, parent, x, y, parser);
        }
        return key;
    }

    /**
     * Dynamically change the visibility of the language switch key (a.k.a. globe key).
     * @param visible True if the language switch key should be visible.
     */
    void setLanguageSwitchKeyVisibility(boolean visible) {
        if (visible) {
            // The language switch key should be visible. Restore the size of the mode change key
            // and language switch key using the saved layout.
            modeChangeKey.width = savedModeChangeKey.width;
            modeChangeKey.x = savedModeChangeKey.x;
            languageSwitchKey.width = savedLanguageSwitchKey.width;
            languageSwitchKey.icon = savedLanguageSwitchKey.icon;
            languageSwitchKey.iconPreview = savedLanguageSwitchKey.iconPreview;
        } else {
            // The language switch key should be hidden. Change the width of the mode change key
            // to fill the space of the language key so that the user will not see any strange gap.
            modeChangeKey.width = savedModeChangeKey.width + savedLanguageSwitchKey.width;
            languageSwitchKey.width = 0;
            languageSwitchKey.icon = null;
            languageSwitchKey.iconPreview = null;
        }
    }

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    void setImeOptions(Resources res, int options) {
        if (enterKey == null) {
            return;
        }

        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                enterKey.iconPreview = null;
                enterKey.icon = null;
                enterKey.label = res.getText(R.string.label_go_key);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                enterKey.iconPreview = null;
                enterKey.icon = null;
                enterKey.label = res.getText(R.string.label_next_key);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                enterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
                enterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
                enterKey.iconPreview = null;
                enterKey.icon = null;
                enterKey.label = res.getText(R.string.label_send_key);
                break;
            default:
                enterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
                enterKey.label = null;
                break;
        }
    }

    void setSpaceIcon(final Drawable icon) {
        if (spaceKey != null) {
            spaceKey.icon = icon;
        }
    }

    static class LatinKey extends Keyboard.Key {
        
        public LatinKey(Resources res, Keyboard.Row parent, int x, int y,
                XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }
        
        /**
         * Overriding this method so that we can reduce the target area for the key that
         * closes the keyboard. 
         */
        @Override
        public boolean isInside(int x, int y) {
            return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
        }
    }

}

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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;
    // TODO: Move this into android.inputmethodservice.Keyboard
    static final int KEYCODE_LANGUAGE_SWITCH = -101;
    
    boolean isCapturedBackground = false;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    
    @Override
    public void onDraw (Canvas canvas) {
    	if(!isCapturedBackground) {
    		isCapturedBackground = true;

            Bitmap bitmap = createBitmapForCanvas(canvas);   
            
            canvas.setBitmap(bitmap);                      
            setActualBackgroundColor(canvas);
            
            super.onDraw(canvas);
                                   
            BitmapDrawable newBackground = getKeyboardSnapshot(bitmap);
                        
            this.setBackground(newBackground);                       
    	}
    	else {
    		super.onDraw(canvas);
    	}
    }

	private Bitmap createBitmapForCanvas(Canvas canvas) {
		int width = Math.max(1, canvas.getWidth());
		int height = Math.max(1, canvas.getHeight()); 				
		
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		return bitmap;
	}

	private void setActualBackgroundColor(Canvas canvas) {
		try {
			ColorDrawable background = (ColorDrawable) this.getBackground();
			int color = background.getColor();
		
			int alpha = (color & 0xff000000) >> 24;
			int red = (color & 0x00ff0000) >> 16;
			int green = (color & 0x0000ff00) >> 8;
			int blue = (color & 0x000000ff);
			
			canvas.drawARGB(alpha, red, green, blue);
		}
		catch (ClassCastException e) {
			Log.e("Hexakey", "The background is not a color", e);
		}
	}

	private BitmapDrawable getKeyboardSnapshot(Bitmap bitmap) {
		Bitmap KeyboardSnapshot = null;
		if (isPortraitOrientation(bitmap)) {
			KeyboardSnapshot = bitmap;
		}
		else {
			KeyboardSnapshot = cutKeyboardArea(bitmap);				
		}
		
		return new BitmapDrawable(getResources(), KeyboardSnapshot);
	}

	private boolean isPortraitOrientation(Bitmap bitmap) {
		return bitmap.getWidth() < 700;
	}
	
	private Bitmap cutKeyboardArea(Bitmap bitmap) {
		return Bitmap.createBitmap(bitmap, 
									0, bitmap.getHeight() - getHeight(),
									getWidth(), getHeight());
	}


    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } 
        else {
            return super.onLongPress(key);
        }
    }
	
	public void updatePadding(int maxWidth) {
		final float keyWidth = getResources().getFraction(R.fraction.key_width, 1, 1);
		final float totalKeysWidth = keyWidth * getResources().getInteger(R.integer.maxKeysPerRow);
		final int remainingWidth = (int) (maxWidth * (1 - totalKeysWidth));
		final int lateralPadding = remainingWidth  >> 1;
		this.setPadding(lateralPadding, getPaddingTop(), lateralPadding, getPaddingBottom());
	}

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        invalidateAllKeys();
    }
}

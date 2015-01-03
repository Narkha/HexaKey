/*
 * Copyright (C) 2011 The Android Open Source Project
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

/**
 * This is a part of the inputmethod-common static Java library.
 * The original source code can be found at frameworks/opt/inputmethodcommon of Android Open Source
 * Project.
 */

package es.csc.android.hexakey.inputmethodcommon;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

/* package private */ 
class InputMethodSettingsImpl implements InputMethodSettingsInterface {
    private Preference subtypeEnablerPreference;
    private int subtypeEnablerTitleRes;
    private CharSequence subtypeEnablerTitle;
    private int subtypeEnablerIconRes;
    private Drawable subtypeEnablerIcon;
    private InputMethodManager imm;
    private InputMethodInfo imi;
    private Context context;

    /**
     * Initialize internal states of this object.
     * @param context the context for this application.
     * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
     * @return true if this application is an IME and has two or more subtypes, false otherwise.
     */
    public boolean init(final Context context, final PreferenceScreen prefScreen) {
        this.context = context;
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imi = getMyImi(context, imm);
        if (imi == null || imi.getSubtypeCount() <= 1) {
            return false;
        }
        subtypeEnablerPreference = new Preference(context);
        subtypeEnablerPreference
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final CharSequence title = getSubtypeEnablerTitle(context);
                        final Intent intent =
                                new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
                        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, imi.getId());
                        if (!TextUtils.isEmpty(title)) {
                            intent.putExtra(Intent.EXTRA_TITLE, title);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(intent);
                        return true;
                    }
                });
        prefScreen.addPreference(subtypeEnablerPreference);
        updateSubtypeEnabler();
        return true;
    }

    private static InputMethodInfo getMyImi(Context context, InputMethodManager imm) {
        final List<InputMethodInfo> imis = imm.getInputMethodList();
        for (int i = 0; i < imis.size(); ++i) {
            final InputMethodInfo imi = imis.get(i);
            if (imis.get(i).getPackageName().equals(context.getPackageName())) {
                return imi;
            }
        }
        return null;
    }

    private static String getEnabledSubtypesLabel(
            Context context, InputMethodManager imm, InputMethodInfo imi) {
        if (context == null || imm == null || imi == null) return null;
        final List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
        final StringBuilder sb = new StringBuilder();
        for(final InputMethodSubtype subtype : subtypes) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(subtype.getDisplayName(context, imi.getPackageName(),
                    imi.getServiceInfo().applicationInfo));
        }
        return sb.toString();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputMethodSettingsCategoryTitle(int resId) {
        updateSubtypeEnabler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputMethodSettingsCategoryTitle(CharSequence title) {
        updateSubtypeEnabler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerTitle(int resId) {
        subtypeEnablerTitleRes = resId;
        updateSubtypeEnabler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerTitle(CharSequence title) {
        subtypeEnablerTitleRes = 0;
        subtypeEnablerTitle = title;
        updateSubtypeEnablerTitle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerIcon(int resId) {
        subtypeEnablerIconRes = resId;
        updateSubtypeEnablerIcon();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerIcon(Drawable drawable) {
        subtypeEnablerIconRes = 0;
        subtypeEnablerIcon = drawable;
        updateSubtypeEnablerIcon();
    }

    private CharSequence getSubtypeEnablerTitle(Context context) {
        if (subtypeEnablerTitleRes != 0) {
            return context.getString(subtypeEnablerTitleRes);
        } else {
            return subtypeEnablerTitle;
        }
    }

    public void updateSubtypeEnabler() {
        if (subtypeEnablerPreference != null) {
            updateSubtypeEnablerTitle();
            updateSubtypeEnablerSummary();
            updateSubtypeEnablerIcon();
        }
    }

	private void updateSubtypeEnablerTitle() {
		if (subtypeEnablerTitleRes != 0) {
		    subtypeEnablerPreference.setTitle(subtypeEnablerTitleRes);
		} 
		else if (!TextUtils.isEmpty(subtypeEnablerTitle)) {
		    subtypeEnablerPreference.setTitle(subtypeEnablerTitle);
		}
	}

	private void updateSubtypeEnablerSummary() {
		final String summary = getEnabledSubtypesLabel(context, imm, imi);
		if (!TextUtils.isEmpty(summary)) {
		    subtypeEnablerPreference.setSummary(summary);
		}
	}

	private void updateSubtypeEnablerIcon() {
		if (subtypeEnablerIconRes != 0) {
		    subtypeEnablerPreference.setIcon(subtypeEnablerIconRes);
		} 
		else if (subtypeEnablerIcon != null) {
		    subtypeEnablerPreference.setIcon(subtypeEnablerIcon);
		}
	}
}

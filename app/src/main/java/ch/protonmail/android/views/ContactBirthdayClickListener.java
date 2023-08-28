/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.views;

import android.view.View;

import androidx.fragment.app.FragmentManager;

import ch.protonmail.android.activities.fragments.DatePickerFragment;
import ch.protonmail.android.utils.DateUtil;
import me.proton.core.presentation.ui.view.ProtonInput;

/**
 * Created by dino on 12/16/17.
 */

public class ContactBirthdayClickListener implements View.OnClickListener {

    private FragmentManager mSupportFragmentManager;

    public ContactBirthdayClickListener(FragmentManager fragmentManager) {
        mSupportFragmentManager = fragmentManager;
    }

    @Override
    public void onClick(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        final ProtonInput bDayView = (ProtonInput) v;
        newFragment.setListener(date -> {
            bDayView.setText(DateUtil.formatDate(date));
            bDayView.setTag(date);
        });
        newFragment.show(mSupportFragmentManager, "ContactBdayDatePicker");
    }
}

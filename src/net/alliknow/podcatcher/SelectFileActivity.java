/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;

import net.alliknow.podcatcher.view.fragments.SelectFileFragment;

/**
 * Non-UI activity to select files and folders. Use the intent and constants
 * defined here to configure its behavior. Start the activity with
 * {@link Activity#startActivityForResult(Intent, int)} to be alerted on
 * selection.
 */
public class SelectFileActivity extends BaseActivity implements OnCancelListener {

    /** The tag we identify our file selection fragment with */
    private static final String SELECT_FILE_FRAGMENT_TAG = "select_file";

    /** The fragment containing the select file UI */
    private SelectFileFragment selectFileFragment;

    @Override
    protected void onStart() {
        super.onStart();

        // use getIntent() to configure

        // Show the dialog fragment
        // Try to find existing fragment
        selectFileFragment = (SelectFileFragment) getFragmentManager().findFragmentByTag(
                SELECT_FILE_FRAGMENT_TAG);

        // No fragment found, create it
        if (selectFileFragment == null) {
            selectFileFragment = new SelectFileFragment();
            selectFileFragment.setStyle(DialogFragment.STYLE_NORMAL,
                    android.R.style.Theme_Holo_Light_Dialog);

            // Show the fragment
            selectFileFragment.show(getFragmentManager(), SELECT_FILE_FRAGMENT_TAG);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // TODO Auto-generated method stub
    }
}

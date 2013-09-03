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

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import net.alliknow.podcatcher.view.fragments.FirstRunFragment;
import net.alliknow.podcatcher.view.fragments.FirstRunFragment.FirstRunListener;

/**
 * Activity to run on the very first app start. Welcomes the user and gives some
 * hints.
 */
public class FirstRunActivity extends BaseActivity implements FirstRunListener {

    /** The tag we identify our fragment with */
    private static final String FIRST_RUN_FRAGMENT_TAG = "first_run";

    /** The fragment containing the UI */
    private FirstRunFragment firstRunFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure we only run once
        preferences.edit().putBoolean(SettingsActivity.KEY_FIRST_RUN, false).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Try to find existing fragment
        firstRunFragment = (FirstRunFragment) getFragmentManager().findFragmentByTag(
                FIRST_RUN_FRAGMENT_TAG);

        // No fragment found, create it
        if (firstRunFragment == null) {
            firstRunFragment = new FirstRunFragment();
            firstRunFragment.setStyle(DialogFragment.STYLE_NORMAL,
                    android.R.style.Theme_Holo_Light_Dialog);

            // Show the fragment
            firstRunFragment.show(getFragmentManager(), FIRST_RUN_FRAGMENT_TAG);
        }
    }

    @Override
    public void onAddPodcasts() {
        firstRunFragment.dismiss();
        finish();

        startActivity(new Intent(this, AddPodcastActivity.class));
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}

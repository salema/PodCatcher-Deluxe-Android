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

package net.alliknow.podcatcher.view.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.alliknow.podcatcher.R;

/**
 * A dialog to greet the user and introduce the app.
 */
public class FirstRunFragment extends DialogFragment {

    /** The podcatcher help website URL (add anchor) */
    private static final String PODCATCHER_HELPSITE = "http://www.podcatcher-deluxe.com/help#add";

    /** The listener we report back to */
    private FirstRunListener listener;

    /** The listener interface to implement by our activity */
    public interface FirstRunListener extends OnCancelListener {

        /** Called when the add podcast button is pressed */
        public void onAddPodcasts();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (FirstRunListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FirstRunListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.first_run, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.first_run_title);

        final Button helpButton = (Button) view.findViewById(R.id.first_run_help_button);
        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE)));
                } catch (ActivityNotFoundException e) {
                    // We are in a restricted profile without a browser, pass
                    // TODO Find a better solution here
                }
            }
        });

        final Button addButton = (Button) view.findViewById(R.id.first_run_add_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onAddPodcasts();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        if (listener instanceof OnCancelListener)
            ((OnCancelListener) listener).onCancel(getDialog());

        super.onCancel(dialog);
    }
}

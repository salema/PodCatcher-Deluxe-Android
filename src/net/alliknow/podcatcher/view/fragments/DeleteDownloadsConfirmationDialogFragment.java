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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import net.alliknow.podcatcher.R;

/**
 * A confirmation dialog for the user to make sure he/she really wants
 * downloaded episode files to be removed from the local storage. Use
 * {@link #setListener(DeleteDownloadsConfirmationListener)} to register as a
 * call-back. If you are removeing multiple downloads, you might want to use
 * {@link #setArguments(Bundle)} with an integer for the number of episodes set
 * using the key {@link #EPISODE_COUNT_KEY}. (This needs to be done before
 * showing the dialog.)
 */
public class DeleteDownloadsConfirmationDialogFragment extends DialogFragment {

    /** Argument key for the downloads to delete count */
    public static final String EPISODE_COUNT_KEY = "episode_count";
    /** The tag we identify our confirmation dialog fragment with */
    public static final String TAG = "confirm_download_delete";

    /** The number episodes about to be deleted */
    private int episodeCount = 1;

    /** The callback we are working with */
    private DeleteDownloadsConfirmationListener listener;

    /**
     * The callback definition, use
     * {@link DeleteDownloadsConfirmationDialogFragment#setListener(DeleteDownloadsConfirmationListener)}
     * to register.
     */
    public interface DeleteDownloadsConfirmationListener {
        /**
         * Called on the listener if the user confirmed the deletion.
         */
        public void onConfirm();

        /**
         * Called on the listener if the user cancelled the deletion.
         */
        public void onCancel();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        episodeCount = args.getInt(EPISODE_COUNT_KEY);

        // Cannot be negative or zero
        if (episodeCount < 1)
            episodeCount = 1;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String title = getResources()
                .getQuantityString(R.plurals.downloads_remove_title, episodeCount, episodeCount);
        final String message = getResources()
                .getQuantityString(R.plurals.downloads_remove_text, episodeCount, episodeCount);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title).setMessage(message)
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null)
                            listener.onConfirm();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (listener != null)
                            listener.onCancel();
                    }
                });

        return builder.create();
    }

    /**
     * Register the callback.
     * 
     * @param listener Listener to call on user action.
     */
    public void setListener(DeleteDownloadsConfirmationListener listener) {
        this.listener = listener;
    }
}

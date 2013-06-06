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

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import net.alliknow.podcatcher.SelectFileActivity.SelectionMode;
import net.alliknow.podcatcher.listeners.OnStorePodcastListListener;
import net.alliknow.podcatcher.model.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that exports a list of selected podcasts to an OPML file.
 */
public class ExportOpmlActivity extends BaseActivity implements OnStorePodcastListListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the list of positions to export
        List<Integer> positions = getIntent().getIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY);

        // If list is there, export podcasts at given positions
        if (positions != null) {
            // Only do this initial creation to avoid multiple folder selection
            // dialogs
            if (savedInstanceState == null) {
                Intent selectFolderIntent = new Intent(this, SelectFileActivity.class);
                selectFolderIntent
                        .putExtra(SelectFileActivity.SELECTION_MODE_KEY, SelectionMode.FOLDER);

                startActivityForResult(selectFolderIntent, 1);
            }
        } else
            // Nothing to do
            finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && data != null) {
            // Get the list of positions to export
            List<Integer> positions = getIntent()
                    .getIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY);

            // If list is there, export podcasts at given positions
            if (positions != null) {
                List<Podcast> podcasts = new ArrayList<Podcast>();

                for (Integer position : positions)
                    podcasts.add(podcastManager.getPodcastList().get(position));

                StorePodcastListTask exportTask = new StorePodcastListTask(this, this);
                // Determine and set output folder
                File exportFolder = new File(
                        data.getStringExtra(SelectFileActivity.RESULT_PATH_KEY));
                exportTask.setCustomLocation(exportFolder);

                exportTask.execute(podcasts);
            }
        }

        // Make sure we finish here
        finish();
    }

    @Override
    public void onPodcastListStored(List<Podcast> podcastList, File outputFile) {
        showToast(getString(R.string.opml_export_success) + "\n" +
                outputFile.getAbsolutePath(), Toast.LENGTH_LONG);
    }

    @Override
    public void onPodcastListStoreFailed(List<Podcast> podcastList, File outputFile,
            Exception exception) {
        showToast(getString(R.string.opml_export_failed));
    }
}

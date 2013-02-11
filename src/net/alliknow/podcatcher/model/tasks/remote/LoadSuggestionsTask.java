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

package net.alliknow.podcatcher.model.tasks.remote;

import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.model.tags.JSON;
import net.alliknow.podcatcher.model.tasks.Progress;
import net.alliknow.podcatcher.model.types.Genre;
import net.alliknow.podcatcher.model.types.Language;
import net.alliknow.podcatcher.model.types.MediaType;
import net.alliknow.podcatcher.model.types.Podcast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A task that loads and reads suggested podcasts.
 */
public class LoadSuggestionsTask extends LoadRemoteFileTask<Void, List<Podcast>> {

    /** Owner */
    private final OnLoadSuggestionListener listener;

    /** The file encoding */
    private static final String SUGGESTIONS_FILE_ENCODING = "utf8";
    /** The online resource to find suggestions */
    private static final String SOURCE = "https://raw.github.com/salema/PodCatcher-Deluxe/master/suggestions.json";

    /**
     * Create new task
     * 
     * @param listener Owner fragment
     */
    public LoadSuggestionsTask(OnLoadSuggestionListener listener) {
        this.listener = listener;
    }

    @Override
    protected List<Podcast> doInBackground(Void... params) {
        List<Podcast> result = new ArrayList<Podcast>();

        try {
            // Load the file from the internets
            publishProgress(Progress.CONNECT);
            byte[] suggestionsFile = loadFile(new URL(SOURCE));

            // Get result as a document
            publishProgress(Progress.PARSE);
            JSONObject completeJson = new JSONObject(new String(suggestionsFile,
                    SUGGESTIONS_FILE_ENCODING));
            if (isCancelled())
                return null;

            // Add all featured podcasts
            addSuggestionsFromJsonArray(completeJson.getJSONArray(JSON.FEATURED), result);
            if (isCancelled())
                return null;
            // Add all suggestions
            addSuggestionsFromJsonArray(completeJson.getJSONArray(JSON.SUGGESTION), result);
            if (isCancelled())
                return null;

            Collections.sort(result);
        } catch (Exception e) {
            failed = true;
            Log.w(getClass().getSimpleName(), "Load failed for podcast suggestions file", e);
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Progress... progress) {
        if (listener != null)
            listener.onSuggestionsLoadProgress(progress[0]);
        else if (listener == null)
            Log.w(getClass().getSimpleName(),
                    "Suggestions progress update, but no listener attached");
    }

    @Override
    protected void onPostExecute(List<Podcast> suggestions) {
        // Background task failed to complete
        if (failed) {
            if (listener != null)
                listener.onSuggestionsLoadFailed();
            else
                Log.w(getClass().getSimpleName(),
                        "Suggestions failed to load, but no listener attached");
        } // Podcast was loaded
        else if (listener != null)
            listener.onSuggestionsLoaded(suggestions);
        else
            Log.w(getClass().getSimpleName(), "Suggestions loaded, but no listener attached");
    }

    /**
     * Add all podcast suggestions in given array to the list.
     * 
     * @param array JSON array to scan.
     * @param list List to add suggestions to.
     */
    private void addSuggestionsFromJsonArray(JSONArray array, List<Podcast> list) {
        for (int index = 0; index < array.length(); index++) {
            JSONObject object;

            try {
                object = array.getJSONObject(index);
            } catch (JSONException e) {
                continue; // If an index fails, try the next one...
            }

            Podcast suggestion = createSuggestion(object);
            if (suggestion != null)
                list.add(suggestion);
        }
    }

    /**
     * Create a podcast suggestion for the given JSON object and set its
     * properties.
     * 
     * @param json The JSON object to work on.
     * @return The podcast suggestion or <code>null</code> if any problem
     *         occurs.
     */
    private Podcast createSuggestion(JSONObject json) {
        Podcast suggestion = null;

        try {
            suggestion = new Podcast(json.getString(JSON.TITLE), new URL(json.getString(JSON.URL)));
            suggestion.setDescription(json.getString(JSON.DESCRIPTION).trim());
            suggestion.setLanguage(Language.valueOf(json.getString(JSON.LANGUAGE)
                    .toUpperCase(Locale.US).trim()));
            suggestion.setMediaType(MediaType.valueOf(json.getString(JSON.TYPE)
                    .toUpperCase(Locale.US).trim()));
            suggestion.setGenre(Genre.valueOf(json.getString(JSON.CATEGORY).toUpperCase(Locale.US)
                    .trim()));
        } catch (JSONException e) {
            Log.w(getClass().getSimpleName(), "Problem parsing JSON for suggestion: " + suggestion,
                    e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.w(getClass().getSimpleName(), "Enum value missing for suggestion: " + suggestion, e);
            return null;
        } catch (MalformedURLException e) {
            Log.w(getClass().getSimpleName(), "Bad URL for suggestion: " + suggestion, e);
            return null;
        }

        return suggestion;
    }
}

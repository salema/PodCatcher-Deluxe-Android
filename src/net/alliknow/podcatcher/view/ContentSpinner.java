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

package net.alliknow.podcatcher.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.PodcastManager;

/**
 * The spinner for the main action bar menu, that allows for the selection of
 * the content mode. This is a bit of a hack since it does not work exactly like
 * usual spinner, but allows the user to pick an action twice. The initial
 * default selection is ignored. In addition, we override the default behavior
 * to show the selected view when closed. Instead, we show another view with
 * name and status information.
 */
public class ContentSpinner extends Spinner implements
        AdapterView.OnItemSelectedListener {

    /** The listener call-back to alert on content mode selection */
    private OnSelectPodcastListener listener;

    /** Flag allowing use to skip the first selection on creation */
    private boolean isInitialSelection = true;

    /** Handle to the closed spinner title text view */
    private TextView closedTitleView;
    /** Handle to the closed spinner subtitle text view */
    private TextView closedSubtitleView;

    /** Our spinner handle */
    private NavigationSpinnerAdapter spinnerAdapter;

    /** The data adapter used to populate the spinner */
    private static class NavigationSpinnerAdapter extends BaseAdapter {

        /** The view we return for a closed spinner */
        private View closedView;

        public NavigationSpinnerAdapter(ViewGroup parent) {
            this.closedView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.content_spinner_item, parent, false);

            // FOr the closed view, no padding is needed
            closedView.setPadding(0, 0, 0, 0);

            // Set the initial name and status
            ((TextView) closedView.findViewById(R.id.title)).setText(R.string.app_name);
            ((TextView) closedView.findViewById(R.id.subtitle)).setVisibility(View.GONE);

            // Hide icon view for the closed view
            closedView.findViewById(R.id.icon).setVisibility(View.GONE);
        }

        /**
         * @return The view shown when the spinner is closed. This is never
         *         <code>null</code>.
         */
        public View getClosedView() {
            return closedView;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public Object getItem(int position) {
            return position == 0 ? R.string.podcast_select_all : 0;
        }

        @Override
        public long getItemId(int position) {
            return position == 0 ? R.string.podcast_select_all : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // This is shown if the spinner is closed
            return closedView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // Create the return view (this should not be recycled)
            final View spinnerItemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.content_spinner_item, parent, false);

            // Set the icon
            final ImageView imageView = (ImageView) spinnerItemView.findViewById(R.id.icon);
            imageView.setImageResource(R.drawable.ic_menu_select_all);

            // Set the title view
            final TextView titleView = (TextView) spinnerItemView.findViewById(R.id.title);
            titleView.setText(R.string.podcast_select_all);

            // Set the subtitle
            final TextView subtitleView = (TextView) spinnerItemView.findViewById(R.id.subtitle);
            final int podcastCount = PodcastManager.getInstance().size();
            if (podcastCount == 0)
                subtitleView.setText(R.string.podcast_none);
            else
                subtitleView.setText(parent.getContext().getResources()
                        .getQuantityString(R.plurals.podcasts, podcastCount, podcastCount));

            return spinnerItemView;
        }
    }

    /**
     * Create a new content mode spinner to be added as a custom view to the
     * app's action bar.
     * 
     * @param context Context the view lives in.
     * @param listener The action call-back to alert when a content selection is
     *            made.
     */
    public ContentSpinner(Context context, OnSelectPodcastListener listener) {
        super(context, null, android.R.attr.actionDropDownStyle);

        this.listener = listener;
        this.spinnerAdapter = new NavigationSpinnerAdapter(this);

        closedTitleView = (TextView) spinnerAdapter.getClosedView().findViewById(R.id.title);
        closedSubtitleView = (TextView) spinnerAdapter.getClosedView().findViewById(R.id.subtitle);

        setAdapter(spinnerAdapter);
        setOnItemSelectedListener(this);
    }

    /**
     * Set the main text for the closed spinner view.
     * 
     * @param title Text to show.
     */
    public void setTitle(String title) {
        closedTitleView.setText(title);
    }

    /**
     * Set the sub title for the closed spinner view.
     * 
     * @param subtitle The subtitle, set to <code>null</code> to hide.
     */
    public void setSubtitle(String subtitle) {
        closedSubtitleView.setText(subtitle);
        closedSubtitleView.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!isInitialSelection)
            switch (Long.valueOf(id).intValue()) {
                case R.string.podcast_select_all:
                    listener.onAllPodcastsSelected();
            }

        // This invalidates the selection, so the same item can be picked again
        setSelection(getAdapter().getCount());
        // Reset flag to check whether this is the activity start-up selection
        isInitialSelection = false;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // pass
    }
}

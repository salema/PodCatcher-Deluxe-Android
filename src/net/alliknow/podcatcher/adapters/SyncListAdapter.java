/** Copyright 2012-2014 Kevin Hausmann
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

package net.alliknow.podcatcher.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnConfigureSyncListener;
import net.alliknow.podcatcher.model.SyncManager;
import net.alliknow.podcatcher.model.sync.ControllerImpl;
import net.alliknow.podcatcher.view.SyncListItemView;

import java.util.ArrayList;
import java.util.List;

/**
 * The sync list adapter to provide the data for the list of sync controllers
 * available.
 */
public class SyncListAdapter extends PodcatcherBaseAdapter {

    /** The callback to invoke if the user interacts with list items */
    private final OnConfigureSyncListener listener;
    /** The {@link SyncManager} handle */
    private final SyncManager syncManager;

    /** Our list of available sync controller implementations */
    private final List<ControllerImpl> impls;

    /**
     * Create new adapter.
     * 
     * @param context Context we live in.
     * @param listener The listener for user interaction.
     */
    public SyncListAdapter(Context context, OnConfigureSyncListener listener) {
        super(context);

        this.listener = listener;
        this.syncManager = SyncManager.getInstance();

        // Create the list of available sync controller implementation for the
        // environment we live in
        this.impls = new ArrayList<ControllerImpl>(ControllerImpl.values().length);
        for (ControllerImpl impl : ControllerImpl.values())
            if (impl.isAvailable(context))
                impls.add(impl);
    }

    @Override
    public int getCount() {
        return impls.size();
    }

    @Override
    public Object getItem(int position) {
        return impls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final SyncListItemView returnView = (SyncListItemView)
                findReturnView(convertView, parent, R.layout.sync_list_item);

        final ControllerImpl impl = impls.get(position);

        // Make the view represent controller at given position
        returnView.show(impl, impl.isLinked(context), syncManager.getSyncMode(impl), listener);

        return returnView;
    }
}

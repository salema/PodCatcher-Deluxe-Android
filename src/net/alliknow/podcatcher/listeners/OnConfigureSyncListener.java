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

package net.alliknow.podcatcher.listeners;

import android.content.DialogInterface.OnCancelListener;

import net.alliknow.podcatcher.model.sync.ControllerImpl;
import net.alliknow.podcatcher.model.sync.SyncController;
import net.alliknow.podcatcher.model.sync.SyncController.SyncMode;

/**
 * Interface definition for a callback to be invoked when sync settings are
 * changed.
 */
public interface OnConfigureSyncListener extends OnCancelListener {

    /**
     * Called on the listener when the user request the {@link SyncController}'s
     * settings to be displayed (and possibly changed).
     * 
     * @param impl The controller to present settings for.
     */
    public void onUpdateSettings(ControllerImpl impl);

    /**
     * Called on the listener when the user set the {@link SyncMode} for a
     * {@link SyncController}.
     * 
     * @param impl The controller mode is set for.
     * @param mode The new sync mode. By giving <code>null</code> here, the
     *            controller is disabled.
     */
    public void onUpdateMode(ControllerImpl impl, SyncMode mode);

    /**
     * Called on the listener when the user wants to see the help screen.
     */
    public void onShowHelp();

    /**
     * Called on the listener when the user triggered a sync all event.
     */
    public void onSyncNow();
}

/** Copyright 2012 Kevin Hausmann
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;

/**
 * Abstract super class for this app's adapters.
 * 
 * @author Kevin Hausmann
 */
public abstract class PodcatcherBaseAdapter extends BaseAdapter {

	protected int SELECTED_COLOR = android.R.color.holo_blue_light;
	protected int UNSELECTED_COLOR = android.R.color.transparent;
	
	/** We need to know the selected podcast's position in the list */
	protected int selectedPosition = -1;
	/** Inflater for new views */
	protected LayoutInflater inflater;

	/**
	 * Create new adapter
	 * @param context The current context
	 */
	public PodcatcherBaseAdapter(Context context) {
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public void setSelectedPosition(int position) {
		this.selectedPosition = position;
		this.notifyDataSetChanged();
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	protected void setBackground(int position, View view) {
		if (position == selectedPosition) view.setBackgroundResource(SELECTED_COLOR);
		else view.setBackgroundResource(UNSELECTED_COLOR);
	}
}

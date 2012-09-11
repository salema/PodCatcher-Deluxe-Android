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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Abstract super class for this app's adapters.
 * Handles the selection parts. All lists are single choice and
 * have a background changed for the selected item.
 * 
 * @author Kevin Hausmann
 */
public abstract class PodcatcherBaseAdapter extends BaseAdapter {

	/** Color id for selected items */
	public static int SELECTED_COLOR = android.R.color.holo_blue_light;
	/** Color id for items not selected */
	public static int UNSELECTED_COLOR = android.R.color.transparent;
	
	/** We need to know the selected item's position in the list */
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
	
	/**
	 * Set the selected item in the list and updates the UI to reflect
	 * the selection.
	 * @param position Position selected.
	 */
	public void setSelectedPosition(int position) {
		if (this.selectedPosition != position) {
			this.selectedPosition = position;
			this.notifyDataSetChanged();
		}
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	/**
	 * Check whether a view can be recycled and inflate new one if not
	 * 
	 * @param convertView View to check
	 * @param parent View group to attach to
	 * @param inflateId Id of view to inflate if recycling is not possible
	 * @return A view to use (not <code>null</code>)
	 */
	protected View findReturnView(View convertView, ViewGroup parent, int inflateId) {
		// Can we recycle the convert view?
		// No:
		if (convertView == null) return inflater.inflate(inflateId, parent, false);
		// Yes:
		else return convertView;
	}
	
	/**
	 * Set text and background for a list item view element.
	 * 
	 * @param listItem The view respresenting the whole list item
	 * @param viewId View id of the child view, has to be (a subclass of) <code>TextView</code>
	 * @param text Text to display
	 * @param position Position in list
	 */
	protected void setTextAndBackground(View listItem, int viewId, String text, int position) {
		TextView textView = (TextView) listItem.findViewById(viewId);
		textView.setText(text);
		textView.setSingleLine(position != selectedPosition);
		
		setBackground(position, textView);
	}
	
	/**
	 * Set a view background according to whether the item is at the
	 * selected position.
	 * @param position The item's position
	 * @param view View associated with the item and to set background for
	 */
	protected void setBackground(int position, View view) {
		if (position == selectedPosition) view.setBackgroundResource(SELECTED_COLOR);
		else view.setBackgroundResource(UNSELECTED_COLOR);
	}
}

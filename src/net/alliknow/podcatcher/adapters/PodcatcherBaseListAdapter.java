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
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckedTextView;

/**
 * Abstract super class for this app's list adapters.
 * Handles the selection/choice parts. All lists are single choice and
 * have a background changed for the selected item.
 * 
 * @author Kevin Hausmann
 */
public abstract class PodcatcherBaseListAdapter extends PodcatcherBaseAdapter {

	/** We need to know the selected item's position in the list */
	protected int selectedPosition = -1;
	/** Also, there might be checked items */
	protected SparseBooleanArray checkedPositions;
		
	/**
	 * Create new adapter
	 * @param context The current context
	 */
	public PodcatcherBaseListAdapter(Context context) {
		super(context);
	}
	
	/**
	 * Set the selected item in the list and updates the UI to reflect
	 * the selection.
	 * @param position Position selected.
	 */
	public void setSelectedPosition(int position) {
		if (this.selectedPosition != position) {
			selectedPosition = position;
			notifyDataSetChanged();
		}
	}
	
	/**
	 * Set the choosen items in the list.
	 * @param positions The array denoting choosen positions.
	 */
	public void setCheckedPositions(SparseBooleanArray positions) {
		this.checkedPositions = positions;
		notifyDataSetChanged();
	}

	/**
	 * Set text and selection/choice state for a list item view element.
	 * 
	 * @param listItem The view representing the whole list item
	 * @param viewId View id of the child view, has to be (a subclass of) <code>TextView</code>
	 * @param text Text to display
	 * @param position Position in list
	 */
	protected void setText(View listItem, int viewId, String text, int position) {
		CheckedTextView textView = (CheckedTextView) listItem.findViewById(viewId);
		textView.setText(text);
		textView.setSingleLine(position != selectedPosition);
		
		if (checkedPositions != null) textView.setChecked(checkedPositions.get(position));
		textView.setSelected(position == selectedPosition);
	}
}

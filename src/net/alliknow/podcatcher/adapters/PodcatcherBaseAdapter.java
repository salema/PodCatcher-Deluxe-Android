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

import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Abstract super class for this app's adapters.
 * Allows access to inflater and resources.
 * 
 * @author Kevin Hausmann
 */
public abstract class PodcatcherBaseAdapter extends BaseAdapter {
	
	/** Our context's resources */
	//protected Resources resources;
	/** Inflater for new views */
	//protected LayoutInflater inflater;
	/** We need to know our package name to retrieve identifiers */
	protected String packageName;

	/** The def type for string resources */
	private static final String STRING_DEFTYPE = "string";
	
	/**
	 * Create new adapter
	 * @param context The current context
	 */
	public PodcatcherBaseAdapter(Context context) {
		//this.resources = context.getResources();
		//this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.packageName = context.getPackageName();
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
		if (convertView == null) return LayoutInflater.from(parent.getContext()).inflate(inflateId, parent, false);
		// Yes:
		else return convertView;
	}
	
	/**
	 * Get the resource (language-specific) string for an item.
	 * @param resources Resources to use.
	 * @param item Item which <code>toString</code> method result
	 * in lower case is a string resource key.
	 * @return The string in the correct language. Throws
	 * a runtime exception if no such resource exists.
	 */
	protected String getResourceString(Resources resources, Object item) {
		return getResourceString(resources, item.toString());
	}
	
	/**
	 * Get the resource (language-specific) string for the given key.
	 * @param resources Resources to use.
	 * @param key Key to look up. Will be changed to lower case.
	 * @return The string in the correct language. Throws
	 * a runtime exception if no such resource exists.
	 */
	protected String getResourceString(Resources resources, String key) {
		return resources.getString(getStringIdentifier(resources, key));
	}
	
	/**
	 * Get the resource (language-specific) string for
	 * item at the given position.
	 * @param resources Resources to use.
	 * @param position Index to the item to get string for.
	 * @return The string in the correct language. Throws
	 * a runtime exception if no such resource exists.
	 */
	protected String getResourceString(Resources resources, int position) {
		return getResourceString(resources, getItem(position));
	}

	/**
	 * Get the resource key (identifier) from the given string key.
	 * @param resources Resources to use.
	 * @param key Key as a string (will be changed to lower case).
	 * @return The resource identifier for this key. Throws
	 * a runtime exception if no such resource exists.
	 */
	protected int getStringIdentifier(Resources resources, String key) {
		return resources.getIdentifier(key.toLowerCase(Locale.US), STRING_DEFTYPE, packageName);
	}
}

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

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.OnSelectFileListener;
import net.alliknow.podcatcher.view.adapters.FileListAdapter;

import java.io.File;

/**
 * Fragment for file selection.
 */
public class SelectFileFragment extends DialogFragment {

    /** The call back we work on */
    private OnSelectFileListener listener;
    /** The path we are currently showing */
    private File currentPath;

    /** The current path view */
    private TextView currentPathView;
    /** The up button */
    private ImageButton upButton;
    /** The file list view */
    private ListView fileListView;
    /** The select button */
    private Button selectButton;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (OnSelectFileListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectFileListener");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.file_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getDialog().setTitle("Select Folder");

        currentPathView = (TextView) view.findViewById(R.id.current_path);
        upButton = (ImageButton) view.findViewById(R.id.path_up);
        upButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setPath(currentPath.getParentFile());
            }
        });

        fileListView = (ListView) view.findViewById(R.id.files);
        fileListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setPath((File) fileListView.getAdapter().getItem(position));
            }
        });
        selectButton = (Button) view.findViewById(R.id.select_file);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentPath != null)
            setPath(currentPath);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        if (listener instanceof OnCancelListener)
            ((OnCancelListener) listener).onCancel(dialog);
    }

    public void setPath(File path) {
        this.currentPath = path;

        if (isResumed() && path != null) {
            currentPathView.setText(path.getAbsolutePath());
            upButton.setEnabled(path.getParent() != null);
            fileListView.setAdapter(new FileListAdapter(getActivity(), path));
        }
    }
}

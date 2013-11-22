package net.alliknow.podcatcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.ContextMenuListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuView extends LinearLayout implements AdapterView.OnItemClickListener,
        View.OnFocusChangeListener {

    public static final int ITEM_CANCEL = -1;
    public static final int ITEM_PLAY = 0;
    public static final int ITEM_REVERSE_MARKER = 1;
    public static final int ITEM_ADD_TO_PLAYLIST = 2;
    public static final int ITEM_DOWNLOAD = 3;

    ListView lvContextMenu;

    OnSelectEpisodeListener selectEpisodeListener;
    ContextMenuListener episodeContextMenuListener;
    private boolean isNew;
    private boolean isInPlaylist;
    private Episode episode;
    private boolean mIsOpened;

    /** The episode manager handle */
    private EpisodeManager episodeManager;

    public ContextMenuView(Context context) {
        super(context);
        prepare(context);
    }

    public ContextMenuView(Context context, AttributeSet set) {
        super(context, set);
        prepare(context);
    }

    public ContextMenuView(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        prepare(context);
    }

    private void prepare(Context context) {
        LayoutInflater.from(context).inflate(R.layout.context_menu, this, true);
        selectEpisodeListener = (OnSelectEpisodeListener) context;
        episodeContextMenuListener = (ContextMenuListener) context;
    }

    public void initialize(Episode episode) {
        this.episode = episode;

        this.episodeManager = EpisodeManager.getInstance();
        this.isNew = !episodeManager.isOld(episode);
        this.isInPlaylist = episodeManager.isInPlaylist(episode);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_list_item_1,
                formList()
        );
        lvContextMenu = (ListView) findViewById(R.id.context_menu_list);
        lvContextMenu.setAdapter(adapter);
        lvContextMenu.setOnItemClickListener(this);
        lvContextMenu.setOnFocusChangeListener(this);
    }

    private List<String> formList() {
        List<String> list = new ArrayList<String>(4);
        list.add(getContext().getString(R.string.context_play));
        list.add(getContext().getString(
                isNew ? R.string.context_mark_old : R.string.context_mark_new)
        );
        list.add(getContext().getString(
                isInPlaylist? R.string.context_playlist_remove : R.string.context_playlist_add)
        );
        list.add(getContext().getString(R.string.context_download));
        return list;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PodcastActivity activity = (PodcastActivity) getContext();

        switch (position) {

            // todo: does not work
            case ITEM_PLAY:
                    activity.onToggleLoad();
                break;

            // todo: does not work
            case ITEM_REVERSE_MARKER:
                episodeManager.setState(episode, isNew);
                activity.onStateChanged(episode);
                break;

            // todo: does not refresh the layout
            case ITEM_DOWNLOAD:
                activity.onToggleDownload();
                break;

//            case R.id.episode_remove_contextmenuitem:
//                final DeleteDownloadsConfirmationFragment confirmationDialog =
//                        new DeleteDownloadsConfirmationFragment();
//                // Create bundle to make dialog aware of selection count
//                final Bundle args = new Bundle();
//                args.putInt(EPISODE_COUNT_KEY, deletesTriggered);
//                confirmationDialog.setArguments(args);
//                // Set the callback
//                confirmationDialog.setListener(new OnDeleteDownloadsConfirmationListener() {
//
//                    @Override
//                    public void onConfirmDeletion() {
//                        // Go delete the downloads
//                        for (int position = 0; position < fragment.getListAdapter().getCount(); position++)
//                            if (checkedItems.get(position))
//                                episodeManager.deleteDownload(
//                                        (Episode) fragment.getListAdapter().getItem(position));
//
//                        // Action picked, so close the CAB
//                        mode.finish();
//                    }
//
//                    @Override
//                    public void onCancelDeletion() {
//                        // Nothing to do here
//                    }
//                });
//                // Finally show the dialog
//                confirmationDialog.show(fragment.getFragmentManager(), TAG);
//
//                return true;

            case ITEM_ADD_TO_PLAYLIST:
                if (episodeManager.isInPlaylist(episode))
                    episodeManager.removeFromPlaylist(episode);
                else
                    episodeManager.appendToPlaylist(episode);
                activity.onPlaylistChanged();
                break;

//            case R.id.episode_select_all_contextmenuitem:
//                // Disable expensive UI updates
//                updateUi = false;
//                for (int index = 0; index < fragment.getListAdapter().getCount(); index++)
//                    fragment.getListView().setItemChecked(index, true);
//
//                // Re-enable UI updates
//                updateUi = true;
//                update(mode);
//                return true;
//            default:
//                return false;
        }

        hide();
    }

    public void show() {
        setVisibility(View.VISIBLE);
        setEnabled(true);
        mIsOpened = true;
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.menu_show);
        setAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                selectFirst();
                lvContextMenu.requestFocus();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public void hide() {
        if (!mIsOpened) {
            return;
        }
        clearFocus();
        mIsOpened = false;
        setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.menu_hide));
        setVisibility(View.INVISIBLE);
        setEnabled(false);

        episodeContextMenuListener.onEpisodeContextMenuClose();
    }

    public void toggle() {
        if (mIsOpened) {
            hide();
        } else {
            show();
        }
    }

    private void selectFirst() {
        if (lvContextMenu != null) {
            lvContextMenu.setSelection(0);
        }
    }

    public boolean isOpened() {
        return mIsOpened;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            hide();
        }
    }
}

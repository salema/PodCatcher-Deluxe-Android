package net.alliknow.podcatcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import net.alliknow.podcatcher.PodcastActivity;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.ContextMenuListener;
import net.alliknow.podcatcher.listeners.OnDeleteDownloadsConfirmationListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuView extends ListView implements AdapterView.OnItemClickListener {

    public static final int ITEM_PLAY = 0;
    public static final int ITEM_REVERSE_MARKER = 1;
    public static final int ITEM_ADD_TO_PLAYLIST = 2;
    public static final int ITEM_DOWNLOAD = 3;

    OnSelectEpisodeListener selectEpisodeListener;
    ContextMenuListener episodeContextMenuListener;
    private boolean isNew;
    private boolean isInPlaylist;
    private boolean isDownloaded;
    private Episode episode;

    /**
     * The episode manager handle
     */
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

//    @Override
//    public Drawable getDivider() {
//        return getResources().getDrawable(R.color.divider_color);
//    }
//
//    @Override
//    public int getDividerHeight() {
//        return 2;
//    }
//
//    @Override
//    public Drawable getSelector() {
//        return getResources().getDrawable(R.drawable.list_item_bg_focused_fragment);
//    }
//
//    @Override
//    public int getChoiceMode() {
//        return CHOICE_MODE_SINGLE;
//    }
//


    private void prepare(Context context) {
//        LayoutInflater.from(context).inflate(R.layout.context_menu, this, false);
        selectEpisodeListener = (OnSelectEpisodeListener) context;
        episodeContextMenuListener = (ContextMenuListener) context;
    }

    public void initialize(Episode episode) {
        this.episode = episode;
        refreshState();

        ContextMenuAdapter adapter = new ContextMenuAdapter(formList(), formIconsList());
        setAdapter(adapter);
        setOnItemClickListener(this);
    }

    private List<String> formList() {
        List<String> list = new ArrayList<String>(4);
        list.add(getContext().getString(R.string.context_play));
        list.add(getContext().getString(
                isNew ? R.string.context_mark_old : R.string.context_mark_new)
        );
        list.add(getContext().getString(
                isInPlaylist ? R.string.context_playlist_remove : R.string.context_playlist_add)
        );
        list.add(
                isDownloaded ?
                        getResources().getQuantityString(R.plurals.downloads_remove_title, 1) :
                        getResources().getString(R.string.context_download)
        );
        return list;
    }

    private List<Drawable> formIconsList() {
        List<Drawable> list = new ArrayList<Drawable>(4);
        list.add(getResources().getDrawable(R.drawable.ic_play));
        list.add(getResources().getDrawable(R.drawable.ic_new));
        list.add(getResources().getDrawable(R.drawable.ic_playlist));
        list.add(getResources().getDrawable(R.drawable.ic_downloaded));
        return list;
    }

    protected void refreshState() {
        this.episodeManager = EpisodeManager.getInstance();
        this.isNew = !episodeManager.isOld(episode);
        this.isInPlaylist = episodeManager.isInPlaylist(episode);
        this.isDownloaded = episodeManager.isDownloadingOrDownloaded(episode);
    }

    protected void refreshList() {
        refreshState();
        int selection = getSelectedItemPosition();
        setAdapter(new ContextMenuAdapter(formList(), formIconsList()));
        setSelection(selection);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PodcastActivity activity = (PodcastActivity) getContext();

        switch (position) {

            case ITEM_PLAY:
                activity.onToggleLoad();
                break;

            case ITEM_REVERSE_MARKER:
                episodeManager.setState(episode, isNew);
                activity.onStateChanged(episode);
                refreshList();
                break;

            case ITEM_DOWNLOAD:
                boolean nowRemoving = isDownloaded;
                activity.onToggleDownload(new OnDeleteDownloadsConfirmationListener() {
                    @Override
                    public void onConfirmDeletion() {
                        refreshList();
                    }

                    @Override
                    public void onCancelDeletion() {
                    }
                });
                if (!nowRemoving) {
                    refreshList();
                }
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
                refreshList();
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

//        hide();
    }

//    public void show() {
//        setEnabled(true);
//        mIsOpened = true;
//        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.menu_show);
//        setAnimation(animation);
//        animation.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//                selectFirst();
//                requestFocus();
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//            }
//        });
//    }

//    public void hide() {
//        if (!mIsOpened) {
//            return;
//        }
//        clearFocus();
//        mIsOpened = false;
//        setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.menu_hide));
////        setVisibility(View.INVISIBLE);
//        setEnabled(false);
//
//        episodeContextMenuListener.onEpisodeContextMenuClose();
//    }

//    public void toggle() {
//        if (mIsOpened) {
//            hide();
//        } else {
//            show();
//        }
//    }

    private void selectFirst() {
            setSelection(0);
    }

//    @Override
//    public void onFocusChange(View v, boolean hasFocus) {
//        if (!hasFocus) {
//            hide();
//        }
//    }

    public class ContextMenuAdapter extends BaseAdapter {

        List<String> items;
        List<Drawable> icons;
        LayoutInflater inflater;
        private static final int ITEM_RESOURCE = R.layout.context_menu_item;
        private static final int COMPOUND_DRAWABLE_PADDING = 5;

        public ContextMenuAdapter(List<String> strings, List<Drawable> icons) {
            super();
            this.items = strings;
            this.icons = icons;
            inflater = LayoutInflater.from(getContext());
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public String getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v;
            if (convertView == null) {
                v = inflater.inflate(ITEM_RESOURCE, parent, false);
            } else {
                v = convertView;
            }

            TextView textView = (TextView) v.findViewById(android.R.id.text1);

            textView.setText(items.get(position));
            textView.setCompoundDrawablesWithIntrinsicBounds(icons.get(position), null, null, null);
            textView.setCompoundDrawablePadding(COMPOUND_DRAWABLE_PADDING);

            return v;
        }
    }
}

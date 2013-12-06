package net.alliknow.podcatcher.view;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ExpandCollapseAnimation extends Animation {

    private int initialWidth;
    private int deltaWidth;
    private View view;
    private int direction;

    public static final int DIRECTION_EXPAND = 0;
    public static final int DIRECTION_COLLAPSE = 1;

    public ExpandCollapseAnimation(View view, int targetWidth, int direction) {
        super();
        this.initialWidth = view.getWidth();
        this.deltaWidth = this.initialWidth - targetWidth;
        this.view = view;
        this.direction = direction;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        int width = initialWidth - (int) (interpolatedTime * deltaWidth);
//        switch (direction) {
//            case DIRECTION_EXPAND:
//                width = (int) (interpolatedTime * deltaWidth);
//                break;
//            case DIRECTION_COLLAPSE:
//                width = (int) ((1.0 - interpolatedTime) * deltaWidth);
//                break;
//            default:
//                throw new IllegalArgumentException();
//        }
        view.getLayoutParams().width = width;
        view.requestLayout();
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}

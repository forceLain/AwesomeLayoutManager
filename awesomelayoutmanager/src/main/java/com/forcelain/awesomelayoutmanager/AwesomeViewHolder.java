package com.forcelain.awesomelayoutmanager;

/**
 * Implement this interface to receive transition changes
 */
public interface AwesomeViewHolder {
    /**
     * Called by AwesomeLayoutManager when the view holder has been created and during the transition animation
     * @param progress [0, 1]. 0 is for Orientation.VERTICAL state; 1 is for Orientation.HORIZONTAL
     */
    void onStateChanged(float progress);
}

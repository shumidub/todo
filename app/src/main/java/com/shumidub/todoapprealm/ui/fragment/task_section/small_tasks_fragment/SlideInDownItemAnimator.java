package com.shumidub.todoapprealm.ui.fragment.task_section.small_tasks_fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by A.shumidub on 16.03.18.
 *
 */

/** couldnt run yet */

public class SlideInDownItemAnimator extends SimpleItemAnimator {


    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {

            holder.itemView.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(3.0f))
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            dispatchAddFinished(holder);
                        }
                    })
                    .start();

        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        return false;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {



        return false;
    }

    @Override
    public void runPendingAnimations() {

    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder holder) {


        holder.itemView.animate()
                .translationY(0)
                .setInterpolator(new DecelerateInterpolator(3.0f))
                .setDuration(10000)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dispatchAddFinished(holder);
                    }
                })
                .start();

    }

    @Override
    public void endAnimations() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}

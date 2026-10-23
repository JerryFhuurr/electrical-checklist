package com.jerry.electrical_checklist;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.view.View;

/** Short, interruptible page entrances. Replacing a page never waits for motion to finish. */
final class PageTransitions {
    private Animator animator;
    private View target;

    void enter(View page, boolean backward) {
        clear();
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        boolean fromLeft = backward ^ (page.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        target = page;
        animator = AnimatorInflater.loadAnimator(page.getContext(),
                fromLeft ? R.animator.page_back_enter : R.animator.page_forward_enter);
        animator.setTarget(page);
        animator.start();
    }

    void clear() {
        if (animator != null) animator.cancel();
        if (target != null) {
            target.setAlpha(1f);
            target.setTranslationX(0f);
        }
        animator = null;
        target = null;
    }
}

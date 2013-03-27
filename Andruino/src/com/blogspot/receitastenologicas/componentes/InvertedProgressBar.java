package com.blogspot.receitastenologicas.componentes;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class InvertedProgressBar extends ProgressBar {

	public InvertedProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWillNotDraw(false);
		// setGravity(Gravity.CENTER);
	}

	public InvertedProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWillNotDraw(false);
		// setGravity(Gravity.CENTER);
	}

	public InvertedProgressBar(Context context) {
		super(context);
		setWillNotDraw(false);
		// setGravity(Gravity.CENTER);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int left = getPaddingLeft();
		int top = getPaddingTop();
		int right = getPaddingRight();
		int bottom = getPaddingBottom();
		int width = getWidth() - left - right;
		int height = getHeight() - top - bottom;

		int saveCount = canvas.getSaveCount();
		canvas.translate(left + width / 2, top + height / 2);
		canvas.rotate(-180);
		canvas.translate((-width / 2) - left, (-height / 2) - top);
		canvas.restoreToCount(saveCount);

		super.onDraw(canvas);

	}

}

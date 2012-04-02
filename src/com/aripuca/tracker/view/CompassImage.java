package com.aripuca.tracker.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class CompassImage extends ImageView {
	
	protected float angle = 0;
	
	public CompassImage(Context context) {
		super(context);
	}

	public CompassImage(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		
		canvas.rotate(angle, this.getMeasuredWidth()/2, this.getMeasuredHeight()/2);
		super.onDraw(canvas);

	}
	
	public void setAngle(float a) {
		angle = a;
	}
	
	public float getAngle() {
		return angle;
	}
	
	@Override
	public boolean isInEditMode () {
		return false;
	}
	
}

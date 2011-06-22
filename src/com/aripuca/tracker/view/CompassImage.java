package com.aripuca.tracker.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
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
		
		canvas.rotate(angle, this.getMeasuredWidth()*0.5F, this.getMeasuredHeight()*0.5F);
		super.onDraw(canvas);

	}
	
	public void setAngle(float a) {
		angle = a;
	}

	@Override
	public boolean isInEditMode () {
		return true;
	}
	
}

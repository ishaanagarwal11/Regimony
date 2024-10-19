package com.pe5.regimony;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.BitmapShader;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class CustomCircularImageView extends AppCompatImageView {

    private Path clipPath;
    private Paint paint;

    public CustomCircularImageView(Context context) {
        super(context);
        init();
    }

    public CustomCircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCircularImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clipPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bitmapCanvas = new Canvas(bitmap);

        super.onDraw(bitmapCanvas);

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        clipPath.reset();
        clipPath.addOval(new RectF(0, 0, getWidth(), getHeight()), Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawOval(new RectF(0, 0, getWidth(), getHeight()), paint);
    }
}

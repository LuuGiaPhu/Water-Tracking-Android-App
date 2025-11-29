package com.example.myapplication;

        import android.content.Context;
        import android.content.res.TypedArray;
        import android.graphics.Bitmap;
        import android.graphics.BitmapShader;
        import android.graphics.Canvas;
        import android.graphics.Paint;
        import android.graphics.Shader;
        import android.util.AttributeSet;

        import androidx.appcompat.widget.AppCompatImageView;

        public class CircularImageView extends AppCompatImageView {
            private Paint paint;
            private Paint borderPaint;
            private BitmapShader shader;
            private int borderColor = 0xFFFFFFFF; // Default border color (white)
            private float borderWidth = 4f; // Default border width

            public CircularImageView(Context context) {
                super(context);
                init(null);
            }

            public CircularImageView(Context context, AttributeSet attrs) {
                super(context, attrs);
                init(attrs);
            }

            public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
                super(context, attrs, defStyle);
                init(attrs);
            }

            private void init(AttributeSet attrs) {
                paint = new Paint();
                paint.setAntiAlias(true);

                borderPaint = new Paint();
                borderPaint.setAntiAlias(true);
                borderPaint.setStyle(Paint.Style.STROKE);

                if (attrs != null) {
                    TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircularImageView);
                    borderColor = a.getColor(R.styleable.CircularImageView_borderColor, borderColor);
                    borderWidth = a.getDimension(R.styleable.CircularImageView_borderWidth, borderWidth);
                    a.recycle();
                }

                borderPaint.setColor(borderColor);
                borderPaint.setStrokeWidth(borderWidth);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                Bitmap bitmap = getBitmapFromDrawable();
                if (bitmap != null) {
                    shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    paint.setShader(shader);

                    float radius = Math.min(getWidth() / 2f, getHeight() / 2f) - borderWidth;
                    float centerX = getWidth() / 2f;
                    float centerY = getHeight() / 2f;

                    // Draw the image
                    canvas.drawCircle(centerX, centerY, radius, paint);

                    // Draw the border
                    canvas.drawCircle(centerX, centerY, radius + borderWidth / 2f, borderPaint);
                }
            }

            private Bitmap getBitmapFromDrawable() {
                if (getDrawable() == null) return null;

                Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                getDrawable().setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                getDrawable().draw(canvas);
                return bitmap;
            }

            public void setBorderColor(int color) {
                borderColor = color;
                borderPaint.setColor(borderColor);
                invalidate();
            }

            public void setBorderWidth(float width) {
                borderWidth = width;
                borderPaint.setStrokeWidth(borderWidth);
                invalidate();
            }
        }
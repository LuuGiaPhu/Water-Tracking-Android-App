package com.example.myapplication;

            import android.content.Context;
            import android.graphics.Canvas;
            import android.graphics.LinearGradient;
            import android.graphics.Paint;
            import android.graphics.Path;
            import android.graphics.Shader;
            import android.util.AttributeSet;
            import android.view.View;

            import androidx.core.content.ContextCompat;

            import java.text.SimpleDateFormat;
            import java.util.Date;
            import java.util.Locale;

            public class WaveView extends View {
                private Paint paint;
                private Path path;
                private float waveOffset = 0;
                private float waveSpeed = 6f; // Speed of wave movement
                private float waveHeight = 60; // Height of the wave
                private float horizontalOffset = 0;
                private float horizontalSpeed = 0.02f; // Speed of horizontal movement
                private float time = 0;
                private float waterLevel = 0f; // Initial water level (0%)
                private Paint textPaint;
                private String percentageText = "0%";
                private String lastUpdatedDate = ""; // Track the last updated date
                private float cyclicOffset = 0; // Offset for cyclic left-to-right movement
                private float cyclicSpeed = 2f; // Speed of cyclic movement

                public WaveView(Context context, AttributeSet attrs) {
                    super(context, attrs);
                    init(context);
                }

                private void init(Context context) {
                    paint = new Paint();
                    paint.setStyle(Paint.Style.FILL);
                    path = new Path();

                    textPaint = new Paint();
                    textPaint.setColor(ContextCompat.getColor(context, android.R.color.black));
                    textPaint.setTextSize(48f);
                    textPaint.setTextAlign(Paint.Align.CENTER);

                    // Initialize the last updated date
                    lastUpdatedDate = getCurrentDate();
                }

                public void setWaterLevel(float level) {
                    // Check if the date has changed
                    if (!lastUpdatedDate.equals(getCurrentDate())) {
                        // Reset water level if the date has changed
                        waterLevel = 0f;
                        percentageText = "0%";
                        lastUpdatedDate = getCurrentDate();
                    } else {
                        // Update water level for the current day
                        this.waterLevel = level;
                        this.percentageText = (int) (level * 100) + "%";
                    }
                    invalidate();
                }

                private String getCurrentDate() {
                    // Get the current date in "yyyy-MM-dd" format
                    return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                }

                @Override
                protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);

                    // Enable anti-aliasing for smooth edges
                    paint.setAntiAlias(true);

                    path.reset(); // Reset the path to avoid residual paths
                    int width = getWidth();
                    int height = getHeight();
                    float waveLength = width;

                    // Update offsets
                    horizontalOffset = (float) (waveHeight * Math.sin(time));
                    time += horizontalSpeed;
                    cyclicOffset += cyclicSpeed;
                    if (cyclicOffset > waveLength) {
                        cyclicOffset -= waveLength;
                    }

                    float waterHeightPosition = height * (1 - waterLevel);

                    // Create a gradient for the first wave
                         LinearGradient gradient1 = new LinearGradient(0, waterHeightPosition - waveHeight, 0, waterHeightPosition + waveHeight,
                                 new int[]{
                                         ContextCompat.getColor(getContext(), R.color.dark_blue),
                                         ContextCompat.getColor(getContext(), R.color.blue),
                                         ContextCompat.getColor(getContext(), R.color.light_blue_dark)
                                 },
                                 new float[]{0f, 0.5f, 1f},
                                 Shader.TileMode.CLAMP);
                         paint.setShader(gradient1);

                         // Draw the first wave with reversed movement
                         path.reset();
                         path.moveTo(-waveLength * 1.2f + waveOffset - horizontalOffset - cyclicOffset, waterHeightPosition); // Adjusted starting position
                         for (int i = (int) -waveLength; i < width + waveLength; i += waveLength) {
                             path.quadTo(i + waveLength / 6 - horizontalOffset - cyclicOffset, waterHeightPosition + waveHeight,
                                     i + waveLength / 2 - horizontalOffset - cyclicOffset, waterHeightPosition);
                             path.quadTo(i + 5 * waveLength / 6 - horizontalOffset - cyclicOffset, waterHeightPosition - waveHeight,
                                     i + waveLength - horizontalOffset - cyclicOffset, waterHeightPosition);
                         }
                         path.lineTo(width, height);
                         path.lineTo(0, height);
                         path.close();
                         canvas.drawPath(path, paint);

                    // Create a darker gradient for the second wave
                    LinearGradient gradient2 = new LinearGradient(0, waterHeightPosition - waveHeight, 0, waterHeightPosition + waveHeight,
                            new int[]{
                                    ContextCompat.getColor(getContext(), R.color.darker_blue),
                                    ContextCompat.getColor(getContext(), R.color.dark_blue),
                                    ContextCompat.getColor(getContext(), R.color.blue)
                            },
                            new float[]{0f, 0.5f, 1f},
                            Shader.TileMode.CLAMP);
                    paint.setShader(gradient2);

//                    // Draw the second wave
                    path.reset();
                        path.moveTo(-waveLength * 1.5f - waveOffset - horizontalOffset - cyclicOffset, waterHeightPosition); // Extend starting position
                        for (int i = (int) -waveLength; i < width + waveLength; i += waveLength) {
                            path.quadTo(i + waveLength / 4 - horizontalOffset - cyclicOffset, waterHeightPosition - waveHeight * 1.2f, // Adjust control point for smoother curve
                                    i + waveLength / 2 - horizontalOffset - cyclicOffset, waterHeightPosition);
                            path.quadTo(i + 3 * waveLength / 4 - horizontalOffset - cyclicOffset, waterHeightPosition + waveHeight * 1.2f, // Adjust control point for rounded downward curve
                                    i + waveLength - horizontalOffset - cyclicOffset, waterHeightPosition);
                        }
                        path.lineTo(width, height);
                        path.lineTo(0, height);
                        path.close();
                        canvas.drawPath(path, paint);

                    // Draw the percentage text
                    canvas.drawText(percentageText, width / 2, waterHeightPosition - waveHeight, textPaint);

                    // Update wave offset
                    waveOffset += waveSpeed;
                    if (waveOffset > waveLength) {
                        waveOffset -= waveLength;
                    }

                    postInvalidateOnAnimation(); // Trigger the next frame
                }
            }
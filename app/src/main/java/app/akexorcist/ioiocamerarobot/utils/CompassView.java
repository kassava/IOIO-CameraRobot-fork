package app.akexorcist.ioiocamerarobot.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by OldMan on 04.11.2016.
 */

public class CompassView extends View {
    private enum CompassDirection {
        N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW
    }

    int[] borderGradientColors;
    float[] borderGradientPositions;

    int[] glassGradientColors;
    float[] glassGradientPositions;

    int skyHorizonColorFrom;
    int skyHorizonColorTo;
    int groundHorizonColorFrom;
    int groundHorizonColorTo;

    private Paint markerPaint;
    private Paint textPaint;
    private Paint circlePaint;
    private int textHeight;

    private float bearing;
    float pitch = 0;
    float roll = 0;
    private Resources r;

    public void setBearing(float _bearing) {
        bearing = _bearing;
    }

    public float getBearing() {
        return bearing;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public CompassView(Context context) {
        super(context);
        initCompassView();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCompassView();
    }

    public CompassView(Context context, AttributeSet ats, int defaultStyle) {
        super(context, ats, defaultStyle);
        initCompassView();
    }

    protected void initCompassView() {
        setFocusable(true);
        // Get external resources
        r = this.getResources();

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(r.getColor(R.color.colorPrimaryDark));
        circlePaint.setStrokeWidth(1);
        circlePaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(r.getColor(R.color.text_color));
        textPaint.setTextSize(r.getDimension(R.dimen.text_size_compass));

        textPaint.setFakeBoldText(true);
        textPaint.setSubpixelText(true);
        textPaint.setTextAlign(Paint.Align.LEFT);

        textHeight = (int) textPaint.measureText("yY");

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(r.getColor(R.color.marker_color));
        markerPaint.setAlpha(200);
        markerPaint.setStrokeWidth(1);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setShadowLayer(2, 1, 1, r.getColor(R.color.shadow_color));

        borderGradientColors = new int[4];
        borderGradientPositions = new float[4];

        borderGradientColors[3] = r.getColor(R.color.colorPrimaryDark);
        borderGradientColors[2] = r.getColor(R.color.colorPrimaryDark);
        borderGradientColors[1] = r.getColor(R.color.colorPrimaryDark);
        borderGradientColors[0] = r.getColor(R.color.colorPrimaryDark);
        borderGradientPositions[3] = 0.0f;
        borderGradientPositions[2] = 1 - 0.03f;
        borderGradientPositions[1] = 1 - 0.06f;
        borderGradientPositions[0] = 1.0f;

        glassGradientColors = new int[5];
        glassGradientPositions = new float[5];

        int glassColor = 245;
        glassGradientColors[4] = Color.argb(65, glassColor, glassColor,
                glassColor);
        glassGradientColors[3] = Color.argb(100, glassColor, glassColor,
                glassColor);
        glassGradientColors[2] = Color.argb(50, glassColor, glassColor,
                glassColor);
        glassGradientColors[1] = Color.argb(0, glassColor, glassColor,
                glassColor);
        glassGradientColors[0] = Color.argb(0, glassColor, glassColor,
                glassColor);
        glassGradientPositions[4] = 1 - 0.0f;
        glassGradientPositions[3] = 1 - 0.06f;
        glassGradientPositions[2] = 1 - 0.10f;
        glassGradientPositions[1] = 1 - 0.20f;
        glassGradientPositions[0] = 1 - 1.0f;

        skyHorizonColorFrom = r.getColor(R.color.horizon_sky_from);
        skyHorizonColorTo = r.getColor(R.color.horizon_sky_to);

        groundHorizonColorFrom = r.getColor(R.color.horizon_ground_from);
        groundHorizonColorTo = r.getColor(R.color.horizon_ground_to);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The compass is a circle that fills as much space as possible.
        // Set the measured dimensions by figuring out the shortest boundary,
        // height or width.
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);

        int d = Math.min(measuredWidth, measuredHeight);

        setMeasuredDimension(d, d);
    }

    private int measure(int measureSpec) {
        int result = 0;

        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float ringWidth = textHeight + 3;
        int height = getMeasuredHeight();
        int width = getMeasuredWidth();

        int px = width / 2;
        int py = height / 2;
        Point center = new Point(px, py);

        int radius = Math.min(px, py) - 2;

        RectF boundingBox = new RectF(center.x - radius, center.y - radius,
                center.x + radius, center.y + radius);

        RectF innerBoundingBox = new RectF(center.x - radius + ringWidth,
                center.y - radius + ringWidth, center.x + radius - ringWidth,
                center.y + radius - ringWidth);

        float innerRadius = innerBoundingBox.height() / 2;
        RadialGradient borderGradient = new RadialGradient(px, py, radius,
                borderGradientColors, borderGradientPositions, Shader.TileMode.MIRROR);

        Paint pgb = new Paint();
        pgb.setShader(borderGradient);

        Path outerRingPath = new Path();
        outerRingPath.addOval(boundingBox, Path.Direction.CW);

        canvas.drawPath(outerRingPath, pgb);
        LinearGradient skyShader = new LinearGradient(center.x,
                innerBoundingBox.top, center.x, innerBoundingBox.bottom,
                skyHorizonColorFrom, skyHorizonColorTo, Shader.TileMode.CLAMP);

        Paint skyPaint = new Paint();
        skyPaint.setShader(skyShader);

        LinearGradient groundShader = new LinearGradient(center.x,
                innerBoundingBox.top, center.x, innerBoundingBox.bottom,
                groundHorizonColorFrom, groundHorizonColorTo, Shader.TileMode.CLAMP);

        Paint groundPaint = new Paint();
        groundPaint.setShader(groundShader);
        float tiltDegree = pitch;
        while (tiltDegree > 90 || tiltDegree < -90) {
            if (tiltDegree > 90)
                tiltDegree = -90 + (tiltDegree - 90);
            if (tiltDegree < -90)
                tiltDegree = 90 - (tiltDegree + 90);
        }

        float rollDegree = roll;
        while (rollDegree > 180 || rollDegree < -180) {
            if (rollDegree > 180)
                rollDegree = -180 + (rollDegree - 180);
            if (rollDegree < -180)
                rollDegree = 180 - (rollDegree + 180);
        }
        Path skyPath = new Path();
        skyPath.addArc(innerBoundingBox, -tiltDegree, (180 + (2 * tiltDegree)));
        canvas.rotate(-rollDegree, px, py);
        canvas.drawOval(innerBoundingBox, groundPaint);
        canvas.drawPath(skyPath, skyPaint);
        canvas.drawPath(skyPath, markerPaint);
        int markWidth = radius / 3;
        int startX = center.x - markWidth;
        int endX = center.x + markWidth;

        double h = innerRadius * Math.cos(Math.toRadians(90 - tiltDegree));
        float justTiltY = (float) (center.y - h);

        float pxPerDegree = (innerBoundingBox.height() / 2) / 45f;
        for (int i = 90; i >= -90; i -= 10) {
            double ypos = justTiltY + i * pxPerDegree;

            // Only display the scale within the inner face.
            if ((ypos < (innerBoundingBox.top + textHeight))
                    || (ypos > innerBoundingBox.bottom - textHeight))
                continue;

            // Draw a line and the tilt angle for each scale increment.
            canvas.drawLine(startX, (float) ypos, endX, (float) ypos,
                    markerPaint);
            int displayPos = (int) (tiltDegree - i);
            String displayString = String.valueOf(displayPos);
            float stringSizeWidth = textPaint.measureText(displayString);
            canvas.drawText(displayString,
                    (int) (center.x - stringSizeWidth / 2), (int) (ypos) + 1,
                    textPaint);
            markerPaint.setStrokeWidth(6);
            if (justTiltY < 500)
                canvas.drawLine(center.x - justTiltY, (float) justTiltY,
                        center.x + justTiltY, (float) justTiltY, markerPaint);
            else
                canvas.drawLine(center.x - (1000 - justTiltY), (float) justTiltY,
                        center.x + (1000 - justTiltY), (float) justTiltY, markerPaint);
            markerPaint.setStrokeWidth(1);
        }

        // Draw the arrow
        Path rollArrow = new Path();
        markerPaint.setStrokeWidth(4);
        rollArrow.moveTo(center.x - 20, (int) innerBoundingBox.top + 50);
        rollArrow.lineTo(center.x, (int) innerBoundingBox.top + 28);
        rollArrow.moveTo(center.x + 20, innerBoundingBox.top + 50);
        rollArrow.lineTo(center.x, innerBoundingBox.top + 28);
        canvas.drawPath(rollArrow, markerPaint);
        markerPaint.setStrokeWidth(1);
        // Draw the string
        String rollText = String.valueOf(rollDegree);
        double rollTextWidth = textPaint.measureText(rollText);
        canvas.drawText(rollText, (float) (center.x - rollTextWidth / 2),
                innerBoundingBox.top + textHeight + 50, textPaint);
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M) canvas.restore();
//        canvas.restore();

        canvas.save();
        canvas.rotate(180, center.x, center.y);
        for (int i = -180; i < 180; i += 10) {
            // Show a numeric value every 30 degrees
            if (i % 30 == 0) {
                String rollString = String.valueOf(i * -1);
                float rollStringWidth = textPaint.measureText(rollString);
                PointF rollStringCenter = new PointF(center.x - rollStringWidth
                        / 2, innerBoundingBox.top + 1 + textHeight);
                canvas.drawText(rollString, rollStringCenter.x,
                        rollStringCenter.y, textPaint);
                if (i == 0) {

                    Path path = new Path();
                    // î÷èñòêà path
                    path.reset();
                    markerPaint.setStrokeWidth(10);
                    markerPaint.setColor(r
                            .getColor(R.color.actionbar_background_sensors));

                    // airplane
                    path.moveTo(width / 4, (float) py);
                    path.lineTo((width - 60) / 2, (float) py);
                    path.addCircle(px, (float) py, 30, Path.Direction.CW);
                    path.moveTo((width + 60) / 2, (float) py);
                    path.lineTo(3 * width / 4, (float) py);
                    path.moveTo(px, (float) py - 26);
                    path.lineTo(px, (float) py - 80);
                    // markerPaint);
                    canvas.drawPath(path, markerPaint);
                    // path airplane
                    markerPaint.setColor(r.getColor(R.color.marker_color));
                    markerPaint.setStrokeWidth(3);
                    // float[] intervals = new float[] { 60.0f, 10.0f };
                    // float phase = 0;
                    // DashPathEffect mDashPathEffect = new
                    // DashPathEffect(intervals, phase);
                    // markerPaint.setPathEffect(mDashPathEffect);
                    path.reset();
                    // one line
                    path.moveTo(width / 4, (float) 3 * py / 2 + 50);
                    path.lineTo(px, (float) py);
                    path.moveTo(3 * width / 4, (float) 3 * py / 2 + +50);
                    path.lineTo(px, (float) py);
                    // two line
                    path.moveTo(width / 8, (float) 5 * py / 4);
                    path.lineTo(px, (float) py);
                    path.moveTo(7 * width / 8, (float) 5 * py / 4);
                    path.lineTo(px, (float) py);
                    canvas.drawPath(path, markerPaint);
                    path.reset();
                    markerPaint.setStrokeWidth(1);
                    // markerPaint.setColor(r.getColor(R.color.marker_color));
                    // markerPaint.setPathEffect(null);
                }
            }
            // Otherwise draw a marker line
            else {
                canvas.drawLine(center.x, (int) innerBoundingBox.top, center.x,
                        (int) innerBoundingBox.top + 5, markerPaint);

            }

            canvas.rotate(10, center.x, center.y);
        }
        canvas.restore();
        canvas.save();
        canvas.rotate(-1 * (bearing), px, py);

        double increment = 22.5;

        for (double i = 0; i < 360; i += increment) {
            CompassDirection cd = CompassDirection.values()[(int) (i / 22.5)];
            String headString = cd.toString();

            float headStringWidth = textPaint.measureText(headString);
            PointF headStringCenter = new PointF(
                    center.x - headStringWidth / 2, boundingBox.top + 1
                    + textHeight);

            if (i % increment == 0)
                canvas.drawText(headString, headStringCenter.x,
                        headStringCenter.y, textPaint);
            else
                canvas.drawLine(center.x, (int) boundingBox.top, center.x,
                        (int) boundingBox.top + 3, markerPaint);

            canvas.rotate((int) increment, center.x, center.y);
        }
        canvas.restore();
        RadialGradient glassShader = new RadialGradient(center.x, center.y,
                (int) innerRadius, glassGradientColors, glassGradientPositions,
                Shader.TileMode.CLAMP);
        Paint glassPaint = new Paint();
        glassPaint.setShader(glassShader);

        canvas.drawOval(innerBoundingBox, glassPaint);
        // Draw the outer ring
        canvas.drawOval(boundingBox, circlePaint);

        // Draw the inner ring
        circlePaint.setStrokeWidth(2);
        canvas.drawOval(innerBoundingBox, circlePaint);
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M) canvas.restore();
//        canvas.restore();
    }
}

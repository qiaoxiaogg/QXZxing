
package com.yzq.zxinglibrary.view;


import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.google.zxing.ResultPoint;
import com.yzq.zxinglibrary.R;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

public final class ViewfinderView extends View {

    /*界面刷新间隔时间*/
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private Paint paint, scanLinePaint, reactPaint, frameLinePaint, paintText;
    private Bitmap resultBitmap;
    private int maskColor; // 取景框外的背景颜色
    private int resultColor;// result Bitmap的颜色
    private int resultPointColor; // 特征点的颜色
    private int reactColor;//四个角的颜色
    private int scanLineColor;//扫描线的颜色
    private int frameLineColor;//边框线的颜色


    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    // 扫描线移动的y
    private int scanLineTop;

    private ZxingConfig config;
    private ValueAnimator valueAnimator;
    private Rect frame;

    private Bitmap bitmap;
    private boolean isShowScanBitmap = false;
    private Context context;

    public ViewfinderView(Context context) {
        this(context, null);
        this.context = context;
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;

    }


    public void setZxingConfig(ZxingConfig config) {
        this.config = config;
        reactColor = ContextCompat.getColor(context, config.getReactColor());

        if (config.getScanBitmap() != 0) {
            bitmap = BitmapFactory.decodeResource(getResources(), config.getScanBitmap());
            isShowScanBitmap = config.isShowScanBitmap();
        }
//        if (config.getFrameLineColor() != -1) {
//            frameLineColor = ContextCompat.getColor(context, config.getFrameLineColor());
//        }
        scanLineColor = ContextCompat.getColor(context, config.getScanLineColor());
        initPaint();

    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        maskColor = ContextCompat.getColor(context, R.color.viewfinder_mask);
        resultColor = ContextCompat.getColor(context, R.color.result_view);
        resultPointColor = ContextCompat.getColor(context, R.color.possible_result_points);

        possibleResultPoints = new ArrayList<ResultPoint>(10);
        lastPossibleResultPoints = null;


    }

    public Bitmap getNewBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        // 获得图片的宽高.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算缩放比例.
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数.
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片.
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return newBitmap;
    }

    private void initPaint() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        /*四个角的画笔*/
        reactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reactPaint.setStrokeWidth(dp2px(1));
        reactPaint.setStyle(Paint.Style.FILL);
        reactPaint.setDither(true);
        reactPaint.setColor(reactColor);

        /*边框线画笔*/

        if (frameLineColor != -1) {
            frameLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            frameLinePaint.setColor(ContextCompat.getColor(getContext(), config.getFrameLineColor()));
            frameLinePaint.setStrokeWidth(dp2px(1));
            frameLinePaint.setStyle(Paint.Style.STROKE);
        }


        /*扫描线画笔*/
        scanLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scanLinePaint.setStrokeWidth(dp2px(1));
        scanLinePaint.setStyle(Paint.Style.FILL);
        scanLinePaint.setDither(true);
        scanLinePaint.setColor(scanLineColor);

        /*绘制提示文本*/
        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setStrokeWidth(dp2px(1));
        paintText.setStyle(Paint.Style.FILL);
        paintText.setDither(true);
        paintText.setColor(0xffffffff);
        paintText.setTextSize(getResources().getDimensionPixelSize(R.dimen.x30));
        paintText.setTextAlign(Paint.Align.CENTER);

    }

    private void initAnimator() {
        if (valueAnimator == null) {
            if (isShowScanBitmap) {
                valueAnimator = ValueAnimator.ofInt(frame.top, frame.bottom - bitmap.getHeight());
            } else {
                valueAnimator = ValueAnimator.ofInt(frame.top, frame.bottom);
            }
            valueAnimator.setDuration(2000);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.setRepeatMode(ValueAnimator.RESTART);
            valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    scanLineTop = (int) animation.getAnimatedValue();
                    invalidate();

                }
            });

            valueAnimator.start();
        }


    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;


    }

    public void stopAnimator() {
        if (valueAnimator != null) {
            valueAnimator.end();
            valueAnimator.cancel();
            valueAnimator = null;
        }

    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {

        if (cameraManager == null) {
            return;
        }

        // frame为取景框
        frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        initAnimator();

        int width = canvas.getWidth();
        int height = canvas.getHeight();


        /*绘制遮罩*/
        drawMaskView(canvas, frame, width, height);
        /*绘制取景框边框*/
        drawFrameBounds(canvas, frame);
        /*绘制提示文本*/
        canvas.drawText("将二维码置于框内进行扫描", getScreenWidth(context)/2, frame.bottom + getResources().getDimensionPixelSize(R.dimen.x48), paintText);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

            /*绘制扫描线*/
            drawScanLight(canvas, frame);
            /*绘制闪动的点*/
            // drawPoint(canvas, frame, previewFrame);
        }
    }

    private int mScreenWidth;

    public int getScreenWidth(Context context) {
        if (mScreenWidth <= 0) {
            mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        }
        return mScreenWidth;
    }

    private void drawPoint(Canvas canvas, Rect frame, Rect previewFrame) {
        float scaleX = frame.width() / (float) previewFrame.width();
        float scaleY = frame.height() / (float) previewFrame.height();

        // 绘制扫描线周围的特征点
        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;
        int frameLeft = frame.left;
        int frameTop = frame.top;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<ResultPoint>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setColor(resultPointColor);
            synchronized (currentPossible) {
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frameLeft
                                    + (int) (point.getX() * scaleX), frameTop
                                    + (int) (point.getY() * scaleY), POINT_SIZE,
                            paint);
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frameLeft
                            + (int) (point.getX() * scaleX), frameTop
                            + (int) (point.getY() * scaleY), radius, paint);
                }
            }
        }

        // Request another update at the animation interval, but only
        // repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY, frame.left - POINT_SIZE,
                frame.top - POINT_SIZE, frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    private void drawMaskView(Canvas canvas, Rect frame, int width, int height) {
        // Draw the exterior (i.e. outside the framing rect) darkened
        // 绘制取景框外的暗灰色的表面，分四个矩形绘制
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        /*上面的框*/
        canvas.drawRect(0, 0, width, frame.top, paint);
        /*绘制左边的框*/
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        /*绘制右边的框*/
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
                paint);
        /*绘制下面的框*/
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }


    /**
     * 绘制取景框边框
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameBounds(Canvas canvas, Rect frame) {

        /*扫描框的边框线*/
        if (frameLineColor != -1) {
            canvas.drawRect(frame, frameLinePaint);
        }


        /*四个角的长度和宽度*/
        int width = frame.width();
        int corLength = (int) (width * 0.07);
        int corWidth = (int) (corLength * 0.1);

        corWidth = corWidth > 15 ? 15 : corWidth;


        /*角在线外*/
        // 左上角
        canvas.drawRect(frame.left - corWidth, frame.top, frame.left, frame.top
                + corLength, reactPaint);
        canvas.drawRect(frame.left - corWidth, frame.top - corWidth, frame.left
                + corLength, frame.top, reactPaint);
        // 右上角
        canvas.drawRect(frame.right, frame.top, frame.right + corWidth,
                frame.top + corLength, reactPaint);
        canvas.drawRect(frame.right - corLength, frame.top - corWidth,
                frame.right + corWidth, frame.top, reactPaint);
        // 左下角
        canvas.drawRect(frame.left - corWidth, frame.bottom - corLength,
                frame.left, frame.bottom, reactPaint);
        canvas.drawRect(frame.left - corWidth, frame.bottom, frame.left
                + corLength, frame.bottom + corWidth, reactPaint);
        // 右下角
        canvas.drawRect(frame.right, frame.bottom - corLength, frame.right
                + corWidth, frame.bottom, reactPaint);
        canvas.drawRect(frame.right - corLength, frame.bottom, frame.right
                + corWidth, frame.bottom + corWidth, reactPaint);
    }


    /**
     * 绘制移动扫描线
     *
     * @param canvas
     * @param frame
     */
    private void drawScanLight(Canvas canvas, Rect frame) {
        if (isShowScanBitmap) {
            /*设置bitmap的方式*/
            Bitmap b = getNewBitmap(bitmap, frame.right - frame.left, bitmap.getHeight());
            canvas.drawBitmap(b, frame.left, scanLineTop, null);
        } else {
            /*手动画线条的方式*/
            canvas.drawLine(frame.left, scanLineTop, frame.right, scanLineTop, scanLinePaint);

        }


    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }


    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());

    }

}

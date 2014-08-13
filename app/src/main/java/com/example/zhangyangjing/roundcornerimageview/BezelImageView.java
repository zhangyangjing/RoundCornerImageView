/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.zhangyangjing.roundcornerimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * An {@link android.widget.ImageView} that draws its contents inside a mask and draws a border
 * drawable on top. This is useful for applying a beveled look to image contents, but is also
 * flexible enough for use with other desired aesthetics.
 */
public class BezelImageView extends ImageView {

    private Rect mBounds;
    private RectF mBoundsF;

    private ColorMatrixColorFilter mDesaturateColorFilter;
    private boolean mDesaturateOnPress = false;

    private boolean mCacheValid = false;
    private Bitmap mCacheBitmap;
    private int mCachedWidth;
    private int mCachedHeight;
    private float mCornerRadius = 6;

    private Path mMaskedPath;
    private Paint mMaskedPaint;
    private Paint mDesaturatePaint;

    public BezelImageView(Context context) {
        this(context, null);
    }

    public BezelImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BezelImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Attribute initialization
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BezelImageView,
                defStyle, 0);

        mDesaturateOnPress = a.getBoolean(R.styleable.BezelImageView_desaturateOnPress,
                mDesaturateOnPress);
        mCornerRadius = a.getDimension(R.styleable.BezelImageView_cornerRadius, mCornerRadius);

        a.recycle();

        mDesaturatePaint = new Paint();
        mMaskedPaint = new Paint();
        mMaskedPaint.setAntiAlias(true);
        mMaskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Always want a cache allocated.
        mCacheBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

        if (mDesaturateOnPress) {
            // Create a desaturate color filter for pressed state.
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            mDesaturateColorFilter = new ColorMatrixColorFilter(cm);
        }
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        final boolean changed = super.setFrame(l, t, r, b);
        mBounds = new Rect(0, 0, r - l, b - t);
        mBoundsF = new RectF(mBounds);

        if (changed) {
            mCacheValid = false;
        }

        return changed;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBounds == null) {
            return;
        }

        int width = mBounds.width();
        int height = mBounds.height();

        if (width == 0 || height == 0) {
            return;
        }

        if (!mCacheValid || width != mCachedWidth || height != mCachedHeight) {
            // Need to redraw the cache
            if (width == mCachedWidth && height == mCachedHeight) {
                // Have a correct-sized bitmap cache already allocated. Just erase it.
                mCacheBitmap.eraseColor(0);
            } else {
                // Allocate a new bitmap with the correct dimensions.
                mCacheBitmap.recycle();
                //noinspection AndroidLintDrawAllocation
                mCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCachedWidth = width;
                mCachedHeight = height;
                generateMaskPath(mCachedWidth, mCachedHeight);
            }


            Canvas cacheCanvas = new Canvas(mCacheBitmap);
            int sc = cacheCanvas.save();
            mDesaturatePaint.setColorFilter((mDesaturateOnPress && isPressed())
                    ? mDesaturateColorFilter : null);
            cacheCanvas.saveLayer(mBoundsF, mDesaturatePaint,
                    Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
            super.onDraw(cacheCanvas);
            cacheCanvas.drawPath(this.mMaskedPath, this.mMaskedPaint);
            cacheCanvas.restoreToCount(sc);
        }

        // Draw from cache
        canvas.drawBitmap(mCacheBitmap, mBounds.left, mBounds.top, null);
    }

    private void generateMaskPath(int width, int height) {
        mMaskedPath = new Path();
        mMaskedPath.addRoundRect(new RectF(0.0F, 0.0F, width, height), mCornerRadius, mCornerRadius, Path.Direction.CW);
        mMaskedPath.setFillType(Path.FillType.INVERSE_WINDING);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        ViewCompat.postInvalidateOnAnimation(this);
    }
}

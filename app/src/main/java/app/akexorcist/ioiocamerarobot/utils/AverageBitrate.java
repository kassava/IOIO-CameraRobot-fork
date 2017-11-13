package app.akexorcist.ioiocamerarobot.utils;

import android.os.SystemClock;

/**
 * Computes an average bit rate.
 **/
public class AverageBitrate {

    private final static long RESOLUTION = 200;

    private long mOldNow, mNow, mDelta;
    private long[] mElapsed, mSum;
    private int mCount, mIndex, mTotal;
    private int mSize;

    public AverageBitrate() {
        mSize = 5000 / ((int) RESOLUTION);
        reset();
    }

    public AverageBitrate(int delay) {
        mSize = delay / ((int) RESOLUTION);
        reset();
    }

    public void reset() {
        mSum = new long[mSize];
        mElapsed = new long[mSize];
        mNow = SystemClock.elapsedRealtime();
        mOldNow = mNow;
        mCount = 0;
        mDelta = 0;
        mTotal = 0;
        mIndex = 0;
    }

    public void push(int length) {
        mNow = SystemClock.elapsedRealtime();
        if (mCount > 0) {
            mDelta += mNow - mOldNow;
            mTotal += length;
            if (mDelta > RESOLUTION) {
                mSum[mIndex] = mTotal;
                mTotal = 0;
                mElapsed[mIndex] = mDelta;
                mDelta = 0;
                mIndex++;
                if (mIndex >= mSize) mIndex = 0;
            }
        }
        mOldNow = mNow;
        mCount++;
    }

    public int average() {
        long delta = 0, sum = 0;
        for (int i = 0; i < mSize; i++) {
            sum += mSum[i];
            delta += mElapsed[i];
        }
        //Log.d(TAG, "Time elapsed: "+delta);
        return (int) (delta > 0 ? 8000 * sum / delta : 0);
    }
}

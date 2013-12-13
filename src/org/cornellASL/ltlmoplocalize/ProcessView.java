package org.cornellASL.ltlmoplocalize;

import java.util.List;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;

//Extends JavaCameraView - used by OpenCV to display image on android device
//This extension enables user to select desired resolution
public class ProcessView extends JavaCameraView {
	public ProcessView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

}

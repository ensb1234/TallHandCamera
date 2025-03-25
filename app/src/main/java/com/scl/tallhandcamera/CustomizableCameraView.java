/**
 * 作者：涉川良
 * 联系方式：QQ470707134
 */
package com.scl.tallhandcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Size;


import java.util.Arrays;


public class CustomizableCameraView extends JavaCameraView {

    public MainActivity main;

    public CustomizableCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 用于调整帧率。
     * 难以理解，使用camera2时在onCameraViewStarted中调用该方法时仍旧显示mPreviewRequestBuilder为null。
     * 且使用camera2时，默认帧率似乎下降了。
     * 在点击更新ip时，该方法也应调用，进行锁帧。
     * @param fps 帧率
     */
    public void setPreviewFPS(int fps){
//        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
//        try{
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
//        } catch (CameraAccessException e) {
//            throw new RuntimeException(e);
//        }
//        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(min, max));
        if(fps<20){
            fps = 20;
        }
        fps *= 1000;
        int max = fps;
        Camera.Parameters params = mCamera.getParameters();
        for(int[] range : params.getSupportedPreviewFpsRange()){
            System.out.println(Arrays.toString(range));
            if(range[1]>max){
                continue;
            }
            if(range[0]<15000){
                continue;
            }
            fps = range[1];//一般相机应该或许可能差不多能拿出一个可用的吧
        }
        params.setPreviewFpsRange(fps, fps);
        mCamera.setParameters(params);
    }

    @Override
    protected boolean initializeCamera(int width, int height) {
        boolean res = super.initializeCamera(width, height);
        //java浪费青春，写回调或者委托太麻烦了，耦合就耦合吧反正就一demo
        main.setTwoRect(mFrameWidth, mFrameHeight);
        return res;
    }
}
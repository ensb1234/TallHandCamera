/**
 * 作者：涉川良
 * 联系方式：QQ470707134
 */
package com.scl.tallhandcamera;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.SurfaceView;
import android.util.Log;
import android.view.WindowManager;


import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private CustomizableCameraView mOpenCvCameraView;
    private String localIP;
    private TextView textHelp;
    private EditText textConfig;
    private ImageView dark;
    private ImageView imgModel;
    private IMGContainer imgContainerStd;
    private TextView scoreText;
    private ImageView imgTestee;
    private IMGContainer imgContainerTestee;
    private Button btn;
    private int modelWidth;
    private int modelHeight;
    private int targetX;
    private int targetY;
    private int targetW;
    private int targetH;
    public int cameraW = 1920;
    public int cameraH = 1080;
    private TwoRect twoRect;
    public enum TakePhotoModeType{
        TAKE_MODEL,
        AUTO_TAKE,
        MANUAL_TAKE
    }
    private TakePhotoModeType takePhotoMode = TakePhotoModeType.MANUAL_TAKE;
    private boolean btnPressed4TakePhoto = false;
    private float currentBrightness;
    private CameraServer cs;
    private int currentFrame = 20;
    private MatOfPoint contour2;
    private long modelHash;
    private int blockSize;
    private float C;
    private int hashSensitive;
    private float minBlackValue;
    private SharedPreferences sp;
    private SharedPreferences.Editor ed;
    public enum ScanningModeType {
        COUNT_CONTOURS,
        CAL_S
    }//count contours 数轮廓法；cal S 面积法
    public ScanningModeType scanningMode;
    private String errorStr = "";

    //Mat,MatOfPoint,以及相关的List，尽量不要重复赋值！用一个创建一个，方便最后release()

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //测试opencv库是否被导入
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        //初始化关键数据，从私有存储区中读取。
        sp = getSharedPreferences("com.scl.tallhandcamera",MODE_PRIVATE);
        ed = sp.edit();
        scanningMode = ScanningModeType.valueOf(sp.getString("scanning mode", "CAL_S"));
        modelHeight = sp.getInt("model height", 131);
        modelWidth = sp.getInt("model width", 112);
        targetW = sp.getInt("target W", 112);
        targetH = sp.getInt("target H", 131);
        targetX = sp.getInt("target X", 0);
        targetY = sp.getInt("target Y", 0);
        modelHash = sp.getLong("model hash", 0);
        blockSize = sp.getInt("block size", 37);
        C = sp.getFloat("C", 7);
        hashSensitive = sp.getInt("hash sensitive", 10);

        //默认常亮屏幕全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        setContentView(R.layout.activity_main);
        //初始化1080p摄像头
        mOpenCvCameraView = (CustomizableCameraView) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(1920, 1080);
        mOpenCvCameraView.main = this;
        //当用户点击摄录画面角落时，实现各种功能（详见相关class）
        mOpenCvCameraView.setOnTouchListener(new touchCornerListener());

        //初始化摄像头服务器
        try {
            cs = new CameraServer(8887);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        cs.start();
        System.out.println("相机服务端已经启动于端口：" + cs.getPort());

        //初始化红色帮助文字提示，以及相关配置文本。点击红色帮助文字时，这两个文本都消失。
        textHelp = (TextView) findViewById(R.id.textView);
        textConfig = (EditText) findViewById(R.id.textView2);

        textHelp.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                textHelp.setVisibility(View.GONE);
                textConfig.setVisibility(View.GONE);
                mOpenCvCameraView.setVisibility(View.VISIBLE);
                btn.setVisibility(View.VISIBLE);
                //检测textView2的配置文本并尝试解析
                analyzeTextConfig();
            }
        });

        //获取窗口尺寸（已废除）
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//        if (wm != null) {
//            wm.getDefaultDisplay().getRealMetrics(displayMetrics);
//        }
//        int width = displayMetrics.widthPixels;
//        int height = displayMetrics.heightPixels;

        //初始化黑幕
        dark = (ImageView) findViewById(R.id.dark);
        dark.setVisibility(View.GONE);
        //获取当前屏幕亮度
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        currentBrightness = layoutParams.screenBrightness;
        //当用户点击黑幕时，关闭黑幕，恢复亮度
        dark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Window window = getWindow();
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                //设置屏幕亮度到之前的亮度
                layoutParams.screenBrightness = currentBrightness;
                window.setAttributes(layoutParams);
                //关闭黑幕
                dark.setVisibility(View.GONE);
            }
        });

        //初始化拍照按钮。按下该按钮以后，设置为按过了的状态（接下来扫描到的画面将存储为模板画，在相关代码中实现）
        btn = (Button) findViewById(R.id.btn);
        btn.setOnTouchListener(new View.OnTouchListener() {
            private int state = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if(imgContainerStd.getVisibility() == View.VISIBLE) {
                            state = 1;
                        }else if(imgContainerTestee.getVisibility() == View.VISIBLE){
                            state = 2;
                        }else{
                            state = 3;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(state >= 3){
                            state = 4;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if(state == 1){
                            mOpenCvCameraView.setVisibility(View.VISIBLE);
                            imgContainerStd.setVisibility(View.GONE);
                            setSp();
                        }else if(state == 2){
                            mOpenCvCameraView.setVisibility(View.VISIBLE);
                            imgContainerTestee.setVisibility(View.GONE);
                        }else if(state == 3){
                            if(takePhotoMode != TakePhotoModeType.AUTO_TAKE){
                                btnPressed4TakePhoto = true;
                            }
                        }else if(state == 4){
                            //切换自动识别（橙色），手动识别（白色默认），手动拍模板照（绿色）
                            if(takePhotoMode == TakePhotoModeType.AUTO_TAKE){
                                takePhotoMode = TakePhotoModeType.MANUAL_TAKE;
                                btn.setBackgroundResource(R.drawable.round_button);
                            }else if(takePhotoMode == TakePhotoModeType.MANUAL_TAKE){
                                takePhotoMode = TakePhotoModeType.TAKE_MODEL;
                                btn.setBackgroundResource(R.drawable.round_button_take_model);
                            }else{
                                takePhotoMode = TakePhotoModeType.AUTO_TAKE;
                                btn.setBackgroundResource(R.drawable.round_button_auto);
                            }
                        }
                        state = 0;
                        break;
                }
                return true;
            }
        });

        //这个是二值化截图的容器，同时也是若干个GreatGridView容器。
        imgContainerStd = findViewById(R.id.container);
        imgContainerStd.isAnswers = true;
        imgContainerStd.setFeatures(sp.getString("img container features", "x0y0_h180w180_r3c4_P:A,B,CD_2:2+2x,3:4-2x"));

        //这个位图容器用于显示刚刚拍下来的、二值化并截取后的画面。
        imgModel = (ImageView) findViewById(R.id.img);
        imgContainerStd.setOnTouchListener(new TouchImgListener());

        //设置配置文本。
        setTextConfig();

        scoreText = findViewById(R.id.scoretxt);
        imgTestee = findViewById(R.id.img2);
        imgContainerTestee = findViewById(R.id.container2);

        minBlackValue = sp.getFloat("min black value", 1.2f);
    }

    public void setTwoRect(int w, int h){
        cameraW = w;
        cameraH = h;
        twoRect = new TwoRect(w, h, modelWidth, modelHeight, targetX, targetY, targetW, targetH);
    }

    /**
     * 用于将dp值转换为px
     * @param dp 要转换的dp值
     * @return 输出的px值。int格式。
     */
    public static int dp2px(int dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 点击摄录画面不同角落，抬起手指后，实现对应功能。<br/>
     * 注意：以下语境基于横屏操作，可参考文字方向。<br/>
     * 左上角：黑屏节能模式<br/>
     * 右上角：更新ip、显示帮助文本、配置文本<br/>
     */
    public class touchCornerListener implements View.OnTouchListener{
        @SuppressLint("SetTextI18n")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_UP){
                boolean x = event.getX()/v.getWidth() < 0.5;
                boolean y = event.getY()/v.getHeight()< 0.5;
                if(x){
                    if(y){
                        //左上
                        Window window = getWindow();
                        WindowManager.LayoutParams layoutParams = window.getAttributes();
                        //设置屏幕亮度最低
                        layoutParams.screenBrightness = 0.0f;
                        window.setAttributes(layoutParams);
                        //显示黑幕（用于屏幕节能，针对LCD作用不大）
                        dark.setVisibility(View.VISIBLE);
                    }else{
                        //左下
                    }
                }else{
                    if(y){
                        //右上
                        //获取实时IP
                        localIP = CameraServer.getLocalIpAddress();
                        //更新文字
                        textHelp.setText(getString(R.string.textView_pre) + "\nws://" + localIP + ":8887/\n" + errorStr);
                        setTextConfig();
                        textHelp.setVisibility(View.VISIBLE);
                        textConfig.setVisibility(View.VISIBLE);
                        btn.setVisibility(View.GONE);
                        System.out.println("地址已刷新。现在地址："+localIP);
                        mOpenCvCameraView.setVisibility(View.GONE);
                    }else{
                        //右下
                    }
                }
            }
            return true;//表示事件已经被消费
        }
    }

//配置相关（开始）----------------------------------------------------------------------------------------------

    /**
     * 生成配置文本。
     */
    private void setTextConfig(){
        @SuppressLint("DefaultLocale")
        String str = String.format("%s=%s\n%s=%d,%d\n%s=%d,%d\n%s=%d,%d\n%s=%016X\n%s=%d\n%s=%f\n%s=%d\n%s=%f\n%s",
                getString(R.string.scanningMode),scanningMode.toString(),
                getString(R.string.modelWH), modelWidth, modelHeight,
                getString(R.string.targetXY), targetX, targetY,
                getString(R.string.targetWH), targetW, targetH,
                getString(R.string.modelHash),modelHash,
                getString(R.string.blockSize),blockSize,
                getString(R.string.C),C,
                getString(R.string.hashSensitive),hashSensitive,
                getString(R.string.minBlackValue),minBlackValue,
                imgContainerStd.features == null? "" : String.join("\n", imgContainerStd.features));
        textConfig.setText(str);
    }

    /**
     * 读取新的配置文本并解析。
     * @return 如果解析出了较大问题，则返回false
     */
    private Boolean analyzeTextConfig(){
        String[] rows = String.valueOf(textConfig.getText()).split("\\r?\\n|\\r");
        if(rows.length < 8){
            return false;
        }
        String str;
        imgContainerStd.features = new ArrayList<>();
        for(int i = 0; i < rows.length; i++){
            String[] strs = rows[i].split("=");
            if(strs.length != 2){
                strs = rows[i].split("_");
                if(strs.length == 5){
                    imgContainerStd.features.add(rows[i]);
                }
                continue;
            }
            str = strs[0];
            try{
                if(str.equals(getString(R.string.scanningMode))){
                    scanningMode = ScanningModeType.valueOf(strs[1].trim().toUpperCase().replaceAll("\\s+","_"));
                } else if (str.equals(getString(R.string.modelWH))) {
                    modelWidth = GreatGridView.matchInt(strs[1], "(\\d+)\\D");
                    modelHeight = GreatGridView.matchInt(strs[1], "\\D(\\d+)");
                } else if (str.equals(getString(R.string.targetXY))) {
                    targetX = GreatGridView.matchInt(strs[1], "([0-9\\-]+)[^0-9\\-]");
                    targetY = GreatGridView.matchInt(strs[1], "[^0-9\\-]([0-9\\-]+)");
                } else if (str.equals(getString(R.string.targetWH))) {
                    targetW = GreatGridView.matchInt(strs[1], "(\\d+)\\D");
                    targetH = GreatGridView.matchInt(strs[1], "\\D(\\d+)");
                } else if (str.equals(getString(R.string.modelHash))) {
                    modelHash = Long.parseLong(strs[1], 16);
                } else if (str.equals(getString(R.string.blockSize))) {
                    blockSize = Integer.parseInt(strs[1]);
                } else if (str.equals(getString(R.string.C))) {
                    C = Float.parseFloat(strs[1]);
                } else if (str.equals(getString(R.string.hashSensitive))) {
                    hashSensitive = Integer.parseInt(strs[1]);
                } else if (str.equals(getString(R.string.minBlackValue))) {
                    minBlackValue = Float.parseFloat(strs[1]);
                }
            } catch (Exception e) {
                errorStr = e.getLocalizedMessage();
                return false;
            }
        }
        setTwoRect(cameraW, cameraH);
        return true;
    }

    /**
     * 存储全程序的设置。
     */
    private void setSp(){
        ed.putString("scanning mode", scanningMode.toString());
        ed.putInt("model height", modelHeight);
        ed.putInt("model width", modelWidth);
        ed.putInt("target W", targetW);
        ed.putInt("target H", targetH);
        ed.putInt("target X", targetX);
        ed.putInt("target Y", targetY);
        ed.putLong("model hash", modelHash);
        ed.putInt("block size", blockSize);
        ed.putFloat("C", C);
        ed.putInt("hash sensitive", hashSensitive);
        ed.putFloat("min black value", minBlackValue);
        ed.putString("img container features", imgContainerStd.features == null? "" : String.join("\n", imgContainerStd.features));
        ed.apply();
    }

//配置相关（结束）----------------------------------------------------------------------------------------------

    /**
     * 将图片放到img容器中并显示。
     * @param container 图片容器。
     * @param mat 要显示的对象。
     */
    private void showMat(IMGContainer container,ImageView imgview, Mat mat){
        Bitmap bm = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bm);
        //非主线程不能更新ui
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOpenCvCameraView.setVisibility(View.GONE);
                container.setVisibility(View.VISIBLE);
                imgview.setImageBitmap(bm);
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onStop(){
        setSp();//退出应用时，将数据存储起来。
        super.onStop();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        //在摄像头启动后，设置fps为20。
        //试过了camera2在这里无法设置fps，不能理解，camera没问题。
        mOpenCvCameraView.setPreviewFPS(20);
    }

    @Override
    public void onCameraViewStopped() {
    }

    /**
     * 用于获取包含所有轮廓的子轮廓个数的键值对。
     * @param hierarchy 从findContours方法获取的Mat，一般的Mat不能用在这里。
     * @return 键为轮廓id；值为该轮廓的子轮廓总个数。
     */
    private HashMap<Integer,Integer> getChildCountOfContours(Mat hierarchy){
        //hierarchy相当于多列一行的数组。
        //存储所有轮廓对应的父轮廓
        int length = hierarchy.cols();
        int[] parents = new int[length];
        for(int id = 0; id < length; id++){
            parents[id] = (int) hierarchy.get(0, id)[3];
        }
        //遍历所有轮廓，并上溯所有祖轮廓，每一代父轮廓都记录+1
        //最终得到的hashmap中，键为轮廓id，值为该轮廓的子孙轮廓数量合计。
        HashMap<Integer, Integer> relative = new HashMap<>();
        int pid = 0;
        int count = 0;
        for(int id = 0; id < length; id++){
            pid = parents[id];
            while(pid > -1) {
                if(null == relative.get(pid)){
                    relative.put(pid, 1);
                }else{
                    count = relative.get(pid) + 1;
                    relative.put(pid, count);
                }
                pid = parents[pid];
            }
        }
        return relative;
    }

    /**
     * 用于获取最大面积的轮廓。
     * @param contours 从findContours方法获取的所有轮廓数据。
     * @return 键为轮廓id；值为该轮廓的面积。
     */
    private HashMap<Integer, Double> getSofContours(List<MatOfPoint> contours){
        HashMap<Integer,Double> contourSs = new HashMap<>();
        double S = 0;
        for(int i = contours.size() - 1; i >= 0; i--){
            S = Imgproc.contourArea(contours.get(i));
            contourSs.put(i,S);
        }
        return contourSs;
    }

    /**
     * 给HashMap排序用。
     * @param hashmap 要排序的对象。
     * @return 降序后的list。get(0)为值最大的键值对。
     * @param <K> 键的类型。没有限制。
     * @param <V> 值的类型。必须为可比较的数字类型。
     */
    private static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> HashMapSort(HashMap<K, V> hashmap){
        List<Map.Entry<K, V>> list = new ArrayList<>(hashmap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        return list;
    }

    /**
     * 均值哈希法前置技能。其实就是个缩小8x8再二值化的过程。由于刚好64个1或0的像素，故最后存储为一个64位整数。
     * @param thresholdmat 要序列化的图片。
     * @return 代表该图片的一个hash值。用于对比含1量得知图片相似度。
     */
    private long aHash(Mat thresholdmat){
        Mat dst = new Mat();
        Imgproc.resize(thresholdmat, dst, new Size(8, 8), 0, 0, Imgproc.INTER_AREA);
        //求均值
        double average = Core.mean(thresholdmat).val[0];
        long hash = 0;
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                if(dst.get(row, col)[0] > average){
                    hash = (hash << 1) + 1;
                }else{
                    hash = (hash << 1);
                }
            }
        }
        dst.release();
        return hash;
    }

    /**
     * 猜测结果点位与给出轮廓四个角点的对应关系（不要拍太歪一般可以成功）
     * @param approxCurve 给出的轮廓。必须为四边形的四个角。
     * @return 结果四个角的点位。
     */
    private static int[] guessFourCorners(MatOfPoint2f approxCurve){
        //点位图：（建议拍照时，画面平行于拍摄对象，且尽量正视）
        //0,0——w,0
        // |    |
        //0,h——w,h
        //0——3
        //|  |
        //1——2
        //猜测结果点位与给出轮廓四个角点的对应关系（不要拍太歪一般可以成功）
        Point[] arr = approxCurve.toArray();
        double[] xs = {arr[0].x, arr[1].x, arr[2].x, arr[3].x};
        double[] ys = {arr[0].y, arr[1].y, arr[2].y, arr[3].y};
        //sort是升序
        Arrays.sort(xs);
        Arrays.sort(ys);
        int[] res = new int[4];
        for(int i = 0; i < 4; i++){
            if(Arrays.binarySearch(xs, arr[i].x) < 2){
                //该点的x坐标靠前，是方框的左边，可能是左上点或左下点
                if(Arrays.binarySearch(ys, arr[i].y) < 2){
                    //该点的y坐标靠上，是方框的上边，可能是左上点或右上点
                    //综上为左上点
                    res[i] = 0;
                }else{
                    res[i] = 1;
                }
            }else{
                if(Arrays.binarySearch(ys, arr[i].y) < 2){
                    res[i] = 3;
                }else{
                    res[i] = 2;
                }
            }
        }
        return res;
    }

    /**
     * 平面转换，截取相应位置并缩放到新尺寸
     * @param ori_mat
     * @param approxCurve
     * @param new_points
     * @param new_w
     * @param new_h
     * @return
     */
    private static Mat setYesAndCut(Mat ori_mat, MatOfPoint2f approxCurve, MatOfPoint2f new_points, int new_w, int new_h){
        //平面转换并缩放到新尺寸
        Mat m = Imgproc.getPerspectiveTransform(approxCurve, new_points);
        Mat res = new Mat();
        Imgproc.warpPerspective(ori_mat, res, m, new Size(new_w, new_h));
        //打完收工
        m.release();
        return res;
    }

    /**
     * 猜测四边形轮廓的各个角点对应的实际位置，将图片摆正并剪切到适合于容器的大小。
     * @param originMat 来自摄像头拍下来的一帧源图像，并且已经二值化。
     * @param approxCurve 已经扫描出来的四边形轮廓。
     * @return 摆正剪切后的图片。
     */
    private Mat setYesAndCutModel(Mat originMat, MatOfPoint2f approxCurve, int[] newPointsPosition){
        MatOfPoint2f new_points = twoRect.getFourCornerPointsOfModel(newPointsPosition);
        Mat res = setYesAndCut(originMat, approxCurve, new_points, twoRect.mw0, twoRect.mh0);
        new_points.release();
        return res;
    }

    private Mat setYesAndCutTarget(Mat originMat, MatOfPoint2f approxCurve, int[] newPointsPosition){
        //先按模板轮廓摆正，并截取twoRect范围中的内容
        MatOfPoint2f new_points = twoRect.getFourCornerPointsOfModelInTwoRect(newPointsPosition);
        Mat circum_rect = setYesAndCut(originMat, approxCurve, new_points, twoRect.cw, twoRect.ch);
        Mat target_rect = circum_rect.submat(twoRect.targetRect);
        Mat res = target_rect.clone();
        new_points.release();
        circum_rect.release();
        target_rect.release();
        return res;
    }

    /**
     * 在二值化图片中，扫描xys定义的网格，找出已经涂黑的格子。
     * @param mat 源图片。已经二值化过。
     * @param xys_list 要扫描的所有网格的左上角坐标。
     * @return 被涂黑的网格组成的数组。第一个网格序号为0，右边为1、2、3...等，换行继续。与GreatGridView.answers相同设定
     */
    private List<HashSet<Integer>> findOutBlackGrids(Mat mat, List<int[][][]> xys_list){
        int w, h, rows_count, cols_count, num, j;
        List<HashSet<Integer>> res = new ArrayList<>();
        for(int i = 0; i < xys_list.size(); i++){
            HashMap<Integer, Integer> hashMapBlackCount = new HashMap<>();//用于储存每个格子的黑点数（黑色像素总计数）。第一个整数为格子序号，第二个为黑点数。
            int[][][] xys = xys_list.get(i);
            rows_count = xys.length;
            //计算出网格的大小。
            w = xys[0][1][0] - xys[0][0][0];
            h = xys[1][0][1] - xys[0][0][1];
            num = 0;
            for(int r = 0; r < rows_count; r++){
                int[][] grids = xys[r];
                cols_count = grids.length;
                for(int c = 0; c < cols_count; c++){
                    //定义格子方框
                    Rect roi = new Rect(grids[c][0], grids[c][1], w, h);
                    //截取方框内容
                    Mat roi_mat = mat.submat(roi);
                    //计算黑色像素个数并存储
                    hashMapBlackCount.put(num, (int)roi_mat.total() - Core.countNonZero(roi_mat));
                    //更新正在扫描的格子序号
                    num++;
                    roi_mat.release();
                }
            }
            //这里使用自识别（即不借助空题照片，而是自行判断涂黑的格子）
            //尽管可以提前保存空的选择题图片进行对比识别，但这里还是做了自识别。
            //对比识别，要么使用base64存储空题照片，要么打开本地读写权限。由于没有做这个，就不思考有关方案了。
            //以下这套自识别的方法，我称之为“升序第3号起始首个较大邻差划分法”
            List<Map.Entry<Integer,Integer>> listBlackCount = HashMapSort(hashMapBlackCount);//降序
            Collections.reverse(listBlackCount);//升序
            //此时num刚好是最大的格子序号+1，即格子总数，或hashMap/list的长度
            //3号起始，忽略前三个格子。主要是防止考生用力擦除之后导致黑色像素少而误判。这个数字可以设计为答案网格自定义属性。
            //同理，邻差倍数也可以设计为自定义。此处设计为1.2
            for(j = 3; j < num; j++){
                if(listBlackCount.get(j).getValue() > listBlackCount.get(j - 1).getValue() * minBlackValue) {
                    break;
                }
            }
            //使用subList方法获取涂黑的格子
            List<Map.Entry<Integer, Integer>> subList = listBlackCount.subList(j, num);
            //提取出黑格子序号并成为一个HashSet
            HashSet<Integer> keysSet = new HashSet<>();
            for (Map.Entry<Integer, Integer> entry : subList) {
                keysSet.add(entry.getKey());
            }
            res.add(keysSet);
        }
        return res;
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat src = inputFrame.rgba();
        if(null != cs.user && dark.getVisibility() == View.VISIBLE){
            //当有用户连接时，发送屏幕内容（待实现）
            //cs.broadcast("hello man");
            //cs.broadcast();
            return src;
        }
        while(currentFrame >= 20){
            //每20帧即每秒扫描一次，降低资源消耗。
            currentFrame = 0;
            //当前帧画面二值化
            Mat gray = inputFrame.gray();
            Mat threshold_src = new Mat();
            Imgproc.adaptiveThreshold(gray,threshold_src,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,blockSize,C);
            gray.release();
            //找出所有轮廓
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(threshold_src,contours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
            if(contours.size() <= 1){
                //全黑屏时只有一个轮廓，这样写不至于闪退。
                threshold_src.release();
                hierarchy.release();
                for(MatOfPoint mop : contours){
                    mop.release();
                }
                contours.clear();
                break;
            }
            int target_id = 0;
            if(scanningMode == ScanningModeType.COUNT_CONTOURS){
                //数轮廓法：搜索包含子轮廓第二多的大轮廓。
                HashMap<Integer,Integer> lunkuos = getChildCountOfContours(hierarchy);
                List<Map.Entry<Integer,Integer>> list = HashMapSort(lunkuos);
                if(list.size() < 3){
                    threshold_src.release();
                    hierarchy.release();
                    for(MatOfPoint mop : contours){
                        mop.release();
                    }
                    contours.clear();
                    break;
                }
                target_id = list.get(1).getKey();
            }else if(scanningMode == ScanningModeType.CAL_S){
                //面积法：搜索面积第二大的轮廓。
                HashMap<Integer,Double> lunkuos = getSofContours(contours);
                List<Map.Entry<Integer,Double>> list = HashMapSort(lunkuos);
                if(list.size() < 3){
                    threshold_src.release();
                    hierarchy.release();
                    for(MatOfPoint mop : contours){
                        mop.release();
                    }
                    contours.clear();
                    break;
                }
                target_id = list.get(1).getKey();
            }
            if(contour2 != null){
                contour2.release();
            }
            contour2 = contours.get(target_id);
            //对目标轮廓拟合四边形
            double C = 0.0d;
            MatOfPoint2f contour2f = new MatOfPoint2f(contour2.toArray());
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double p = 0.02;
            do{
                C = Imgproc.arcLength(contour2f,true) * p;
                approxCurve.release();
                approxCurve = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f,approxCurve,C,true);
                p *= 1.5;
            }while(approxCurve.toArray().length > 4);
            contour2 = new MatOfPoint(approxCurve.toArray());
            if(approxCurve.toArray().length == 4){
                System.out.println("开始扫描...");
                //如果拟合出四边形了，就尝试扫描答案
                //如果按下了拍照按钮，这里将其记录为模板。
                if(takePhotoMode == TakePhotoModeType.TAKE_MODEL && btnPressed4TakePhoto){
                    //将按钮状态重置为未按下。
                    btnPressed4TakePhoto = false;
                    //对应已扫描的轮廓四角到即将摆正的模板图四角。
                    int[] newPointsPos = guessFourCorners(approxCurve);
                    //截取模板图。
                    Mat modelmat = setYesAndCutModel(threshold_src, approxCurve, newPointsPos);
                    //记录模板图hash
                    modelHash = aHash(modelmat);
                    //重新截取目标图（答案所在区域）
                    Mat targetmat = setYesAndCutTarget(threshold_src, approxCurve, newPointsPos);
                    //显示答案区域
                    showMat(imgContainerStd, imgModel, targetmat);
                    //打完收工
                    modelmat.release();
                    targetmat.release();
                }else if((takePhotoMode == TakePhotoModeType.AUTO_TAKE || (takePhotoMode == TakePhotoModeType.MANUAL_TAKE && btnPressed4TakePhoto)) && modelHash != 0){
                    //如果正在扫描，就与拍下的模板比较，得出相似度
                    //重置按钮状态。
                    btnPressed4TakePhoto = false;
                    //对应已扫描的轮廓四角到即将摆正的模板图四角。
                    int[] newPointsPos = guessFourCorners(approxCurve);
                    //截取模板图。
                    Mat modelmat = setYesAndCutModel(threshold_src, approxCurve, newPointsPos);
                    //比较模板图hash
                    long hash = aHash(modelmat) ^ modelHash;
                    //计算汉明距离
                    int hamming_distance = Long.bitCount(hash);
                    //比较汉明距离与相似图容忍度。
                    if(hamming_distance < hashSensitive){
                        //证明两个图片属于相似范围内
                        //重新截取目标图（答案所在区域）
                        Mat targetmat = setYesAndCutTarget(threshold_src, approxCurve, newPointsPos);
                        //显示答案区域
                        showMat(imgContainerTestee, imgTestee, targetmat);
                        scoreText.setText("总分为：" + imgContainerTestee.addGreatGrids(imgContainerStd.features, findOutBlackGrids(targetmat, imgContainerStd.ansXYs)) + "分");
                        targetmat.release();
                    }
                    modelmat.release();
                }
            }
            threshold_src.release();
            hierarchy.release();
            for(MatOfPoint mop : contours){
                mop.release();
            }
            contours.clear();
            contour2f.release();
            approxCurve.release();
        }
        if(contour2 != null){
            List<MatOfPoint> contours2 = new ArrayList<>();
            contours2.add(contour2);
            Imgproc.drawContours(src,contours2,0,new Scalar(0,255,0),12);
        }
        currentFrame++;
        return src;
    }

}



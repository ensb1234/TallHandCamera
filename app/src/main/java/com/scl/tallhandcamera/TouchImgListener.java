/**
 * 作者：涉川良
 * 联系方式：QQ470707134
 */
package com.scl.tallhandcamera;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class TouchImgListener implements View.OnTouchListener{
    private float originX;//记录触摸的起始坐标
    private float originY;
    private GreatGridView[] ggs = new GreatGridView[10];//限制最多10个答案网格
    private int drawingNum = -1;//正在绘制的答案网格
    private IMGContainer imgContainer;
    //实际上可以不限制数量，只记录当前是否正在绘制即可。但还是保留了这个办法。
    @Override
    public boolean onTouch(View v, MotionEvent event){
        if(imgContainer == null){
            imgContainer = (IMGContainer) v;
        }
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                originX = event.getX();//记录起始触摸的坐标
                originY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(drawingNum == -1){
                    //检查限制数量的子答案视图是否还有剩余的位置。如果有，开始绘制。
                    for(int i = 0; i < 10; i++){
                        if(ggs[i] == null){
                            if(drawingNum == -1){
                                //记录正在绘制的子视图编号。
                                drawingNum = i;
                                //一开始绘制时，须先创建答案网格实例。
                                GreatGridView gg = new GreatGridView(imgContainer.getContext(), imgContainer.getId());
                                //并按滑动到的坐标进行绘制。
                                gg.layout((int) originX, (int) originY, (int) event.getX(), (int) event.getY());
                                //添加到容器。
                                imgContainer.addView(gg);
                                //记录到子视图数组。
                                ggs[i] = gg;
                            }
                        } else if (ggs[i].removed) {
                            //从子视图数组中，移除已经标记删除了的答案网格
                            ggs[i] = null;
                        }
                    }
                }else{
                    ggs[drawingNum].layout((int) originX, (int) originY, (int) event.getX(), (int) event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                if(drawingNum <= -1 || drawingNum >= 10){
                    break;
                }
                //如果不足180宽高，则自动重置到180
                float w = event.getX() - originX;
                float h = event.getY() - originY;
                if(Math.abs(w) < 180){
                    w = Math.signum(w) * 180;
                }
                if(Math.abs(h) < 180){
                    h = Math.signum(h) * 180;
                }
                if(w == 0) w = 180;
                if(h == 0) h = 180;
                ggs[drawingNum].layout((int) originX, (int) originY, (int) (originX + w), (int) (originY + h));
                //触摸结束，记录手指离开屏幕的状态。
                drawingNum = -1;
                break;
        }
        return true;
    }
}
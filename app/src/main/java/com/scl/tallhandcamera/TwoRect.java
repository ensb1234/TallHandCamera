/**
 * 作者：涉川良
 * 联系方式：QQ470707134
 */
package com.scl.tallhandcamera;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.Arrays;

public class TwoRect {

    //点位图：
    //0,0——w,0
    // |    |
    //0,h——w,h
    //0——3
    //|  |
    //1——2
    /**
     * 这个数组用于记录模板图矩形的四个角坐标。<br/>
     * 四个角的点位图如下：<br/>
     * 0,0——w,0<br/>
     * 0,h——w,h<br/>
     * 四个角对应的数组元素下标：<br/>
     * 0——3<br/>
     * 1——2<br/>
     */
    public Point[] fourCornerPointsOfModel = new Point[4];
    public Point[] fourCornerPointsOfModelInTwoRect = new Point[4];
    public int mx;
    public int my;
    public int mh;
    public int mw;
    public int mh0;
    public int mw0;
    public int tx;
    public int ty;
    public int th;
    public int tw;
    public Rect targetRect;
    public int cx;
    public int cy;
    public int ch;
    public int cw;

    public TwoRect(int container_w, int container_h, int model_w, int model_h, int target_x, int target_y, int target_w, int target_h){
        //找出包含两个矩形的最小外接矩形。
        int[] xs = {0, model_w, target_x, target_x + target_w};
        int[] ys = {0, model_h, target_y, target_y + target_h};
        Arrays.sort(xs);
        Arrays.sort(ys);
        cx = xs[0];
        cy = ys[0];
        cw = xs[3] - cx;
        ch = ys[3] - cy;
        //计算出外接矩形缩放后在容器中的最大尺寸
        AdaptedRect ar = getAdaptedWH(container_w, container_h, cw, ch);
        cw = ar.w;
        ch = ar.h;
        int ratio = ar.ratio;
        //将包含两个小矩形的外接矩形左上角移动到坐标轴0点处，计算出两个小矩形的坐标及尺寸，以及外接矩形的尺寸。
        mx = -cx * ratio;
        my = -cy * ratio;
        tx = (target_x - cx) * ratio;
        ty = (target_y - cy) * ratio;
        cx = 0;
        cy = 0;
        mw = model_w * ratio;
        mh = model_h * ratio;
        tw = target_w * ratio;
        th = target_h * ratio;
        targetRect = new Rect(tx, ty, tw, th);
        fourCornerPointsOfModelInTwoRect[0] = new Point(mx, my);
        fourCornerPointsOfModelInTwoRect[1] = new Point(mx, my + mh);
        fourCornerPointsOfModelInTwoRect[2] = new Point(mx + mw, my + mh);
        fourCornerPointsOfModelInTwoRect[3] = new Point(mx + mw, my);
        //计算出模板图单独占据容器时的尺寸
        ar = getAdaptedWH(container_w, container_h, model_w, model_h);
        mw0 = ar.w;
        mh0 = ar.h;
        fourCornerPointsOfModel[0] = new Point(0, 0);
        fourCornerPointsOfModel[1] = new Point(0, mh0);
        fourCornerPointsOfModel[2] = new Point(mw0, mh0);
        fourCornerPointsOfModel[3] = new Point(mw0, 0);
    }

    public MatOfPoint2f getFourCornerPointsOfModel(int[] order){
        Point[] res = new Point[]{
                fourCornerPointsOfModel[order[0]],
                fourCornerPointsOfModel[order[1]],
                fourCornerPointsOfModel[order[2]],
                fourCornerPointsOfModel[order[3]]
        };
        return new MatOfPoint2f(res);
    }

    public MatOfPoint2f getFourCornerPointsOfModelInTwoRect(int[] order){
        Point[] res = new Point[]{
                fourCornerPointsOfModelInTwoRect[order[0]],
                fourCornerPointsOfModelInTwoRect[order[1]],
                fourCornerPointsOfModelInTwoRect[order[2]],
                fourCornerPointsOfModelInTwoRect[order[3]]
        };
        return new MatOfPoint2f(res);
    }

    public static AdaptedRect getAdaptedWH(int container_w, int container_h, int origin_w, int origin_h){
        int res_w, res_h, ratio;
        res_w = container_h * origin_w / origin_h;
        res_h = container_h;
        if(res_w > container_w){
            res_h = container_w * origin_h / origin_w;
            res_w = container_w;
        }
        ratio = res_w / origin_w;
        return new AdaptedRect(res_w, res_h, ratio);
    }

    public static class AdaptedRect{
        public int w;
        public int h;
        public int ratio;
        public AdaptedRect(int new_w, int new_h, int new_ratio){
            w = new_w;
            h = new_h;
            ratio = new_ratio;
        }
    }
}

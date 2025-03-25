/**
 * 作者：涉川良
 * 联系方式：QQ470707134
 */
package com.scl.tallhandcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class IMGContainer extends ConstraintLayout {
    public List<String> features = new ArrayList<>();
    public Boolean isAnswers = false;
    public List<int[][][]> ansXYs = new ArrayList<>();

    public IMGContainer(@NonNull Context context) {
        super(context);
    }

    public IMGContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 用于imgContainer2，即扫描出考生答案时，将标准答案和被扫描出来的涂黑格子数组传入本函数，计算总分并返回。
     * @param standard_features 标准答案，即从imgContainer中获得的特征码
     * @param testee_answers_list 考生答案，是从图上计算得来的涂黑格子数组
     * @return 返回总分，包括所有答案网格的计算总分
     */
    public int addGreatGrids(List<String> standard_features, List<HashSet<Integer>> testee_answers_list){
        int ggcount = standard_features.size();
        if(ggcount != testee_answers_list.size()){
            return 0;
        }
        int res = 0;
        for (int i = 0; i < ggcount; i++) {
            GreatGridView gg = new GreatGridView(getContext(), getId(), standard_features.get(i));
            gg.Convert2TesteeMode(testee_answers_list.get(i));
            addView(gg);
            res += gg.getTotalScore();
        }
        return res;
    }

    /**
     * 用于在xml创建的imgContainer初始化。
     * @param features_str 存储于应用记忆中的所有答案网格特征码
     */
    public void setFeatures(String features_str){
        features = Arrays.asList(features_str.split("\\r?\\n|\\r"));
        ansXYs = new ArrayList<>();
        for(int i = 0; i < features.size(); i++){
            ansXYs.add(GreatGridView.getXYPoints(features.get(i)));
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if(isAnswers){
            //如果是储存标准答案的话，要在答案网格消失前生成所有答案网格的特征码、坐标点数组，随后删除所有答案网格
            //重新出现答案网格时，按特征码重新绘制答案网格。
            if(visibility == GONE){
                //消失时应生成特征码，并删除所有子视图。
                features = new ArrayList<>();
                int childCount = getChildCount();
                List<View> children_will_remove = new ArrayList<>();
                ansXYs = new ArrayList<>();
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child instanceof GreatGridView) {
                        features.add(((GreatGridView) child).toFeatureCode());
                        ansXYs.add(((GreatGridView) child).getXYPoints());
                        children_will_remove.add(child);
                        //removeView(childView);//会导致一边移除一边子视图数组变小，后续读取出错
                    }
                }
                for(View child : children_will_remove){
                    removeView(child);
                }
            } else if (visibility == VISIBLE) {
                //重现时，应该按照特征码绘制
                for (String feature : features) {
                    GreatGridView gg = new GreatGridView(getContext(), getId(), feature);
                    addView(gg);
                }
            }
        }else{
            //如果是储存考生答案的话，在答案网格消失前，删除所有答案网格，重新出现时则不需要执行任务。
            if(visibility == GONE){
                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child instanceof GreatGridView) {
                        removeView(child);
                    }
                }
            }
        }
        super.setVisibility(visibility);
    }
}

/**
 * 作者：涉川良
 * 联系方式：QQ470707134
 */
package com.scl.tallhandcamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GreatGridView extends View implements View.OnTouchListener {
    private Paint paintLineGrids = new Paint();//网格画笔
    private Paint paintFillAns = new Paint();//答案填充
    private Paint paintLineBorder = new Paint();//外框
    private Paint paintFillInside = new Paint();//默认底色填充
    private Paint paintLineTestee = new Paint();//考生答案填充
    public int rowsCount = 4;//横屏行数，默认为3
    public int colsCount = 3;//横屏列数，默认为4

    ///答案数组。记录的是答案所在格子的序号。横屏左上角第一个网格序号为0，其右边为2、3、4...换行继续，依此类推。
    public HashSet<Integer> answers = new HashSet<>(Arrays.asList(9, 7, 5, 2));

    public HashSet<Integer> testeeAnswers;
    private int testeeMode = 0;
    ///答案使用的字符，当需要翻译纸面答案时会用到它。
    private static final String ANSWERS_26 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    ///答案是否为横屏识别？默认竖屏。
    private boolean landScape = false;

    ///表示给分设定的字符串。
    private String scoreSetsStr = "2:2+2x,3:4-2x";
    private int[][] scoreSets;//记得在构造函数中先初始化

    ///九个图标大小
    private static final int ICON_DIAMETER = 60;

    //以下都是图标们
    private Drawable da_add;
    private Drawable da_sub;
    private Drawable da_mul;
    private Drawable da_move;
    private Drawable da_ud;
    private Drawable da_lr;
    private Drawable da_rd;
    private Drawable da_switch;
    private Drawable da_l;
    private Drawable da_r;
    private Drawable da_u;
    private Drawable da_d;
    private Drawable da_l1;
    private Drawable da_r1;
    private Drawable da_u1;
    private Drawable da_d1;
    private Drawable da_zheng;
    private Drawable da_zheng1;

    ///粗调图标数组
    private Drawable[] iconsCoarseTuning;

    ///微调图标数组
    private Drawable[] iconsFineTuning;

    ///含9个图标的left值、top值、right值、bottom值的二维数组
    private int[][] iconsPositions = new int[9][4];
    public enum ModeType{
        COARSE_TUNING,
        FINE_TUNING,
        EDIT
    }//粗调，微调，编辑模式
    public ModeType currentMode = ModeType.EDIT;//这个视图目前的功能模式。默认为编辑（即没有图标的模式）
    private boolean fineTuningMove = false;//微调模式下，是否为四向微移模式？默认否。
    private float startingTouchX;//手指触摸该视图时，触摸起点的x坐标。
    private float startingTouchY;//同上y坐标
    private int originLeft;//手指触摸该视图时，最初的左边距。
    private int originRight;
    private int originTop;
    private int originBottom;//以上类推
    private int maxRight;//最右
    private int maxBottom;//最底
    private int pressingBtn = -1;//0-8粗调；10-18微调；9编辑
    public boolean removed = false;//点击x关闭后置true，后将移除。
    private ConstraintLayout.LayoutParams layoutParams;//
    public void init(){
        //绕原点逆旋
        //setPivotX(0f);
        //setPivotY(0f);
        //setRotation(-90f);
        //线条画笔
        paintLineGrids.setStyle(Paint.Style.STROKE);
        paintLineGrids.setColor(0xa00068b7);
        paintLineGrids.setStrokeWidth(5.0f);
        //答案填充
        paintFillAns.setStyle(Paint.Style.FILL);
        paintFillAns.setColor(0x600068b7);
        //外框线条
        paintLineBorder.setStyle(Paint.Style.STROKE);
        paintLineBorder.setColor(0xa00068b7);
        paintLineBorder.setStrokeWidth(10.0f);
        //默认填充
        paintFillInside.setStyle(Paint.Style.FILL);
        paintFillInside.setColor(0x400068b7);
        //考生答案格子填充
        paintLineTestee.setStyle(Paint.Style.STROKE);
        paintLineTestee.setColor(0xffff0000);
        paintLineTestee.setStrokeWidth(5.0f);
        //图标资源引用
        da_add = ContextCompat.getDrawable(getContext(), R.drawable.add);
        da_sub = ContextCompat.getDrawable(getContext(), R.drawable.sub);
        da_mul = ContextCompat.getDrawable(getContext(), R.drawable.mul);
        da_move = ContextCompat.getDrawable(getContext(), R.drawable.move);
        da_ud = ContextCompat.getDrawable(getContext(), R.drawable.ud);
        da_lr = ContextCompat.getDrawable(getContext(), R.drawable.lr);
        da_rd = ContextCompat.getDrawable(getContext(), R.drawable.rd);
        da_switch = ContextCompat.getDrawable(getContext(), R.drawable.resource_switch);
        da_l = ContextCompat.getDrawable(getContext(), R.drawable.l);
        da_r = ContextCompat.getDrawable(getContext(), R.drawable.r);
        da_u = ContextCompat.getDrawable(getContext(), R.drawable.u);
        da_d = ContextCompat.getDrawable(getContext(), R.drawable.d);
        da_l1 = ContextCompat.getDrawable(getContext(), R.drawable.l1);
        da_r1 = ContextCompat.getDrawable(getContext(), R.drawable.r1);
        da_u1 = ContextCompat.getDrawable(getContext(), R.drawable.u1);
        da_d1 = ContextCompat.getDrawable(getContext(), R.drawable.d1);
        da_zheng = ContextCompat.getDrawable(getContext(), R.drawable.zheng);
        da_zheng1 = ContextCompat.getDrawable(getContext(), R.drawable.zheng1);
        iconsCoarseTuning = new Drawable[]{da_switch, da_sub, da_add, da_sub, da_move, da_lr, da_add, da_ud, da_rd};
        iconsFineTuning = new Drawable[]{da_switch, da_u, da_l1, da_l, da_move, da_r1, da_u1, da_d1, landScape?da_zheng:da_zheng1};
        setOnTouchListener(this);
    }

    ///该构造函数在new时调用。这个对应的是手指从头开始绘制该视图。
    public GreatGridView(Context context, int container_id) {
        super(context);

        init();
        layoutParams = new ConstraintLayout.LayoutParams(180, 180);
        layoutParams.leftToLeft = container_id;
        layoutParams.topToTop = container_id;
        setLayoutParams(layoutParams);
        setScoreSets(rowsCount);
    }

    /**
     * 按给出的含一个捕获组的正则表达式，输出给定字符串中符合要求的数字。
     * @param str 源字符串。
     * @param p_str 正则表达式。
     * @return 符合要求的数字。
     */
    public static int matchInt(String str, String p_str){
        Pattern pattern = Pattern.compile(p_str);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    /**
     * 按给出的含一个捕获组的正则表达式，输出给定字符串中符合要求的字符串。
     * @param str 源字符串。
     * @param p_str 正则表达式。
     * @return 符合要求的字符串。
     */
    public static String matchStr(String str, String p_str){
        Pattern pattern = Pattern.compile(p_str);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * 检视客户的给分设定是否符合要求，并按标准语法进行矫正、升序排序、补全所有题目的给分设定。
     * @param score_sets_str 客户写进来的给分设定字符串。
     * @return 标准化的设定字符串。
     */
    private String checkScoreSets(@NonNull String score_sets_str){
        String[] score_sets = score_sets_str.split(",");
        int question_num_max = landScape ? rowsCount : colsCount;
        String[] res = new String[question_num_max];//res是假定每一题都有各自的设定，后面方便排序简化
        int question_num;
        String tmp;
        for(String score_set : score_sets){
            question_num = matchInt(score_set, "(\\d+):");
            if(question_num <= 0 || question_num > question_num_max){
                //说明没有题号或超过最大题号，直接摒弃该项
                continue;
            }
            //判断冒号后面是否符合格式，若合格，则存入res数组中
            do{
                //完整格式
                tmp = matchStr(score_set, ":(\\d+[+\\-]\\d+[ox])");
                if(tmp != ""){
                    res[question_num - 1] = question_num + ":" + tmp;
                    break;
                }
                //缺少最后的【ox】,标准格式
                tmp = matchStr(score_set, ":(\\d+[+\\-]\\d+)");
                if(tmp != ""){
                    res[question_num - 1] = question_num + ":" + tmp;
                    break;
                }
                //短格式，意味着必须全部与标准答案相同才可得分。
                tmp = matchStr(score_set, ":(\\d+)");
                if(tmp != ""){
                    res[question_num - 1] = question_num + ":" + tmp;
                    break;
                }
            } while (false);
        }
        //简化res
        tmp = String.join(",",res);
        tmp = tmp.replaceAll("null,","");
        //如果找不到最大题号，就补充最大题号的默认给分设定
        //借用question_num
        question_num = tmp.indexOf(",null");
        if(question_num > -1 && question_num == tmp.length() - 5){
            tmp = tmp.substring(0, question_num + 1) + question_num_max + ":2+2x";
        }
        return tmp;
    }

    /**
     * 用于及时地设置scoreSets的内容。仅应用于构造函数初始化。
     * @param r 行数，即题数
     */
    private void setScoreSets(int r){
        //scoreSets的格式应该是，先填充由scoreSetsStr设置的对应题号设定，
        //然后倒序上溯，把某题号之前null的设定一律填充该题号的设定，
        //当遇到下一个题号已有非null的设定时，再更换新设定。
        scoreSets = new int[r][3];
        String[] sss = scoreSetsStr.split(",");
        for(int i = sss.length - 1; i >= 0 ; i--){
            scoreSets[matchInt(sss[i], "(\\d+):") - 1] = analyseScoreSet(matchStr(sss[i], ":(.+)"));
        }
        int[] tmp = scoreSets[scoreSets.length - 1];
        //在checkScoreSets时，已经保证了最末一题一定会有设定
        for(int i = scoreSets.length - 1; i >= 0; i--){
            if(scoreSets[i][0] == 0){
                scoreSets[i] = tmp;
            }else{
                tmp = scoreSets[i];
            }
        }
    }

    /**
     * 分析scoreSet内容。获得某个题序的详细给分设定。
     * @param score_set 从scoreSets中获取的某个题序对应的给分设定。
     * @return 细化、可用于给分的设定。
     */
    private int[] analyseScoreSet(String score_set){
        int big_score,bit_score;
        int take0 = 0;//错题是否全扣？默认全扣
        big_score = matchInt(score_set, "(\\d+)[+-]");
        if(big_score == 0){
            //短模式
            big_score = Integer.parseInt(score_set);
            bit_score = big_score;
            return new int[]{big_score, bit_score, take0};
        }
        //完整或标准模式
        bit_score = matchInt(score_set, "([+\\-]\\d+)");
        take0 = score_set.charAt(score_set.length() - 1) == 'o' ? 1 : 0;
        return new int[]{big_score, bit_score, take0};
    }

    private HashSet<Integer> strs2answers(String[] strs){
        HashSet<Integer> res = new HashSet<>();
        String str;
        if(landScape){
            //横屏
            for(int i = 0; i < strs.length; i++){
                str = strs[i];
                for(int j = str.length() - 1; j >= 0; j--){
                    //横屏模式下，i即题号等于行号，故存储的格子序号应该等于行号乘以列数（即每行格子数）再加列号（即选项转换的数字）
                    res.add(i * colsCount + ANSWERS_26.indexOf(str.charAt(j)));
                }
            }
        }else{
            //竖屏（在前面的横屏条件下，顺时针转动九十度获得的竖屏，此时前置摄像头在屏幕上侧）
            int tmp;
            for(int i = 0; i < strs.length; i++){
                str = strs[i];
                for(int j = str.length() - 1; j >= 0; j--){
                    //竖屏模式下，i即题号等于横屏列号，格子序号计算公式如下
                    tmp = rowsCount - ANSWERS_26.indexOf(str.charAt(j)) - 1;
                    if(tmp < 0){
                        continue;
                    }
                    res.add(tmp * colsCount + i);
                }
            }
        }
        return res;
    }

    ///该构造函数在new时调用。这个对应的是已有特征码的情况下，自动绘制本视图。
    public GreatGridView(Context context, int container_id, String feature){
        super(context);

        String[] strings = feature.split("_");
        if(strings.length != 5){
            strings = ("x0y0_h180w180_r3c4_P:A,B,CD_2:2+2x,3:4-2x").split("_");
        }
        //首先分析xy坐标（位置）
        int x = matchInt(strings[0], "x(\\d+)");
        int y = matchInt(strings[0], "y(\\d+)");
        if(x < 0) x = 0;
        if(y < 0) y = 0;
        //然后分析hw高和宽（尺寸）
        int h = matchInt(strings[1], "h(\\d+)");
        int w = matchInt(strings[1], "w(\\d+)");
        if(h < 180) h = 180;
        if(w < 180) w = 180;
        //应用以上指示的位置和尺寸到格式中。
        layoutParams = new ConstraintLayout.LayoutParams(w, h);
        layoutParams.leftToLeft = container_id;
        layoutParams.topToTop = container_id;
        setLayoutParams(layoutParams);
        layout(x, y, x+w, y+h);
        //再而是rc行列数
        rowsCount = matchInt(strings[2], "r(\\d+)");
        colsCount = matchInt(strings[2], "c(\\d+)");
        if(rowsCount < 3 || rowsCount > 26) rowsCount = 1;
        if(colsCount < 3 || colsCount > 26) colsCount = 1;
        //接着是各题答案，半角冒号前是横屏L竖屏P，冒号后每题答案用半角逗号隔开
        landScape = (strings[3].charAt(0) == 'L');
        //读取出由每题的答案组成的数组
        String[] questions_answers = matchStr(strings[3], ":(.+)").split(",");
        String str;//temp
        //将答案（ABCD...）转化为数字（格子序号）
        answers = strs2answers(questions_answers);
        //最后是各题给分设定
        scoreSetsStr = checkScoreSets(strings[4]);
        setScoreSets(landScape?rowsCount:colsCount);//根据scoreSetsStr，设置scoreSets

        init();
    }

    ///该构造函数在xml中定义时调用。仅初期调试时使用。
    public GreatGridView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    @Override
    public void layout(int l, int t, int r, int b){
        //满足从起始点开始往任意方向拖动，都能绘制出来（由于设置了最小尺寸，因而这个应该只能在初添加到父视图时执行有效）
        int w = r - l;
        int h = b - t;
        if(w < 0){
            //虽然可以实现翻转，但按钮功能要改的代码似乎不少，暂时不考虑，又不是不能用
            //setScaleX(-1);
            w = -w;
            r = l;
            l -= w;
        }
        if(h < 0){
            //setScaleY(-1);
            h = -h;
            b = t;
            t -= h;
        }
        //父类方法
        super.layout(l, t, r, b);
        //布局参数同步设置
        //父视图新添加另一个子视图时，父视图会根据现在这个视图的布局参数对其进行重绘
        //如果不修改这个，那在新添加视图时，这个视图尺寸会出错
        layoutParams.leftMargin = l;
        layoutParams.topMargin = t;
        layoutParams.width = w;
        layoutParams.height = h;
        //如果不set，那在父容器gone之后visible时，父容器中动态添加的第二个及以后的视图尺寸会出错（原因不明）
        //这个尺寸竟然记忆了刚画上去的时候最小的那个尺寸
        //例如，在添加第二个GreatGridView时，拖动大小，首先是4宽0高，
        //然后父容器会立刻确认一次第一个视图的尺寸，比如600宽550高这样，
        //接下来就是第二个视图变成4宽1080高，
        //然后才是正常的从4宽6高一直过渡到比如600宽500高这样，
        //但是父容器调整可视度之后，重现时，第一个子视图可以正常显示为600*550，
        //而第二个视图就不行了，显示的不是600*500，而是4*1080
        //这个可以在随后的System.out中获取这个调试信息以进行研究
        setLayoutParams(layoutParams);
        //有了上面这句就不会出现那个奇怪的错误了
        System.out.println("drawing GreatGridView width: " + layoutParams.width + " height: " + layoutParams.height);
    }

    /**
     * 将 由格子序号组成的所有题目答案 转化为字面可见的字符串数组。
     * @param answer_nums 可能是标准答案，也可能是客户给出的考生答案
     * @return 字面可见的字符串答案数组。每一题对应一个数组元素。
     */
    private String[] answers2Strs(HashSet<Integer> answer_nums){
        int ques_count = landScape?rowsCount:colsCount;
        String[] answer_strs = new String[ques_count];
        for(int i = 0; i < ques_count; i++){
            answer_strs[i] = "";
        }
        if(landScape){
            for(int i : answer_nums){
                answer_strs[(int)(i/colsCount)] += ANSWERS_26.charAt(i%colsCount);
            }
        }else{
            int new_num;
            for(int i : answer_nums){
                new_num = rowsCount - (int)(i/colsCount) - 1 + (i%colsCount) * rowsCount;
                answer_strs[(int)(new_num/rowsCount)] += ANSWERS_26.charAt(new_num%rowsCount);
            }
        }
        return answer_strs;
    }

    /**
     * 生成对应特征码。一般是父容器在隐藏时，调用所有这种子视图的这个函数。<br/>
     * 《特征码说明书》<br/>
     * xy坐标，代表该答案表格的位置<br/>
     * hw高宽，代表该答案表格的大小尺寸<br/>
     * rc行列，代表该答案表格的行数列数<br/>
     * LP横竖屏，代表该答案是匹配横屏或竖屏，L横屏，P竖屏<br/>
     * 答案，用字母A到Z表示。答案表格每行第一个格子对应字母A，第二个格子对应B，依此类推。每题答案之间用半角逗号隔开。<br/>
     * 分数设定，格式为：<br/>
     * 【题序】:【总分】【+-】【s】【ox】<br/>
     * 首先是【题序】，代表这个设定到哪一题结束（包括这一题），首题题序为1，后面依此类推。<br/>
     * 因此，分数设定可以有多个，不同设定的题序应该不同。如果相同，后面的设定会覆盖前面的设定。<br/>
     * 第一个设定的范围是从第一题开始，到这个设定的题序结束。<br/>
     * 第二个及以后的设定中，该设定的范围是从上一个题序的下一题开始，到这个设定的题序结束。<br/>
     * 不同的设定之间用半角逗号隔开。<br/>
     * 【总分】代表该题总分。本题扣分值最大不能超过这个数字。<br/>
     * 当本题为单选时，【总分】之后的符号可以省略。<br/>
     * 【+-】可能为+或-；【s】是一个数字，代表给分或扣分。<br/>
     * 【+-】连同【s】代表逐个答案的给分细则。<br/>
     * 如果是+s，代表“对一得s”；<br/>
     * 如果是-s，代表“漏一扣s”。<br/>
     * 【ox】可能为o或x（小写字母）；<br/>
     * 其中o代表“错一无视”，x代表“错一全扣”。省略这一项时，默认为错一全扣。<br/>
     * @return 特征码。可以根据特征码重绘该视图。<br/>
     */
    public String toFeatureCode(){
        String[] answer_strs = answers2Strs(answers);

        if(scoreSetsStr.isEmpty()){
            scoreSetsStr = (landScape?rowsCount:colsCount) + ":2+2x";
        }
        @SuppressLint("DefaultLocale") String res = String.format("x%dy%d_h%dw%d_r%dc%d_%s:%s_%s",
                (int)getX(),(int)getY(),getHeight(),getWidth(),rowsCount,colsCount,
                landScape ? "L":"P",String.join(",", answer_strs), scoreSetsStr);
        return res;
    }

    /**
     * 针对某一题进行给分。
     * @param big_score
     * @param bit_score
     * @param take0
     * @param bingo_count
     * @param wrong_count
     * @param missing_count
     * @return
     */
    private int getScore(int big_score, int bit_score, int take0, int bingo_count, int wrong_count, int missing_count){
        int res = 0;
        if(bit_score > 0){
            //小题分是正数，即对一得bit_score
            res = bingo_count * bit_score;
            if(res > big_score){
                //这不可能发生！
                res = big_score;
            }
        }else{
            //小题分为负数（0虽然包括在内但无影响），即错一扣bit_score
            res = big_score + (missing_count * bit_score);
            if(res < 0){
                //可能发生
                res = 0;
            }
        }
        if(wrong_count > 0){
            res *= take0;
        }
        return res;
    }

    /**
     * 获取这个网格视图的得分。这个方法合理只用于显示考生答案时，与传进来的标准答案对比。
     * @return 计算结果。
     */
    public int getTotalScore(){
        int score = 0;
        //首先转换考生答案和标准答案为字符串数组模式。
        int ques_count = landScape?rowsCount:colsCount;
        //【注意】该方法在使用时，这个网格并非保存标准答案，而是保存了考生答案。
        String[] testee_answer_strs = answers2Strs(testeeAnswers);
        String[] standard_answer_strs = answers2Strs(answers);
        String sta,tes;
        int bingo_count,wrong_count,missing_count;
        for(int i = 0; i < ques_count; i++){
            sta = standard_answer_strs[i];
            tes = testee_answer_strs[i];
            bingo_count = wrong_count = missing_count = 0;
            for(int j = tes.length() - 1; j >= 0; j--){
                if(sta.indexOf(tes.charAt(j)) > -1){
                    //在标准答案中发现考生的选择。说明考生选对了。
                    bingo_count++;
                }else{
                    //找不到考生的选择，说明考生的选择错误。
                    wrong_count++;
                }
            }
            for(int j = sta.length() - 1; j >= 0; j--){
                if(tes.indexOf(sta.charAt(j)) == -1){
                    //考生的答案中，找不到标准答案的选择，说明考生漏选。
                    missing_count++;
                }
            }
            //计算出正确、错误、漏选个数之后，对照给分设定进行给分。
            score += getScore(scoreSets[i][0], scoreSets[i][1], scoreSets[i][2], bingo_count, wrong_count, missing_count);
        }
        //按照不同的分值设定，逐题对比计算得分。
        return score;
    }

    public void Convert2TesteeMode(HashSet<Integer> testee_answers){
        testeeAnswers = testee_answers;
        testeeMode = 1;
    }

    /**
     * 移动该view
     * @param dx 移动的x轴距离
     * @param dy 移动的y轴距离
     */
    private void move(int dx, int dy){
        int nl = getLeft() + dx;
        int nr = getRight() + dx;
        int nt = getTop() + dy;
        int nb = getBottom() +dy;
        if(nl < 0 || nr > maxRight){
            nl -= dx;
            nr -= dx;
        }
        if(nt < 0 || nb > maxBottom){
            nt -= dy;
            nb -= dy;
        }
        layout(nl, nt, nr, nb);
    }

    /**
     * 拖动改变view的大小（仅右、下边线）
     * @param dx 右边线位置改变量
     * @param dy 下边线位置改变量
     */
    private void drag(int dx, int dy){
        int l = originLeft;
        int t = originTop;
        int nr = originRight + dx;
        int nb = originBottom + dy;
        if(nr > maxRight){
            nr = maxRight;
        }else if(nr - l < 180){
            nr = l + 180;
        }
        if(nb > maxBottom){
            nb = maxBottom;
        }else if(nb - t < 180){
            nb = t + 180;
        }
        layout(l, t, nr, nb);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(testeeMode > 0){
            return true;
        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                switch (currentMode){
                    case COARSE_TUNING:
                        pressingBtn = checkFingerPosition(event.getX(), event.getY());
                        break;
                    case FINE_TUNING:
                        pressingBtn = checkFingerPosition(event.getX(), event.getY()) + 10;
                        break;
                    default:
                        //EDIT
                        pressingBtn = 9;
                }
                //触摸事件开始，存储触摸起始点坐标
                startingTouchX = event.getX();
                startingTouchY = event.getY();
                //也储存一开始view的四个边距
                originLeft = getLeft();
                originRight = getRight();
                originTop = getTop();
                originBottom = getBottom();
                //并储存父视图的尺寸
                maxRight = ((ViewGroup) getParent()).getWidth();
                maxBottom = ((ViewGroup) getParent()).getHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                //存储手指在横纵坐标上的位移分量
                int displacementX = (int) (event.getX() - startingTouchX);
                int displacementY = (int) (event.getY() - startingTouchY);
                switch (pressingBtn){
                    //粗调模式
                    case 4:
                        //任意移动
                        move(displacementX, displacementY);
                        break;
                    case 5:
                        //右边线调整
                        drag(displacementX, 0);
                        break;
                    case 7:
                        //下边线调整
                        drag(0, displacementY);
                        break;
                    case 8:
                        //右、下边线一起调整
                        drag(displacementX, displacementY);
                        break;
                    //编辑模式
                    case 9:
                        //编辑模式下，移动手指超过一定范围就触发模式切换
                        if(Math.abs(displacementX) > 100 || Math.abs(displacementY) > 100){
                            currentMode = ModeType.COARSE_TUNING;
                            invalidate();
                            pressingBtn = -100;
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                switch (pressingBtn){
                    //粗调模式
                    case 0:
                        //左上角手指离开，是切换模式命令
                        currentMode = ModeType.FINE_TUNING;
                        invalidate();
                        break;
                    case 1:
                        //减少列数
                        if(colsCount > 1){
                            colsCount--;
                            invalidate();
                        }
                        break;
                    case 2:
                        //增加列数
                        if(colsCount <= 26){
                            colsCount++;
                            invalidate();
                        }
                        break;
                    case 3:
                        //减少行数
                        if(rowsCount > 1){
                            rowsCount--;
                            invalidate();
                        }
                        break;
                    case 6:
                        //增加行数
                        if(rowsCount <= 26){
                            rowsCount++;
                            invalidate();
                        }
                        break;
                    //EDIT 编辑模式
                    case 9:
                        //计算点击处所在格子的在第几行第几列（0开始）
                        int res_x = (int) Math.floor(event.getX() / getWidth() * colsCount);
                        int res_y = (int) Math.floor(event.getY() / getHeight() * rowsCount);
                        //存储点击处所在格子序号为答案，左上角第一个序号为0
                        res_x = res_y * colsCount + res_x;//借用res_x来存储这个序号
                        if(!answers.add(res_x)){
                            answers.remove(res_x);
                        }
                        //更新绘制，绘制时会将已点击处的格子染色（见onDraw）
                        invalidate();
                        break;
                    //微调模式
                    case 10:
                        //左上角手指离开，是切换模式命令
                        currentMode = ModeType.EDIT;
                        invalidate();
                        break;
                    case 11:
                        //向上微移
                        move(0, -1);
                        break;
                    case 12:
                        //右边线微向左，变窄
                        drag(-1, 0);
                        break;
                    case 13:
                        //向左微移
                        move(-1, 0);
                        break;
                    case 14:
                        //中间，表示切换微调的move模式
                        if(fineTuningMove){
                            fineTuningMove = false;
                            iconsFineTuning[5] = da_r1;
                            iconsFineTuning[7] = da_d1;
                            iconsFineTuning[8] = landScape?da_zheng:da_zheng1;
                        }else{
                            fineTuningMove = true;
                            iconsFineTuning[5] = da_r;
                            iconsFineTuning[7] = da_d;
                            iconsFineTuning[8] = da_mul;
                        }
                        invalidate();
                        break;
                    case 15:
                        if(fineTuningMove){
                            //向右微移
                            move(1, 0);
                        }else{
                            //右边线微向右，加宽
                            drag(1, 0);
                        }
                        break;
                    case 16:
                        //下边线微向上，变窄
                        drag(0, -1);
                        break;
                    case 17:
                        if(fineTuningMove){
                            //向下微移
                            move(0, 1);
                        }else{
                            //下边线微向下，加高
                            drag(0, 1);
                        }
                        break;
                    case 18:
                        //右下角关闭或切换横竖屏
                        if(!fineTuningMove){
                            landScape = !landScape;
                            iconsFineTuning[8] = landScape?da_zheng:da_zheng1;
                            invalidate();
                        }else{
                            ((ViewGroup) getParent()).removeView(this);
                            removed = true;
                        }
                        break;
                }
                pressingBtn = -1;
                break;
        }
        return true;
    }

    /**
     * 计算九个图标的位置。
     * 图标对应的数组元素：
     * 0 1 2
     * 3 4 5
     * 6 7 8
     * @param w view的宽度
     * @param h view的高度
     * @param d 图标直径
     */
    private void getIconsPosition(int w, int h, int d){
        iconsPositions[0][0] = iconsPositions[3][0] = iconsPositions[6][0] = 0;
        iconsPositions[0][2] = iconsPositions[3][2] = iconsPositions[6][2] = d;
        iconsPositions[1][0] = iconsPositions[4][0] = iconsPositions[7][0] = (w - d)/2;
        iconsPositions[1][2] = iconsPositions[4][2] = iconsPositions[7][2] = (w + d)/2;
        iconsPositions[2][0] = iconsPositions[5][0] = iconsPositions[8][0] = w - d;
        iconsPositions[2][2] = iconsPositions[5][2] = iconsPositions[8][2] = w;

        iconsPositions[0][1] = iconsPositions[1][1] = iconsPositions[2][1] = 0;
        iconsPositions[0][3] = iconsPositions[1][3] = iconsPositions[2][3] = d;
        iconsPositions[3][1] = iconsPositions[4][1] = iconsPositions[5][1] = (h - d)/2;
        iconsPositions[3][3] = iconsPositions[4][3] = iconsPositions[5][3] = (h + d)/2;
        iconsPositions[6][1] = iconsPositions[7][1] = iconsPositions[8][1] = h - d;
        iconsPositions[6][3] = iconsPositions[7][3] = iconsPositions[8][3] = h;
    }

    /**
     * 根据九图标位置数组，对比后得知点击处位于哪个图标区域内。
     * @param x 点击处x坐标
     * @param y 点击处y坐标
     * @return 点击处所在的图标序号。如果不在图标区域内，则返回-100
     */
    private int checkFingerPosition(float x, float y){
        for(int i = 0; i < 9; i++){
            if(x>=iconsPositions[i][0] && x<=iconsPositions[i][2] && y>=iconsPositions[i][1] && y<=iconsPositions[i][3]){
                return i;
            }
        }
        return -100;
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        //onDraw调用时机：
        //添加这个view到窗口时；view尺寸、位置发生改变时；手动调用invalidate()或postInvalidate();

        //当这个答案网格是扫描学生答案后显示的、也即只有一次性的功能时，应该只需要绘制一次。
        //但该应用测试时，系统会自动重绘随机一到数次，由于使用的是透明蓝色，叠加后肉眼可见不同。推测有可能是因为硬件加速？
        //以下是防止重绘的解决方法。这个问题我只能这样解决。
        if(testeeMode > 1){
            return;
        }else if(testeeMode == 1){
            testeeMode++;
        }
        //获取该view尺寸
        int w = getWidth();
        int h = getHeight();
        //获取每行每列间距
        float colspacing = (float) w / colsCount;
        float rowspacing = (float) h / rowsCount;
        //绘制网格
        for(float i = colspacing; i < w; i += colspacing){
            canvas.drawLine(i, 0, i, h, paintLineGrids);
        }
        for(float i = rowspacing; i < h; i += rowspacing){
            canvas.drawLine(0, i, w, i, paintLineGrids);
        }
        //绘制外框和默认底色
        canvas.drawRect(0,0,w,h, paintLineBorder);
        canvas.drawRect(0,0,w,h, paintFillInside);
        //填充答案所在方框
        float left, top;
        for(int answer : answers){
            left = colspacing * (answer % colsCount);
            top = rowspacing * ((int) Math.floor((double) answer / colsCount));
            canvas.drawRect(left, top, left + colspacing, top + rowspacing, paintFillAns);
        }
        if(testeeMode == 2){
            //考生答案模式，只能进入这里一次，函数入口时是1，这里是2
            for(int answer : testeeAnswers){
                left = colspacing * (answer % colsCount);
                top = rowspacing * ((int) Math.floor((double) answer / colsCount));
                canvas.drawRect(left, top, left + colspacing, top + rowspacing, paintLineTestee);
            }
        }
        //绘制九图标
        switch (currentMode){
            case COARSE_TUNING:
                getIconsPosition(w, h, ICON_DIAMETER);
                for(int i = 0; i < 9; i++){
                    iconsCoarseTuning[i].setBounds(iconsPositions[i][0], iconsPositions[i][1], iconsPositions[i][2], iconsPositions[i][3]);
                    iconsCoarseTuning[i].draw(canvas);
                }
                break;
            case FINE_TUNING:
                getIconsPosition(w, h, ICON_DIAMETER);
                for(int i = 0; i < 9; i++){
                    iconsFineTuning[i].setBounds(iconsPositions[i][0], iconsPositions[i][1], iconsPositions[i][2], iconsPositions[i][3]);
                    iconsFineTuning[i].draw(canvas);
                }
                break;
        }
    }

    /**
     * 获取网格当中所有格子的左上角坐标数组。
     * @return 数组元素各层级为【行】【列】【x或y】
     */
    public int[][][] getXYPoints(){
        return getXYPoints((int)getX(),(int)getY(),getHeight(),getWidth(),rowsCount,colsCount);
    }

    public static int[][][] getXYPoints(String feature){
        String[] strings = feature.split("_");
        if(strings.length != 5){
            strings = ("x0y0_h180w180_r3c4_P").split("_");
        }
        //首先分析xy坐标（位置）
        int x = matchInt(strings[0], "x(\\d+)");
        int y = matchInt(strings[0], "y(\\d+)");
        //然后分析hw高和宽（尺寸）
        int h = matchInt(strings[1], "h(\\d+)");
        int w = matchInt(strings[1], "w(\\d+)");
        //再而是rc行列数
        int r = matchInt(strings[2], "r(\\d+)");
        int c = matchInt(strings[2], "c(\\d+)");
        //最后输出
        if(strings[3].charAt(0) == 'L'){
            return getXYPoints(x, y, h, w, r, c);
        }else{
            return getXYPoints(x, y, h, w, c, r);
        }
    }

    /**
     * 获取网格当中所有格子的左上角坐标数组。
     * @param x 网格最左上角x坐标
     * @param y 网格最左上角y坐标
     * @param w 网格总宽度
     * @param h 网格总高度
     * @param r 网格总行数
     * @param c 网格总列数
     * @return 坐标数组元素各层级为【行】【列】【x或y】
     */
    private static int[][][] getXYPoints(int x, int y, int h, int w, int r, int c){
        int[][][] res = new int[r][c][2];
        float colspacing = (float) w / c;
        float rowspacing = (float) h / r;
        for(int row = 0; row < r; row++){
            for(int col = 0; col < c; col++){
                res[row][col][0] = (int) (x + col * colspacing);
                res[row][col][1] = (int) (y + row * rowspacing);
            }
        }
        return res;
    }

}

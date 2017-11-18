package cookiework.encryptedvideoview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

import cookiework.encryptedvideoview.encryption.VideoInfo;
import cookiework.encryptedvideoview.util.DensityUtil;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer.*;

import static cookiework.encryptedvideoview.Constants.PLAY_ADDRESS;
import static cookiework.encryptedvideoview.Constants.PLAY_VOD_ADDRESS;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoPlayActivity extends Activity implements OnClickListener,
        OnGestureListener, OnTouchListener{
    private VideoPlayView custom_VideoPlay;
    private boolean isOnline = false;
    private boolean isChangedVideo = false;
    private int playedTime;
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private final static int TIME = 6868;
    private boolean isPaused = false;
    private final static int PROGRESS_CHANGED = 0;
    private final static int HIDE_CONTROLER = 1;
    SeekBar skbProgress;

    Button button_start;
    Button button_back;
    String downPath;
    String videName;

    TextView textview_time_start, textview_time_end, textview_course_name;
    LinearLayout layout_tabs;
    RelativeLayout surfaceview_layout_title;
    RelativeLayout surfaceview_layout_controls;
    RelativeLayout surfaceview_layout_loadinglogo;

    private RelativeLayout root_layout;// 根布局
    private RelativeLayout gesture_volume_layout;// 音量控制布局
    private TextView geture_tv_volume_percentage;// 音量百分比
    private ImageView gesture_iv_player_volume;// 音量图标
    private RelativeLayout gesture_progress_layout;// 进度图标
    private TextView geture_tv_progress_time;// 播放时间进度
    private ImageView gesture_iv_progress;// 快进或快退标志
    private GestureDetector gestureDetector;
    private AudioManager audiomanager;
    private int maxVolume, currentVolume;
    private static final float STEP_PROGRESS = 2f;// 设定进度滑动时的步长，避免每次滑动都改变，导致改变过快
    private static final float STEP_VOLUME = 2f;// 协调音量滑动时的步长，避免每次滑动都改变，导致改变过快
    private boolean firstScroll = false;// 每次触摸屏幕后，第一次scroll的标志
    private int GESTURE_FLAG = 0;// 1,调节进度，2，调节音量
    private static final int GESTURE_MODIFY_PROGRESS = 1;
    private static final int GESTURE_MODIFY_VOLUME = 2;
    private long palyerCurrentPosition = 0;// 模拟进度播放的当前标志，毫秒
    private long playerDuration = 0;// 模拟播放资源总时长，毫秒

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        Intent intent = getIntent();
        VideoInfo info = intent.getParcelableExtra("videoInfo");

        System.out.println(info.getCipherAddr());

        if(info.getStatus().equals("vod")){
            downPath = PLAY_VOD_ADDRESS.replace("[stream_name]", info.getCipherAddr());
        }else {
            downPath = PLAY_ADDRESS.replace("[stream_name]", info.getCipherAddr());
        }
        System.out.println(downPath);
        videName = info.getCipherTitle();
        viewId();

    }

    protected void onStart()
    {
        super.onStart();
    }

    protected void onRestart()
    {
        super.onRestart();
    }

    protected void onStop()
    {
        super.onStop();
    }

    protected void onPause()
    {
        playedTime = custom_VideoPlay.getCurrentPosition();
        custom_VideoPlay.pause();
        button_start.setBackgroundResource(R.drawable.video_player);
        super.onPause();
    }

    protected void onResume()
    {
        surfaceview_layout_loadinglogo.setVisibility(View.VISIBLE);
        if (!isChangedVideo)
        {
            custom_VideoPlay.seekTo(playedTime);
            custom_VideoPlay.start();
        } else
        {
            isChangedVideo = false;
        }
        if (custom_VideoPlay.isPlaying())
        {
            button_start.setBackgroundResource(R.drawable.video_pauseer);
            hideControllerDelay();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        myHandler.removeMessages(PROGRESS_CHANGED);
        myHandler.removeMessages(HIDE_CONTROLER);
        if (custom_VideoPlay.isPlaying())
        {
            custom_VideoPlay.stopPlayback();
        }
        super.onDestroy();
    }

    private void viewId()
    {
        surfaceview_layout_loadinglogo = (RelativeLayout) findViewById(R.id.course_surfaceview_layout_loadinglogo);
        surfaceview_layout_title = (RelativeLayout) findViewById(R.id.course_surfaceview_layout_title);
        surfaceview_layout_controls = (RelativeLayout) findViewById(R.id.course_surfaceview_layout_controls);

        textview_course_name = (TextView) findViewById(R.id.course_textView_time_coursename);
        textview_time_start = (TextView) findViewById(R.id.course_textView_time_start);
        textview_time_end = (TextView) findViewById(R.id.course_textView_time_end);

        button_start = (Button) findViewById(R.id.course_surfaceview_button_start);
        button_back = (Button) findViewById(R.id.course_button_time_back);
        custom_VideoPlay = (VideoPlayView) findViewById(R.id.course_custom_videoplay);
        skbProgress = (SeekBar) findViewById(R.id.course_skbProgress);

        root_layout = (RelativeLayout) findViewById(R.id.course_surfaceview_layout__md);
        root_layout.setOnClickListener(this);

        skbProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
        button_start.setOnClickListener(this);
        custom_VideoPlay.setOnClickListener(this);
        button_back.setOnClickListener(this);

        custom_VideoPlay.setOnErrorListener(new OnErrorListener()
        {
            public boolean onError(IMediaPlayer mp, int what, int extra)
            {
                custom_VideoPlay.stopPlayback();
                isOnline = false;
                new AlertDialog.Builder(VideoPlayActivity.this)
                        .setTitle("错误")
                        .setMessage("视频播放已停止,请重新播放")
                        .setPositiveButton("知道了",
                                new AlertDialog.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog,
                                                        int which)
                                    {
                                        custom_VideoPlay.stopPlayback();
                                        finish();
                                    }
                                }).setCancelable(false).show();

                return false;
            }
        });
        custom_VideoPlay.setOnPreparedListener(new OnPreparedListener()
        {
            public void onPrepared(IMediaPlayer arg0)
            {
                setVideoScale();
                int i = custom_VideoPlay.getDuration();
                playerDuration = i;
                skbProgress.setMax(i);
                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;
                textview_time_end.setText(String.format("%02d:%02d:%02d", hour,
                        minute, second));

                custom_VideoPlay.start();
                button_start.setBackgroundResource(R.drawable.video_pauseer);
                hideControllerDelay();
                myHandler.sendEmptyMessage(PROGRESS_CHANGED);
                surfaceview_layout_loadinglogo.setVisibility(View.GONE);

            }
        });
        // 播放结束之后弹出提示
        custom_VideoPlay.setOnCompletionListener(new OnCompletionListener()
                {
                    public void onCompletion(IMediaPlayer arg0)
                    {
                        System.out.println("播放结束");
                        custom_VideoPlay.stopPlayback();
                    }
                });

        textview_course_name.setText(videName);
        custom_VideoPlay.setVideoPath(downPath);

        gesture_volume_layout = (RelativeLayout) findViewById(R.id.gesture_volume_layout);
        gesture_progress_layout = (RelativeLayout) findViewById(R.id.gesture_progress_layout);
        geture_tv_progress_time = (TextView) findViewById(R.id.geture_tv_progress_time);
        geture_tv_volume_percentage = (TextView) findViewById(R.id.geture_tv_volume_percentage);
        gesture_iv_progress = (ImageView) findViewById(R.id.gesture_iv_progress);
        gesture_iv_player_volume = (ImageView) findViewById(R.id.gesture_iv_player_volume);
        gestureDetector = new GestureDetector(this, this);
        root_layout.setLongClickable(true);
        gestureDetector.setIsLongpressEnabled(true);
        root_layout.setOnTouchListener(this);
        audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 获取系统最大音量
        currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC); // 获取当前值

    }

    public void onClick(View v)
    {
        switch (v.getId())
        {

            case R.id.course_custom_videoplay:
                break;
            case R.id.course_surfaceview_layout__md:

                if (surfaceview_layout_title.getVisibility() == View.INVISIBLE
                        && surfaceview_layout_controls.getVisibility() == View.INVISIBLE)
                {
                    surfaceview_layout_title.setVisibility(View.VISIBLE);
                    surfaceview_layout_controls.setVisibility(View.VISIBLE);
                } else
                {
                    surfaceview_layout_title.setVisibility(View.INVISIBLE);
                    surfaceview_layout_controls.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.course_surfaceview_button_start:
                cancelDelayHide();
                if (isPaused)
                {
                    custom_VideoPlay.start();
                    button_start.setBackgroundResource(R.drawable.video_pauseer);
                    hideControllerDelay();
                } else
                {
                    custom_VideoPlay.pause();
                    button_start.setBackgroundResource(R.drawable.video_player);
                }
                isPaused = !isPaused;
                break;
            case R.id.course_button_time_back:
                finish();
                break;
            default:
                break;
        }

    }

    // Handler handler = new Handler()
    // {
    // public void handleMessage(Message msg)
    // {
    // switch (msg.arg1)
    // {
    // case 4:
    //
    // if (surfaceview_layout_title.getVisibility() == View.VISIBLE
    // && surfaceview_layout_controls.getVisibility() == View.VISIBLE)
    // {
    // surfaceview_layout_title.setVisibility(View.INVISIBLE);
    // surfaceview_layout_controls.setVisibility(View.INVISIBLE);
    // hideHead(surfaceview_layout_title,
    // surfaceview_layout_controls);
    // }
    // break;
    // default:
    // break;
    // }
    // }
    // };

    /**
     * 拖动条监听
     *
     * @author William
     *
     */
    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener
    {
        public void onProgressChanged(SeekBar seekbar, int progress,
                                      boolean fromUser)
        {
            if (fromUser)
            {
                if (!isOnline)
                {
                    custom_VideoPlay.seekTo(progress);
                    palyerCurrentPosition = progress;
                }
            }
        }

        public void onStartTrackingTouch(SeekBar arg0)
        {
            myHandler.removeMessages(HIDE_CONTROLER);
        }

        public void onStopTrackingTouch(SeekBar seekBar)
        {
            myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
        }
    }

    Handler myHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {

                case PROGRESS_CHANGED:

                    int i = custom_VideoPlay.getCurrentPosition();
                    skbProgress.setProgress(i);

                    if (isOnline)
                    {
                        int j = custom_VideoPlay.getBufferPercentage();
                        skbProgress.setSecondaryProgress(j * skbProgress.getMax()
                                / 100);
                    } else
                    {
                        skbProgress.setSecondaryProgress(0);
                    }

                    i /= 1000;
                    int minute = i / 60;
                    int hour = minute / 60;
                    int second = i % 60;
                    minute %= 60;
                    textview_time_start.setText(String.format("%02d:%02d:%02d",
                            hour, minute, second));

                    sendEmptyMessageDelayed(PROGRESS_CHANGED, 100);
                    break;

                case HIDE_CONTROLER:
                    break;
            }

            super.handleMessage(msg);
        }
    };

    private void hideControllerDelay()
    {
        myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
    }

    private void cancelDelayHide()
    {
        myHandler.removeMessages(HIDE_CONTROLER);
    }

    private void setVideoScale()
    {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int videoWidth = custom_VideoPlay.getVideoWidth();
        int videoHeight = custom_VideoPlay.getVideoHeight();
        int mWidth = dm.widthPixels;
        int mHeight = dm.heightPixels;

        if (videoWidth > 0 && videoHeight > 0)
        {
             if (videoWidth * mHeight > mWidth * videoHeight)
             {
                  mHeight = mWidth * videoHeight / videoWidth;
              } else if (videoWidth * mHeight < mWidth * videoHeight)
              {
                  mWidth = mHeight * videoWidth / videoHeight;
              }
         }
         custom_VideoPlay.setVideoScale(mWidth, mHeight);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            finish();
            return false;
        }
        return false;
    }

    public boolean onTouch(View v, MotionEvent event)
    {
        // TODO Auto-generated method stub
        // 手势里除了singleTapUp，没有其他检测up的方法
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            GESTURE_FLAG = 0;// 手指离开屏幕后，重置调节音量或进度的标志
            gesture_volume_layout.setVisibility(View.INVISIBLE);
            gesture_progress_layout.setVisibility(View.INVISIBLE);
        }
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        // TODO Auto-generated method stub
        firstScroll = true;// 设定是触摸屏幕后第一次scroll的标志
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY)
    {
        if (firstScroll)
        {// 以触摸屏幕后第一次滑动为标准，避免在屏幕上操作切换混乱
            // 横向的距离变化大则调整进度，纵向的变化大则调整音量
            if (Math.abs(distanceX) >= Math.abs(distanceY))
            {
                gesture_volume_layout.setVisibility(View.INVISIBLE);
                gesture_progress_layout.setVisibility(View.VISIBLE);
                GESTURE_FLAG = GESTURE_MODIFY_PROGRESS;
            } else
            {
                gesture_volume_layout.setVisibility(View.VISIBLE);
                gesture_progress_layout.setVisibility(View.INVISIBLE);
                GESTURE_FLAG = GESTURE_MODIFY_VOLUME;
            }
        }
        // 如果每次触摸屏幕后第一次scroll是调节进度，那之后的scroll事件都处理音量进度，直到离开屏幕执行下一次操作
        if (GESTURE_FLAG == GESTURE_MODIFY_PROGRESS)
        {
            // distanceX=lastScrollPositionX-currentScrollPositionX，因此为正时是快进
            if (Math.abs(distanceX) > Math.abs(distanceY))
            {// 横向移动大于纵向移动
                if (distanceX >= DensityUtil.dip2px(this, STEP_PROGRESS))
                {// 快退，用步长控制改变速度，可微调
                    gesture_iv_progress
                            .setImageResource(R.drawable.souhu_player_backward);
                    if (palyerCurrentPosition > 3 * 1000)
                    {// 避免为负
                        palyerCurrentPosition -= 3 * 1000;// scroll方法执行一次快退3秒
                    } else
                    {
                        palyerCurrentPosition = 3 * 1000;
                    }
                } else if (distanceX <= -DensityUtil
                        .dip2px(this, STEP_PROGRESS))
                {// 快进
                    gesture_iv_progress
                            .setImageResource(R.drawable.souhu_player_forward);
                    if (palyerCurrentPosition < playerDuration - 16 * 1000)
                    {// 避免超过总时长
                        palyerCurrentPosition += 3 * 1000;// scroll执行一次快进3秒
                    } else
                    {
                        palyerCurrentPosition = playerDuration - 10 * 1000;
                    }
                }
            }

            geture_tv_progress_time
                    .setText(converLongTimeToStr(palyerCurrentPosition) + "/"
                            + converLongTimeToStr(playerDuration));

            custom_VideoPlay.seekTo((int) palyerCurrentPosition);

        }
        // 如果每次触摸屏幕后第一次scroll是调节音量，那之后的scroll事件都处理音量调节，直到离开屏幕执行下一次操作
        else if (GESTURE_FLAG == GESTURE_MODIFY_VOLUME)
        {
            currentVolume = audiomanager
                    .getStreamVolume(AudioManager.STREAM_MUSIC); // 获取当前值
            if (Math.abs(distanceY) > Math.abs(distanceX))
            {// 纵向移动大于横向移动
                if (distanceY >= DensityUtil.dip2px(this, STEP_VOLUME))
                {// 音量调大,注意横屏时的坐标体系,尽管左上角是原点，但横向向上滑动时distanceY为正
                    if (currentVolume < maxVolume)
                    {// 为避免调节过快，distanceY应大于一个设定值
                        currentVolume++;
                    }
                    gesture_iv_player_volume
                            .setImageResource(R.drawable.souhu_player_volume);
                } else if (distanceY <= -DensityUtil.dip2px(this, STEP_VOLUME))
                {// 音量调小
                    if (currentVolume > 0)
                    {
                        currentVolume--;
                        if (currentVolume == 0)
                        {// 静音，设定静音独有的图片
                            gesture_iv_player_volume
                                    .setImageResource(R.drawable.souhu_player_silence);
                        }
                    }
                }
                int percentage = (currentVolume * 100) / maxVolume;
                geture_tv_volume_percentage.setText(percentage + "%");
                audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        currentVolume, 0);
            }

        }

        firstScroll = false;// 第一次scroll执行完成，修改标志
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * 转换毫秒数成“分、秒”，如“01:53”。若超过60分钟则显示“时、分、秒”，如“01:01:30
     *
     * @param time 待转换的毫秒数
     * */
    private String converLongTimeToStr(long time)
    {
        int ss = 1000;
        int mi = ss * 60;
        int hh = mi * 60;

        long hour = (time) / hh;
        long minute = (time - hour * hh) / mi;
        long second = (time - hour * hh - minute * mi) / ss;

        String strHour = hour < 10 ? "0" + hour : "" + hour;
        String strMinute = minute < 10 ? "0" + minute : "" + minute;
        String strSecond = second < 10 ? "0" + second : "" + second;
        if (hour > 0)
        {
            return strHour + ":" + strMinute + ":" + strSecond;
        } else
        {
            return strMinute + ":" + strSecond;
        }
    }
}

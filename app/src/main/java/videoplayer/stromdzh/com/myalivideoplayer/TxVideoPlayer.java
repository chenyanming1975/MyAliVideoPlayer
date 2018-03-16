package videoplayer.stromdzh.com.myalivideoplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.alivc.player.AliVcMediaPlayer;
import com.alivc.player.MediaPlayer;

import java.lang.ref.WeakReference;

/**
 * author : dzh .
 * date   : 2018/1/26
 * desc   : 视屏播放器
 */
public class TxVideoPlayer extends RelativeLayout implements View.OnClickListener {

    private final int CODE_VIDEO_PROGRESS = 0; //更新进度条
    private final int CODE_VIDEO_AUTO_HIDE = 1; //自动隐藏控制控件
    private static final int TIME_AUTO_HIDE_BARS_DELAY = 3800;
    private int iconPause = R.drawable.zz_player_pause;
    private int iconPlay = R.drawable.zz_player_play;
    int iconShrink = R.drawable.zz_player_shrink;
    int iconExpand = R.drawable.zz_player_expand;
    private Animation mEnterFromTop;
    private Animation mEnterFromBottom;
    private Animation mExitFromTop;
    private Animation mExitFromBottom;

    private TextView positionTxt;
    private TextView durationTxt;
    private CustomSeekBar progressBar;
    private AliVcMediaPlayer mPlayer;
    private SurfaceView mSurfaceView;
    private RelativeLayout rl_play_pause;
    private ImageView iv_play_pause;
    private RelativeLayout rl_toggle_expandable;
    private ImageView iv_toggle_expandable;
    private RelativeLayout mController;
    private LinearLayout mTitleBar;
    private RelativeLayout rl_back;
    private TextView tv_title;
    private String mUrl = null;
    private boolean inSeek = false;
    private boolean isCompleted = false;
    private WeakReference<Activity> mHostActivity;
    private int mCurOrientation;
    private int mLastPlayingPos = -1;//onPause时的播放位置
    private boolean isActivityStop = false;
    private IPlayerImpl mIPlayerImpl;

    public TxVideoPlayer(Context context) {
        this(context, null);
    }

    public TxVideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TxVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        inflate(getContext(), R.layout.view_tx_player, this);
        mSurfaceView = findViewById(R.id.mSurfaceView);
        positionTxt = findViewById(R.id.tv_current_time);
        durationTxt = findViewById(R.id.tv_total_time);
        progressBar = findViewById(R.id.csb);
        rl_play_pause = findViewById(R.id.rl_play_pause);
        rl_play_pause.setOnClickListener(this);
        iv_play_pause = findViewById(R.id.iv_play_pause);
        rl_toggle_expandable = findViewById(R.id.rl_toggle_expandable);
        rl_toggle_expandable.setOnClickListener(this);
        iv_toggle_expandable = findViewById(R.id.iv_toggle_expandable);
        mController = findViewById(R.id.mController);
        mTitleBar = findViewById(R.id.ll_video_title);
        rl_back = findViewById(R.id.rl_back);
        rl_back.setOnClickListener(this);
        tv_title = findViewById(R.id.tv_title);
        initAnimation();
        initSeekBar();
        initSurfaceView();
        initVodPlayer();
    }


    /**
     * 初始化标题栏/控制栏显隐动画效果
     */
    private void initAnimation() {
        mEnterFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.enter_from_top);
        mEnterFromBottom = AnimationUtils.loadAnimation(getContext(), R.anim.enter_from_bottom);
        mExitFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.exit_from_top);
        mExitFromBottom = AnimationUtils.loadAnimation(getContext(), R.anim.exit_from_bottom);

        mEnterFromTop.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mTitleBar.setVisibility(VISIBLE);
            }
        });
        mEnterFromBottom.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mController.setVisibility(VISIBLE);
            }
        });
        mExitFromTop.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mTitleBar.setVisibility(GONE);
            }
        });
        mExitFromBottom.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mController.setVisibility(GONE);
            }
        });
    }

    /**
     * 初始化进度
     */
    private void initSeekBar() {
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlayer != null) {
                    mPlayer.seekTo(seekBar.getProgress());
                    if (isCompleted) {
                        inSeek = false;
                    } else {
                        inSeek = true;
                    }
                }
            }
        });
    }

    /**
     * 初始化SurfaceView
     */
    private void initSurfaceView() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
//                holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
                holder.setKeepScreenOn(true);
                // Important: surfaceView changed from background to front, we need reset surface to mediaplayer.
                // 对于从后台切换到前台,需要重设surface;部分手机锁屏也会做前后台切换的处理
                if (mPlayer != null) {
                    mPlayer.setVideoSurface(holder.getSurface());
                }
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                if (mPlayer != null) {
                    mPlayer.setSurfaceChanged();
                }
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mPlayer != null) {
                    mPlayer.releaseVideoSurface();
                }
            }
        });


        mSurfaceView.setOnClickListener(this);
    }

    /**
     * 初始化视屏播放器
     */
    private void initVodPlayer() {
        mPlayer = new AliVcMediaPlayer(getContext(), mSurfaceView);
//开启缓存
//        String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save_cache";
//        mPlayer.setPlayingCache(true, sdDir, 60 * 60 /*时长, s */, 300 /*大小，MB*/);
//开启循环播放
//        mPlayer.setCirclePlay(true);

        mPlayer.setPreparedListener(new MyPrepareListener(this));
        mPlayer.setPcmDataListener(new MyPcmDataListener(this));
        mPlayer.setCircleStartListener(new MyCircleStartListener(this));
        mPlayer.setFrameInfoListener(new MyFrameInfoListener(this));
        mPlayer.setErrorListener(new MyErrorListener(this));
        mPlayer.setCompletedListener(new MyCompletedListener(this));
        mPlayer.setSeekCompleteListener(new MySeekCompleteListener(this));
        mPlayer.setStoppedListener(new MyPlayerStoppedListener(this));
        mPlayer.setBufferingUpdateListener(new MyBufferUpdateListener(this));
        //打开、关闭播放器日志
        mPlayer.enableNativeLog();
//        mPlayer.disableNativeLog();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //播放暂停
            case R.id.rl_play_pause:
                isCompleted = false;
                inSeek = false;
                if (mPlayer.isPlaying()) {
                    pause();
                    setPlayState(PlayState.PAUSE);
                } else {
                    resume();
                }
                break;
            //切换全屏
            case R.id.rl_toggle_expandable:
                OrientationUtil.changeOrientation(mHostActivity.get());
                break;
            //视屏上面的返回
            case R.id.rl_back:
                if (mIPlayerImpl != null) {
                    mIPlayerImpl.onBack();
                } else {
                    if (mHostActivity.get() != null)
                        mHostActivity.get().finish();
                }

//                Activity activity = mHostActivity.get();
//                if (activity == null) return;
//                // 全屏播放时,单击左上角返回箭头,先回到竖屏状态,再关闭
//                // 这里功能最好跟onBackPressed()操作一致
//                int orientation = OrientationUtil.getOrientation(activity);
//                if (orientation == OrientationUtil.HORIZONTAL) {
//                    OrientationUtil.forceOrientation(activity, OrientationUtil.VERTICAL);
//                } else {
//                    activity.finish();
//                }
                break;
            //点击播放控件
            case R.id.mSurfaceView:
                if (mController.getVisibility() == VISIBLE) {
                    showOrHideBars(false, true);
                } else {
                    showOrHideBars(true, true);
                }
                break;
        }
    }


    /**
     * 显隐标题栏和控制条
     *
     * @param show          是否显示
     * @param animateEffect 是否需要动画效果
     */
    private void showOrHideBars(boolean show, boolean animateEffect) {
        if (animateEffect) {
            animateShowOrHideBars(show);
        } else {
            forceShowOrHideBars(show);
        }
    }

    /**
     * 带动画效果的显隐标题栏和控制栏
     */
    private void animateShowOrHideBars(boolean show) {
        mController.clearAnimation();
        mTitleBar.clearAnimation();

        if (show) {
            if (mController.getVisibility() != VISIBLE) {
                if (isVerticalShow())
                    mTitleBar.startAnimation(mEnterFromTop);
                mController.startAnimation(mEnterFromBottom);
            }
            sendAutoHideBarsMsg();
        } else {
            if (mController.getVisibility() != GONE) {
                if (isVerticalShow())
                    mTitleBar.startAnimation(mExitFromTop);
                else
                    mTitleBar.setVisibility(GONE);
                mController.startAnimation(mExitFromBottom);
            }
        }
    }

    /**
     * 竖屏时候是否需要显示标题栏
     *
     * @return 返回true 需要显示 false：不需要显示
     */
    private boolean isVerticalShow() {
        Activity activity = mHostActivity.get();
        if (activity == null) return false;
        int orientation = OrientationUtil.getOrientation(mHostActivity.get());
        if (orientation == OrientationUtil.VERTICAL) {
            return mShowVerticalTitleBar;
        } else
            return true;
    }

    /**
     * 直接显隐标题栏和控制栏
     */
    private void forceShowOrHideBars(boolean show) {
        mTitleBar.clearAnimation();
        mController.clearAnimation();

        if (show) {
            mController.setVisibility(VISIBLE);
            if (isVerticalShow())
                mTitleBar.setVisibility(VISIBLE);
            else
                mTitleBar.setVisibility(GONE);
        } else {
            mController.setVisibility(GONE);
            mTitleBar.setVisibility(GONE);
        }
    }


    private void resume() {
        if (mPlayer != null) {
            mPlayer.play();
            setPlayState(PlayState.PLAY);
        }
    }

    /**
     * 发送message给handler,自动隐藏标题栏
     */
    private void sendAutoHideBarsMsg() {
        //  初始自动隐藏标题栏和控制栏
        mHanlder.removeMessages(CODE_VIDEO_AUTO_HIDE);
        mHanlder.sendEmptyMessageDelayed(CODE_VIDEO_AUTO_HIDE, TIME_AUTO_HIDE_BARS_DELAY);
    }

    /**
     * 设置播放按钮的状态
     *
     * @param curPlayState 是不是播放状态
     */
    public void setPlayState(int curPlayState) {

        switch (curPlayState) {
            case PlayState.PLAY:
                iv_play_pause.setImageResource(iconPause);
//                curPlayIconType = PLAY_ICON_TYPE_PAUSE;
                break;
            case PlayState.PAUSE:
            case PlayState.STOP:
            case PlayState.COMPLETE:
            case PlayState.ERROR:
                iv_play_pause.setImageResource(iconPlay);
//                curPlayIconType = PLAY_ICON_TYPE_PLAY;
                break;
        }
    }

    private static class MySeekCompleteListener implements MediaPlayer.MediaPlayerSeekCompleteListener {


        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MySeekCompleteListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onSeekCompleted() {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onSeekCompleted();
            }
        }
    }

    //准备好
    private static class MyPrepareListener implements MediaPlayer.MediaPlayerPreparedListener {
        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyPrepareListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onPrepared() {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onPrepared();
            }
        }
    }

    private static class MyPcmDataListener implements MediaPlayer.MediaPlayerPcmDataListener {

        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyPcmDataListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }


        @Override
        public void onPcmData(byte[] bytes, int i) {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onPcmData(bytes, i);
            }
        }
    }

    private static class MyCircleStartListener implements MediaPlayer.MediaPlayerCircleStartListener {
        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyCircleStartListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onCircleStart() {

            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onCircleStart();
            }
        }
    }


    private static class MyFrameInfoListener implements MediaPlayer.MediaPlayerFrameInfoListener {

        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyFrameInfoListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onFrameInfoListener() {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onFrameInfoListener();
            }
        }
    }


    private static class MyErrorListener implements MediaPlayer.MediaPlayerErrorListener {

        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyErrorListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }


        @Override
        public void onError(int i, String msg) {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onError(i, msg);
            }
        }
    }

    private static class MyCompletedListener implements MediaPlayer.MediaPlayerCompletedListener {

        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyCompletedListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onCompleted() {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onCompleted();
            }
        }
    }


    private static class MyPlayerStoppedListener implements MediaPlayer.MediaPlayerStoppedListener {

        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyPlayerStoppedListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onStopped() {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onStopped();
            }
        }
    }


    private static class MyBufferUpdateListener implements MediaPlayer.MediaPlayerBufferingUpdateListener {

        private WeakReference<TxVideoPlayer> vodModeActivityWeakReference;

        public MyBufferUpdateListener(TxVideoPlayer vodModeActivity) {
            vodModeActivityWeakReference = new WeakReference<>(vodModeActivity);
        }

        @Override
        public void onBufferingUpdateListener(int percent) {
            TxVideoPlayer vodModeActivity = vodModeActivityWeakReference.get();
            if (vodModeActivity != null) {
                vodModeActivity.onBufferingUpdateListener(percent);
            }
        }
    }

    private void onBufferingUpdateListener(int percent) {
        updateBufferingProgress(percent);
    }

    private void updateBufferingProgress(int percent) {
        int duration = mPlayer.getDuration();
        int secondaryProgress = (int) (duration * percent * 1.0f / 100);
        progressBar.setSecondaryProgress(secondaryProgress);
    }


    private void onStopped() {
//        logStrs.add(format.format(new Date()) + getString(R.string.log_play_stopped));
    }

    private void onSeekCompleted() {
        inSeek = false;
    }

    private void onCompleted() {
        mLastPlayingPos = 0;
        isCompleted = true;
        showVideoProgressInfo();
        stopUpdateTimer();
        if (mIPlayerImpl != null) {
            mIPlayerImpl.onComplete();
        }
    }

    private void onError(int i, String msg) {
        pause();
        if (mIPlayerImpl != null) {
            mIPlayerImpl.onError();
        }
    }

    private void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
            setPlayState(PlayState.ERROR);
        }
    }

    private void onFrameInfoListener() {
        inSeek = false;
        showVideoProgressInfo();
    }

    private void onCircleStart() {
        //循环播放开始
    }

    private void onPcmData(byte[] bytes, int i) {
        //pcm数据获取到了
    }

    private void onPrepared() {
        mPlayer.play();
        setPlayState(PlayState.PLAY);
    }


    private void showVideoProgressInfo() {
        int curPosition = (int) mPlayer.getCurrentPosition();
        int duration = (int) mPlayer.getDuration();
        int bufferPosition = mPlayer.getBufferPosition();
        if ((mPlayer.isPlaying())
                && !inSeek) {

            positionTxt.setText(Formatter.formatTime(curPosition));
            durationTxt.setText(Formatter.formatTime(duration));
            progressBar.setMax(duration);
            progressBar.setSecondaryProgress(bufferPosition);
            progressBar.setProgress(curPosition);
        }
        mLastPlayingPos = getCurrentTime();
        startUpdateTimer();
    }

    private int getCurrentTime() {
        if (mPlayer == null) return 0;
        return mPlayer.getCurrentPosition();
    }

    private void startUpdateTimer() {
        mHanlder.removeMessages(CODE_VIDEO_PROGRESS);
        mHanlder.sendEmptyMessageDelayed(CODE_VIDEO_PROGRESS, 1000);
    }

    private void stopUpdateTimer() {
        mHanlder.removeMessages(CODE_VIDEO_PROGRESS);
    }

    private Handler mHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_VIDEO_PROGRESS:
                    showVideoProgressInfo();
                    break;
                case CODE_VIDEO_AUTO_HIDE:
                    animateShowOrHideBars(false);
                    break;
                default:
                    break;
            }

        }
    };


    public void updateActivityOrientation() {
        int orientation = OrientationUtil.getOrientation(mHostActivity.get());

        //更新播放器宽高
        float width = DensityUtil.getWidthInPx(mHostActivity.get());
        float height = DensityUtil.getHeightInPx(mHostActivity.get());
        if (orientation == OrientationUtil.HORIZONTAL) {
            getLayoutParams().height = (int) height;
            getLayoutParams().width = (int) width;
        } else {
            width = DensityUtil.getWidthInPx(mHostActivity.get());
            height = DensityUtil.dip2px(mHostActivity.get(), 200f);
        }
        getLayoutParams().height = (int) height;
        getLayoutParams().width = (int) width;

        //需要强制显示再隐藏控制条,不然若切换为横屏时控制条是隐藏的,首次触摸显示时,会显示在200dp的位置
        forceShowOrHideBars(true);
        sendAutoHideBarsMsg();
        //更新全屏图标
        setOrientation(orientation);
    }

    public void setOrientation(int orientation) {
        mCurOrientation = orientation;
        //更新全屏图标
        if (orientation == OrientationUtil.HORIZONTAL) {
            iv_toggle_expandable.setImageResource(iconShrink);
        } else {
            iv_toggle_expandable.setImageResource(iconExpand);
        }
    }


    /**
     * 播放器控制功能对外开放接口,包括返回按钮,播放等...
     */
    public void setPlayerController(IPlayerImpl IPlayerImpl) {
        mIPlayerImpl = IPlayerImpl;
    }


    /**
     * 设置视屏标题
     *
     * @param title 标题
     */
    public void setTitle(String title) {
        if (tv_title != null)
            tv_title.setText(title);
    }

    /**
     * 设置视屏播放地址
     *
     * @param url 播放地址
     */
    public void setUrl(String url) {
        this.mUrl = url;
        start();
    }

    private boolean mShowVerticalTitleBar = false;

    /**
     * 在竖屏时候是否需要标题栏
     */
    public void showTitleBarVertical(boolean showVerticalTitleBar) {
        mShowVerticalTitleBar = showVerticalTitleBar;
    }

    public void setmHostActivity(Activity activity) {
        mHostActivity = new WeakReference<Activity>(activity);
    }

    private void start() {
        if (mPlayer != null) {
            mPlayer.prepareToPlay(mUrl);
            setPlayState(PlayState.PLAY);
        }
    }

    /**
     * 宿主页面onResume的时候从上次播放位置继续播放
     */
    public void onHostResume() {
        //        Log.i(TAG, "onHostResume " + mLastPlayingPos);
//        if (mLastPlayingPos >= 0) {
//            // 进度条更新为上次播放时间
//            startOrRestartPlay();
//        }
        if (isActivityStop) {
            mPlayer.play();
            isActivityStop = false;
        }

        //强制弹出标题栏和控制栏
        forceShowOrHideBars(true);
        sendAutoHideBarsMsg();
    }


    /**
     * 宿主页面onPause的时候记录播放位置，好在onResume的时候从中断点继续播放
     * 如果在宿主页面onStop的时候才来记录位置,则取到的都会是0
     */
    public void onHostPause() {
        mLastPlayingPos = getCurrentTime();
        isActivityStop = true;
//        getBufferLength();
        stopUpdateTimer();
        mHanlder.removeMessages(CODE_VIDEO_PROGRESS);
        mHanlder.removeMessages(CODE_VIDEO_AUTO_HIDE);
        // 在这里不进行stop或者pause播放的行为，因为特殊情况下会导致ANR出现
    }


    /**
     * 宿主页面destroy的时候页面恢复成竖直状态
     */
    public void onHostDestroy() {
        OrientationUtil.forceOrientation(mHostActivity.get(), OrientationUtil.VERTICAL);
    }


}

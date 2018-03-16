package videoplayer.stromdzh.com.myalivideoplayer;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {


//        private String url = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";
        private String url = "http://cdn2.txbimg.com/test/video/275220270.ts";
//    private String url = "http://v.cctv.com/flash/mp4video6/TMS/2011/01/05/cf752b1c12ce452b3040cab2f90bc265_h264818000nero_aac32-1.mp4";
    private TxVideoPlayer mTxVideoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.activity_main3);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mTxVideoPlayer = findViewById(R.id.mTxVideoPlayer);
        mTxVideoPlayer.setmHostActivity(this);
        mTxVideoPlayer.setPlayerController(playerImpl);
        mTxVideoPlayer.setUrl(url);

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mTxVideoPlayer != null) {
            mTxVideoPlayer.updateActivityOrientation();
        }
    }

    private IPlayerImpl playerImpl = new IPlayerImpl() {

        @Override
        public void onNetWorkError() {
//            showToast(null);
        }

        @Override
        public void onBack() {
            mBackPressed();

        }

        @Override
        public void onError() {
//            showToast("播放器发生异常");
        }
    };

    private void mBackPressed() {
        // 全屏播放时,单击左上角返回箭头,先回到竖屏状态,再关闭
        // 这里功能最好跟onBackPressed()操作一致
        int orientation = OrientationUtil.getOrientation(MainActivity.this);
        if (orientation == OrientationUtil.HORIZONTAL) {
            OrientationUtil.forceOrientation(MainActivity.this, OrientationUtil.VERTICAL);
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        mBackPressed();
    }
}

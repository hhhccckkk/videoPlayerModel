package com.hck.video.player.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hck.player3.R;
import com.hck.player3.R.id;
import com.hck.video.bean.VideoBean;
import com.hck.video.player.util.StringUtils;

public class PlayVideoActivity extends Activity implements Callback,
		OnSeekBarChangeListener, OnBufferingUpdateListener, OnInfoListener,
		OnCompletionListener, OnErrorListener, OnPreparedListener,
		OnClickListener {
	private SurfaceView sView;
	private SurfaceHolder holder;
	private SeekBar bar;
	private MediaPlayer mPlayer;
	private String playUrl;
	private Button button;
	private boolean isPlay;
	private threads thread;
	private View view;
	private boolean isCachOk;
	private FrameLayout fLayout;
	private LinearLayout layout;
	private ImageView imageView;
	private ImageView mOperationBg;
	private ImageView mOperationPercent;
	private View mVolumeBrightnessLayout;
	private AudioManager mAudioManager;
	private boolean isLock;
	/** 最大声音 */
	private int mMaxVolume;
	/** 当前声音 */
	private int mVolume = -1;
	/** 当前亮度 */
	private float mBrightness = -1f;
	private GestureDetector mGestureDetector;
	private TextView play_tiem, endTime;
	private TextView nametTextView;
	private long pauseSize;
	private boolean isFinish;
	private ArrayList<VideoBean> videos;
	private VideoBean video;
	private int playId;
	private Button previousButton;
	private Button nextButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.play);
		init();
		setName();
	}

	private void init() {
		videos = new ArrayList<VideoBean>();
		Object[] movies = (Object[]) getIntent().getSerializableExtra("videos");
		for (int i = 0; i < movies.length; i++) {
			video = (VideoBean) movies[i];
			videos.add(video);
		}
		playId = getIntent().getIntExtra("id", -1);
		previousButton = (Button) findViewById(R.id.previous);
		previousButton.setOnClickListener(this);
		nextButton = (Button) findViewById(R.id.next);
		nextButton.setOnClickListener(this);
		if (!videos.isEmpty() && videos.size() > 1) {
			isCanOnclick(true);
		}
		imageView = (ImageView) findViewById(R.id.image_lock);
		imageView.setOnClickListener(this);
		imageView.setVisibility(View.GONE);
		sView = (SurfaceView) findViewById(R.id.mSurfaceView);
		holder = sView.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder.setKeepScreenOn(true);
		holder.addCallback(this);
		bar = (SeekBar) findViewById(R.id.seekBar3);
		bar.setOnSeekBarChangeListener(this);
		button = (Button) findViewById(R.id.play);
		button.setOnClickListener(this);
		thread = new threads();
		view = findViewById(R.id.pb);
		view.setVisibility(View.VISIBLE);
		bar.setOnSeekBarChangeListener(this);
		fLayout = (FrameLayout) findViewById(R.id.title_fl);
		fLayout.setVisibility(View.GONE);
		layout = (LinearLayout) findViewById(R.id.buttom_lin);
		layout.setVisibility(View.GONE);
		nametTextView = (TextView) findViewById(R.id.movie_name);
		play_tiem = (TextView) findViewById(R.id.play_time);
		endTime = (TextView) findViewById(R.id.play_end_time);
		mVolumeBrightnessLayout = findViewById(R.id.operation_volume_brightness);
		mOperationBg = (ImageView) findViewById(R.id.operation_bg);
		mOperationPercent = (ImageView) findViewById(R.id.operation_percent);
		mGestureDetector = new GestureDetector(this, new MyGestureListener());
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	private void setName() {
		nametTextView.setText(videos.get(playId).getVideoName());
	}

	private void isCanOnclick(boolean b) {
		if (b) {
			previousButton.setEnabled(true);
			nextButton.setEnabled(true);
		} else {
			previousButton.setEnabled(false);
			nextButton.setEnabled(false);
		}
	}

	private class MyGestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			float mOldX = e1.getX(), mOldY = e1.getY();
			int y = (int) e2.getRawY();
			Display disp = getWindowManager().getDefaultDisplay();
			int windowWidth = disp.getWidth();
			int windowHeight = disp.getHeight();

			if (mOldX > windowWidth * 4.0 / 5)// 右边滑动
				onVolumeSlide((mOldY - y) / windowHeight);
			else if (mOldX < windowWidth / 5.0)// 左边滑动
				onBrightnessSlide((mOldY - y) / windowHeight);

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	/**
	 * 滑动改变声音大小
	 * 
	 * @param percent
	 */
	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;

			// 显示
			mOperationBg.setImageResource(R.drawable.video_volumn_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		// 变更声音
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

		// 变更进度条
		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = findViewById(R.id.operation_full).getLayoutParams().width
				* index / mMaxVolume;
		mOperationPercent.setLayoutParams(lp);
	}

	private void onBrightnessSlide(float percent) {
		if (mBrightness < 0) {
			mBrightness = getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;
			// 显示
			mOperationBg.setImageResource(R.drawable.video_brightness_bg);
			mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		}
		WindowManager.LayoutParams lpa = getWindow().getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		if (lpa.screenBrightness > 1.0f)
			lpa.screenBrightness = 1.0f;
		else if (lpa.screenBrightness < 0.01f)
			lpa.screenBrightness = 0.01f;
		getWindow().setAttributes(lpa);

		ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
		lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * lpa.screenBrightness);
		mOperationPercent.setLayoutParams(lp);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event))
			return true;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (!isLock) {
				fLayout.setVisibility(View.VISIBLE);
				layout.setVisibility(View.VISIBLE);
			}
			imageView.setVisibility(View.VISIBLE);
			break;
		case MotionEvent.ACTION_UP:
			endGesture();
			break;
		default:
			break;
		}
		return super.onTouchEvent(event);
	}

	private void endGesture() {
		mVolume = -1;
		mBrightness = -1f;
		// 隐藏
		disHandler.removeMessages(0);
		disHandler.sendEmptyMessageDelayed(0, 1000);
		disHandler.removeMessages(1);
		disHandler.sendEmptyMessageDelayed(1, 5000);
	}

	private Handler disHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0) {
				mVolumeBrightnessLayout.setVisibility(View.GONE);
			} else {
				fLayout.setVisibility(View.GONE);
				layout.setVisibility(View.GONE);
				imageView.setVisibility(View.GONE);
			}
		}
	};

	private void play() {
		if (playId == -1) {
			Toast.makeText(this, "没有可以播放的地址", Toast.LENGTH_LONG).show();
			return;
		}
		playUrl = videos.get(playId).getVideoPlayUrl();
		isPlay = true;
		mPlayer = new MediaPlayer();
		try {
			mPlayer.reset();
			mPlayer.setDataSource(playUrl);
			mPlayer.setDisplay(sView.getHolder());
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnBufferingUpdateListener(this);
			mPlayer.prepareAsync();
		} catch (Exception e) {
			Toast.makeText(this, "播放出现问题：" + e.toString(), Toast.LENGTH_LONG)
					.show();
		}
	}

	class threads implements Runnable {
		@Override
		public void run() {
			handler.sendEmptyMessage(1);
			handler.postDelayed(thread, 1000);
		}
	}

	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (mPlayer != null) {
				bar.setProgress(mPlayer.getCurrentPosition());
				play_tiem.setText(StringUtils.generateTime(mPlayer
						.getCurrentPosition()));
			}
		};
	};

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			Toast.makeText(this, "网络太慢或者视频已经破损", Toast.LENGTH_LONG).show();
			break;
	
		
	
		}
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			mPlayer.pause();
			view.setVisibility(View.VISIBLE);
			isPlay = false;
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			mPlayer.start();
			view.setVisibility(View.GONE);
			isPlay = true;
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		if (!isCachOk && !isFinish) {
			int size = percent * bar.getMax() / 100;
			if (size < bar.getSecondaryProgress()) {
				return;
			}
			bar.setSecondaryProgress(size);
		}
		if (percent == 100) {
			isCachOk = true;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			mPlayer.seekTo(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		mPlayer.pause();
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mPlayer.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		view.setVisibility(View.VISIBLE);
		play();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mPlayer != null) {
			pauseSize = mPlayer.getCurrentPosition();
			mPlayer.stop();
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mPlayer.start();
		endTime.setText(StringUtils.generateTime(mPlayer.getDuration()));
		bar.setMax(mPlayer.getDuration());
		view.setVisibility(View.GONE);
		new Thread(thread).start();
		if (pauseSize > 0) {
			mPlayer.seekTo((int) pauseSize);
			bar.setProgress((int) pauseSize);
			pauseSize = 0;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.play) {
			if (isPlay) {
				mPlayer.pause();
				button.setBackgroundResource(R.drawable.player_play_highlight);
				isPlay = false;
				bar.setEnabled(false);
			} else {
				button.setBackgroundResource(R.drawable.player_pause_highlight);
				mPlayer.start();
				isPlay = true;
				bar.setEnabled(true);
			}
		}

		if (v.getId() == R.id.image_lock) {
			if (isLock) {
				isLock = false;
				imageView.setBackgroundResource(R.drawable.lock_off);
			} else {
				isLock = true;
				imageView.setBackgroundResource(R.drawable.lock_on);
			}
		}

		if (v.getId() == R.id.next) {
			playId++;
			if (playId >= videos.size()) {
				playId = 0;
			}
			destory();
			play();
			setName();
		}

		if (v.getId() == R.id.previous) {
			playId--;
			if (playId < 0) {
				playId = videos.size() - 1;
			}
			destory();
			play();
			setName();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private void destory() {
		isFinish = true;
		mPlayer.stop();
		mPlayer.release();
		mPlayer = null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destory();
	}
}

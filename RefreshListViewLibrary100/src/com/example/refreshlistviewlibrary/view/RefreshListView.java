package com.example.refreshlistviewlibrary.view;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.refreshlistviewlibrary.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshListView extends ListView implements OnScrollListener {

	private LinearLayout mHanderView;
	private View mCustomHeaderView;
	private int downY = -1;
	private View pullDownview;
	private int measuredHeight;
	private final int PULL_DOWN = 0;
	private final int RELEASE_REFRESH = 1;
	private final int REFRESHING = 2;
	private int currentState = PULL_DOWN;
	private RotateAnimation upAnim; // 向上旋转的动画
	private RotateAnimation downAnim;
	private ImageView ivArrow; // 头布局的箭头
	private TextView tvState;
	private TextView tvLastUpdateTime;
	private ProgressBar mProgressbar;
	private int mListViewYOnScreen = -1;
	private OnRefreshListener monRefreshListener;
	private View mFooterView;
	private int measuredHeight2;
	private boolean isLoadingMore= false;

	public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initHander();
		initFooter();
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHander();
		initFooter();
	}

	public RefreshListView(Context context) {
		super(context);
		initHander();
		initFooter();

	}

	private void initFooter() {
		mFooterView = View.inflate(getContext(), R.layout.refresh_footer_view, null);
		mFooterView.measure(0, 0);
		measuredHeight2 = mFooterView.getMeasuredHeight();
		mFooterView.setPadding(0, -measuredHeight2, 0, 0);
		this.addFooterView(mFooterView);
		this.setOnScrollListener(this);
	}

	// 初始化下拉刷新头布局
	private void initHander() {
		mHanderView = (LinearLayout) View.inflate(getContext(), R.layout.refresh_header_view, null);
		pullDownview = mHanderView.findViewById(R.id.ll_refresh_header_pull_down);
		ivArrow = (ImageView) mHanderView.findViewById(R.id.iv_refresh_header_pull_down_arrow);
		tvState = (TextView) mHanderView.findViewById(R.id.tv_refresh_header_pull_down_state);
		tvLastUpdateTime = (TextView) mHanderView.findViewById(R.id.tv_refresh_header_pull_down_time);
		mProgressbar = (ProgressBar) mHanderView.findViewById(R.id.pb_refresh_header_pull_down);
		tvLastUpdateTime.setText("最后刷新时间:" + getCurrentTime());
		// 测量下拉刷新头的高度

		pullDownview.measure(0, 0);
		measuredHeight = pullDownview.getMeasuredHeight();
		// System.out.println(+measuredHeight);
		pullDownview.setPadding(0, -measuredHeight, 0, 0);
		this.addHeaderView(mHanderView);

		// 初始化动画
		initAnimation();
	}

	private void initAnimation() {
		upAnim = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		upAnim.setDuration(500);
		upAnim.setFillAfter(true);
		downAnim = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		downAnim.setDuration(500);
		downAnim.setFillAfter(true);

	}

	// 添加一个自定义布局
	public void AddCustomHeaderView(View v) {
		this.mCustomHeaderView = v;
		mHanderView.addView(v);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downY = (int) ev.getY();
			break;

		case MotionEvent.ACTION_MOVE:
			if (downY == -1) {
				downY = (int) ev.getY();
			}
			if (currentState == REFRESHING) {
				break;
			}
			// 判断添加的轮播图是否完全显示了，如果没有完全显示，不执行下面下拉头的代码
			// 跳转switch语句，执行父元素的touch事件
			if (mCustomHeaderView != null) {
				int[] location = new int[2];
				if (mListViewYOnScreen == -1) {
					// 获取listview在屏幕中y轴的值

					this.getLocationOnScreen(location);
					mListViewYOnScreen = location[1];
				}
				// 获取mCustomHeaderView在屏幕y轴的值
				mCustomHeaderView.getLocationOnScreen(location);
				int mCustomHeaderViewYOnScreen = location[1];

				if (mListViewYOnScreen > mCustomHeaderViewYOnScreen) {

					break;
				}
			}

			int moveY = (int) ev.getY();
			// 移动的差值
			int diffY = moveY - downY;

			if (diffY > 0 && getFirstVisiblePosition() == 0) {
				int paddingTop = -measuredHeight + diffY;

				if (paddingTop > 0 && currentState != RELEASE_REFRESH) {
					currentState = RELEASE_REFRESH;
					refreshPullDownHeadleState();
				} else if (paddingTop < 0 && currentState != PULL_DOWN) {

					currentState = PULL_DOWN;
					refreshPullDownHeadleState();
				}
				pullDownview.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			downY = -1;

			if (currentState == PULL_DOWN) {
				// 当前状态是下拉刷新状态，把头布局隐藏
				pullDownview.setPadding(0, -measuredHeight, 0, 0);

			} else if (currentState == RELEASE_REFRESH) {
				// 当前状态是释放
				pullDownview.setPadding(0, 0, 0, 0);
				currentState = REFRESHING;
				refreshPullDownHeadleState();
				// 调用用户的回调接口
				if (monRefreshListener != null) {
					monRefreshListener.onPullDownRefresh();
				}
			}
			break;
		}

		return super.onTouchEvent(ev);
	}

	private void refreshPullDownHeadleState() {
		// TODO Auto-generated method stub
		switch (currentState) {
		case PULL_DOWN: // 下拉刷新
			ivArrow.startAnimation(downAnim);
			tvState.setText("下拉刷新");
			break;

		case RELEASE_REFRESH:
			ivArrow.startAnimation(upAnim);
			tvState.setText("释放刷新");
			break;
		case REFRESHING:
			ivArrow.clearAnimation();
			ivArrow.setVisibility(View.INVISIBLE);
			mProgressbar.setVisibility(View.VISIBLE);
			tvState.setText("正在刷新中...");
			break;
		}
	}

	public void onRefreshFinish() {
		if(isLoadingMore){
			isLoadingMore=false;
			mFooterView.setPadding(0, -measuredHeight2, 0, 0);
		}else{
			
			pullDownview.setPadding(0, -measuredHeight, 0, 0);
			currentState = PULL_DOWN;
			mProgressbar.setVisibility(View.INVISIBLE);
			ivArrow.setVisibility(View.VISIBLE);
			tvState.setText("下拉刷新");
			tvLastUpdateTime.setText("最后刷新时间:" + getCurrentTime());

		}
	
	}

	private String getCurrentTime() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		return format.format(new Date());

	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		this.monRefreshListener = listener;
	}

	public interface OnRefreshListener {
		// 当下拉刷新时 触发次方法，实现此方法是抓取数据
		public void onPullDownRefresh();
		public void onLoadingMore();
	}

	/**
	 * scrollState 当前的状态
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		if (scrollState == SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_FLING) {
			int lastVisiblePosition = getLastVisiblePosition();
			if (lastVisiblePosition == getCount() - 1 && !isLoadingMore) {
				isLoadingMore = true;
				mFooterView.setPadding(0, 0, 0, 0);
				this.setSelection(getCount());
				if(monRefreshListener!=null){
					monRefreshListener.onLoadingMore();
				}
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub

	}

}

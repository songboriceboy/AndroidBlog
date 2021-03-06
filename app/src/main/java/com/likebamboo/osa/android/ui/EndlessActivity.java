package com.likebamboo.osa.android.ui;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.widget.AbsListView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.likebamboo.osa.android.R;
import com.likebamboo.osa.android.entity.BaseRsp;
import com.likebamboo.osa.android.exception.ErrorTrans;
import com.likebamboo.osa.android.exception.OsaException;
import com.likebamboo.osa.android.request.RequestManager;
import com.likebamboo.osa.android.request.RequestParams;
import com.likebamboo.osa.android.ui.adapter.BaseAdapter;
import com.likebamboo.osa.android.ui.view.LoadingLayout;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * 分页加载数据Activity基类
 * Created by likebamboo on 2015/5/14.
 */
public abstract class EndlessActivity<T extends BaseRsp> extends NavigationActivity implements AbsListView.OnScrollListener,
        SwipeRefreshLayout.OnRefreshListener {

    /**
     * pageSize ， 默认值 20
     */
    public static final int PAGE_SIZE = 20;

    /**
     * 参数， pageSize ， 默认值 20
     */
    public static final String PARAM_PAGE_SIZE = "pageSize";

    /**
     * 参数，pageNo
     */
    public static final String PARAM_PAGE_NO = "pageNo";

    /**
     * 适配器
     */
    protected BaseAdapter mAdapter = null;

    /**
     * footer
     */
    protected LoadingLayout mFooterView = null;

    /**
     * loading
     */
    @InjectView(android.R.id.empty)
    protected LoadingLayout mLoadingLayout = null;

    /**
     * 页码
     */
    protected int mPageIndex = 0;

    /**
     * 页容量
     */
    protected int mPageSize = PAGE_SIZE;

    /**
     * 是否还有更多
     */
    protected boolean mHasMore = true;

    /**
     * 是否正在加载数据
     */
    protected boolean isLoading = false;

    /**
     * 是否正在下拉刷新数据
     */
    protected boolean isRefreshing = false;

    /**
     * 下拉刷新控件
     */
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        ButterKnife.inject(this);
        // 添加footer
        mFooterView = (LoadingLayout) LayoutInflater.from(this).inflate(R.layout.footer_loading_layout, null);
        // 重试
        mLoadingLayout.setRetryListener(new LoadingLayout.IRetryListener() {
            @Override
            public void onRetry() {
                reloadDatas();
            }
        });
        mFooterView.setRetryListener(new LoadingLayout.IRetryListener() {
            @Override
            public void onRetry() {
                mFooterView.showLoading(true);
                reloadDatas();
            }
        });

        // 下拉刷新
        try {
            mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
            mSwipeRefreshLayout.setOnRefreshListener(this);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public final void setContentView(int layoutId) {
        super.setContentView(layoutId);
    }

    /**
     * 重试
     */
    public void reloadDatas() {
        if (mPageIndex == 0) {
            // 显示加载中的布局
            mLoadingLayout.showLoading(true);
        }
        // 加载数据
        loadDatas();
    }

    /**
     * 网络请求出错时回调
     *
     * @return
     */
    protected final Response.ErrorListener errorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isLoading = false;
                doOnError(ErrorTrans.transToOsaException(error));
            }
        };
    }


    /**
     * 加载数据成功时回调
     *
     * @return
     */
    protected final Response.Listener<T> responseListener() {
        return new Response.Listener<T>() {
            @Override
            public void onResponse(T data) {
                isLoading = false;
                mLoadingLayout.showLoading(false);
                // 如果是刷新数据
                if (isRefreshing) {
                    // 清空现有数据
                    reset();
                    // 停止刷新
                    stopRefresh();
                }
                doOnSuccess(data);
            }
        };
    }


    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        // our handling
        int lastInScreen = firstVisibleItem + visibleItemCount;
        if (lastInScreen >= totalItemCount && mAdapter != null && !mAdapter.isEmpty()) {
            // 加载更多数据。
            loadDatas();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消请求
        RequestManager.cancelAll(this.getClass().getName());
        isLoading = false;
    }

    /**
     * 加载数据
     */
    protected final void loadDatas() {
        // 如果正在加载数据，或者没有更多数据了，
        if (isLoading) {
            return;
        }
        if (!mHasMore) {
            mFooterView.showEmpty(getString(R.string.has_not_more_data));
            return;
        }
        isLoading = true;
        if (mPageIndex > 0) {
            mFooterView.showLoading(true);
        } else {
            mLoadingLayout.showLoading(true);
        }
        // 加载数据
        RequestParams params = new RequestParams();
        params.add(PARAM_PAGE_NO, (mPageIndex + 1) + "").add(PARAM_PAGE_SIZE, "" + mPageSize);
        loadDatas(params);
    }

    @Override
    public void onRefresh() {
        // 如果正在加载数据，不刷新
        if (isLoading) {
            // stop refresh
            stopRefresh();
            return;
        }
        isRefreshing = true;
        // 加载数据
        RequestParams params = new RequestParams();
        params.add(PARAM_PAGE_NO, 1 + "").add(PARAM_PAGE_SIZE, "" + mPageSize);
        loadDatas(params);
    }

    /**
     * 加载失败，显示错误信息
     *
     * @param error
     */
    protected void doOnError(OsaException error) {
        // 如果加载第一页的数据出错，显示错误信息
        if (mPageIndex == 0) {
            mLoadingLayout.showError(error.getMessage() + "\n" + getString(R.string.retry));
            mFooterView.showLoading(false);
            return;
        }
        mFooterView.showError(error.getMessage() + "\n" + getString(R.string.retry));
    }

    /**
     * 显示服务器返回的信息
     *
     * @param data
     */
    protected void showMessage(T data) {
        // 如果加载第一页的数据为空
        if (mPageIndex == 0) {
            mLoadingLayout.showEmpty(data == null ? getString(R.string.can_not_find_data) : data.getMessage());
        } else {
            mFooterView.showEmpty(getString(R.string.has_not_more_data));
        }
    }

    /**
     * 重置
     */
    protected void reset() {
        // 清空现有数据
        if (mAdapter != null) {
            mAdapter.clear();
        }
        isLoading = false;
        mHasMore = true;
        mPageIndex = 0;
    }

    /**
     * stopRefresh
     */
    protected void stopRefresh() {
        isRefreshing = false;
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    protected abstract void loadDatas(RequestParams params);

    /**
     * 加载数据成功回调
     */
    protected abstract void doOnSuccess(T data);

    /**
     * layoutId
     */
    protected abstract int getLayoutId();

}

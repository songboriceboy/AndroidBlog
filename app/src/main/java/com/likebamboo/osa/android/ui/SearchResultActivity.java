package com.likebamboo.osa.android.ui;

import android.os.Bundle;
import android.text.TextUtils;

import com.likebamboo.osa.android.request.RequestParams;
import com.likebamboo.osa.android.request.RequestUrl;

import java.net.URLEncoder;

/**
 * 搜索结果界面
 */
public class SearchResultActivity extends BlogListActivity {

    /**
     * 搜索关键字
     */
    public static final String EXTRA_SEARCH_KEY = "extra_search_key";

    /**
     * 搜索博客的关键字
     */
    private String mSearchKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SEARCH_KEY)) {
            mSearchKey = getIntent().getStringExtra(EXTRA_SEARCH_KEY);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void addParams(RequestParams params) {
        // 关键字
        if (!TextUtils.isEmpty(mSearchKey)) {
            // fix 使用一个新的局部变量key,防止下拉刷新或者加载更多的时候mSearchKey不同。
            String key = mSearchKey;
            try {
                key = URLEncoder.encode(mSearchKey, "UTF-8");
            } catch (Exception e) {
            }
            params.add("key", key);
        }
    }

    @Override
    public String getRequestUrl() {
        // 改为搜索url
        return RequestUrl.BLOG_SEARCH_URL;
    }
}

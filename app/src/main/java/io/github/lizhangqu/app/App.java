package io.github.lizhangqu.app;

import android.app.Application;

import com.alibaba.fastjson.JSON;

/**
 * @author lizhangqu
 * @version V1.0
 * @since 2018-04-22 13:24
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            JSON.parse("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

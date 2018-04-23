package io.github.lizhangqu.app;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.alibaba.fastjson.JSON;

/**
 * @author lizhangqu
 * @version V1.0
 * @since 2018-04-22 13:24
 */
public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        MultiDex.install(base);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Object parse = JSON.parse("{\n" +
                    "  \"a\":\"a\",\n" +
                    "  \"b\":1,\n" +
                    "  \"c\":true   \n" +
                    "}");
            Log.e("TAG", "parse901:" + parse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package cn.com.kp.ai.utils;
import android.os.Handler;
import android.os.Message;

import cn.com.kp.ai.consts.NotifyMessage;

public class NotifyUtils {
    private Handler handler;

    public NotifyUtils(Handler handler){
        this.handler = handler;
    }

    public void updateUIContent(String message) {
        Message msg = Message.obtain();
        msg.what = NotifyMessage.UPDATE_CONTENT_UI_MSG;
        msg.obj = message;

        handler.sendMessage(msg);
    }
}

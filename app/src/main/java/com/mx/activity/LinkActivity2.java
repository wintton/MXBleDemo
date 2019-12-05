package com.mx.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mx.ble2until.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LinkActivity2 extends Activity {

    private static TextView bleStatusText, consleText;
    private  static EditText strEdit;
    private static Button strSendBtn;
    private static StringBuffer consoleStrBuff;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static Date date;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link2);

        bleStatusText = (TextView) this.findViewById(R.id.link_info_text);
        consleText = (TextView) this.findViewById(R.id.show_msg_text);
        strEdit = (EditText) this.findViewById(R.id.ble_cmd_edit1);
        strSendBtn = (Button) this.findViewById(R.id.send_btn1);

        strSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String stateStr = bleStatusText.getText().toString();
                if (stateStr.indexOf("成功") < 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(LinkActivity2.this);
                    builder.setTitle("提示");
                    builder.setMessage("当前设备未连接成功，无法发送");
                    builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    builder.create().show();
                    return;
                }
                String sendStr = strEdit.getText().toString();
                if (sendStr.length() > 0){
                    MainActivity2.ble4Util.send(sendStr);
                    appendConsole("发送：" + sendStr);
                    Toast.makeText(LinkActivity2.this,"指令已发送",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(LinkActivity2.this,"发送指令不能为空！",Toast.LENGTH_SHORT).show();
                }
            }
        });

        bleStatusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bleStatusText.getText().toString().indexOf("成功") < 0){
                    AlertDialog.Builder builder = new AlertDialog.Builder(LinkActivity2.this);
                    builder.setTitle("提示");
                    builder.setMessage("断开当前连接？");
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity2.ble4Util.disconnect();
                            LinkActivity2.this.finish();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    builder.create().show();
                }
            }
        });
    }

    public static void appendConsole(String msg){
        if (consoleStrBuff == null) consoleStrBuff = new StringBuffer();
        if (consoleStrBuff.length() > 1024) consoleStrBuff.delete(0,200);
        if (date == null) date = new Date();
        date.setTime(System.currentTimeMillis());
        consoleStrBuff.append( dateFormat.format(date) + ":" + msg + "\n");
        consleText.setText(consoleStrBuff);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity2.ble4Util.disconnect();
    }

    public static Handler linkHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 99: {
                    //连接
                    if(bleStatusText != null){
                        bleStatusText.setText(message.obj.toString());
                    }
                }
                break;
                case 101: {
                    //接收到数据
                    String strData =  message.obj.toString();
                    if (consleText != null){
                        appendConsole("接收：" + strData );
                    }
                }
                break;
            }

            return false;
        }
    });


}

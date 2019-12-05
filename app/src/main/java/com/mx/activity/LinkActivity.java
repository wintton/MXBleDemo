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

public class LinkActivity extends Activity {

    private static TextView bleStatusText, consleText;
    private  static EditText strEdit;
    private static Button strSendBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        bleStatusText = (TextView) this.findViewById(R.id.link_info_text);
        consleText = (TextView) this.findViewById(R.id.show_msg_text);
        strEdit = (EditText) this.findViewById(R.id.ble_cmd_edit1);
        strSendBtn = (Button) this.findViewById(R.id.send_btn1);

        strSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (strEdit.getText().toString().length() > 0){
                    MainActivity.ble2until.sendBleMsg(strEdit.getText().toString());
                }else {
                    Toast.makeText(LinkActivity.this,"发送指令不能为空！",Toast.LENGTH_SHORT).show();
                }
            }
        });


        bleStatusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bleStatusText.getText().toString().equals("已连接")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(LinkActivity.this);
                    builder.setTitle("提示");
                    builder.setMessage("断开当前连接？");
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.ble2until.disconnect();
                            LinkActivity.this.finish();
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    builder.create().show();
                }else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LinkActivity.this);
                    builder.setTitle("提示");
                    builder.setMessage("重新连接？");
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.ble2until.reconnect();
                            bleStatusText.setText("连接中");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.ble2until.disconnect();
    }

    public static Handler linkHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 99: {
                    //连接
                    if(bleStatusText != null){
                        bleStatusText.setText("已连接");
                    }
                }
                break;
                case 100: {
                    //断开连接
                    if(bleStatusText != null) {
                        bleStatusText.setText("已断开连接");
                    }
                }
                break;
                case 101: {
                    //接收到数据
                    String strData =  message.obj.toString();

                    if (consleText != null){
                        consleText.setText(strData);
                    }
                }
                break;
            }

            return false;
        }
    });


}

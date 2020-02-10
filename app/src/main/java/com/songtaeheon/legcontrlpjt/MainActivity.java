package com.songtaeheon.legcontrlpjt;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothService btService = null;
    private final String TAG = "TAGMainActivity";
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_CONNECT_DEVICE = 4;

    // 상태를 나타내는 상태 변수
//    private static final int STATE_NONE = 0; // we're doing nothing
//    private static final int STATE_LISTEN = 1; // now listening for incoming connections
//    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
//    private static final int STATE_CONNECTED = 3; // now connected to a remote device

    public static final int MODE_REQUEST = 4 ; // button state
    public static final int MESSAGE_WRITE = 5 ; // button state
    public static final int MESSAGE_READ = 6 ; // button state
    public static final int MODE_NOT_CONNECTED = 7 ; // button state



    // synchronized flags
    private static final int STATE_SENDING = 1 ;
    private static final int STATE_NO_SENDING = 2 ;

    private static final int BUTTON_STOP = 11 ;
    private static final int BUTTON_MOVE_HELP = 12 ;
    private static final int BUTTON_MOVE_SIMPLE = 13 ;

    private int mSendingState ;
    private boolean isSending = false;


    private StringBuffer mOutStringBuffer;

    public boolean isConnected = false;
    private int mSelectedBtn;
    Button btn_connect;
    Button btn_stop;
    Button btn_moveHelp;
    Button btn_moveSimple;

    TextView tv_state;
    TextView tv_mode;
    TextView tv_battery;


    @SuppressLint("HandlerLeak")
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final int what = msg.what;
            switch(what) {
                case MESSAGE_READ:
                    Log.d(TAG, "handle readMsg : " + msg.obj);
                    if(isSending){
                        //보내는 메시지 핸들링,
                        if(mSelectedBtn == BUTTON_STOP){
                            tv_mode.setText(R.string.text_mode_stop);
                        }else if(mSelectedBtn == BUTTON_MOVE_HELP){
                            tv_mode.setText(R.string.text_mode_move_help);
                        }else if(mSelectedBtn == BUTTON_MOVE_SIMPLE){
                            tv_mode.setText(R.string.text_mode_move_simple);
                        }
                        isSending = false;
                    }
                    String readMsg = (String)msg.obj;
                    char c = readMsg.charAt(0);
                    if(c == 'g'){
                        tv_battery.setText(R.string.text_battery_good);
                    }else if(c == 'b'){
                        tv_battery.setText(R.string.text_battery_bad);
                    }
                    break;
                case BluetoothService.STATE_CONNECTED:
                    //연결됨!
                    tv_mode.setVisibility(View.VISIBLE);
                    tv_battery.setVisibility(View.VISIBLE);
                    tv_mode.setText(R.string.text_mode_stop);
                    tv_state.setText(getString(R.string.title_connected_to, btService.getName()));
                    btn_stop.setVisibility(View.VISIBLE);
                    btn_moveHelp.setVisibility(View.VISIBLE);
                    btn_moveSimple.setVisibility(View.VISIBLE);
                    btn_connect.setText(R.string.button_disconnect);
                    break;


                case MESSAGE_WRITE:
//                    //보내는 메시지 핸들링,
//                    if(mSelectedBtn == BUTTON_STOP){
//                        tv_mode.setText(R.string.text_mode_stop);
//                    }else if(mSelectedBtn == BUTTON_MOVE_HELP){
//                        tv_mode.setText(R.string.text_mode_move_help);
//                    }else if(mSelectedBtn == BUTTON_MOVE_SIMPLE){
//                        tv_mode.setText(R.string.text_mode_move_simple);
//                    }
                    break;
                case MODE_NOT_CONNECTED:
                    //연결 끊어
                    tv_mode.setVisibility(View.INVISIBLE);
                    tv_battery.setVisibility(View.INVISIBLE);
                    tv_mode.setText(R.string.text_mode_default);
                    tv_state.setText(R.string.title_not_connected);
                    btn_stop.setVisibility(View.INVISIBLE);
                    btn_moveHelp.setVisibility(View.INVISIBLE);
                    btn_moveSimple.setVisibility(View.INVISIBLE);
                    btn_connect.setText(R.string.button_connect);

            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // BluetoothService 클래스 생성
        if(btService == null) {
            btService = new BluetoothService(this, mHandler);
            mOutStringBuffer = new StringBuffer("");
        }


        btn_connect = findViewById(R.id.button_connect);
        btn_stop = findViewById(R.id.button_stop);
        btn_moveHelp = findViewById(R.id.button_move_help);
        btn_moveSimple = findViewById(R.id.button_move_simple);
        tv_state = findViewById(R.id.text_state);
        tv_mode = findViewById(R.id.text_mode);
        tv_battery = findViewById(R.id.text_battery);

        btn_connect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(btService.getState() == BluetoothService.STATE_CONNECTED){
                    btService.disconnect();
                }else {
                    //연결되어 있는 경
                    if (btService.getDeviceState()) {
                        btService.enableBluetooth();
                    } else {
                        Log.e(TAG, "btService.getDeivecState return error");
                        finish();
                    }
                }
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                sendMessage("s", MODE_REQUEST);
                mSelectedBtn = BUTTON_STOP;
            }
        });
        btn_moveHelp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                sendMessage("h", MODE_REQUEST);
                mSelectedBtn = BUTTON_MOVE_HELP;
            }
        });
        btn_moveSimple.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                sendMessage("g", MODE_REQUEST);
                mSelectedBtn = BUTTON_MOVE_SIMPLE;
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK){
                    //확인 눌렀을 때!!
                    btService.scanDevice();
                }else{
                    Log.e(TAG, "bluetooth is not enabled");
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if(resultCode == Activity.RESULT_OK){
                    btService.getDeviceInfo(data);
                }else{
                    Log.e(TAG, "bluetooth is not enabled");
                }
                break;
        }
    }

    private synchronized void sendMessage( String message, int mode ) {

        if ( mSendingState == STATE_SENDING ) {
            try {
                wait() ;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mSendingState = STATE_SENDING ;
        isSending = true;

        // Check that we're actually connected before trying anything
        if ( btService.getState() != BluetoothService.STATE_CONNECTED ) {
            mSendingState = STATE_NO_SENDING ;
            return ;
        }

        // Check that there's actually something to send
        if ( message.length() > 0 ) {
        // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes() ;
            btService.write(send, mode) ;

             // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0) ;

        }

        mSendingState = STATE_NO_SENDING ;
        notify() ;
    }
}

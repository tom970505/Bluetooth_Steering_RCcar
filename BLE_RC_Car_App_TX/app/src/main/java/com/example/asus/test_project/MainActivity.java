package com.example.asus.test_project;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.lang.Math.abs;
import static java.lang.Math.round;

public class MainActivity extends AppCompatActivity {
    private static final UUID BT_uuid;
    static {
        BT_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }
    //BTConnect btcon;
    SensorManager sensorManager;
    boolean accelerometerPresent; // flag: 控制
    Sensor accelerometerSensor;
    SeekBar speedBar;
    Switch foward_reverse;
    ImageButton settings;
    boolean flag=false;

    TextView txtAngle,txtSpeed;                //  輸出方向盤角度
    TextView ttv;
    String data="";
    double rad=0;
    int speed=0;
    int turn = 0;
    private Event_Bluetooth event_bluetooth;    //處理藍芽事件

    ImageView mySteer;
    Matrix vMatrix;
    Bitmap vBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);  // 改為橫向
        initializeBluetooth();

        mySteer = (ImageView) findViewById(R.id.mySteer);
        vBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.p01);
        vMatrix = new Matrix();


        txtAngle = (TextView) findViewById(R.id.steeringAngle);
        txtSpeed =(TextView) findViewById(R.id.speed);
        txtSpeed.setText("時速:"+Integer.toString(speed) );
        settings=(ImageButton) findViewById(R.id.setting);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    event_bluetooth.disconnect();
                } catch (Exception ee) {

                }
                Intent myInt = new Intent(MainActivity.this,bluetooth_setting.class); //
                //startActivity(myInt);
                startActivityForResult(myInt, 12);
            }
        });
        speedBar =(SeekBar) findViewById(R.id.speedPanel);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { // 分成四段變數的seek bar
                speed=(speedBar.getProgress()) * 20;
                if(flag) {
                    speed = speed * (-1);
                }
                txtSpeed.setText("時速:"+Integer.toString(speed) );
                data = Integer.toString(speed)+" "+Double.toString(Math.round(Math.toDegrees(rad)));
                ttv.setText(data.toString());
                if(event_bluetooth.address!=null)
                    event_bluetooth.sendMsg(data);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                data = Integer.toString(speed)+" "+Double.toString(Math.round(Math.toDegrees(rad)));
                if(event_bluetooth.address!=null)
                    event_bluetooth.sendMsg(data);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {   // 放開 會變成0
                speedBar.setProgress(0);
                speed = (speedBar.getProgress());
                txtSpeed.setText("時速: " +Integer.toString(speed) );
                data = Integer.toString(speed)+" "+Double.toString(Math.round(Math.toDegrees(rad)));
                ttv.setText(data.toString());
                if(event_bluetooth.address==null)
                    event_bluetooth.sendMsg(data);
            }
    });
        foward_reverse=(Switch)findViewById(R.id.foward_reverse);
        foward_reverse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isChecked()) {
                    flag =true;
                }else{
                    flag =false;
                }
            }
        });


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensorList.size() > 0){
            accelerometerPresent = true;
            accelerometerSensor = sensorList.get(0);     // 在主程式就準備了
            //  這裡可以輸出資訊
        } else{
            accelerometerPresent = false;
        }
        ttv = (TextView) findViewById(R.id.ttv);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater minf =getMenuInflater();
        minf.inflate(R.menu.main_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem Item){
        switch(Item.getItemId()){
            case R.id.help:
                try{
                    event_bluetooth.sendMsg("@@");
                }catch(Exception ee){

                }
                break;
        }
        return super.onOptionsItemSelected(Item);
    }


    private void initializeBluetooth() {
        event_bluetooth = new Event_Bluetooth();    //建立物件
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if(accelerometerPresent){
            sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);  // 重新註冊 listener
            Toast.makeText(this, "註冊  加速度計", Toast.LENGTH_LONG).show();
        }

    }

    private SensorEventListener accelerometerListener = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub

            if (Sensor.TYPE_ACCELEROMETER != event.sensor.getType()) {
                return;
            }
            float[] values = event.values;
            float ax = values[0];
            float ay = values[1];
            float az = values[2];
            double g = Math.sqrt(ax * ax + ay * ay+az*az);//+az*az
            double cos = ay / g;
            if (cos > 1) {
                cos = 1;
            } else if (cos < -1) {
                cos = -1;
            }
            rad = Math.acos(cos);
            if (ax < 0) {
                rad = 2 * Math.PI - rad;
            }
            int uiRot = getWindowManager().getDefaultDisplay().getRotation();
            double uiRad = Math.PI / 2 * uiRot;
            rad -= uiRad;

            if(Math.toDegrees(rad)<3 && Math.toDegrees(rad)>-3) rad=Math.toRadians(0);
            if(Math.toDegrees(rad)>60) rad=Math.toRadians(60);
            if(Math.toDegrees(rad)<-60) rad=Math.toRadians(-60);

            txtAngle.setText("角度: "+Double.toString(round(Math.toDegrees(rad))));      //  負代表順時針
            data = Integer.toString(speed)+" "+Double.toString(Math.round(Math.toDegrees(rad)));
            ttv.setText(data.toString());

            if(event_bluetooth.address != null)
                event_bluetooth.sendMsg(data);
            turn =(int) Math.toDegrees(rad);
            turn = turn*(-1);
            vMatrix.setRotate(turn);
            Bitmap vB2 = Bitmap.createBitmap(vBitmap
                    , 0
                    , 0
                    , vBitmap.getWidth()   // 寬度
                    , vBitmap.getHeight()  // 高度
                    , vMatrix
                    , true
            );

            mySteer.setImageBitmap(vB2);
        }};

    private class Event_Bluetooth extends BroadcastReceiver {

        private BluetoothAdapter bluetoothAdapter;              //   本機藍芽設備
        private BluetoothDevice oneDevice;
        private BluetoothSocket socket;
        OutputStream outstream;
        boolean bug1=false;
        public String address;

        public Event_Bluetooth() {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);       //  擁有搜尋周圍藍芽設備的功能
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);    //  若藍芽設備狀態改變(ex:開→關)
            registerReceiver(this, filter);                                     //  註冊這個功能

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();                //  取得自身的藍芽設備

            if (bluetoothAdapter == null){                                         //  若為null，代表本機無藍芽設備
                Toast.makeText(MainActivity.this, "this device ", Toast.LENGTH_LONG).show();
                //finish();//關閉應用程式
            }
            if (bluetoothAdapter.isEnabled() == false)                             //   若本機藍芽設備未開啟
                openBluetooth();

        }

        private void openBluetooth(){                                              //開啟藍芽設備
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);    //  請求使用者開啟藍芽設備
            startActivityForResult(intent, 1);                                            //  執行此請求，並回傳使用者的選擇。
            //  回傳的訊息將由onActivityResult接收，而onActivityResult為Activity內的方法
        }

        //---------------------------------------------------對單一設備---------------------------------------------------------

        public void connectBluetooth() throws IOException {                           //   連接設備
            socket = oneDevice.createRfcommSocketToServiceRecord(BT_uuid);
            socket.connect();
        }

        public void disconnect() {                                                  //    關閉連接
            try {
                this.socket.close();
            } catch (Exception e) {
            }
        }
        public void sendMsg(String str){
            if(bluetoothAdapter.isEnabled() && !bug1) {
                try {
                    outstream = socket.getOutputStream();
                    outstream.write(str.getBytes());
                    outstream.flush();
                } catch (Exception e) {
                    disconnect();
                    Toast.makeText(MainActivity.this, "未連接至設備 請進入設定", Toast.LENGTH_LONG).show();
                }
            }
        }
        @Override
        public void onReceive(Context context, Intent intent){     //   此為BroadcastReceiver提供的方法，必須寫出來。每次有IntentFilter時都會自動呼叫這個方法接收
            // TODO Auto-generated method stub
            String action = intent.getAction();                                                        //        儲存IntentFilter所傳進來的要求
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){                       //      若藍芽設備狀態改變(ex:開→關)
                if (bluetoothAdapter.getState() == bluetoothAdapter.STATE_OFF) {
                    openBluetooth();
                }
            }
        }

    }

    @Override
    protected void onDestroy(){                                                                         //    一旦使用者結束應用程式
        // TODO Auto-generated method stub
        super.onDestroy();

        if(accelerometerPresent){
            sensorManager.unregisterListener(accelerometerListener);   // 銷毀listener
            Toast.makeText(this, "註銷 加速度計", Toast.LENGTH_LONG).show();
        }
        try {
            event_bluetooth.disconnect();
        } catch (Exception ee) {

        }
        //若有藍芽裝置且藍芽裝置已開啟
        if (event_bluetooth.bluetoothAdapter!=null && event_bluetooth.bluetoothAdapter.isEnabled()==true) {
            event_bluetooth.bluetoothAdapter.cancelDiscovery();                                          //  關閉搜索藍芽裝置功能，防止效能消耗
        }
        unregisterReceiver(event_bluetooth);                                                            //  解除註冊
    }

    //接收Intent的請求
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        //判斷是否為開啟藍芽的Intent
        if (requestCode==1) {
            if (resultCode==RESULT_CANCELED) {                     //  若使用者按下取消，不開啟藍芽
                Toast.makeText(this, "請開啟藍芽", Toast.LENGTH_LONG).show();
                //finish();
            }
        }
        if (requestCode==12) {
            try {
                event_bluetooth.address = data.getExtras().getString("address");
                if (event_bluetooth.address != null) {
                    try {
                        event_bluetooth.oneDevice = event_bluetooth.bluetoothAdapter.getRemoteDevice(event_bluetooth.address); //"98:D3:31:FB:50:82"
                        event_bluetooth.connectBluetooth();
                        event_bluetooth.sendMsg("@@");
                    } catch (Exception e) {
                        Toast.makeText(this, "未連接至設備", Toast.LENGTH_LONG).show();
                        try {
                            event_bluetooth.disconnect();
                        } catch (Exception ee) {

                        }
                    }
                }
            }catch(Exception ee){
                Toast.makeText(this, "未連接至設備", Toast.LENGTH_LONG).show();
            }
        }
    }


}


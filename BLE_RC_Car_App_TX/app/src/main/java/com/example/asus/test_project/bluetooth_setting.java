package com.example.asus.test_project;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class bluetooth_setting extends AppCompatActivity {
    private static final UUID BT_uuid;
    static {
        BT_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    private Event_Button event_button;          //處理按鈕事件
    private Event_ListView event_listView;      //處理ListView事件
    private Event_Bluetooth event_bluetooth;    //處理藍芽事件

    EditText ed;
    boolean start = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_bluetooth_setting);
        initializeBluetooth();
        initializeButtonEvent();
        initializeListViewEvent();
        ed = (EditText) findViewById(R.id.editMsg);
        event_listView.showConnectedDeviceName();//開啟後自動顯示已連接過的藍芽裝置名稱
        start =true;
    }
    private void initializeButtonEvent() {
        event_button = new Event_Button();      //建立物件
    }

    private void initializeListViewEvent() {
        event_listView = new Event_ListView();    //建立物件
    }

    private void initializeBluetooth() {
        event_bluetooth = new Event_Bluetooth();    //建立物件
    }

    //按鈕相關事件處理
    private class Event_Button implements View.OnClickListener {

        private Button btn_showConnectedDevices;    //「顯示已經連線過的藍芽」按鈕
        private Button btn_searchDevices;             //「搜尋附近的藍芽裝置」按鈕
        private Button btn_addDevice;
        private Button btn_giveSearch;
        private Button btn_sendEdMsg;
        private Button btn;

        public Event_Button() {
            //獲取按鈕的目標和註冊按鈕事件
            btn_showConnectedDevices = (Button) findViewById(R.id.btn_showConnectedDevices);
            btn_searchDevices = (Button) findViewById(R.id.btn_searchDevices);
            btn_addDevice = (Button) findViewById(R.id.addDevice);

            btn_giveSearch = (Button) findViewById(R.id.btn_givenSearch);
            btn_sendEdMsg =(Button) findViewById(R.id.sendMsg);
            btn =(Button) findViewById(R.id.back);

            btn_showConnectedDevices.setOnClickListener(this);
            btn_searchDevices.setOnClickListener(this);
            btn_sendEdMsg.setOnClickListener(this);
            btn_giveSearch.setOnClickListener(this);
            btn_addDevice.setOnClickListener(this);
            btn.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btn_showConnectedDevices:
                    showConnectedDevice();
                    break;
                case R.id.btn_searchDevices:
                    searchDevice();
                    break;
                case R.id.sendMsg:
                    sendMsg();
                    break;
                case R.id.btn_givenSearch:
                    givenSearch();
                    break;
                case R.id.addDevice:
                    addDevice();
                    break;
                case R.id.back:
                    goBack();
                    break;
            }
        }

        private void showConnectedDevice(){    //顯示已連接過的裝置名稱
            event_listView.showConnectedDeviceName();
        }

        private void searchDevice(){           //搜尋周圍的藍芽裝置
            event_bluetooth.searchBluetooth();
        }

        private void givenSearch(){           //  手機可以被搜尋
            event_bluetooth.openDiscover(30);
        }

        private void addDevice(){
            Intent settintIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(settintIntent);
        }

        private void sendMsg(){
            // 開始傳訊
            try{
                event_bluetooth.sendMsg(event_listView.otDeviceAd);
                ed.setText("");
            }catch (Exception e){
                Toast.makeText(bluetooth_setting.this, "未連線 無法傳訊" , Toast.LENGTH_SHORT).show();
            };
        }
        private void goBack(){

            Intent intent = new Intent();
            intent.putExtra("address",event_listView.otDeviceAd);
            setResult(12,intent);
            finish();
        }
    }

    //ListView相關事件處理
    private class Event_ListView implements AdapterView.OnItemClickListener {

        private ListView listView_showDevice;
        private ArrayAdapter<String> deviceName;        //儲存藍芽裝置名稱
        public String otDeviceAd;                       // 遠端裝置名稱

        public Event_ListView() {
            listView_showDevice = (ListView) findViewById(R.id.listView_showDevice);
            listView_showDevice.setOnItemClickListener(this);
        }

        private void showConnectedDeviceName(){         //顯示連接過的藍芽裝置名稱
            deviceName = event_bluetooth.getConnectedDeviceName();
            listView_showDevice.setAdapter(deviceName);
        }

        private void showSearchDeviceName(){              //顯示搜索到的藍芽裝置名稱
            deviceName = event_bluetooth.deviceName;
            listView_showDevice.setAdapter(deviceName);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {    //顯示是哪個ListView被按下
            String itemString = parent.getItemAtPosition(position).toString();
            Toast.makeText(bluetooth_setting.this, "已點選" + itemString, Toast.LENGTH_SHORT).show();
            // 1. 建立連結  (flag)
            // 2.  才能傳訊
            otDeviceAd =event_bluetooth.getMAC(position);
            event_bluetooth.sendMsg(otDeviceAd);
            //start = true;
        }
    }

    //藍芽相關事件處理
    private class Event_Bluetooth extends BroadcastReceiver {

        private BluetoothAdapter bluetoothAdapter;              //   本機藍芽設備
        private ArrayAdapter<String> deviceName = new ArrayAdapter<String> (bluetooth_setting.this, android.R.layout.simple_list_item_1);    //   儲存藍芽設備名稱
        private ArrayAdapter<String> deviceMAC = new ArrayAdapter<String>(bluetooth_setting.this, android.R.layout.simple_list_item_2);
        private Set<BluetoothDevice> pairedDevice;              //   儲存藍芽設備

        private BluetoothDevice oneDevice;
        private BluetoothSocket socket;
        OutputStream outstream;

        public Event_Bluetooth() {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);       //  擁有搜尋周圍藍芽設備的功能
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);    //  若藍芽設備狀態改變(ex:開→關)
            registerReceiver(this, filter);                                     //  註冊這個功能

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();                //  取得自身的藍芽設備

            if (bluetoothAdapter == null){                                         //  若為null，代表本機無藍芽設備
                Toast.makeText(bluetooth_setting.this, "this device ", Toast.LENGTH_LONG).show();
                finish();//關閉應用程式
            }
            if (bluetoothAdapter.isEnabled() == false){                              //   若本機藍芽設備未開啟
                openBluetooth();
            }
        }

        private void openBluetooth(){                                              //開啟藍芽設備
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);    //  請求使用者開啟藍芽設備
            startActivityForResult(intent, 1);                                            //  執行此請求，並回傳使用者的選擇。
            //  回傳的訊息將由onActivityResult接收，而onActivityResult為Activity內的方法
        }

        private ArrayAdapter<String> getConnectedDeviceName(){                          //  得到已連接過的藍芽裝置名稱
            pairedDevice = bluetoothAdapter.getBondedDevices();                         //  取得和本機連接過的藍芽設備
            deviceName.clear();                                                        // 先清除藍芽設備名稱的內容，否則若重複呼叫則會出現相同的裝置
            deviceMAC.clear();

            if (pairedDevice.size() > 0){                                               //    若有已連接過的藍芽裝置
                for (BluetoothDevice device : pairedDevice) {
                    this.deviceName.add(device.getName());                            //      將裝置名稱儲存
                    this.deviceMAC.add(device.getAddress());
                }
            }
            return this.deviceName;                                                   //     回傳藍芽裝置名稱
        }

        private void searchBluetooth(){                                                //    搜尋藍芽裝置
            deviceName.clear();                                                      //      先清空儲存的名稱
            deviceMAC.clear();
            bluetoothAdapter.cancelDiscovery();                                       //      先取消搜尋藍芽，防止使用者連按造成效能嚴重消耗
            bluetoothAdapter.startDiscovery();                                        //       開始搜尋藍芽。一次搜尋時間為12秒
        }

        private void openDiscover(int time){                                          //       使藍芽裝置可被探索
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);   //       請求探索
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, time);        //       加入探索功能，並選擇探索幾秒。最大值為300秒
            startActivity(intent);                                                               //       開始探索
        }

        private void closeBluetoothDevice() {
            bluetoothAdapter.disable();                                              //     關閉藍芽
        }
        //---------------------------------------------------對單一設備---------------------------------------------------------
        public String getMAC(int position) {
            return deviceMAC.getItem(position);
        }

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
        private void sendMsg(String address){
            oneDevice = bluetoothAdapter.getRemoteDevice(address);
            try{
                connectBluetooth();
                outstream = socket.getOutputStream();
                if(!start) {
                    outstream.write(ed.getText().toString().trim().getBytes());
                }else{
                    outstream.write(("@@").getBytes());
                    outstream.flush();
					outstream.write(ed.getText().toString().trim().getBytes());
                    start = false;
                }
                disconnect();
                outstream.flush();
            }catch (Exception e){
                Toast.makeText(bluetooth_setting.this, "no Send", Toast.LENGTH_LONG).show();
                //start = true;
            }
        }
        @Override
        public void onReceive(Context context, Intent intent){     //   此為BroadcastReceiver提供的方法，必須寫出來。每次有IntentFilter時都會自動呼叫這個方法接收
            // TODO Auto-generated method stub

            String action = intent.getAction();                                                        //        儲存IntentFilter所傳進來的要求
            if (action.equals(BluetoothDevice.ACTION_FOUND) == true) {                               //       若要求為搜索藍芽設備
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);   //         儲存搜索到的藍芽裝置
                deviceName.add(device.getName());                                                     //         將藍芽裝置名稱儲存
                deviceMAC.add(device.getAddress());
                event_listView.showSearchDeviceName();                                                 //           並顯示
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){                       //      若藍芽設備狀態改變(ex:開→關)
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
                finish();
            }
            else if (resultCode==RESULT_OK){                          //  若使用者按下確定，開啟藍芽
                //event_listView.showConnectedDeviceName();//開啟後自動顯示已連接過的藍芽裝置名稱
            }
        }
    }
}

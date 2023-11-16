package com.example.textiles_music;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.textiles_music.adapters.BasicList;
import com.example.textiles_music.audio.AudioModel;
import com.example.textiles_music.simpleble.interfaces.BleCallback;
import com.example.textiles_music.simpleble.models.BluetoothLE;
import com.example.textiles_music.simpleble.utils.BluetoothLEHelper;
import com.example.textiles_music.simpleble.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    BluetoothLEHelper ble;
    AlertDialog dAlert;
    ListView listBle;
    TextView receivedDataView;

    ImageButton prevButton;
    ImageButton nextButton;
    ImageButton playButton;

    Boolean musicFound;
    TextView songTitle, currentTimeTv, totalTimeTv;

    SeekBar seekBar;
    AudioModel currentSong;
    MediaPlayer mediaPlayer = new MediaPlayer();

    ArrayList<AudioModel> songsList = new ArrayList<>();

    Integer songIndex = 0;

    BluetoothManager bluetoothManager;

    Boolean musicVisible = false;
    RelativeLayout musicMain;
    RelativeLayout mainWindow;

    final String SWIPE_LEFT_ACTION = "slide1";
    final String SWIPE_RIGHT_ACTION = "slide2";
    final String TAP_ACTION = "single click";
    final String DOUBLE_TAP_ACTION = "double click";
    final String TRIPLE_TAP_ACTION = "triple click";

    final String LONG_PRESS_ACTION = "long press";

    ArrayList<Integer> backgroundColors = new ArrayList<>();
    List<BluetoothDevice> connectedDevices = new ArrayList<>();

    AudioManager audioManager;

    ListView listSongs;

    Boolean playSongs;

    ImageView backButton;


    private AlertDialog setDialogInfo(String title, String message, boolean btnVisible) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_standard, null);

        TextView btnNeutral = view.findViewById(R.id.btnNeutral);
        TextView txtTitle = view.findViewById(R.id.txtTitle);
        TextView txtMessage = view.findViewById(R.id.txtMessage);

        txtTitle.setText(title);
        txtMessage.setText(message);

        if (btnVisible) {
            btnNeutral.setVisibility(View.VISIBLE);
        } else {
            btnNeutral.setVisibility(View.GONE);
        }

        btnNeutral.setOnClickListener(view1 -> {
            dAlert.dismiss();
        });

        builder.setView(view);
        return builder.create();
    }

    private void setList() {

        ArrayList<BluetoothLE> aBleAvailable = new ArrayList<>();

        if (ble.getListDevices().size() > 0) {
            for (int i = 0; i < ble.getListDevices().size(); i++) {
                String btName = ble.getListDevices().get(i).getName();
                if (btName != null) {
                    aBleAvailable.add(new BluetoothLE(btName, ble.getListDevices().get(i).getMacAddress(), ble.getListDevices().get(i).getRssi(), ble.getListDevices().get(i).getDevice()));
                }
            }

            BasicList mAdapter = new BasicList(this, R.layout.simple_row_list, aBleAvailable) {
                @Override
                public void onItem(Object item, View view, int position) {

                    TextView txtName = view.findViewById(R.id.txtText);

                    String btName = ((BluetoothLE) item).getName();
                    String aux = btName + "    " + ((BluetoothLE) item).getMacAddress();
                    txtName.setText(aux);

                }
            };

            listBle.setAdapter(mAdapter);
            listBle.setOnItemClickListener((parent, view, position, id) -> {
                BluetoothLE itemValue = (BluetoothLE) listBle.getItemAtPosition(position);
                ble.connect(itemValue.getDevice(), bleCallbacks());
                System.out.println("why disconnect?");
                System.out.println(itemValue.getDevice());
            });
        } else {
            dAlert = setDialogInfo("Ups", "We do not find active devices", true);
            dAlert.show();
        }
    }

    private BleCallback bleCallbacks() {

        return new BleCallback() {

            @Override
            public void onBleConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onBleConnectionStateChange(gatt, status, newState);

                System.out.println("NEW VIEWWW: " + newState + " " + String.valueOf(newState == BluetoothProfile.STATE_CONNECTED));
                if (newState == BluetoothProfile.STATE_CONNECTED) {


                    runOnUiThread(() -> {
                        musicVisible = true;

                        listBle.setVisibility(View.GONE);
                        listSongs.setVisibility(View.VISIBLE);
                        connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
                        if (connectedDevices.size() > 0) {
                            BluetoothDevice d = connectedDevices.get(0);
                            receivedDataView.setText("Device connected to bluetooth: " + d.getName());

                        }




                        System.out.println("REMOVING LIST");
//                        ble.read();
                        Toast.makeText(MainActivity.this, "Connected to GATT server.", Toast.LENGTH_SHORT).show();
                    });
                }

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Disconnected from GATT server.", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onBleServiceDiscovered(BluetoothGatt gatt, int status) {
                super.onBleServiceDiscovered(gatt, status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("Ble ServiceDiscovered", "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onBleCharacteristicChange(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onBleCharacteristicChange(gatt, characteristic);
                Log.i("BluetoothLEHelper", "onCharacteristicChanged Value: " + Arrays.toString(characteristic.getValue()));
            }

            @Override
            public void onBleRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onBleRead(gatt, characteristic, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("TAG", Arrays.toString(characteristic.getValue()));
                    String s = Arrays.toString(characteristic.getValue());
                    System.out.println("RECEIVED DATAAAAA: " + s);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "onCharacteristicRead : " + s, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onBleWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onBleWrite(gatt, characteristic, status);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "onCharacteristicWrite Status : " + status, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDataReceive (String data) {
                runOnUiThread(() -> {
                    if (!playSongs) {
                        return;
                    }
                    System.out.println("FINAL DATA FROM CALLBACK: " + data);
                    receivedDataView.setText(data);
                    switch (data) {
                        case TRIPLE_TAP_ACTION:
                            playPreviousSong();
                            break;
                        case DOUBLE_TAP_ACTION:
                            playNextSong();
                            break;
                        case TAP_ACTION:
                            pausePlay();
                            break;
                        case SWIPE_LEFT_ACTION:
                            increaseVolume();
                            break;
                        case SWIPE_RIGHT_ACTION:
                            decreaseVolume();
                            break;
                        case LONG_PRESS_ACTION:
                            muteVolume();
                            break;
                        default:
                            System.out.println("wrong action");


                    }
                });
            }
        };
    }

    private void scanCollars() {
        if (!ble.isScanning()) {

            dAlert = setDialogInfo("Scan in progress", "Loading...", false);
            dAlert.show();

            Handler mHandler = new Handler();
            ble.scanLeDevice(true);

            mHandler.postDelayed(() -> {
                dAlert.dismiss();
                setList();
            }, ble.getScanPeriod());

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void listenerButtons() {

//        btnScan.setOnClickListener(v -> {
//            if(ble.isReadyForScan()){
//                scanCollars();
//            }else{
//                Toast.makeText(MainActivity.this, "You must accept the bluetooth and Gps permissions or must turn on the bluetooth and Gps", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        btnRead.setOnClickListener(v -> {
//            if(ble.isConnected()) {
//                ble.read(Constants.SERVICE_COLLAR_INFO, Constants.CHARACTERISTIC_NOTIFICATION);
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ble.disconnect();
    }

    boolean checkPermission(){
//        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT);
//        if(result == PackageManager.PERMISSION_GRANTED){
//            return true;
//        }else{
//            return false;
//        }

        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }

    void requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_MEDIA_AUDIO)){
            System.out.println("permission toasttt");
            Toast.makeText(MainActivity.this,"READ PERMISSION IS REQUIRED,PLEASE ALLOW FROM SETTTINGS",Toast.LENGTH_SHORT).show();
        }else {
            System.out.println("permission reqqqqqqqqq");
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_MEDIA_AUDIO},123);
        }

//        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)) {
//            System.out.println("permission toasttt");
//            Toast.makeText(MainActivity.this, "READ PERMISSION IS REQUIRED,PLEASE ALLOW FROM SETTTINGS", Toast.LENGTH_SHORT).show();
//        } else {
//            System.out.println("permission reqqqqqqqqq");
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 222);
//        }

    }

    void setMusicControlListeners() {

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSongs = false;
                pause();
                musicMain.setVisibility(View.GONE);
                listSongs.setVisibility(View.VISIBLE);
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("prevButton pressed");
                playPreviousSong();
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("playButton pressed");
                pausePlay();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("nextButton pressed");
                playNextSong();
            }
        });

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTimeTv.setText(convertToMMSS(mediaPlayer.getCurrentPosition() + ""));
                    if (mediaPlayer.isPlaying()) {
                        playButton.setImageResource(R.drawable.pause_outline);
                    } else {
                        playButton.setImageResource(R.drawable.play_outline);
                    }

                }
                new Handler().postDelayed(this, 100);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public static String convertToMMSS(String duration) {
        Long millis = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }


    private void setSongsList() {

        ArrayList<AudioModel> availableSongs = songsList;

        System.out.println("SONGSSS: " + songsList.size());

        if (songsList.size() > 0) {

            BasicList dAdapter = new BasicList(this, R.layout.simple_row_list, availableSongs) {
                @Override
                public void onItem(Object item, View view, int position) {
                    System.out.println("inside onitem");

                    TextView txtName = view.findViewById(R.id.txtText);

                    String aux = ((AudioModel) item).getTitle();
                    txtName.setText(aux);

                }
            };

            listSongs.setAdapter(dAdapter);
            listSongs.setOnItemClickListener((parent, view, position, id) -> {

                playSongs = true;
                songIndex = position;
                musicMain.setVisibility(View.VISIBLE);
                listSongs.setVisibility(View.GONE);
                setResourcesWithMusic();
                setMusicControlListeners();
            });
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playSongs = false;
        setContentView(R.layout.activity_main);
        System.out.println("helooooooooooo");
        backButton = findViewById(R.id.back_button_image);
        listBle = findViewById(R.id.listBle);
        listSongs = findViewById(R.id.songsList);
        receivedDataView = findViewById(R.id.receivedDataView);
        prevButton = findViewById(R.id.previous);
        nextButton = findViewById(R.id.next);
        playButton = findViewById(R.id.pause_play);
        songTitle = findViewById(R.id.song_title);
        seekBar = findViewById(R.id.seek_bar);
        currentTimeTv = findViewById(R.id.current_time);
        totalTimeTv = findViewById(R.id.total_time);
        mainWindow = findViewById(R.id.main_window);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        backgroundColors.add(R.drawable.gradient_background);
        backgroundColors.add(R.drawable.green_gradient_background);
        backgroundColors.add(R.drawable.orange_gradient_background);
        backgroundColors.add(R.drawable.purple_gradient_background);

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        musicMain = findViewById(R.id.music_main);
        musicMain.setVisibility(View.GONE);
        listSongs.setVisibility(View.GONE);
        listBle.setVisibility(View.VISIBLE);
        receivedDataView.setText("Device not connected to bluetooth.");




//        if(!checkPermission()){
//            requestPermission();
//            return;
//        }

        //--- Initialize BLE Helper
        ble = new BluetoothLEHelper(this);


//        if (ble.isConnected()) {
//            receivedDataView.setText("Device connected to bluetooth.");
//        }

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC +"";
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null);
        while(cursor.moveToNext()){
            AudioModel songData = new AudioModel(cursor.getString(1),cursor.getString(0),cursor.getString(2));
            System.out.println("pathhhhhh: "+songData.getPath());
            System.out.println("durrrr: "+songData.getDuration());
            if ((songData.getDuration() != null) && new File(songData.getPath()).exists()) {
                songsList.add(songData);

            }
        }
        if (songsList.size() > 0) {
            musicFound = true;
            currentSong = songsList.get(0);
            songTitle.setText(currentSong.getTitle());
            totalTimeTv.setText(convertToMMSS(currentSong.getDuration()));
            setSongsList();
        } else {
            System.out.println("NO SONGGGG");
        }
        for(AudioModel i: songsList){
            System.out.println(i);
        }


//        receivedDataView = findViewById(R.id.receivedDataView);
        listenerButtons();
//        if (!musicVisible) {
            scanCollars();
//        }
    }

    private void playMusic(){

        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void playNextSong(){

        if(songIndex == songsList.size()-1) {
            songIndex = 0;
        } else {
            songIndex +=1;
        }
        mainWindow.setBackgroundResource(backgroundColors.get(songIndex%backgroundColors.size()));

        mediaPlayer.reset();
        setResourcesWithMusic();

    }

    void setResourcesWithMusic(){
        currentSong = songsList.get(songIndex);

        songTitle.setText(currentSong.getTitle());

        totalTimeTv.setText(convertToMMSS(currentSong.getDuration()));

        playButton.setOnClickListener(v-> pausePlay());
        nextButton.setOnClickListener(v-> playNextSong());
        prevButton.setOnClickListener(v-> playPreviousSong());

        if (musicVisible) {
            playMusic();
        }
    }

    private void playPreviousSong(){
        if(songIndex == 0) {
            songIndex = songsList.size() - 1;
        } else {
            songIndex -=1;
        }
        mainWindow.setBackgroundResource(backgroundColors.get(songIndex%backgroundColors.size()));

        mediaPlayer.reset();
        setResourcesWithMusic();
    }

    private void pausePlay(){
        if(mediaPlayer.isPlaying())
            mediaPlayer.pause();
        else
            mediaPlayer.start();
    }

    private void pause() {
        if(mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

    private void increaseVolume(){
        audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI);
    }

    private void decreaseVolume(){
        audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
    }

    private void muteVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);

    }

}
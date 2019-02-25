package com.example.hakeemsyd.ipcapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.oculus.aidl.OVRMediaServiceInterface;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity {
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = OVRMediaServiceInterface.Stub.asInterface(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private final View.OnClickListener startCaptureClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            String pkg = packageName.getText().toString().trim();
            if (pkg == null || pkg.isEmpty()) {
                pkg = packageName.getHint().toString();
            }
            Bundle b = new Bundle();
            b.putString(MESSAGE_TYPE_KEY, BROADCAST_CAPTURE_ABUSE_START);
            b.putString(INTENT_KEY_PACKAGE_NAME, pkg);
            try {
                Bundle res = mService.sendCommandToMediaService(b);
                boolean status = res.getBoolean(BROADCAST_CAPTURE_ABUSE_STATUS);
                setInProgress(status);
                if (status) {
                    String id = res.getString(BROADCAST_CAPTURE_ABUSE_RECORDING_ID);
                    Toast.makeText(MainActivity.this, "Starting recording with ID: " + id, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Recoding NOT started for target: " + pkg, Toast.LENGTH_SHORT).show();
                }
            } catch (RemoteException ex) {
                Log.e("", ex.getMessage());
            }
        }
    };

    private final View.OnClickListener cancelCaptureClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String pkg = packageName.getText().toString().trim();
            if (pkg == null || pkg.isEmpty()) {
                pkg = packageName.getHint().toString();
            }

            Bundle b = new Bundle();
            b.putString(MESSAGE_TYPE_KEY, BROADCAST_CAPTURE_ABUSE_CANCEL);
            // b.putString(BROADCAST_CAPTURE_ABUSE_RECORDING_ID,
            b.putString(INTENT_KEY_PACKAGE_NAME, pkg);
            try {
                Bundle res = mService.sendCommandToMediaService(b);
                boolean status = res.getBoolean(BROADCAST_CAPTURE_ABUSE_STATUS);
                Toast.makeText(MainActivity.this, "Stop command exec, recording status: " + status, Toast.LENGTH_SHORT).show();

                setInProgress(false);
            } catch (RemoteException ex) {
                Log.e("", ex.getMessage());
            }
        }
    };

    private final View.OnClickListener stopCaptureClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String pkg = packageName.getText().toString().trim();
            if (pkg == null || pkg.isEmpty()) {
                pkg = packageName.getHint().toString();
            }
            Bundle b = new Bundle();
            b.putString(MESSAGE_TYPE_KEY, BROADCAST_CAPTURE_ABUSE_STOP);
            b.putString(INTENT_KEY_PACKAGE_NAME, pkg);
            try {
                Bundle res = mService.sendCommandToMediaService(b);
                boolean status = res.getBoolean(BROADCAST_CAPTURE_ABUSE_STATUS);
                Toast.makeText(MainActivity.this, "Stop command exec, recording status: " + status, Toast.LENGTH_SHORT).show();

                setInProgress(false);
            } catch (RemoteException ex) {
                Log.e("", ex.getMessage());
            }
        }
    };

    private final View.OnClickListener mFileShareListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent();
            intent.setAction("com.oculus.horizon.VIDEO_SHARE");

            sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Bundle b = getResultExtras(true);
                    String u = b.getString("URI");
                    Log.d("", "URI: " + u);
                    setResultData("");


                    // Now read file
                    Uri uri = Uri.parse(u);
                    ParcelFileDescriptor pfd = null;

                    try {
                        pfd = getContentResolver().openFileDescriptor(uri, "r");
                    } catch (FileNotFoundException e) {
                        Log.e("", "File not found ",e );
                    }
                    FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
                    int off = 0;
                    int len = 256;
                    byte[] arr = new byte[256];
                    try {
                        while (inputStream.read(arr, off, 256) != -1) {
                            Log.d("", "bytes read: " + arr.toString());
                        }
                    } catch (IOException e) {
                        Log.i("", "IOException", e);
                    }
                }
            }, null, Activity.RESULT_OK, null, null);
        }
    };


    static final String INTENT_KEY_PACKAGE_NAME = "package_name";
    static final String MESSAGE_TYPE_KEY = "message_type";
    static final String BROADCAST_CAPTURE_ABUSE_START = "broadcast_abuse_capture_start";
    static final String BROADCAST_CAPTURE_ABUSE_STOP = "broadcast_abuse_capture_stop";
    static final String BROADCAST_CAPTURE_ABUSE_STATUS = "recording_command_status";
    static final String BROADCAST_CAPTURE_ABUSE_RECORDING_ID = "recording_uuid";
    static final String BROADCAST_CAPTURE_ABUSE_CANCEL = "broadcast_abuse_capture_cancel";

    private Button sendStartBroadcast;
    private Button sendCancelBroadcast;
    private Button sendStopBroadcast;
    private TextView packageName;
    OVRMediaServiceInterface mService;
    boolean mBound = false;
    // private Intent mRequestFileIntent;
    // private Intent mRequestFileIntent;com.example.hakeemsyd.ipcapp
    private Button mShareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        sendStartBroadcast = (Button) findViewById(R.id.send_start_broadcast);
        sendCancelBroadcast = (Button) findViewById(R.id.send_cancel_broadcast);
        sendStopBroadcast = (Button) findViewById(R.id.send_stop_broadcast);
        mShareButton = (Button) findViewById(R.id.file_share_button);
        packageName = (TextView) findViewById(R.id.package_name);
        sendStartBroadcast.setOnClickListener(startCaptureClickListener);
        sendCancelBroadcast.setOnClickListener(cancelCaptureClickListener);
        sendStopBroadcast.setOnClickListener(stopCaptureClickListener);
        mShareButton.setOnClickListener(mFileShareListener);

        Intent i = new Intent();
        i.setAction("com.oculus.systemactivities.BEGIN_VIDEO_CAPTURE_WITH_SURFACE");
        i.setPackage("com.oculus.maliciousapp");
        // i.putExtra("surface", "hello");
        i.putExtra("capture_key", "ab");
        i.putExtra("lift_inhibit", true);
        MainActivity.this.sendBroadcast(i);
        Log.d("ScreenCaptureReceiver", "screencapture sent broadcast");
    }

    @Override
    protected void onResume() {
        super.onResume();
        bind();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unbind();
    }

    private void bind() {
        if (!mBound) {
            Intent intent = new Intent();
            intent.setClassName("com.oculus.horizon", "com.oculus.horizon.service_media.OVRMediaService");
            mBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        Log.i("", "status: " + mBound);
    }

    private void unbind() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void setInProgress(boolean isInProgress) {
        packageName.setEnabled(!isInProgress);
        //sendStartBroadcast.setEnabled(!isInProgress);
    }

}

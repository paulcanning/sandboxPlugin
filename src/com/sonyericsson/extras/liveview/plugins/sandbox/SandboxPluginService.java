package com.sonyericsson.extras.liveview.plugins.sandbox;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;
import com.sonyericsson.extras.liveview.plugins.PluginUtils;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SandboxPluginService extends AbstractPluginService {
    
    private int MSG_TYPE_TIMER = 1;
    
    // Our handler.
    private Handler mHandler = null;
    
    // Workers
    private Timer mTimer = new Timer();
    
    // Worker state
    private int mCurrentWorker = 0;
    
    private String[] arr;
    
    // timezones
    private String[] tzIDs = TimeZone.getAvailableIDs();
    
    // timezone index
    private int i = 0;
    
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Create handler.
		if(mHandler == null) {
		    mHandler = new Handler();
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		stopWork();
	}
	
    /**
     * Plugin is sandbox.
     */
    protected boolean isSandboxPlugin() {
        return true;
    }
	
	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {
	    if(!workerRunning()) {
	    	
	        // Timezones from array
	        Resources res = this.getResources();
	        arr = res.getStringArray(R.array.timezones);
	    	
	        mHandler.postDelayed(new Runnable() {
                public void run() {
                    // First message to LiveView
                    try {
                        mLiveViewAdapter.clearDisplay(mPluginId);
                    } catch(Exception e) {
                        Log.e(PluginConstants.LOG_TAG, "Failed to clear display.");
                    }
                    // Prepare and send
                    Date currentDate = new Date(System.currentTimeMillis());
                    
                    TimeZone tz = TimeZone.getTimeZone("GMT");
                    SimpleDateFormat tzFormat = new SimpleDateFormat("HH:mm:ss", Locale.UK);
                    tzFormat.setTimeZone(tz);
                    
                    PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId, tzFormat.format(currentDate));
                    
                    Log.d(PluginConstants.LOG_TAG, "Image sent to LiveView.");
                    
                    // Schedule new timer event.
                    scheduleTimer();
                }
            }, 1000);
        }
	}
	
	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {
		stopUpdates();
	}
	
	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView Service. 
	 * 
	 * If needed, do additional actions here, e.g. 
	 * starting any worker that is needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className, IBinder service) {
		
	}
	
	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been stopped. 
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {
		
	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed. 
	 */	
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs, String key) {
		if(key.equals("pluginEnabled")) {
			if(mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
				stopPlugin();
			}
		}
	}

	protected void startPlugin() {
		Log.d(PluginConstants.LOG_TAG, "startPlugin");
		startWork();
	}
			
	protected void stopPlugin() {
		Log.d(PluginConstants.LOG_TAG, "stopPlugin");
		stopWork();
	}
	
	protected void button(String buttonType, boolean doublepress, boolean longpress) {
	    //Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType + ", doublepress " + doublepress + ", longpress " + longpress);
		
		if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {
		    /*if(longpress) {
		        mLiveViewAdapter.ledControl(mPluginId, 50, 50, 50);
		    }*/
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {
            /*if(longpress) {
                mLiveViewAdapter.vibrateControl(mPluginId, 50, 50);
            }*/
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {
			nextTime();
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {
			previousTime();
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {
			/* Toggle users time to list of world times */
			//toggleTime();
		}
	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
        Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx + ", height " + displayHeigthPx);
    }

	protected void onUnregistered() throws RemoteException {
		Log.d(PluginConstants.LOG_TAG, "onUnregistered");
		stopWork();
	}

	protected void openInPhone(String openInPhoneAction) {
		Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);
	}
	
    protected void screenMode(int mode) {
        Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now " + ((mode == 0) ? "OFF" : "ON"));
        
        if(mode == PluginConstants.LIVE_SCREEN_MODE_ON) {
            startUpdates();
        } else {
            stopUpdates();
        }
    }
    
    private void stopUpdates() {
        saveState();
        mHandler.removeMessages(MSG_TYPE_TIMER);
    }
    
    private void startUpdates() {
        if(mCurrentWorker == MSG_TYPE_TIMER) {
            scheduleTimer();
        }
    }
    
    /**
     * The runnable used for posting to handler
     */
    private class Timer implements Runnable {
        
        @Override
        public void run() {
            // Prepare and send
            Date currentDate = new Date(System.currentTimeMillis());
            
            //TimeZone tz = TimeZone.getTimeZone(tzIDs[i]);
            TimeZone tz = TimeZone.getTimeZone(arr[i]);
            SimpleDateFormat tzFormat = new SimpleDateFormat("HH:mm:ss", Locale.UK);
            tzFormat.setTimeZone(tz);
            
            String timezoneLabel = arr[i].toString();
            String[] timezoneLabelParts = timezoneLabel.split("/");
            
            String timezone = timezoneLabelParts[1].replace("_", " ");
            
            PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId, timezone + "\n" + tzFormat.format(currentDate));
            
            //Log.d(PluginConstants.LOG_TAG, "Image sent to LiveView.");
            
            // Schedule new timer event.
            scheduleTimer();
        }
        
    }
    
    
    /**
     * Schedules a timer. 
     */
    private void scheduleTimer() {
        Message msg = Message.obtain(mHandler, mTimer);
        msg.what = MSG_TYPE_TIMER;
        mHandler.sendMessageDelayed(msg, 1000);
        //Log.d(PluginConstants.LOG_TAG, "Timer scheduled.");
    }
    
    /**
     * Start/stop timer.
     */
    private void toggleTimer() {
        if(workerRunning()) {
            stopWork();
        } else {
            stopWork();
            try {
                if(mLiveViewAdapter != null) {
                    mLiveViewAdapter.clearDisplay(mPluginId);
                }
            } catch(Exception e) {
                // NOP
            }
            scheduleTimer();
        }
    }
    
    /*
     * Change between users time and list of world times
     */
    private void toggleTime()
    {
    	
    }
    
    /*
     * Show next timezone
     */
	private void nextTime() {
		// TODO Auto-generated method stub
		//stopWork();
		if(i < arr.length - 1) {
			i++;
		}
		
        scheduleTimer();

	}
	
	/*
	 * Shwo previous timezone
	 */
	private void previousTime() {
		// TODO Auto-generated method stub
		if(i > 0) {
			i--;
		}
		
        scheduleTimer();
	}

    private void saveState() {
        int state = 0;
        
        if(workerRunning()) {
           state = MSG_TYPE_TIMER;
        }
        
        mCurrentWorker = state;
    }
    
    private boolean timersOnQueue() {
        return mHandler.hasMessages(MSG_TYPE_TIMER);
    }
    
    
    private boolean workerRunning() {
        return (timersOnQueue());
    }
    
}
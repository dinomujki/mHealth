//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.heartrate;

import java.lang.ref.WeakReference;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import android.content.Intent;
import android.widget.Toast;
import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseObject;
import com.parse.Parse;
import com.parse.ParseQuery;
import com.parse.ParseException;

//YouTube
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;

//public class BandHeartRateAppActivity extends Activity {
public class BandHeartRateAppActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

	private BandClient client = null;
	private Button btnStart, btnConsent;
	private TextView txtStatus;
	private TextView txtbpm;
	private TextView txtrr;
	private TextView txtgsr;

    private int bpm;
    private double gsr;
    private double rr;

    //video vars
    String theID;
    public static final int RECOVERY_REQUEST = 1;
    private YouTubePlayerView youTubeView;

    public final class Config {
        private Config() {
        }

        public static final String YOUTUBE_API_KEY = "AIzaSyDL6_lgg5HEODCoIKk2vzgb2gFFPVUBDYQ";
    }

	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                bpm = event.getHeartRate();
                appendTobpm(Integer.toString(bpm));
            	//appendToUI(String.format("Heart Rate = %d beats per minute\n"
                //        + "Quality = %s\n", event.getHeartRate(), event.getQuality()));

                ParseObject testObject = new ParseObject("SensorData");
                testObject.put("HeartRate", bpm);
                testObject.put("GSR", gsr);
                testObject.put("RR", rr);
                testObject.saveInBackground();

                if (bpm > 75 && rr > 1) {
                    appendToUI("You are stressed, relax with some comedy.");
                }
                else {
                    appendToUI("You're good! You don't appear stressed!");
                }
            }
        }
    };

    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                gsr = event.getResistance();
                appendTogsr(Double.toString(gsr));
                Log.d("gsr", Double.toString(gsr));
                //appendToUI(String.format("Resistance = %d kOhms\n", event.getResistance()));
            }
        }
    };

    private BandRRIntervalEventListener mRREventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(BandRRIntervalEvent bandRRIntervalEvent) {
            if (bandRRIntervalEvent != null) {
                rr = bandRRIntervalEvent.getInterval();
                appendTorr(Double.toString(rr));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize YouTube player view
//        youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
//        youTubeView.initialize(Config.YOUTUBE_API_KEY, this);

		// [Optional] Power your app with Local Datastore. For more info, go to
		// https://parse.com/docs/android/guide#local-datastore
		Parse.enableLocalDatastore(this);
		Parse.initialize(this);

        //creating a query for retrieving YouTube vids
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("url");
        Log.d("query", "made the query");
        query.whereEqualTo("objectId", "61WL7CRujY"); // to grab the right one - will change later
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    Log.d("read", "failed");
                }
                else {
                    //you got it
                    String theID = object.getString("vidID");
                    setTheID(theID);
                    Log.d("read", "passed");
                }
            }
        });

        //SHAWN: fix from here until...
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtbpm = (TextView) findViewById(R.id.txtbpm);

        txtrr = (TextView) findViewById(R.id.txtrr);

        txtgsr = (TextView) findViewById(R.id.txtgsr);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txtStatus.setText("");
				new HeartRateSubscriptionTask().execute();
			}
		});
        
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        
        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnConsent.setOnClickListener(new OnClickListener() {
			@SuppressWarnings("unchecked")
            @Override
			public void onClick(View v) {
				new HeartRateConsentTask().execute(reference);
                new GsrSubscriptionTask().execute();
			}
		});

        //...here
    }

    public void setTheID(String myID) {
        theID = myID;
        Log.d("set", "set the ID!");
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        Log.d("success", "success");

        if (!wasRestored) {
            Log.d("id", theID);
            player.cueVideo(theID); // Plays https://www.youtube.com/watch?v=theID
        }
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        String error = String.format("Error initializing YouTube player", errorReason.toString());
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            //retry initialization
            getYouTubePlayerProvider().initialize(Config.YOUTUBE_API_KEY, this);
        }
    }

    protected Provider getYouTubePlayerProvider() {
        return youTubeView;
    }


	
	@Override
	protected void onResume() {
		super.onResume();
		txtStatus.setText("");
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		if (client != null) {
			try {
				client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
			} catch (BandIOException e) {
				appendToUI(e.getMessage());
			}
		}
	}
	
    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }
    
	private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
						client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                        client.getSensorManager().registerRRIntervalEventListener(mRREventListener);
					} else {
						appendToUI("You have not given this application consent to access heart rate data yet."
								+ " Please press the Heart Rate Consent button.\n");
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}

    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        appendToUI("Band is connected.\n");
                        client.getSensorManager().registerGsrEventListener(mGsrEventListener);
                    } else {
                        appendToUI("The Gsr sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }
	
	private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
		@Override
		protected Void doInBackground(WeakReference<Activity>... params) {
			try {
				if (getConnectedBandClient()) {
					
					if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
							}
					    });
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}
	
	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtStatus.setText(string);
            }
        });
	}

    private void appendTobpm(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtbpm.setText("BPM: " + string);
            }
        });
    }

    private void appendTogsr(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtgsr.setText("GSR: " + string);
            }
        });
    }

    private void appendTorr(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtrr.setText("RR: " + string);
            }
        });
    }
    
	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}
}


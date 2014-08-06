package com.scpi;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import explib.ejPlot;
 
public class Logger extends Activity {
	int duration=30,length=0;
	float[] Y=new float[10000],X=new float[10000];
	Button button;
	ejPlot ejplot;
	SeekBar timebase;
	TextView identity;
	String filename = new String();
	private Handler mHandler;
	private TextView msg;
	private EditText INTERVAL;
	private double ymin=0, ymax=1;
    private boolean running=false,connected=false;
    String read=new String();
    private long start_time=0;
    ClientThread clt;
    ConnectionThread conn;
   public int port=8888,interval=100;
   public Socket socket;
   public InetAddress serverAddr;
   Common comm;
   String id=new String();

	 BufferedReader input;
	 BufferedWriter output;

	  public Builder about_dialog;
		private File dataDirectory;

		@Override
		public boolean onCreateOptionsMenu(Menu menu)
		{
		    MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.logger, menu);
		    //MenuItem refresh = menu.getItem(R.id.menu_refresh);
		    //refresh.setEnabled(true);
		    return true;
		}
		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
		    switch(item.getItemId())	    
		    {
		    case R.id.credits:
		    	//display_about_dialog();
		    	about_dialog.show();
		    	break;
		    case R.id.save:
		    	//display_about_dialog();
		    	dumpToFile();
		    	break;
		    }
		    return true;
		}
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logger);
		Toast.makeText(getBaseContext(),"Data logger",Toast.LENGTH_SHORT).show();
		
		setTitle("SCPI data logger");
		msg = (TextView) findViewById(R.id.msg);
		INTERVAL = (EditText) findViewById(R.id.INTERVAL);

		
		identity = (TextView) findViewById(R.id.identity);
		identity.setText("waiting...");
     	LinearLayout plot=(LinearLayout)findViewById(R.id.plot);
     	
     	dataDirectory = new File(Environment.getExternalStorageDirectory()+"/SCPI_DATA_LOGGER/");
		Log.e("DIR",dataDirectory.getName());
		dataDirectory.mkdirs();
     	
		about_dialog = new AlertDialog.Builder(this);
        
        about_dialog.setMessage("e-mail:jithinbp@gmail.com.\n https://github.com/jithinbp \n IISER Mohali, India");
        about_dialog.setTitle("Developed by Jithin B.P");
        about_dialog.setCancelable(true);
        
     	
     	ejplot = new ejPlot(this, plot);
	    ejplot.xlabel="Time";
	    ejplot.ylabel="Units";
	    ejplot.setWorld(0, duration, 0, 30);
	    
	    comm=Common.getInstance();
	    conn = new ConnectionThread();
	    conn.start();
	    
	    clt = new ClientThread();
	    
	    mHandler = new Handler();

		     	
	}
    
	public int toInt(EditText txt) {
		String val = txt.getText().toString();

		if (val == null || val.isEmpty()) {
			return 100;
		} else {
			return Integer.parseInt(val);
		}
	}

	public double toDouble(String str) {
		if (str == null || str.isEmpty()) {
			return 0.0;
		} else {
			try{
			return Double.parseDouble(str);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
	}
	@Override
	public void onDestroy(){
		if(conn.isAlive()){conn.interrupt();
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}}
		
		if(clt.isAlive())clt.interrupt();
		connected=false;
		running=false;
		super.onDestroy();
		
	}
 
	public void autoscale(View v){
		if(length<2)return;
		
		ejplot.setWorld(0, duration, ymin,( Math.abs(ymax-ymin)<0.0001)?(ymax+0.0001):ymax );
		
	}
	
	public void start(View v){
		length=0;
		start_time=System.currentTimeMillis();
		interval=toInt(INTERVAL);
		if(interval<100){
				interval=100;
				Toast.makeText(getBaseContext(),"Minimum interval of 100mS required. Correction made.",Toast.LENGTH_SHORT).show();
		}
		duration=30;
		ymin = 0;
		ymax = 1;
		ejplot.setWorld(0, duration, ymin,( Math.abs(ymax-ymin)<0.0001)?(ymax+0.0001):ymax);
		if(!connected){
			Toast.makeText(getBaseContext(),"Failed to connect to the socket. Return to main menu.",Toast.LENGTH_SHORT).show();
		   	return;
		}
		if(!running){
			Toast.makeText(getBaseContext(),"Logging -> "+comm.command,Toast.LENGTH_SHORT).show();
			running=true;
			if(!clt.isAlive())clt.start();
			}
	}
	
	
	
	


	
	
	
	
	class ConnectionThread extends Thread{
		@Override
		public void run(){
			Log.e("CONNECTION THREAD STARTING",comm.ip+":"+comm.port);
			try {
				serverAddr = InetAddress.getByName(comm.ip);
				socket = new Socket(serverAddr, comm.port);
				socket.setSoTimeout(200);
				InputStreamReader iStream = new InputStreamReader(socket.getInputStream());
				input = new BufferedReader(iStream);
				output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				output.append("*IDN?"+'\n');
				output.flush();
				comm.id = input.readLine();
				Logger.this.runOnUiThread(new Runnable() {
				    @Override
				     public void run() {
				    	identity.setText(comm.id);
				     }
				    });
				
				connected=true;
				Log.e("ID",comm.id+":");
				} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
					
				e.printStackTrace();
			} catch (IOException e) {
					// TODO Auto-generated catch block
				    
					e.printStackTrace();
				}
					
			

		}
	}
	
	
	
	class ClientThread extends Thread {

		@Override
		public void run() {
					while (!Thread.currentThread().isInterrupted()) {
					try {
						//Log.e("writing", command+"\n");
						output.append(comm.command+'\n');
						output.flush();
						read = input.readLine();
						
						X[length]=(float) ((System.currentTimeMillis()-start_time)/1.0e3);
				    	Y[length]=(float) toDouble(read);
				    	if(length==0){ymin=Y[length];ymax=Y[length];}
				    	else if(Y[length]<ymin){ymin=Y[length];ejplot.setWorld(0, duration, ymin, ymax);}
				    	else if(Y[length]>ymax){ymax=Y[length];ejplot.setWorld(0, duration, ymin, ymax);}
					    	
				    	
				    	if(X[length]>duration){
				    		duration+=10;
				    		ejplot.setWorld(0, duration, ymin, ymax);
				    	}
				    	
				    	if(length>9999){
				    		
				    		Toast.makeText(getBaseContext(),"Array size exceeded. Restarting. !",Toast.LENGTH_SHORT).show();
				    		start();
				    	}
				    	
						
						mHandler.postDelayed(cro, 1);
						SystemClock.sleep(interval);
						

					} catch (IOException e) {
						connected=false;
						e.printStackTrace();
					}

				}
	
					
					
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					connected=false;

		}

	}

	Runnable cro = new Runnable() {  
	    @Override 
	    public void run() {
	    	msg.setText(read);
	    	ejplot.clearPlots();
			if(length>1)ejplot.line(X,Y,length,1);
			ejplot.updatePlots();
			length++;
			
			
	    	
	      
	    }
	};

	
	
	
private void appendToFile(OutputStreamWriter writer,float[] x,float[] y,int length) throws IOException{
	for(int i=0;i<length;i++){writer.append(x[i]+" "+y[i]+"\n");}
	writer.append("\n");
	
}

public void dumpToFile(){
	SimpleDateFormat s = new SimpleDateFormat("dd-MM_hh-mm-ss");
	String format = s.format(new Date());
	Log.e("FILENAME",format+"");
	filename = format+".txt";
	Log.e("SAVING to ",""+filename);
	try {
    	File outputFile = new File(dataDirectory, filename);
  		outputFile.createNewFile();
		FileOutputStream fOut = new FileOutputStream(outputFile);
		OutputStreamWriter myOutWriter =  new OutputStreamWriter(fOut);
		appendToFile(myOutWriter,X,Y,length);
        myOutWriter.close();
        fOut.close();
        
        Toast.makeText(getBaseContext(), "Done writing to ./SCPI_DATA_LOGGER/" + filename + "",Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }
	
	
}
	

}

package com.prozone.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * 
 * This class will provide the heartbeat functionality.
 * 
 */
public class HeartbeatHandler implements Runnable {

	private int interval;
	private int server_port;
	
	public HeartbeatHandler(int interval, int server_port) {
		
		this.interval=interval;
		this.server_port=server_port;
	}

	private void sendDevicesListToPrimary() throws MalformedURLException {
		
		try {
			HttpURLConnection conn;
			URL url=new URL("http://"+StreamingProxyServer.getPrimaryServer()+":"+server_port+"/deviceList");
			String devicesList="";
			List<String> devices=StreamingProxyServer.getStreamingDevicesList();
			if(devices!=null && devices.size()>0) {
				for(int i=0;i<devices.size();i++) {
					//For each device add a key-value pair
					if(i!=0)
						devicesList+="&";
					devicesList+=URLEncoder.encode(String.valueOf(i), "UTF-8");
					devicesList+="=";
					devicesList+=URLEncoder.encode(devices.get(i), "UTF-8");
				}
				System.out.println("Going to send:"+devicesList);
				conn=(HttpURLConnection)url.openConnection();
				conn.setRequestMethod("POST");
				conn.setDoInput(false);
				conn.setDoOutput(true);
				OutputStreamWriter out=new OutputStreamWriter(conn.getOutputStream());
				out.write(devicesList);
				out.flush();
				out.close();
			}
		} catch (IOException e) {
			System.out.println("Error in sending state info to the new primary server.");
		}
	}
	
	public void run() {
		
		try {
			int failedCount=0;
			while(true) {
				
				String primaryServer=StreamingProxyServer.getPrimaryServer();
				if(!primaryServer.equals("")) {
					System.out.println("Ping "+primaryServer);
					HttpURLConnection conn;
					URL url=new URL("http://"+primaryServer+":"+server_port+"/heartbeat");
					try {
						conn=(HttpURLConnection)url.openConnection();
						conn.setRequestMethod("GET");
						BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String response=in.readLine();
						if(!response.equals("Ok")) {
							failedCount++;
						}
						else {
							failedCount=0;
						}
					} catch (IOException e) {
						System.out.println("Could not connect with server : "+primaryServer);
						failedCount++;
					} finally {

						if(failedCount==3) {
							
							System.out.println("Primary server down...");
							StreamingProxyServer.getStreamingServersList().remove(primaryServer);
							String newPrimary="";
							if(StreamingProxyServer.getStreamingServersList()!=null && StreamingProxyServer.getStreamingServersList().size()>0) {
								//Choose a new primary and send the devices list to it
								newPrimary=StreamingProxyServer.getStreamingServersList().get(0).toString();
								StreamingProxyServer.setPrimaryServer(newPrimary);
								sendDevicesListToPrimary();
							}
							failedCount=0;
						}
					}
				}
				Thread.sleep(interval*1000);
			}
		}
		catch (InterruptedException e) {
			System.out.println("Heartbeat thread interrupted from sleep");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			System.out.println("Malformed URL");
			e.printStackTrace();
		}
	}
}

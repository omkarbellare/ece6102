package com.prozone.proxy;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.omg.CORBA.NameValuePair;

/**
 * 
 * This class will provide the heartbeat functionality.
 * 
 */
public class HeartbeatHandler implements Runnable {

	private int interval;
	private int server_port;
	private final int FAILURE_COUNT=3;
	private final int SEND_DEVICES_COUNT=3;
	
	public HeartbeatHandler(int interval, int server_port) {
		
		this.interval=interval;
		this.server_port=server_port;
	}

	private void sendDevicesListToPrimary() throws MalformedURLException {
		
		try {
			
			DefaultHttpClient httpClient=new DefaultHttpClient();
			URL url=new URL("http://"+StreamingProxyServer.getPrimaryServer()+":"+server_port+"/deviceList");
			HttpPost post=new HttpPost(url.toString());
			String devicesList="";
			List<String> devices=StreamingProxyServer.getStreamingDevicesList();
			List<BasicNameValuePair> list=new ArrayList<BasicNameValuePair>();
			for(int i=0;i<devices.size();i++) {
				
				list.add(new BasicNameValuePair(String.valueOf(i),devices.get(i)));
			}
			post.setEntity(new UrlEncodedFormEntity(list));
			httpClient.execute(post);
			System.out.println("Going to send:"+devicesList);
		} catch (IOException e) {
			System.out.println("Error in sending state info to the new primary server.");
		}
	}
	
	public void run() {
		
		try {
			int failedCount=0;
			int successCount=0;
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
							successCount++;
							//Send the devices list after a few successful heartbeats
							if(successCount==SEND_DEVICES_COUNT) {
								sendDevicesListToPrimary();
								successCount=0;
							}
						}
					} catch (IOException e) {
						System.out.println("Could not connect with server : "+primaryServer);
						failedCount++;
					} finally {

						if(failedCount==FAILURE_COUNT) {
							
							System.out.println("Primary server down...");
							StreamingProxyServer.getStreamingServersList().remove(primaryServer);
							String newPrimary="";
							if(StreamingProxyServer.getStreamingServersList()!=null && StreamingProxyServer.getStreamingServersList().size()>0) {
								//Choose a new primary and send the devices list to it
								newPrimary=StreamingProxyServer.getStreamingServersList().get(0).toString();
								StreamingProxyServer.setPrimaryServer(newPrimary);
								sendDevicesListToPrimary();
							}
							else {
								StreamingProxyServer.setPrimaryServer(newPrimary);
							}
							failedCount=0;
							successCount=0;
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

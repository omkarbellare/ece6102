package com.prozone.proxy;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
			
			DefaultHttpClient httpClient1=new DefaultHttpClient();
			//Send the list of devices
			URL url=new URL("http://"+StreamingProxyServer.getPrimaryServer()+":"+server_port+"/deviceList");
			HttpPost post=new HttpPost(url.toString());
			List<String> devices=StreamingProxyServer.getStreamingDevicesList();
			List<BasicNameValuePair> list=new ArrayList<BasicNameValuePair>();
			for(int i=0;i<devices.size();i++) {
				
				list.add(new BasicNameValuePair(String.valueOf(i),devices.get(i)));
			}
			post.setEntity(new UrlEncodedFormEntity(list));
			httpClient1.execute(post);
						
			DefaultHttpClient httpClient2=new DefaultHttpClient();
			//Also send the list of servers
			url=new URL("http://"+StreamingProxyServer.getPrimaryServer()+":"+server_port+"/serverList");
			post=new HttpPost(url.toString());
			List<String> servers=StreamingProxyServer.getStreamingServersList();
			list.clear();
			for(int i=0;i<servers.size();i++) {
				
				list.add(new BasicNameValuePair(String.valueOf(i),servers.get(i)));
			}
			post.setEntity(new UrlEncodedFormEntity(list));
			httpClient2.execute(post);
		} catch (IOException e) {
			System.out.println("Error in sending state info to the new primary server.");
		}
	}
	
	private void sendDevicesListToNextProxy() {
		
		//First we need to find a working alternate proxy server
		List<String> proxyList=StreamingProxyServer.getProxyList();
		for(String proxy:proxyList) {
			if(!proxy.equals(StreamingProxyServer.getOwnIP())) {
				//Register the devices currently under this proxy to the new proxy
				boolean allDevicesRegistered=true;
				HttpURLConnection conn;
				System.out.println("Going to send devices list to "+proxy);
				for(Object device:StreamingProxyServer.getStreamingDevicesList()) {
					
					try {
						URL url=new URL("http://"+proxy+":"+StreamingProxyServer.getPort()+"/registerDevice?ip="+device.toString());
						try {
							conn=(HttpURLConnection)url.openConnection();
							conn.setRequestMethod("GET");
							BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
							String response=in.readLine();
							if(!response.equals("Ok")) {
								allDevicesRegistered=false;
								break;
							}
						} catch (IOException e) {
							System.out.println("Could not connect with proxy : "+proxy);
							allDevicesRegistered=false;
							break;
						}
					}
					catch(MalformedURLException e) {
						System.out.println("Malformed URL");
						e.printStackTrace();
					}
				}
				if(allDevicesRegistered) {
					//Clear own streamingDevicesList
					StreamingProxyServer.getStreamingDevicesList().clear();
					break;
				}
			}
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
								//This means that no servers are left under this proxy server
								//In this case, we need to send any devices under this proxy
								//to a different proxy.
								StreamingProxyServer.setPrimaryServer("");
								if(StreamingProxyServer.getStreamingDevicesList()!=null && StreamingProxyServer.getStreamingDevicesList().size()>0)
									sendDevicesListToNextProxy();
							}
							failedCount=0;
							successCount=0;
						}
					}
				}
				else {
					if(StreamingProxyServer.getStreamingDevicesList()!=null && StreamingProxyServer.getStreamingDevicesList().size()>0)
						sendDevicesListToNextProxy();
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

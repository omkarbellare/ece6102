package com.prozone.proxy;

import java.io.*;
import java.util.*;

import com.prozone.helper.ProxyHelper;
import com.prozone.http.NanoHTTPD;

/**
 * 
 * The class containing the streaming proxy server. 
 * Internally this will use NanoHTTPD: 
 * Copyright © 2001,2005-2012 Jarno Elonen <elonen@iki.fi> and Copyright © 2010 Konstantinos Togias <info@ktogias.gr>
 * 
 */
public class StreamingProxyServer extends NanoHTTPD implements Runnable
{
	private static List streamingServersList;
	private static List streamingDevicesList;
	private static String primaryServer; 
	
	public StreamingProxyServer(int port) throws IOException
	{
		//Start the server on port 8080
		super(port, new File("."));
		streamingServersList=new ArrayList<String>();
		streamingDevicesList=new ArrayList<String>();
		primaryServer="";
	}
	
	static synchronized List getStreamingServersList() {
		return streamingServersList;
	}

	static synchronized void setStreamingServersList(List streamingServersList) {
		StreamingProxyServer.streamingServersList = streamingServersList;
	}

	static synchronized List getStreamingDevicesList() {
		return streamingDevicesList;
	}

	static synchronized void setStreamingDevicesList(List streamingDevicesList) {
		StreamingProxyServer.streamingDevicesList = streamingDevicesList;
	}

	static synchronized String getPrimaryServer() {
		return primaryServer;
	}

	static synchronized void setPrimaryServer(String primaryServer) {
		StreamingProxyServer.primaryServer = primaryServer;
	}
	
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
		if(uri.equals("/registerServer")) {
			
			if(parms.containsKey("ip") && ProxyHelper.isValidIP(parms.get("ip").toString())) {
				//Check for double registration
				synchronized (streamingServersList) {
					
					if(!streamingServersList.contains(parms.get("ip").toString())) {
						streamingServersList.add(parms.get("ip").toString());
						//If no primary server elected so far, then pick this one
						synchronized (primaryServer) {
							
							if(primaryServer.equals("")) {
								primaryServer=parms.get("ip").toString();
							}
						}
					}
				}
				String msg="Total registered servers="+streamingServersList.size();
				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, msg );
			}
			else {
				return new NanoHTTPD.Response( HTTP_BADREQUEST, MIME_PLAINTEXT, "Bad Request" );
			}
		}
		else if(uri.equals("/registerDevice")) {
			
			if(parms.containsKey("ip") && ProxyHelper.isValidIP(parms.get("ip").toString())) {
				synchronized (streamingDevicesList) {
							
					if(!streamingDevicesList.contains(parms.get("ip").toString()))
						streamingDevicesList.add(parms.get("ip").toString());
				}
				String msg="Total registered devices="+streamingDevicesList.size();
				return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, msg );
			}
			else {
				return new NanoHTTPD.Response( HTTP_BADREQUEST, MIME_PLAINTEXT, "Bad Request" );
			}
		}
		else {
			return new NanoHTTPD.Response( HTTP_BADREQUEST, MIME_PLAINTEXT, "Bad Request" );
		}
	}

	@Override
	public void run() {
		//This is to make this thread run forever
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
}

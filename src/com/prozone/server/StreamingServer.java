package com.prozone.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.prozone.helper.ProxyHelper;
import com.prozone.http.NanoHTTPD;
import com.prozone.http.NanoHTTPD.Response;

public class StreamingServer extends NanoHTTPD implements Runnable {

	private String proxy_ip;
	private int proxy_port;
	private static List streamingDevicesList;
	
	public StreamingServer(int port, String proxy_ip, int proxy_port) throws IOException {
		super(port, new File("."));
		this.proxy_ip=proxy_ip;
		this.proxy_port=proxy_port;
		streamingDevicesList=new ArrayList<String>();
	}

	public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
	{
		if(uri.equals("/heartbeat"))
			return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, "Ok" );
		else if(uri.equals("/deviceList")) {
			
			for(Object key:parms.keySet()) {
				
				System.out.println(parms.get(key).toString());
			}
			return new NanoHTTPD.Response( HTTP_OK, MIME_PLAINTEXT, "Ok" );
		}
		else {
			System.out.println("URI="+uri);
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

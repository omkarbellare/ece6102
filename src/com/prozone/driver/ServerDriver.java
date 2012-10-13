package com.prozone.driver;

import java.io.IOException;
import java.util.Properties;

import com.prozone.proxy.StreamingProxyServer;
import com.prozone.server.StreamingServer;

public class ServerDriver {
	
	private static Thread serverThread;

	public static void main(String[] args) {
		
		try {
			Properties prop=new Properties();
			prop.load(ProxyDriver.class.getResourceAsStream("/config/config.properties"));
			int proxy_port_num=Integer.parseInt(prop.getProperty("proxy_port"));
			String proxy_ip=prop.getProperty("heartbeat_interval");
			int server_port=Integer.parseInt(prop.getProperty("server_port"));
			StreamingServer server = new StreamingServer(server_port,proxy_ip,proxy_port_num);
			serverThread=new Thread(server);
			serverThread.start();
			serverThread.join();
		} catch( IOException e ) {
			System.err.println( "Couldn't start proxy server:\n" + e );
			System.exit( -1 );
		} catch (InterruptedException e) {
			System.err.println( "Couldn't start proxy server interrupted:\n" + e );
			e.printStackTrace();
		}
	}

}

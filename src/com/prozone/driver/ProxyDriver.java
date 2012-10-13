package com.prozone.driver;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import com.prozone.proxy.HeartbeatHandler;
import com.prozone.proxy.StreamingProxyServer;

public class ProxyDriver {

	private static Thread proxyThread;
	private static Thread heartbeatThread;
	
	public static void main( String[] args )
	{
		try
		{
			Properties prop=new Properties();
			prop.load(ProxyDriver.class.getResourceAsStream("/config/config.properties"));
			int port_num=Integer.parseInt(prop.getProperty("proxy_port"));
			int heartbeat_interval=Integer.parseInt(prop.getProperty("heartbeat_interval"));
			int server_port=Integer.parseInt(prop.getProperty("server_port"));
			StreamingProxyServer proxy=new StreamingProxyServer(port_num);
			proxyThread=new Thread(proxy);
			proxyThread.start();
			HeartbeatHandler heartbeat=new HeartbeatHandler(heartbeat_interval,server_port);
			heartbeatThread=new Thread(heartbeat);
			heartbeatThread.start();
			heartbeatThread.join();
			proxyThread.join();
		}
		catch( IOException e )
		{
			System.err.println( "Couldn't start proxy server:\n" + e );
			System.exit( -1 );
		} catch (InterruptedException e) {
			System.err.println( "Couldn't start proxy server interrupted:\n" + e );
			e.printStackTrace();
		}
	}
}

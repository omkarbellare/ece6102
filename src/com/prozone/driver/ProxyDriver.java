package com.prozone.driver;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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
			String listProxies[]=prop.getProperty("proxy_ips").split(",");
			List<String> proxyList=Arrays.asList(listProxies);
			
			NetworkInterface network = null;
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			Collection<NetworkInterface> netsCollection = Collections.list(nets);
			for(NetworkInterface net : netsCollection){
				if(net.getDisplayName().equals("wlan0")){
					network = net;
					break;
				}
			}
			InetAddress inetAddress = null;
			Enumeration<InetAddress> inets = network.getInetAddresses();
			while (inets.hasMoreElements()) {
				inetAddress= (InetAddress) inets.nextElement();
				if(inetAddress instanceof Inet4Address)
					break;
			}
			System.out.println("Proxy running on IP:"+inetAddress.getHostAddress());
			StreamingProxyServer proxy=new StreamingProxyServer(inetAddress.getHostAddress(),proxyList,port_num);
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

package com.prozone.driver;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import com.prozone.server.StreamServer;

public class ServerDriver {

	public static void main(String[] args){
		try {
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

			System.out.println(inetAddress.getHostAddress());
			StreamServer ss = new StreamServer(inetAddress);
			ss.startServer();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

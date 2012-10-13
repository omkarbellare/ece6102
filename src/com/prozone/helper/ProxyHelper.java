package com.prozone.helper;

public class ProxyHelper {

	public static boolean isValidIP(String ip) {
		
		if(ip==null)
			return false;
		String octets[]=ip.split("\\.");
		if(octets.length!=4)
			return false;
		for(int i=0;i<4;i++) {
			if(Integer.parseInt(octets[i])<0 || Integer.parseInt(octets[i])>255)
				return false;
			if(i==0 && Integer.parseInt(octets[i])==0)
				return false;
		}
		return true;
	}
}

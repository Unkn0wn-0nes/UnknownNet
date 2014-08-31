package com.Unkn0wn0ne.unknownet.tests;

import com.Unkn0wn0ne.unknownnet.server.UnknownClient;
import com.Unkn0wn0ne.unknownnet.server.UnknownServer;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

public class ClientAcceptKickTest extends UnknownServer{
	
	public ClientAcceptKickTest() {
		super();
		this.registerPacket(1, Packet1ChatMessage.class);
		this.startServer();
	}
	

	@Override
	public boolean handleNewConnection(UnknownClient client, String[] loginData) {
		if (loginData == null) {
			return false;
		} else if (!loginData[0].equalsIgnoreCase("Password")) {
			return false;
		}
		
		String username = loginData[1];
		client.setObjectTag(username);
		Packet1ChatMessage msgPacket = null;
		try {
			msgPacket = (Packet1ChatMessage) this.createPacket(1);
		} catch (ProtocolViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msgPacket.setVariables(username + " has joined our server!");
		
		int num = 0;
		synchronized (this.getConnectedClients()) {
			for (UnknownClient c: this.getConnectedClients()) {
				num++;
				c.queuePacket(msgPacket);
			}
		}
		
		Packet1ChatMessage msgPacket2 = null;
		msgPacket2 = new Packet1ChatMessage();
		msgPacket2.setVariables("Welcome to our chat server! There are : " + num + " client(s) connected right now.");
		client.queuePacket(msgPacket2);
		return true;
	}

	@Override
	public void onClientLeave(UnknownClient client) {
		Packet1ChatMessage msgPacket = null;
		msgPacket = new Packet1ChatMessage();
		
		msgPacket.setVariables(client.getClientTag() + " has left the server.");
		synchronized (this.getConnectedClients()) {
			for (UnknownClient c: this.getConnectedClients()) {
				c.queuePacket(msgPacket);
			}
		}
	}

	@Override
	public void onPacketReceived(UnknownClient client, Packet packet) {
		if (packet instanceof Packet1ChatMessage) {
			String msg = ((Packet1ChatMessage)packet).getMessage();
			String str = client.getClientTag() + ": " + msg;
			
			try {
				Packet1ChatMessage msgPacket = (Packet1ChatMessage) this.createPacket(1);
				msgPacket.setVariables(str);
				for (UnknownClient c: this.getConnectedClients()) {
					c.queuePacket(msgPacket);
				}
			} catch (ProtocolViolationException e) {
				
			}
		}
	}

	@Override
	public void mainLoop() {
		//synchronized (this.getConnectedClients()) {
		//	for (UnknownClient client : this.getConnectedClients()) {
		//		System.out.println("Kicking client: " + client.getSocket().getInetAddress().getHostAddress());
		//		client.eject("Server shutting down for mainitence.");
		//	}
	//}
		
	}
}

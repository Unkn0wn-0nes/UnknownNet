/** Copyright 2014 Unkn0wn0ne

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. **/
package com.Unkn0wn0ne.unknownet.tests;

import java.util.logging.Level;

import com.Unkn0wn0ne.unknownnet.server.UnknownClient;
import com.Unkn0wn0ne.unknownnet.server.UnknownServer;
import com.Unkn0wn0ne.unknownnet.server.logging.LogType;
import com.Unkn0wn0ne.unknownnet.server.logging.UnknownLogger;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

public class ChatTest extends UnknownServer{
	
	public ChatTest() {
		super();
		this.registerPacket(1, Packet1ChatMessage.class);
		this.startServer();
	}
	

	@Override
	public boolean handleNewConnection(UnknownClient client, String[] loginData) {
		// Check our login data and make sure the client sent the password, which for testing purposes is 'Password'
		if (loginData == null) {
			return false;
		} else if (!loginData[0].equalsIgnoreCase("Password")) {
			return false;
		}
		
		// Get the username the client sent and set it as a tag so we can identify the client
		String username = loginData[1];
		client.setObjectTag(username);
		Packet1ChatMessage msgPacket = null;
		
		try {
			msgPacket = (Packet1ChatMessage) this.createPacket(1);
		} catch (ProtocolViolationException e) {
			UnknownLogger.log(Level.SEVERE, LogType.IMPLEMENTATION, "Failed to create message packet. :(");
		}
		
		// Let the other clients know someone connected
		msgPacket.setVariables(username + " has joined our server!");
		
		// Get the number of clients connected.
		int num = 0;
		synchronized (this.getConnectedClients()) {
			int numClients = this.getConnectedClients().size();
			msgPacket.setRecipentCount(numClients);
			for (UnknownClient c: this.getConnectedClients()) {
				num++;
				c.queuePacket(msgPacket);
			}
		}
		
		// Let the new client know how many other clients are connected.
		if (num == 0) 
			num = 1;
		try {
			Packet1ChatMessage	msgPacket2 = (Packet1ChatMessage) this.createPacket(1);
			msgPacket2.setVariables("Welcome to our chat server! There are : " + num + " client(s) connected right now.");
			client.queuePacket(msgPacket2);
		} catch (ProtocolViolationException e) {
			
		}
		
		
		// Allow the client into the server
		return true;
	}

	@Override
	public void onClientLeave(UnknownClient client) {
		// Let the clients know that a client has left
		try {
			Packet1ChatMessage msgPacket = (Packet1ChatMessage) this.createPacket(1);
			msgPacket.setVariables(client.getClientTag() + " has left the server.");
			synchronized (this.getConnectedClients()) {
				int numClients = this.getConnectedClients().size();
				msgPacket.setRecipentCount(numClients);
				for (UnknownClient c: this.getConnectedClients()) {
					c.queuePacket(msgPacket);
				}
			}
		} catch (ProtocolViolationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPacketReceived(UnknownClient client, Packet packet) {
		// Check packet type
		if (packet instanceof Packet1ChatMessage) {
			// Get message and format it to have the client's username, which happens to be it's object tag
			String msg = ((Packet1ChatMessage)packet).getMessage();
			String str = client.getClientTag() + ": " + msg;
			
			// Log the message
			UnknownLogger.log(Level.INFO, LogType.IMPLEMENTATION, "[MSG] " + str);
			
			try {
				// Send the msg to all the clients
				Packet1ChatMessage msgPacket = (Packet1ChatMessage) this.createPacket(1);
				int numClients = this.getConnectedClients().size();
				msgPacket.setRecipentCount(numClients);
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
		// We really don't do anything in our main loop for this chat server.
	}
}

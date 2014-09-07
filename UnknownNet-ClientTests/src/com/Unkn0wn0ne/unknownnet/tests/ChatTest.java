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
package com.Unkn0wn0ne.unknownnet.tests;

import com.Unkn0wn0ne.unknownet.client.UnknownClient;
import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.net.Packet;
import com.Unkn0wn0ne.unknownet.client.util.Protocol;

public class ChatTest extends UnknownClient{

	public ChatTest() {
		// We our not using SSL for this example, and our protocol version is unknownserv-dev
		super(false, "unknownserver-dev");
		// Register our packet
		this.registerPacket(1, Packet1ChatMessage.class);
		
		// Get our connection information
		String ip = "";
		int port = 4334;
		Protocol proto;
		int aPort = 4333;
		
		System.out.println("Enter chat server ip: ");
		ip = System.console().readLine();
		System.out.println("Enter chat server port: ");
		port = Integer.parseInt(System.console().readLine());
		System.out.println("Use TCP, UDP, or Dualstack: " );
		String response = System.console().readLine();
		if (response.equalsIgnoreCase("tcp")) {
			proto = Protocol.TCP;
		} else if (response.equalsIgnoreCase("udp")){
			proto = Protocol.UDP;
			System.out.println("Enter UDP authserver port: ");
		    aPort = Integer.parseInt(System.console().readLine());
		} else {
			proto = Protocol.DUALSTACK;
			System.out.println("Enter udp port: ");
			aPort = Integer.parseInt(System.console().readLine());
		}
		
		System.out.println("Enter username: ");
		String msg = System.console().readLine();
		// Set our login information, which includes the server password (Password :P) and the client username
		String[] secretLoginCode = new String[2];
		secretLoginCode[0] = "Password";
		secretLoginCode[1] = msg;
		if (proto == Protocol.TCP) {
			connectTCP(ip, port, secretLoginCode);
		} else if (proto == Protocol.UDP){
			this.connectUDP(ip, port, aPort, secretLoginCode);
		} else {
			this.connectDualstack(ip, port, aPort, secretLoginCode);
		}
		mainLoop();
	}

	private void mainLoop() {
		while (true) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Get message from command line
		    String msg = System.console().readLine();
		    if (msg == null) {
		    	return;
		    }
		    try {
		    	// Send message to server
				Packet chatPacket = this.createPacket(1);
				chatPacket.setVariables(msg);
			    this.queuePacket(chatPacket);
			} catch (ProtocolViolationException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onConnectionSuccess() {
		System.out.println("Success!");
	}

	@Override
	public void onConnectionFailed(String reason) {
		System.out.println("Failed: " + reason);
	}

	@Override
	public void onClientKicked(String reason) {
		System.out.println("Kicked: " + reason);
	}

	@Override
	public void onPacketReceived(Packet packet) {
		if (packet instanceof Packet1ChatMessage) {
			// Write message to console and the console beep code (\007)
			System.out.println(((Packet1ChatMessage)packet).getMessage());
			System.out.println("\007");
		}
	}
}
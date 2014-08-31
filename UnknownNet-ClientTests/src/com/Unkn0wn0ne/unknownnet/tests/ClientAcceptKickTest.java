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

public class ClientAcceptKickTest extends UnknownClient{

	public ClientAcceptKickTest() {
		super(false, "unknownserver-dev", false);

		this.registerPacket(1, Packet1ChatMessage.class);
		String msg = System.console().readLine();
		String[] secretLoginCode = new String[2];
		secretLoginCode[0] = "Password";
		secretLoginCode[1] = msg;
		this.connectUDP("192.168.1.16", 4334, 4333, secretLoginCode);
		mainLoop();
	}

	private void mainLoop() {
		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		    String msg = System.console().readLine();
		    if (msg == null) {
		    	return;
		    }
		    try {
				Packet chatPacket = this.createPacket(1);
				chatPacket.setVariables(msg);
			    this.queuePacket(chatPacket);
			} catch (ProtocolViolationException e) {
				// TODO Auto-generated catch block
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
	//	System.out.println("Received: " + packet.getId());
		if (packet instanceof Packet1ChatMessage) {
			System.out.println(((Packet1ChatMessage)packet).getMessage());
			System.out.println("\007");
		}
	}
}
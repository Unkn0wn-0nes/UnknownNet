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
package com.Unkn0wn0ne.unknownet.client;

import java.io.IOException;

import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownet.client.net.Packet;

class TCPClient implements IClientImplementation {

	private UnknownClient uClient;
	
	public TCPClient(UnknownClient client) {
		this.uClient = client;
	}
	
	@Override
	public boolean authenticate() {
		try {
			
			InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake) this.uClient.clientRepository.getPacket(-2);
			handshakePacket.setVariables(this.uClient.protocolVersion, (this.uClient.loginParams != null) ? true : false, this.uClient.loginParams);
			handshakePacket._write(this.uClient.dataOutputStream);
		    this.uClient.dataInputStream.readInt();
			handshakePacket.read(this.uClient.dataInputStream);
			
			if (handshakePacket.getResponse() == false) {
				// A getResponse() of false mandates a reason, so a InternalPacket1Kick will be sent explaining the reason
				this.uClient.dataInputStream.readInt();
				InternalPacket1Kick disconnectPacket = (InternalPacket1Kick)this.uClient.clientRepository.getPacket(-1);
				disconnectPacket.read(this.uClient.dataInputStream);
				String reason = disconnectPacket.getReason();
				
				this.uClient.logger.info("Internal/UnknownClient: Server is kicking us out! Message: " + reason);
				this.uClient.socket.close();
				this.uClient.onConnectionFailed(reason);
				return false;
			}
		} catch (ProtocolViolationException e) {
			this.uClient.logger.severe("Internal/UnknownClient: Failed to connect to server; a ProtocolViolationException has occurred.  (Message: " + e.getMessage() + ")");
			this.uClient.onConnectionFailed("Failed to connect to server; an ProtocolViolationException has occurred. (Message: " + e.getMessage() + ")");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			this.uClient.logger.severe("Internal/UnknownClient: Failed to connect to server; an IOException has occurred.  (Message: " + e.getMessage() + ")");
			this.uClient.onConnectionFailed("Failed to connect to server; an IOException has occurred. (Message: " + e.getMessage() + ")");
			e.printStackTrace();
			return false;
		}
		
		this.uClient.lastReceivedKeepAlive = System.currentTimeMillis();
		return true;
	}


	@Override
	public void handleConnection() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				TCPClient.this.doReadLoop();
			}
		}).start();
		
		while (this.uClient.socket.isConnected()) {
			while (!this.uClient.highsToBeSent.isEmpty()) {
				Packet highPacket = this.uClient.highsToBeSent.poll();
				try {
					highPacket._write(this.uClient.dataOutputStream);
				} catch (IOException e) {
					this.uClient.logger.severe("Internal/TCPClient: An IOException has occurred while sending a packet. Disconnecting.");
					e.printStackTrace();
					this.uClient.shouldDisconnect = true;
					this.uClient.onClientKicked("An IOException occurred while sending a a packet");
					return;
				}
				this.uClient.clientRepository.freePacket(highPacket);
			}
			
			while (!this.uClient.internalsToBeSent.isEmpty()) {
				try {
					Packet internal = this.uClient.internalsToBeSent.poll();
					internal._write(this.uClient.dataOutputStream);
					
					if (internal instanceof InternalPacket1Kick) {
						this.uClient.socket.close();
						return;
					}
					
					this.uClient.clientRepository.freePacket(internal);
				} catch (IOException e) {
					this.uClient.logger.severe("Internal/TCPClient: An IOException has occurred while sending a packet. Disconnecting.");
					e.printStackTrace();
					this.uClient.shouldDisconnect = true;
					this.uClient.onClientKicked("An IOException occurred while sending a a packet");
					return;
				}
			}
			
			while (!this.uClient.lowsToBeSent.isEmpty()) {
				Packet lowPacket = this.uClient.lowsToBeSent.poll();
				try {
					lowPacket._write(this.uClient.dataOutputStream);
				} catch (IOException e) {
					this.uClient.logger.severe("Internal/TCPClient: An IOException has occurred while sending a packet. Disconnecting.");
					e.printStackTrace();
					this.uClient.shouldDisconnect = true;
					this.uClient.onClientKicked("An IOException occurred while sending a a packet");
					return;
				}
				this.uClient.clientRepository.freePacket(lowPacket);
			}
			
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				this.uClient.logger.warning("Internal/UnknownClient: InterruptedEception occurred while sleeping client writing thread. Ignoring.");
			}
		}
	}
	
	/**
	 * Internal method
	 * This loop runs in a separate thread and handles reading the packet ids from the stream and than calling handlePacketReceive
	 */
	protected void doReadLoop() {
		while (this.uClient.socket.isConnected()) {
			try {
				if (this.uClient.dataInputStream.available() > 0) {
					int id = this.uClient.dataInputStream.readInt();
					this.uClient.handlePacketReceive(id, this.uClient.dataInputStream);
				}
			} catch (IOException e) {
				// TODO
			} catch (ProtocolViolationException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				this.uClient.logger.warning("Internal/UnknownClient: InterruptedEception occurred while sleeping client reading thread. Ignoring.");
			}
			
			if (System.currentTimeMillis() - this.uClient.lastReceivedKeepAlive > 180000) {
				this.uClient.leaveServer("Client timed out.");
				return;
			}
		}
	}
}
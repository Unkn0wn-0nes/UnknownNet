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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket5Hello;
import com.Unkn0wn0ne.unknownet.client.net.Packet;
import com.Unkn0wn0ne.unknownet.client.net.Packet.PACKET_PROTOCOL;

class DualstackClient extends UDPClient{
	
	private Socket socket;
	private DataInputStream diStream;
	private DataOutputStream doStream;
	
	public DualstackClient(UnknownClient client) {
		super(client);
	}

	@Override
	public boolean authenticate() {	
		try {
			if (!this.uClient.useSSL) {
				this.socket = new Socket(this.uClient.ipAddress, this.uClient.port);
			} else {
				SocketFactory sslSocketFactory = SSLSocketFactory.getDefault();
				this.socket = sslSocketFactory.createSocket(this.uClient.ipAddress, this.uClient.port);
			}
			
			diStream = new DataInputStream(this.socket.getInputStream());
		    doStream = new DataOutputStream(this.socket.getOutputStream());
			InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake) this.uClient.clientRepository.getPacket(-2);
			handshakePacket.setVariables(this.uClient.protocolVersion, (this.uClient.loginParams != null) ? true : false, this.uClient.loginParams);
			handshakePacket._write(doStream);
			diStream.readInt();
			handshakePacket.read(diStream);
			
			if (handshakePacket.getResponse() == false) {
				// A getResponse() of false mandates a reason, so a InternalPacket1Kick will be sent explaining the reason
				diStream.readInt();
				InternalPacket1Kick disconnectPacket = (InternalPacket1Kick)this.uClient.clientRepository.getPacket(-1);
				disconnectPacket.read(diStream);
				String reason = disconnectPacket.getReason();
				
				this.uClient.logger.info("Internal/UnknownClient: Server is kicking us out! Message: " + reason);
				this.socket.close();
				this.uClient.onConnectionFailed(reason);
				return false;	
		    }
			this.uClient.uid = diStream.readInt();
		}catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			this.uClient.onConnectionFailed("An UnknownHostException has ocurred. Message: " + uhe.getMessage());
			return false;
		} catch (IOException ie) {
			ie.printStackTrace();
			return false;
		} catch (ProtocolViolationException pe) {
			pe.printStackTrace();
			return false;
		}
		
		this.uClient.lastReceivedKeepAlive = System.currentTimeMillis();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				DualstackClient.this.doReadLoop();
			}
		}).start();
		
		return true;
	}

	@Override
	public void handleConnection() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				DualstackClient.this.doUDPReadLoop();
			}
			
		}).start();
		
		InternalPacket5Hello hello = null;
		try {
			hello = (InternalPacket5Hello) this.uClient.clientRepository.getPacket(-5);
		} catch (ProtocolViolationException e1) {
			this.uClient.logger.severe("Internal/UnknownClient: ProtocolViolationException occurred while creating Hello packet; this should never happen.");
		}
		
		if (hello == null) {
			hello = new InternalPacket5Hello();
		}
		
		for (int x = 0; x < 3; x++) {
			this.uClient.queuePacket(hello);
		}
		
		
		while (true) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {

			}
			
			this.udpWriter.reset();
			while (!this.uClient.highsToBeSent.isEmpty()) {
				Packet p2 = this.uClient.highsToBeSent.poll();
				if (p2.getProtocol() == PACKET_PROTOCOL.TCP) {
					try {
						p2._write(doStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					this.sendUdp(p2);
				}
				this.uClient.clientRepository.freePacket(p2);
			}
			
			while (!this.uClient.internalsToBeSent.isEmpty()) {
				Packet p2 = this.uClient.internalsToBeSent.poll();
				if (p2.getProtocol() == PACKET_PROTOCOL.TCP) {
					try {
						p2._write(doStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					this.sendUdp(p2);
				}
				this.uClient.clientRepository.freePacket(p2);
			}
			
			while (!this.uClient.lowsToBeSent.isEmpty()) {
				Packet p2 = this.uClient.lowsToBeSent.poll();
				if (p2.getProtocol() == PACKET_PROTOCOL.TCP) {
					try {
						p2._write(doStream);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					this.sendUdp(p2);
				}
				this.uClient.clientRepository.freePacket(p2);
			}
		}
	  }
	
	/**
	 * Internal method
	 * This loop runs in a separate thread and handles reading the packet ids from the stream and than calling handlePacketReceive
	 */
	protected void doReadLoop() {
		while (!this.socket.isClosed()) {
			try {
				if (this.diStream.available() > 0) {
					int id = this.diStream.readInt();
					this.uClient.handlePacketReceive(id, this.diStream);
				}
			} catch (IOException e) {
				this.uClient.logger.severe("Internal/DualStackClient: IOException occurred while reading from TCP stream, disconnecting...");
				e.printStackTrace();
				this.uClient.shouldDisconnect = true;
				this.uClient.onClientKicked("IOException has occurred while reading from TCP stream.");
				return;
			} catch (ProtocolViolationException e) {
				this.uClient.logger.severe("Internal/DualStackClient: ProtocolViolationException occurred while reading from TCP stream, disconnecting...");
				e.printStackTrace();
				this.uClient.shouldDisconnect = true;
				this.uClient.onClientKicked("A ProtocolViolationException has occurred while reading from TCP stream.");
				return;
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				this.uClient.logger.warning("Internal/UnknownClient: InterruptedEception occurred while sleeping client reading thread. Ignoring.");
			}
			
			if (System.currentTimeMillis() - this.uClient.lastReceivedKeepAlive > 300000) {
				this.uClient.leaveServer("Client timed out.");
				return;
			}
			
		}
	}
	
	private void sendUdp(Packet p) {
		try {
			this.dataOutputStream.writeInt(this.uClient.uid);
			p._write(this.dataOutputStream);
			this.dataOutputStream.flush();
			this.dPacket.setData(this.udpWriter.toByteArray());
			this.dPacket.setLength(this.dPacket.getData().length);
			this.dSocket.send(this.dPacket);
			this.udpWriter.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	protected void doUDPReadLoop() {
		while (!this.uClient.shouldDisconnect ) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				
			}
			
			try {
				this.closeAndLoad();
				int id = this.dataInputStream.readInt();
				this.uClient.handlePacketReceive(id, this.dataInputStream);
			} catch (IOException e) {
				this.uClient.logger.severe("Internal/DualStackClient: IOException occurred while reading from UDP stream, disconnecting...");
				e.printStackTrace();
				this.uClient.shouldDisconnect = true;
				this.uClient.onClientKicked("IOException has occurred while reading from UDP stream.");
				return;
			} catch (ProtocolViolationException e) {
				this.uClient.logger.severe("Internal/DualStackClient: ProtocolViolationException occurred while reading from UDP stream, disconnecting...");
				e.printStackTrace();
				this.uClient.shouldDisconnect = true;
				this.uClient.onClientKicked("A ProtocolViolationException has occurred while reading from UDP stream.");
				return;
			}
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
	}
}
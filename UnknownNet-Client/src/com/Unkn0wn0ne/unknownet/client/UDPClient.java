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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownet.client.net.InternalPacket5Hello;
import com.Unkn0wn0ne.unknownet.client.net.Packet;

class UDPClient implements IClientImplementation {

	protected UnknownClient uClient;
	
	protected DatagramSocket dSocket;
	protected DatagramPacket dPacket, dPacket2;
	protected ByteArrayOutputStream udpWriter;
	
	protected ByteArrayInputStream udpReader;
	
	protected DataOutputStream dataOutputStream;
	protected DataInputStream dataInputStream = null;
	
	public UDPClient(UnknownClient client) {
		this.uClient = client;
	}
	
	@Override
	public boolean authenticate() {
		try {
			Socket authSocket = null;
			if (!this.uClient.useSSL) {
				authSocket = new Socket(this.uClient.ipAddress, this.uClient.authPort);
			} else {
				SocketFactory sslSocketFactory = SSLSocketFactory.getDefault();
				authSocket = sslSocketFactory.createSocket(this.uClient.ipAddress, this.uClient.authPort);
			}
			
			DataInputStream diStream = new DataInputStream(authSocket.getInputStream());
			DataOutputStream doStream = new DataOutputStream(authSocket.getOutputStream());
			
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
				authSocket.close();
				this.uClient.onConnectionFailed(reason);
				return false;	
		    }
			this.uClient.uid = diStream.readInt();
			diStream.close();
			doStream.close();
			authSocket.close();
		}catch (UnknownHostException e1) {
			e1.printStackTrace();
			return false;	
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;	
		} catch (ProtocolViolationException e) {
			e.printStackTrace();
			return false;	
		}
		
		return true;
	}

	@Override
	public void handleConnection() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				UDPClient.this.doUDPReadLoop();
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
				Packet highPacket = this.uClient.highsToBeSent.poll();
				try {
					this.dataOutputStream.writeInt(this.uClient.uid);
					highPacket._write(this.dataOutputStream);
					this.dataOutputStream.flush();
					this.dPacket.setData(this.udpWriter.toByteArray());
					this.dPacket.setLength(this.dPacket.getData().length);
					this.dSocket.send(this.dPacket);
					this.udpWriter.reset();
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
				Packet internal = this.uClient.internalsToBeSent.poll();
				try {
					this.dataOutputStream.writeInt(this.uClient.uid);
					internal._write(this.dataOutputStream);
					this.dataOutputStream.flush();
					this.dPacket.setData(this.udpWriter.toByteArray());
					this.dPacket.setLength(this.dPacket.getData().length);
					this.dSocket.send(this.dPacket);
					this.udpWriter.reset();
					if (internal instanceof InternalPacket1Kick) {
						this.uClient.shouldDisconnect = true;
						this.dSocket.close();
						return;
					}
				} catch (IOException e) {
					this.uClient.logger.severe("Internal/TCPClient: An IOException has occurred while sending a packet. Disconnecting.");
					e.printStackTrace();
					this.uClient.shouldDisconnect = true;
					this.uClient.onClientKicked("An IOException occurred while sending a a packet");
					return;
				}
				
				this.uClient.clientRepository.freePacket(internal);
			}
			
			while (!this.uClient.lowsToBeSent.isEmpty()) {
				Packet lowPacket = this.uClient.lowsToBeSent.poll();
				try {
					this.dataOutputStream.writeInt(this.uClient.uid);
					lowPacket._write(this.dataOutputStream);
					this.dataOutputStream.flush();
					this.dPacket.setData(this.udpWriter.toByteArray());
					this.dPacket.setLength(this.dPacket.getData().length);
					this.dSocket.send(this.dPacket);
					this.udpWriter.reset();
				} catch (IOException e) {
					this.uClient.logger.severe("Internal/TCPClient: An IOException has occurred while sending a packet. Disconnecting.");
					e.printStackTrace();
					this.uClient.shouldDisconnect = true;
					this.uClient.onClientKicked("An IOException occurred while sending a a packet");
					return;
				}
				this.uClient.clientRepository.freePacket(lowPacket);
			}
		}
	}

	protected void closeAndLoad() throws IOException {
		if (this.dataInputStream  != null) {
			this.dataInputStream.close();
		}
		this.udpReader = null;
		this.dataInputStream = null;
	    
		this.dSocket.receive(this.dPacket2);
	    
		this.udpReader = new ByteArrayInputStream(this.dPacket2.getData());
		this.dataInputStream = new DataInputStream(this.udpReader);
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
		this.dataInputStream.close();
		this.dataOutputStream.close();
		this.dSocket.close();
	}

	public void setConnection(DatagramSocket dSocket, DatagramPacket dPacket, DatagramPacket dPacket2, ByteArrayOutputStream udpWriter, DataOutputStream dataOutputStream) {
		this.dSocket = dSocket;
		this.dPacket = dPacket;
		this.dPacket2 = dPacket2;
		this.udpWriter = udpWriter;
		this.dataOutputStream = dataOutputStream;
	}
}
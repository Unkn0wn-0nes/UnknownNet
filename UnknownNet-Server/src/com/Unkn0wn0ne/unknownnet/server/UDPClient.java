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
package com.Unkn0wn0ne.unknownnet.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.Unkn0wn0ne.unknownnet.server.UnknownClient;
import com.Unkn0wn0ne.unknownnet.server.UnknownServer;
import com.Unkn0wn0ne.unknownnet.server.VIOLATION_TYPE;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownnet.server.util.Protocol;

public class UDPClient extends UnknownClient {

	private String[] loginData = null;
	private int hellosReceived = 0;
	
	protected UDPClient(Socket socket, UnknownServer server) {
		super(socket, server, Protocol.UDP);
	}

	@Override
	protected void processPacket(Packet uPacket) {
		if (this.hasBeenEjected) {
			// Don't handle it. Client has been ejected and this thread will be
			// shutting down.
			return;
		}

		// Our sandbox has to be modified slightly for udp, so we do the logic
		// checking here.
		if (this.clientState == -1) {
			if (!(uPacket instanceof InternalPacket2Handshake)) {
				// Client is not authenticated and the packet is not a handshake packet
				this.eject("Protocol Violation: First packet was not handshake packet.", false);
				return;
			}
		}

		switch (uPacket.getId()) {
			case -5: {
				this.hellosReceived++;
				if (this.hellosReceived > 3) {
					this.eject("Protocol Error: Invalid Hello packet received.", false);
				}
				return;
			}
			case -4: {
				if (this.clientState != 1) {
					// Client is not registered as an administrator. (Hacking?
					// Possible accident?)
					// We won't handle this, instead we'll log it to our
					// security manager, ServerGuard
					this.server.getSeverGuard().logSecurityViolation(VIOLATION_TYPE.SECURITY_ISSUE, this);
				}
				return;
			}
			case -3: {
				this.receivedKeepAlives++;
				if (this.sendKeepAlives < this.receivedKeepAlives) {
					// Client is spamming keep-alive packets, eject them
					this.eject("Protocol Error: Invalid keep alive packet received.", false);
				}
				this.missedKeepAlives = -1;
				return;
			}
			case -2: {
				// Handshake packets should have already been handled
				this.eject("Protocol Error: Invalid HandshakePacket, you've already been authenticated.", false);
				return;
			}
			case -1: {
				InternalPacket1Kick disconnectPacket = (InternalPacket1Kick) uPacket;
				this.server.logger.info("Internal/UnknownClient: Client '" + this.getAddress().getHostAddress() + "' has disconnection. [Reason: "+ disconnectPacket.getMessage() + "]");
				this.server.handleClientLeaving(this);
				break;
			}
			default: {
				// Not an internal packet, let implementation handle it.
				this.server.onPacketReceived(this, uPacket);
				break;
			}
		}
	}

	@Override
	protected void authenticateClient() throws IOException, ProtocolViolationException {
		InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake)this.server.getRepository().getPacket(-2);
		handshakePacket.read(dataInputStream);
		String version = handshakePacket.getVersion();
		this.loginData = handshakePacket.getLoginData();
		if (!version.equalsIgnoreCase(this.server.getProtocolVersion())) {
			handshakePacket.setVariables(false);
			handshakePacket._write(dataOutputStream);
			this.eject("Protocol Error: Protocol version mismatch. (Server = " + this.server.getProtocolVersion() + " You = " + version + ") Have you updated your client?", false);
			return;
		} else {
			server.freeClientFromSandbox(this);
			handshakePacket.setVariables(true);
			handshakePacket._write(dataOutputStream);
			dataOutputStream.writeInt(this.clientId);
		}
	}

	@Override
	protected void handleConnection() {
		try {
			this.connection.close();
		} catch (IOException e2) {
			
		}
		
		this.connection = null;
		
		while (!this.udpActive) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				
			}
			
			if (this.datagramsToBeProcessed.isEmpty()) {
				continue;
			} else {
				this.udpActive = true;
				if (!this.server.handleNewConnection(this, loginData)) {
					if (!this.hasBeenEjected()) {
						this.eject("Server has refused to authenicate you.", false);
						return;
					}
				} else {
					this.setState(0); // Allows the client to escape the sandbox and access the rest of the server
				}
				break;
			}
		}
		this.udpWriter = new ByteArrayOutputStream();
		this.dataOutputStream = new DataOutputStream(this.udpWriter);
		while (!this.hasBeenEjected) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				
			}
			
			while (!this.datagramsToBeProcessed.isEmpty()) {
				this.processPacket(this.datagramsToBeProcessed.poll());
			}
			
			try {
				this.dataOutputStream.flush();
			} catch (IOException e1) {
			}
			this.udpWriter.reset();
			
			while (!this.highPriorityToBeSent.isEmpty()) {
				try {
					this.sendPacket(this.highPriorityToBeSent.poll());
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}
			}
			
			while (!this.internalsToBeSent.isEmpty()) {
				Packet internalPacket = this.internalsToBeSent.poll();
				try {
					this.sendPacket(internalPacket);
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}

				if (internalPacket instanceof InternalPacket1Kick) {
						return;
				}
			}
			
			
			while (!this.lowPriorityToBeSent.isEmpty()) {
				try {
					this.sendPacket(this.lowPriorityToBeSent.poll());
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}
			}
		}
	}
	
	private void sendPacket(Packet p) throws IOException {
		this.udpWriter.reset();
		p._write(dataOutputStream);
		this.datagram.setData(this.udpWriter.toByteArray());
		this.datagram.setLength(this.datagram.getData().length);
		this.server.sendDatagram(this.datagram);
	}

	@Override
	protected void shutdown() {
		this.hasBeenEjected = true;
	}
}

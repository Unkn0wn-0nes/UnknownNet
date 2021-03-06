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

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

import com.Unkn0wn0ne.unknownnet.server.logging.LogType;
import com.Unkn0wn0ne.unknownnet.server.logging.UnknownLogger;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket1Kick;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket2Handshake;
import com.Unkn0wn0ne.unknownnet.server.net.Packet;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownnet.server.util.Protocol;

public class TCPClient extends UnknownClient {

	protected TCPClient(Socket socket, UnknownServer server) {
		super(socket, server, Protocol.TCP);
	}

	@Override
	protected void processPacket(Packet packet) {
		if (this.hasBeenEjected) {
			// Don't handle it. Client has been ejected and this thread will be
			// shutting down.
			this.server.getRepository().freePacket(packet);
			return;
		}
		switch (packet.getId()) {
			case -5: {
				// Hello packets are not sent in TCP, there is no need.
				this.eject("Protocol Error: Invalid Hello packet.", false);
				this.server.getRepository().freePacket(packet);
				return;
			}
			case -4: {
				if (this.clientState != 1) {
					// Client is not registered as an administrator. (Hacking?
					// Possible accident?)
					// We won't handle this, instead we'll log it to our
					// security manager, ServerGuard
					this.server.getSeverGuard().logSecurityViolation(
							VIOLATION_TYPE.SECURITY_ISSUE, this);
				}
				this.server.getRepository().freePacket(packet);
				return;
			}
			case -3: {
				this.receivedKeepAlives++;
				if (this.sendKeepAlives < this.receivedKeepAlives) {
					// Client is spamming keep-alive packets, eject them
					this.eject(
							"Protocol Error: Invalid keep alive packet received.",
							false);
				}
				this.missedKeepAlives = -1;
				this.server.getRepository().freePacket(packet);
				return;
			}
			case -2: {
				// Handshake packets are handled before this method is used, and
				// can only be sent once in the beginning of the connection.
				// This should not happen and violates the protocol
				// specification. This results in termination of the connection.
				this.eject(
						"Protocol Error: You've already been authenticated.",
						false);
				this.server.getRepository().freePacket(packet);
				return;
			}
			case -1: {
				InternalPacket1Kick disconnectPacket = (InternalPacket1Kick) packet;
				UnknownLogger.log(Level.INFO, LogType.NETWORKING, "Internal/UnknownClient: Client '"
						+ this.getAddress().getHostAddress()
						+ "' has disconnection. [Reason: "
						+ disconnectPacket.getMessage() + "]");
				this.server.handleClientLeaving(this);
				this.server.getRepository().freePacket(packet);
				break;
			}
			default: {
				// Not an internal packet, let implementation handle it.
				this.server.onPacketReceived(this, packet);
				this.server.getRepository().freePacket(packet);
				break;
			}
		}
	}

	@Override
	protected void authenticateClient() throws IOException, ProtocolViolationException {
		String[] loginData = null;
		InternalPacket2Handshake handshakePacket = (InternalPacket2Handshake)this.server.getRepository().getPacket(-2);
		handshakePacket.read(dataInputStream);
			String version = handshakePacket.getVersion();
			loginData = handshakePacket.getLoginData();
			if (!version.equalsIgnoreCase(this.server.getProtocolVersion())) {
				handshakePacket.setVariables(false);
				handshakePacket._write(dataOutputStream);
				this.eject("Protocol Error: Protocol version mismatch. (Server = " + this.server.getProtocolVersion() + " You = " + version + ") Have you updated your client?", false);
				return;
			} else if (!this.server.handleNewConnection(this, loginData)) {
				handshakePacket.setVariables(false);
				handshakePacket._write(dataOutputStream);
				if (!this.hasBeenEjected()) {
					this.eject("Server has refused to authenicate you.", false);
					return;
				}
			} else {
				this.setState(0); // Allows the client to escape the sandbox and access the rest of the server
				server.freeClientFromSandbox(this);
				handshakePacket.setVariables(true);
				handshakePacket._write(dataOutputStream);
			}
	}

	@Override
	protected void handleConnection() {
		while (!this.connection.isClosed()) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				
			}
			
			try {
				if (dataInputStream.available() > 0) {
					int id = dataInputStream.readInt();
					Packet packet = this.server.getRepository().getPacket(id);
					packet.read(this.dataInputStream);
					this.processPacket(packet);
				}
			} catch (IOException e) {
				this.eject("Networking Error: " + e.getMessage(), false);
			} catch (ProtocolViolationException e) {
				this.eject("Protocol Error: " + e.getMessage(), false);
			}
			
			while (!this.highPriorityToBeSent.isEmpty()) {
				try {
					Packet highPacket = this.highPriorityToBeSent.poll();
					highPacket._write(this.dataOutputStream);
					highPacket.setRecipentCount(highPacket.getRecipentCount() - 1);
					if (highPacket.getRecipentCount() == 0) {
						this.server.getRepository().freePacket(highPacket);
					}
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}
			}
			
			while (!this.internalsToBeSent.isEmpty()) {
				Packet internalPacket = this.internalsToBeSent.poll();
				
				try { 
					internalPacket._write(this.dataOutputStream);
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				} 
				
				if (internalPacket instanceof InternalPacket1Kick) {
						return;
				}
				
				internalPacket.setRecipentCount(internalPacket.getRecipentCount() - 1);
				if (internalPacket.getRecipentCount() == 0) {
					this.server.getRepository().freePacket(internalPacket);
				}
			}
			
			while (!this.lowPriorityToBeSent.isEmpty()) {
				try {
					Packet lowPacket = this.lowPriorityToBeSent.poll();
					lowPacket._write(this.dataOutputStream);
					lowPacket.setRecipentCount(lowPacket.getRecipentCount() - 1);
					if (lowPacket.getRecipentCount() == 0) {
						this.server.getRepository().freePacket(lowPacket);
					}
				} catch (IOException e) {
					this.eject("IOException occurred while sending data to stream.", false);
				}
			}
		} 
	}

	@Override
	protected void shutdown() {
		try {
			this.connection.close();
		} catch (IOException e) {
		}
	}
}

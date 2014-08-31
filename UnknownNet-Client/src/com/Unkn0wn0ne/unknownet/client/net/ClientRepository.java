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
package com.Unkn0wn0ne.unknownet.client.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;

/**
 * ClientRepository - Manages the protocol for both internal UnknownNet connections and your implementation.
 * @author Unkn0wn0ne
 */
public class ClientRepository {
	private Logger logger = Logger.getLogger("UnknownNet");
	private ConcurrentHashMap<Integer, Packet> registeredPackets = new ConcurrentHashMap<Integer, Packet>();
	
	/**
	 * Called by UnknownClient, registers the internal UnknownNet packets for use with communication to an UnknownNet server
	 */
	public void init() {
		logger.info("Internal/ClientRepository: Init");
		
		// Register our internal packets
		registerPacket(-1, InternalPacket1Kick.class);
		registerPacket(-2, InternalPacket2Handshake.class);
		registerPacket(-3, InternalPacket3KeepAlive.class);
		// TODO: registerPacket(-4, InternalPacket4AdministrativeAction.class);
		registerPacket(-5, InternalPacket5Hello.class);
	}
	
	/**
	 * Registers a packet for use on the network. This only needs to be called once per packet class you have
	 * @param id The id that you'd like to register your packet to.
	 * @param packet The class of the packet you'd like to register
	 */
	public void registerPacket(int id, Class<? extends Packet> packet){
		logger.info("Internal/ClientRepository: Registering packet with id '" + id + "' to class '" + packet.getName() + "'");
		Packet nPacket;
		try {
			nPacket = packet.newInstance();
			this.registeredPackets.put(id, nPacket);
		} catch (InstantiationException e) {
			logger.severe("Failed to register packet id '" + id + "' An InstantiationException has occurred.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			logger.severe("Failed to register packet id '" + id + "' An IllegalAccessException has occurred.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a packet from the id you've specified
	 * @param id The id of the packet object you'd like to get
	 * @return A packet created from the id you've specified
	 * @throws ProtocolViolationException If there was an issue retrieving the packet
	 */
	public Packet getPacket(int id) throws ProtocolViolationException {
		if (this.registeredPackets.get(id) == null) {
			logger.severe("Internal/ClientRepository: Protocol violation, attempted to access a packet with non-existant id '" + id + "' disconnecting. (Did you register the packet using UnknownClient.registerPacket?)");
			throw new ProtocolViolationException("A protocol violation has occurred. Attempted to access a packet with a non-existant id '" + id + "' (Did you register the packet using UnknownClient.registerPacket?)");
		}
		return this.registeredPackets.get(id);
	}
}

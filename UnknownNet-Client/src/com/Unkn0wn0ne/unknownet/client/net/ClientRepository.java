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

import com.Unkn0wn0ne.unknownet.client.distributed.DistributedObject;
import com.Unkn0wn0ne.unknownet.client.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownet.client.util.ObjectPool;

/**
 * ClientRepository - Manages the protocol for both internal UnknownNet connections and your implementation.
 * @author Unkn0wn0ne
 */
public class ClientRepository {
	private Logger logger = Logger.getLogger("UnknownNet");
	private ConcurrentHashMap<Integer, ObjectPool<Packet>> registeredPacketPools = new ConcurrentHashMap<Integer, ObjectPool<Packet>>();
	private ConcurrentHashMap<String, Class<? extends DistributedObject>> registeredDistributedObjects = new ConcurrentHashMap<String, Class<? extends DistributedObject>>();
	
	/**
	 * Called by UnknownClient, registers the internal UnknownNet packets for use with communication to an UnknownNet server
	 */
	public void init() {
		this.logger.info("Internal/ClientRepository: Init");
		
		// Register our internal packets
		registerPacket(-1, InternalPacket1Kick.class);
		registerPacket(-2, InternalPacket2Handshake.class);
		registerPacket(-3, InternalPacket3KeepAlive.class);
		// TODO: registerPacket(-4, InternalPacket4AdministrativeAction.class);
		registerPacket(-5, InternalPacket5Hello.class);
		registerPacket(-6, InternalPacket6DistributedObjectCreation.class);
		registerPacket(-7, InternalPacket7DestroyDistributedObject.class);
		registerPacket(-8, InternalPacket8DistributedObjectEdit.class);
		registerPacket(-9, InternalPacket9LeaveZone.class);
	}
	
	/**
	 * Registers a packet for use on the network. This only needs to be called once per packet class you have
	 * @param id The id that you'd like to register your packet to.
	 * @param packet The class of the packet you'd like to register
	 */
	public void registerPacket(int id, Class<? extends Packet> packet){
		this.logger.info("Internal/ClientRepository: Registering packet with id '" + id + "' to class '" + packet.getName() + "'");
		Packet nPacket;
		try {
			nPacket = packet.newInstance();
			ObjectPool<Packet> packetPool = new ObjectPool<Packet>();
			packetPool.setType(nPacket);
			this.registeredPacketPools.put(id, packetPool);
		} catch (InstantiationException e) {
			this.logger.severe("Failed to register packet id '" + id + "' An InstantiationException has occurred.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			this.logger.severe("Failed to register packet id '" + id + "' An IllegalAccessException has occurred.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a packet from the id you've specified
	 * @param id The id of the packet object you'd like to get
	 * @return A packet created from the id you've specified
	 * @throws ProtocolViolationException If there was an issue retrieving the packet
	 */
	public Packet getPacket(int id) {
		if (this.registeredPacketPools.get(id) == null) {
			this.logger.severe("Internal/ClientRepository: Protocol violation, attempted to access a packet with a non-existant id '" + id + "' disconnecting. (Did you register the packet using UnknownClient.registerPacket?)");
			throw new ProtocolViolationException("A protocol violation has occurred. Attempted to access a packet with a non-existant id '" + id + "' (Did you register the packet using UnknownClient.registerPacket?)");
		}
		try {
			return this.registeredPacketPools.get(id).getObject();
		} catch (InstantiationException e) {
			this.logger.severe("Internal/ClientRepository: Failed to allocate packet id '" + id + "', an InstantiationException has occurrred.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			this.logger.severe("Internal/ClientRepository: Failed to allocate packet id '" + id + "', an IllegalAccessException has occurrred.");
			e.printStackTrace();
		}
		return null;
	}

	public void freePacket(Packet packet) {
		this.registeredPacketPools.get(packet.getId()).freeObject(packet);
	}
	
	public void registerDistributedObject(String type, Class<? extends DistributedObject> dobject) {
		this.logger.info("Internal/ClientRepository: Registering DistributedObject type '" + type + "' to class '" + dobject.getName());
		this.registeredDistributedObjects.put(type, dobject);
	}
	
	public ObjectPool<Packet> getPacketPool(int id) {
		return this.registeredPacketPools.get(id);
	}
	
	public DistributedObject createDistributedObject(String type) {
		if (this.registeredDistributedObjects.get(type) == null) {
			this.logger.severe("Internal/ClientRepository: Protocol violation, attempted to access a DistributedObject with a non-existant type '" + type + "' disconnecting. (Did you register the DistributedObject using UnknownClient.registerDistributedObject?)");
			throw new ProtocolViolationException("A protocol violation has occurred. Attempted to access a DistributedObject with a non-existant type '" + type + "' (Did you register the DistributedObject using UnknownClient.registerDistributedObject?)");
		}
		
		try {
			return this.registeredDistributedObjects.get(type).newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}

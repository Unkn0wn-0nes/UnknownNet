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
package com.Unkn0wn0ne.unknownnet.server.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;
import com.Unkn0wn0ne.unknownnet.server.util.ObjectPool;

public class ServerRepository {

	private Logger logger = Logger.getLogger("UnknownNet");
	private ConcurrentHashMap<Integer, ObjectPool<Packet>> registeredPacketPools = new ConcurrentHashMap<Integer, ObjectPool<Packet>>();
	
	
	public void init() {
		logger.info("Internal/ServerRepository: Init");
		registerPacket(-1, InternalPacket1Kick.class);
		registerPacket(-2, InternalPacket2Handshake.class);
		registerPacket(-3, InternalPacket3KeepAlive.class);
		registerPacket(-4, InternalPacket4AdministrativeAction.class);
		registerPacket(-5, InternalPacket5Hello.class);
	}
	
	public void registerPacket(int id, Class<? extends Packet> packet){
		logger.info("Internal/ServerRepository: Registering packet with id '" + id + "' to class '" + packet.getName() + "'");
		try {
			ObjectPool<Packet> pool = new ObjectPool<Packet>();
			pool.setType(packet.newInstance());
			this.registeredPacketPools.put(id, pool);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Packet getPacket(int id) throws ProtocolViolationException {
		if (this.registeredPacketPools.get(id) == null) {
			logger.severe("Internal/ServerRepository: Protocol security violation, attempted to access a packet with non-existant id '" + id + "' ejecting client");
			throw new ProtocolViolationException("A protocol security violation has occurred. Attempted to access a packet with a non-existant id '" + id + "'");
		}
		try {
			return (Packet)this.registeredPacketPools.get(id).getObject();
		} catch (InstantiationException e) {
			this.logger.severe("Internal/ServerRepository: Failed to allocate packet id '" + id + "', an InstantiationException has occurrred.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			this.logger.severe("Internal/ServerRepository: Failed to allocate packet id '" + id + "', an IllegalAccessException has occurrred.");
			e.printStackTrace();
		}
		return null;
	}
	
	public void freePacket(Packet packet) {
		this.registeredPacketPools.get(packet.getId()).freeObject(packet);
	}
}

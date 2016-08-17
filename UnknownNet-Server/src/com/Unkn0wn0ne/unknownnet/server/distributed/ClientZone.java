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
package com.Unkn0wn0ne.unknownnet.server.distributed;

import java.util.ArrayList;
import java.util.List;

import com.Unkn0wn0ne.unknownnet.server.UnknownClient;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket6DistributedObjectCreation;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket7DestroyDistributedObject;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket8DistributedObjectEdit;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket9LeaveZone;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

public class ClientZone {

	private List<UnknownClient> clients = new ArrayList<UnknownClient>();
	private ObjectManager objManager = new ObjectManager();
	private int id = 0;
	private long objCount = Long.MIN_VALUE; 
	
	public ClientZone(int id) {
		this.id = id;
	}
	
	public void spawnDistributedObject(DistributedObject object) {
		this.objCount++;
		object.setId(this.objCount);
		object.setZoneId(this.id);
		this.objManager.addDistributedObject(object);
		sendObjectToClients(object);
	}

	private void sendObjectToClients(DistributedObject object) {
		InternalPacket6DistributedObjectCreation objCreationPacket = null;
		
		synchronized (this.clients) {
			for (int i = 0; i < this.clients.size(); i++) {
				UnknownClient client = this.clients.get(i);
				if (objCreationPacket == null) {
					try {
						objCreationPacket = (InternalPacket6DistributedObjectCreation) client.getServer().createPacket(-6);
						objCreationPacket.setRecipentCount(this.clients.size());
						objCreationPacket.setVariables(object.toString());
					} catch (ProtocolViolationException e) {
						objCreationPacket = new InternalPacket6DistributedObjectCreation();
					}
				}
				client.queuePacket(objCreationPacket);
			}
		}
	}
	
	public void destroyObject(long id) {
		synchronized (this.clients) {
			this.objManager.removeDistributedObject(id);
			
			InternalPacket7DestroyDistributedObject objDestroyPacket = null;
			
			for (int i = 0; i < this.clients.size(); i++) {
				UnknownClient client = this.clients.get(i);
				if (objDestroyPacket == null) {
					try {
						objDestroyPacket = (InternalPacket7DestroyDistributedObject) client.getServer().createPacket(-7);
					} catch (ProtocolViolationException e) {
						objDestroyPacket = new InternalPacket7DestroyDistributedObject();
					}
					objDestroyPacket.setRecipentCount(this.clients.size());
					objDestroyPacket.setVariables(id, this.id);
				}
				client.queuePacket(objDestroyPacket);
			}
		}
	}

	public void addClient(UnknownClient client) {
		synchronized (this.clients) {
			if (this.clients.contains(client)) {
				// Client is already in, don't duplicate.
				return;
			}
			this.clients.add(client);
		}
		this.objManager.prepareClient(client);
	}
	
	public void removeClient(UnknownClient client) {
		synchronized (this.clients) {
			this.clients.remove(client);
		}
		
		try {
			InternalPacket9LeaveZone zonePacket = (InternalPacket9LeaveZone) client.getServer().createPacket(-9);
			zonePacket.setVariables(this.id);
			client.queuePacket(zonePacket);
		} catch (ProtocolViolationException e) {
			
		}
	}
	
	public void updateObject(long id) {
		this.objManager.getDistributedObject(id).computeUpdate();
		synchronized (this.clients) {
			InternalPacket8DistributedObjectEdit objEditPacket = null;
			
			for (int i = 0; i < this.clients.size(); i++) {
				UnknownClient client = this.clients.get(i);
				if (objEditPacket == null) {
					try {
						objEditPacket = (InternalPacket8DistributedObjectEdit) client.getServer().createPacket(-8);
					} catch (ProtocolViolationException e) {
						objEditPacket = new InternalPacket8DistributedObjectEdit();
					}
					
					objEditPacket.setRecipentCount(this.clients.size());
					objEditPacket.setVariables(this.objManager.getDistributedObject(id).toString());
				}
				client.queuePacket(objEditPacket);
			}
		}
	}
}
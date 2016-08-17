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

import java.util.HashMap;

import com.Unkn0wn0ne.unknownnet.server.UnknownClient;
import com.Unkn0wn0ne.unknownnet.server.net.InternalPacket6DistributedObjectCreation;
import com.Unkn0wn0ne.unknownnet.server.net.errors.ProtocolViolationException;

public class ObjectManager {

	private HashMap<Long, DistributedObject> distributedObjects = new HashMap<Long, DistributedObject>();

	public void addDistributedObject(DistributedObject object) {
		object.computeUpdate();
		synchronized (this.distributedObjects) {
			this.distributedObjects.put(object.getId(), object);
		}
	}

	public void removeDistributedObject(long id) {
		synchronized (this.distributedObjects) {
			this.distributedObjects.remove(id);
		}
	}
	
	public DistributedObject getDistributedObject(long id) {
		synchronized (this.distributedObjects) {
			return this.distributedObjects.get(id);
		}
	}
	
	public void prepareClient(UnknownClient client) {
		Object[] objs = null;
		synchronized (this.distributedObjects) {
			objs = this.distributedObjects.values().toArray();
		}
		
		for (int i = 0; i < objs.length; i++) {
			Object o = objs[i];
			try {
				InternalPacket6DistributedObjectCreation objPacket = (InternalPacket6DistributedObjectCreation) client.getServer().createPacket(-6);
				objPacket.setVariables(o.toString());
				client.queuePacket(objPacket);
			} catch (ProtocolViolationException e) {
				
			}
		}
	}
}
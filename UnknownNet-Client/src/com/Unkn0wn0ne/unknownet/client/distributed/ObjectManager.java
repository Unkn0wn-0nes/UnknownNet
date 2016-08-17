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
package com.Unkn0wn0ne.unknownet.client.distributed;

import java.util.ArrayList;
import java.util.HashMap;

import com.Unkn0wn0ne.unknownet.client.UnknownClient;

public class ObjectManager {

	private HashMap<Long, DistributedObject> distributedObjects = new HashMap<Long, DistributedObject>();
	
	public void addDistributedObject(DistributedObject dobject) {
		synchronized (this.distributedObjects) {
			this.distributedObjects.put(dobject.getId(), dobject);
		}
	}
	
	public void removeDistributedObject(long l) {
		synchronized (this.distributedObjects) {
			this.distributedObjects.remove(l);
		}
	}
	
	public DistributedObject getDistributedObject(long id) {
		synchronized (this.distributedObjects) {
			return this.distributedObjects.get(id);
		}
	}
	
	public void clearObjects() {
		synchronized (this.distributedObjects) {
			this.distributedObjects.clear();
		}
	}

	public void leaveZone(int id, UnknownClient unknownClient) {
		ArrayList<Long> ids = new ArrayList<Long>();
		
		synchronized (this.distributedObjects) {
			for (DistributedObject dObj : this.distributedObjects.values()) {
				if (dObj.getZoneId() == id) {
					ids.add(dObj.getId());
					continue;
				}
			}
		}
		
		Long[] longs = new Long[ids.size()];
		unknownClient.onZoneLeave(id, ids.toArray(longs));
		
		for (long did : longs) {
			synchronized (this.distributedObjects) {
				this.distributedObjects.remove(did);
			}
		}
	}
}

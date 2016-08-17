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
package com.Unkn0wn0ne.unknownnet.server.util;

import java.util.LinkedList;

public class ObjectPool<T extends PoolableObject> {

	private T poolableObjectType;
	private LinkedList<T> poolableObjects = new LinkedList<T>();
	private int maxSize = Integer.MAX_VALUE;

	@SuppressWarnings("unchecked")
	public T getObject() throws InstantiationException, IllegalAccessException {
		if (poolableObjects.isEmpty()) {
			return (T) poolableObjectType.getClass().newInstance();
		} else {
			T object = poolableObjects.poll();
			if (object == null) {
				return (T) this.poolableObjectType.getClass().newInstance();
			} else {
				return object;
			}
		}
	}

	public void freeObject(T object) {
		if (object == null)
			return;
		if (this.poolableObjects.size() < this.maxSize) {
			object.resetVariables();
			this.poolableObjects.add(object);
		}
	}

	public void setType(T newInstance) {
		this.poolableObjectType = newInstance;
	}

	public int getMaximumSize() {
		return this.maxSize;
	}

	public void setMaximumSize(int size) {
		this.maxSize = size;
	}
	
	public void clearPool() {
		this.poolableObjects.clear();
	}
}
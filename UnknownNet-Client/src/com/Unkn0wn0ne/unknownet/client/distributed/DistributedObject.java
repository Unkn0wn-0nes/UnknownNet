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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is the basis for UnknownNet's distributed networking system
 * DistributedObjects hold various variable types (bytes and byte arrays, strings, integers, floats, shorts, longs, and booleans) that when altered can be flagged to be distributed to all clients that the object is of interest to.
 * @author John <Unkn0wn0ne>
 */
public abstract class DistributedObject {

	private Map<String, Byte> byte_values = new HashMap<String, Byte>();
	private Map<String, byte[]> byte_array_values = new HashMap<String, byte[]>();
	private Map<String, String> string_values = new HashMap<String, String>();
	private Map<String, Integer> integer_values = new HashMap<String, Integer>();
	private Map<String, Double> double_values = new HashMap<String, Double>();
	private Map<String, Float> float_values = new HashMap<String, Float>();
	private Map<String, Short> short_values = new HashMap<String, Short>();
	private Map<String, Long> long_values = new HashMap<String, Long>();
	private Map<String, Boolean> boolean_values = new HashMap<String, Boolean>();
	
	private long  id;
	private int zId = -1;
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getId() {
		return this.id;
	}
	
	public abstract String getObjectType();
	
	public void setByteValue(String title, byte b) {
		synchronized (this.byte_values) {
			this.byte_values.put(title, b);
		}
	}
	
	public byte getByteValue(String title) {
		synchronized (this.byte_values) {
			return this.byte_values.get(title);
		}
	}
	
	public Set<String> getByteValueKeys() {
		synchronized (this.byte_values) {
			return this.byte_values.keySet();
		}
	}
	
	public void setByteArrayValue(String title, byte[] array) {
		synchronized (this.byte_array_values) {
			this.byte_array_values.put(title, array);
		}
	}

	public byte[] getByteArrayValue(String title) {
		synchronized (this.byte_array_values) {
			return this.byte_array_values.get(title);
		}
	}
	
	public Set<String> getByteArrayValueKeys() {
		synchronized (this.byte_array_values) {
			return this.byte_array_values.keySet();
		}
	}
	
	public void setStringValue(String title, String value) {
		synchronized (this.string_values) {
			this.string_values.put(title, value);
		}
	}
	
	public String getStringValue(String title) {
		synchronized (this.string_values) {
			return this.string_values.get(title);
		}
	}
	
	public Set<String> getStringValueKeys() {
		synchronized (this.string_values) {
			return this.string_values.keySet();
		}
	}
	
	public void setIntegerValue(String title, int value) {
		synchronized (this.integer_values) {
			this.integer_values.put(title, value);
		}
	}
	
	public int getIntegerValue(String title) {
		synchronized (this.integer_values) {
			return this.integer_values.get(title);
		}
	}
	
	public Set<String> getIntegerValueKeys() {
		synchronized (this.integer_values) {
			return this.integer_values.keySet();
		}
	}
	
	public void setDoubleValue(String title, double value) {
		synchronized (this.double_values) {
			this.double_values.put(title, value);
		}
	}
	
	public double getDoubleValue(String title) {
		synchronized (this.double_values) {
			return this.double_values.get(title);
		}
	}
	
	public Set<String> getDoubleValueKeys() {
		synchronized (this.double_values) {
			return this.double_values.keySet();
		}
	}
	
	public void setFloatValue(String title, float value) {
		synchronized (this.float_values) {
			this.float_values.put(title, value);
		}
	}
	
	public float getFloatValue(String title) {
		synchronized (this.float_values) {
			return this.float_values.get(title);
		}
	}
	
	public Set<String> getFloatValueKeys() {
		synchronized (this.float_values) {
			return this.float_values.keySet();
		}
	}
	
	public void setShortValue(String title, short value) {
		synchronized (this.short_values) {
			this.short_values.put(title, value);
		}
	}
	
	public short getShortValue(String title) {
		synchronized (this.short_values) {
			return this.short_values.get(title);
		}
	}
	
	public Set<String> getShortValueKeys() {
		synchronized (this.short_values) {
			return this.short_values.keySet();
		}
	}
	
	public void setLongValue(String title, long value) {
		synchronized (this.long_values) {
			this.long_values.put(title, value);
		}
	}
	
	public long getLongValue(String title) {
		synchronized (this.long_values) {
			return this.long_values.get(title);
		}
	}
	
	public Set<String> getLongValueKeys() {
		synchronized (this.long_values) {
			return this.long_values.keySet();
		}
	}
	
	public void setBooleanValue(String title, boolean value) {
		synchronized (this.long_values) {
			this.boolean_values.put(title, value);
		}
	}
	
	public boolean getBooleanValue(String title) {
		synchronized (this.boolean_values) {
			return this.boolean_values.get(title);
		}
	}
	
	public Set<String> getBooleanValueKeys() {
		synchronized (this.boolean_values) {
			return this.boolean_values.keySet();
		}
	}
	
	public void setZoneId(int id) {
		this.zId  = id;
	}
	
	public int getZoneId() {
		return this.zId;
	}
	
	@Override
	public String toString() {
	StringBuilder strBuilder = new StringBuilder();
		
		strBuilder.append("name=");
		strBuilder.append(this.getObjectType());
		strBuilder.append(";");
		
		strBuilder.append("id= ");
		strBuilder.append(this.id);
		strBuilder.append(";");
		
		strBuilder.append("zId=");
		strBuilder.append(this.zId);
		strBuilder.append(";");
		
		strBuilder.append("numBytes=");
		strBuilder.append(this.getByteValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getByteValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getByteValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numByteArrays=");
		strBuilder.append(this.getByteArrayValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getByteArrayValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getByteArrayValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numStrings=");
		strBuilder.append(this.getStringValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getStringValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getStringValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numIntegers=");
		strBuilder.append(this.getIntegerValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getIntegerValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getIntegerValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numDoubles=");
		strBuilder.append(this.getDoubleValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getDoubleValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getDoubleValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numFloats=");
		strBuilder.append(this.getFloatValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getFloatValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getFloatValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numShorts=");
		strBuilder.append(this.getShortValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getShortValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getShortValue(str));
			strBuilder.append(";");
		}
	
		strBuilder.append("numLongs=");
		strBuilder.append(this.getLongValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getLongValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getLongValue(str));
			strBuilder.append(";");
		}
		
		strBuilder.append("numBooleans=");
		strBuilder.append(this.getBooleanValueKeys().size());
		strBuilder.append(";");
		
		for (String str : this.getBooleanValueKeys()) {
			strBuilder.append(str);
			strBuilder.append("=");
			strBuilder.append(this.getBooleanValue(str));
			strBuilder.append(";");
		}
		
		return strBuilder.toString();
	}
}
/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.ogm.datastore.spi;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.ogm.datastore.spi.JSONedClassDetector;

import com.google.gson.Gson;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class JSONHelper {

	private final WrapperClassDetector classDetector;
	private final JSONedClassDetector jsonDetector;
	private final Gson gson = new Gson();

	public JSONHelper(WrapperClassDetector classDetector, JSONedClassDetector jsonDetector) {
		this.classDetector = classDetector;
		this.jsonDetector = jsonDetector;
	}

	/**
	 * Creates JSON representation based on the specified object.
	 * 
	 * @param obj
	 *            To be JSONed.
	 * @return JSON representation of the specified object.
	 */
	public String toJSON(Object obj) {
		return this.gson.toJson( obj );
	}

	/**
	 * Creates Object from the specified JSON representation based on the
	 * specified Class.
	 * 
	 * @param json
	 *            To be turned to Object.
	 * @param cls
	 *            Used to turn the JSON to object.
	 * @return Object representation of the JSON.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object fromJSON(String json, Class cls) {
		return this.gson.fromJson( json, cls );
	}

	/**
	 * Converts both key and value to Json.
	 * 
	 * @param map
	 *            Map to be converted to Jsoned key and value pairs.
	 * @return Jsoned map.
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, String> convertKeyAndValueToJsonOn(Map map) {
		Map<String, String> jsonedMap = new HashMap<String, String>();
		for ( Iterator itr = map.keySet().iterator(); itr.hasNext(); ) {
			Object k = itr.next();
			jsonedMap.put( this.toJSON( k ), this.toJSON( map.get( k ) ) );
		}

		return jsonedMap;
	}

	/**
	 * Gets object from JSON when the specified field type is one of JSONed type
	 * as the specified columnName on the specified Map.
	 * 
	 * @param field
	 *            Corresponding field to the columnName.
	 * @param columnName
	 *            Column name used on the datastore.
	 * @param map
	 *            Stores entity objects.
	 */
	public void getObjectFromJsonOn(Field field, String columnName, Map<String, Object> map) {

		if ( field.getType().isArray() ) {
			map.put( columnName, this.fromJSON( (String) map.get( columnName ), field.getType() ) );
		}
		else if ( this.classDetector.isWrapperClass( field.getType() ) ) {
			map.put( columnName, this.classDetector.castWrapperClassFrom( map.get( columnName ), field.getType() ) );
		}
		else if ( this.jsonDetector.isAssignable( field.getType() ) ) {
			map.put( columnName, this.fromJSON( (String) map.get( columnName ), field.getType() ) );
		}
	}
}

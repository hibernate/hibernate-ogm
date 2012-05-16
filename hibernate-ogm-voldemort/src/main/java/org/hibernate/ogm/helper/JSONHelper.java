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

package org.hibernate.ogm.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.helper.annotation.AnnotationFinder;
import org.hibernate.ogm.helper.annotation.embedded.EmbeddableHelper;
import org.hibernate.ogm.helper.annotation.embedded.EmbeddableObject;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class JSONHelper {

	private static final Log log = LoggerFactory.make();
	private final ConcurrentMap<String, Object> rawTuple = new ConcurrentHashMap<String, Object>();
	private final Gson gson = new GsonBuilder().registerTypeAdapter( Date.class, new JsonSerializer<Date>() {
		@Override
		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive( src.getTime() );
		}
	} ).registerTypeAdapter( Date.class, new JsonDeserializer<Date>() {

		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return new Date( json.getAsLong() );
		}
	} ).registerTypeAdapter( Calendar.class, new JsonSerializer<Calendar>() {

		@Override
		public JsonElement serialize(Calendar src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive( src.getTimeInMillis() );
		}

	} ).registerTypeAdapter( Calendar.class, new JsonDeserializer<Calendar>() {

		@Override
		public Calendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis( json.getAsLong() );
			return calendar;
		}

	} ).registerTypeAdapter( GregorianCalendar.class, new JsonSerializer<GregorianCalendar>() {

		@Override
		public JsonElement serialize(GregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive( src.getTimeInMillis() );
		}

	} ).create();

	/**
	 * Creates JSON representation based on the specified object.
	 * 
	 * @param obj
	 *            To be JSONed.
	 * @return JSON representation of the specified object.
	 */
	public String toJSON(Object obj) {
		return gson.toJson( obj );
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
		if ( json == null || json.equals( "null" ) ) {
			return null;
		}

		if ( cls.getCanonicalName().equals( "java.util.UUID" )
				|| cls.getCanonicalName().equals( "java.math.BigDecimal" )
				|| cls.getCanonicalName().equals( "java.net.URL" )
				|| cls.getCanonicalName().equals( "java.math.BigInteger" ) ) {
			return gson.fromJson( json, String.class );
		}

		return gson.fromJson( json, cls );
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
			jsonedMap.put( toJSON( k ), toJSON( map.get( k ) ) );
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

		try {
			map.put( columnName, fromJSON( (String) map.get( columnName ), field.getType() ) );
		}
		catch ( JsonParseException ex ) {

			AnnotationFinder finder = new AnnotationFinder();
			if ( !finder.isEmbeddableAnnotated( field.getType() ) ) {
				map.put( columnName, rawTuple.get( columnName ) );
			}
			else {

				Map<String, Class> columnMap = finder.findAllColumnNamesFrom( field.getType() );
				Iterator<String> itr = map.keySet().iterator();
				while ( itr.hasNext() ) {
					String k = itr.next();
					Class type = columnMap.get( k );
					if ( type != null ) {
						map.put( k, fromJSON( (String) map.get( k ), type ) );
					}
				}

				EmbeddableObject embeddableObject = new EmbeddableHelper().getObjectFromEmbeddableOn( columnName, map,
						field.getType() );
				Iterator<Entry<String, Object>> tupleItr = embeddableObject.getEntrySetFromTuple();
				while ( tupleItr.hasNext() ) {
					Entry<String, Object> tupleEntry = tupleItr.next();

					log.info( "tupleEntry: " + tupleEntry.getKey() + " " + tupleEntry.getValue() + " classEntry: "
							+ embeddableObject.getCls( tupleEntry.getKey() ) );
					map.put( tupleEntry.getKey(),
							fromJSON( (String) tupleEntry.getValue(), embeddableObject.getCls( tupleEntry.getKey() ) ) );
				}
			}
		}
	}

	/**
	 * Converts the value for the column as JSON format.
	 * 
	 * @param columnNames
	 *            All the columnNames in the entity object.
	 * @return Newly created Map storing JSON format when required.
	 * @throws ClassNotFoundException
	 */
	public Map<String, Object> convertJsonAsNeededOn(Set<String> columnNames, TupleSnapshot snapshot) {

		Map<String, Object> map = new HashMap<String, Object>();

		for ( String columnName : columnNames ) {
			if ( snapshot.get( columnName ) == null ) {
				map.put( columnName, null );
				rawTuple.putIfAbsent( columnName, "null" );
			}
			else {
				map.put( columnName, toJSON( snapshot.get( columnName ) ) );
				rawTuple.putIfAbsent( columnName, snapshot.get( columnName ) );
			}
		}
		return map;
	}

	/**
	 * Converts Json to object representation for the return value from the
	 * datastore when needed.
	 * 
	 * @param key
	 *            Used to retrieve the corresponding value.
	 * @param tuple
	 *            Contains key value pairs from the datastore.
	 * @return Retrieved key value pairs with JSON modification when needed.
	 */
	public Map<String, Object> convertFromJsonOn(EntityKey key, Map<String, Object> tuple, Field[] fields) {

		for ( Field field : fields ) {
			String columnName = key.getColumnName( field.getName() );
			if ( tuple.get( columnName ) != null ) {
				getObjectFromJsonOn( field, columnName, tuple );
			}
			else {
				tuple.put( columnName, null );
			}
		}

		return tuple;
	}
}

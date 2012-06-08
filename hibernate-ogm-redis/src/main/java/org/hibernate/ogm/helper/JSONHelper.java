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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.helper.annotation.AnnotationFinder;
import org.json.JSONException;
import org.json.JSONObject;

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

	private final AnnotationFinder finder = new AnnotationFinder();
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

	} ).registerTypeAdapter( UUID.class, new JsonSerializer<UUID>() {

		@Override
		public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive( src.toString() );
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
	 * 
	 * @param map
	 * @return
	 */
	public String toJSON(Map map){
		return new JSONObject(map).toString();
	}

	/**
	 * 
	 * @param json
	 * @param key
	 * @return
	 */
	public Object get(String json, String key) {
		try {
			return new JSONObject( json ).get( key );
		}
		catch ( JSONException ex ) {
			throw new RuntimeException( ex );
		}
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
	public void getObjectFromJsonOn(Class cls, String columnName, Map<String, Object> map) {

		try {
			map.put( columnName, fromJSON( (String) map.get( columnName ), cls ) );
		}
		catch ( JsonParseException ex ) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Converts the value for the column as JSON format.
	 * 
	 * @param entityRecord
	 * @return Newly created Map storing JSON format when required.
	 * @throws ClassNotFoundException
	 */
	public Map<String, Object> convertJsonAsNeededOn(Map<String, Object> entityRecord) {

		if ( entityRecord == null || entityRecord.isEmpty() ) {
			return Collections.EMPTY_MAP;
		}

		Map<String, Object> map = new HashMap<String, Object>();

		for ( Iterator<Entry<String, Object>> itr = entityRecord.entrySet().iterator(); itr.hasNext(); ) {
			Entry<String, Object> entry = itr.next();
			map.put( entry.getKey(), toJSON( entry.getValue() ) );
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

		Object obj = null;
		Map<String, Class> map = null;
		for ( Field field : fields ) {
			if ( Modifier.isTransient( field.getModifiers() ) ) {
				continue;
			}

			obj = tuple.get( field.getName() );
			if ( obj != null ) {
				putObjectFieldNameAsColumnName( field, obj, tuple );
			}
			else {
				putObjectFieldNameNotAsColumnName( field, map, tuple );
			}
		}

		return tuple;
	}
	
	/**
	 * Puts object from JSON whose key is the field name.
	 * 
	 * @param field
	 *            Current field in an iterator to be examined for its field type and annotations.
	 * @param obj
	 *            Object having the expected type.
	 * @param tuple
	 */
	private void putObjectFieldNameAsColumnName(Field field, Object obj, Map<String, Object> tuple) {
		// No @Column nor other annotations change the corresponding column names.
		if ( finder.isEntityAnnotated( field.getType() ) ) {
			putObject( field, obj.getClass(), tuple );
		}
		else {
			putObject( field, field.getType(), tuple );
		}
	}
	
	/**
	 * Gets Object from JSON and puts the Object to the parameter, tuple.
	 * 
	 * @param field
	 *            Current field in an iterator to be examined for its field type and annotations.
	 * @param cls
	 *            Expected class to be set as the field type.
	 * @param tuple
	 */
	private void putObject(Field field, Class cls, Map<String, Object> tuple) {
		if ( isReturnAsString( field.getType() ) ) {
			getObjectFromJsonOn( String.class, field.getName(), tuple );
		}
		else {
			getObjectFromJsonOn( cls, field.getName(), tuple );
		}
	}

	/**
	 * 
	 * @param field
	 * @param tuple
	 */
	private void putObjectFieldNameNotAsColumnName(Field field, Map<String, Class> map, Map<String, Object> tuple) {

		if ( finder.isEntityAnnotated( field.getType() ) ) {
			putObjectWithAssociation( field, map, tuple );
		}
		else if ( finder.isEmbeddableAnnotated( field.getType() ) ) {
			putObjectWithEmbeddable( field, tuple );
		}
		else {
			putObjectWithColumn( field, tuple );
		}
	}

	/**
	 * 
	 * @param field
	 * @param tuple
	 */
	private void putObjectWithEmbeddable(Field field, Map<String, Object> tuple) {
		//this field has @Embeddable and also has field and column mapping field
		putObjectWithInverseSide( field, tuple );
		putObjectWith( field, finder.findAllColumnNamesFrom( field.getType(), "", true ), tuple );
		putObjectWith( field, finder.findAllJoinColumnNamesFrom( field.getType(), "", true ), tuple );
	}

	/**
	 * 
	 * @param field
	 * @param tuple
	 */
	private void putObjectWithInverseSide(Field field, Map<String, Object> tuple) {
		for ( Field f : field.getType().getDeclaredFields() ) {
			if ( tuple.get( field.getName() + "." + f.getName() ) != null ) {
				getObjectFromJsonOn( String.class, field.getName() + "." + f.getName(), tuple );
			}
		}
	}	

	/**
	 * 
	 * @param field
	 * @param map
	 * @param tuple
	 */
	private void putObjectWith(Field field, Map<String, Class> map, Map<String, Object> tuple) {
		if ( map != null && !map.isEmpty() ) {
			for ( Iterator<Entry<String, Class>> itr = map.entrySet().iterator(); itr.hasNext(); ) {
				Entry<String, Class> entry = itr.next();
				getObjectFromJsonOn( entry.getValue(), entry.getKey(), tuple );
			}
		}
	}

	/**
	 * Gets Object from JSON whose keys representing some kind of associations and puts it to the parameter, tuple.
	 * 
	 * @param field
	 *            Current field in an iterator to be examined for its field type and annotations.
	 * @param map
	 *            Stores field-column mapping.
	 * @param tuple
	 */
	private void putObjectWithAssociation(Field field, Map<String, Class> map, Map<String, Object> tuple) {
		// this field has some kind of association, field:
		map = finder.findAllJoinColumnNamesFrom( field.getType(), "", true );
		if ( map.isEmpty() ) {
			map = createKeys( field.getName(), finder.findAllIdsFrom( field.getType(), "", true ), "_" );
		}

		putObjectAs( String.class, map, tuple );
	}

	/**
	 * 
	 * @param field
	 * @param map
	 * @param tuple
	 */
	private void putObjectWithColumn(Field field, Map<String, Object> tuple) {
		// this field has some kind of field and column mapping

		for ( Iterator<Entry<String, Class>> itr = finder
				.findAllColumnNamesFrom( field.getDeclaringClass(), field.getName(), true ).entrySet().iterator(); itr
				.hasNext(); ) {

			String columnName = itr.next().getKey();
			if ( isReturnAsString( field.getType() ) ) {
				getObjectFromJsonOn( String.class, columnName, tuple );
			}
			else {
				if ( tuple.get( columnName ) != null ) {
					getObjectFromJsonOn( field.getType(), columnName, tuple );
				}
				else {
					putObjectAs( null, finder.findAllIdsFrom( field.getType(), "", true ), tuple );
				}
			}
		}
	}
	
	/**
	 * Gets Object with the parameter,cls, Class from JSON and puts it to the parameter, tuple.
	 * 
	 * @param cls
	 *            Expected class type used to deserialize JSON. if null then, uses the values, Class from the parameter,
	 *            map.
	 * @param map
	 *            Stores field-column mapping.
	 * @param tuple
	 */
	private void putObjectAs(Class cls, Map<String, Class> map, Map<String, Object> tuple) {
		if ( !map.isEmpty() ) {
			if ( cls != null ) {
				for ( Iterator<Entry<String, Class>> itr = map.entrySet().iterator(); itr.hasNext(); ) {
					Entry<String, Class> entry = itr.next();
					if ( tuple.get( entry.getKey() ) != null ) {
						getObjectFromJsonOn( cls, entry.getKey(), tuple );
					}
				}
			}
			else if ( cls == null ) {
				for ( Iterator<Entry<String, Class>> itr = map.entrySet().iterator(); itr.hasNext(); ) {
					Entry<String, Class> entry = itr.next();
					if ( tuple.get( entry.getKey() ) != null ) {
						getObjectFromJsonOn( entry.getValue(), entry.getKey(), tuple );
					}
				}
			}
		}
	}
	
	private Map<String,Class> createKeys(String fieldName, Map<String,Class> map,String separator){
		
		Map<String,Class> keyMap = new HashMap<String,Class>();
		for(Iterator<Entry<String,Class>> itr = map.entrySet().iterator();itr.hasNext();){
			Entry<String,Class> entry = itr.next();
			keyMap.put( fieldName + separator + entry.getKey(), entry.getValue() );
		}
		return keyMap;
	}
	
	/**
	 * Checks if the type of the parameter, cls is stored as String or not based on types in org.hibernate.ogm.type.
	 * @param cls Examined if it's stored as String.
	 * @return True if it's stored as String, otherwise false.
	 */
	private boolean isReturnAsString(Class cls){
		
		if ( cls.getCanonicalName().equals( "java.util.UUID" )
				|| cls.getCanonicalName().equals( "java.math.BigDecimal" )
				|| cls.getCanonicalName().equals( "java.net.URL" )
				|| cls.getCanonicalName().equals( "java.math.BigInteger" ) ) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Creates association based on the parameter, associationAsJson.
	 * @param associationAsJson JSON representation of association.
	 * @return Map representation of association.
	 */
	public Map<String, Object> createAssociation(String associationAsJson) {

		Map<String, Object> val = new HashMap<String, Object>();
		try {
			JSONObject json = new JSONObject( associationAsJson );
			for ( Iterator itr = json.keys(); itr.hasNext(); ) {
				String key = (String) itr.next();
				val.put( key, json.get( key ) );
			}
		}
		catch ( JSONException e ) {
			throw new RuntimeException( e );
		}

		return val;
	}
}

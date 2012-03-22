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
package org.hibernate.ogm.datastore.voldemort.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.JSONedClassDetector;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.VoldemortDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.StoreClientFactory;
import voldemort.client.UpdateAction;
import voldemort.versioning.Versioned;

import com.google.gson.Gson;

/**
 * 
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 * 
 */
public class VoldemortDatastoreProvider implements DatastoreProvider,
		Startable, Stoppable {

	private static final Log log = LoggerFactory.make();
	private StoreClientFactory clientFactory;
	private final ObjectMapper mapper = new ObjectMapper();
	private final ConcurrentMap<String, Set<Serializable>> tableIds = new ConcurrentHashMap<String, Set<Serializable>>();
	private Map<String, String> requiredProperties;
	private final Gson gson = new Gson();
	private final Set<AssociationKey> associationKeys = Collections
			.synchronizedSet(new HashSet<AssociationKey>());
	private final WrapperClassDetector classDetector = new WrapperClassDetector();
	private final JSONedClassDetector jsonedDetector = new JSONedClassDetector();
	private StoreClient dataClient;
	private StoreClient associationClient;
	private StoreClient sequenceClient;
	public static final String SEQUENCE_LABEL = "nextSequence";
	private final ConcurrentMap<RowKey, List<Integer>> nextValues = new ConcurrentHashMap<RowKey, List<Integer>>();
	private boolean flushToDb = false;
	private int maxTries;
	private VoldemortUpdateAction updateAction;

	private static enum VoldemortProp {

		ASSOCIATION("HibernateOGM-Association", "voldemort_association_store",
				"hibernate.ogm.datastore.voldemort_association_store"), DATA(
				"HibernateOGM", "voldemort_store",
				"hibernate.ogm.datastore.voldemort_store"), SEQUENCE(
				"HibernateOGM-Sequence", "voldemort_sequence_store",
				"hibernate.ogm.datastore.voldemort_sequence_store"), FLUSH_SEQUENCE_TO_DB(
				"FlushSequence", "FlushSequence",
				"hibernate.ogm.datastore.voldemort_flush_sequence_to_db"), MAX_TRIES(
				"MaxTries", "MaxTries",
				"hibernate.ogm.datastore.voldemort_max_tries"), UPDATE_ACTION(
				"UpdateAction", "UpdateAction",
				"hibernate.ogm.datastore.voldemort_update_action");

		private String name;
		private String alias;
		private String propPath;

		VoldemortProp(String name, String alias, String propPath) {
			this.name = name;
			this.alias = alias;
			this.propPath = propPath;
		}

		public String getName() {
			return this.name;
		}

		public String getAlias() {
			return this.alias;
		}

		public String getPropPath() {
			return this.propPath;
		}

		public String toString() {
			return this.name + " " + this.alias + " " + this.propPath;
		}
	}

	private static enum RequiredProp {

		PROVIDER("provider", "hibernate.ogm.datastore.provider"), DIALECT(
				"dialect", "hibernate.dialect"), PROVIDER_URL("provider_url",
				"hibernate.ogm.datastore.provider_url");

		private String name;
		private String propPath;

		RequiredProp(String name, String propPath) {
			this.name = name;
			this.propPath = propPath;
		}

		public String getName() {
			return this.name;
		}

		public String getPropPath() {
			return this.propPath;
		}
	}

	private static enum Setter {
		SET_KEY("setKey"), SET_VALUE("setValue");

		private String name;

		Setter(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.ogm.datastore.spi.DatastoreProvider#getDefaultDialect()
	 */
	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return VoldemortDialect.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.service.spi.Stoppable#stop()
	 */
	@Override
	public void stop() {
		log.info("stopping Voldemort");
		this.tableIds.clear();
		this.clientFactory.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.service.spi.Startable#start()
	 */
	@Override
	public void start() {
		if (!this.checkRequiredSettings()) {
			throw new HibernateException(
					"Please configure Voldemort on hibernate.properties correctly.");
		}

		this.setClientFactory();
		this.setVoldemortClients();
		this.setFlushToDBFlag();
		this.setMaxTries();
		this.setUpdateAction();
	}

	/**
	 * Sets clientFactory property.
	 */
	private void setClientFactory() {
		this.clientFactory = new SocketStoreClientFactory(
				new ClientConfig().setBootstrapUrls(this.requiredProperties
						.get("provider_url")));
	}

	/**
	 * Sets three voldemort clients for each store,association store, data store
	 * and sequence store.
	 */
	private void setVoldemortClients() {
		this.dataClient = this.clientFactory.getStoreClient(this
				.getVoldemortStoreName());
		this.sequenceClient = this.clientFactory.getStoreClient(this
				.getVoldemortSequenceStoreName());
		this.associationClient = this.clientFactory.getStoreClient(this
				.getVoldemortAssociationStoreName());
	}

	/**
	 * Sets flushToDb flag based on the value from hibernate.properties. The
	 * default value is true.
	 */
	private void setFlushToDBFlag() {
		String flushToDbProp = this.requiredProperties
				.get(VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias());
		log.info("flushToDbProp: "
				+ this.requiredProperties
						.get(VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias()));
		this.flushToDb = flushToDbProp == null || flushToDbProp.equals("")
				|| flushToDbProp.equals("true") ? true : false;

		log.info("set flush sequence to db flag as " + this.flushToDb);
	}

	/**
	 * Gets the Voldemort specific setting values. Currently there is only one
	 * settings for the store name.
	 * 
	 * @return Key-value pair for the Voldemort specific setting.
	 */
	private Map<String, String> getSpecificSettings() {

		Map<String, String> specificSettings = new HashMap<String, String>();
		specificSettings.put(VoldemortProp.DATA.getAlias(), Environment
				.getProperties().getProperty(VoldemortProp.DATA.getPropPath()));
		specificSettings.put(
				VoldemortProp.ASSOCIATION.getAlias(),
				Environment.getProperties().getProperty(
						VoldemortProp.ASSOCIATION.getPropPath()));
		specificSettings.put(
				VoldemortProp.SEQUENCE.getAlias(),
				Environment.getProperties().getProperty(
						VoldemortProp.SEQUENCE.getPropPath()));
		specificSettings.put(
				VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias(),
				Environment.getProperties().getProperty(
						VoldemortProp.FLUSH_SEQUENCE_TO_DB.getPropPath()));
		specificSettings.put(
				VoldemortProp.MAX_TRIES.getAlias(),
				Environment.getProperties().getProperty(
						VoldemortProp.MAX_TRIES.getPropPath()));
		specificSettings.put(
				VoldemortProp.UPDATE_ACTION.getAlias(),
				Environment.getProperties().getProperty(
						VoldemortProp.UPDATE_ACTION.getPropPath()));
		return Collections.unmodifiableMap(specificSettings);
	}

	/**
	 * Gets the common required property values among other datastore provider
	 * and the datastore specific property values.
	 * 
	 * @return Key value pairs storing the properties.
	 */
	private Map<String, String> getRequiredPropertyValues() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(RequiredProp.PROVIDER.getName(), Environment.getProperties()
				.getProperty(RequiredProp.PROVIDER.getPropPath()));
		map.put(RequiredProp.DIALECT.getName(),
				RequiredProp.DIALECT.getPropPath());
		map.put(RequiredProp.PROVIDER_URL.getName(),
				Environment.getProperties().getProperty(
						RequiredProp.PROVIDER_URL.getPropPath()));
		map.putAll(this.getSpecificSettings());
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Checks required property settings for Voldemort on hibernate.properties.
	 * 
	 * @return True if all the required properties are set, false otherwise.
	 */
	private boolean checkRequiredSettings() {
		this.requiredProperties = this.getRequiredPropertyValues();

		if (this.requiredProperties.get(RequiredProp.PROVIDER.getName())
				.equals(this.getClass().getCanonicalName())
				&& this.requiredProperties.get(RequiredProp.PROVIDER_URL
						.getName()) != null
				&& this.requiredProperties.get(RequiredProp.DIALECT.getName()) != null) {
			return true;
		}
		return false;
	}

	/**
	 * Sets max try for put. This is the way to achieve optimistic lock in
	 * Voldemort. See
	 * http://project-voldemort.com/javadoc/all/index.html?overview-summary.html
	 */
	private void setMaxTries() {
		String mTries = this.requiredProperties.get(VoldemortProp.MAX_TRIES
				.getAlias());
		log.info("max tries: " + mTries);
		this.maxTries = mTries == null || mTries.equals("")
				|| Integer.parseInt(mTries) <= 0 ? 3 : Integer.parseInt(mTries);

		log.info("set max tries as " + this.maxTries);
	}

	/**
	 * Sets updateAction property. This is the object that achieves optimistic
	 * lock with max tries property in Voldemort. See
	 * http://project-voldemort.com/javadoc/all/index.html?overview-summary.html
	 */
	@SuppressWarnings("rawtypes")
	private void setUpdateAction() {
		String uAction = this.requiredProperties
				.get(VoldemortProp.UPDATE_ACTION.getAlias());
		log.info("update action: " + uAction);

		if (uAction != null && !uAction.equals("")) {
			try {
				this.updateAction = (VoldemortUpdateAction) Class.forName(
						uAction).newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
				this.throwHibernateExceptionFrom(e);
			} catch (IllegalAccessException e) {
				this.throwHibernateExceptionFrom(e);
			} catch (ClassNotFoundException e) {
				this.throwHibernateExceptionFrom(e);
			}
		}

		log.info("set updateAction as " + this.updateAction);
	}

	@SuppressWarnings("rawtypes")
	public void setUpdateAction(VoldemortUpdateAction updateAction) {
		this.updateAction = updateAction;
	}

	/**
	 * Sets flushToDb flag.
	 * 
	 * @param flushToDb
	 */
	public void setFlushToDb(boolean flushToDb) {
		this.flushToDb = flushToDb;
	}

	/**
	 * Gets a value as Map from Voldemort using the specified key.
	 * 
	 * @param key
	 *            Used to retrieve the corresponding value.
	 * @return The Corresponding value to the key.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Object> getEntityTuple(EntityKey key) {
		Versioned v = this.getValue(this.getVoldemortStoreName(),
				key.getEntityKeyAsMap(), false);

		if (v == null) {
			return null;
		}

		return this
				.applyJson(key, (Map<String, Object>) this
						.createReturnObjectFrom(v, Map.class));
	}

	/**
	 * Gets value with the specified store and key from Voldemort.
	 * 
	 * @param storeName
	 *            Store name to get the value.
	 * @param key
	 *            Used to get the corresponding value.
	 * @param keyByteArray
	 *            True if the key must be converted to byte array, false
	 *            otherwise.
	 * @return Corresponding value.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Versioned getValue(String storeName, Object key, boolean keyByteArray) {

		Versioned v = null;
		if (keyByteArray) {
			v = this.getValueWithByteKey(key, storeName);
		} else {

			if (storeName.equals(this.getVoldemortStoreName())) {
				v = this.dataClient.get(key);
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				v = this.sequenceClient.get(key);
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				v = this.associationClient.get(key);
			}
		}

		return v;
	}

	/**
	 * Gets value with the specified key converted to byte array.
	 * 
	 * @param key
	 *            Used to retrieve the value.
	 * @param storeName
	 *            Store name to look for the key.
	 * @return Corresponding value.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Versioned getValueWithByteKey(Object key, String storeName) {

		Versioned v = null;
		try {
			if (storeName.equals(this.getVoldemortStoreName())) {
				return this.dataClient.get(this.mapper.writeValueAsBytes(key));
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				return this.sequenceClient.get(this.mapper
						.writeValueAsBytes(key));
			}

			v = this.associationClient.get(this.mapper.writeValueAsBytes(key));

		} catch (JsonGenerationException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return v;
	}

	/**
	 * Creates entity tuple based on the returned value from Voldemort.
	 * 
	 * @param v
	 *            Returned value from Voldemort.
	 * @param cls
	 *            Represents the returned value class.
	 * @return Returned value or null.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object createReturnObjectFrom(Versioned v, Class cls) {

		Object rtnValue = null;
		try {
			rtnValue = this.mapper.readValue((byte[]) v.getValue(), 0,
					((byte[]) v.getValue()).length, cls);
		} catch (JsonParseException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return rtnValue;
	}

	/**
	 * Applies JSON conversion for the return value from the datastore when
	 * needed.
	 * 
	 * @param key
	 *            Used to retrieve the corresponding value.
	 * @param tuple
	 *            Contains key value pairs from the datastore.
	 * @return Retrieved key value pairs with JSON modification when needed.
	 */
	private Map<String, Object> applyJson(EntityKey key,
			Map<String, Object> tuple) {

		for (Field field : this.getDeclaredFieldsFrom(key.getEntityName())) {
			String columnName = key.getColumnName(field.getName());

			if (tuple.get(columnName) != null) {
				this.putJSONedValueTo(field, columnName, tuple);
			} else {
				tuple.put(columnName, null);
			}
		}

		return tuple;
	}

	/**
	 * Gets the declared fields from the specified class.
	 * 
	 * @param className
	 *            Class name used to get the declared fields.
	 * @return Field array storing the declared fields.
	 */
	private Field[] getDeclaredFieldsFrom(String className) {

		Field[] fields = null;
		try {
			fields = Class.forName(className).getDeclaredFields();
		} catch (SecurityException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (ClassNotFoundException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return fields;
	}

	/**
	 * Puts the JSONed value when the specified field type is one of JSONed type
	 * as the specified columnName on the specified Map.
	 * 
	 * @param field
	 *            Corresponding field to the columnName.
	 * @param columnName
	 *            Column name used on the datastore.
	 * @param map
	 *            Stores entity objects.
	 */
	private void putJSONedValueTo(Field field, String columnName,
			Map<String, Object> map) {

		if (field.getType().isArray()) {
			map.put(columnName, this.fromJSON((String) map.get(columnName),
					field.getType()));
		} else if (this.classDetector.isWrapperClass(field.getType())) {
			map.put(columnName, this.classDetector.castWrapperClassFrom(
					map.get(columnName), field.getType()));
		} else if (this.jsonedDetector.isAssignable(field.getType())) {
			map.put(columnName, this.fromJSON((String) map.get(columnName),
					field.getType()));
		}
	}

	/**
	 * Puts key-value pair on Voldemort.
	 * 
	 * @param key
	 *            Key.
	 * @param tuple
	 *            Value for the key.
	 * @return True if put is successful, false otherwise.
	 */
	public boolean putEntity(EntityKey key, Map<String, Object> tuple) {

		this.addEntryToIdTable(key);
		return this.writeEntityTupleFrom(key, tuple);
	}

	/**
	 * Writes entity tuple to Voldemort based on the specified key and tuple.
	 * 
	 * @param key
	 *            Key to be stored.
	 * @param tuple
	 *            Value to be stored for the corresponding key.
	 * @return True if put is successful, false otherwise.
	 */
	private boolean writeEntityTupleFrom(EntityKey key,
			Map<String, Object> tuple) {
		return this.putValue(this.getVoldemortStoreName(),
				key.getEntityKeyAsMap(), tuple, false, true);
	}

	/**
	 * Puts specified key-value pair on Voldemort.
	 * 
	 * @param storeName
	 *            Store to be used to store the pair.
	 * @param key
	 *            Key to be stored.
	 * @param value
	 *            Value to be stored.
	 * @param keyByteArray
	 *            True if the key must be converted to byte array, false
	 *            otherwise.
	 * @param valueByteArray
	 *            True if the value must be converted to byte array, false
	 *            otherwise.
	 * @return True if put is successful, false otherwise.
	 */
	public boolean putValue(String storeName, Object key, Object value,
			boolean keyByteArray, boolean valueByteArray) {

		boolean b = false;

		if (keyByteArray && valueByteArray) {
			b = this.putByteArrayKeyAndByteArrayValue(key, value, storeName);
		} else if (keyByteArray && !valueByteArray) {
			b = this.putByteArrayKeyAndValue(key, value, storeName);
		} else if (!keyByteArray && valueByteArray) {
			b = this.putKeyAndByteArrayValue(key, value, storeName);
		} else if (!keyByteArray && !valueByteArray) {
			if (storeName.equals(this.getVoldemortStoreName())) {
				b = this.putWithApplyUpdate(dataClient, key, value);
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				b = this.putWithApplyUpdate(this.sequenceClient, key, value);
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				b = this.putWithApplyUpdate(this.associationClient, key, value);
			}
		}

		return b;
	}

	/**
	 * Puts byte array key and byte array value.
	 * 
	 * @param key
	 *            Key converted to byte array.
	 * @param value
	 *            Value converted to byte array.
	 * @param storeName
	 *            Store to be used to store the pair.
	 * @return True if put is successful, false otherwise.
	 */
	private boolean putByteArrayKeyAndByteArrayValue(Object key, Object value,
			String storeName) {

		boolean b = false;
		try {
			if (storeName.equals(this.getVoldemortStoreName())) {
				b = this.putWithApplyUpdate(this.dataClient, key, value);
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				b = this.putWithApplyUpdate(this.sequenceClient,
						this.mapper.writeValueAsBytes(key),
						this.mapper.writeValueAsBytes(value));
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				b = this.putWithApplyUpdate(this.associationClient,
						this.mapper.writeValueAsBytes(key),
						this.mapper.writeValueAsBytes(value));
			}

		} catch (JsonGenerationException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return b;
	}

	/**
	 * Puts byte array key and array value.
	 * 
	 * @param key
	 *            Key converted to byte array.
	 * @param value
	 *            Value.
	 * @param storeName
	 *            Store to be used to store the pair.
	 * @return True if put is successful, false otherwise.
	 */
	private boolean putByteArrayKeyAndValue(Object key, Object value,
			String storeName) {

		boolean b = false;
		try {
			if (storeName.equals(this.getVoldemortStoreName())) {
				b = this.putWithApplyUpdate(this.dataClient,
						this.mapper.writeValueAsBytes(key), value);
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				b = this.putWithApplyUpdate(this.sequenceClient,
						this.mapper.writeValueAsBytes(key), value);
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				b = this.putWithApplyUpdate(this.associationClient,
						this.mapper.writeValueAsBytes(key), value);
			}
		} catch (JsonGenerationException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return b;
	}

	/**
	 * Puts key and byte array value.
	 * 
	 * @param key
	 *            Key.
	 * @param value
	 *            Value converted to byte array.
	 * @param storeName
	 *            Store to be used to store the pair.
	 * @return True if put is successful, false otherwise.
	 */
	private boolean putKeyAndByteArrayValue(Object key, Object value,
			String storeName) {

		boolean b = false;
		try {
			if (storeName.equals(this.getVoldemortStoreName())) {
				b = this.putWithApplyUpdate(this.dataClient, key,
						this.mapper.writeValueAsBytes(value));
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				b = this.putWithApplyUpdate(this.sequenceClient, key,
						this.mapper.writeValueAsBytes(value));
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				b = this.putWithApplyUpdate(this.associationClient, key,
						this.mapper.writeValueAsBytes(value));
			}
		} catch (JsonGenerationException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return b;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean putWithApplyUpdate(StoreClient client, final Object key,
			final Object value) {

		if (this.updateAction == null) {
			return client.applyUpdate(new UpdateAction() {
				@Override
				public void update(StoreClient storeClient) {
					storeClient.put(key, value);
				}
			}, this.maxTries);
		}

		// return client.applyUpdate( this.updateAction, this.maxTries );
		return this.callApplyUpdateWith(client, key, value);
	}

	@SuppressWarnings("rawtypes")
	private boolean callApplyUpdateWith(StoreClient client, Object key,
			Object value) {

		try {
			VoldemortUpdateAction.class.getDeclaredMethod(
					Setter.SET_KEY.getName(), Object.class).invoke(
					this.updateAction, key);
			VoldemortUpdateAction.class.getDeclaredMethod(
					Setter.SET_VALUE.getName(), Object.class).invoke(
					this.updateAction, value);
		} catch (IllegalArgumentException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (SecurityException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IllegalAccessException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (InvocationTargetException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (NoSuchMethodException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return client.applyUpdate(this.updateAction, this.maxTries);
	}

	/**
	 * Stores the specified key to this object.
	 * 
	 * @param key
	 *            Entity key to be stored.
	 */
	private void addEntryToIdTable(EntityKey key) {

		Serializable rtnId = null;
		this.tableIds.get(key.getTableName());

		if (this.tableIds.get(key.getTableName()) == null) {
			Set<Serializable> set = new HashSet<Serializable>();
			set.add(key.getId());
			rtnId = (Serializable) this.tableIds.put(key.getTableName(), set);
		} else {
			this.tableIds.get(key.getTableName()).add(key.getId());
		}

		// this.showAllTableIds();
	}

	/**
	 * Shows all the table name and id pairs currently stored on this object.
	 * This method for debugging purpose.
	 */
	private void showAllTableIds() {
		StringBuilder stringBuilder = new StringBuilder();
		Set<Entry<String, Set<Serializable>>> entries = this.tableIds
				.entrySet();
		boolean found = false;
		for (Iterator<Entry<String, Set<Serializable>>> itr = entries
				.iterator(); itr.hasNext();) {
			Entry<String, Set<Serializable>> entry = itr.next();
			this.generateAllTableIdsMessage(entry, stringBuilder);
			found = true;
		}

		if (found) {
			log.info(stringBuilder);
		} else {
			log.info("currently there are no ids stored");
		}
	}

	/**
	 * Generates a message for showing all the table name and id pairs. This
	 * method is also for debugging purpose.
	 * 
	 * @param entry
	 *            Stores table name and the corresponding ids.
	 * @param stringBuilder
	 *            Used to build the message.
	 */
	private void generateAllTableIdsMessage(
			Entry<String, Set<Serializable>> entry, StringBuilder stringBuilder) {

		stringBuilder.append("table name: " + entry.getKey() + "\n");
		if (entry.getValue().isEmpty()) {
			stringBuilder.append("\tall the ids on table, " + entry.getKey()
					+ " are already deleted.\n");
		} else {
			for (Iterator<Serializable> itr = entry.getValue().iterator(); itr
					.hasNext();) {
				stringBuilder.append("\tid: " + itr.next() + "\n");
			}
		}
	}

	/**
	 * Removes entry with the specified key from Voldemort.
	 * 
	 * @param key
	 *            Used to remove the entry.
	 */
	public void removeEntityTuple(EntityKey key) {
		this.removeEntryFromIdTable(key);
		this.deleteValue(this.getVoldemortStoreName(), key.getEntityKeyAsMap(),
				false);
	}

	/**
	 * Deletes value from Voldemort.
	 * 
	 * @param storeName
	 *            Store where the value is deleted.
	 * @param key
	 *            Used to delete the value.
	 * @param keyByteArray
	 *            True if the key must be converted to byte array, false
	 *            otherwise.
	 * @return True if delete is successful, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	public boolean deleteValue(String storeName, Object key,
			boolean keyByteArray) {

		boolean b = false;

		if (keyByteArray) {
			b = this.deleteWithByteArrayKey(key, storeName);
		} else {
			if (storeName.equals(this.getVoldemortStoreName())) {
				b = this.dataClient.delete(key);
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				b = this.sequenceClient.delete(key);
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				b = this.associationClient.delete(key);
			}
		}

		return b;
	}

	/**
	 * Deletes with byte array key.
	 * 
	 * @param key
	 *            Converted to byte array.
	 * @param storeName
	 *            Store where the value is deleted.
	 * @return True if delete is successful, false otherwise.
	 */
	@SuppressWarnings("unchecked")
	private boolean deleteWithByteArrayKey(Object key, String storeName) {

		boolean b = false;
		try {
			if (storeName.equals(this.getVoldemortStoreName())) {
				b = this.dataClient.delete(this.mapper.writeValueAsBytes(key));
			} else if (storeName.equals(this.getVoldemortSequenceStoreName())) {
				b = this.sequenceClient.delete(this.mapper
						.writeValueAsBytes(key));
			} else if (storeName
					.equals(this.getVoldemortAssociationStoreName())) {
				b = this.associationClient.delete(this.mapper
						.writeValueAsBytes(key));
			}
		} catch (JsonGenerationException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			this.throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			this.throwHibernateExceptionFrom(e);
		}

		return b;
	}

	/**
	 * Removes the specified key from this object.
	 * 
	 * @param key
	 *            Entity key to be removed.
	 */
	private void removeEntryFromIdTable(EntityKey key) {
		this.tableIds.get(key.getTableName()).remove(key.getId());
	}

	@SuppressWarnings("rawtypes")
	public Map<RowKey, Map<String, Object>> getAssociation(AssociationKey key) {

		Versioned v = this.getAssociationFrom(key);

		if (v == null) {
			return null;
		}

		return this.createAssociationFrom((String) this.createReturnObjectFrom(
				v, String.class));
	}

	/**
	 * Creates association from the specified Jsoned string.
	 * 
	 * @param jsonedAssociation
	 *            Representation of association as JSON.
	 * @return Association based on the specified string.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<RowKey, Map<String, Object>> createAssociationFrom(
			String jsonedAssociation) {

		Map associationMap = (Map) this.fromJSON(jsonedAssociation, Map.class);

		Map<RowKey, Map<String, Object>> association = new HashMap<RowKey, Map<String, Object>>();
		for (Iterator itr = associationMap.keySet().iterator(); itr.hasNext();) {
			String key = (String) itr.next();
			RowKey rowKey = (RowKey) this.fromJSON(key, RowKey.class);
			Map<String, Object> val = (Map<String, Object>) this.fromJSON(
					(String) associationMap.get(key), Map.class);
			association.put(rowKey, val);
		}

		return association;
	}

	/**
	 * Gets association based on the specified association key.
	 * 
	 * @param key
	 *            Used to retrieve the association.
	 * @return Object represents the association or null if there is no
	 *         association around the key.
	 */
	@SuppressWarnings("rawtypes")
	private Versioned getAssociationFrom(AssociationKey key) {

		return this.getValue(this.getVoldemortAssociationStoreName(),
				this.toJSON(key.getAssociationKeyAsMap()), true);
	}

	/**
	 * Puts association on Voldemort.
	 * 
	 * @param key
	 *            Key for the association.
	 * @param associationMap
	 *            Map representing the association.
	 */
	public void putAssociation(AssociationKey key,
			Map<RowKey, Map<String, Object>> associationMap) {

		this.associationKeys.add(key);
		this.putValue(this.getVoldemortAssociationStoreName(),
				this.toJSON(key.getAssociationKeyAsMap()),
				this.toJSON(this.convertKeyAndValueToJsonOn(associationMap)),
				true, true);
	}

	/**
	 * Converts both key and value to Json.
	 * 
	 * @param map
	 *            Map to be converted to Jsoned key and value pairs.
	 * @return Jsoned map.
	 */
	@SuppressWarnings("rawtypes")
	private Map<String, String> convertKeyAndValueToJsonOn(Map map) {
		Map<String, String> jsonedMap = new HashMap<String, String>();
		for (Iterator itr = map.keySet().iterator(); itr.hasNext();) {
			Object k = itr.next();
			jsonedMap.put(this.toJSON(k), this.toJSON(map.get(k)));
		}

		return jsonedMap;
	}

	/**
	 * Removes association around the specified association key,
	 * 
	 * @param key
	 *            Used to remove the association.
	 */
	public void removeAssociation(AssociationKey key) {
		this.associationKeys.remove(key);
		this.deleteValue(this.getVoldemortAssociationStoreName(),
				this.toJSON(key.getAssociationKeyAsMap()), true);
	}

	public void setNextValue(RowKey key, IntegralDataTypeHolder value,
			int increment, int initialValue) {

		List<Integer> l = new LinkedList<Integer>();
		l.add(initialValue);
		if (this.nextValues.putIfAbsent(key, l) == null) {
			Map<String, Integer> nextSequence = new HashMap<String, Integer>();
			nextSequence.put(VoldemortDatastoreProvider.SEQUENCE_LABEL,
					initialValue);
			value.initialize(initialValue);

			if (this.flushToDb) {
				// this.taskQueue.offer(new PutSequenceRunnable(this, this
				// .getVoldemortSequenceStoreName(), this.toJSON(key
				// .getRowKeyAsMap()), nextSequence, true, false));

				this.putValue(this.getVoldemortSequenceStoreName(),
						this.toJSON(key.getRowKeyAsMap()), nextSequence, true,
						false);
			}
		} else {
			List<Integer> list = this.nextValues.get(key);
			synchronized (list) {
				boolean notContained = false;
				int candidate = list.get(list.size() - 1) + increment;
				if (!list.contains(candidate)) {
					this.nextValues.get(key).add(candidate);
					notContained = true;
				}
				value.initialize(candidate);
				Map<String, Integer> nextSequence = new HashMap<String, Integer>();
				nextSequence.put(VoldemortDatastoreProvider.SEQUENCE_LABEL,
						candidate);

				if (this.flushToDb) {
					if (notContained) {
						this.putValue(this.getVoldemortSequenceStoreName(),
								this.toJSON(key.getRowKeyAsMap()),
								nextSequence, true, false);
					}
				}
			}
		}
	}

	/**
	 * Meant to execute assertions in tests only
	 * 
	 * @return a read-only view of the map containing the relations between
	 *         entities
	 */
	public Map<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationsMap() {

		Map<AssociationKey, Map<RowKey, Map<String, Object>>> associations = new HashMap<AssociationKey, Map<RowKey, Map<String, Object>>>();
		synchronized (this.associationKeys) {
			for (AssociationKey associationKey : this.associationKeys) {
				associations.put(associationKey,
						this.getAssociation(associationKey));
			}
		}

		return associations;
	}

	/**
	 * Creates JSON representation based on the specified object.
	 * 
	 * @param obj
	 *            To be JSONed.
	 * @return JSON representation of the specified object.
	 */
	protected String toJSON(Object obj) {
		return this.gson.toJson(obj);
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
	protected Object fromJSON(String json, Class cls) {
		return this.gson.fromJson(json, cls);
	}

	/**
	 * Meant to execute assertions in tests only. Delete
	 * EntityKeyBuilder.DEBUG_OGM_PERSISTER when tests are done.
	 * 
	 * @return a read-only view of the map containing the entities
	 */
	public Map<EntityKey, Map<String, Object>> getEntityMap() {

		Map<EntityKey, Map<String, Object>> map = new HashMap<EntityKey, Map<String, Object>>();
		Set<Entry<String, Set<Serializable>>> entries = this.tableIds
				.entrySet();
		for (Iterator<Entry<String, Set<Serializable>>> itr = entries
				.iterator(); itr.hasNext();) {
			Entry<String, Set<Serializable>> entry = itr.next();
			for (Iterator<Serializable> itr2 = entry.getValue().iterator(); itr2
					.hasNext();) {
				Serializable id = itr2.next();
				EntityKey entityKey = new EntityKey(
						entry.getKey(),
						id,
						EntityKeyBuilder.DEBUG_OGM_PERSISTER.getEntityName(),
						EntityKeyBuilder
								.getColumnMap(EntityKeyBuilder.DEBUG_OGM_PERSISTER));
				map.put(entityKey, this.getEntityTuple(entityKey));
			}
		}

		return map;
	}

	/**
	 * Gets sequence store name for Voldemort.
	 * 
	 * @return Sequence store name specified on hibernate.properties.
	 */
	public String getVoldemortSequenceStoreName() {
		String sequenceStoreName = this.requiredProperties
				.get(VoldemortProp.SEQUENCE.getAlias());
		return sequenceStoreName == null || sequenceStoreName.equals("") ? VoldemortProp.SEQUENCE
				.getName() : sequenceStoreName;
	}

	/**
	 * Gets store name for Voldemort.
	 * 
	 * @return Store name specified on hibernate.properties.
	 */
	public String getVoldemortStoreName() {

		String storeName = this.requiredProperties.get(VoldemortProp.DATA
				.getAlias());
		return storeName == null || storeName.equals("") ? VoldemortProp.DATA
				.getName() : storeName;
	}

	/**
	 * Gets association store name for Voldemort.
	 * 
	 * @return Association store name specified on hibernate.properties.
	 */
	public String getVoldemortAssociationStoreName() {

		String associationStoreName = this.requiredProperties
				.get(VoldemortProp.ASSOCIATION.getAlias());
		return associationStoreName == null || associationStoreName.equals("") ? VoldemortProp.ASSOCIATION
				.getName() : associationStoreName;
	}

	/**
	 * Converts the specified exception to HibernateException and rethrows it.
	 * 
	 * @param <T>
	 * @param exception
	 *            Exception to be rethrown as HibernateException.
	 */
	private <T extends Throwable> void throwHibernateExceptionFrom(T exception) {
		throw new HibernateException(exception.getCause());
	}
}

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
import org.hibernate.ogm.datastore.spi.AbstractDatastoreProvider;
import org.hibernate.ogm.datastore.spi.JSONHelper;
import org.hibernate.ogm.datastore.spi.JSONedClassDetector;
import org.hibernate.ogm.datastore.spi.WrapperClassDetector;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.VoldemortDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

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
public class VoldemortDatastoreProvider extends AbstractDatastoreProvider {

	private static final Log log = LoggerFactory.make();
	private StoreClientFactory clientFactory;
	private final ObjectMapper mapper = new ObjectMapper();
	private final ConcurrentMap<String, Set<Serializable>> tableIds = new ConcurrentHashMap<String, Set<Serializable>>();
	private final Gson gson = new Gson();
	private final Set<AssociationKey> associationKeys = Collections
			.synchronizedSet(new HashSet<AssociationKey>());
	private StoreClient dataClient;
	private StoreClient associationClient;
	private StoreClient sequenceClient;
	public static final String SEQUENCE_LABEL = "nextSequence";
	private final ConcurrentMap<RowKey, List<Integer>> nextValues = new ConcurrentHashMap<RowKey, List<Integer>>();
	private boolean flushToDb = false;
	private int maxTries;
	private VoldemortUpdateAction updateAction;
	private final JSONHelper jsonHelper = new JSONHelper(
			new WrapperClassDetector(), new JSONedClassDetector());

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
			return name;
		}

		public String getAlias() {
			return alias;
		}

		public String getPropPath() {
			return propPath;
		}

		public String toString() {
			return name + " " + alias + " " + propPath;
		}
	}

	private static enum Setter {
		SET_KEY("setKey"), SET_VALUE("setValue");

		private String name;

		Setter(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
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
		tableIds.clear();

		if (clientFactory != null) {
			clientFactory.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.service.spi.Startable#start()
	 */
	@Override
	public void start() {
		try {
			if (!checkRequiredSettings()) {
				throw new HibernateException(
						"Please configure Voldemort on hibernate.properties correctly.");
			}

			setClientFactory();
			setVoldemortClients();
			setFlushToDBFlag();
			setMaxTries();
			setUpdateAction();
		} catch (Throwable ex) {
			stop();
		}
	}

	/**
	 * Sets clientFactory property.
	 */
	private void setClientFactory() {
		clientFactory = new SocketStoreClientFactory(
				new ClientConfig().setBootstrapUrls(this
						.getRequiredProperties().get("provider_url")));
	}

	/**
	 * Sets three voldemort clients for each store,association store, data store
	 * and sequence store.
	 */
	private void setVoldemortClients() {
		dataClient = clientFactory.getStoreClient(getVoldemortStoreName());
		sequenceClient = clientFactory
				.getStoreClient(getVoldemortSequenceStoreName());
		associationClient = clientFactory
				.getStoreClient(getVoldemortAssociationStoreName());
	}

	/**
	 * Sets flushToDb flag based on the value from hibernate.properties. The
	 * default value is true.
	 */
	private void setFlushToDBFlag() {
		String flushToDbProp = getRequiredProperties().get(
				VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias());
		log.info("flushToDbProp: "
				+ getRequiredProperties().get(
						VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias()));
		flushToDb = flushToDbProp == null || flushToDbProp.equals("")
				|| flushToDbProp.equals("true") ? true : false;

		log.info("set flush sequence to db flag as " + flushToDb);
	}

	/**
	 * Gets the Voldemort specific setting values. Currently there is only one
	 * settings for the store name.
	 * 
	 * @return Key-value pair for the Voldemort specific setting.
	 */
	protected Map<String, String> getSpecificSettings() {

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
	 * Sets max try for put. This is the way to achieve optimistic lock in
	 * Voldemort. See
	 * http://project-voldemort.com/javadoc/all/index.html?overview-summary.html
	 */
	private void setMaxTries() {
		String mTries = this.getRequiredProperties().get(
				VoldemortProp.MAX_TRIES.getAlias());
		log.info("max tries: " + mTries);
		maxTries = mTries == null || mTries.equals("")
				|| Integer.parseInt(mTries) <= 0 ? 3 : Integer.parseInt(mTries);

		log.info("set max tries as " + maxTries);
	}

	/**
	 * Sets updateAction property. This is the object that achieves optimistic
	 * lock with max tries property in Voldemort. See
	 * http://project-voldemort.com/javadoc/all/index.html?overview-summary.html
	 */
	@SuppressWarnings("rawtypes")
	private void setUpdateAction() {
		String uAction = getRequiredProperties().get(
				VoldemortProp.UPDATE_ACTION.getAlias());
		log.info("update action: " + uAction);

		if (uAction != null && !uAction.equals("")) {
			try {
				updateAction = (VoldemortUpdateAction) Class.forName(uAction)
						.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
				throwHibernateExceptionFrom(e);
			} catch (IllegalAccessException e) {
				throwHibernateExceptionFrom(e);
			} catch (ClassNotFoundException e) {
				throwHibernateExceptionFrom(e);
			}
		}

		log.info("set updateAction as " + updateAction);
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
		Versioned v = getValue(getVoldemortStoreName(),
				key.getEntityKeyAsMap(), false);

		if (v == null) {
			return null;
		}

		return convertFromJson(key,
				(Map<String, Object>) createReturnObjectFrom(v, Map.class));
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
			v = getValueWithByteKey(key, storeName);
		} else {

			if (storeName.equals(getVoldemortStoreName())) {
				v = dataClient.get(key);
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				v = sequenceClient.get(key);
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				v = associationClient.get(key);
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
			if (storeName.equals(getVoldemortStoreName())) {
				return dataClient.get(mapper.writeValueAsBytes(key));
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				return sequenceClient.get(mapper.writeValueAsBytes(key));
			}

			v = associationClient.get(mapper.writeValueAsBytes(key));

		} catch (JsonGenerationException e) {
			throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			throwHibernateExceptionFrom(e);
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
			rtnValue = mapper.readValue((byte[]) v.getValue(), 0,
					((byte[]) v.getValue()).length, cls);
		} catch (JsonParseException e) {
			throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			throwHibernateExceptionFrom(e);
		}

		return rtnValue;
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
	private Map<String, Object> convertFromJson(EntityKey key,
			Map<String, Object> tuple) {

		for (Field field : getDeclaredFieldsFrom(key.getEntityName())) {
			String columnName = key.getColumnName(field.getName());

			if (tuple.get(columnName) != null) {
				jsonHelper.getObjectFromJsonOn(field, columnName, tuple);
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
			throwHibernateExceptionFrom(e);
		} catch (ClassNotFoundException e) {
			throwHibernateExceptionFrom(e);
		}

		return fields;
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

		addEntryToIdTable(key);
		return writeEntityTupleFrom(key, tuple);
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
		return putValue(getVoldemortStoreName(), key.getEntityKeyAsMap(),
				tuple, false, true);
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
			b = putByteArrayKeyAndByteArrayValue(key, value, storeName);
		} else if (keyByteArray && !valueByteArray) {
			b = putByteArrayKeyAndValue(key, value, storeName);
		} else if (!keyByteArray && valueByteArray) {
			b = putKeyAndByteArrayValue(key, value, storeName);
		} else if (!keyByteArray && !valueByteArray) {
			if (storeName.equals(getVoldemortStoreName())) {
				b = putWithApplyUpdate(dataClient, key, value);
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				b = putWithApplyUpdate(sequenceClient, key, value);
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				b = putWithApplyUpdate(associationClient, key, value);
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
			if (storeName.equals(getVoldemortStoreName())) {
				b = putWithApplyUpdate(dataClient, key, value);
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				b = putWithApplyUpdate(sequenceClient,
						mapper.writeValueAsBytes(key),
						mapper.writeValueAsBytes(value));
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				b = putWithApplyUpdate(associationClient,
						mapper.writeValueAsBytes(key),
						mapper.writeValueAsBytes(value));
			}

		} catch (JsonGenerationException e) {
			throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			throwHibernateExceptionFrom(e);
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
			if (storeName.equals(getVoldemortStoreName())) {
				b = putWithApplyUpdate(dataClient,
						mapper.writeValueAsBytes(key), value);
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				b = putWithApplyUpdate(sequenceClient,
						mapper.writeValueAsBytes(key), value);
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				b = putWithApplyUpdate(associationClient,
						mapper.writeValueAsBytes(key), value);
			}
		} catch (JsonGenerationException e) {
			throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			throwHibernateExceptionFrom(e);
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
			if (storeName.equals(getVoldemortStoreName())) {
				b = putWithApplyUpdate(dataClient, key,
						mapper.writeValueAsBytes(value));
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				b = putWithApplyUpdate(sequenceClient, key,
						mapper.writeValueAsBytes(value));
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				b = putWithApplyUpdate(associationClient, key,
						mapper.writeValueAsBytes(value));
			}
		} catch (JsonGenerationException e) {
			throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			throwHibernateExceptionFrom(e);
		}

		return b;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean putWithApplyUpdate(StoreClient client, final Object key,
			final Object value) {

		if (updateAction == null) {
			return client.applyUpdate(new UpdateAction() {
				@Override
				public void update(StoreClient storeClient) {
					storeClient.put(key, value);
				}
			}, maxTries);
		}

		// return client.applyUpdate( updateAction, maxTries );
		return callApplyUpdateWith(client, key, value);
	}

	@SuppressWarnings("rawtypes")
	private boolean callApplyUpdateWith(StoreClient client, Object key,
			Object value) {

		try {
			VoldemortUpdateAction.class.getDeclaredMethod(
					Setter.SET_KEY.getName(), Object.class).invoke(
					updateAction, key);
			VoldemortUpdateAction.class.getDeclaredMethod(
					Setter.SET_VALUE.getName(), Object.class).invoke(
					updateAction, value);
		} catch (IllegalArgumentException e) {
			throwHibernateExceptionFrom(e);
		} catch (SecurityException e) {
			throwHibernateExceptionFrom(e);
		} catch (IllegalAccessException e) {
			throwHibernateExceptionFrom(e);
		} catch (InvocationTargetException e) {
			throwHibernateExceptionFrom(e);
		} catch (NoSuchMethodException e) {
			throwHibernateExceptionFrom(e);
		}

		return client.applyUpdate(updateAction, maxTries);
	}

	/**
	 * Stores the specified key to this object.
	 * 
	 * @param key
	 *            Entity key to be stored.
	 */
	private void addEntryToIdTable(EntityKey key) {

		Serializable rtnId = null;
		tableIds.get(key.getTableName());

		if (tableIds.get(key.getTableName()) == null) {
			Set<Serializable> set = new HashSet<Serializable>();
			set.add(key.getId());
			rtnId = (Serializable) tableIds.put(key.getTableName(), set);
		} else {
			tableIds.get(key.getTableName()).add(key.getId());
		}

		// showAllTableIds();
	}

	/**
	 * Shows all the table name and id pairs currently stored on this object.
	 * This method for debugging purpose.
	 */
	private void showAllTableIds() {
		StringBuilder stringBuilder = new StringBuilder();
		Set<Entry<String, Set<Serializable>>> entries = tableIds.entrySet();
		boolean found = false;
		for (Iterator<Entry<String, Set<Serializable>>> itr = entries
				.iterator(); itr.hasNext();) {
			Entry<String, Set<Serializable>> entry = itr.next();
			generateAllTableIdsMessage(entry, stringBuilder);
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
		removeEntryFromIdTable(key);
		deleteValue(getVoldemortStoreName(), key.getEntityKeyAsMap(), false);
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
			b = deleteWithByteArrayKey(key, storeName);
		} else {
			if (storeName.equals(getVoldemortStoreName())) {
				b = dataClient.delete(key);
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				b = sequenceClient.delete(key);
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				b = associationClient.delete(key);
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
			if (storeName.equals(getVoldemortStoreName())) {
				b = dataClient.delete(mapper.writeValueAsBytes(key));
			} else if (storeName.equals(getVoldemortSequenceStoreName())) {
				b = sequenceClient.delete(mapper.writeValueAsBytes(key));
			} else if (storeName.equals(getVoldemortAssociationStoreName())) {
				b = associationClient.delete(mapper.writeValueAsBytes(key));
			}
		} catch (JsonGenerationException e) {
			throwHibernateExceptionFrom(e);
		} catch (JsonMappingException e) {
			throwHibernateExceptionFrom(e);
		} catch (IOException e) {
			throwHibernateExceptionFrom(e);
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
		tableIds.get(key.getTableName()).remove(key.getId());
	}

	@SuppressWarnings("rawtypes")
	public Map<RowKey, Map<String, Object>> getAssociation(AssociationKey key) {

		Versioned v = getAssociationFrom(key);

		if (v == null) {
			return null;
		}

		return createAssociationFrom((String) createReturnObjectFrom(v,
				String.class));
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

		Map associationMap = (Map) jsonHelper.fromJSON(jsonedAssociation,
				Map.class);

		Map<RowKey, Map<String, Object>> association = new HashMap<RowKey, Map<String, Object>>();
		for (Iterator itr = associationMap.keySet().iterator(); itr.hasNext();) {
			String key = (String) itr.next();
			RowKey rowKey = (RowKey) jsonHelper.fromJSON(key, RowKey.class);
			Map<String, Object> val = (Map<String, Object>) jsonHelper
					.fromJSON((String) associationMap.get(key), Map.class);
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

		return getValue(getVoldemortAssociationStoreName(),
				jsonHelper.toJSON(key.getAssociationKeyAsMap()), true);
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

		associationKeys.add(key);
		putValue(getVoldemortAssociationStoreName(), jsonHelper.toJSON(key
				.getAssociationKeyAsMap()), jsonHelper.toJSON(jsonHelper
				.convertKeyAndValueToJsonOn(associationMap)), true, true);
	}

	/**
	 * Removes association around the specified association key,
	 * 
	 * @param key
	 *            Used to remove the association.
	 */
	public void removeAssociation(AssociationKey key) {
		associationKeys.remove(key);
		deleteValue(getVoldemortAssociationStoreName(),
				jsonHelper.toJSON(key.getAssociationKeyAsMap()), true);
	}

	public void setNextValue(RowKey key, IntegralDataTypeHolder value,
			int increment, int initialValue) {

		List<Integer> l = new LinkedList<Integer>();
		l.add(initialValue);
		if (nextValues.putIfAbsent(key, l) == null) {
			Map<String, Integer> nextSequence = new HashMap<String, Integer>();
			nextSequence.put(VoldemortDatastoreProvider.SEQUENCE_LABEL,
					initialValue);
			value.initialize(initialValue);

			if (flushToDb) {
				// taskQueue.offer(new PutSequenceRunnable(this, this
				// .getVoldemortSequenceStoreName(), toJSON(key
				// .getRowKeyAsMap()), nextSequence, true, false));

				putValue(getVoldemortSequenceStoreName(),
						jsonHelper.toJSON(key.getRowKeyAsMap()), nextSequence,
						true, false);
			}
		} else {
			List<Integer> list = nextValues.get(key);
			synchronized (list) {
				boolean notContained = false;
				int candidate = list.get(list.size() - 1) + increment;
				if (!list.contains(candidate)) {
					nextValues.get(key).add(candidate);
					notContained = true;
				}
				value.initialize(candidate);
				Map<String, Integer> nextSequence = new HashMap<String, Integer>();
				nextSequence.put(VoldemortDatastoreProvider.SEQUENCE_LABEL,
						candidate);

				if (flushToDb) {
					if (notContained) {
						putValue(getVoldemortSequenceStoreName(),
								jsonHelper.toJSON(key.getRowKeyAsMap()),
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
		synchronized (associationKeys) {
			for (AssociationKey associationKey : associationKeys) {
				associations
						.put(associationKey, getAssociation(associationKey));
			}
		}

		return associations;
	}

	/**
	 * Meant to execute assertions in tests only. Delete
	 * EntityKeyBuilder.DEBUG_OGM_PERSISTER when tests are done.
	 * 
	 * @return a read-only view of the map containing the entities
	 */
	public Map<EntityKey, Map<String, Object>> getEntityMap() {

		Map<EntityKey, Map<String, Object>> map = new HashMap<EntityKey, Map<String, Object>>();
		Set<Entry<String, Set<Serializable>>> entries = tableIds.entrySet();
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
				map.put(entityKey, getEntityTuple(entityKey));
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
		String sequenceStoreName = getRequiredProperties().get(
				VoldemortProp.SEQUENCE.getAlias());
		return sequenceStoreName == null || sequenceStoreName.equals("") ? VoldemortProp.SEQUENCE
				.getName() : sequenceStoreName;
	}

	/**
	 * Gets store name for Voldemort.
	 * 
	 * @return Store name specified on hibernate.properties.
	 */
	public String getVoldemortStoreName() {

		String storeName = getRequiredProperties().get(
				VoldemortProp.DATA.getAlias());
		return storeName == null || storeName.equals("") ? VoldemortProp.DATA
				.getName() : storeName;
	}

	/**
	 * Gets association store name for Voldemort.
	 * 
	 * @return Association store name specified on hibernate.properties.
	 */
	public String getVoldemortAssociationStoreName() {

		String associationStoreName = getRequiredProperties().get(
				VoldemortProp.ASSOCIATION.getAlias());
		return associationStoreName == null || associationStoreName.equals("") ? VoldemortProp.ASSOCIATION
				.getName() : associationStoreName;
	}
}

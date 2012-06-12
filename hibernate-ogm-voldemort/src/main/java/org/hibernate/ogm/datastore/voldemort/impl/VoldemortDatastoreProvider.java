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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
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
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.ClassUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.VoldemortDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.helper.JSONHelper;
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
import voldemort.server.VoldemortConfig;
import voldemort.server.VoldemortServer;
import voldemort.versioning.Versioned;

/**
 * 
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 * 
 */
public class VoldemortDatastoreProvider implements DatastoreProvider, Startable, Stoppable {

	private static final Log log = LoggerFactory.make();
	private StoreClientFactory clientFactory;
	private final ObjectMapper mapper = new ObjectMapper();
	private final ConcurrentMap<String,EntityKey> entityKeys = new ConcurrentHashMap<String,EntityKey>();
	private final Set<AssociationKey> associationKeys = Collections.synchronizedSet( new HashSet<AssociationKey>() );
	private StoreClient dataClient;
	private StoreClient associationClient;
	private StoreClient sequenceClient;
	public static final String SEQUENCE_LABEL = "nextSequence";
	private final ConcurrentMap<RowKey, List<Integer>> nextValues = new ConcurrentHashMap<RowKey, List<Integer>>();
	private boolean flushToDb = false;
	private int maxTries;
	private VoldemortUpdateAction updateAction;
	private final JSONHelper jsonHelper = new JSONHelper();
	private Map<String, String> requiredProperties;
	private VoldemortServer embeddedServer;
	private static final CopyOnWriteArrayList<DatastoreProvider> datastoreProviderList = new CopyOnWriteArrayList<DatastoreProvider>();

	private static enum RequiredProp {

		PROVIDER("provider", "hibernate.ogm.datastore.provider"), DIALECT("dialect", "hibernate.dialect"), PROVIDER_URL(
				"provider_url", "hibernate.ogm.datastore.provider_url");

		private String name;
		private String propPath;

		RequiredProp(String name, String propPath) {
			this.name = name;
			this.propPath = propPath;
		}

		public String getName() {
			return name;
		}

		public String getPropPath() {
			return propPath;
		}
	}

	private static enum VoldemortProp {

		ASSOCIATION("HibernateOGM-Association", "voldemort_association_store",
				"hibernate.ogm.datastore.voldemort_association_store"), DATA("HibernateOGM", "voldemort_store",
				"hibernate.ogm.datastore.voldemort_store"), SEQUENCE("HibernateOGM-Sequence",
				"voldemort_sequence_store", "hibernate.ogm.datastore.voldemort_sequence_store"), FLUSH_SEQUENCE_TO_DB(
				"FlushSequence", "FlushSequence", "hibernate.ogm.datastore.voldemort_flush_sequence_to_db"), MAX_TRIES(
				"MaxTries", "MaxTries", "hibernate.ogm.datastore.voldemort_max_tries"), UPDATE_ACTION("UpdateAction",
				"UpdateAction", "hibernate.ogm.datastore.voldemort_update_action"), DEBUG_DATASTORE_LOCATION(
				"DebugDatastoreLocation", "DebugDatastoreLocation", "hibernate.ogm.datastore.provider_debug_location");

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

	public JSONHelper getJsonHelper() {
		return jsonHelper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.service.spi.Stoppable#stop()
	 */
	@Override
	public void stop() {
		log.info( "stopping Voldemort embeddedServer: " + embeddedServer );
		entityKeys.clear();
		associationKeys.clear();
		nextValues.clear();
		datastoreProviderList.clear();

		if ( clientFactory != null ) {
			clientFactory.close();
		}

		if ( embeddedServer != null ) {
			embeddedServer.stop();
			removeAllEntries();
		}
	}

	/**
	 * Assumes that Voldemort is used for testing since it's started as an embedded datastore.
	 * Removes data from the specified embedded location.
	 * TODO would be better to remove all the entries using VoldemrotDialect or VoldemortDatastore since they cache
	 * all the entries.
	 */
	private void removeAllEntries() {
		deleteDirectories( new File( requiredProperties.get( VoldemortProp.DEBUG_DATASTORE_LOCATION.getAlias() )
				+ File.separator + "data" ) );
	}

	public boolean deleteDirectories(File directory) {
		if ( directory.isDirectory() ) {
			for ( String subDirectory : directory.list() ) {
				boolean done = deleteDirectories( new File( directory, subDirectory ) );
				if ( !done ) {
					return false;
				}
			}

		}
		return directory.delete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.service.spi.Startable#start()
	 */
	@Override
	public void start() {
		try {
			if ( !checkRequiredSettings() ) {
				throw new HibernateException( "Please configure Voldemort on hibernate.properties correctly." );
			}

			if(!addDatastoreProvider()){
				stopAlreadyStartedDatastoreProvider();
				addDatastoreProvider();
			}
			
			startVoldemortServer();
			setClientFactory();
			setVoldemortClients();
			setFlushToDBFlag();
			setMaxTries();
			setUpdateAction();
		}
		catch ( Throwable ex ) {
			stopAlreadyStartedDatastoreProvider();
		}
	}

	/**
	 * Checks required property settings for Voldemort on hibernate.properties.
	 * 
	 * @return True if all the required properties are set, false otherwise.
	 */
	protected boolean checkRequiredSettings() {
		requiredProperties = getRequiredPropertyValues();

		if ( requiredProperties.get( RequiredProp.PROVIDER.getName() ).equals( this.getClass().getCanonicalName() )
				&& requiredProperties.get( RequiredProp.PROVIDER_URL.getName() ) != null
				&& requiredProperties.get( RequiredProp.DIALECT.getName() ) != null ) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the common required property values among other datastore provider
	 * and the datastore specific property values.
	 * 
	 * @return Key value pairs storing the properties.
	 */
	private Map<String, String> getRequiredPropertyValues() {
		Map<String, String> map = new HashMap<String, String>();
		map.put( RequiredProp.PROVIDER.getName(),
				Environment.getProperties().getProperty( RequiredProp.PROVIDER.getPropPath() ) );
		map.put( RequiredProp.DIALECT.getName(), RequiredProp.DIALECT.getPropPath() );
		map.put( RequiredProp.PROVIDER_URL.getName(),
				Environment.getProperties().getProperty( RequiredProp.PROVIDER_URL.getPropPath() ) );
		map.putAll( getSpecificSettings() );
		return Collections.unmodifiableMap( map );
	}

	/**
	 * Adds this to datastoreProviderList.
	 * @return True if and only if there are no already started providers exist, otherwise false.
	 */
	private boolean addDatastoreProvider() {
		if(!datastoreProviderList.isEmpty()){
			return false;
		}
		return datastoreProviderList.add( this );
	}

	/**
	 * Stops already started datastore providers. When starting datastore provider multiple times,
	 * it is necessary to stop them first before the current one works properly.
	 * @return True if and only if there are already started datastore provider and stop them successfully. Return false
	 * when there are no datastores have not been started or there is only datastore provider exists which is the current one.
	 */
	private boolean stopAlreadyStartedDatastoreProvider() {

		if ( datastoreProviderList.isEmpty() ) {
			// // Nothing to stop.
			return false;
		}
		else if ( datastoreProviderList.size() == 1 ) {
			// // There is only the current datastore provider exists in the list.
			return false;
		}

		// // Stop already started datastore provider except the current one.
		boolean[] stopped = new boolean[datastoreProviderList.size()];
		for ( int i = 0; i < datastoreProviderList.size(); i++ ) {
			( (VoldemortDatastoreProvider) datastoreProviderList.get( i ) ).stop();
			stopped[i] = true;
		}

		int trueCount = 0;
		synchronized ( stopped ) {
			for ( boolean b : stopped ) {
				if ( b ) {
					trueCount++;
				}
			}
		}
		
		if(trueCount == stopped.length){
			return true;
		}
		
		return false;
	}

	/**
	 * Starts Voldemort server.
	 */
	private void startVoldemortServer() {

		String embeddedLocation = getEmbeddedLocation();
		if ( embeddedLocation.equals( "" ) ) {
			log.info( "Voldemort standalone server should be started by the user" );
		}
		else {
			startEmbeddedServer( embeddedLocation );
		}
	}

	/**
	 * Gets the location of configuration file for embedded Voldemort datastore from hibernate.properties.
	 * 
	 * @return Location if it exists and non-empty string otherwise empty string.
	 */
	private String getEmbeddedLocation() {
		String embeddedLocation = requiredProperties.get( VoldemortProp.DEBUG_DATASTORE_LOCATION.getAlias() );
		if ( embeddedLocation == null || embeddedLocation.equals( "" ) ) {
			return "";
		}

		return embeddedLocation;
	}

	/**
	 * Starts Voldemort as Embedded datastore.
	 * 
	 * @param embeddedLocation
	 *            Location of the configuration file for Voldemort embedded datastore.
	 */
	private void startEmbeddedServer(String embeddedLocation) {

		if ( embeddedServer == null ) {
			log.info( "Voldemort embedded server starting ..." );
			embeddedServer = new VoldemortServer( VoldemortConfig.loadFromVoldemortHome( embeddedLocation ) );
			embeddedServer.start();
		}
		else {
			log.info( "Voldemort embedded serevr is already started" );
		}
	}

	/**
	 * Sets clientFactory property.
	 */
	private void setClientFactory() {
		clientFactory = new SocketStoreClientFactory( new ClientConfig().setBootstrapUrls( requiredProperties
				.get( "provider_url" ) ) );
	}

	/**
	 * Sets three voldemort clients for each store,association store, data store
	 * and sequence store.
	 */
	private void setVoldemortClients() {
		dataClient = clientFactory.getStoreClient( getVoldemortStoreName() );
		sequenceClient = clientFactory.getStoreClient( getVoldemortSequenceStoreName() );
		associationClient = clientFactory.getStoreClient( getVoldemortAssociationStoreName() );
	}

	/**
	 * Sets flushToDb flag based on the value from hibernate.properties. The
	 * default value is true.
	 */
	private void setFlushToDBFlag() {
		String flushToDbProp = requiredProperties.get( VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias() );
		log.info( "flushToDbProp: " + requiredProperties.get( VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias() ) );
		flushToDb = flushToDbProp == null || flushToDbProp.equals( "" ) || flushToDbProp.equals( "true" ) ? true
				: false;

		log.info( "set flush sequence to db flag as " + flushToDb );
	}

	/**
	 * Gets the Voldemort specific setting values. Currently there is only one
	 * settings for the store name.
	 * 
	 * @return Key-value pair for the Voldemort specific setting.
	 */
	protected Map<String, String> getSpecificSettings() {

		Map<String, String> specificSettings = new HashMap<String, String>();
		specificSettings.put( VoldemortProp.DATA.getAlias(),
				Environment.getProperties().getProperty( VoldemortProp.DATA.getPropPath() ) );
		specificSettings.put( VoldemortProp.ASSOCIATION.getAlias(),
				Environment.getProperties().getProperty( VoldemortProp.ASSOCIATION.getPropPath() ) );
		specificSettings.put( VoldemortProp.SEQUENCE.getAlias(),
				Environment.getProperties().getProperty( VoldemortProp.SEQUENCE.getPropPath() ) );
		specificSettings.put( VoldemortProp.FLUSH_SEQUENCE_TO_DB.getAlias(),
				Environment.getProperties().getProperty( VoldemortProp.FLUSH_SEQUENCE_TO_DB.getPropPath() ) );
		specificSettings.put( VoldemortProp.MAX_TRIES.getAlias(),
				Environment.getProperties().getProperty( VoldemortProp.MAX_TRIES.getPropPath() ) );
		specificSettings.put( VoldemortProp.UPDATE_ACTION.getAlias(),
				Environment.getProperties().getProperty( VoldemortProp.UPDATE_ACTION.getPropPath() ) );
		specificSettings.put( VoldemortProp.DEBUG_DATASTORE_LOCATION.getAlias(), Environment.getProperties()
				.getProperty( VoldemortProp.DEBUG_DATASTORE_LOCATION.getPropPath() ) );
		return Collections.unmodifiableMap( specificSettings );
	}

	/**
	 * Sets max try for put. This is the way to achieve optimistic lock in
	 * Voldemort. See
	 * http://project-voldemort.com/javadoc/all/index.html?overview-summary.html
	 */
	private void setMaxTries() {
		String mTries = requiredProperties.get( VoldemortProp.MAX_TRIES.getAlias() );
		log.info( "max tries: " + mTries );
		maxTries = mTries == null || mTries.equals( "" ) || Integer.parseInt( mTries ) <= 0 ? 3 : Integer
				.parseInt( mTries );

		log.info( "set max tries as " + maxTries );
	}

	/**
	 * Sets updateAction property. This is the object that achieves optimistic
	 * lock with max tries property in Voldemort. See
	 * http://project-voldemort.com/javadoc/all/index.html?overview-summary.html
	 */
	@SuppressWarnings("rawtypes")
	private void setUpdateAction() {
		String uAction = requiredProperties.get( VoldemortProp.UPDATE_ACTION.getAlias() );
		log.info( "update action: " + uAction );

		if ( uAction != null && !uAction.equals( "" ) ) {
			try {
				updateAction = (VoldemortUpdateAction) Class.forName( uAction ).newInstance();
			}
			catch ( InstantiationException e ) {
				e.printStackTrace();
				throwHibernateExceptionFrom( e );
			}
			catch ( IllegalAccessException e ) {
				throwHibernateExceptionFrom( e );
			}
			catch ( ClassNotFoundException e ) {
				throwHibernateExceptionFrom( e );
			}
		}

		log.info( "set updateAction as " + updateAction );
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
		Versioned v = getValue( getVoldemortStoreName(), getEntityKeyAsMap( key ), false );

		if ( v == null ) {
			return null;
		}

		return jsonHelper.convertFromJsonOn( key, (Map<String, Object>) createReturnObjectFrom( v, Map.class ),
				getDeclaredFieldsFrom( key.getEntityName() ) );
	}
	
    /**
     * Gets entity key as Map containing id and table name.
     * 
     * @return Map containing id and table name.
     */
    public Map<String, String> getEntityKeyAsMap(EntityKey entityKey) {
        Map<String, String> map = new HashMap<String, String>();
        map.put( "id", entityKey.getId().toString() );
        map.put( "table", entityKey.getTable() );
        return Collections.unmodifiableMap( map );
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
		if ( keyByteArray ) {
			v = getValueWithByteKey( key, storeName );
		}
		else {

			if ( storeName.equals( getVoldemortStoreName() ) ) {
				v = dataClient.get( key );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				v = sequenceClient.get( key );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				v = associationClient.get( key );
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
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				return dataClient.get( mapper.writeValueAsBytes( key ) );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				return sequenceClient.get( mapper.writeValueAsBytes( key ) );
			}

			v = associationClient.get( mapper.writeValueAsBytes( key ) );

		}
		catch ( JsonGenerationException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( JsonMappingException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IOException e ) {
			throwHibernateExceptionFrom( e );
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
			rtnValue = mapper.readValue( (byte[]) v.getValue(), 0, ( (byte[]) v.getValue() ).length, cls );
		}
		catch ( JsonParseException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( JsonMappingException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IOException e ) {
			throwHibernateExceptionFrom( e );
		}

		return rtnValue;
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
			fields = Class.forName( className ).getDeclaredFields();
		}
		catch ( SecurityException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( ClassNotFoundException e ) {
			throwHibernateExceptionFrom( e );
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

		addEntryToIdTable( key );
		return writeEntityTupleFrom( key, tuple );
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
	private boolean writeEntityTupleFrom(EntityKey key, Map<String, Object> tuple) {
		return putValue( getVoldemortStoreName(), getEntityKeyAsMap( key ), tuple, false, true );
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
	public boolean putValue(String storeName, Object key, Object value, boolean keyByteArray, boolean valueByteArray) {

		boolean b = false;

		if ( keyByteArray && valueByteArray ) {
			b = putByteArrayKeyAndByteArrayValue( key, value, storeName );
		}
		else if ( keyByteArray && !valueByteArray ) {
			b = putByteArrayKeyAndValue( key, value, storeName );
		}
		else if ( !keyByteArray && valueByteArray ) {
			b = putKeyAndByteArrayValue( key, value, storeName );
		}
		else if ( !keyByteArray && !valueByteArray ) {
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				b = putWithApplyUpdate( dataClient, key, value );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				b = putWithApplyUpdate( sequenceClient, key, value );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				b = putWithApplyUpdate( associationClient, key, value );
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
	private boolean putByteArrayKeyAndByteArrayValue(Object key, Object value, String storeName) {

		boolean b = false;
		try {
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				b = putWithApplyUpdate( dataClient, key, value );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				b = putWithApplyUpdate( sequenceClient, mapper.writeValueAsBytes( key ),
						mapper.writeValueAsBytes( value ) );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				b = putWithApplyUpdate( associationClient, mapper.writeValueAsBytes( key ),
						mapper.writeValueAsBytes( value ) );
			}

		}
		catch ( JsonGenerationException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( JsonMappingException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IOException e ) {
			throwHibernateExceptionFrom( e );
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
	private boolean putByteArrayKeyAndValue(Object key, Object value, String storeName) {

		boolean b = false;
		try {
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				b = putWithApplyUpdate( dataClient, mapper.writeValueAsBytes( key ), value );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				b = putWithApplyUpdate( sequenceClient, mapper.writeValueAsBytes( key ), value );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				b = putWithApplyUpdate( associationClient, mapper.writeValueAsBytes( key ), value );
			}
		}
		catch ( JsonGenerationException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( JsonMappingException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IOException e ) {
			throwHibernateExceptionFrom( e );
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
	private boolean putKeyAndByteArrayValue(Object key, Object value, String storeName) {

		boolean b = false;
		try {
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				b = putWithApplyUpdate( dataClient, key, mapper.writeValueAsBytes( value ) );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				b = putWithApplyUpdate( sequenceClient, key, mapper.writeValueAsBytes( value ) );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				b = putWithApplyUpdate( associationClient, key, mapper.writeValueAsBytes( value ) );
			}
		}
		catch ( JsonGenerationException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( JsonMappingException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IOException e ) {
			throwHibernateExceptionFrom( e );
		}

		return b;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean putWithApplyUpdate(StoreClient client, final Object key, final Object value) {

		if ( updateAction == null ) {
			return client.applyUpdate( new UpdateAction() {
				@Override
				public void update(StoreClient storeClient) {
					storeClient.put( key, value );
				}
			}, maxTries );
		}

		// return client.applyUpdate( updateAction, maxTries );
		return callApplyUpdateWith( client, key, value );
	}

	@SuppressWarnings("rawtypes")
	private boolean callApplyUpdateWith(StoreClient client, Object key, Object value) {

		try {
			VoldemortUpdateAction.class.getDeclaredMethod( Setter.SET_KEY.getName(), Object.class ).invoke(
					updateAction, key );
			VoldemortUpdateAction.class.getDeclaredMethod( Setter.SET_VALUE.getName(), Object.class ).invoke(
					updateAction, value );
		}
		catch ( IllegalArgumentException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( SecurityException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IllegalAccessException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( InvocationTargetException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( NoSuchMethodException e ) {
			throwHibernateExceptionFrom( e );
		}

		return client.applyUpdate( updateAction, maxTries );
	}

	/**
	 * Stores the specified key to this object.
	 * 
	 * @param key
	 *            Entity key to be stored.
	 */
	private void addEntryToIdTable(EntityKey key) {
		entityKeys.putIfAbsent( jsonHelper.toJSON( getEntityKeyAsMap( key ) ), key );
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
	private void generateAllTableIdsMessage(Entry<String, Set<Serializable>> entry, StringBuilder stringBuilder) {

		stringBuilder.append( "table name: " + entry.getKey() + "\n" );
		if ( entry.getValue().isEmpty() ) {
			stringBuilder.append( "\tall the ids on table, " + entry.getKey() + " are already deleted.\n" );
		}
		else {
			for ( Iterator<Serializable> itr = entry.getValue().iterator(); itr.hasNext(); ) {
				stringBuilder.append( "\tid: " + itr.next() + "\n" );
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
		removeEntryFromIdTable( key );
		deleteValue( getVoldemortStoreName(), getEntityKeyAsMap( key ), false );
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
	public boolean deleteValue(String storeName, Object key, boolean keyByteArray) {

		boolean b = false;

		if ( keyByteArray ) {
			b = deleteWithByteArrayKey( key, storeName );
		}
		else {
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				b = dataClient.delete( key );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				b = sequenceClient.delete( key );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				b = associationClient.delete( key );
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
			if ( storeName.equals( getVoldemortStoreName() ) ) {
				b = dataClient.delete( mapper.writeValueAsBytes( key ) );
			}
			else if ( storeName.equals( getVoldemortSequenceStoreName() ) ) {
				b = sequenceClient.delete( mapper.writeValueAsBytes( key ) );
			}
			else if ( storeName.equals( getVoldemortAssociationStoreName() ) ) {
				b = associationClient.delete( mapper.writeValueAsBytes( key ) );
			}
		}
		catch ( JsonGenerationException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( JsonMappingException e ) {
			throwHibernateExceptionFrom( e );
		}
		catch ( IOException e ) {
			throwHibernateExceptionFrom( e );
		}

		return b;
	}

	/**
	 * Converts the specified exception to HibernateException and rethrows it.
	 * 
	 * @param <T>
	 * @param exception
	 *            Exception to be rethrown as HibernateException.
	 */
	protected <T extends Throwable> void throwHibernateExceptionFrom(T exception) {
		throw new HibernateException( exception.getCause() );
	}

	/**
	 * Removes the specified key from this object.
	 * 
	 * @param key
	 *            Entity key to be removed.
	 */
	private void removeEntryFromIdTable(EntityKey key) {
		entityKeys.remove( jsonHelper.toJSON( getEntityKeyAsMap( key ) ) );
	}

	@SuppressWarnings("rawtypes")
	public Map<RowKey, Map<String, Object>> getAssociation(AssociationKey key) {

		Versioned v = getAssociationFrom( key );

		if ( v == null ) {
			return null;
		}

		return createAssociationFrom( (String) createReturnObjectFrom( v, String.class ) );
	}

	/**
	 * Creates association from the specified Jsoned string.
	 * 
	 * @param jsonedAssociation
	 *            Representation of association as JSON.
	 * @return Association based on the specified string.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<RowKey, Map<String, Object>> createAssociationFrom(String jsonedAssociation) {

		Map associationMap = (Map) jsonHelper.fromJSON( jsonedAssociation, Map.class );

		Map<RowKey, Map<String, Object>> association = new HashMap<RowKey, Map<String, Object>>();
		for ( Iterator itr = associationMap.keySet().iterator(); itr.hasNext(); ) {
			String key = (String) itr.next();
			RowKey rowKey = (RowKey) jsonHelper.fromJSON( key, RowKey.class );
			association.put( rowKey, jsonHelper.createAssociation( (String) associationMap.get( key ) ) );
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

		return getValue( getVoldemortAssociationStoreName(), jsonHelper.toJSON( getAssociationKeyAsMap(key) ), true );
	}

	/**
	 * Puts association on Voldemort.
	 * 
	 * @param key
	 *            Key for the association.
	 * @param associationMap
	 *            Map representing the association.
	 */
	public void putAssociation(AssociationKey key, Map<RowKey, Map<String, Object>> associationMap) {

		associationKeys.add( key );
		putValue( getVoldemortAssociationStoreName(), jsonHelper.toJSON( getAssociationKeyAsMap(key) ),
				jsonHelper.toJSON( jsonHelper.convertKeyAndValueToJsonOn( associationMap ) ), true, true );
	}

	/**
	 * Removes association around the specified association key,
	 * 
	 * @param key
	 *            Used to remove the association.
	 */
	public void removeAssociation(AssociationKey key) {
		associationKeys.remove( key );
		deleteValue( getVoldemortAssociationStoreName(), jsonHelper.toJSON( getAssociationKeyAsMap(key) ), true );
	}
	
	/**
	 * Gets association key as Map object containing owning columns.
	 * 
	 * @return Association key as Map representation.
	 */
	public Map<String, Object> getAssociationKeyAsMap(AssociationKey associationKey) {

		Map<String, Object> map = new HashMap<String, Object>();
		for ( int i = 0; i < associationKey.getColumnNames().length; i++ ) {
			map.put( associationKey.getColumnNames()[i], associationKey.getColumnValues()[i] );
		}
		map.put( "table", associationKey.getTable());
		return Collections.unmodifiableMap( map );
	}

	public void setNextValue(RowKey key, Map<String,Object> rowKeyMap, IntegralDataTypeHolder value, int increment, int initialValue) {

		/**
		 * TODO To implement VoldemortDatastoreProvider.setNextValue() method, I
		 * was confused a little bit. I originally referenced other datastore
		 * and implemented it, but on VoldemortDialectTest.testIsThreadSafe()
		 * which tests concurrency on the method, [my original
		 * implementation](https://gist.github
		 * .com/1903794#file_original+set_next_value%28%29) ran poorly because
		 * it required an exclusive lock. And then I modified the method and got
		 * the current implementation. However, it doesn't quite reduce the
		 * number of accesses to the underlying datastore as I wanted, but
		 * allows concurrency. As a result, I put a flag to store the next value
		 * on the datastore or not. I'm not quite sure if this is the right
		 * implementation or not.
		 */

		List<Integer> l = new LinkedList<Integer>();
		l.add( initialValue );
		if ( nextValues.putIfAbsent( key, l ) == null ) {
			Map<String, Integer> nextSequence = new HashMap<String, Integer>();
			nextSequence.put( VoldemortDatastoreProvider.SEQUENCE_LABEL, initialValue );
			value.initialize( initialValue );

			if ( flushToDb ) {
				putValue( getVoldemortSequenceStoreName(), jsonHelper.toJSON( rowKeyMap ), nextSequence,
						true, false );
			}
		}
		else {
			List<Integer> list = nextValues.get( key );
			synchronized ( list ) {
				boolean notContained = false;
				int candidate = list.get( list.size() - 1 ) + increment;
				if ( !list.contains( candidate ) ) {
					nextValues.get( key ).add( candidate );
					notContained = true;
				}
				value.initialize( candidate );
				Map<String, Integer> nextSequence = new HashMap<String, Integer>();
				nextSequence.put( VoldemortDatastoreProvider.SEQUENCE_LABEL, candidate );

				if ( flushToDb ) {
					if ( notContained ) {
						putValue( getVoldemortSequenceStoreName(), jsonHelper.toJSON( rowKeyMap ),
								nextSequence, true, false );
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
		synchronized ( associationKeys ) {
			for ( AssociationKey associationKey : associationKeys ) {
				associations.put( associationKey, getAssociation( associationKey ) );
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
		for(Iterator<Entry<String,EntityKey>> itr = entityKeys.entrySet().iterator();itr.hasNext();){
			Entry<String,EntityKey> entry = itr.next();
			map.put( entry.getValue(), getEntityTuple( entry.getValue()) );
		}

		return map;
	}

	/**
	 * Gets sequence store name for Voldemort.
	 * 
	 * @return Sequence store name specified on hibernate.properties.
	 */
	public String getVoldemortSequenceStoreName() {
		String sequenceStoreName = requiredProperties.get( VoldemortProp.SEQUENCE.getAlias() );
		return sequenceStoreName == null || sequenceStoreName.equals( "" ) ? VoldemortProp.SEQUENCE.getName()
				: sequenceStoreName;
	}

	/**
	 * Gets store name for Voldemort.
	 * 
	 * @return Store name specified on hibernate.properties.
	 */
	public String getVoldemortStoreName() {

		String storeName = requiredProperties.get( VoldemortProp.DATA.getAlias() );
		return storeName == null || storeName.equals( "" ) ? VoldemortProp.DATA.getName() : storeName;
	}

	/**
	 * Gets association store name for Voldemort.
	 * 
	 * @return Association store name specified on hibernate.properties.
	 */
	public String getVoldemortAssociationStoreName() {

		String associationStoreName = requiredProperties.get( VoldemortProp.ASSOCIATION.getAlias() );
		return associationStoreName == null || associationStoreName.equals( "" ) ? VoldemortProp.ASSOCIATION.getName()
				: associationStoreName;
	}
}

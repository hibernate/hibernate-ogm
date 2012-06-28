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

package org.hibernate.ogm.datastore.redis.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ClassUtils;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.RedisDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.helper.JSONHelper;
import org.hibernate.ogm.helper.rollback.RollbackAction;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class RedisDatastoreProvider implements DatastoreProvider, Startable, Stoppable {

	private static final Log log = LoggerFactory.make();
	private final JSONHelper jsonHelper = new JSONHelper();
	private Map<String, String> requiredProperties;
	private Pattern setterPattern;
	private JedisPool pool;
	private Jedis jedis;
	private static final String PROPERTY_PREFIX = "hibernate.datastore.provider.redis_config.";
	private static final String ENTITY_HSET = "OGM-Entity";
	private static final String ASSOCIATION_HSET = "OGM-Association";
	private static final String SEQUENCE_HSET = "OGM-Sequence";
	// entityKeys is meant to execute assertions in tests only same as getEntityMap() and getAssociationsMap().
	private final ConcurrentMap<String, EntityKey> entityKeys = new ConcurrentHashMap<String, EntityKey>();
	// associationKeys is meant to execute assertions in tests only same as getEntityMap() and getAssociationsMap().
	private final ConcurrentMap<String, AssociationKey> associationKeys = new ConcurrentHashMap<String, AssociationKey>();
	private static final String SEQUENCE_LABEL = "nextSequence";
	
	private static enum RequiredProp {
		PROVIDER("provider", "hibernate.ogm.datastore.provider"), PROVIDER_URL(
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
	
	/* (non-Javadoc)
	 * @see org.hibernate.service.spi.Stoppable#stop()
	 */
	@Override
	public void stop() {
		log.redisStopping();
		entityKeys.clear();
		associationKeys.clear();
		pool.destroy();
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
				throw new HibernateException( "Please configure Redis on hibernate.properties correctly." );
			}

			log.redisStarting();
			setUpRedis();
		}
		catch ( Exception ex ) {
			stop();
		}
	}
	
	/**
	 * Reused from VoldemortDatastoreProvider. Checks required property settings for Redis on hibernate.properties.
	 * 
	 * @return True if all the required properties are set, false otherwise.
	 */
	protected synchronized boolean checkRequiredSettings() {
		requiredProperties = getRequiredPropertyValues();

		if ( requiredProperties.get( RequiredProp.PROVIDER.getName() ).equals( this.getClass().getCanonicalName() )
				&& requiredProperties.get( RequiredProp.PROVIDER_URL.getName() ) != null ) {
			return true;
		}
		return false;
	}

	/**
	 * Reused from VoldemortDatastoreProvider. Gets the common required property values among other datastore provider
	 * and the datastore specific property values.
	 * 
	 * @return Key value pairs storing the properties.
	 */
	private Map<String, String> getRequiredPropertyValues() {
		Map<String, String> map = new HashMap<String, String>();
		map.put( RequiredProp.PROVIDER.getName(),
				Environment.getProperties().getProperty( RequiredProp.PROVIDER.getPropPath() ) );
		map.put( RequiredProp.PROVIDER_URL.getName(),
				Environment.getProperties().getProperty( RequiredProp.PROVIDER_URL.getPropPath() ) );
		
		return Collections.unmodifiableMap( map );
	}
	
	/**
	 * Sets up Redis.
	 */
	private synchronized void setUpRedis() {
		pool = new JedisPool( createJedisConfig(), requiredProperties.get( RequiredProp.PROVIDER_URL.getName() ) );
		jedis = pool.getResource();
	}
	
	/**
	 * Reads Redis configuration properties from hibernate.properties and creates JedisPoolConfig object.
	 * 
	 * @return Newly created JedisPoolConfig object.
	 */
	private final JedisPoolConfig createJedisConfig() {

		final JedisPoolConfig jedisConfig = new JedisPoolConfig();
		Method[] methods = jedisConfig.getClass().getDeclaredMethods();
		Entry<Object, Object> entry = null;
		String key = "";
		for ( Iterator<Entry<Object, Object>> itr = Environment.getProperties().entrySet().iterator(); itr.hasNext(); ) {
			entry = itr.next();
			key = (String) entry.getKey();

			if ( key.startsWith( PROPERTY_PREFIX ) ) {
				setConfigProperty( key, (String) entry.getValue(), methods, jedisConfig );
			}
		}

		return jedisConfig;
	}
	
	/**
	 * Sets Redis properties on hibernate.properties started with "hibernate.datastore.provider.redis_config." using the
	 * corresponding setter methods on the parameter, jedisConfig. Please check the javadoc at
	 * http://www.jarvana.com/jarvana/view/redis/clients/jedis/2.0.0/jedis-2.0.0-javadoc.jar!/index.html
	 * 
	 * @param key
	 *            Name of the property.
	 * @param value
	 *            Value of the property.
	 * @param methods
	 *            Methods declared on JedisPoolConfig.
	 * @param jedisConfig
	 *            Where the value is set calling the corresponding setter.
	 */
	private void setConfigProperty(String key, String value, Method[] methods, JedisPoolConfig jedisConfig) {

		String propertyName = key.substring( key.lastIndexOf( "." ) + 1 );
		for ( Method method : methods ) {
			setterPattern = Pattern.compile( "^set" + propertyName + "$", Pattern.CASE_INSENSITIVE );
			if ( hasPattern( method.getName(), setterPattern ) ) {
				callSetter( value, method, jedisConfig );
			}
		}
	}
	
	/**
	 * Calls setter method.
	 * 
	 * @param value
	 *            Value to be set through the setter.
	 * @param method
	 *            Setter.
	 * @param jedisConfig
	 *            Where the setter with the value is called.
	 */
	private void callSetter(String value, Method method, JedisPoolConfig jedisConfig) {

		Class[] parameters = method.getParameterTypes();
		if ( parameters.length != 1 ) {
			throw new RuntimeException( "the parameter for setter should be length == 1, but found "
					+ parameters.length + " on " + method.getName() );
		}

		try {
			Object targetObj = createWrapperObject( value, parameters[0] );
			method.invoke(
					jedisConfig,
					targetObj.getClass().getDeclaredMethod( parameters[0].getCanonicalName() + "Value" )
							.invoke( targetObj ) );
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}
	}

	/**
	 * Creates wrapper object based on the parameters.
	 * @param initValue Initial value to be set when the wrapper object is constructed.
	 * @param primitiveClass Class representing primitive type.
	 * @return Newly created wrapper object.
	 */
	private Object createWrapperObject(String initValue, Class primitiveClass) {

		Class wrapperClass = ClassUtils.primitiveToWrapper( primitiveClass );
		Constructor ctor = null;

		try {
			ctor = wrapperClass.getDeclaredConstructor( String.class );
			return ctor.newInstance( initValue );
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}

		return null;
	}
	
	/**
	 * Copied from AnnotationFinder. Checks if the parameter, str has the parameter, pattern.
	 * 
	 * @param str
	 *            String to be examined.
	 * @param pattern
	 *            Used to check the parameter, str.
	 * @return True if patter is included.
	 */
	private boolean hasPattern(String str, Pattern pattern) {

		Matcher matcher = pattern.matcher( str );
		while ( matcher.find() ) {
			return true;
		}

		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.hibernate.ogm.datastore.spi.DatastoreProvider#getDefaultDialect()
	 */
	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return  RedisDialect.class;
	}
	
	/**
	 * @return
	 */
	public JSONHelper getJsonHelper() {
		return jsonHelper;
	}

	/**
	 * @param key
	 * @return
	 */
	public Map<String, Object> getEntityTuple(EntityKey key) {

		boolean isNull = false;
		Response<String> value = null;
		try {
			Transaction tx = jedis.multi();
			value = tx.hget( ENTITY_HSET, jsonHelper.toJSON( getEntityKeyAsMap( key ) ) );
			tx.exec();
		}
		catch ( NullPointerException ex ) {
			isNull = true;
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}

		if ( isNull ) {
			return null;
		}
		
		return jsonHelper.convertFromJsonOn( key, (Map<String, Object>) jsonHelper.fromJSON( value.get(), Map.class ),
				getDeclaredFieldsFrom( key.getEntityName() ) );
	}
	
	/**
	 * Reused from VoldemortDatastoreProvider. Gets the declared fields from the specified class.
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
	 * @param key
	 * @param convertJsonAsNeededOn
	 */
	public void putEntity(EntityKey key, Map<String, Object> tuple) {

		RollbackAction rollbackAction = new RollbackAction( this, key );
		entityKeys.put( jsonHelper.toJSON( getEntityKeyAsMap( key ) ), key );
		
		try {
			Transaction tx = jedis.multi();
			tx.hset( ENTITY_HSET, jsonHelper.toJSON( getEntityKeyAsMap( key ) ), jsonHelper.toJSON( tuple ) );
			tx.exec();
		}
		catch ( Exception ex ) {
			rollbackAction.rollback();
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}
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
	 * @param key
	 */
	public void removeEntity(EntityKey key) {

		RollbackAction rollbackAction = new RollbackAction( this, key );
		entityKeys.remove( jsonHelper.toJSON( getEntityKeyAsMap( key ) ) );
		
		try {
			Transaction tx = jedis.multi();
			Response<Long> res = tx.hdel( ENTITY_HSET, jsonHelper.toJSON( getEntityKeyAsMap( key ) ) );
			tx.exec();
		}
		catch ( Exception ex ) {
			rollbackAction.rollback();
		}
		finally {
			pool.returnResource( jedis );
		}
	}

	/**
	 * @param key
	 * @return
	 */
	public Map<RowKey, Map<String, Object>> getAssociation(AssociationKey key) {

		Response<String> res = null;
		boolean isNull = false;
		try {
			Transaction tx = jedis.multi();
			res = tx.hget( ASSOCIATION_HSET, jsonHelper.toJSON( getAssociationKeyAsMap( key ) ) );
			tx.exec();
		}
		catch ( NullPointerException ex ) {
			isNull = true;
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}

		if ( isNull ) {
			return null;
		}

		return createAssociationFrom( res.get() );
	}

	/**
	 * Copied from VoldemortDatastoreProvider. Creates association from the specified Jsoned string.
	 * 
	 * @param jsonedAssociation
	 *            Representation of association as JSON.
	 * @return Association based on the specified string.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<RowKey, Map<String, Object>> createAssociationFrom(String jsonedAssociation) {

		Map associationMap = (Map) jsonHelper.fromJSON( jsonedAssociation, Map.class );
		Map<RowKey, Map<String, Object>> association = new HashMap<RowKey, Map<String, Object>>();
		String key = "";
		RowKey rowKey = null;
		for ( Iterator itr = associationMap.keySet().iterator(); itr.hasNext(); ) {
			key = (String) itr.next();
			rowKey = (RowKey) jsonHelper.fromJSON( key, RowKey.class );
			association.put( rowKey, jsonHelper.createAssociation( (String) associationMap.get( key ) ) );
		}

		return association;
	}
	
	/**
	 * @param key
	 * @param underlyingMap
	 */
	public void putAssociation(AssociationKey key, Map<RowKey, Map<String, Object>> associationMap) {

		RollbackAction rollbackAction = new RollbackAction( this, key );
		associationKeys.put( jsonHelper.toJSON( getAssociationKeyAsMap( key ) ), key );
		
		try {
			Transaction tx = jedis.multi();
			Response<Long> res = tx.hset( ASSOCIATION_HSET, jsonHelper.toJSON( getAssociationKeyAsMap( key ) ),
					jsonHelper.toJSON( jsonHelper.convertKeyAndValueToJsonOn( associationMap ) ) );
			tx.exec();
		}
		catch ( Exception ex ) {
			rollbackAction.rollback();
		}
		finally {
			pool.returnResource( jedis );
		}
	}

	/**
	 * @param key
	 */
	public void removeAssociation(AssociationKey key) {

		RollbackAction rollbackAction = new RollbackAction( this, key );
		associationKeys.remove( jsonHelper.toJSON( getAssociationKeyAsMap( key ) ) );

		try {
			Transaction tx = jedis.multi();
			tx.hdel( ASSOCIATION_HSET, jsonHelper.toJSON( getAssociationKeyAsMap( key ) ) );
			tx.exec();
		}
		catch ( Exception ex ) {
			rollbackAction.rollback();
		}
		finally {
			pool.returnResource( jedis );
		}
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
		map.put( "table", associationKey.getTable() );

		return Collections.unmodifiableMap( map );
	}

	/**
	 * @param key
	 * @param rowKeyAsMap
	 * @param value
	 * @param increment
	 * @param initialValue
	 */
	public void setNextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {

		Integer seq = getSequence(key);

		if ( seq == null ) {

			synchronized(this){
				value.initialize( initialValue );
			}

			Map<String, Integer> nextSequence = new HashMap<String, Integer>();
			nextSequence.put( SEQUENCE_LABEL, initialValue );
			putSequence( key, nextSequence );
		}
		else {
			int sequence = 0;

			synchronized(this){
				sequence = seq + increment;
				value.initialize( sequence );
			}

			Map<String, Integer> nextSequence = new HashMap<String, Integer>();
			nextSequence.put( SEQUENCE_LABEL, sequence );
			putSequence( key, nextSequence );
		}
	}

	/**
	 * Gets sequence.
	 * @param rowKey Used to search for the corresponding sequence.
	 * @return Sequence.
	 */
	public synchronized Integer getSequence(RowKey rowKey) {

		if ( rowKey == null ) {
			return null;
		}

		Response<String> res = null;
		boolean isNull = false;
		try {
			Transaction tx = jedis.multi();
			res = tx.hget( SEQUENCE_HSET, jsonHelper.toJSON( getRowKeyAsMap( rowKey ) ) );
			tx.exec();
		}
		catch ( NullPointerException ex ) {
			isNull = true;
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}

		if ( isNull ) {
			return null;
		}

		return (Integer) jsonHelper.get( res.get(), SEQUENCE_LABEL );
	}

	/**
	 * Puts sequence.
	 * @param key
	 * @param nextSequence
	 */
	public synchronized void putSequence(RowKey key, Map<String, Integer> nextSequence) {

		RollbackAction rollbackAction = new RollbackAction( this, key );
		
		try {
			Transaction tx = jedis.multi();
			tx.hset( SEQUENCE_HSET, jsonHelper.toJSON( getRowKeyAsMap( key ) ), jsonHelper.toJSON( nextSequence ) );
			tx.exec();
		}
		catch ( Exception ex ) {
			rollbackAction.rollback();
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}
	}
	
	/**
	 * Reused from VoldemortDialect. Gets row key as Map object containing owning columns.
	 * 
	 * @return Row key as Map representation.
	 */
	public Map<String, Object> getRowKeyAsMap(RowKey rowKey) {

		Map<String, Object> map = new HashMap<String, Object>();

		if ( rowKey.getColumnNames() != null && rowKey.getColumnValues() != null ) {
			for ( int i = 0; i < rowKey.getColumnNames().length; i++ ) {
				map.put( rowKey.getColumnNames()[i], rowKey.getColumnValues()[i] );
			}
		}
		map.put( "table", rowKey.getTable() );
		return Collections.unmodifiableMap( map );
	}

	public Map<EntityKey, Map<String, Object>> getEntityMap() {

		Map<EntityKey, Map<String, Object>> map = new HashMap<EntityKey, Map<String, Object>>();
		boolean isNull = false;
		Response<Map<String, String>> res = null;
		try {
			Transaction tx = jedis.multi();
			res = tx.hgetAll( ENTITY_HSET );
			tx.exec();

		}
		catch ( NullPointerException ex ) {
			isNull = true;
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}

		if ( isNull ) {
			return Collections.EMPTY_MAP;
		}

		Entry<String, String> entry = null;
		EntityKey entityKey = null;
		for ( Iterator<Entry<String, String>> itr = res.get().entrySet().iterator(); itr.hasNext(); ) {
			entry = itr.next();
			entityKey = entityKeys.get( entry.getKey() );
			map.put( entityKey, jsonHelper.convertFromJsonOn( entityKey,
					(Map<String, Object>) jsonHelper.fromJSON( entry.getValue(), Map.class ),
					getDeclaredFieldsFrom( entityKey.getEntityName() ) ) );
		}

		return map;
	}
	
	public Map<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationsMap() {

		Map<AssociationKey, Map<RowKey, Map<String, Object>>> associations = new HashMap<AssociationKey, Map<RowKey, Map<String, Object>>>();
		boolean isNull = false;
		Response<Map<String, String>> res = null;
		try {
			Transaction tx = jedis.multi();
			res = tx.hgetAll( ASSOCIATION_HSET );
			tx.exec();
		}
		catch ( NullPointerException ex ) {
			isNull = true;
		}
		catch ( Exception ex ) {
			throwHibernateExceptionFrom( ex );
		}
		finally {
			pool.returnResource( jedis );
		}

		if ( isNull ) {
			return Collections.EMPTY_MAP;
		}

		Entry<String, String> entry = null;
		for ( Iterator<Entry<String, String>> itr = res.get().entrySet().iterator(); itr.hasNext(); ) {
			entry = itr.next();
			associations.put( associationKeys.get( entry.getKey() ),
					createAssociationFrom( entry.getValue() ) );
		}
		return associations;
	}

	public void removeAll() {

		log.info( "about to remove all data in Redis" );
		try {
			jedis.flushDB();
		}
		finally {
			pool.returnResource( jedis );
		}
	}

	/**
	 * Converts the specified exception to HibernateException and rethrows it.
	 * 
	 * @param <T>
	 * @param exception
	 *            Exception to be rethrown as HibernateException.
	 */
	protected <T extends Throwable> void throwHibernateExceptionFrom(T exception) {
		throw new HibernateException( exception );
	}
}

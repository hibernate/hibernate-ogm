/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.common.request.impl.RemoteStatement;
import org.hibernate.ogm.datastore.neo4j.remote.common.request.impl.RemoteStatements;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;

/**
 * @author Davide D'Alto
 */
public abstract class RemoteNeo4jSequenceGenerator extends BaseNeo4jSequenceGenerator {

	/**
	 * Query for creating SEQUENCE nodes.
	 */
	protected static final String SEQUENCE_CREATION_QUERY =
			"MERGE (n:" + NodeLabel.SEQUENCE.name() + " {" + SEQUENCE_NAME_PROPERTY + ": {sequenceName}} )"
			+ " RETURN n";

	protected static final String SEQUENCE_LOCK_QUERY =
			"MATCH (n:" + NodeLabel.SEQUENCE.name() + ")"
			+ " WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} "
			+ " SET n.__locked = true "
			+ " RETURN n";

	/**
	 * Query for retrieving the next value from SEQUENCE nodes.
	 */
	protected static final String SEQUENCE_VALUE_QUERY =
			"MATCH (n:" + NodeLabel.SEQUENCE.name() + ")"
			+ " WHERE n." + SEQUENCE_NAME_PROPERTY + " = {sequenceName} "
			+ " REMOVE n.__locked "
			+ " SET n." + SEQUENCE_VALUE_PROPERTY + " = coalesce(n." + SEQUENCE_VALUE_PROPERTY + ", {initialValue}) + {increment}"
			+ " RETURN n." + SEQUENCE_VALUE_PROPERTY;

	private static final Log logger = LoggerFactory.make( MethodHandles.lookup() );

	private final BoundedConcurrentHashMap<String, RemoteStatements> queryCache;

	public RemoteNeo4jSequenceGenerator(int sequenceCacheMaxSize) {
		this.queryCache = new BoundedConcurrentHashMap<String, RemoteStatements>( sequenceCacheMaxSize, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	/**
	 * Generate the next value in a sequence for a given {@link IdSourceKey}.
	 *
	 * @return the next value in a sequence
	 */
	@Override
	public Long nextValue(NextValueRequest request) {
		String sequenceName = sequenceName( request.getKey() );
		// This method return 2 statements: the first one to acquire a lock and the second one to update the sequence node
		RemoteStatements remoteStatements = updateNextValueQuery( request );

		Number nextValue = nextValue( remoteStatements );

		// sequence nodes are expected to have been created up-front
		if ( request.getKey().getMetadata().getType() == IdSourceType.SEQUENCE ) {
			if ( nextValue == null ) {
				throw logger.sequenceNotFound( sequenceName );
			}
		}
		// The only way I found to make it work in a multi-threaded environment is to first increment the value and then read it.
		// Our API allows for an initial value and to make sure that I'm actually reading the correct one,
		// the first time I need to decrement the value I obtain from the db.
		return nextValue.longValue() - request.getIncrement();
	}

	protected abstract Number nextValue(RemoteStatements remoteStatements);

	protected String increaseQuery(NextValueRequest request) {
		StringBuilder queryBuilder = new StringBuilder();
		IdSourceKeyMetadata metadata = request.getKey().getMetadata();
		queryBuilder.append( " MATCH (n " );
		queryBuilder.append( labels( metadata.getName(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) );
		queryBuilder.append( " { " );
		queryBuilder.append( metadata.getKeyColumnName() );
		queryBuilder.append( ": {" );
		queryBuilder.append( SEQUENCE_NAME_QUERY_PARAM );
		queryBuilder.append( "}} )" );
		queryBuilder.append( " SET n." );
		queryBuilder.append( metadata.getValueColumnName() );
		queryBuilder.append( " = n." );
		queryBuilder.append( metadata.getValueColumnName() );
		queryBuilder.append( " + " );
		queryBuilder.append( request.getIncrement() );
		queryBuilder.append( " REMOVE n.__locked RETURN n." );
		queryBuilder.append( metadata.getValueColumnName() );
		String query = queryBuilder.toString();
		return query;
	}

	protected String acquireLockQuery(NextValueRequest request) {
		StringBuilder queryBuilder = new StringBuilder();
		IdSourceKeyMetadata metadata = request.getKey().getMetadata();
		queryBuilder.append( "MERGE (n" );
		queryBuilder.append( labels( metadata.getName(), NodeLabel.TABLE_BASED_SEQUENCE.name() ) );
		queryBuilder.append( " { " );
		queryBuilder.append( metadata.getKeyColumnName() );
		queryBuilder.append( ": {" );
		queryBuilder.append( SEQUENCE_NAME_QUERY_PARAM );
		queryBuilder.append( "}} ) " );
		queryBuilder.append( " ON MATCH SET n.__locked=true " );
		queryBuilder.append( " ON CREATE SET n.__locked=true, n." );
		queryBuilder.append( metadata.getValueColumnName() );
		queryBuilder.append( " = " );
		queryBuilder.append( request.getInitialValue() );
		queryBuilder.append( " RETURN n." );
		queryBuilder.append( metadata.getValueColumnName() );
		String query = queryBuilder.toString();
		return query;
	}

	/*
	 * This will always return 2 statements: the first one to acquire a lock and the second one to update the sequence value
	 */
	protected RemoteStatements updateNextValueQuery(NextValueRequest request) {
		return request.getKey().getMetadata().getType() == IdSourceType.TABLE
				? getTableQuery( request )
				: getSequenceIncrementQuery( request );
	}

	private RemoteStatements getSequenceIncrementQuery(NextValueRequest request) {
		// Acquire a lock on the node
		String sequenceName = sequenceName( request.getKey() );
		RemoteStatement lockStatement = new RemoteStatement( SEQUENCE_LOCK_QUERY, Collections.<String, Object>singletonMap( SEQUENCE_NAME_QUERY_PARAM, sequenceName ) );
		RemoteStatements statements = new RemoteStatements();
		statements.addStatement( lockStatement );

		// Increment the value on the node
		String query = SEQUENCE_VALUE_QUERY.replace( "{increment}", String.valueOf( request.getIncrement() ) ).replace( "{initialValue}", String.valueOf( request.getInitialValue() ) );
		RemoteStatement statement = new RemoteStatement( query, params( request ), true );
		statements.addStatement( statement );
		return statements;
	}

	private RemoteStatements getTableQuery(NextValueRequest request) {
		String key = key( request );
		RemoteStatements statements = queryCache.get( key );
		if ( statements == null ) {
			statements = new RemoteStatements();
			getUpdateTableSequenceQuery( statements, request );
			RemoteStatements cached = queryCache.putIfAbsent( key, statements );
			if ( cached != null ) {
				statements = cached;
			}
		}
		return statements;
	}

	private void getUpdateTableSequenceQuery(RemoteStatements statements, NextValueRequest request) {
		Map<String, Object> params = params( request );

		// Acquire lock
		String acquireLockQuery = acquireLockQuery( request );
		RemoteStatement acquireLockStatement = new RemoteStatement( acquireLockQuery, params, true );
		statements.addStatement( acquireLockStatement );

		// Update value
		String updateQuery = increaseQuery( request );
		RemoteStatement updateStatement = new RemoteStatement( updateQuery, params, true );
		statements.addStatement( updateStatement );
	}

	public abstract void createSequences(List<Sequence> sequences, Iterable<IdSourceKeyMetadata> idSourceKeyMetadata);
}

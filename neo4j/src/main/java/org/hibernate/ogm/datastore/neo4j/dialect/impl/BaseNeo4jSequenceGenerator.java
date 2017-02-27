/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;

/**
 * @author Davide D'Alto
 */
public abstract class BaseNeo4jSequenceGenerator {

	protected static final String INITIAL_VALUE_QUERY_PARAM = "initialValue";
	protected static final String SEQUENCE_NAME_QUERY_PARAM = "sequenceName";

	/**
	 * Name of the property of SEQUENCE nodes which holds the sequence name. ORM's default for emulated sequences,
	 * "sequence_name", is used.
	 */
	protected static final String SEQUENCE_NAME_PROPERTY = "sequence_name";

	/**
	 * Name of the property of SEQUENCE nodes which holds the next value. ORM's default for emulated sequences,
	 * "next_val", is used.
	 */
	protected static final String SEQUENCE_VALUE_PROPERTY = "next_val";

	protected String createUniqueConstraintQuery(String propertyName, String label) {
		StringBuilder query = new StringBuilder( "CREATE CONSTRAINT ON (n:" );
		query.append( label );
		query.append( ") ASSERT n." );
		escapeIdentifier( query, propertyName );
		query.append( " IS UNIQUE" );
		return query.toString();
	}

	protected String key(NextValueRequest request) {
		return request.getKey().getTable() + ":" + request.getInitialValue() + ":" + request.getIncrement();
	}

	protected String sequenceName(IdSourceKey key) {
		return key.getMetadata().getType() == IdSourceType.SEQUENCE ? key.getMetadata().getName() : key.getColumnValue();
	}

	protected Map<String, Object> params(NextValueRequest request) {
		return params( sequenceName( request.getKey() ), request.getInitialValue() );
	}

	protected Map<String, Object> params(Sequence sequence) {
		return params( sequence.getName().render(), sequence.getInitialValue() );
	}

	private Map<String, Object> params(String sequenceName, int initialValue) {
		Map<String, Object> params = new HashMap<String, Object>( 3 );
		params.put( INITIAL_VALUE_QUERY_PARAM, initialValue );
		params.put( SEQUENCE_NAME_QUERY_PARAM, sequenceName );
		return params;
	}

	protected String labels(String... labels) {
		StringBuilder builder = new StringBuilder();
		for ( String label : labels ) {
			builder.append( ":`" );
			builder.append( label );
			builder.append( "`" );
		}
		return builder.toString();
	}

	public abstract Long nextValue(NextValueRequest request);
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.neo4j.graphdb.Result;

/**
 * Provides common functionality required for query creation.
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
class BaseNeo4jQueries {

	protected static void appendLabel(EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		escapeIdentifier( queryBuilder, entityKeyMetadata.getTable() );
	}

	protected static void appendProperties(EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		appendProperties( queryBuilder, entityKeyMetadata.getColumnNames(), 0 );
	}

	protected static void appendProperties(StringBuilder queryBuilder, String[] columnNames, int offset) {
		if ( columnNames.length > 0 ) {
			queryBuilder.append( " {" );
			for ( int i = 0; i < columnNames.length; i++ ) {
				escapeIdentifier( queryBuilder, columnNames[i] );
				queryBuilder.append( ": {" );
				queryBuilder.append( offset + i );
				queryBuilder.append( "}" );
				if ( i < columnNames.length - 1 ) {
					queryBuilder.append( ", " );
				}
			}
			queryBuilder.append( "}" );
		}
	}

	protected Map<String, Object> params(Object[] columnValues) {
		return params( columnValues, 0 );
	}

	protected Map<String, Object> params(Object[] columnValues, int offset) {
		Map<String, Object> params = new HashMap<String, Object>( columnValues.length );
		for ( int i = 0; i < columnValues.length; i++ ) {
			params.put( String.valueOf( offset + i ), columnValues[i] );
		}
		return params;
	}

	/*
	 * Example:
	 *
	 * MATCH (owner:ENTITY:table {id: {0}})
	 */
	protected static void appendMatchOwnerEntityNode(StringBuilder queryBuilder, EntityKeyMetadata ownerEntityKeyMetadata) {
		queryBuilder.append( "MATCH " );
		appendEntityNode( "owner", ownerEntityKeyMetadata, queryBuilder );
	}

	/*
	 * Example:
	 *
	 * (owner:ENTITY:table {id: {0}})
	 */
	protected static void appendEntityNode(String alias, EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		appendEntityNode( alias, entityKeyMetadata, queryBuilder, 0 );
	}

	protected static void appendEntityNode(String alias, EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder, int offset) {
		appendEntityNode( alias, entityKeyMetadata, queryBuilder, offset, true );
	}

	protected static void appendEntityNode(String alias, EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder, int offset, boolean addProperties) {
		queryBuilder.append( "(" );
		queryBuilder.append( alias );
		queryBuilder.append( ":" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( entityKeyMetadata, queryBuilder );
		if ( addProperties ) {
			appendProperties( queryBuilder, entityKeyMetadata.getColumnNames(), offset );
		}
		queryBuilder.append( ")" );
	}

	protected static void appendRelationshipType(StringBuilder queryBuilder, String relationshipType) {
		escapeIdentifier( queryBuilder, relationshipType );
	}

	@SuppressWarnings("unchecked")
	protected <T> T singleResult(Result result) {
		try {
			if ( result.hasNext() ) {
				return (T) result.next().values().iterator().next();
			}
			return null;
		}
		finally {
			result.close();
		}
	}
}

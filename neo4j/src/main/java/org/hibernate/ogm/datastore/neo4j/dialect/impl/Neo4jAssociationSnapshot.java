/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

/**
 * Represents the association snapshot as loaded by Neo4j.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class Neo4jAssociationSnapshot implements AssociationSnapshot {

	private final Node ownerNode;
	private final AssociationKey associationKey;

	public Neo4jAssociationSnapshot(Node ownerNode, AssociationKey associationKey) {
		this.ownerNode = ownerNode;
		this.associationKey = associationKey;
	}

	@Override
	public Tuple get(RowKey rowKey) {
		for ( Relationship relationship : relationships() ) {
			if ( matches( rowKey, relationship ) ) {
				return new Tuple( new Neo4jTupleSnapshot( relationship ) );
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		for ( Relationship relationship : relationships() ) {
			if ( matches( rowKey, relationship ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean matches(RowKey key, PropertyContainer container) {
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			if ( !columnValueMatches( key.getColumnNames()[i], key.getColumnValues()[i], container ) ) {
				return false;
			}
		}

		return true;
	}

	private boolean columnValueMatches(String columnName, Object columnValue, PropertyContainer container) {
		if ( container.hasProperty( columnName ) ) {
			return container.getProperty( columnName ).equals( columnValue );
		}
		else {
			return columnValue == null;
		}
	}

	@Override
	public int size() {
		int count = 0;
		for ( Relationship relationship : relationships() ) {
			count++;
		}
		return count;
	}

	private Iterable<Relationship> relationships() {
		return ownerNode.getRelationships( Direction.OUTGOING, CypherCRUD.relationshipType( associationKey ) );
	}

	@Override
	public Set<RowKey> getRowKeys() {
		Set<RowKey> rowKeys = new HashSet<RowKey>();
		for ( Relationship relationship : relationships() ) {
			rowKeys.add( convert( relationship ) );
		}
		return rowKeys;
	}

	private RowKey convert(PropertyContainer container) {
		String[] columnNames = associationKey.getRowKeyColumnNames();
		Object[] values = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			String columnName = columnNames[i];
			if ( container.hasProperty( columnName ) ) {
				values[i] = container.getProperty( columnName );
			}
		}

		return new RowKey( associationKey.getTable(), columnNames, values );
	}
}

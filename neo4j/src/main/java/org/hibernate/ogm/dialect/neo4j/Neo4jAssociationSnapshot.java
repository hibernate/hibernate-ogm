/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.neo4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * Represents the association snapshot as loaded by Neo4j.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class Neo4jAssociationSnapshot implements AssociationSnapshot {

	private final Node ownerNode;
	private final RelationshipType relationshipType;
	private final AssociationKey associationKey;

	public Neo4jAssociationSnapshot(Node ownerNode, RelationshipType type, AssociationKey associationKey) {
		this.ownerNode = ownerNode;
		this.relationshipType = type;
		this.associationKey = associationKey;
	}

	@Override
	public Tuple get(RowKey rowKey) {
		for ( Relationship relationship : relationships() ) {
			if ( matches( rowKey, relationship ) ) {
				return new Tuple( new Neo4jTupleSnapshot( relationship.getEndNode() ) );
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		for ( Relationship relationship : relationships() ) {
			return matches( rowKey, relationship );
		}
		return false;
	}

	private boolean matches(RowKey key, PropertyContainer container) {
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			String column = key.getColumnNames()[i];
			if ( !container.hasProperty( column ) || !key.getColumnValues()[i].equals( container.getProperty( column ) ) ) {
				return false;
			}
		}
		return true;
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
		return ownerNode.getRelationships( Direction.OUTGOING, relationshipType );
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
		List<String> columns = new ArrayList<String>();
		List<Object> values = new ArrayList<Object>();
		for ( String column : columnNames ) {
			columns.add( column );
			values.add( container.getProperty( column ) );
		}
		return new RowKey( associationKey.getTable(), columns.toArray( new String[columns.size()] ),
				values.toArray( new Object[values.size()] ) );
	}

}

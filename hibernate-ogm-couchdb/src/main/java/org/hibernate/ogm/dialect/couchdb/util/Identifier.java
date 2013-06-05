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
package org.hibernate.ogm.dialect.couchdb.util;

import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;

import java.util.regex.Pattern;

/**
 * Generates the ids used to create the {@link org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBDocument}
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class Identifier {

	private static final String COLUMN_VALUES_SEPARATOR = "_";
	private static final Pattern escapingPattern = Pattern.compile( COLUMN_VALUES_SEPARATOR );

	/**
	 * Create the id used to store an {@link org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBEntity}
	 *
	 * @param key
	 *            the {@link EntityKey} used to generate the id
	 * @return the value of the generate id
	 */
	public String createEntityId(EntityKey key) {
		return key.getTable() + fromColumnValues( key.getColumnValues() );
	}

	/**
	 * Create the id used to store an {@link org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBAssociation}
	 *
	 * @param key
	 *            the{@link AssociationKey} used to generate the id
	 * @return the value of the generate id
	 */
	public String createAssociationId(AssociationKey key) {
		return key.getTable() + fromColumnValues( key.getColumnValues() );
	}

	private String fromColumnValues(Object[] columnValues) {
		String id = "";
		for ( int i = 0; i < columnValues.length; i++ ) {
			id += escapeCharsValuesUsedAsColumnValuesSeparator( columnValues[i] ) + COLUMN_VALUES_SEPARATOR;
		}
		return id;
	}

	private String escapeCharsValuesUsedAsColumnValuesSeparator(Object columnValue) {
		final String value = String.valueOf( columnValue );
		return escapingPattern.matcher( value ).replaceAll( "/_" );
	}
}

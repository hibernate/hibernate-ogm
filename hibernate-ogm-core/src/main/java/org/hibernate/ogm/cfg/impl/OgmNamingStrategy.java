/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.cfg.impl;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.util.StringHelper;

/**
 * Implements the OGM naming strategy:
 *  - based of the JPA 2 naming strategy
 *  - column names in components defaults to the fully qualified path (ie address.city)
 *
 * @author Emmanuel Bernard
 */
public class OgmNamingStrategy extends EJB3NamingStrategy {
	public static final NamingStrategy INSTANCE = new OgmNamingStrategy();

	private String addPropertySeparator(String name) {
		//the . is already present, no need to replace it
		return name; //name.replace( '.', '_' ).toLowerCase();
	}

	@Override
	public String propertyToColumnName(String propertyName) {
		return addPropertySeparator( propertyName );
	}

	@Override
	public String collectionTableName(
			String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable,
			String propertyName
	) {
		return tableName(
				new StringBuilder( ownerEntityTable ).append( "_" )
						.append(
								associatedEntityTable != null ?
										associatedEntityTable :
										addPropertySeparator( propertyName )
						).toString()
		);
	}


	public String foreignKeyColumnName(
			String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName
	) {
		String header = propertyName != null ? addPropertySeparator( propertyName ) : propertyTableName;
		if ( header == null ) throw new AssertionFailure( "NamingStrategy not properly filled" );
		return columnName( header + "_" + referencedColumnName );
	}

	@Override
	public String logicalColumnName(String columnName, String propertyName) {
		return StringHelper.isNotEmpty( columnName ) ? columnName : propertyName;
	}

	@Override
	public String logicalCollectionTableName(
			String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName
	) {
		if ( tableName != null ) {
			return tableName;
		}
		else {
			//use of a stringbuffer to workaround a JDK bug
			return new StringBuffer( ownerEntityTable ).append( "_" )
					.append(
							associatedEntityTable != null ?
									associatedEntityTable :
									propertyName
					).toString();
		}

	}

	@Override
	public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
		return StringHelper.isNotEmpty( columnName ) ?
				columnName :
				propertyName + "_" + referencedColumn;
	}

}

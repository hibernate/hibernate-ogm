/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.impl;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Implements the OGM naming strategy:
 *  - based of the JPA 2 naming strategy
 *  - column names in components defaults to the fully qualified path (ie address.city)
 *
 * @author Emmanuel Bernard
 */
public class OgmNamingStrategy extends EJB3NamingStrategy {

	public static final NamingStrategy INSTANCE = new OgmNamingStrategy();

	/**
	 * A pattern common to all property names used in element collections.
	 */
	private static final String ELEMENT_COLLECTION_NAME_PATTERN = "collection&&element";


	// noop method kept for documentation purposes
	private String replacePropertySeparator(String name) {
		//the . is already present, no need to replace it
		// Other strategies would typically replace '.' with '_'
		return name;
	}

	@Override
	public String propertyToColumnName(String propertyName) {
		// for element collections just use the simple name
		if ( propertyName.contains( ELEMENT_COLLECTION_NAME_PATTERN ) ) {
			propertyName = propertyName.substring( propertyName.lastIndexOf( "." ) + 1 );
		}

		return replacePropertySeparator( propertyName );
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
										replacePropertySeparator( propertyName )
						).toString()
		);
	}


	@Override
	public String foreignKeyColumnName(
			String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName
	) {
		String header = propertyName != null ? replacePropertySeparator( propertyName ) : propertyTableName;
		if ( header == null ) {
			throw new AssertionFailure( "NamingStrategy not properly filled" );
		}
		return columnName( header + "_" + referencedColumnName );
	}

	@Override
	public String logicalColumnName(String columnName, String propertyName) {
		return StringHelper.isEmpty( columnName ) == false ? columnName : propertyName;
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
		return StringHelper.isEmpty( columnName ) == false ?
				columnName :
				propertyName + "_" + referencedColumn;
	}

}

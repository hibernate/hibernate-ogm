/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

/**
 * The complete identifier of a property in the form alias.propertyName.
 *
 * @author Guillaume Smet
 */
public class PropertyIdentifier {

	private final String alias;
	private final String propertyName;

	public PropertyIdentifier(String alias, String propertyName) {
		this.alias = alias;
		this.propertyName = propertyName;
	}

	public String getAlias() {
		return alias;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "PropertyIdentifier [alias=" );
		sb.append( alias );
		sb.append( ", propertyName=" );
		sb.append( propertyName );
		sb.append( "]" );
		return sb.toString();
	}

}

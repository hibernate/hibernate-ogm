/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

/**
 * @author Victor Kadachigov
 */
public class PropertyIdentifier {

	public static final ParameterPlace PARAM_INSTANCE = new ParameterPlace();

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

	public static class ParameterPlace {
		@Override
		public String toString() {
			return "?";
		}
	}
}

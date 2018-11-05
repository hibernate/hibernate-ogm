/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

/**
 * Represents a single order by item of a given {@link javax.persistence.OrderBy}.
 * Expressed in term of datastore column name and order.
 *
 * @author Fabio Massimo Ercoli
 */
public class AssociationOrderBy {

	public enum Order {
		ASC, DESC
	}

	private final String columnName;
	private final Order kind;

	public AssociationOrderBy(String expression, String order) {
		// extract column name from {} expression, such as "{columnName}"
		columnName = expression.substring( 1, expression.length() - 1 );
		kind = "desc".equalsIgnoreCase( order ) ? Order.DESC : Order.ASC;
	}

	public String getColumnName() {
		return columnName;
	}

	public Order getKind() {
		return kind;
	}
}

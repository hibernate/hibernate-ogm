/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

/**
 * Operation applied to the Tuple.
 * A column name is provided and when it makes sense a column value
 * (eg DELETE or PUT_NULL do not have column value)
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TupleOperation {
	private final String column;
	private final Object value;
	private final TupleOperationType type;

	public TupleOperation(String column, Object value, TupleOperationType type) {
		this.column = column;
		this.value = value;
		this.type = type;
	}

	public String getColumn() {
		return column;
	}

	public Object getValue() {
		return value;
	}

	public TupleOperationType getType() {
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "TupleOperation" );
		sb.append( "{type=\'" ).append( type ).append( '\'' );
		sb.append( ", column='" ).append( column ).append( '\'' );
		sb.append( ", value=\'" ).append( value ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}

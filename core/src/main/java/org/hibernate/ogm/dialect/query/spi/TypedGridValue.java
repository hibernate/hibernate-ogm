/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;

/**
 * Represents a value and its grid type.the selection criteria of a query. Modelled after {@code TypedValue} in
 * Hibernate ORM.
 *
 * @author Gunnar Morling
 */
public class TypedGridValue {

	private final GridType type;
	private final Object value;

	public TypedGridValue(GridType type, Object value) {
		this.type = type;
		this.value = value;
	}

	public static TypedGridValue fromOrmTypedValue(TypedValue typedValue, TypeTranslator typeTranslator) {
		GridType gridType = typeTranslator.getType( typedValue.getType() );
		return new TypedGridValue( gridType, typedValue.getValue() );
	}

	public GridType getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
}

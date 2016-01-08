/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.engine.spi.TypedValue;
import org.hibernate.ogm.type.spi.TypeTranslator;

/**
 * Represents the parameters passed to a query. Modelled after {@code QueryParameters} in Hibernate ORM, providing only
 * those values needed for OGM's purposes at this point.
 *
 * @author Gunnar Morling
 */
public class QueryParameters {

	private final RowSelection rowSelection;
	private final Map<String, TypedGridValue> namedParameters;

	public QueryParameters(RowSelection rowSelection, Map<String, TypedGridValue> namedParameters) {
		this.rowSelection = rowSelection;
		this.namedParameters = namedParameters;
	}

	public static QueryParameters fromOrmQueryParameters(org.hibernate.engine.spi.QueryParameters parameters, TypeTranslator typeTranslator) {
		RowSelection selection = RowSelection.fromOrmRowSelection( parameters.getRowSelection() );
		Map<String, TypedGridValue> namedParameters = new HashMap<>();

		for ( Entry<String, TypedValue> parameter : parameters.getNamedParameters().entrySet() ) {
			namedParameters.put( parameter.getKey(), TypedGridValue.fromOrmTypedValue( parameter.getValue(), typeTranslator ) );
		}

		return new QueryParameters( selection, namedParameters );
	}

	public RowSelection getRowSelection() {
		return rowSelection;
	}

	public Map<String, TypedGridValue> getNamedParameters() {
		return namedParameters;
	}
}

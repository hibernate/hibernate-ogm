/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.engine.spi.SessionFactoryImplementor;
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
	private final List<TypedGridValue> positionalParameters;
	private final List<String> queryHints;

	public QueryParameters(RowSelection rowSelection, Map<String, TypedGridValue> namedParameters, List<TypedGridValue> positionalParameters, List<String> queryHints) {
		this.rowSelection = rowSelection;
		this.namedParameters = namedParameters;
		this.positionalParameters = positionalParameters;
		this.queryHints = queryHints;
	}

	public static QueryParameters fromOrmQueryParameters(org.hibernate.engine.spi.QueryParameters parameters, TypeTranslator typeTranslator, SessionFactoryImplementor sessionFactoryImplementor) {
		RowSelection selection = RowSelection.fromOrmRowSelection( parameters.getRowSelection() );
		Map<String, TypedGridValue> namedParameters = createNamedParameters( sessionFactoryImplementor, parameters, typeTranslator );
		List<TypedGridValue> positionalParameters = createPositionalParameters( parameters, typeTranslator );
		return new QueryParameters( selection, namedParameters, positionalParameters, parameters.getQueryHints() );
	}

	private static List<TypedGridValue> createPositionalParameters(org.hibernate.engine.spi.QueryParameters parameters, TypeTranslator typeTranslator) {
		List<TypedGridValue> positionalParameters = new ArrayList<>( parameters.getPositionalParameterTypes().length );
		for ( int i = 0; i < parameters.getPositionalParameterTypes().length; i++ ) {
			positionalParameters.add(
					new TypedGridValue(
							typeTranslator.getType( parameters.getPositionalParameterTypes()[i] ),
							parameters.getPositionalParameterValues()[i]
					)
			);
		}
		return positionalParameters;
	}

	private static Map<String, TypedGridValue> createNamedParameters(SessionFactoryImplementor factory, org.hibernate.engine.spi.QueryParameters parameters, TypeTranslator typeTranslator) {
		Map<String, TypedGridValue> namedParameters = new HashMap<>();
		for ( Entry<String, TypedValue> parameter : parameters.getNamedParameters().entrySet() ) {
			TypedGridValue typedGridValue = TypedGridValue.fromOrmTypedValue( parameter.getValue(), typeTranslator, factory );
			namedParameters.put( parameter.getKey(), typedGridValue );
		}
		return namedParameters;
	}

	public RowSelection getRowSelection() {
		return rowSelection;
	}

	public Map<String, TypedGridValue> getNamedParameters() {
		return namedParameters;
	}

	public List<TypedGridValue> getPositionalParameters() {
		return positionalParameters;
	}

	public List<String> getQueryHints() {
		return queryHints;
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteQueryDescriptor;
import org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl.IgnitePredicateFactory;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

/**
 * Parser delegate which creates Ignite SQL queries in form of {@link StringBuilder}s.
 *
 * @author Victor Kadachigov
 */
public class IgniteQueryRendererDelegate extends SingleEntityQueryRendererDelegate<StringBuilder, IgniteQueryParsingResult> {

	private static final List<String> ENTITY_COLUMN_NAMES = Collections.unmodifiableList( Arrays.asList( "_KEY", "_VALUE" ) );

	private final IgnitePropertyHelper propertyHelper;
	private final SessionFactoryImplementor sessionFactory;
	private final Map<String, Object> namedParamsWithValues;
	private List<Object> indexedParameters;
	private List<OrderByClause> orderByExpressions;

	public IgniteQueryRendererDelegate(SessionFactoryImplementor sessionFactory, IgnitePropertyHelper propertyHelper, EntityNamesResolver entityNamesResolver, Map<String, Object> namedParameters) {
		super(
				propertyHelper,
				entityNamesResolver,
				SingleEntityQueryBuilder.getInstance( new IgnitePredicateFactory( propertyHelper ), propertyHelper ),
				namedParameters != null ? NamedParametersMap.INSTANCE : null /* we put '?' in query instead of parameter value */
		);
		this.propertyHelper = propertyHelper;
		this.sessionFactory = sessionFactory;
		this.namedParamsWithValues = namedParameters;
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		this.propertyPath = propertyPath;
	}

	private void where( StringBuilder queryBuilder ) {
		StringBuilder where = builder.build();
		if ( where != null && where.length() > 0 ) {
			queryBuilder.append( " WHERE " ).append( where );
		}
	}

	private void orderBy(StringBuilder queryBuilder) {
		if ( orderByExpressions != null && !orderByExpressions.isEmpty() ) {
			queryBuilder.append( " ORDER BY " );
			int counter = 1;
			for ( OrderByClause orderBy : orderByExpressions ) {
				orderBy.asString( queryBuilder );
				if ( counter++ < orderByExpressions.size() ) {
					queryBuilder.append( ", " );
				}
			}
		}
	}

	private void select(StringBuilder queryBuilder) {
		queryBuilder.append( "SELECT _KEY, _VAL " );
	}

	private void from(StringBuilder queryBuilder) {
		String tableAlias = propertyHelper.findAliasForType( targetTypeName );
		OgmEntityPersister persister = (OgmEntityPersister) ( sessionFactory ).getEntityPersister( targetType.getName() );
		String tableName = propertyHelper.getKeyMetaData( targetType.getName() ).getTable();
		queryBuilder.append( " FROM " ).append( tableName ).append( ' ' ).append( tableAlias ).append( ' ' );
	}

	@Override
	public IgniteQueryParsingResult getResult() {
		StringBuilder queryBuilder = new StringBuilder();
		select( queryBuilder );
		from( queryBuilder );
		where( queryBuilder );
		orderBy( queryBuilder );

		boolean hasScalar = false; // no projections for now
		IgniteQueryDescriptor queryDescriptor = new IgniteQueryDescriptor( queryBuilder.toString(), indexedParameters, hasScalar );

		return new IgniteQueryParsingResult( queryDescriptor, ENTITY_COLUMN_NAMES );
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		if ( orderByExpressions == null ) {
			orderByExpressions = new ArrayList<OrderByClause>();
		}

		List<String> propertyPathWithoutAlias = resolveAlias( propertyPath );
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( targetTypeName, propertyPathWithoutAlias );

		OrderByClause order = new OrderByClause( identifier.getAlias(), identifier.getPropertyName(), isAscending );
		orderByExpressions.add( order );
	}

	private void fillIndexedParams(String param) {
		if ( param.startsWith( ":" ) ) {
			if ( indexedParameters == null ) {
				indexedParameters = new ArrayList<>();
			}
			Object paramValue = namedParamsWithValues.get( param.substring( 1 ) );
			if ( paramValue != null && paramValue.getClass().isEnum() ) {
				//vk: for now I work only with @Enumerated(EnumType.ORDINAL) field params
				//    How to determite corresponding field to this param and check it's annotation?
				paramValue = ( (Enum) paramValue ).ordinal();
			}
			indexedParameters.add( paramValue );
		}
	}

	@Override
	public void predicateLess(String comparativePredicate) {
		fillIndexedParams( comparativePredicate );
		super.predicateLess( comparativePredicate );
	}

	@Override
	public void predicateLessOrEqual(String comparativePredicate) {
		fillIndexedParams( comparativePredicate );
		super.predicateLessOrEqual( comparativePredicate );
	}

	@Override
	public void predicateEquals(final String comparativePredicate) {
		fillIndexedParams( comparativePredicate );
		super.predicateEquals( comparativePredicate );
	}

	@Override
	public void predicateNotEquals(String comparativePredicate) {
		fillIndexedParams( comparativePredicate );
		super.predicateNotEquals( comparativePredicate );
	}

	@Override
	public void predicateGreaterOrEqual(String comparativePredicate) {
		fillIndexedParams( comparativePredicate );
		super.predicateGreaterOrEqual( comparativePredicate );
	}

	@Override
	public void predicateGreater(String comparativePredicate) {
		fillIndexedParams( comparativePredicate );
		super.predicateGreater( comparativePredicate );
	}

	@Override
	public void predicateBetween(String lower, String upper) {
		fillIndexedParams( lower );
		fillIndexedParams( upper );
		super.predicateBetween( lower, upper );
	}

	@Override
	public void predicateLike(String patternValue, Character escapeCharacter) {
		fillIndexedParams( patternValue );
		super.predicateLike( patternValue, escapeCharacter );
	}

	@Override
	public void predicateIn(List<String> list) {
		for ( String s : list ) {
			fillIndexedParams( s );
		}
		super.predicateIn( list );
	}

	private static class NamedParametersMap implements Map<String, Object> {

		public static final NamedParametersMap INSTANCE = new NamedParametersMap();

		@Override
		public int size() {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public boolean isEmpty() {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public boolean containsKey(Object key) {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public boolean containsValue(Object value) {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public Object get(Object key) {
			return PropertyIdentifier.PARAM_INSTANCE;
		}
		@Override
		public Object put(String key, Object value) {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public Object remove(Object key) {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public void putAll(Map<? extends String, ? extends Object> m) {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public void clear() {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public Set<String> keySet() {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public Collection<Object> values() {
			throw new UnsupportedOperationException( "Not supported" );
		}
		@Override
		public Set<java.util.Map.Entry<String, Object>> entrySet() {
			throw new UnsupportedOperationException( "Not supported" );
		}
	}

}

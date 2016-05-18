/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.as;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.node;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate.Type;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl.Neo4jPredicateFactory;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

/**
 * Parser delegate which creates Neo4j queries in form of {@link StringBuilder}s.
 *
 * @author Davide D'Alto
 */
public class Neo4jQueryRendererDelegate extends SingleEntityQueryRendererDelegate<StringBuilder, Neo4jQueryParsingResult> {

	private final Neo4jPropertyHelper propertyHelper;
	private final SessionFactoryImplementor sessionFactory;
	private List<OrderByClause> orderByExpressions;
	private final List<String> embeddedPropertyProjection;
	private final AliasResolver embeddedAliasResolver;

	public Neo4jQueryRendererDelegate(SessionFactoryImplementor sessionFactory, AliasResolver embeddedAliasResolver, EntityNamesResolver entityNames, Neo4jPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super( propertyHelper, entityNames, singleEntityQueryBuilder( propertyHelper, embeddedAliasResolver ), namedParameters );
		this.sessionFactory = sessionFactory;
		this.embeddedAliasResolver = embeddedAliasResolver;
		this.propertyHelper = propertyHelper;
		this.embeddedPropertyProjection = new ArrayList<String>();
	}

	private static SingleEntityQueryBuilder<StringBuilder> singleEntityQueryBuilder(Neo4jPropertyHelper propertyHelper, AliasResolver embeddedAliasResolver) {
		return SingleEntityQueryBuilder.getInstance( new Neo4jPredicateFactory( propertyHelper, embeddedAliasResolver ), propertyHelper );
	}

	private EntityKeyMetadata getKeyMetaData(Class<?> entityType) {
		OgmEntityPersister persister = (OgmEntityPersister) ( sessionFactory ).getEntityPersister( entityType.getName() );
		return persister.getEntityKeyMetadata();
	}

	@Override
	public Neo4jQueryParsingResult getResult() {
		String targetAlias = embeddedAliasResolver.findAliasForType( targetTypeName );
		String label = getKeyMetaData( targetType ).getTable();
		StringBuilder queryBuilder = new StringBuilder();
		match( queryBuilder, targetAlias, label );
		where( queryBuilder );
		optionalMatch( queryBuilder, targetAlias );
		returns( queryBuilder, targetAlias );
		orderBy( queryBuilder );
		return new Neo4jQueryParsingResult( targetType, projections, queryBuilder.toString() );
	}

	private void match(StringBuilder queryBuilder, String targetAlias, String label) {
		queryBuilder.append( "MATCH " );
		node( queryBuilder, targetAlias, label );
		EmbeddedAliasTree node = embeddedAliasResolver.getAliasTree( targetAlias );
		if ( node != null ) {
			boolean first = true;
			for ( EmbeddedAliasTree child : node.getChildren() ) {
				if ( !embeddedAliasResolver.isOptionalMatch( child.getAlias() ) ) {
					StringBuilder embeddedMatch = new StringBuilder();
					if ( first ) {
						first = false;
					}
					else {
						embeddedMatch.append( ", " );
						node( embeddedMatch, targetAlias, label );
					}
					relationship( embeddedMatch, child.getName() );
					node( embeddedMatch, child.getAlias(), NodeLabel.EMBEDDED.name() );
					appendEmbedded( queryBuilder, embeddedMatch.toString(), child, true );
				}
			}
		}
	}

	private void optionalMatch(StringBuilder queryBuilder, String targetAlias) {
		EmbeddedAliasTree node = embeddedAliasResolver.getAliasTree( targetAlias );
		if ( node != null ) {
			appendOptionalMatch( queryBuilder, targetAlias, node.getChildren() );
		}
	}

	private void appendOptionalMatch(StringBuilder queryBuilder, String targetAlias, List<EmbeddedAliasTree> children) {
		for ( EmbeddedAliasTree child : children ) {
			if ( embeddedAliasResolver.isOptionalMatch( child.getAlias() ) ) {
				queryBuilder.append( " OPTIONAL MATCH " );
				node( queryBuilder, targetAlias );
				relationship( queryBuilder, child.getName() );
				node( queryBuilder, child.getAlias(), NodeLabel.EMBEDDED.name() );
			}
			appendOptionalMatch( queryBuilder, child.getAlias(), child.getChildren() );
		}
	}

	private void appendEmbedded(StringBuilder queryBuilder, String currentMatch, EmbeddedAliasTree node, boolean matchCondition) {
		if ( node.getChildren().isEmpty() ) {
			queryBuilder.append( currentMatch );
		}
		else {
			for ( EmbeddedAliasTree child : node.getChildren() ) {
				boolean optional = embeddedAliasResolver.isOptionalMatch( child.getAlias() );
				if ( (matchCondition && !optional) || !matchCondition) {
					StringBuilder builder = new StringBuilder( currentMatch );
					relationship( builder, child.getName() );
					node( builder, child.getAlias(), NodeLabel.EMBEDDED.name() );
					appendEmbedded( queryBuilder, builder.toString(), child, matchCondition );
				}
				else {
					queryBuilder.append( currentMatch );
				}
			}
		}
	}

	private void where(StringBuilder queryBuilder) {
		StringBuilder whereCondition = builder.build();
		if ( whereCondition != null ) {
			queryBuilder.append( " WHERE " );
			queryBuilder.append( whereCondition );
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

	private void returns(StringBuilder builder, String targetAlias) {
		builder.append( " RETURN " );
		if ( projections.isEmpty() ) {
			identifier( builder, targetAlias );
		}
		else {
			int counter = 1;
			for ( String projection : projections ) {
				if ( embeddedPropertyProjection.contains( projection ) ) {
					builder.append( projection );
				}
				else {
					identifier( builder, targetAlias, projection );
					as( builder, projection );
				}
				if ( counter++ < projections.size() ) {
					builder.append( ", " );
				}
			}
		}
	}

	@Override
	public void setPropertyPath(PropertyPath path) {
		if ( status == Status.DEFINING_SELECT ) {
			defineSelect( path );
		}
		else {
			this.propertyPath = path;
		}
	}

	private void defineSelect(PropertyPath path) {
		List<String> pathWithoutAlias = resolveAlias( path );
		if ( !pathWithoutAlias.isEmpty() ) { // It might be empty if we have selected the target entity
			if ( propertyHelper.isSimpleProperty( pathWithoutAlias ) ) {
				defineSelectForSimpleProperty( pathWithoutAlias );
			}
			else if ( propertyHelper.isEmbeddedProperty( targetTypeName, pathWithoutAlias ) ) {
				defineSelectForEmbeddedProperty( entityAlias( path ), pathWithoutAlias );
			}
			else {
				throw new UnsupportedOperationException( "Selecting associated properties not yet implemented." );
			}
		}
	}

	private void defineSelectForSimpleProperty(List<String> pathWithoutAlias) {
		projections.add( propertyHelper.getColumnName( targetTypeName, pathWithoutAlias ) );
	}

	private void defineSelectForEmbeddedProperty(String entityAlias, List<String> pathWithoutAlias) {
		if ( propertyHelper.isIdProperty( targetTypeName, pathWithoutAlias ) ) {
			// Id properties get stored like simple properties
			defineSelectForSimpleProperty( pathWithoutAlias );
		}
		else {
			final List<String> associationPath = propertyHelper.findAssociationPath( targetTypeName, pathWithoutAlias );
			// Currently, it is possible to nest only one association inside an embedded
			if ( associationPath != null ) {
				List<String> nextPath = new ArrayList<String>( associationPath.size() + 1 );
				int next = associationPath.size();
				nextPath.addAll( associationPath );
				if ( next < pathWithoutAlias.size() - 1 ) {
					nextPath.add( pathWithoutAlias.get( next ) );
				}
				boolean leftJoin = false; // TODO: Not sure how to obtain this information at the moment
				embeddedAliasResolver.createAliasForEmbedded( entityAlias, nextPath, leftJoin );
			}
			boolean optional = true;
			String embeddedAlias = embeddedAliasResolver.createAliasForEmbedded( entityAlias, pathWithoutAlias, optional );
			String columnName = propertyHelper.getColumnName( targetTypeName, pathWithoutAlias );
			String projection = identifier( embeddedAlias, columnName );
			projections.add( projection );
			embeddedPropertyProjection.add( projection );
		}
	}

	/**
	 * The entity containing the selected property path.
	 * Note that the path MUST start with an alias.
	 *
	 * @param path the path to the property
	 * @return the alias of the entity dealing with the property
	 * @throws an exception if the path does not start with an alias
	 */
	private String entityAlias(PropertyPath path) {
		if ( path.getFirstNode().isAlias() ) {
			String alias = path.getFirstNode().getName();
			if ( aliasToEntityType.containsKey( alias ) ) {
				return alias;
			}
			else if ( aliasToPropertyPath.containsKey( alias ) ) {
				PropertyPath resolvedAlias = aliasToPropertyPath.get( alias );
				if ( resolvedAlias.getFirstNode().isAlias() ) {
					return resolvedAlias.getFirstNode().getName();
				}
				throw new HibernateException( "It does not start with an alias" );
			}
		}
		throw new HibernateException( "It does not start with an alias" );
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		if ( orderByExpressions == null ) {
			orderByExpressions = new ArrayList<OrderByClause>();
		}
		String columnName = propertyHelper.getColumnName( targetType, propertyPath.getNodeNamesWithoutAlias() );
		String alias = embeddedAliasResolver.findAliasForType( targetTypeName );

		OrderByClause order = new OrderByClause( alias, columnName, isAscending );
		orderByExpressions.add( order );
	}


	// TODO Methods below were not required here if fromNamedQuery() could be overridden from super

	@Override
	public void predicateLess(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.LESS );
	}

	@Override
	public void predicateLessOrEqual(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.LESS_OR_EQUAL );
	}

	/**
	 * This implements the equality predicate; the comparison
	 * predicate could be a constant, a subfunction or
	 * some random type parameter.
	 * The tree node has all details but with current tree rendering
	 * it just passes it's text so we have to figure out the options again.
	 */
	@Override
	public void predicateEquals(final String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.EQUALS );
	}

	@Override
	public void predicateNotEquals(String comparativePredicate) {
		builder.pushNotPredicate();
		addComparisonPredicate( comparativePredicate, Type.EQUALS );
		builder.popBooleanPredicate();
	}

	@Override
	public void predicateGreaterOrEqual(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.GREATER_OR_EQUAL );
	}

	@Override
	public void predicateGreater(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, Type.GREATER );
	}

	private void addComparisonPredicate(String comparativePredicate, Type comparisonType) {
		Object comparisonValue = fromNamedQuery( comparativePredicate );
		List<String> property = resolveAlias( propertyPath );
		builder.addComparisonPredicate( property, comparisonType, comparisonValue );
	}

	@Override
	public void predicateIn(List<String> list) {
		List<Object> values = fromNamedQuery( list );
		List<String> property = resolveAlias( propertyPath );
		builder.addInPredicate( property, values );
	}

	@Override
	public void predicateBetween(String lower, String upper) {
		Object lowerComparisonValue = fromNamedQuery( lower );
		Object upperComparisonValue = fromNamedQuery( upper );

		List<String> property = resolveAlias( propertyPath );
		builder.addRangePredicate( property, lowerComparisonValue, upperComparisonValue );
	}

	@Override
	public void predicateLike(String patternValue, Character escapeCharacter) {
		Object pattern = fromNamedQuery( patternValue );
		List<String> property = resolveAlias( propertyPath );
		builder.addLikePredicate( property, (String) pattern, escapeCharacter );
	}

	@Override
	public void predicateIsNull() {
		List<String> property = resolveAlias( propertyPath );
		builder.addIsNullPredicate( property );
	}

	private Object fromNamedQuery(String comparativePredicate) {
		// It's a named parameter; Value given via setParameter(), taking that as is
		if ( comparativePredicate.startsWith( ":" ) ) {
			return new Neo4jQueryParameter( comparativePredicate.substring( 1 ) );
		}
		// It's a value given in JP-QL; Convert the literal value
		else {
			List<String> path = new ArrayList<String>();
			path.addAll( propertyPath.getNodeNamesWithoutAlias() );

			PropertyPath fullPath = propertyPath;

			// create the complete path in case it's a join
			while ( fullPath.getFirstNode().isAlias() && aliasToPropertyPath.containsKey( fullPath.getFirstNode().getName() ) ) {
				fullPath = aliasToPropertyPath.get( fullPath.getFirstNode().getName() );
				path.addAll( 0, fullPath.getNodeNamesWithoutAlias() );
			}

			return propertyHelper.convertToPropertyType( targetTypeName, path, comparativePredicate );
		}
	}

	private List<Object> fromNamedQuery(List<String> list) {
		List<Object> elements = new ArrayList<Object>( list.size() );

		for ( String string : list ) {
			elements.add( fromNamedQuery( string ) );
		}

		return elements;
	}
}

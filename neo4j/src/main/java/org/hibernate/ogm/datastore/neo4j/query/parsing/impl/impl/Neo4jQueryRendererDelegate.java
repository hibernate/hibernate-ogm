/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.impl;

import static org.neo4j.cypherdsl.CypherQuery.as;
import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.node;
import static org.neo4j.cypherdsl.CypherQuery.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl.Neo4jPredicateFactory;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.neo4j.cypherdsl.CypherQuery;
import org.neo4j.cypherdsl.Order;
import org.neo4j.cypherdsl.expression.BooleanExpression;
import org.neo4j.cypherdsl.expression.Expression;
import org.neo4j.cypherdsl.expression.PathExpression;
import org.neo4j.cypherdsl.query.OrderByExpression;
import org.neo4j.cypherdsl.query.Query;
import org.neo4j.cypherdsl.query.clause.MatchClause;
import org.neo4j.cypherdsl.query.clause.OrderByClause;
import org.neo4j.cypherdsl.query.clause.ReturnClause;
import org.neo4j.cypherdsl.query.clause.WhereClause;

/**
 * Parser delegate which creates Neo4j queries in form of {@link Expression}s.
 *
 * @author Davide D'Alto
 */
public class Neo4jQueryRendererDelegate extends SingleEntityQueryRendererDelegate<BooleanExpression, Neo4jQueryParsingResult> {

	private final Neo4jPropertyHelper propertyHelper;
	private final Neo4jQueryResolverDelegate resolverDelegate;
	private final SessionFactoryImplementor sessionFactory;
	private List<Expression> orderByExpressions;

	public Neo4jQueryRendererDelegate(SessionFactoryImplementor sessionFactory, Neo4jQueryResolverDelegate resolverDelegate, EntityNamesResolver entityNames,
			Neo4jPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super( entityNames, singleEntityQueryBuilder( propertyHelper, resolverDelegate ), namedParameters );
		this.sessionFactory = sessionFactory;
		this.resolverDelegate = resolverDelegate;
		this.propertyHelper = propertyHelper;
	}

	private static SingleEntityQueryBuilder<BooleanExpression> singleEntityQueryBuilder(Neo4jPropertyHelper propertyHelper,
			Neo4jQueryResolverDelegate resolverDelegate) {
		return SingleEntityQueryBuilder.getInstance( new Neo4jPredicateFactory( propertyHelper, resolverDelegate ), propertyHelper );
	}

	private EntityKeyMetadata getKeyMetaData(Class<?> entityType) {
		OgmEntityPersister persister = (OgmEntityPersister) ( sessionFactory ).getEntityPersister( entityType.getName() );
		return new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
	}

	@Override
	public Neo4jQueryParsingResult getResult() {
		String targetAlias = resolverDelegate.findAliasForType( targetTypeName );
		String label = getKeyMetaData( targetType ).getTable();
		PathExpression matchExpression = node( new LabelValue( targetAlias, label ) );
		BooleanExpression whereExpression = (BooleanExpression) builder.build();
		Expression[] returnExpression = returnExpression( targetAlias );
		Query cypherQuery = createCypherQuery( matchExpression, whereExpression, returnExpression, orderByExpressions );
		return new Neo4jQueryParsingResult( targetType, projections, cypherQuery );
	}

	private Query createCypherQuery(PathExpression matchExpression, BooleanExpression whereExpression, Expression[] projectionsExpression, List<Expression> orderByExpressions) {
		Query cypherQuery = new Query();
		cypherQuery.add( new MatchClause( Arrays.asList( matchExpression ) ) );
		if ( whereExpression != null ) {
			cypherQuery.add( new WhereClause( whereExpression ) );
		}
		cypherQuery.add( new ReturnClause( Arrays.asList( projectionsExpression ) ) );
		if ( orderByExpressions != null ) {
			cypherQuery.add( new OrderByClause( orderByExpressions ) );
		}
		return cypherQuery;
	}

	private Expression[] returnExpression(String targetAlias) {
		if ( projections.isEmpty() ) {
			return new Expression[] { identifier( targetAlias ) };
		}
		else {
			Expression[] projectionsExpression = new Expression[projections.size()];
			int i = 0;
			for ( String projection : projections ) {
				projectionsExpression[i++] = as( identifier( targetAlias ).property( projection ), projection );
			}
			return projectionsExpression;
		}
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		if ( status == Status.DEFINING_SELECT ) {
			// currently only support selecting non-nested properties (either qualified or unqualified)
			if ( ( propertyPath.getNodes().size() == 1 && !propertyPath.getLastNode().isAlias() )
					|| ( propertyPath.getNodes().size() == 2 && propertyPath.getNodes().get( 0 ).isAlias() ) ) {
				projections.add( propertyHelper.getColumnName( targetTypeName, propertyPath.asStringPathWithoutAlias() ) );
			}
			else if ( propertyPath.getNodes().size() != 1 ) {
				throw new UnsupportedOperationException( "Selecting nested/associated properties not yet implemented." );
			}
		}
		else {
			this.propertyPath = propertyPath;
		}
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		if ( orderByExpressions == null ) {
			orderByExpressions = new ArrayList<Expression>();
		}
		String columnName = propertyHelper.getColumnName( targetType, propertyPath.asStringPathWithoutAlias() );
		String alias = resolverDelegate.findAliasForType( targetTypeName );

		OrderByExpression order = order( CypherQuery.identifier( alias ).property( columnName ), isAscending ? Order.ASCENDING : Order.DESCENDING );
		orderByExpressions.add( order );
	}

}

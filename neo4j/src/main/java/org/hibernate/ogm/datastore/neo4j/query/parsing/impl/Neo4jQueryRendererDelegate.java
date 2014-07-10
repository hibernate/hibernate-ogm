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

import java.util.ArrayList;
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

/**
 * Parser delegate which creates Neo4j queries in form of {@link StringBuilder}s.
 *
 * @author Davide D'Alto
 */
public class Neo4jQueryRendererDelegate extends SingleEntityQueryRendererDelegate<StringBuilder, Neo4jQueryParsingResult> {

	private final Neo4jPropertyHelper propertyHelper;
	private final Neo4jQueryResolverDelegate resolverDelegate;
	private final SessionFactoryImplementor sessionFactory;
	private List<OrderByClause> orderByExpressions;

	public Neo4jQueryRendererDelegate(SessionFactoryImplementor sessionFactory, Neo4jQueryResolverDelegate resolverDelegate, EntityNamesResolver entityNames,
			Neo4jPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super( entityNames, singleEntityQueryBuilder( propertyHelper, resolverDelegate ), namedParameters );
		this.sessionFactory = sessionFactory;
		this.resolverDelegate = resolverDelegate;
		this.propertyHelper = propertyHelper;
	}

	private static SingleEntityQueryBuilder<StringBuilder> singleEntityQueryBuilder(Neo4jPropertyHelper propertyHelper,
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
		StringBuilder queryBuilder = new StringBuilder();
		match( queryBuilder, targetAlias, label );
		where( queryBuilder );
		returns( queryBuilder, targetAlias );
		orderBy( queryBuilder );
		return new Neo4jQueryParsingResult( targetType, projections, queryBuilder.toString() );
	}

	private void match(StringBuilder queryBuilder, String targetAlias, String label) {
		queryBuilder.append( "MATCH " );
		node( queryBuilder, targetAlias, label );
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
				identifier( builder, targetAlias, projection );
				as( builder, projection );
				if ( counter++ < projections.size() ) {
					builder.append( ", " );
				}
			}
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
			orderByExpressions = new ArrayList<OrderByClause>();
		}
		String columnName = propertyHelper.getColumnName( targetType, propertyPath.asStringPathWithoutAlias() );
		String alias = resolverDelegate.findAliasForType( targetTypeName );

		OrderByClause order = new OrderByClause( alias, columnName, isAscending );
		orderByExpressions.add( order );
	}

}

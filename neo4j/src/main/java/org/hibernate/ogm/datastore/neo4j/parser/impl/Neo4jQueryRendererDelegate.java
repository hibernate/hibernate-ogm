/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.neo4j.parser.impl;

import static org.neo4j.cypherdsl.CypherQuery.as;
import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.match;
import static org.neo4j.cypherdsl.CypherQuery.node;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.ogm.datastore.neo4j.parser.predicate.impl.Neo4jPredicateFactory;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.neo4j.cypherdsl.expression.BooleanExpression;
import org.neo4j.cypherdsl.expression.Expression;
import org.neo4j.cypherdsl.expression.PathExpression;
import org.neo4j.cypherdsl.query.Query;
import org.neo4j.cypherdsl.query.clause.MatchClause;
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
		Expression[] projectionsExpression = projectionsExpression( targetAlias );
		Object query = null;
		if ( whereExpression == null ) {
			query = match( matchExpression ).returns( projectionsExpression );
		}
		else {
			Query cypherQuery = new Query();
			cypherQuery.add( new MatchClause( Arrays.asList( matchExpression ) ) );
			cypherQuery.add( new WhereClause( whereExpression ) );
			cypherQuery.add( new ReturnClause( Arrays.asList( projectionsExpression ) ) );
			query = cypherQuery;
		}
		return new Neo4jQueryParsingResult( targetType, query, projections );
	}

	private Expression[] projectionsExpression(String targetAlias) {
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

}

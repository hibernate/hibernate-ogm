/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.parser.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.any;
import static org.neo4j.cypherdsl.CypherQuery.collection;
import static org.neo4j.cypherdsl.CypherQuery.has;
import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.none;
import static org.neo4j.cypherdsl.CypherQuery.not;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.neo4j.cypherdsl.Property;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jInPredicate extends InPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	private static final String X = "_x_";
	private final String alias;

	public Neo4jInPredicate(String alias, String columnName, List<Object> values) {
		super( columnName, values );
		this.alias = alias;
	}

	/**
	 * ANY( x IN values WHERE n.propertyName = x)
	 */
	@Override
	public BooleanExpression getQuery() {
		return any( X, collection( values() ), property().eq( identifier( X ) ) );
	}

	/**
	 * NOT HAS n.propertyName OR NONE( x IN values WHERE n.propertyName = x)
	 */
	@Override
	public BooleanExpression getNegatedQuery() {
		return not( has( property() ) ).or( none( X, collection( values() ), property().eq( identifier( X ) ) ) );
	}

	private Property property() {
		return identifier( alias ).property( propertyName );
	}

	private Object[] values() {
		return values.toArray( new Object[values.size()] );
	}

}

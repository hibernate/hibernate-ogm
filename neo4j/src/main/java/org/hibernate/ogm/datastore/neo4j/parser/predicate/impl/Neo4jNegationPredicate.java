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

import static org.neo4j.cypherdsl.CypherQuery.not;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jNegationPredicate extends NegationPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	@Override
	public BooleanExpression getQuery() {
		if ( getChild() instanceof NegatablePredicate ) {
			NegatablePredicate<BooleanExpression> negatable = (NegatablePredicate<BooleanExpression>) getChild();
			return negatable.getNegatedQuery();
		}
		return not( getChild().getQuery() );
	}

	@Override
	public BooleanExpression getNegatedQuery() {
		return getChild().getQuery();
	}

}

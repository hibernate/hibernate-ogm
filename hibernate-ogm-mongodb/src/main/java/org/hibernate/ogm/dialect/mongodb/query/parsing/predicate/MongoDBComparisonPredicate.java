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
package org.hibernate.ogm.dialect.mongodb.query.parsing.predicate;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoDB-based implementation of {@link ComparisonPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBComparisonPredicate extends ComparisonPredicate<DBObject> implements NegatablePredicate<DBObject> {

	public MongoDBComparisonPredicate(String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
	}

	@Override
	protected DBObject getStrictlyLessQuery() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	protected DBObject getLessOrEqualsQuery() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	protected DBObject getEqualsQuery() {
		return new BasicDBObject( propertyName, value );
	}

	@Override
	protected DBObject getGreaterOrEqualsQuery() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	protected DBObject getStrictlyGreaterQuery() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public DBObject getNegatedQuery() {
		return new BasicDBObject( propertyName, new BasicDBObject( "$ne", value ) );
	}
}

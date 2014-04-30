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
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import java.util.regex.Pattern;

import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.util.parser.impl.LikeExpressionToRegExpConverter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoDB-based implementation of {@link LikePredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBLikePredicate extends LikePredicate<DBObject> implements NegatablePredicate<DBObject> {

	private final Pattern pattern;

	public MongoDBLikePredicate(String propertyName, String patternValue, Character escapeCharacter) {
		super( propertyName, patternValue, escapeCharacter );

		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter( escapeCharacter );
		pattern = converter.getRegExpFromLikeExpression( patternValue );
	}

	@Override
	public DBObject getQuery() {
		return new BasicDBObject( propertyName, pattern );
	}

	@Override
	public DBObject getNegatedQuery() {
		return new BasicDBObject( propertyName, new BasicDBObject( "$not", pattern ) );
	}
}

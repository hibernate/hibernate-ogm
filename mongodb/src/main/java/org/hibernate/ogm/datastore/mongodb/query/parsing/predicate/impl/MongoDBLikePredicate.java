/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.HasExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.IdentifierExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.NotExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jIsNullPredicate extends IsNullPredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	private final IdentifierExpression identifier;

	public Neo4jIsNullPredicate(String alias, String propertyName) {
		super( propertyName );
		identifier = identifier( alias ).property( propertyName );
	}

	@Override
	public CypherExpression getQuery() {
		return new NotExpression( new HasExpression( identifier ) );
	}

	@Override
	public CypherExpression getNegatedQuery() {
		return new HasExpression( identifier );
	}

}

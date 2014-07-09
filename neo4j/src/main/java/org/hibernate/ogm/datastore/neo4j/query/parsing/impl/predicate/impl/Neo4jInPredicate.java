/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.IdentifierExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.InExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.NotInExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jInPredicate extends InPredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	private final IdentifierExpression identifier;

	public Neo4jInPredicate(String alias, String propertyName, List<Object> values) {
		super( propertyName, values );
		identifier = identifier( alias ).property( propertyName );
	}

	@Override
	public CypherExpression getQuery() {
		return new InExpression( identifier, values );
	}

	@Override
	public CypherExpression getNegatedQuery() {
		return new NotInExpression( identifier, values );
	}

}

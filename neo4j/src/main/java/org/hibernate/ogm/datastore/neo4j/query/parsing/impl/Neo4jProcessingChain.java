/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.AstProcessingChain;
import org.hibernate.hql.ast.spi.AstProcessor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.QueryRendererProcessor;
import org.hibernate.hql.ast.spi.QueryResolverProcessor;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jProcessingChain implements AstProcessingChain<Neo4jQueryParsingResult> {

	private final QueryResolverProcessor resolverProcessor;
	private final QueryRendererProcessor rendererProcessor;
	private final Neo4jQueryRendererDelegate rendererDelegate;

	public Neo4jProcessingChain(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNamesResolver, Map<String, Object> namedParameters) {
		Neo4jQueryResolverDelegate resolverDelegate = new Neo4jQueryResolverDelegate();
		Neo4jPropertyHelper propertyHelper = new Neo4jPropertyHelper( sessionFactory, entityNamesResolver );
		this.rendererDelegate = new Neo4jQueryRendererDelegate( sessionFactory, resolverDelegate, entityNamesResolver, propertyHelper, namedParameters );
		this.rendererProcessor = new QueryRendererProcessor( rendererDelegate );
		this.resolverProcessor = new QueryResolverProcessor( resolverDelegate );
	}

	@Override
	public Iterator<AstProcessor> iterator() {
		return Arrays.asList( resolverProcessor, rendererProcessor ).iterator();
	}

	@Override
	public org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryParsingResult getResult() {
		return rendererDelegate.getResult();
	}

}

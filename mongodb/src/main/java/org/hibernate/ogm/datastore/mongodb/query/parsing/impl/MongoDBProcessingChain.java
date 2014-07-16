/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

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
 * AST processing chain for creating MongoDB queries (in form of {@link com.mongodb.DBObject}s from HQL queries.
 *
 * @author Gunnar Morling
 */
public class MongoDBProcessingChain implements AstProcessingChain<MongoDBQueryParsingResult> {

	private final QueryResolverProcessor resolverProcessor;
	private final QueryRendererProcessor rendererProcessor;
	private final MongoDBQueryRendererDelegate rendererDelegate;

	public MongoDBProcessingChain(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames, Map<String, Object> namedParameters) {
		this.resolverProcessor = new QueryResolverProcessor( new MongoDBQueryResolverDelegate() );

		MongoDBPropertyHelper propertyHelper = new MongoDBPropertyHelper( sessionFactory, entityNames );
		MongoDBQueryRendererDelegate rendererDelegate = new MongoDBQueryRendererDelegate(
				sessionFactory,
				entityNames,
				propertyHelper,
				namedParameters );
		this.rendererProcessor = new QueryRendererProcessor( rendererDelegate );
		this.rendererDelegate = rendererDelegate;
	}

	@Override
	public Iterator<AstProcessor> iterator() {
		return Arrays.asList( resolverProcessor, rendererProcessor ).iterator();
	}

	@Override
	public MongoDBQueryParsingResult getResult() {
		return rendererDelegate.getResult();
	}
}

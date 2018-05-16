/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.AstProcessingChain;
import org.hibernate.hql.ast.spi.AstProcessor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.QueryRendererProcessor;
import org.hibernate.hql.ast.spi.QueryResolverProcessor;
import org.hibernate.ogm.query.parsing.impl.HibernateOGMQueryResolverDelegate;

/**
 * AST processing chain for creating Infinispan server queries in form of {@link String}s from HQL queries.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteProcessingChain implements AstProcessingChain<InfinispanRemoteQueryParsingResult> {

	private final QueryResolverProcessor resolverProcessor;
	private final QueryRendererProcessor rendererProcessor;
	private final InfinispanRemoteQueryRendererDelegate rendererDelegate;

	public InfinispanRemoteProcessingChain(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNamesResolver, Map<String, Object> namedParameters) {
		HibernateOGMQueryResolverDelegate resolverDelegate = new HibernateOGMQueryResolverDelegate();
		rendererDelegate = new InfinispanRemoteQueryRendererDelegate(
				sessionFactory, entityNamesResolver, new InfinispanRemotePropertyHelper( sessionFactory, entityNamesResolver ), namedParameters );
		this.resolverProcessor = new QueryResolverProcessor( resolverDelegate );
		this.rendererProcessor = new QueryRendererProcessor( rendererDelegate );
	}

	@Override
	public Iterator<AstProcessor> iterator() {
		return Arrays.asList( resolverProcessor, rendererProcessor ).iterator();
	}

	@Override
	public InfinispanRemoteQueryParsingResult getResult() {
		return rendererDelegate.getResult();
	}
}

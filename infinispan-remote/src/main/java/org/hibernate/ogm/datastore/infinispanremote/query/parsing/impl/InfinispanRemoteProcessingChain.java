package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.AstProcessingChain;
import org.hibernate.hql.ast.spi.AstProcessor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.QueryRendererProcessor;
import org.hibernate.hql.ast.spi.QueryResolverProcessor;

public class InfinispanRemoteProcessingChain implements AstProcessingChain<InfinispanRemoteQueryParsingResult> {

	private final QueryResolverProcessor resolverProcessor;
	private final QueryRendererProcessor rendererProcessor;
	private final InfinispanRemoteQueryRendererDelegate rendererDelegate;

	public InfinispanRemoteProcessingChain(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNamesResolver, Map<String, Object> namedParameters) {
		InfinispanRemoteQueryResolverDelegate resolverDelegate = new InfinispanRemoteQueryResolverDelegate();
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

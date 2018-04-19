package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;

public class InfinispanRemoteQueryRendererDelegate extends SingleEntityQueryRendererDelegate<StringBuilder, InfinispanRemoteQueryParsingResult> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final SessionFactoryImplementor sessionFactory;

	public InfinispanRemoteQueryRendererDelegate(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames,
			InfinispanRemotePropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super( propertyHelper, entityNames, SingleEntityQueryBuilder.getInstance( new InfinispanRemotePraticateFactory( sessionFactory ), propertyHelper ), namedParameters );
		this.sessionFactory = sessionFactory;
	}

	@Override
	public InfinispanRemoteQueryParsingResult getResult() {
		return new InfinispanRemoteQueryParsingResult( builder.build().toString(), projections );
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		this.propertyPath = propertyPath;
	}
}

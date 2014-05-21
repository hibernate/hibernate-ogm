/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;

/**
 * Caches parameter metadata for native NoSQL queries.
 * <p>
 * Resembles {@link QueryPlanCache} which atm. can not directly be re-used as it doesn't allow to customize the
 * parameter parser.
 * <p>
 * TODO: To be removed once HHH-9190 and OGM-414 have been resolved.
 *
 * @author Gunnar Morling
 */
public class NoSqlQueryParameterMetadataCache {

	private final BoundedConcurrentHashMap<String,ParameterMetadata> parameterMetadataCache;
	private final SessionFactoryImplementor sessionFactory;
	private final ParameterMetadataBuilder parameterMetadataBuilder;

	public NoSqlQueryParameterMetadataCache(SessionFactoryImplementor factory) {
		this.sessionFactory = factory;

		Integer maxParameterMetadataCount = new ConfigurationPropertyReader( factory.getProperties() )
				.property( Environment.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, int.class )
				.withDefault( QueryPlanCache.DEFAULT_PARAMETER_METADATA_MAX_COUNT )
				.getValue();

		parameterMetadataCache = new BoundedConcurrentHashMap<String, ParameterMetadata>(
				maxParameterMetadataCount,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);

		parameterMetadataBuilder = sessionFactory
				.getServiceRegistry()
				.getService( GridDialect.class )
				.getParameterMetadataBuilder();
	}

	public ParameterMetadata getParameterMetadata(final String noSqlQuery)  {
		ParameterMetadata metadata = parameterMetadataCache.get( noSqlQuery );

		if ( metadata == null ) {
			metadata = parameterMetadataBuilder.buildParameterMetadata( noSqlQuery );

			ParameterMetadata cached = parameterMetadataCache.putIfAbsent( noSqlQuery, metadata );
			if ( cached != null ) {
				metadata = cached;
			}
		}

		return metadata;
	}
}

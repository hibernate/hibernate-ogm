/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
 * TODO: To be removed once HHH-9190 and OGM-714 have been resolved.
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.parser.impl.Neo4jBasedQueryParserService;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Provides access to the Neo4j system.
 *
 * @author Davide D'Alto
 */
public class Neo4jDatastoreProvider implements DatastoreProvider, Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private GraphDatabaseService neo4jDb;

	private GraphDatabaseServiceFactory graphDbFactory;

	private ServiceRegistryImplementor registry;

	private Neo4jSequenceGenerator sequenceGenerator;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.registry = serviceRegistry;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return Neo4jBasedQueryParserService.class;
	}

	@Override
	public void configure(Map cfg) {
		graphDbFactory = new Neo4jGraphDatabaseServiceFactoryProvider().load( cfg, registry.getService( ClassLoaderService.class ) );
	}

	@Override
	public void stop() {
		neo4jDb.shutdown();
	}

	@Override
	public void start() {
		this.neo4jDb = graphDbFactory.create();
		this.sequenceGenerator = new Neo4jSequenceGenerator( neo4jDb );
		this.graphDbFactory = null;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return Neo4jDialect.class;
	}

	public GraphDatabaseService getDataBase() {
		return neo4jDb;
	}

	public SchemaBuilder getSchemaBuilder() {
		return new SchemaBuilder();
	}

	public Neo4jSequenceGenerator getSequenceGenerator() {
		return this.sequenceGenerator;
	}

	public class SchemaBuilder {

		private final Map<String, Set<String>> sequences = new HashMap<String, Set<String>>();

		public SchemaBuilder addSequence(String generatorKey, String segmentValue) {
			if ( sequences.containsKey( generatorKey ) ) {
				sequences.get( generatorKey ).add( segmentValue );
			}
			else {
				Set<String> segments = new HashSet<String>();
				segments.add( segmentValue );
				sequences.put( generatorKey, segments );
			}
			return this;
		}

		public void update() {
			sequenceGenerator.createUniqueConstraint( sequences );
		}
	}
}

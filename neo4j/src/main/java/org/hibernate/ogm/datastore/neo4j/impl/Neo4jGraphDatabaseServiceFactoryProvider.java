/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Properties;

import org.hibernate.ogm.datastore.neo4j.Environment;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Creates an instance of {@link GraphDatabaseServiceFactory} using the implementation selected in the properties.
 * <p>
 * If an implementation is not selected the default one is {@link EmbeddedGraphDatabaseFactory}.
 *
 * @see GraphDatabaseServiceFactory
 * @see Environment#NEO4J_GRAPHDB_FACTORYCLASS
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jGraphDatabaseServiceFactoryProvider {

	public GraphDatabaseServiceFactory load(Properties properties) {
		String factoryClassName = (String) properties.get( Environment.NEO4J_GRAPHDB_FACTORYCLASS );
		GraphDatabaseServiceFactory factory = createFactory( factoryClassName );
		factory.initialize( properties );
		return factory;
	}

	private GraphDatabaseServiceFactory createFactory(String className) {
		if ( className == null ) {
			return new EmbeddedGraphDatabaseFactory();
		}
		else {
			return ClassLoaderHelper.instanceFromName( GraphDatabaseServiceFactory.class, className, getClass(),
					GraphDatabaseService.class.getName() );
		}
	}

}

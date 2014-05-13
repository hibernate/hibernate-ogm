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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Contains methods to create an {@link org.neo4j.kernel.EmbeddedGraphDatabase}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class EmbeddedGraphDatabaseFactory implements GraphDatabaseServiceFactory {

	private String dbLocation;

	private URL configurationLocation;

	private Map<?, ?> configuration;

	@Override
	public void initialize(Map<?, ?> properties) {
		ConfigurationPropertyReader configurationPropertyReader = new ConfigurationPropertyReader( properties );

		this.dbLocation = configurationPropertyReader.property( Neo4jProperties.DATABASE_PATH, String.class )
				.required()
				.getValue();

		this.configurationLocation = configurationPropertyReader
				.property( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
				.getValue();

		configuration = properties;
	}

	@Override
	public GraphDatabaseService create() {
		GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( dbLocation );
		setConfigurationFromLocation( builder, configurationLocation );
		setConfigurationFromProperties( builder, configuration );
		return builder.newGraphDatabase();
	}

	private void setConfigurationFromProperties(GraphDatabaseBuilder builder, Map<?, ?> properties) {
		if ( properties != null ) {
			builder.setConfig( convert( properties ) );
		}
	}

	private Map<String, String> convert(Map<?, ?> properties) {
		Map<String, String> neo4jConfiguration = new HashMap<String, String>();
		for ( Map.Entry<?, ?> entry : properties.entrySet() ) {
			neo4jConfiguration.put( String.valueOf( entry.getKey() ), String.valueOf( entry.getValue() ) );
		}
		return neo4jConfiguration;
	}

	private void setConfigurationFromLocation(GraphDatabaseBuilder builder, URL cfgLocation) {
		if ( cfgLocation != null ) {
			builder.loadPropertiesFromURL( cfgLocation );
		}
	}
}

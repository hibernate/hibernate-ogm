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

import static org.hibernate.ogm.datastore.neo4j.Environment.NEO4J_DATABASE_PATH;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.ogm.datastore.neo4j.Environment;
import org.hibernate.ogm.datastore.neo4j.api.GraphDatabaseServiceFactory;
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

	private String configurationLocation;

	private Properties configuration;

	@Override
	public void initialize(Properties properties) {
		validate( properties );
		dbLocation = properties.getProperty( Environment.NEO4J_DATABASE_PATH );
		configurationLocation = properties.getProperty( Environment.NEO4J_CONFIGURATION_LOCATION );
		configuration = properties;
	}

	private void validate(Properties properties) {
		String dbLocation = (String) properties.get( NEO4J_DATABASE_PATH );
		if ( dbLocation == null ) {
			throw new IllegalArgumentException( "Property " + NEO4J_DATABASE_PATH + " cannot be null" );
		}
	}

	@Override
	public GraphDatabaseService create() {
		GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( dbLocation );
		setConfigurationFromLocation( builder, configurationLocation );
		setConfigurationFromProperties( builder, configuration );
		return builder.newGraphDatabase();
	}

	private void setConfigurationFromProperties(GraphDatabaseBuilder builder, Properties properties) {
		if ( properties != null ) {
			builder.setConfig( convert( properties ) );
		}
	}

	private Map<String, String> convert(Properties properties) {
		Map<String, String> neo4jConfiguration = new HashMap<String, String>();
		for ( Map.Entry<?, ?> entry : properties.entrySet() ) {
			neo4jConfiguration.put( String.valueOf( entry.getKey() ), String.valueOf( entry.getValue() ) );
		}
		return neo4jConfiguration;
	}

	private void setConfigurationFromLocation(GraphDatabaseBuilder builder, String cfgLocation) {
		if ( cfgLocation != null ) {
			try {
				builder.loadPropertiesFromURL( new URL( cfgLocation ) );
			}
			catch ( MalformedURLException e ) {
				builder.loadPropertiesFromFile( cfgLocation );
			}
		}
	}

}

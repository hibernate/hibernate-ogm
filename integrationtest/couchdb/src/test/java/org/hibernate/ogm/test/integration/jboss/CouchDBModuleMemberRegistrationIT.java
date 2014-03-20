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
package org.hibernate.ogm.test.integration.wildfly;

import org.hibernate.ogm.test.integration.wildfly.model.Member;
import org.hibernate.ogm.test.integration.wildfly.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.runner.RunWith;

/**
 * Test the hibernate OGM module in JBoss AS using CouchDB
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 */
@RunWith(Arquillian.class)
public class CouchDBModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	public static final String ENVIRONMENT_COUCHDB_HOSTNAME = "COUCHDB_HOSTNAME";
	public static final String ENVIRONMENT_COUCHDB_PORT = "COUCHDB_PORT";

	public static final String DEFAULT_HOSTNAME = "localhost";
	public static final String DEFAULT_PORT = "5984";

	private static String couchDBHostName;
	private static String couchDBPortNumber;

	static {
		setHostName();
		setPortNumber();
	}

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment.Builder( CouchDBModuleMemberRegistrationIT.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate:ogm services, org.hibernate.ogm.couchdb services" )
				.createDeployment();
	}

	private static void setHostName() {
		couchDBHostName = System.getenv( ENVIRONMENT_COUCHDB_HOSTNAME );
		if ( isNull( couchDBHostName ) ) {
			couchDBHostName = DEFAULT_HOSTNAME;
		}
	}

	private static void setPortNumber() {
		couchDBPortNumber = System.getenv( ENVIRONMENT_COUCHDB_PORT );
		if ( isNull( couchDBPortNumber ) ) {
			couchDBPortNumber = DEFAULT_PORT;
		}
	}

	private static boolean isNull(String value) {
		return value == null || value.length() == 0 || value.toLowerCase().equals( "null" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
					.version( "2.0" )
					.createPersistenceUnit()
						.name( "primary" )
						.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
						.clazz( Member.class.getName() )
						.getOrCreateProperties()
							.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "couchdb" ).up()
							.createProperty().name( "hibernate.ogm.datastore.host" ).value( couchDBHostName ).up()
							.createProperty().name( "hibernate.ogm.datastore.port" ).value( couchDBPortNumber ).up()
							.createProperty().name( "hibernate.ogm.datastore.database" ).value( "ogm_test_database" ).up()
							.createProperty().name( "hibernate.ogm.datastore.create_database" ).value( "true" ).up()
							.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.up().up();
	}

}

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
package org.hibernate.ogm.test.integration.jbossas7;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.runner.RunWith;
import org.hibernate.ogm.test.integration.jbossas7.model.Member;
import org.hibernate.ogm.test.integration.jbossas7.util.ModuleMemberRegistrationDeployment;

/**
 * Test the hibernate OGM module in JBoss AS using MongoDB
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
@RunWith(Arquillian.class)
public class MongoDBModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( MongoDBModuleMemberRegistrationIT.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate:ogm services, org.hibernate.ogm.mongodb services" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		String host = System.getenv( "MONGODB_HOSTNAME" );
		String port = System.getenv( "MONGODB_PORT" );

		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( Member.class.getName() )
				.getOrCreateProperties();
		if ( isNotNull( host ) ) {
			propertiesContext.createProperty().name( "hibernate.ogm.mongodb.host" ).value( host );
		}
		if ( isNotNull( port ) ) {
			propertiesContext.createProperty().name( "hibernate.ogm.mongodb.port" ).value( port );
		}
		return propertiesContext
					.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "mongodb" ).up()
					.createProperty().name( "hibernate.ogm.mongodb.database" ).value( "ogm_test_database" ).up()
				.up().up();
	}

	private static boolean isNotNull(String mongoHostName) {
		return mongoHostName != null && mongoHostName.length() > 0 && ! "null".equals( mongoHostName );
	}
}

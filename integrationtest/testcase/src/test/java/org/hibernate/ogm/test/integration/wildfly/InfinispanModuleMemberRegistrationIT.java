/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module in WildFly using Infinispan.
 *
 * @author Davide D'Alto
 */
@RunWith(Arquillian.class)
public class InfinispanModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
			.Builder( InfinispanModuleMemberRegistrationIT.class )
			.persistenceXml( persistenceXml() )
			.manifestDependencies( "org.hibernate:ogm services" )
			.createDeployment()
			.addAsResource( infinispanXml(), "infinispan.xml" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( Member.class.getName() ).getOrCreateProperties()
				.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "infinispan" ).up()
				.createProperty().name( "hibernate.ogm.infinispan.configuration_resourcename" ).value( "infinispan.xml" ).up()
			.up().up();
	}

	private static Asset infinispanXml() {
		String infinispanXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<infinispan>"
				+ "<global />"
				+ "<default >"
					+ "<transaction transactionMode=\"TRANSACTIONAL\" transactionManagerLookupClass=\"org.infinispan.transaction.lookup.GenericTransactionManagerLookup\" />"
					+ "<jmxStatistics enabled=\"true\" />"
					+ "<eviction strategy=\"NONE\" />"
					+ "<expiration wakeUpInterval=\"-1\" reaperEnabled=\"false\" />"
				+ "</default>"
				+ "<namedCache name=\"ENTITIES\" />"
				+ "<namedCache name=\"ASSOCIATIONS\" />"
				+ "<namedCache name=\"IDENTIFIERS\" />"
			+ "</infinispan>";
		return new StringAsset( infinispanXml );
	}

}

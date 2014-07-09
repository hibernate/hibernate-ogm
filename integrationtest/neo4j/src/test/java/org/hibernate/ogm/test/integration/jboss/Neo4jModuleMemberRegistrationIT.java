/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;

import javax.inject.Inject;

import org.fest.util.Files;
import org.fest.util.FilesException;
import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module using Neo4j
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
@RunWith(Arquillian.class)
public class Neo4jModuleMemberRegistrationIT {

	@Inject
	HelloWorldBean bean;

	@Deployment
	public static Archive<?> createTestArchive() throws Exception {
		return ShrinkWrap
				.create( WebArchive.class, Neo4jModuleMemberRegistrationIT.class.getSimpleName() + ".war" )
				.addClasses( Neo4jModuleMemberRegistrationIT.class, HelloWorldBean.class, Files.class, FilesException.class )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.add( manifest(), "META-INF/MANIFEST.MF" )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				;
	}

	private static Asset manifest() {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", "org.hibernate:ogm services, org.hibernate.ogm.neo4j services" )
				.exportAsString();
		return new StringAsset( manifest );
	}

	private static StringAsset persistenceXml() throws Exception {
		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.provider( HibernateOgmPersistence.class.getName() )
				.clazz( Member.class.getName() )
				.getOrCreateProperties();
		PersistenceDescriptor persistenceDescriptor = propertiesContext
				.createProperty().name( Neo4jProperties.DATASTORE_PROVIDER ).value( Neo4j.DATASTORE_PROVIDER_NAME ).up()
				.createProperty()
				.name( Neo4jProperties.DATABASE_PATH )
				.value( neo4jFolder() )
				.up()
				.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.up().up();
		return new StringAsset( persistenceDescriptor.exportAsString() );
	}

	private static String neo4jFolder() throws Exception {
		InputStream propertiesStrem = Thread.currentThread().getContextClassLoader().getResourceAsStream( "neo4j-test.properties" );
		java.util.Properties properties = new java.util.Properties();
		properties.load( propertiesStrem );
		String buildDirectory = properties.getProperty( "build.directory" );
		return buildDirectory + File.separator + "NEO4J-DB" + File.separator + System.currentTimeMillis();
	}

	// TODO OGM-373 Provide an actual test with some queries etc.; For now we just ensure that the module definitions
	// are not broken and the application can be deployed/started
	@Test
	public void shouldBeStarted() throws Exception {
		assertEquals( "Container should be started", HelloWorldBean.HELLO, bean.hello() );
	}

}

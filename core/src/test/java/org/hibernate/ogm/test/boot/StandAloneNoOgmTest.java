/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.boot;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.ogm.utils.Throwables;
import org.junit.Test;

/**
 * Test for bootstrapping Hibernate ORM if Hibernate OGM is present.
 *
 * @author Gunnar Morling
 */
public class StandAloneNoOgmTest {

	@Test
	public void canBootstrapHibernateOrmWithOgmBeingPresent() {
		// Hibernate OGM is not auto-enabled if a connection url is defined on the persistence unit.
		// In this case Hibernate ORM will be stated.
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.applySetting( "hibernate.connection.url", "jdbc:h2:tcp://localhost/mem:test" )
			.build();

		try {
			new MetadataSources( registry )
				.buildMetadata()
				.buildSessionFactory();

			fail( "Expected exception was not raised" );
		}
		// Failure is expected as we didn't configure a JDBC connection nor a Dialect
		// (and this would fail only if effectively loading Hibernate ORM without OGM superpowers)
		catch ( Exception pe ) {
			assertThat( Throwables.getRootCause( pe ).getMessage() ).contains( "hibernate.dialect" );
		}
	}

	@Test
	public void sessionFactoryBuilderIsTheOneFromOrm() {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.applySetting( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" )
			.build();

		SessionFactoryBuilder sfb = new MetadataSources( registry )
			.buildMetadata()
			.getSessionFactoryBuilder();

		assertThat( sfb )
				.describedAs( "Expecting instance of ORM's SessionFactoryBuilder implementation" ).
				isInstanceOf( SessionFactoryBuilderImpl.class );
	}
}

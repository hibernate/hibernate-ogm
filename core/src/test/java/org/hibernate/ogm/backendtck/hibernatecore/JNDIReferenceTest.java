/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hibernatecore;

import static org.fest.assertions.Assertions.assertThat;

import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryRegistry.ObjectFactoryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JNDIReferenceTest extends OgmTestCase {

	@Test
	public void testGetReferenceImplementation() throws Exception {
		final Session session = openSession();
		SessionFactory factory = session.getSessionFactory();

		assertThat( factory.getClass() ).isEqualTo( OgmSessionFactoryImpl.class );

		Reference reference = factory.getReference();
		assertThat( reference.getClassName() ).isEqualTo( OgmSessionFactoryImpl.class.getName() );
		assertThat( reference.getFactoryClassName() ).isEqualTo( ObjectFactoryImpl.class.getName() );
		assertThat( reference.get( 0 ) ).isNotNull();
		assertThat( reference.getFactoryClassLocation() ).isNull();

		ObjectFactory objFactory = new ObjectFactoryImpl();
		SessionFactory factoryFromRegistry = (SessionFactory) objFactory.getObjectInstance( reference, null, null, null );
		assertThat( factoryFromRegistry.getClass() ).isEqualTo( OgmSessionFactoryImpl.class );
		assertThat( factoryFromRegistry.getReference() ).isEqualTo( factory.getReference() );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Contact.class
		};
	}
}

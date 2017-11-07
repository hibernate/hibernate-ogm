/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class JPAStandaloneORMAndOGMTest {
	@Rule
	public PackagingRule ogmPackaging = new PackagingRule( "persistencexml/orm-and-ogm.xml", Poem.class );

	@Test
	//Test for OGM-416 (avoid StackOverFlow when both an OGM and ORM PU are used
	public void testJTAStandaloneNoOgm() throws Exception {
		Map<String, String> settings = TestHelper.getDefaultTestSettings();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "none"  );

		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "ogm", settings );
		emf.close();

		emf = Persistence.createEntityManagerFactory( "no-ogm", settings );
		emf.close();
	}
}

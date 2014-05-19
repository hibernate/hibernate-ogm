/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.storageconfiguration;

import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Before;

/**
 * Base for tests for configuring association storage strategies.
 *
 * @author Gunnar Morling
 */
public abstract class AssociationStorageTestBase extends OgmTestCase {

	protected OgmConfiguration configuration;
	protected OgmSessionFactory sessions;

	@Before
	public void setupConfiguration() {
		configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );
		configure( configuration );
	}

	protected void setupSessionFactory() {
		sessions = configuration.buildSessionFactory();
	}

	protected long associationDocumentCount() {
		return TestHelper.getNumberOfAssociations( sessions, AssociationStorageType.ASSOCIATION_DOCUMENT );
	}

	protected long inEntityAssociationCount() {
		return TestHelper.getNumberOfAssociations( sessions, AssociationStorageType.IN_ENTITY );
	}
}

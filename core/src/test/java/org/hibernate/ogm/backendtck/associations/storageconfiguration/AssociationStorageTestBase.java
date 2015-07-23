/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.storageconfiguration;

import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.runner.RunWith;

/**
 * Base for tests for configuring association storage strategies.
 *
 * @author Gunnar Morling
 */
@RunWith(SkippableTestRunner.class)
public abstract class AssociationStorageTestBase {

	protected OgmSessionFactory sessions;

	protected long associationDocumentCount() {
		return TestHelper.getNumberOfAssociations( sessions, AssociationStorageType.ASSOCIATION_DOCUMENT );
	}

	protected long inEntityAssociationCount() {
		return TestHelper.getNumberOfAssociations( sessions, AssociationStorageType.IN_ENTITY );
	}

	protected Class<? extends DatastoreConfiguration<DocumentStoreGlobalContext<?, ?>>> getDocumentDatastoreConfiguration() {
		return TestHelper.<DatastoreConfiguration<DocumentStoreGlobalContext<?, ?>>>getCurrentDatastoreConfiguration();
	}
}

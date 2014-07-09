/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import org.hibernate.ogm.backendtck.id.CompositeIdTest;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class CompositeIdInEmbeddedTest extends CompositeIdTest {
	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		super.refineInfo( info );
		info.getProperties()
			.put( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.IN_ENTITY );
	}
}

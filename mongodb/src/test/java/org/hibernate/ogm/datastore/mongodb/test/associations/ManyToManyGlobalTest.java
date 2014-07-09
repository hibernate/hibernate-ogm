/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.ManyToManyTest;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ManyToManyGlobalTest extends ManyToManyTest {
	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
	}
}

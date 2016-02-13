/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.associations;

import java.util.Map;

import org.hibernate.ogm.backendtck.associations.collection.manytomany.ManyToManyTest;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;

/**
 * @author Mark Paluch
 */
public class ManyToManyDocumentTest extends ManyToManyTest {

	@Override
	protected void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
	}
}

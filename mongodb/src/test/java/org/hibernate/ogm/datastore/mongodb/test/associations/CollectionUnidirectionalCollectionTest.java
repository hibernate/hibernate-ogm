/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import java.util.Map;

import org.hibernate.ogm.backendtck.associations.collection.unidirectional.CollectionUnidirectionalTest;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorageType;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class CollectionUnidirectionalCollectionTest extends CollectionUnidirectionalTest {

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
		settings.put(
				MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE,
				AssociationDocumentStorageType.COLLECTION_PER_ASSOCIATION
		);
	}
}

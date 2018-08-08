/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * The delegator provides access to binary storage.
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public interface BinaryStorageDelegator {
	void storeContentToBinaryStorage(MongoDatabase mongoDatabase,OptionsContext optionsContext, Document currentDocument,String fieldName, Tuple tuple);

	void removeContentFromBinaryStore(MongoDatabase mongoDatabase, OptionsContext optionsContext, Document deletedDocument, String fieldName);

	void loadContentFromBinaryStorageToField(MongoDatabase mongoDatabase, OptionsContext optionsContext, Document currentDocument, String fieldName);
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.binarystorage;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

import org.bson.Document;

/**
 * It is empty implementation of BinaryStorageDelegator
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoopBinaryStore implements BinaryStorage {
	@Override
	public void storeContentToBinaryStorage( OptionsContext optionsContext, Document currentDocument, String fieldName, Tuple tuple) {

	}

	@Override
	public void removeContentFromBinaryStore(OptionsContext optionsContext, Document deletedDocument, String fieldName) {

	}

	@Override
	public void loadContentFromBinaryStorageToField( OptionsContext optionsContext, Document currentDocument, String fieldName, Class<?> fieldType) {

	}
}

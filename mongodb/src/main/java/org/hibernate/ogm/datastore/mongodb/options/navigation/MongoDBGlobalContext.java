/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.navigation;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;

import com.mongodb.WriteConcern;

/**
 * Allows to configure MongoDB-specific options applying on a global level. These options may be overridden for single
 * entities or properties.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public interface MongoDBGlobalContext extends DocumentStoreGlobalContext<MongoDBGlobalContext, MongoDBEntityContext> {

	/**
	 * Defines the type of write concern to be applied when performing write operations against the datastore.
	 *
	 * @param writeConcern the write concern type
	 * @return this context, allowing for further fluent API invocations
	 * @see http://docs.mongodb.org/manual/core/write-concern/
	 */
	MongoDBGlobalContext writeConcern(WriteConcernType writeConcern);

	/**
	 * Specifies a custom {@link WriteConcern} implementation to be applied when performing write operations against the
	 * datastore. Either use this option or {@link #writeConcern(WriteConcernType)} but not both at the same type.
	 *
	 * @param writeConcern the write concern
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBGlobalContext writeConcern(WriteConcern writeConcern);

	/**
	 * Defines the type of read preference to be applied when performing read operations against the datastore.
	 *
	 * @param readPreference the read preference type
	 * @return this context, allowing for further fluent API invocations
	 * @see http://docs.mongodb.org/manual/core/read-preference/
	 */
	MongoDBGlobalContext readPreference(ReadPreferenceType readPreference);

	/**
	 * Specifies how association documents should be persisted. Only applies when the association storage strategy is
	 * set to {@link AssociationStorageType#ASSOCIATION_DOCUMENT}.
	 *
	 * @param associationDocumentStorage the association document type to be used when not configured on the entity or
	 * property level
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBGlobalContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage);
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.navigation;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreEntityContext;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;

import com.mongodb.WriteConcern;

/**
 * Allows to configure MongoDB-specific options applying on a global level. These options may be overridden for single
 * properties.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public interface MongoDBEntityContext extends DocumentStoreEntityContext<MongoDBEntityContext, MongoDBPropertyContext> {

	/**
	 * Defines the type of <a href="http://docs.mongodb.org/manual/core/write-concern/">write concern</a> to be applied
	 * when performing write operations for the current entity.
	 *
	 * @param writeConcern the write concern type
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBEntityContext writeConcern(WriteConcernType writeConcern);

	/**
	 * Specifies a custom {@link WriteConcern} implementation to be applied when performing write operations for the
	 * current entity. Either use this option or {@link #writeConcern(WriteConcernType)} but not both at the same type.
	 *
	 * @param writeConcern the write concern
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBEntityContext writeConcern(WriteConcern writeConcern);

	/**
	 * Defines the type of <a href="http://docs.mongodb.org/manual/core/read-preference/">read preference</a> to be
	 * applied when performing read operations against the datastore.
	 *
	 * @param readPreference the read preference type
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBEntityContext readPreference(ReadPreferenceType readPreference);

	/**
	 * Specifies how association documents should be persisted. Only applies when the association storage strategy is
	 * set to {@link AssociationStorageType#ASSOCIATION_DOCUMENT}.
	 *
	 * @param associationDocumentStorage the association document type to be used when not configured the property level
	 * @return this context, allowing for further fluent API invocations
	 */
	MongoDBEntityContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage);
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.navigation.impl;

import org.hibernate.ogm.datastore.document.options.navigation.spi.BaseDocumentStorePropertyContext;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.AssociationDocumentStorageOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBEntityContext;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBPropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.util.impl.Contracts;

import com.mongodb.WriteConcern;

/**
 * Converts MongoDB property-level options.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public abstract class MongoDBPropertyContextImpl extends BaseDocumentStorePropertyContext<MongoDBEntityContext, MongoDBPropertyContext> implements
		MongoDBPropertyContext {

	public MongoDBPropertyContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public MongoDBPropertyContext associationDocumentStorage(AssociationDocumentType associationDocumentStorage) {
		Contracts.assertParameterNotNull( associationDocumentStorage, "associationDocumentStorage" );
		addPropertyOption( new AssociationDocumentStorageOption(), associationDocumentStorage );
		return this;
	}

	@Override
	public MongoDBPropertyContext writeConcern(WriteConcern writeConcern) {
		Contracts.assertParameterNotNull( writeConcern, "writeConcern" );
		addPropertyOption( new WriteConcernOption(), writeConcern );
		return this;
	}

	@Override
	public MongoDBPropertyContext writeConcern(WriteConcernType writeConcern) {
		Contracts.assertParameterNotNull( writeConcern, "writeConcern" );
		addPropertyOption( new WriteConcernOption(), writeConcern.getWriteConcern() );
		return this;
	}

	@Override
	public MongoDBPropertyContext readPreference(ReadPreferenceType readPreference) {
		Contracts.assertParameterNotNull( readPreference, "readPreference" );
		addPropertyOption( new ReadPreferenceOption(), readPreference.getReadPreference() );
		return this;
	}
}

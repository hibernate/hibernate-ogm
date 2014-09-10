/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.impl;

import java.util.Map.Entry;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl.TuplesDesignDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.logging.impl.Log;
import org.hibernate.ogm.datastore.couchdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Performs sanity checks of the mapped entities. In particular a log message at WARN level is issued in case an entity
 * doesn't map the {@code _rev} attribute. Also creates required design documents in the store.
 *
 * @author Gunnar Morling
 */
public class CouchDBSchemaSchemaDefiner extends BaseSchemaDefiner {

	private static final Log logger = LoggerFactory.getLogger();

	@Override
	public void validateMapping(SessionFactoryImplementor factory) {
		for ( Entry<String, EntityPersister> entityAndPersister : factory.getEntityPersisters().entrySet() ) {
			if ( !hasRevisionColumn( ( (OgmEntityPersister) entityAndPersister.getValue() ) ) ) {
				logger.entityShouldHaveRevisionProperty( entityAndPersister.getKey() );
			}
		}
	}

	@Override
	public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
		CouchDBDatastoreProvider datastoreProvider = (CouchDBDatastoreProvider) factory.getServiceRegistry().getService( DatastoreProvider.class );
		CouchDBDatastore dataStore = datastoreProvider.getDataStore();

		// create tuple design document if required
		if ( !dataStore.exists( TuplesDesignDocument.DOCUMENT_ID, true ) ) {
			dataStore.saveDocument( new TuplesDesignDocument() );
		}
	}

	/**
	 * Whether the specified entity type maps the {@code _rev} field or not.
	 */
	private boolean hasRevisionColumn(OgmEntityPersister persister) {
		for ( int i = 0; i < persister.getPropertyNames().length; i++ ) {
			for ( String columnName : persister.getPropertyColumnNames( i ) ) {
				if ( columnName.equals( Document.REVISION_FIELD_NAME ) ) {
					return true;
				}
			}
		}

		return false;
	}
}

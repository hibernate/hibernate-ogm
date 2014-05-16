/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.impl;

import java.util.Map.Entry;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.logging.impl.Log;
import org.hibernate.ogm.datastore.couchdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.service.impl.ConfigurationService;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrates with the ORM engine by contributing a {@link SessionFactoryObserver} which checks the mapped entities for
 * CouchDB-specific requirements.
 * <p>
 * Note: This could be implemented using the {@code StartStoppable} contract once OGM-445 has been addressed
 *
 * @author Gunnar Morling
 */
public class CouchDBIntegrator implements Integrator {

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		sessionFactory.addObserver( new EntityMappingValidator() );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		sessionFactory.addObserver( new EntityMappingValidator() );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do
	}

	/**
	 * Performs sanity checks of the mapped entities. In particular a log message at WARN level is issued in case an
	 * entity doesn't map the {@code _rev} attribute.
	 *
	 * @author Gunnar Morling
	 */
	private static class EntityMappingValidator implements SessionFactoryObserver {

		private static final Log logger = LoggerFactory.getLogger();

		@Override
		public void sessionFactoryCreated(SessionFactory factory) {
			SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;

			// only perform the validation if OGM is enabled and uses the CouchDB datastore
			if ( !currentDialectIsCouchDB( sessionFactoryImplementor ) ) {
				return;
			}

			validateEntityMappings( sessionFactoryImplementor );
		}

		private boolean currentDialectIsCouchDB(SessionFactoryImplementor sessionFactoryImplementor) {
			ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();

			return registry.getService( ConfigurationService.class ).isOgmOn() &&
					( registry.getService( DatastoreProvider.class ) instanceof CouchDBDatastoreProvider );
		}

		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			// nothing to do
		}

		private void validateEntityMappings(SessionFactoryImplementor factory) {
			for ( Entry<String, EntityPersister> entityAndPersister : factory.getEntityPersisters().entrySet() ) {
				if ( !hasRevisionColumn( ( (OgmEntityPersister) entityAndPersister.getValue() ) ) ) {
					logger.entityShouldHaveRevisionProperty( entityAndPersister.getKey() );
				}
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
}

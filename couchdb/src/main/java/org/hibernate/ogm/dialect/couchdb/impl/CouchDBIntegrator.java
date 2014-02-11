/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.couchdb.impl;

import java.util.Map.Entry;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.dialect.couchdb.backend.json.impl.Document;
import org.hibernate.ogm.logging.couchdb.impl.Log;
import org.hibernate.ogm.logging.couchdb.impl.LoggerFactory;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.service.impl.ConfigurationService;
import org.hibernate.persister.entity.EntityPersister;
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
			validateEntityMappings( (SessionFactoryImplementor) factory );
		}

		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			// nothing to do
		}

		private void validateEntityMappings(SessionFactoryImplementor factory) {
			if ( ! factory.getServiceRegistry().getService( ConfigurationService.class ).isOgmOn() ) {
				return;
			}

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

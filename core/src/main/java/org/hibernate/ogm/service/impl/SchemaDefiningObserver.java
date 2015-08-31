/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Triggers mapping validation and schema initialization after the SF has been bootstrapped.
 *
 * @author Gunnar Morling
 */
public class SchemaDefiningObserver implements SessionFactoryObserver {

	private final Database database;

	public SchemaDefiningObserver(Metadata metadata) {
		this.database = metadata.getDatabase();
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();

		SchemaDefiner schemaInitializer = registry.getService( SchemaDefiner.class );
		DefaultSchemaInitializationContext context = new DefaultSchemaInitializationContext(
				database,
				sessionFactoryImplementor
		);

		schemaInitializer.validateMapping( context );
		schemaInitializer.initializeSchema( context );
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
	}
}

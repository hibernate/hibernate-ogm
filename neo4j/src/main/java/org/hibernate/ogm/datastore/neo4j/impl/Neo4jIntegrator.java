/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.service.impl.ConfigurationService;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrates with the ORM engine by contributing a {@link SessionFactoryObserver} which checks the mapped entities for
 * Neo4j-specific requirements.
 * <p>
 * Note: This could be implemented using the {@code StartStoppable} contract once OGM-445 has been addressed
 *
 * @author Davide D'Alto
 */
public class Neo4jIntegrator implements Integrator {

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		addNeo4jObserverIfRequired( sessionFactory );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		addNeo4jObserverIfRequired( sessionFactory );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// nothing to do
	}

	private void addNeo4jObserverIfRequired(SessionFactoryImplementor sessionFactory) {
		if ( currentDialectIsNeo4j( sessionFactory ) ) {
			sessionFactory.addObserver( new SchemaCreator() );
		}
	}

	private boolean currentDialectIsNeo4j(SessionFactoryImplementor sessionFactoryImplementor) {
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();

		return registry.getService( ConfigurationService.class ).isOgmOn()
				&& ( registry.getService( DatastoreProvider.class ) instanceof Neo4jDatastoreProvider );
	}

	/**
	 * Adds the required constraints to the schema db
	 *
	 * @author Davide D'Alto
	 *
	 */
	private static class SchemaCreator implements SessionFactoryObserver {

		@Override
		public void sessionFactoryCreated(SessionFactory factory) {
			SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) factory;
			ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
			Neo4jDatastoreProvider provider = (Neo4jDatastoreProvider) registry.getService( DatastoreProvider.class );
			Set<String> sequences = sequenceGeneratorKeys( sessionFactoryImplementor, provider );
			provider.getSequenceGenerator().createUniqueConstraint( sequences );
		}

		private Set<String> sequenceGeneratorKeys(SessionFactoryImplementor sessionFactoryImplementor, Neo4jDatastoreProvider provider) {
			Set<String> sequences = new HashSet<String>();
			Map<String, EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters();
			for ( Map.Entry<String, EntityPersister> entry : entityPersisters.entrySet() ) {
				EntityPersister persister = entry.getValue();
				IdentifierGenerator identifierGenerator = persister.getIdentifierGenerator();
				if ( identifierGenerator instanceof OgmSequenceGenerator ) {
					OgmSequenceGenerator sequenceGenerator = (OgmSequenceGenerator) identifierGenerator;
					sequences.add( sequenceGenerator.generatorKey().toString() );
				}
			}
			return sequences;
		}

		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			// nothing to do
		}

	}
}

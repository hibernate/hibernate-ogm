/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.BaseSchemaDefiner;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Creates sequences in the Neo4j datastore.
 *
 * @author Gunnar Morling
 */
public class Neo4jSchemaDefiner extends BaseSchemaDefiner {

	@Override
	public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
		SessionFactoryImplementor sessionFactoryImplementor = factory;
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		Neo4jDatastoreProvider provider = (Neo4jDatastoreProvider) registry.getService( DatastoreProvider.class );
		Set<PersistentNoSqlIdentifierGenerator> sequences = getPersistentGenerators( sessionFactoryImplementor );
		JtaPlatform jtaPlatform = registry.getService( JtaPlatform.class );
		provider.getSequenceGenerator().createSequences( sequences, jtaPlatform.retrieveTransactionManager() );
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.transaction.TxManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jJtaPlatform extends AbstractJtaPlatform {

	@Override
	protected TransactionManager locateTransactionManager() {
		return neo4jDb().getDependencyResolver().resolveDependency( TxManager.class );
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return new UserTransactionImpl( neo4jDb() );
	}

	private GraphDatabaseAPI neo4jDb() {
		ServiceRegistry serviceRegistry = serviceRegistry();
		serviceRegistry = serviceRegistry
				.getService( SessionFactoryServiceRegistryFactory.class )
				.buildServiceRegistry( (SessionFactoryImplementor) null, (Configuration) null );
		Neo4jDatastoreProvider service = (Neo4jDatastoreProvider) serviceRegistry.getService( DatastoreProvider.class );
		return (GraphDatabaseAPI) service.getDataBase();
	}

}

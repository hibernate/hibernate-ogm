/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.impl;

import java.util.Properties;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.spi.MassIndexerFactory;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * {@link MassIndexerFactory} that can be used to register the {@link OgmMassIndexer} to Hibernate Search.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmMassIndexerFactory implements MassIndexerFactory {

	@Override
	public void initialize(Properties properties) {
	}

	@Override
	public MassIndexer createMassIndexer(SearchIntegrator searchFactory, SessionFactoryImplementor sessionFactory,
			Class<?>... entities) {
		return new OgmMassIndexer( sessionFactory.getServiceRegistry().getService( GridDialect.class ), searchFactory, sessionFactory, entities );
	}
}

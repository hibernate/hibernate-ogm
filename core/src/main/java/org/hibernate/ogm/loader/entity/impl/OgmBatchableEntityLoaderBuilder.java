/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.loader.impl.OgmLoader;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class OgmBatchableEntityLoaderBuilder implements BatchingEntityLoaderBuilder.BatchableEntityLoaderBuilder {
	@Override
	public BatchableEntityLoader buildLoader(OuterJoinLoadable persister, int batchSize, LockMode lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers) {
		// OGM does not really support lockMode,
		// factory is retrieved from the persister
		// TODO handle LoadQueryInfluenbcers to handle entityGraph
		return new OgmLoader( new OgmEntityPersister[] { (OgmEntityPersister) persister }, batchSize );
	}

	@Override
	public BatchableEntityLoader buildLoader(OuterJoinLoadable persister, int batchSize, LockOptions lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers) {
		// OGM does not really support lockMode,
		// factory is retrieved from the persister
		// TODO handle LoadQueryInfluenbcers to handle entityGraph
		return new OgmLoader( new OgmEntityPersister[] { (OgmEntityPersister) persister }, batchSize );
	}

	@Override
	public BatchableEntityLoader buildDynamicLoader(OuterJoinLoadable persister, int batchSize, LockMode lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers) {
		// OGM does not really support lockMode,
		// factory is retrieved from the persister
		// TODO handle LoadQueryInfluenbcers to handle entityGraph
		return new OgmLoader( new OgmEntityPersister[] { (OgmEntityPersister) persister }, batchSize );
	}

	@Override
	public BatchableEntityLoader buildDynamicLoader(OuterJoinLoadable persister, int batchSize, LockOptions lockOptions, SessionFactoryImplementor factory, LoadQueryInfluencers influencers) {
		// OGM does not really support lockMode,
		// factory is retrieved from the persister
		// TODO handle LoadQueryInfluenbcers to handle entityGraph
		return new OgmLoader( new OgmEntityPersister[] { (OgmEntityPersister) persister }, batchSize );
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.entity.EntityLoader;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;

/**
 * DO NOT CHANGE: this class is copied from ORM 5 and changes will be backported at some point
 * SPECIFIC CHANGES DONE:
 * - change getBuilder to use one implementation for now (to be undone)
 * - introduce the CoreBuilderContract
 * - change the buildLoader interface to optionally accept a BatchableEntityLoaderBuilder contract
 * - default CorBuildercontract implementation to the EntityLoader one.
 *
 *
 * The contract for building {@link UniqueEntityLoader} capable of performing batch-fetch loading.  Intention
 * is to build these instances, by first calling the static {@link #getBuilder}, and then calling the appropriate
 * {@link #buildLoader} method.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 *
 * @see org.hibernate.loader.BatchFetchStyle
 */
public abstract class BatchingEntityLoaderBuilder {
	// FIXME: Transform this method into a service initiator and have BatchingEntityLoader be a Service
	public static BatchingEntityLoaderBuilder getBuilder(SessionFactoryImplementor factory) {
		return PaddedBatchingEntityLoaderBuilder.INSTANCE;

//		switch ( factory.getSettings().getBatchFetchStyle() ) {
//			case PADDED: {
//				return PaddedBatchingEntityLoaderBuilder.INSTANCE;
//			}
//			case DYNAMIC: {
//				return DynamicBatchingEntityLoaderBuilder.INSTANCE;
//			}
//			default: {
//				return org.hibernate.loader.entity.plan.LegacyBatchingEntityLoaderBuilder.INSTANCE;
////				return LegacyBatchingEntityLoaderBuilder.INSTANCE;
//			}
//		}
	}

	/**
	 * Contract abstracting how the inner entity loader is build.
	 * Allow to inject the OGM one instead of the ORM one for example.
	 *
	 * @author Emmanuel Bernard
	 */
	public interface BatchableEntityLoaderBuilder {

		/**
		 * Builds a single entity loader for that set of parameters.
		 * Abstract away the various UniqueEntityLoader implementations that ORM or OGM might use.
		 */
		BatchableEntityLoader buildLoader(OuterJoinLoadable persister, int batchSize, LockMode lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers);

		/**
		 * Builds a single entity loader for that set of parameters.
		 * Abstract away the various UniqueEntityLoader implementations that ORM or OGM might use.
		 */
		BatchableEntityLoader buildLoader(OuterJoinLoadable persister, int batchSize, LockOptions lockOptions, SessionFactoryImplementor factory, LoadQueryInfluencers influencers);
	}


	/**
	 * Non LoadPlan based UniqueEntityLoader.
	 */
	//TODO: instances should be provided to the buildLoader method optionally
	public static class LegacyEntityLoaderBuilder implements BatchableEntityLoaderBuilder {

		static BatchableEntityLoaderBuilder INSTANCE = new LegacyEntityLoaderBuilder();
		private static class BatchableEntityLoaderAdaptor implements BatchableEntityLoader {
			private final EntityLoader delegate;

			public BatchableEntityLoaderAdaptor(EntityLoader delegate) {
				this.delegate = delegate;
			}

			@Override
			public List<?> loadEntityBatch(SessionImplementor session, Serializable[] ids, Type idType, Object optionalObject, String optionalEntityName, Serializable optionalId, EntityPersister persister, LockOptions lockOptions)
					throws HibernateException {
				return delegate.loadEntityBatch( session, ids, idType, optionalObject, optionalEntityName, optionalId, persister, lockOptions );
			}

			// rest of the methods inherited from UniqueEntityLoader

			@Override
			public Object load(Serializable id, Object optionalObject, SessionImplementor session)
					throws HibernateException {
				return delegate.load( id, optionalObject, session );
			}

			@Override
			public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
				return delegate.load( id, optionalObject, session, lockOptions );
			}
		}

		@Override
		public BatchableEntityLoader buildLoader(OuterJoinLoadable persister, int batchSize, LockMode lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers) {
			return new BatchableEntityLoaderAdaptor( new EntityLoader( persister, batchSize, lockMode, factory, influencers ) );
		}

		@Override
		public BatchableEntityLoader buildLoader(OuterJoinLoadable persister, int batchSize, LockOptions lockOptions, SessionFactoryImplementor factory, LoadQueryInfluencers influencers) {
			return new BatchableEntityLoaderAdaptor( new EntityLoader( persister, batchSize, lockOptions, factory, influencers ) );
		}
	}

	/**
	 * Builds a batch-fetch capable loader based on the given persister, lock-mode, etc.
	 *
	 * @param persister The entity persister
	 * @param batchSize The maximum number of ids to batch-fetch at once
	 * @param lockMode The lock mode
	 * @param factory The SessionFactory
	 * @param influencers Any influencers that should affect the built query
	 * @param innerEntityLoaderBuilder Builder of the entity loader receiving the subset of batches
	 *
	 * @return The loader.
	 */
	public UniqueEntityLoader buildLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
		// defaults to the legacy ORM EntityLoader
		innerEntityLoaderBuilder = innerEntityLoaderBuilder == null ? LegacyEntityLoaderBuilder.INSTANCE : innerEntityLoaderBuilder;
		if ( batchSize <= 1 ) {
			// no batching
			return buildNonBatchingLoader( persister, lockMode, factory, influencers, innerEntityLoaderBuilder );
		}
		return buildBatchingLoader( persister, batchSize, lockMode, factory, influencers, innerEntityLoaderBuilder );
	}

	public UniqueEntityLoader buildLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers) {
		return buildLoader( persister, batchSize, lockMode, factory, influencers, null );
	}

	protected UniqueEntityLoader buildNonBatchingLoader(
			OuterJoinLoadable persister,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
		return innerEntityLoaderBuilder.buildLoader( persister, 1, lockMode, factory, influencers );
	}

	protected abstract UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder);

	/**
	 * Builds a batch-fetch capable loader based on the given persister, lock-options, etc.
	 *
	 * @param persister The entity persister
	 * @param batchSize The maximum number of ids to batch-fetch at once
	 * @param lockOptions The lock options
	 * @param factory The SessionFactory
	 * @param influencers Any influencers that should affect the built query
	 * @param innerEntityLoaderBuilder Builder of the entity loader receiving the subset of batches
	 *
	 * @return The loader.
	 */
	public UniqueEntityLoader buildLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
		// defaults to the legacy ORM EntityLoader
		innerEntityLoaderBuilder = innerEntityLoaderBuilder == null ? LegacyEntityLoaderBuilder.INSTANCE : innerEntityLoaderBuilder;
		if ( batchSize <= 1 ) {
			// no batching
			return buildNonBatchingLoader( persister, lockOptions, factory, influencers, innerEntityLoaderBuilder );
		}
		return buildBatchingLoader( persister, batchSize, lockOptions, factory, influencers, innerEntityLoaderBuilder );
	}

	protected UniqueEntityLoader buildNonBatchingLoader(
			OuterJoinLoadable persister,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
		return innerEntityLoaderBuilder.buildLoader( persister, 1, lockOptions, factory, influencers );
	}

	protected abstract UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder);
}

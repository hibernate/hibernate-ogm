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
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.ogm.loader.impl.TupleBasedEntityLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * DO NOT CHANGE: this class is copied from ORM 5 and changes will be backported at some point
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
	// FIXME: Transform this method into a service initiator and have BatchingEntityLoader be a Service when migrating back to ORM
	public static BatchingEntityLoaderBuilder getBuilder(SessionFactoryImplementor factory) {
		// Today, the MultigetGridDialect interface does not offer support for prepared statement / fixed size abtch queries
		// Better use the dynamic batching in all cases until further notice
		// TODO should we raise an info on ignoring this setting
		// I don't see it being used much in practice and will probably annoy more than help
		return DynamicBatchingEntityLoaderBuilder.INSTANCE;
		// Unused implementation PaddedBatchingEntityLoaderBuilder.INSTANCE;
		// but will be gone when we migrate the code back to ORM
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
		<L extends BatchableEntityLoader & TupleBasedEntityLoader> L buildLoader(OuterJoinLoadable persister, int batchSize, LockMode lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers);

		/**
		 * Builds a single entity loader for that set of parameters.
		 * Abstract away the various UniqueEntityLoader implementations that ORM or OGM might use.
		 */
		<L extends BatchableEntityLoader & TupleBasedEntityLoader> L  buildLoader(OuterJoinLoadable persister, int batchSize, LockOptions lockOptions, SessionFactoryImplementor factory, LoadQueryInfluencers influencers);

		/**
		 * Builds a single entity loader for that set of parameters.
		 * The loader accept a non fixed sized batch load (dynamic loader).
		 * Abstract away the various UniqueEntityLoader implementations that ORM or OGM might use.
		 */
		BatchableEntityLoader buildDynamicLoader(OuterJoinLoadable persister, int batchSize, LockMode lockMode, SessionFactoryImplementor factory, LoadQueryInfluencers influencers);

		/**
		 * Builds a single entity loader for that set of parameters.
		 * The loader accept a non fixed sized batch load (dynamic loader).
		 * Abstract away the various UniqueEntityLoader implementations that ORM or OGM might use.
		 */
		BatchableEntityLoader buildDynamicLoader(OuterJoinLoadable persister, int batchSize, LockOptions lockOptions, SessionFactoryImplementor factory, LoadQueryInfluencers influencers);
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

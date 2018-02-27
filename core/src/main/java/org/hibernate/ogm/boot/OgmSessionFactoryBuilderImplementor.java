/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot;

import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * A {@link SessionFactoryBuilderImplementor} which creates {@link OgmSessionFactory} instances.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionFactoryBuilderImplementor extends SessionFactoryBuilderImplementor, OgmSessionFactoryBuilder {

	@Override
	OgmSessionFactoryBuilderImplementor applyValidatorFactory(Object validatorFactory);

	@Override
	OgmSessionFactoryBuilderImplementor applyBeanManager(Object beanManager);

	@Override
	OgmSessionFactoryBuilderImplementor applyName(String sessionFactoryName);

	@Override
	OgmSessionFactoryBuilderImplementor applyNameAsJndiName(boolean isJndiName);

	@Override
	OgmSessionFactoryBuilderImplementor applyAutoClosing(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyAutoFlushing(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyStatisticsSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyInterceptor(Interceptor interceptor);

	@Override
	OgmSessionFactoryBuilderImplementor addSessionFactoryObservers(SessionFactoryObserver... observers);

	@Override
	OgmSessionFactoryBuilderImplementor applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy);

	@Override
	OgmSessionFactoryBuilderImplementor addEntityNameResolver(EntityNameResolver... entityNameResolvers);

	@Override
	OgmSessionFactoryBuilderImplementor applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate);

	@Override
	OgmSessionFactoryBuilderImplementor applyIdentifierRollbackSupport(boolean enabled);

	@Override
	@Deprecated
	OgmSessionFactoryBuilderImplementor applyDefaultEntityMode(EntityMode entityMode);

	@Override
	OgmSessionFactoryBuilderImplementor applyNullabilityChecking(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyLazyInitializationOutsideTransaction(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory);

	@Override
	OgmSessionFactoryBuilderImplementor applyEntityTuplizer(
			EntityMode entityMode,
			Class<? extends EntityTuplizer> tuplizerClass);

	@Override
	OgmSessionFactoryBuilderImplementor applyMultiTableBulkIdStrategy(MultiTableBulkIdStrategy strategy);

	@Override
	OgmSessionFactoryBuilderImplementor applyBatchFetchStyle(BatchFetchStyle style);

	@Override
	OgmSessionFactoryBuilderImplementor applyDefaultBatchFetchSize(int size);

	@Override
	OgmSessionFactoryBuilderImplementor applyMaximumFetchDepth(int depth);

	@Override
	OgmSessionFactoryBuilderImplementor applyDefaultNullPrecedence(NullPrecedence nullPrecedence);

	@Override
	OgmSessionFactoryBuilderImplementor applyOrderingOfInserts(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyOrderingOfUpdates(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyMultiTenancyStrategy(MultiTenancyStrategy strategy);

	@Override
	OgmSessionFactoryBuilderImplementor applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver resolver);

	@Override
	@Deprecated
	OgmSessionFactoryBuilderImplementor applyJtaTrackingByThread(boolean enabled);

	@Override
	@Deprecated
	OgmSessionFactoryBuilderImplementor applyQuerySubstitutions(Map substitutions);

	@Override
	OgmSessionFactoryBuilderImplementor applyStrictJpaQueryLanguageCompliance(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyNamedQueryCheckingOnStartup(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applySecondLevelCacheSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyQueryCacheSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyQueryCacheFactory(QueryCacheFactory factory);

	@Override
	OgmSessionFactoryBuilderImplementor applyCacheRegionPrefix(String prefix);

	@Override
	OgmSessionFactoryBuilderImplementor applyMinimalPutsForCaching(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyStructuredCacheEntries(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyDirectReferenceCaching(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyAutomaticEvictionOfCollectionCaches(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyJdbcBatchSize(int size);

	@Override
	OgmSessionFactoryBuilderImplementor applyJdbcBatchingForVersionedEntities(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyScrollableResultsSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyResultSetsWrapping(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyGetGeneratedKeysSupport(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applyJdbcFetchSize(int size);

	@Override
	OgmSessionFactoryBuilderImplementor applyConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	@Override
	OgmSessionFactoryBuilderImplementor applySqlComments(boolean enabled);

	@Override
	OgmSessionFactoryBuilderImplementor applySqlFunction(String registrationName, SQLFunction sqlFunction);

	@Override
	OgmSessionFactory build();
}

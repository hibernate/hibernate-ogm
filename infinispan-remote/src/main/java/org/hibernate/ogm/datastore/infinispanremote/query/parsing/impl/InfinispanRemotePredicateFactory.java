/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.DisjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.hibernate.hql.ast.spi.predicate.PredicateFactory;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;
import org.hibernate.hql.ast.spi.predicate.RootPredicate;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteComparisonPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteConjunctionPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteDisjunctionPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteInPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteIsNullPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteLikePredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteNegationPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteRangePredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteRootPredicate;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Factory for {@link org.hibernate.hql.ast.spi.predicate.Predicate}s creating Infinispan server queries in form of
 * {@link StringBuilder}s.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemotePredicateFactory implements PredicateFactory<InfinispanRemoteQueryBuilder> {

	private final SessionFactoryImplementor sessionFactory;
	private final InfinispanRemotePropertyHelper propertyHelper;

	public InfinispanRemotePredicateFactory(SessionFactoryImplementor sessionFactory, InfinispanRemotePropertyHelper propertyHelper) {
		this.sessionFactory = sessionFactory;
		this.propertyHelper = propertyHelper;
	}

	@Override
	public RootPredicate<InfinispanRemoteQueryBuilder> getRootPredicate(String entityType) {
		EntityKeyMetadata entityKeyMetadata = ( (OgmEntityPersister) ( sessionFactory )
				.getMetamodel()
				.entityPersister( entityType ) )
				.getEntityKeyMetadata();

		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		InfinispanRemoteDatastoreProvider datastoreProvider = (InfinispanRemoteDatastoreProvider) serviceRegistry.getService( DatastoreProvider.class );

		return new InfinispanRemoteRootPredicate( entityKeyMetadata.getTable(), datastoreProvider.getProtobufPackageName() );
	}

	@Override
	public ComparisonPredicate<InfinispanRemoteQueryBuilder> getComparisonPredicate(String entityType, ComparisonPredicate.Type comparisonType, List<String> propertyPath, Object value) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath );
		return new InfinispanRemoteComparisonPredicate( columnName, comparisonType, value );
	}

	@Override
	public InPredicate<InfinispanRemoteQueryBuilder> getInPredicate(String entityType, List<String> propertyPath, List<Object> typedElements) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath );
		return new InfinispanRemoteInPredicate( columnName, typedElements );
	}

	@Override
	public RangePredicate<InfinispanRemoteQueryBuilder> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath );
		return new InfinispanRemoteRangePredicate( columnName, lowerValue, upperValue );
	}

	@Override
	public NegationPredicate<InfinispanRemoteQueryBuilder> getNegationPredicate() {
		return new InfinispanRemoteNegationPredicate();
	}

	@Override
	public DisjunctionPredicate<InfinispanRemoteQueryBuilder> getDisjunctionPredicate() {
		return new InfinispanRemoteDisjunctionPredicate();
	}

	@Override
	public ConjunctionPredicate<InfinispanRemoteQueryBuilder> getConjunctionPredicate() {
		return new InfinispanRemoteConjunctionPredicate();
	}

	@Override
	public LikePredicate<InfinispanRemoteQueryBuilder> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath );
		return new InfinispanRemoteLikePredicate( columnName, patternValue, '/' );
	}

	@Override
	public IsNullPredicate<InfinispanRemoteQueryBuilder> getIsNullPredicate(String entityType, List<String> propertyPath) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath );
		return new InfinispanRemoteIsNullPredicate( columnName );
	}
}

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
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl.InfinispanRemoteRootPredicate;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class InfinispanRemotePraticateFactory implements PredicateFactory<StringBuilder> {

	private final SessionFactoryImplementor sessionFactory;

	public InfinispanRemotePraticateFactory(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public RootPredicate<StringBuilder> getRootPredicate(String entityType) {
		EntityKeyMetadata entityKeyMetadata = ( (OgmEntityPersister) ( sessionFactory )
			.getMetamodel()
			.entityPersister( entityType ) )
			.getEntityKeyMetadata();

		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		InfinispanRemoteDatastoreProvider datastoreProvider = (InfinispanRemoteDatastoreProvider) serviceRegistry.getService( DatastoreProvider.class );

		return new InfinispanRemoteRootPredicate( entityKeyMetadata.getTable(), datastoreProvider.getProtobufPackageName() );
	}

	@Override
	public ComparisonPredicate<StringBuilder> getComparisonPredicate(String entityType, ComparisonPredicate.Type comparisonType, List<String> propertyPath, Object value) {
		return new InfinispanRemoteComparisonPredicate( propertyPath.get( 0 ), comparisonType, value );
	}

	@Override
	public InPredicate<StringBuilder> getInPredicate(String entityType, List<String> propertyPath, List<Object> typedElements) {
		return null;
	}

	@Override
	public RangePredicate<StringBuilder> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		return null;
	}

	@Override
	public NegationPredicate<StringBuilder> getNegationPredicate() {
		return null;
	}

	@Override
	public DisjunctionPredicate<StringBuilder> getDisjunctionPredicate() {
		return null;
	}

	@Override
	public ConjunctionPredicate<StringBuilder> getConjunctionPredicate() {
		return null;
	}

	@Override
	public LikePredicate<StringBuilder> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		return null;
	}

	@Override
	public IsNullPredicate<StringBuilder> getIsNullPredicate(String entityType, List<String> propertyPath) {
		return null;
	}
}

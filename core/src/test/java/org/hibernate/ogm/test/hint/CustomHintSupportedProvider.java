/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.hint;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.query.internal.ParameterMetadataImpl;

/**
 * The provider using for test custom (non-standart) hint passing
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class CustomHintSupportedProvider extends BaseDatastoreProvider {

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CustomHintSupportedProvider.Dialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return CustomHintSupportedParser.class;
	}

	public static class CustomHintSupportedParser implements QueryParserService {

		@Override
		public boolean supportsParameters() {
			return false;
		}

		@Override
		public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters) {
			return new CustomHintSupportedQueryParsingResult();
		}

		@Override
		public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString) {
			return new CustomHintSupportedQueryParsingResult();
		}
	}

	public static class CustomHintSupportedQueryParsingResult implements QueryParsingResult {

		@Override
		public Object getQueryObject() {
			return "";
		}

		@Override
		public List<String> getColumnNames() {
			return Collections.emptyList();
		}
	}

	public static class Dialect extends BaseGridDialect implements QueryableGridDialect<Serializable> {

		public static final String DIALECT_SPECIFIED_HINT = Dialect.class.getName() + ".hint";

		public Dialect(CustomHintSupportedProvider provider) {
		}

		@Override
		public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<Serializable> query, QueryParameters queryParameters, TupleContext tupleContext) {
			List<String> queryHints = queryParameters.getQueryHints();
			if ( queryHints.isEmpty() ) {
				throw new HibernateException( "The query must have a hints!" );
			}
			else {
				if (!queryHints.contains( DIALECT_SPECIFIED_HINT )) {
					throw new HibernateException( "The query not contains required hint! The query must contains hint \""+DIALECT_SPECIFIED_HINT+"\"!" );
				}
			}
			Tuple tuple = new Tuple();
			tuple.put( "id", UUID.randomUUID().toString() );
			tuple.put( "frequency", 1.0d );
			return CollectionHelper.newClosableIterator( Collections.singletonList( tuple ) );
		}

		@Override
		public int executeBackendUpdateQuery(BackendQuery<Serializable> query, QueryParameters queryParameters, TupleContext tupleContext) {
			return 0;
		}

		@Override
		public ParameterMetadataBuilder getParameterMetadataBuilder() {
			return new ParameterMetadataBuilder() {

				@Override
				public ParameterMetadataImpl buildParameterMetadata(String nativeQuery) {
					return new ParameterMetadataImpl( null, null );
				}
			};
		}

		@Override
		public Serializable parseNativeQuery(String nativeQuery) {
			return null;
		}

		@Override
		public Tuple getTuple(EntityKey key, OperationContext tupleContext) {
			return null;
		}

		@Override
		public Tuple createTuple(EntityKey key, OperationContext tupleContext) {
			return null;
		}

		@Override
		public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		}

		@Override
		public void removeTuple(EntityKey key, TupleContext tupleContext) {
		}

		@Override
		public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
			return null;
		}

		@Override
		public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
			return null;
		}

		@Override
		public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		}

		@Override
		public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		}

		@Override
		public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
			return false;
		}

		@Override
		public Number nextValue(NextValueRequest request) {
			return null;
		}

		@Override
		public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		}
	}
}

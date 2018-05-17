/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PathedPropertyReferenceSource;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.query.parsing.impl.KeepNamedParametersQueryRendererDelegate;

/**
 * Parser delegate which creates Infinispan Remote queries in form of {@link StringBuilder}s.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteQueryRendererDelegate extends KeepNamedParametersQueryRendererDelegate<InfinispanRemoteQueryBuilder, InfinispanRemoteQueryParsingResult> {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final SessionFactoryImplementor sessionFactory;
	private InfinispanRemoteQueryBuilder sortClause;

	public InfinispanRemoteQueryRendererDelegate(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames,
			InfinispanRemotePropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super(
				propertyHelper, entityNames, getSingleEntityQueryBuilder( sessionFactory, propertyHelper ),
				namedParameters
		);
		this.sessionFactory = sessionFactory;
	}

	private static SingleEntityQueryBuilder<InfinispanRemoteQueryBuilder> getSingleEntityQueryBuilder(SessionFactoryImplementor sessionFactory,
			InfinispanRemotePropertyHelper propertyHelper) {
		return SingleEntityQueryBuilder.getInstance( new InfinispanRemotePredicateFactory( sessionFactory, propertyHelper ), propertyHelper );
	}

	@Override
	public InfinispanRemoteQueryParsingResult getResult() {
		OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) ( sessionFactory )
				.getMetamodel()
				.entityPersister( targetTypeName );

		String table = ogmEntityPersister
				.getEntityKeyMetadata()
				.getTable();

		InfinispanRemoteQueryBuilder queryBuilder;

		if ( projections.isEmpty() ) {
			queryBuilder = builder.build();
		}
		else {
			queryBuilder = createFromProjection();
			queryBuilder.append( builder.build() );
		}

		applyInheritanceStrategy( ogmEntityPersister, queryBuilder );

		if ( sortClause != null ) {
			queryBuilder.append( sortClause );
		}

		return new InfinispanRemoteQueryParsingResult( queryBuilder, table, projections );
	}

	private void applyInheritanceStrategy(OgmEntityPersister entityPersister, InfinispanRemoteQueryBuilder queryBuilder) {
		String discriminatorColumnName = entityPersister.getDiscriminatorColumnName();

		if ( discriminatorColumnName != null ) {
			addConditionOnDiscriminatorValue( entityPersister, queryBuilder, discriminatorColumnName );
		}
		else if ( entityPersister.hasSubclasses() ) {
			Set<String> subclassEntityNames = entityPersister.getEntityMetamodel().getSubclassEntityNames();
			throw LOG.queriesOnPolymorphicEntitiesAreNotSupportedWithTablePerClass( "Infinispan Server", subclassEntityNames );
		}
	}

	private void addConditionOnDiscriminatorValue(OgmEntityPersister entityPersister, InfinispanRemoteQueryBuilder queryBuilder, String discriminatorColumnName) {
		Object discriminatorValue = entityPersister.getDiscriminatorValue();
		Set<String> subclassEntityNames = entityPersister.getEntityMetamodel().getSubclassEntityNames();

		if ( queryBuilder.hasWhere() ) {
			queryBuilder.append( " and " );
		}
		else {
			queryBuilder.append( " where " );
		}

		queryBuilder.append( discriminatorColumnName );

		if ( subclassEntityNames.size() == 1 ) {
			queryBuilder.append( " = " );
			queryBuilder.appendValue( discriminatorValue );
		}
		else {
			List<Object> discriminatorValues = new ArrayList<>();
			for ( String subclass : subclassEntityNames ) {
				OgmEntityPersister subclassPersister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( subclass );
				discriminatorValues.add( subclassPersister.getDiscriminatorValue() );
			}

			queryBuilder.append( " in (" );
			queryBuilder.appendValues( discriminatorValues );
			queryBuilder.append( ")" );
		}
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		if ( status == Status.DEFINING_SELECT ) {
			PathedPropertyReferenceSource last = propertyPath.getLastNode();

			if ( !last.isAlias() ) {
				String columnName = getColumnName( propertyPath );
				projections.add( columnName );
			}
		}
		else {
			this.propertyPath = propertyPath;
		}
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		String columnName = getColumnName( propertyPath );
		if ( sortClause == null ) {
			sortClause = new InfinispanRemoteQueryBuilder( " order by " );
		}
		else {
			sortClause.append( ", " );
		}

		appendSortField( isAscending, columnName );
	}

	private String getColumnName(PropertyPath propertyPath) {
		return ( (InfinispanRemotePropertyHelper) propertyHelper ).getColumnName( targetTypeName, propertyPath.getNodeNamesWithoutAlias() );
	}

	private void appendSortField(boolean isAscending, String columnName) {
		sortClause.append( columnName );

		if ( isAscending ) {
			sortClause.append( " asc" );
		}
		else {
			sortClause.append( " desc" );
		}
	}

	private InfinispanRemoteQueryBuilder createFromProjection() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder( "select " );

		builder.appendStrings( projections );
		builder.append( " " );
		return builder;
	}

	@Override
	protected Object getObjectParameter(String comparativePredicate) {
		return new InfinispanRemoteQueryParameter( comparativePredicate.substring( 1 ) );
	}
}

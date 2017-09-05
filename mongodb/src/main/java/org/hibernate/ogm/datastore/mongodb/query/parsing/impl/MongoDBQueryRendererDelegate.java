/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.StringHelper;

import org.bson.Document;

/**
 * Parser delegate which creates MongoDB queries in form of {@link Document}s.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryRendererDelegate extends SingleEntityQueryRendererDelegate<Document, MongoDBQueryParsingResult> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final SessionFactoryImplementor sessionFactory;
	private final MongoDBPropertyHelper propertyHelper;
	private Document orderBy;
	/*
	 * The fields for which needs to be aggregated using $unwind when running the query
	 */
	private List<String> unwinds;

	public MongoDBQueryRendererDelegate(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames, MongoDBPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super(
				propertyHelper,
				entityNames,
				SingleEntityQueryBuilder.getInstance( new MongoDBPredicateFactory( propertyHelper ), propertyHelper ),
				namedParameters );

		this.sessionFactory = sessionFactory;
		this.propertyHelper = propertyHelper;
	}

	@Override
	public MongoDBQueryParsingResult getResult() {
		OgmEntityPersister entityPersister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( targetType );

		Document query = appendDiscriminatorClause( entityPersister, builder.build() );

		return new MongoDBQueryParsingResult(
				targetType,
				entityPersister.getTableName(),
				query,
				getProjectionDocument(),
				orderBy,
				unwinds );
	}

	private Document appendDiscriminatorClause(OgmEntityPersister entityPersister, Document query) {
		String discriminatorColumnName = entityPersister.getDiscriminatorColumnName();
		if ( discriminatorColumnName != null ) {
			// InheritanceType.SINGLE_TABLE
			Document discriminatorFilter = createDiscriminatorFilter( entityPersister, discriminatorColumnName );

			if ( query.keySet().isEmpty() ) {
				return discriminatorFilter;
			}
			else {
				return new Document( "$and", Arrays.asList( query, discriminatorFilter ) );
			}
		}
		else if ( entityPersister.hasSubclasses() ) {
			// InheritanceType.TABLE_PER_CLASS
			@SuppressWarnings("unchecked")
			Set<String> subclassEntityNames = entityPersister.getEntityMetamodel().getSubclassEntityNames();
			throw log.queriesOnPolymorphicEntitiesAreNotSupportedWithTablePerClass( "MongoDB", subclassEntityNames );
		}
		return query;
	}

	private Document createDiscriminatorFilter(OgmEntityPersister entityPersister, String discriminatorColumnName) {
		final Object discriminatorValue = entityPersister.getDiscriminatorValue();
		Document discriminatorFilter = null;
		@SuppressWarnings("unchecked")
		Set<String> subclassEntityNames = entityPersister.getEntityMetamodel().getSubclassEntityNames();
		if ( subclassEntityNames.size() == 1 ) {
			discriminatorFilter = new Document( discriminatorColumnName, discriminatorValue );
		}
		else {
			Set<Object> discriminatorValues = new HashSet<>();
			discriminatorValues.add( discriminatorValue );
			for ( String subclass : subclassEntityNames ) {
				OgmEntityPersister subclassPersister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( subclass );
				Object subDiscriminatorValue = subclassPersister.getDiscriminatorValue();
				discriminatorValues.add( subDiscriminatorValue );
			}
			discriminatorFilter = new Document( discriminatorColumnName, new Document( "$in", discriminatorValues ) );
		}
		return discriminatorFilter;
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		if ( status == Status.DEFINING_SELECT ) {
			List<String> pathWithoutAlias = resolveAlias( propertyPath );
			if ( propertyHelper.isSimpleProperty( pathWithoutAlias ) ) {
				projections.add( propertyHelper.getColumnName( targetTypeName, propertyPath.getNodeNamesWithoutAlias() ) );
			}
			else if ( propertyHelper.isNestedProperty( pathWithoutAlias ) ) {
				if ( propertyHelper.isEmbeddedProperty( targetTypeName, pathWithoutAlias ) ) {
					String columnName = propertyHelper.getColumnName( targetTypeName, pathWithoutAlias );
					projections.add( columnName );
					List<String> associationPath = propertyHelper.findAssociationPath( targetTypeName, pathWithoutAlias );
					// Currently, it is possible to nest only one association inside an embedded
					if ( associationPath != null ) {
						if ( unwinds == null ) {
							unwinds = new ArrayList<String>();
						}
						String field = StringHelper.join( associationPath, "." );
						if ( !unwinds.contains( field ) ) {
							unwinds.add( field );
						}
					}
				}
				else {
					throw new UnsupportedOperationException( "Selecting associated properties not yet implemented." );
				}
			}
		}
		else {
			this.propertyPath = propertyPath;
		}
	}

	/**
	 * Returns the projection columns of the parsed query in form of a {@code Document} as expected by MongoDB.
	 *
	 * @return a {@code Document} representing the projections of the query
	 */
	private Document getProjectionDocument() {
		if ( projections.isEmpty() ) {
			return null;
		}

		Document projectionDocument = new Document();

		for ( String projection : projections ) {
			projectionDocument.put( projection, 1 );
		}

		return projectionDocument;
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		if ( orderBy == null ) {
			orderBy = new Document();
		}

		String columnName = propertyHelper.getColumnName( targetType, propertyPath.getNodeNamesWithoutAlias() );

		// Document is essentially a LinkedHashMap, so in case of several sort keys they'll be evaluated in the
		// order they're inserted here, which is the order within the original statement
		orderBy.put( columnName, isAscending ? 1 : -1 );
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Parser delegate which creates MongoDB queries in form of {@link DBObject}s.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryRendererDelegate extends SingleEntityQueryRendererDelegate<DBObject, MongoDBQueryParsingResult> {

	private final SessionFactoryImplementor sessionFactory;
	private final MongoDBPropertyHelper propertyHelper;
	private DBObject orderBy;

	public MongoDBQueryRendererDelegate(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames, MongoDBPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super(
				entityNames,
				SingleEntityQueryBuilder.getInstance( new MongoDBPredicateFactory( propertyHelper ), propertyHelper ),
				namedParameters );

		this.sessionFactory = sessionFactory;
		this.propertyHelper = propertyHelper;
	}

	@Override
	public MongoDBQueryParsingResult getResult() {
		OgmEntityPersister entityPersister = (OgmEntityPersister) sessionFactory.getEntityPersister( targetType.getName() );

		return new MongoDBQueryParsingResult(
				targetType,
				entityPersister.getTableName(),
				builder.build(),
				getProjectionDBObject(),
				orderBy
		);
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		if ( status == Status.DEFINING_SELECT ) {
			//currently only support selecting non-nested properties (either qualified or unqualified)
			if ( ( propertyPath.getNodes().size() == 1 && !propertyPath.getLastNode().isAlias() )
					|| ( propertyPath.getNodes().size() == 2 && propertyPath.getNodes().get( 0 ).isAlias() ) ) {
				projections.add( propertyHelper.getColumnName( targetTypeName, propertyPath.asStringPathWithoutAlias() ) );
			}
			else if ( propertyPath.getNodes().size() > 2 && propertyPath.getNodes().get( 0 ).isAlias() ) {
				if ( propertyHelper.isEmbedddedProperty( targetTypeName, propertyPath ) ) {
					projections.add( propertyHelper.getColumnName( targetTypeName, propertyPath.asStringPathWithoutAlias() ) );
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
	 * Returns the projection columns of the parsed query in form of a {@code DBObject} as expected by MongoDB.
	 *
	 * @return a {@code DBObject} representing the projections of the query
	 */
	private DBObject getProjectionDBObject() {
		if ( projections.isEmpty() ) {
			return null;
		}

		DBObject projectionDBObject = new BasicDBObject();

		for ( String projection : projections ) {
			projectionDBObject.put( projection, 1 );
		}

		return projectionDBObject;
	}

	@Override
	protected void addSortField(PropertyPath propertyPath, String collateName, boolean isAscending) {
		if ( orderBy == null ) {
			orderBy = new BasicDBObject();
		}

		String columnName = propertyHelper.getColumnName( targetType, propertyPath.asStringPathWithoutAlias() );

		// BasicDBObject is essentially a LinkedHashMap, so in case of several sort keys they'll be evaluated in the
		// order they're inserted here, which is the order within the original statement
		orderBy.put( columnName, isAscending ? 1 : -1 );
	}
}

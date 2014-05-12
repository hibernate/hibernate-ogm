/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.Map;

import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Parser delegate which creates MongoDB queries in form of {@link DBObject}s.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryRendererDelegate extends SingleEntityQueryRendererDelegate<DBObject, MongoDBQueryParsingResult> {

	private final MongoDBPropertyHelper propertyHelper;

	public MongoDBQueryRendererDelegate(EntityNamesResolver entityNames, MongoDBPropertyHelper propertyHelper, Map<String, Object> namedParameters) {
		super(
				entityNames,
				SingleEntityQueryBuilder.getInstance( new MongoDBPredicateFactory( propertyHelper ), propertyHelper ),
				namedParameters );

		this.propertyHelper = propertyHelper;
	}

	@Override
	public MongoDBQueryParsingResult getResult() {
		return new MongoDBQueryParsingResult( targetType, builder.build(), getProjectionDBObject() );
	}

	@Override
	public void setPropertyPath(PropertyPath propertyPath) {
		if ( status == Status.DEFINING_SELECT ) {
			//currently only support selecting non-nested properties (either qualified or unqualified)
			if ( ( propertyPath.getNodes().size() == 1 && !propertyPath.getLastNode().isAlias() )
					|| ( propertyPath.getNodes().size() == 2 && propertyPath.getNodes().get( 0 ).isAlias() ) ) {
				projections.add( propertyHelper.getColumnName( targetTypeName, propertyPath.asStringPathWithoutAlias() ) );
			}
			else if ( propertyPath.getNodes().size() != 1 ) {
				throw new UnsupportedOperationException( "Selecting nested/associated properties not yet implemented." );
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
}

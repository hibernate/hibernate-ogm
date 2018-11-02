/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import org.bson.Document;
import org.hibernate.hql.ast.origin.hql.resolve.path.AggregationPropertyPath;

/**
 * Class representing aggregation operation. Used for HQL.
 *
 * @author Aleksandr Mylnikov
 */
public class Aggregation {

	private final String propertyPath;

	private final AggregationPropertyPath.Type aggregationType;

	public Aggregation(String propertypath, AggregationPropertyPath.Type aggregationType) {
		this.propertyPath = propertypath;
		this.aggregationType = aggregationType;
	}

	public Document getCount() {
		if ( aggregationType == AggregationPropertyPath.Type.COUNT ) {
			Document count = new Document();
			count.append( "$count", "n" );
			return count;
		}
		return null;
	}

	public Document getAsDocument() {
		String operationAsString;
		switch ( aggregationType ) {
			case SUM:
				operationAsString = "$sum";
				break;
			case MAX:
				operationAsString = "$max";
				break;
			case MIN:
				operationAsString = "$min";
				break;
			case AVG:
				operationAsString = "$avg";
				break;
				default:
					return null;
		}
		Document group = new Document();
		group.append( "_id", "''" );
		group.append( "n", new Document().append( operationAsString, "$" + propertyPath + "" ) );
		return group;
	}

	public String getAggregationProjection() {
		return "n";
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public AggregationPropertyPath.Type getAggregationType() {
		return aggregationType;
	}
}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.bson.Document;
import org.hibernate.hql.ast.origin.hql.resolve.path.AggregationPropertyPath;
import org.hibernate.hql.ast.origin.hql.resolve.path.AggregationPropertyPath.Type;

/**
 * Render the aggregation operation of an HQL query into the corresponding {@link Document}.
 * <p>
 * For example, a {@code COUNT(col)} will return:
 * <pre>{@code
 *    {$count: "col"}
 * }</pre>
 * <p>
 * For a {@code SUM(col)} operation:
 * <p>
 * {$group: {
 *     {'_id': '',
 *     {$sum: $col }}
 * }}
 *
 * @see #asDocumentPipeline()
 * @author Davide D'Alto
 * @author Aleksandr Mylnikov
 */
public class AggregationRenderer {

	private static final String PROJECTION_FIELD = "n";
	private final AggregationPropertyPath.Type aggregationType;
	private final String propertyPath;
	private final String aggregationTypeOperator;

	public AggregationRenderer(AggregationPropertyPath.Type aggregationType) {
		this( null, aggregationType );
	}

	public AggregationRenderer(String propertypath, AggregationPropertyPath.Type aggregationType) {
		this.propertyPath = propertypath != null ? "$" + propertypath : null;
		// Type.COUNT_DISTINCT -> $count, Type.COUNT -> $count. We don't care about the other cases for now.
		this.aggregationTypeOperator = "$" + aggregationType.name().toLowerCase( Locale.ROOT ).split( "_" )[0];
		this.aggregationType = aggregationType;
	}

	public List<Document> asDocumentPipeline() {
		if ( aggregationType == Type.COUNT_DISTINCT && propertyPath != null ) {
			return getDistinctCount();
		}
		else if ( aggregationType == Type.COUNT ) {
			return Arrays.asList( getCount() );
		}
		else {
			return Arrays.asList( getGroup() );
		}
	}

	private Document getGroup() {
		Document groupOptions = new Document();
		groupOptions.append( "_id", null );
		groupOptions.append( PROJECTION_FIELD, new Document().append( aggregationTypeOperator, propertyPath ) );
		Document group = new Document();
		group.append( "$group", groupOptions );
		return group;
	}

	private List<Document> getDistinctCount() {
		List<Document> distinctCount = new LinkedList<>();
		distinctCount.add( new Document().append( "$group", new Document().append( "_id", propertyPath ) ) );
		distinctCount.add( getCount() );
		return distinctCount;
	}

	private Document getCount() {
		Document count = new Document();
		count.append( aggregationTypeOperator, PROJECTION_FIELD );
		return count;
	}

	public String getAggregationProjection() {
		return PROJECTION_FIELD;
	}
}

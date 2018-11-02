/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

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
 * @see #asDocument()
 * @author Davide D'Alto
 * @author Aleksandr Mylnikov
 */
public class AggregationRenderer {

	private static final String PROJECTION_FIELD = "n";
	private final boolean isCount;
	private final String propertyPath;
	private final String aggregationTypeOperator;

	public AggregationRenderer(AggregationPropertyPath.Type aggregationType) {
		this( null, aggregationType );
	}

	public AggregationRenderer(String propertypath, AggregationPropertyPath.Type aggregationType) {
		validate( aggregationType );
		this.propertyPath = "$" + propertypath;
		this.aggregationTypeOperator = "$" + aggregationType.name().toLowerCase( Locale.ROOT );
		this.isCount = aggregationType == Type.COUNT;
	}

	private void validate(Type aggregationType) {
		if ( aggregationType == Type.COUNT_DISTINCT ) {
			throw new UnsupportedOperationException( "Currently OGM does not support count distinct operations" );
		}
	}

	public Document asDocument() {
		if ( isCount ) {
			return getCount();
		}
		else {
			return getGroup();
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

	private Document getCount() {
		Document count = new Document();
		count.append( aggregationTypeOperator, PROJECTION_FIELD );
		return count;
	}

	public String getAggregationProjection() {
		return PROJECTION_FIELD;
	}
}

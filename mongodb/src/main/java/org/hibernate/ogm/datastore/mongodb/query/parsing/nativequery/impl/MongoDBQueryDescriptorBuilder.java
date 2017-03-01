/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.hibernate.ogm.util.impl.StringHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;

/**
 * Builder for {@link MongoDBQueryDescriptor}s.
 *
 * @author Gunnar Morling
 * @author Thorsten Möller
 * @author Guillaume Smet
 */
public class MongoDBQueryDescriptorBuilder {

	private String collection;
	private Operation operation;
	/**
	 * Overloaded to be the 'document' for a FINDANDMODIFY query (which is a kind of criteria),
	 */
	private String criteria;
	private String projection;
	private String orderBy;

	/**
	 * Distinct Operation will be performed on this field
	 */
	private String distinctFieldName;

	/**
	 * Collation document
	 */
	private String collation;

	/**
	 * Document or array of documents to insert/update for an INSERT/UPDATE query.
	 */
	private String updateOrInsert;
	private String options;

	private Set<Integer> parsed = new HashSet<>();
	private List<DBObject> pipeline = new ArrayList<>();

	private Deque<StackedOperation> stack = new ArrayDeque<>();

	public static class PipelineOperation {
		private final String command;
		private final String value;

		public PipelineOperation(String command, String value) {
			this.command = command;
			this.value = value;
		}

		public String getCommand() {
			return command;
		}

		public String getValue() {
			return value;
		}
	}

	private static class StackedOperation {
		private final int index;
		private final String operation;

		public StackedOperation(int index, String operation) {
			this.index = index;
			this.operation = operation;
		}

		public int getIndex() {
			return index;
		}

		public String getOperation() {
			return operation;
		}
	}

	public boolean setCollection(String collection) {
		this.collection = collection.trim();
		return true;
	}

	public boolean setOperation(Operation operation) {
		this.operation = operation;
		return true;
	}

	public boolean setCriteria(String criteria) {
		this.criteria = criteria;
		return true;
	}

	public boolean setProjection(String projection) {
		this.projection = projection;
		return true;
	}

	public boolean setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return true;
	}

	public boolean setOptions(String options) {
		this.options = options;
		return true;
	}

	public boolean setUpdateOrInsert(String updateOrInsert) {
		this.updateOrInsert = updateOrInsert;
		return true;
	}

	public boolean setDistinctFieldName(String fieldName) {
		this.distinctFieldName = fieldName.trim();
		return true;
	}

	public boolean setCollation(String collation) {
		this.collation = collation;
		return true;
	}

	public MongoDBQueryDescriptor build() {
		if ( operation == Operation.DISTINCT ) {
			return new MongoDBQueryDescriptor( collection, operation,parse( criteria ), parseCollation( collation ), distinctFieldName );
		}
		else if ( operation != Operation.AGGREGATE_PIPELINE ) {
			return new MongoDBQueryDescriptor(
				collection,
				operation,
				parse( criteria ),
				parse( projection ),
				parse( orderBy ),
				parse( options ),
				parse( updateOrInsert ),
				null );
		}
		return new MongoDBQueryDescriptor( collection, operation, pipeline );
	}

	/**
	 * Currently, there is no way to parse an array while supporting BSON and JSON extended syntax. So for now, we build
	 * an object from the JSON string representing an array or an object, parse this object then extract the array/object.
	 *
	 * See <a href="https://jira.mongodb.org/browse/JAVA-2186">https://jira.mongodb.org/browse/JAVA-2186</a>.
	 *
	 * @param json a JSON string representing an array or an object
	 * @return a {@code DBObject} representing the array ({@code BasicDBList}) or the object ({@code BasicDBObject})
	 */
	private DBObject parse(String json) {
		return (DBObject) parseAsObject( json );
	}

	private static Object parseAsObject(String json) {
		if ( StringHelper.isNullOrEmptyString( json ) ) {
			return null;
		}
		BasicDBObject object = BasicDBObject.parse( "{ 'json': " + json + "}" );
		return object.get( "json" );
	}

	/**
	 * Collation document parser
	 * @param json
	 * @return
	 */
	private static Collation parseCollation(String json)  {
		DBObject dbObject = ((DBObject) parseAsObject( json ));
		Collation collation = null;

		if(dbObject != null) {
			dbObject = ( DBObject ) dbObject.get( "collation" );
			if ( dbObject != null ) {
				collation = Collation.builder()
						.locale( (String) dbObject.get( "locale" ) )
						.caseLevel( (Boolean) dbObject.get( "caseLevel" ) )
						.numericOrdering( (Boolean) dbObject.get( "numericOrdering" ) )
						.backwards( (Boolean) dbObject.get( "backwards" ) )
						.collationCaseFirst( dbObject.get( "caseFirst" ) == null ? null : CollationCaseFirst.fromString( (String) dbObject.get( "caseFirst" ) ) )
						.collationStrength( dbObject.get( "strength" ) == null ? null : CollationStrength.fromInt( (Integer) dbObject.get( "strength" ) ) )
						.collationAlternate( dbObject.get( "alternate" ) == null ? null : CollationAlternate.fromString( (String) dbObject.get( "alternate" ) ) )
						.collationMaxVariable( dbObject.get( "maxVariable" ) == null ? null : CollationMaxVariable.fromString( (String) dbObject.get( "maxVariable" ) ) )
						.build();
			}

		}
		return collation;
	}

	private static DBObject operation(StackedOperation operation, String value) {
		DBObject stage = new BasicDBObject();
		stage.put( normalize( operation ), parseAsObject( value ) );
		return stage;
	}

	public boolean addPipeline(StackedOperation operation, String value) {
		if ( !parsed.contains( operation.getIndex() ) ) {
			parsed.add( operation.getIndex() );
			pipeline.add( operation( operation, value ) );
		}
		return true;
	}

	private static String normalize(StackedOperation operation) {
		return operation.getOperation().replaceAll( "'", "" ).replaceAll( "\"", "" ).trim();
	}

	public boolean push(int index, String match) {
		stack.push( new StackedOperation( index, match ) );
		return true;
	}

	public StackedOperation pop() {
		return stack.pop();
	}
}

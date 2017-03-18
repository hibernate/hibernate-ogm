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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.hibernate.ogm.util.impl.StringHelper;

import org.bson.Document;

/**
 * Builder for {@link MongoDBQueryDescriptor}s.
 *
 * @author Gunnar Morling
 * @author Thorsten Möller
 * @author Guillaume Smet
 */
public class MongoDBQueryDescriptorBuilder {

	private static final Map<Operation,QueryDescriptor> OPERATION_QUERY_DESCRIPTOR_MAP;

	static {
		EnumMap<Operation, QueryDescriptor> descriptorEnumMap = new EnumMap<>( Operation.class );

		QueryDescriptor defaultDesc = new DefaultQueryDescriptor();
		for ( Operation op : Operation.values() ) {
			descriptorEnumMap.put( op, defaultDesc );
		}
		// operations with non default query description
		descriptorEnumMap.put( Operation.AGGREGATE_PIPELINE, new AggregatePipelineQueryDescriptor() );
		descriptorEnumMap.put( Operation.DISTINCT, new DistinctQueryDescriptor() );
		descriptorEnumMap.put( Operation.INSERTMANY, new InsertManyQueryDescriptor() );
		descriptorEnumMap.put( Operation.INSERT, new InsertQueryDescriptor() );
		OPERATION_QUERY_DESCRIPTOR_MAP = descriptorEnumMap;
	}

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
	private List<Document> pipeline = new ArrayList<>();

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
		return OPERATION_QUERY_DESCRIPTOR_MAP
				.get( operation )
				.build( collection,operation,criteria,projection,orderBy,options,updateOrInsert,
						pipeline,distinctFieldName,collation );
	}

	/**
	 * parse JSON
	 * @param json
	 * @return
	 * @see <a href="http://stackoverflow.com/questions/34436952/json-parse-equivalent-in-mongo-driver-3-x-for-java"> JSON.parse equivalent</a>
	 */
	private static Object parseAsObject(String json) {
		if ( StringHelper.isNullOrEmptyString( json ) ) {
			return null;
		}
		Document object = Document.parse( "{ 'json': " + json + "}" );
		return object.get( "json" );
	}

	private static Document operation(StackedOperation operation, String value) {
		Document stage = new Document();
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

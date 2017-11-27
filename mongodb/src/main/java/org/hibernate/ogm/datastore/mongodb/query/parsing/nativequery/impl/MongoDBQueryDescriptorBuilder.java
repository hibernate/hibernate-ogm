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

import org.bson.Document;
import org.bson.json.JsonParseException;

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
	 * Helper fields for recording the states of the parser
	 */
	private boolean isCliQuery;
	private String invalidJsonParameter;
	private boolean areParametersValid;
	private String operationName;
	private boolean isQueryValid;

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

	private String mapFunction;
	private String reduceFunction;

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

	public boolean isCliQuery() {
		return isCliQuery;
	}

	public boolean setCliQuery(boolean isCliQuery) {
		this.isCliQuery = isCliQuery;
		return true;
	}

	public boolean setInvalidJsonParameter(String invalidJsonParameter) {
		this.invalidJsonParameter = invalidJsonParameter;
		return true;
	}

	public boolean setParametersValid(boolean areParametersValid) {
		this.areParametersValid = areParametersValid;
		return true;
	}

	public boolean setOperationName(String operationName) {
		this.operationName = operationName;
		return true;
	}

	public boolean setQueryValid(boolean isQueryValid) {
		this.isQueryValid = isQueryValid;
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

	public boolean setMapFunction(String mapFunction) {
		this.mapFunction = mapFunction;
		return true;
	}

	public boolean setReduceFunction(String reduceFunction) {
		this.reduceFunction = reduceFunction;
		return true;
	}

	public MongoDBQueryDescriptor build() throws NativeQueryParseException {
		if ( !isQueryValid ) {
			// the input is CLI query: it starts with 'db'
			if ( isCliQuery ) {
				// db ???
				if ( collection == null ) {
					throw new NativeQueryParseException( "Cli query should match the format db.collection.operation(<arguments>)" );
				}
				// db . collection ???
				if ( operation == null && operationName == null ) {
					throw new NativeQueryParseException( "Cli query should match the format db.collection.operation(<arguments>)" );
				}
				// db . collection . UNSUPPORTED
				if ( operation == null ) {
					throw new NativeQueryParseException( "Operation '" + operationName + "' is unsupported" );
				}
				// db . collection . operation ( ???
				if ( invalidJsonParameter != null ) {
					try {
						Document.parse( invalidJsonParameter );
					}
					catch (JsonParseException e ) {
						throw new NativeQueryParseException( e );
					}
				}
				// db . collection . operation ( ???
				if ( !areParametersValid ) {
					throw new NativeQueryParseException( "Parameters are invalid for " + operationName );
				}
				else {
					throw new NativeQueryParseException( "Missing ')'" );
				}
			}
			// the input is not a json object: might be error in { ... } or does not start with '{'
			try {
				Document.parse( invalidJsonParameter );
			}
			catch (JsonParseException e ) {
				throw new NativeQueryParseException( e );
			}
		}

		//@todo refactor the spaghetti!
		if ( operation != Operation.AGGREGATE_PIPELINE ) {
			MongoDBQueryDescriptor descriptor = null;

			if ( operation == Operation.INSERTMANY ) {
				// must be array
				@SuppressWarnings("unchecked")
				List<Document> documents = (List<Document>) parseAsObject( updateOrInsert );
				descriptor = new MongoDBQueryDescriptor(
						collection,
						operation,
						parse( criteria ),
						parse( projection ),
						parse( orderBy ),
						parse( options ),
						null,
						documents,
						null,
						null,
						null,
						null
				);
			}
			else if ( operation == Operation.INSERT ) {
				//can be document or array
				Object anyDocs = parseAsObject( updateOrInsert );
				if ( anyDocs instanceof List ) {
					//this is array
					descriptor = new MongoDBQueryDescriptor(
							collection,
							operation,
							parse( criteria ),
							parse( projection ),
							parse( orderBy ),
							parse( options ),
							null,
							(List<Document>) anyDocs,
							null,
							null,
							null,
							null
					);
				}
				else {
					//this is one document
					descriptor = new MongoDBQueryDescriptor(
							collection,
							operation,
							parse( criteria ),
							parse( projection ),
							parse( orderBy ),
							parse( options ),
							(Document) anyDocs,
							null,
							null,
							null,
							null,
							null
					);
				}
			}
			else {
				descriptor = new MongoDBQueryDescriptor(
						collection,
						operation,
						parse( criteria ),
						parse( projection ),
						parse( orderBy ),
						parse( options ),
						parse( updateOrInsert ),
						null,
						null,
						distinctFieldName,
						mapFunction,
						reduceFunction
				);
			}
			return descriptor;
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
	 * @return returns the array ({@code List}) (for many documents) or the object ({@code Document}) for one document
	 * @see <a href="https://docs.mongodb.com/manual/tutorial/insert-documents/">insert documents</a>
	 */
	private Document parse(String json) {
		return (Document) parseAsObject( json );
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

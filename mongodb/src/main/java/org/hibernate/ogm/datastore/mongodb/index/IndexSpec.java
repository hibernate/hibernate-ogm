/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.index;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.index.OgmIndexSpec;

import java.util.Map;

/**
 * Spec for specifying the Index to be applied to a Mongo collection field
 *
 * @author Francois Le Droff
 */
public class IndexSpec implements OgmIndexSpec {

	/**
	 * the mongo collection/table for which the index will be set
	 */
	private String collection;


	/**
	 * The collection field path for which the index will be set
	 */
	private String field;


	/**
	 * indexing order
	 */
	private IndexOrder order;

	/**
	 * Optional. Builds the index in the background so that building an index does not block other database activities.
	 */
	private boolean background;

	/**
	 * A unique index causes MongoDB to reject all documents that contain a duplicate value for the indexed field.
	 * @see http://docs.mongodb.org/manual/core/index-unique/
	 * true if the indexed field is bound to be unique within the collection
	 */
	private boolean unique;

	/**
	 * Optional. The name of the index. If unspecified, MongoDB generates an index name
	 * by concatenating the names of the indexed fields and the sort order.
	 *
	 * Whether user specified or MongoDB generated, index names including their full namespace (i.e. database.collection)
	 * cannot be longer than the Index Name Limit (that is 128 characters)
	 */
	private String name;

	//TODO partialFilterExpression

	/**
	 * Optional. If true, the index only references documents with the specified field.
	 * These indexes use less space but behave differently in some situations (particularly sorts).
	 * The default value is false
	 */
	private boolean sparse;

	/**
	 * Optional. Specifies a value, in seconds, as a TTL to control
	 * how long MongoDB retains documents in this collection
	 */
	private int expireAfterSeconds;


	//TODO storageEngine

	/**
	 * //TODO we dont support compound index yet : for that we'll have to have a map of field and order
	 * @param field the Field that will be indexed
	 *
	 */
	public IndexSpec(String collection, String field, Indexed index)
	{
		this.field = field;
		this.collection = collection;
		this.order = index.order();
		this.background = index.background();
		this.expireAfterSeconds = index.expireAfterSeconds();
		this.name = index.name();
		this.sparse = index.sparse();
		this.unique = index.unique();
	}

	private DBObject indexKeys;

	public IndexSpec(UniqueKey uniqueKey) {

		indexKeys =  new BasicDBObject();
		this.addIndexKeys(uniqueKey.getColumnOrderMap());
		this.name = uniqueKey.getName();
		this.collection = uniqueKey.getTable().getName();
	}

	public IndexSpec(Index next) {
	}


	public String getCollection() {
		return collection;
	}

	private void addIndexKeys(Map<Column,String> columnOrderMap) {
		for(Column column : columnOrderMap.keySet())
		{
			indexKeys.put(column.getName(),(columnOrderMap.get(column).equals("asc")) ? 1 : -1);
		}
	}

	public DBObject getIndexKeys() {
		return indexKeys;
		/*DBObject dbo = new BasicDBObject();
		dbo.put(field,order.getIndexKeyValue());
		return dbo;*/
	}

	public DBObject getIndexOptions() {

		DBObject dbo = new BasicDBObject();
		if(!name.isEmpty()) {
			dbo.put("name", name);
		}
		if (unique) {
			dbo.put("unique", true);
		}
		if (sparse) {
			dbo.put("sparse", true);
		}
		if (background) {
			dbo.put("background", true);
		}
		if (expireAfterSeconds >= 0) {
			dbo.put("expireAfterSeconds", expireAfterSeconds);
		}

		return dbo;
	}

}

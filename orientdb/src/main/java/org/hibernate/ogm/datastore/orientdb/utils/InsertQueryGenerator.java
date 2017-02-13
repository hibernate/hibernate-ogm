/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import org.hibernate.ogm.datastore.orientdb.dto.GenerationResult;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.dto.EmbeddedColumnInfo;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.spi.Tuple;
import org.json.simple.JSONObject;
import org.hibernate.ogm.model.spi.Association;

/**
 * The class is generator of 'insert' queries.
 * <p>
 * OrientDB supports 'insert' query with JSON format like 'insert into classname content {"name":"value"}'. The format
 * allow to insert embedded classes. But JDBC driver not supports parameters (like '?' or ':name') in query that the
 * type. <b>All binary values must be saved by Base64 encoding.</b>
 * </p>
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 * @see <a href="http://orientdb.com/docs/2.2/SQL-Insert.html">Insert query in OrientDB</a>
 */
@SuppressWarnings("unchecked")
public class InsertQueryGenerator {

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * generate 'insert' query
	 *
	 * @param className name of OrientDB class
	 * @param tuple tuple
	 * @param isStoreTuple true - the class is tuple, false - the class of association
	 * @param keyColumnNames collection of column names
	 * @return result
	 * @see GenerationResult
	 */
	public GenerationResult generate(String className, Tuple tuple, boolean isStoreTuple, Set<String> keyColumnNames) {
		return generate( className, TupleUtil.toMap( tuple ), isStoreTuple, keyColumnNames );
	}

	/**
	 * generate 'insert' query
	 *
	 * @param className name of OrientDB class
	 * @param valuesMap map with column names and their values
	 * @param isStoreTuple true - the class is tuple, false - the class of association
	 * @param keyColumnNames collection of column names
	 * @return result
	 * @see GenerationResult
	 */

	public GenerationResult generate(String className, Map<String, Object> valuesMap, boolean isStoreTuple, Set<String> keyColumnNames) {
		JSONObject queryJsonContent = createJSON( isStoreTuple, keyColumnNames, valuesMap );
		StringBuilder insertQuery = new StringBuilder( 100 );
		insertQuery.append( "insert into " ).append( className ).append( " content " ).append( queryJsonContent.toJSONString() );
		return new GenerationResult( insertQuery.toString() );
	}

	/**
	 * Create JSON object for 'CONTENT' part of the query
	 *
	 * @param isStoreTuple 'true' for storing {@link Tuple} and 'false' for storing {@link Association}
	 * @param keyColumnNames set of columns for inserting
	 * @param valuesMap map of values for inserting
	 * @return JSON
	 */
	protected JSONObject createJSON(boolean isStoreTuple, Set<String> keyColumnNames, Map<String, Object> valuesMap) {
		JSONObject result = new JSONObject();
		for ( Map.Entry<String, Object> entry : valuesMap.entrySet() ) {
			String columnName = entry.getKey();
			Object columnValue = entry.getValue();
			if ( OrientDBConstant.SYSTEM_FIELDS.contains( columnName ) ) {
				continue;
			}
			log.debugf( "createJSON: Column %s; value: %s (class: %s). is primary key: %b ",
					columnName, columnValue, ( columnValue != null ? columnValue.getClass() : null ),
					keyColumnNames.contains( columnName ) );
			if ( EntityKeyUtil.isEmbeddedColumn( columnName ) ) {
				if ( isStoreTuple && keyColumnNames.contains( columnName ) ) {
					// it is primary key column
					columnName = columnName.substring( columnName.indexOf( "." ) + 1 );
					result.put( columnName, columnValue );
				}
				else {
					EmbeddedColumnInfo ec = new EmbeddedColumnInfo( columnName );
					if ( !result.containsKey( ec.getClassNames().get( 0 ) ) ) {
						JSONObject embeddedFieldValue = createEmbeddedRowTemplate( ec.getClassNames().get( 0 ) );
						result.put( ec.getClassNames().get( 0 ), embeddedFieldValue );
					}
					setJsonValue( result, ec, columnValue );
				}
			}
			else if ( columnValue != null && OrientDBConstant.BASE64_TYPES.contains( columnValue.getClass() ) ) {
				if ( columnValue instanceof BigInteger ) {
					result.put( columnName, new String( Base64.encodeBase64( ( (BigInteger) columnValue ).toByteArray() ) ) );
				}
				else if ( columnValue instanceof byte[] ) {
					result.put( columnName, new String( Base64.encodeBase64( (byte[]) columnValue ) ) );
				}
			}
			else if ( columnValue instanceof Date || columnValue instanceof Calendar ) {
				Calendar calendar = null;
				if ( columnValue instanceof Date ) {
					calendar = Calendar.getInstance();
					calendar.setTime( (Date) columnValue );
				}
				else if ( columnValue instanceof Calendar ) {
					calendar = (Calendar) columnValue;
				}
				String formattedStr = FormatterUtil.getDateTimeFormater().get().format( calendar.getTime() );
				result.put( columnName, formattedStr );
			}
			else if ( columnValue instanceof Character ) {
				result.put( columnName, ( (Character) columnValue ).toString() );
			}
			else {
				result.put( columnName, columnValue );
			}
		}
		return result;
	}

	private void setJsonValue(JSONObject result, EmbeddedColumnInfo ec, Object value) {
		log.debugf( "setJsonValue. EmbeddedColumnInfo: %s", ec );
		JSONObject json = (JSONObject) result.get( ec.getClassNames().get( 0 ) );
		for ( int i = 1; i < ec.getClassNames().size(); i++ ) {
			log.debugf( "setJsonValue. index: %d; className: %s", i, ec.getClassNames().get( i ) );
			if ( !json.containsKey( ec.getClassNames().get( i ) ) ) {
				json.put( ec.getClassNames().get( i ), createEmbeddedRowTemplate( ec.getClassNames().get( i ) ) );
			}
			json = (JSONObject) json.get( ec.getClassNames().get( i ) );
		}
		json.put( ec.getPropertyName(), value );
	}

	private JSONObject createEmbeddedRowTemplate(String className) {
		JSONObject embeddedFieldValue = new JSONObject();
		embeddedFieldValue.put( "@type", "d" );
		embeddedFieldValue.put( "@class", className );
		return embeddedFieldValue;
	}
}

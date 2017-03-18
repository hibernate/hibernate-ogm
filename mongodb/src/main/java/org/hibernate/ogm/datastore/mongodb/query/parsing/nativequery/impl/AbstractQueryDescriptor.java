/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import org.hibernate.ogm.util.impl.StringHelper;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import org.bson.Document;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
abstract class AbstractQueryDescriptor implements QueryDescriptor {
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
	protected Document parse(String json) {
		return (Document) parseAsObject( json );
	}

	/**
	 * parse JSON
	 * @param json
	 * @return
	 * @see <a href="http://stackoverflow.com/questions/34436952/json-parse-equivalent-in-mongo-driver-3-x-for-java"> JSON.parse equivalent</a>
	 */
	protected Object parseAsObject(String json) {
		if ( StringHelper.isNullOrEmptyString( json ) ) {
			return null;
		}
		Document object = Document.parse( "{ 'json': " + json + "}" );
		return object.get( "json" );
	}

	protected  Collation parseCollation(String json) {
		Document dbObject = ( (Document) parseAsObject( json ) );

		if ( dbObject != null ) {
			dbObject = (Document) dbObject.get( "collation" );
			if ( dbObject != null ) {
				Collation collation = Collation.builder()
						.locale( (String) dbObject.get( "locale" ) )
						.caseLevel( (Boolean) dbObject.get( "caseLevel" ) )
						.numericOrdering( (Boolean) dbObject.get( "numericOrdering" ) )
						.backwards( (Boolean) dbObject.get( "backwards" ) )
						.collationCaseFirst( caseFirst( dbObject ) )
						.collationStrength( strength( dbObject ) )
						.collationAlternate( alternate( dbObject ) )
						.collationMaxVariable( maxVariable( dbObject ) )
						.build();

				return collation;
			}

		}
		return null;
	}

	private  CollationCaseFirst caseFirst(Document dbObject) {
		String caseFirst = dbObject.getString( "caseFirst" );
		return caseFirst == null ? null : CollationCaseFirst.fromString( caseFirst );
	}

	private  CollationStrength strength(Document dbObject) {
		Integer strength = dbObject.getInteger( "strength" );
		return strength == null ? null : CollationStrength.fromInt( strength );
	}

	private  CollationAlternate alternate(Document dbObject) {
		String value = dbObject.getString( "alternate" );
		return value == null ? null : CollationAlternate.fromString( value );
	}

	private  CollationMaxVariable maxVariable(Document dbObject) {
		String value = dbObject.getString( "maxVariable" );
		return value == null ? null : CollationMaxVariable.fromString( value );
	}


}

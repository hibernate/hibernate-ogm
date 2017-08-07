/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class DocumentUtil {

	public static Map<String, Object> toMap(Document document) {
		Map<String, Object> result = new LinkedHashMap<>();
		for ( Map.Entry<String, Object> entry : document.entrySet() ) {
			String key = entry.getKey();
			Object value = entry.getValue();
			result.put( key, value );
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static List<Document> fromJsonArray(String sourceJson) {
		Document jsonDocument = Document.parse( "{'json': " + sourceJson + "}" );
		return (List<Document>) jsonDocument.get( "json" );
	}

}

/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.value;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Structured value for representing complex structures within Redis.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Mark Paluch
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = StructuredValue.TYPE_DISCRIMINATOR_FIELD_NAME
)
public abstract class StructuredValue {

	/**
	 * Name of the document type discriminator field
	 */
	public static final String TYPE_DISCRIMINATOR_FIELD_NAME = "$type";


	public StructuredValue() {
	}

	@Override
	public String toString() {
		return JsonToStringHelper.toString( this );
	}

	/**
	 * Creates a JSON representation of given documents. As static inner class this is only loaded on demand, i.e. when
	 * {@code toString()} is invoked on a document type.
	 *
	 * @author Gunnar Morling
	 */
	private static class JsonToStringHelper {

		/**
		 * Thread-safe as per the docs.
		 */
		private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

		private static String toString(StructuredValue structuredValue) {
			try {
				return writer.writeValueAsString( structuredValue );
			}
			catch (Exception e) {
				return e.toString();
			}
		}
	}
}

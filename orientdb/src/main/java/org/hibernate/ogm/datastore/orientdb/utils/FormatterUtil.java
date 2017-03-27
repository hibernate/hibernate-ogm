/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import java.text.DateFormat;

/**
 * The utility class contains thread-local formatters for format date and datetime
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class FormatterUtil {

	private static ThreadLocal<DateFormat> dateFormater = null;
	private static ThreadLocal<DateFormat> dateTimeFormater = null;

	/**
	 * get thread local formatter for date
	 *
	 * @return formatter
	 */
	public static ThreadLocal<DateFormat> getDateFormater() {
		return dateFormater;
	}

	/**
	 * set thread local formatter for date
	 *
	 * @param dateFormater formatter
	 */

	public static void setDateFormatter(ThreadLocal<DateFormat> dateFormater) {
		FormatterUtil.dateFormater = dateFormater;
	}

	/**
	 * get thread local formatter for datetime
	 *
	 * @return formatter
	 */
	public static ThreadLocal<DateFormat> getDateTimeFormater() {
		return dateTimeFormater;
	}

	/**
	 * set thread local formatter for datetime
	 *
	 * @param dateTimeFormater formatter
	 */

	public static void setDateTimeFormatter(ThreadLocal<DateFormat> dateTimeFormater) {
		FormatterUtil.dateTimeFormater = dateTimeFormater;
	}

}

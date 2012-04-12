/**
 * 
 */
package org.hibernate.ogm.type.descriptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.hibernate.HibernateException;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;

/**
 * @author Oliver Carr ocarr@redhat.com
 * 
 * NOTE: This class should probably be placed in hibernate-core
 *
 */
public class OgmCalendarDateTypeDescriptor extends CalendarDateTypeDescriptor {

	private static final Log log = LoggerFactory.make();

	public static final OgmCalendarDateTypeDescriptor INSTANCE = new OgmCalendarDateTypeDescriptor();
	
	private SimpleDateFormat DATE_TIME_FORMAT;
	
	public OgmCalendarDateTypeDescriptor() {
		DATE_TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS Z");
		DATE_TIME_FORMAT.setLenient(false);
	}
	
	@Override
	public Calendar fromString(String string) {
		log.info("OgmCalendar reading: " + string);
		Calendar result = new GregorianCalendar();
		try {
			result.setTime(DATE_TIME_FORMAT.parse(string));
		}
		catch ( ParseException pe) {
			log.error("OgmCalendar reading failed " + result);
			throw new HibernateException( "could not parse date string" + string, pe );
		}
		log.info("OgmCalendar reading created " + result);
		return result;
	}

	@Override
	public String toString(Calendar value) {
		log.info("OgmCalendar formatting: " + value);
		log.info("OgmCalendar formatted: " + DATE_TIME_FORMAT.format(value.getTime()));
		return DATE_TIME_FORMAT.format(value.getTime());
	}

}

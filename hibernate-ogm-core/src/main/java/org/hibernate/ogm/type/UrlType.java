package org.hibernate.ogm.type;

import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.ogm.type.descriptor.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.UrlTypeDescriptor;

/**
 * @author Nicolas Helleringer
 */
public class UrlType extends AbstractGenericBasicType<URL> {

	public static final UrlType INSTANCE = new UrlType();

	public UrlType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, UrlTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "url";
	}
	
	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}

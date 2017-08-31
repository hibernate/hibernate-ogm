/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Blob;

import org.hibernate.HibernateError;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.BlobProxy;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.BasicGridBinder;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class BlobMappedGridTypeDescriptor implements GridTypeDescriptor {

	public static final BlobMappedGridTypeDescriptor INSTANCE = new BlobMappedGridTypeDescriptor();
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	public <X> GridValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicGridBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(Tuple resultset, X value, String[] names, WrapperOptions options) {
				Proxy proxy = (Proxy) value;
				InvocationHandler handler = Proxy.getInvocationHandler( proxy );
				log.info( "handler:" + handler );

				BinaryStream stream = null;
				try {
					Method getUnderlyingStreamMethod = getUnderlyingStreamMethod();
					stream = (BinaryStream) handler.invoke( proxy, getUnderlyingStreamMethod, new Object[0] );
				}
				catch (Throwable th) {
					throw new HibernateError( "Cannot get input stream!", th );
				}
				resultset.put( names[0], stream );
			}
		};
	}


	private Method getUnderlyingStreamMethod() {
		//@todo cache it
		for ( Method method : BlobProxy.class.getDeclaredMethods() ) {
			if ( "getUnderlyingStream".equals( method.getName() ) ) {
				return method;
			}
		}
		return null;
	}


	@Override
	public <X> GridValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new GridValueExtractor<X>() {

			@Override
			public X extract(Tuple tuple, String name) {
				MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
				//final GridFSDownloadStream binaryStream = (GridFSDownloadStream) tuple.get( name );
				final ByteArrayOutputStream binaryStream = (ByteArrayOutputStream) tuple.get( name );
				ByteArrayInputStream blobStream = new ByteArrayInputStream( binaryStream.toByteArray() );
				Blob blob = BlobProxy.generateProxy( blobStream, binaryStream.size() );
				return (X) blob;
			}
		};
	}
}

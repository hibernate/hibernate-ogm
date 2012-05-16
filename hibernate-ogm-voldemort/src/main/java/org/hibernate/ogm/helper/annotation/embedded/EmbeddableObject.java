/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.ogm.helper.annotation.embedded;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class EmbeddableObject {

	private final Map<String, Object> tuple;
	private final Map<String, Class> cls;

	public EmbeddableObject(Map<String, Object> tuple, Map<String, Class> cls) {
		this.tuple = tuple;
		this.cls = cls;
	}

	public Iterator<Entry<String, Object>> getEntrySetFromTuple() {
		return tuple.entrySet().iterator();
	}

	public Iterator<Entry<String, Class>> getEntrySetFromCls() {
		return cls.entrySet().iterator();
	}
	
	public Object getTuple(String key){
		return tuple.get( key );
	}
	
	public Class getCls(String key){
		return cls.get( key );
	}
}

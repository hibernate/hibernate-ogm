/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Nicolas Helleringer
 */
@Entity @Persister( impl = OgmEntityPersister.class)
public class Bookmark {

	@Id
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	private String id;

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	private String description;

	public URL getUrl() { return url; }
	public void setUrl(URL url ) { this.url = url; }
	private URL url; 
	
	@Column(name = "site_weigth")
	public BigDecimal getSiteWeigth() { return siteWeigth; }
	public void setSiteWeigth(BigDecimal siteWeigth) { this.siteWeigth= siteWeigth; }
	private BigDecimal siteWeigth;
	
	@Column(name = "visits_count")
	public BigInteger getVisitCount() { return visitCount; }
	public void setVisitCount(BigInteger visitCount) { this.visitCount= visitCount; }
	private BigInteger visitCount;	
	
	@Column(name = "is_favourite")
	public Boolean isFavourite() { return favourite; }
	public void setFavourite(Boolean favourite) { this.favourite= favourite; }
	private Boolean favourite;
	
	@Column(name = "display_mask")
	public Byte getDisplayMask() { return displayMask; }
	public void setDisplayMask(Byte displayMask) { this.displayMask= displayMask; }
	private Byte displayMask;
	
}

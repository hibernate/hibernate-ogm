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
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Nicolas Helleringer
 */
@Entity
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
	
	@Column(name = "site_weight")
	public BigDecimal getSiteWeight() { return siteWeight; }
	public void setSiteWeight(BigDecimal siteWeight) { this.siteWeight= siteWeight; }
	private BigDecimal siteWeight;
	
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

	@Temporal(TemporalType.DATE)
	public Date getCreationDate() { return creationDate; }
	public void setCreationDate(Date date) {  this.creationDate = date; }
	private Date creationDate;

	@Temporal(TemporalType.TIME)
	public Date getUpdateDate() { return updateDate; }
	public void setUpdateDate(Date updateDate) {  this.updateDate = updateDate; }
	private Date updateDate;

	@Temporal(TemporalType.TIMESTAMP)
	public Date getDestructionDate() { return destructionDate; }
	public void setDestructionDate(Date destructionDate) {  this.destructionDate = destructionDate; }
	private Date destructionDate;

	@Temporal(TemporalType.DATE)
	public Calendar getCreationCalendar() { return creationCalendar; }
	public void setCreationCalendar(Calendar creationCalendar) {  this.creationCalendar = creationCalendar; }
	private Calendar creationCalendar;

	//not supported by core today: nobody misses it apparently ;)
//	@Temporal(TemporalType.TIME)
//	public Calendar getUpdateCalendar() { return updateCalendar; }
//	public void setUpdateCalendar(Calendar updateCalendar) {  this.updateCalendar = updateCalendar; }
//	private Calendar updateCalendar;

	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getDestructionCalendar() { return destructionCalendar; }
	public void setDestructionCalendar(Calendar destructionCalendar) {  this.destructionCalendar = destructionCalendar; }
	private Calendar destructionCalendar;

	@Enumerated(EnumType.STRING)
	public BookmarkType getType() { return type; }
	public void setType(BookmarkType type) {  this.type = type; }
	private BookmarkType type;

	@Lob
	@Basic(fetch = FetchType.EAGER)
	@Column(name = "DS_BLOB")
	public byte[] getBlob() { return blob; }
	public void setBlob(byte[] blob) {  this.blob = blob; }
	private byte[] blob;
	
	public UUID getSerialNumber() { return serialNumber; }
	public void setSerialNumber(UUID serialNumber) { this.serialNumber = serialNumber; }
	private UUID serialNumber;
	
	public Integer getStockCount() { return stockCount; }
	public void setStockCount(Integer stockCount) { this.stockCount = stockCount; }
	private Integer stockCount;

	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	private Long userId;
	
}

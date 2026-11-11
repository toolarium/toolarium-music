/*
 * CreditImpl.java
 *
 * Copyright by toolarium, all rights reserved.
 */

package com.github.toolarium.music.model.score.impl;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.github.toolarium.music.model.score.ICredit;
import java.util.Objects;

/**
 * Implements the {@link ICredit} interface.
 *
 * @author patrick
 */
public class CreditImpl implements ICredit {

    @JacksonXmlProperty(isAttribute = true, localName = "page")
    private Integer page;

    @JacksonXmlProperty(localName = "credit-type")
    private String creditType;

    @JacksonXmlProperty(localName = "credit-words")
    private CreditWordsImpl creditWords;


    /**
     * Gets the page.
     *
     * @return the page
     */
    @Override
    public Integer getPage() {
        return page;
    }


    /**
     * Sets the page.
     *
     * @param page the page
     */
    public void setPage(Integer page) {
        this.page = page;
    }


    /**
     * Gets the credit type.
     *
     * @return the credit type
     */
    @Override
    public String getCreditType() {
        return creditType;
    }


    /**
     * Sets the credit type.
     *
     * @param creditType the credit type
     */
    public void setCreditType(String creditType) {
        this.creditType = creditType;
    }


    /**
     * Gets the credit words.
     *
     * @return the credit words
     */
    @Override
    public CreditWordsImpl getCreditWords() {
        return creditWords;
    }


    /**
     * Sets the credit words.
     *
     * @param creditWords the credit words
     */
    public void setCreditWords(CreditWordsImpl creditWords) {
        this.creditWords = creditWords;
    }


    /**
     * Returns a hash code value for this object.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(page, creditType, creditWords);
    }


    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param obj the reference object to compare
     * @return true if this object is the same as the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CreditImpl other = (CreditImpl) obj;
        return Objects.equals(page, other.page)
                && Objects.equals(creditType, other.creditType)
                && Objects.equals(creditWords, other.creditWords);
    }


    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "CreditImpl [page=" + page
                + ", creditType=" + creditType
                + ", creditWords=" + creditWords + "]";
    }
}
